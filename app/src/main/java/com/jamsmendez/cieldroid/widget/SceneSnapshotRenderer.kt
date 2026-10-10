package com.jamsmendez.cieldroid.widget

import android.app.Presentation
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.util.Log
import android.view.View
import com.jamsmendez.cieldroid.domain.PreviewSettings
import com.jamsmendez.cieldroid.scenes.PreviewController
import com.jamsmendez.cieldroid.scenes.SessionPhase
import com.jamsmendez.cieldroid.scenes.WebViewSceneRuntime
import com.jamsmendez.cieldroid.wallpaper.Teardown
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicLong

private const val TAG = "CielSnapshot"

/**
 * Renders one still frame of an original scene off-screen: ImageReader surface -> private
 * VirtualDisplay -> Presentation -> its own WebViewSceneRuntime/PreviewController. Main thread only.
 */
object SceneSnapshotRenderer {
    const val DISPLAY_NAME = "CielSnapshot"
    private const val SETTLE_MS = 500L
    private const val POLL_MS = 100L
    private val nextId = AtomicLong(1)

    /** Returns null on timeout, scene ERROR or failure; every part is released on every path. */
    suspend fun render(context: Context, settings: PreviewSettings, widthPx: Int, heightPx: Int, timeoutMs: Long): Bitmap? {
        val id = nextId.getAndIncrement()
        val teardown = Teardown { part, error -> Log.e(TAG, "snapshot=$id release of $part failed", error) }
        try {
            return withTimeoutOrNull(timeoutMs) { capture(context, id, settings, widthPx, heightPx, teardown) }
                .also { if (it == null) Log.w(TAG, "snapshot=$id produced no frame (timeout or scene error)") }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "snapshot=$id failed", e)
            return null
        } finally {
            teardown.releaseAll()
            Log.i(TAG, "snapshot=$id released")
        }
    }

    private suspend fun capture(
        context: Context, id: Long, settings: PreviewSettings, width: Int, height: Int, teardown: Teardown,
    ): Bitmap? {
        // Render directly at the downscaled size (longest side <= 512): no post-capture scaling pass.
        val size = SnapshotGeometry.fitWithin(width, height)
        val dpi = SnapshotGeometry.renderDensityDpi(size.width, size.height, context.resources.displayMetrics.densityDpi)
        val reader = teardown.add(
            "reader", ImageReader.newInstance(size.width, size.height, PixelFormat.RGBA_8888, 2),
        ) { it.close() }
        val display = teardown.add(
            "display",
            context.getSystemService(DisplayManager::class.java).createVirtualDisplay(
                "$DISPLAY_NAME-$id", size.width, size.height, dpi, reader.surface,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY,
            ) ?: error("VirtualDisplay unavailable"),
        ) { it.release() }
        val scope = teardown.add("scope", CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)) { it.cancel() }
        val presentation = teardown.add("presentation", Presentation(context, display.display)) { it.dismiss() }
        var runtime: WebViewSceneRuntime? = null
        // Disposing the controller releases the runtime (view detached before WebView.destroy).
        val controller = teardown.add(
            "controller",
            PreviewController(id, settings, scope, { runtime?.command(it) }, { runtime?.visible(it) }, { runtime?.release() }),
        ) { it.dispose() }
        val created = WebViewSceneRuntime(presentation.context, id, settings, controller::event)
        runtime = created
        created.view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        presentation.setContentView(created.view)
        presentation.show()
        created.load()
        controller.ownerResumed(true)
        val phase = controller.state.first { it.phase != SessionPhase.LOADING }.phase
        if (phase != SessionPhase.READY) return null
        delay(SETTLE_MS)
        while (true) {
            reader.acquireLatestImage()?.use { image ->
                val plane = image.planes[0]
                val buffer = plane.buffer
                val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
                val pixels = SnapshotPixels.rgbaToArgb(bytes, image.width, image.height, plane.rowStride, plane.pixelStride)
                Log.i(TAG, "snapshot=$id captured ${image.width}x${image.height} dpi=$dpi $settings")
                return Bitmap.createBitmap(pixels, image.width, image.height, Bitmap.Config.ARGB_8888)
            }
            delay(POLL_MS)
        }
    }
}
