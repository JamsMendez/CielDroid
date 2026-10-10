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
import android.view.ViewConfiguration
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.FrameLayout
import com.jamsmendez.cieldroid.data.PreferencesSettingsRepository
import com.jamsmendez.cieldroid.data.settingsPreferences
import com.jamsmendez.cieldroid.domain.OverlayDrag
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
    /** Re-entrancy safe: a release clears the slot first, so late or nested error callbacks are no-ops. */
    private val slot = GenerationSlot<Generation>(
        onStartFailure = { id, error -> Log.e(TAG, "gen=$id start failed; releasing", error) },
    ) { gen ->
        Log.i(TAG, "gen=${gen.id} released")
        gen.teardown.releaseAll()
    }
    private val generation: Generation? get() = slot.current
    private var screenReceiver: BroadcastReceiver? = null
    private var repository: PreferencesSettingsRepository? = null
    private var moveMode = false
    /**
     * The user's anchor: the last position a real drag chose; null means the stored one (or the default).
     * Clamping to the current screen is a display-only projection of it and is never persisted.
     */
    private var anchor: OverlayPosition? = null

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
                val current = generation ?: return
                try {
                    current.controller.ownerResumed(interactive)
                } catch (e: Exception) {
                    Log.e(TAG, "gen=${current.id} ownerResumed failed; releasing", e)
                    slot.fail(current.id)
                }
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
        generation?.let { Log.i(TAG, "gen=${it.id} releasing: settings changed to $next") }
        val id = nextGeneration++
        slot.begin(id)
        val built = try {
            buildGeneration(id, next)
        } catch (e: Exception) {
            Log.e(TAG, "gen=$id creation failed; no overlay", e)
            slot.abort(id)
            return
        }
        // Installed before it starts, so a throwing ownerResumed releases it. A failure reported while building
        // (e.g. a synchronous load error) releases it here instead, never started.
        if (!slot.activate(id, built) { it.controller.ownerResumed(interactive) }) {
            Log.w(TAG, "gen=$id failed while building or starting; no overlay")
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
                            if (slot.fail(id)) Log.i(TAG, "gen=$id failed; releasing")
                        }
                    }
            }
            val bounds = windowManager.currentWindowMetrics.bounds
            val size = OverlayPlacement.size(minOf(bounds.width(), bounds.height()))
            // The scene keeps its full side; the smaller window crops its transparent border around the ring.
            val frame = OverlayFrame(windowContext)
            frame.addView(created.view, FrameLayout.LayoutParams(size.webSide, size.webSide, Gravity.CENTER))
            // Display-only projection of the anchor; nothing is persisted here.
            val start = OverlayPlacement.clamp(
                anchor ?: storedPosition() ?: defaultPosition(windowManager, size.windowSide),
                size.windowSide, bounds.width(), bounds.height(),
            )
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

    private fun storedPosition(): OverlayPosition? = repository?.state?.value?.overlay?.position

    /**
     * Drags move the window clamped to the screen; the position is persisted once, when a real drag ends.
     * A tap (within touch slop) or a drag back to its origin persists nothing, so the default stays unstored.
     */
    private fun attachDrag(window: Window) {
        val drag = OverlayDrag(ViewConfiguration.get(window.frame.context).scaledTouchSlop)
        window.frame.onDown = { rawX, rawY -> drag.down(rawX, rawY, OverlayPosition(window.params.x, window.params.y)) }
        window.frame.onMove = { rawX, rawY ->
            val bounds = window.manager.currentWindowMetrics.bounds
            drag.move(rawX, rawY, window.params.width, bounds.width(), bounds.height())?.let { next ->
                window.params.x = next.x
                window.params.y = next.y
                updateLayout(window)
            }
        }
        window.frame.onEnd = {
            val end = drag.end()
            if (end.dragged) Log.i(TAG, "drag ended persist=${end.persist}") else Log.i(TAG, "tap; nothing persisted")
            end.persist?.let {
                anchor = it
                persistPosition(it)
            }
            end.dragged
        }
    }

    private fun persistPosition(next: OverlayPosition) {
        val repository = repository ?: return
        Log.i(TAG, "persisting overlay position $next")
        scope.launch {
            try {
                repository.setOverlayPosition(next)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (e: Exception) { Log.w(TAG, "could not persist overlay position $next", e) }
        }
    }

    /**
     * Rotation or a display change: re-project the anchor (or the default recomputed for the new width) onto
     * the new screen. Display-only and never persisted, so landscape then portrait restores the original spot.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val window = generation?.window ?: return
        val bounds = window.manager.currentWindowMetrics.bounds
        val target = OverlayPlacement.clamp(
            anchor ?: storedPosition() ?: defaultPosition(window.manager, window.params.width),
            window.params.width, bounds.width(), bounds.height(),
        )
        if (target == OverlayPosition(window.params.x, window.params.y)) return
        window.params.x = target.x
        window.params.y = target.y
        updateLayout(window)
        Log.i(TAG, "re-projected overlay to $target for ${bounds.width()}x${bounds.height()}")
    }

    override fun onDestroy() {
        scope.cancel()
        screenReceiver?.let { unregisterReceiver(it) }
        screenReceiver = null
        generation?.let { Log.i(TAG, "gen=${it.id} releasing: service destroyed") }
        slot.clear()
        Log.i(TAG, "service destroyed")
        super.onDestroy()
    }
}
