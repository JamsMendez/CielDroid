package com.jamsmendez.cieldroid.wallpaper

import android.app.Presentation
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import android.view.View
import com.jamsmendez.cieldroid.domain.PreviewFps
import com.jamsmendez.cieldroid.domain.PreviewSettings
import com.jamsmendez.cieldroid.domain.SceneId
import com.jamsmendez.cieldroid.scenes.PreviewController
import com.jamsmendez.cieldroid.scenes.WebViewSceneRuntime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val TAG = "CielWallpaper"

/** W01 spike: unmeasured, fixed settings, no configuration UI. Not an adopted renderer. */
class SceneWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = SceneEngine()

    /** Engine callbacks run on the main looper; every field is main-thread owned. */
    private inner class SceneEngine : Engine() {
        private val settings = PreviewSettings(SceneId.IDLE, PreviewFps.THIRTY)
        private var nextGeneration = 1L
        private var generation: Generation? = null

        /** One VirtualDisplay, Presentation, controller and WebView; never shared. */
        private inner class Generation(val id: Long, var spec: DisplaySpec, holder: SurfaceHolder) {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
            val display: VirtualDisplay = getSystemService(DisplayManager::class.java).createVirtualDisplay(
                "CielWallpaper-$id", spec.width, spec.height, spec.densityDpi, holder.surface,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY,
            )
            // Presentation derives a display context and uses TYPE_PRIVATE_PRESENTATION for a private display.
            val presentation = Presentation(this@SceneWallpaperService, display.display)
            var runtime: WebViewSceneRuntime? = null
            val controller = PreviewController(
                id, settings, scope, { runtime?.command(it) }, { runtime?.visible(it) }, { runtime?.release() },
            )

            init {
                runtime = WebViewSceneRuntime(presentation.context, id, settings, controller::event)
                scope.launch {
                    controller.state.map { Triple(it.phase, it.drawReady, it.bridgeReady) to it.error }
                        .distinctUntilChanged()
                        .collect { (s, error) ->
                            Log.i(TAG, "gen=$id phase=${s.first} draw=${s.second} bridge=${s.third} error=$error")
                        }
                }
                val view = runtime!!.view
                view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                presentation.setContentView(view)
                presentation.show()
                runtime!!.load()
                Log.i(TAG, "gen=$id created display=${display.display.displayId} $spec")
            }

            fun release() {
                presentation.dismiss()
                controller.dispose()
                display.release()
                scope.cancel()
                Log.i(TAG, "gen=$id released")
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(false)
            Log.i(TAG, "engine created preview=$isPreview")
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            Log.i(TAG, "surface created")
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            val dpi = resources.displayMetrics.densityDpi
            when (val plan = DisplayPlan.of(generation?.spec, width, height, dpi)) {
                is DisplayPlan.Create -> {
                    val created = Generation(nextGeneration++, plan.spec, holder)
                    generation = created
                    created.controller.ownerResumed(isVisible)
                }
                is DisplayPlan.Resize -> generation?.let {
                    it.display.resize(plan.spec.width, plan.spec.height, plan.spec.densityDpi)
                    it.display.surface = holder.surface
                    it.spec = plan.spec
                    Log.i(TAG, "gen=${it.id} resized ${plan.spec}")
                }
                DisplayPlan.Unchanged -> Unit
                DisplayPlan.Invalid -> Log.w(TAG, "ignored surface ${width}x$height dpi=$dpi")
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            Log.i(TAG, "visible=$visible gen=${generation?.id}")
            generation?.controller?.ownerResumed(visible)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            releaseGeneration("surface destroyed")
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
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
