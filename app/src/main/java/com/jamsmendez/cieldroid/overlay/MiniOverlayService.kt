package com.jamsmendez.cieldroid.overlay

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.jamsmendez.cieldroid.data.PreferencesSettingsRepository
import com.jamsmendez.cieldroid.data.settingsPreferences
import com.jamsmendez.cieldroid.domain.PreviewSettings
import com.jamsmendez.cieldroid.domain.SceneVariant
import com.jamsmendez.cieldroid.scenes.PreviewController
import com.jamsmendez.cieldroid.scenes.SessionPhase
import com.jamsmendez.cieldroid.scenes.WebViewSceneRuntime
import com.jamsmendez.cieldroid.wallpaper.Teardown
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

private const val TAG = "CielOverlay"

/**
 * Prototype floating MINI scene: one touch-transparent TYPE_APPLICATION_OVERLAY window owning its own
 * controller/WebView generation. Plain started service (no foreground notification); started and
 * stopped explicitly from the launcher. Main-thread owned.
 */
class MiniOverlayService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var started = false
    private var settings: PreviewSettings? = null
    private var interactive = true
    private var nextGeneration = 1L
    private var generation: Generation? = null
    private var screenReceiver: BroadcastReceiver? = null

    private class Generation(val id: Long, val controller: PreviewController, val teardown: Teardown)

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
            val side = (minOf(bounds.width(), bounds.height()) * 0.6f).toInt()
            val margin = (16 * resources.displayMetrics.density).toInt()
            val params = WindowManager.LayoutParams(
                side, side, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                x = margin
                y = margin
                title = "CielDroid mini overlay"
            }
            windowManager.addView(created.view, params)
            teardown.add("window", created.view) { windowManager.removeViewImmediate(it) }
            created.load()
            Log.i(TAG, "gen=$id created side=$side $settings")
            Generation(id, controller, teardown)
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
