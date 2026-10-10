package com.jamsmendez.cieldroid.overlay

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.FrameLayout
import com.jamsmendez.cieldroid.data.PreferencesSettingsRepository
import com.jamsmendez.cieldroid.data.settingsPreferences
import com.jamsmendez.cieldroid.domain.OverlayPlacement
import com.jamsmendez.cieldroid.domain.OverlayPosition
import com.jamsmendez.cieldroid.domain.PreviewSettings
import com.jamsmendez.cieldroid.domain.SceneVariant
import com.jamsmendez.cieldroid.scenes.PreviewController
import com.jamsmendez.cieldroid.scenes.SessionPhase
import com.jamsmendez.cieldroid.scenes.WebViewSceneRuntime
import com.jamsmendez.cieldroid.wallpaper.Teardown
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

private const val TAG = "CielOverlay"

/**
 * Prototype floating MINI scene: one TYPE_APPLICATION_OVERLAY window owning its own controller/WebView
 * generation. Touches pass through it unless the persisted move mode is on; then the window frame
 * intercepts them to drag the window and the WebView never receives input. Plain started service
 * (no foreground notification); started and stopped explicitly from the launcher. Main-thread owned.
 */
class MiniOverlayService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var started = false
    private var settings: PreviewSettings? = null
    private var interactive = true
    private var nextGeneration = 1L
    private var generation: Generation? = null
    private var screenReceiver: BroadcastReceiver? = null
    private var repository: PreferencesSettingsRepository? = null
    private var moveMode = false
    /** Current window top-left; null until restored from settings or the default is computed. */
    private var position: OverlayPosition? = null

    private class Window(val manager: WindowManager, val frame: OverlayFrame, val params: WindowManager.LayoutParams)
    private class Generation(val id: Long, val controller: PreviewController, val teardown: Teardown, val window: Window)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "overlay permission not granted; stopping")
            stopSelf()
            return START_NOT_STICKY
        }
        if (!started) {
            started = true
            interactive = getSystemService(PowerManager::class.java).isInteractive
            registerScreenReceiver()
            observeSettings()
            Log.i(TAG, "service started interactive=$interactive")
        } else if (generation == null) {
            settings?.let(::applySettings) // An explicit Start after a failed generation rebuilds once.
        }
        return START_NOT_STICKY
    }

    private fun registerScreenReceiver() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                interactive = intent.action == Intent.ACTION_SCREEN_ON
                Log.i(TAG, "screen ${if (interactive) "on" else "off"} gen=${generation?.id}")
                // The mini ignores the bridge pause; the controller's visibility callback runs WebView.onPause.
                generation?.controller?.ownerResumed(interactive)
            }
        }
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF).apply { addAction(Intent.ACTION_SCREEN_ON) }
        registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED)
        screenReceiver = receiver
    }

    /** The overlay reuses the wallpaper scene/FPS for now, always as MINI. */
    private fun observeSettings() {
        val repository = PreferencesSettingsRepository(applicationContext.settingsPreferences, scope)
        this.repository = repository
        // Move mode applies live to the current window; the WebView generation is never recreated for it.
        scope.launch {
            repository.state.filter { it.loaded }.map { it.overlay.moveMode }.distinctUntilChanged().collect { enabled ->
                moveMode = enabled
                generation?.window?.let { applyFlags(it) }
                Log.i(TAG, "move mode=$enabled gen=${generation?.id}")
            }
        }
        scope.launch {
            repository.state
                .mapNotNull { state ->
                    when {
                        state.loaded -> state.wallpaper
                        state.error != null -> {
                            Log.w(TAG, "settings unreadable; using wallpaper defaults", state.error)
                            PreviewSettings.WALLPAPER_DEFAULT
                        }
                        else -> null
                    }
                }
                .map { it.copy(variant = SceneVariant.MINI) }
                .distinctUntilChanged()
                .collect(::applySettings)
        }
    }

    private fun applySettings(next: PreviewSettings) {
        settings = next
        releaseGeneration("settings changed to $next")
        val id = nextGeneration++
        generation = try {
            buildGeneration(id, next).also { it.controller.ownerResumed(interactive) }
        } catch (e: Exception) {
            Log.e(TAG, "gen=$id creation failed; no overlay", e)
            null
        }
    }

    /** Builds every part under one release stack, released in reverse order (window removed before destroy). */
    private fun buildGeneration(id: Long, settings: PreviewSettings) =
        Teardown.build({ part, error -> Log.e(TAG, "gen=$id release of $part failed", error) }) { teardown ->
            val display = getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)
            val windowContext = createWindowContext(display, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null)
            val windowManager = windowContext.getSystemService(WindowManager::class.java)
            val genScope = teardown.add("scope", CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)) { it.cancel() }
            var runtime: WebViewSceneRuntime? = null
            val controller = teardown.add(
                "controller",
                PreviewController(id, settings, genScope, { runtime?.command(it) }, { runtime?.visible(it) }, { runtime?.release() }),
            ) { it.dispose() }
            val created = WebViewSceneRuntime(windowContext, id, settings, controller::event, transparentBackground = true)
            runtime = created
            created.view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            genScope.launch {
                controller.state.map { Triple(it.phase, it.drawReady, it.bridgeReady) to it.error }
                    .distinctUntilChanged()
                    .collect { (s, error) ->
                        Log.i(TAG, "gen=$id phase=${s.first} draw=${s.second} bridge=${s.third} error=$error")
                        // No retry loop: a failed generation removes its window; Start again or new settings rebuild.
                        if (s.first == SessionPhase.ERROR) scope.launch {
                            if (generation?.id == id) releaseGeneration("gen=$id failed")
                        }
                    }
            }
            val bounds = windowManager.currentWindowMetrics.bounds
            val size = OverlayPlacement.size(minOf(bounds.width(), bounds.height()))
            // The scene keeps its full side; the smaller window crops its transparent border around the ring.
            val frame = OverlayFrame(windowContext)
            frame.addView(created.view, FrameLayout.LayoutParams(size.webSide, size.webSide, Gravity.CENTER))
            val start = OverlayPlacement.clamp(
                position ?: repository?.state?.value?.overlay?.position ?: defaultPosition(windowManager, size.windowSide),
                size.windowSide, bounds.width(), bounds.height(),
            )
            position = start
            val params = WindowManager.LayoutParams(
                size.windowSide, size.windowSide, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                windowFlags(moveMode), PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                // Absolute display coordinates, matching the clamp: no inset fitting, no cutout avoidance.
                fitInsetsTypes = 0
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                x = start.x
                y = start.y
                title = "CielDroid mini overlay"
            }
            val window = Window(windowManager, frame, params)
            attachDrag(window)
            windowManager.addView(frame, params)
            teardown.add("window", frame) { windowManager.removeViewImmediate(it) }
            created.load()
            Log.i(TAG, "gen=$id created web=${size.webSide} window=${size.windowSide} at $start move=$moveMode $settings")
            Generation(id, controller, teardown, window)
        }

    /** Top-end corner below the status bar, as the former inset-fitted TOP|END window was. */
    private fun defaultPosition(windowManager: WindowManager, windowSide: Int): OverlayPosition {
        val metrics = windowManager.currentWindowMetrics
        return OverlayPlacement.defaultPosition(
            metrics.bounds.width(), windowSide, (OverlayPlacement.EDGE_MARGIN_DP * resources.displayMetrics.density).toInt(),
            topInset = metrics.windowInsets.getInsets(WindowInsets.Type.statusBars()).top,
        )
    }

    /** Move mode clears FLAG_NOT_TOUCHABLE; the window is never focusable. */
    private fun windowFlags(move: Boolean): Int {
        val base = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        return if (move) base else base or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
    }

    private fun applyFlags(window: Window) {
        val flags = windowFlags(moveMode)
        if (window.params.flags == flags) return
        window.params.flags = flags
        updateLayout(window)
    }

    private fun updateLayout(window: Window) {
        if (window.frame.isAttachedToWindow) window.manager.updateViewLayout(window.frame, window.params)
    }

    /** Drags move the window clamped to the screen; the position is persisted once, when the gesture ends. */
    private fun attachDrag(window: Window) {
        var startX = 0
        var startY = 0
        window.frame.onDragStart = {
            startX = window.params.x
            startY = window.params.y
        }
        window.frame.onDrag = { dx, dy ->
            val bounds = window.manager.currentWindowMetrics.bounds
            val next = OverlayPlacement.clamp(
                OverlayPosition(startX + dx, startY + dy), window.params.width, bounds.width(), bounds.height(),
            )
            window.params.x = next.x
            window.params.y = next.y
            position = next
            updateLayout(window)
        }
        window.frame.onDragEnd = { persistPosition(OverlayPosition(window.params.x, window.params.y)) }
    }

    private fun persistPosition(next: OverlayPosition) {
        val repository = repository ?: return
        scope.launch {
            try {
                repository.setOverlayPosition(next)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (e: Exception) { Log.w(TAG, "could not persist overlay position $next", e) }
        }
    }

    /** Rotation or a display change: keep the whole window on the new screen. */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val window = generation?.window ?: return
        val bounds = window.manager.currentWindowMetrics.bounds
        val current = OverlayPosition(window.params.x, window.params.y)
        val clamped = OverlayPlacement.clamp(current, window.params.width, bounds.width(), bounds.height())
        if (clamped == current) return
        window.params.x = clamped.x
        window.params.y = clamped.y
        position = clamped
        updateLayout(window)
        Log.i(TAG, "re-clamped overlay to $clamped for ${bounds.width()}x${bounds.height()}")
    }

    /** Idempotent: the field is cleared before release. */
    private fun releaseGeneration(reason: String) {
        val current = generation ?: return
        generation = null
        Log.i(TAG, "gen=${current.id} releasing: $reason")
        current.teardown.releaseAll()
    }

    override fun onDestroy() {
        scope.cancel()
        screenReceiver?.let { unregisterReceiver(it) }
        screenReceiver = null
        releaseGeneration("service destroyed")
        Log.i(TAG, "service destroyed")
        super.onDestroy()
    }
}
