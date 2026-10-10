package com.jamsmendez.cieldroid.wallpaper

import android.app.Presentation
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import com.jamsmendez.cieldroid.data.PreferencesSettingsRepository
import com.jamsmendez.cieldroid.data.settingsPreferences
import com.jamsmendez.cieldroid.domain.PreviewSettings
import com.jamsmendez.cieldroid.scenes.PreviewController
import com.jamsmendez.cieldroid.scenes.WebViewSceneRuntime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

private const val TAG = "CielWallpaper"

/** Provisional pipeline 1 renderer: unmeasured beyond the W02 pilot; settings come from the wallpaper keys. */
class SceneWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = SceneEngine()

    /** Engine callbacks and the settings collector run on the main looper; every field is main-thread owned. */
    private inner class SceneEngine : Engine() {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        private var settings: PreviewSettings? = null
        private var surfaceSpec: DisplaySpec? = null
        private var nextGeneration = 1L
        private var generation: Generation? = null

        /** One VirtualDisplay, Presentation, controller and WebView; never shared, released as one unit. */
        private inner class Generation(
            val id: Long,
            var spec: DisplaySpec,
            var surface: Surface,
            val display: VirtualDisplay,
            val controller: PreviewController,
            private val teardown: Teardown,
        ) {
            var released = false
                private set

            fun release() {
                released = true
                teardown.releaseAll()
                Log.i(TAG, "gen=$id released")
            }
        }

        /** Builds every part under one release stack: a failure releases what exists, in reverse order. */
        private fun buildGeneration(id: Long, spec: DisplaySpec, surface: Surface, settings: PreviewSettings) =
            Teardown.build({ part, error -> Log.e(TAG, "gen=$id release of $part failed", error) }) { teardown ->
                val genScope = teardown.add("scope", CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)) {
                    it.cancel()
                }
                val display = teardown.add(
                    "display",
                    getSystemService(DisplayManager::class.java).createVirtualDisplay(
                        "CielWallpaper-$id", spec.width, spec.height, spec.densityDpi, surface,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY,
                    ) ?: error("VirtualDisplay unavailable"),
                ) { it.release() }
                // Presentation derives a display context and uses TYPE_PRIVATE_PRESENTATION for a private display.
                val presentation = teardown.add(
                    "presentation", Presentation(this@SceneWallpaperService, display.display),
                ) { it.dismiss() }
                var runtime: WebViewSceneRuntime? = null
                // Disposing the controller releases the runtime (view detached before WebView.destroy).
                val controller = teardown.add(
                    "controller",
                    PreviewController(
                        id, settings, genScope, { runtime?.command(it) }, { runtime?.visible(it) }, { runtime?.release() },
                    ),
                ) { it.dispose() }
                val created = WebViewSceneRuntime(presentation.context, id, settings, controller::event)
                runtime = created
                genScope.launch {
                    controller.state.map { Triple(it.phase, it.drawReady, it.bridgeReady) to it.error }
                        .distinctUntilChanged()
                        .collect { (s, error) ->
                            Log.i(TAG, "gen=$id phase=${s.first} draw=${s.second} bridge=${s.third} error=$error")
                        }
                }
                created.view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                presentation.setContentView(created.view)
                val built = Generation(id, spec, surface, display, controller, teardown)
                // An external dismissal (e.g. display removed) leaves no content: drop the generation.
                presentation.setOnDismissListener { if (!built.released) onGenerationLost(built) }
                presentation.show()
                created.load()
                Log.i(TAG, "gen=$id created display=${display.display.displayId} $spec $settings")
                built
            }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(false)
            Log.i(TAG, "engine created preview=$isPreview")
            val repository = PreferencesSettingsRepository(applicationContext.settingsPreferences, scope)
            scope.launch {
                repository.state
                    .mapNotNull { state ->
                        when {
                            state.loaded -> state.wallpaper
                            state.error != null -> {
                                Log.w(TAG, "wallpaper settings unreadable; using defaults", state.error)
                                PreviewSettings.WALLPAPER_DEFAULT
                            }
                            else -> null
                        }
                    }
                    .distinctUntilChanged()
                    .collect(::applySettings)
            }
        }

        /** New settings always mean a new generation: the old one is released before the new one is built. */
        private fun applySettings(next: PreviewSettings) {
            settings = next
            if (generation != null) releaseGeneration("settings changed to $next")
            ensureGeneration()
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            Log.i(TAG, "surface created")
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            val dpi = resources.displayMetrics.densityDpi
            val current = generation
            val surfaceChanged = current != null && current.surface !== holder.surface
            when (val plan = DisplayPlan.of(current?.spec, width, height, dpi, surfaceChanged)) {
                is DisplayPlan.Create -> {
                    surfaceSpec = plan.spec
                    ensureGeneration()
                }
                // Resize in place: the Presentation and WebView stay alive across rotation.
                is DisplayPlan.Resize -> current?.let {
                    surfaceSpec = plan.spec
                    it.display.resize(plan.spec.width, plan.spec.height, plan.spec.densityDpi)
                    if (plan.swapSurface) swapSurface(it, holder.surface)
                    it.spec = plan.spec
                    Log.i(TAG, "gen=${it.id} resized ${plan.spec} swapSurface=${plan.swapSurface}")
                }
                DisplayPlan.SwapSurface -> current?.let { swapSurface(it, holder.surface) }
                DisplayPlan.Unchanged -> Unit
                DisplayPlan.Invalid -> Log.w(TAG, "ignored surface ${width}x$height dpi=$dpi")
            }
        }

        private fun swapSurface(current: Generation, surface: Surface) {
            current.display.surface = surface
            current.surface = surface
            Log.i(TAG, "gen=${current.id} surface swapped")
        }

        /** Builds a generation only when settings, a valid surface size and a live surface all exist. */
        private fun ensureGeneration() {
            if (generation != null) return
            val next = settings ?: return
            val spec = surfaceSpec ?: return
            val surface = surfaceHolder.surface?.takeIf { it.isValid } ?: return
            val id = nextGeneration++
            val built = try {
                buildGeneration(id, spec, surface, next)
            } catch (e: Exception) {
                Log.e(TAG, "gen=$id creation failed; no generation", e)
                return
            }
            generation = built
            built.controller.ownerResumed(isVisible)
        }

        private fun onGenerationLost(lost: Generation) {
            if (generation !== lost) return
            releaseGeneration("gen=${lost.id} presentation dismissed externally")
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            Log.i(TAG, "visible=$visible gen=${generation?.id}")
            // A lost or failed generation is retried on the next visible event, never in a loop.
            if (visible) ensureGeneration()
            generation?.controller?.ownerResumed(visible)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            surfaceSpec = null
            releaseGeneration("surface destroyed")
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            scope.cancel()
            surfaceSpec = null
            releaseGeneration("engine destroyed")
            super.onDestroy()
        }

        /** Idempotent: the field is cleared before release so a second call is a no-op. */
        private fun releaseGeneration(reason: String) {
            Log.i(TAG, reason)
            val current = generation ?: return
            generation = null
            current.release()
        }
    }
}
