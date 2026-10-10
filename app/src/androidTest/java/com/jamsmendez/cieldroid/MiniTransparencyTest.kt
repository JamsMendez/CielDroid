package com.jamsmendez.cieldroid

import android.app.Presentation
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.util.Log
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jamsmendez.cieldroid.domain.*
import com.jamsmendez.cieldroid.scenes.PreviewController
import com.jamsmendez.cieldroid.scenes.SessionPhase
import com.jamsmendez.cieldroid.scenes.WebViewSceneRuntime
import com.jamsmendez.cieldroid.wallpaper.Teardown
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Off-screen alpha check for the overlay pipeline: transparent WebView -> translucent Presentation ->
 * private VirtualDisplay -> RGBA ImageReader. Never adds an overlay window or needs its permission.
 */
@RunWith(AndroidJUnit4::class)
class MiniTransparencyTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val size = 400

    /** Width x height RGBA bytes, tightly packed. */
    private class Frame(val width: Int, val height: Int, val rgba: ByteArray) {
        fun alpha(x: Int, y: Int) = rgba[(y * width + x) * 4 + 3].toInt() and 0xFF
        fun bitmap(): Bitmap {
            val pixels = IntArray(width * height) { i ->
                val o = i * 4
                fun c(k: Int) = rgba[o + k].toInt() and 0xFF
                (c(3) shl 24) or (c(0) shl 16) or (c(1) shl 8) or c(2)
            }
            return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
        }
    }

    private fun render(settings: PreviewSettings): Frame = runBlocking {
        withContext(Dispatchers.Main) {
            val teardown = Teardown { part, error -> Log.e("CielMiniTest", "release of $part failed", error) }
            try { withTimeout(30_000) { capture(settings, teardown) } } finally { teardown.releaseAll() }
        }
    }

    private suspend fun capture(settings: PreviewSettings, teardown: Teardown): Frame {
        val reader = teardown.add("reader", ImageReader.newInstance(size, size, PixelFormat.RGBA_8888, 2)) { it.close() }
        val display = teardown.add(
            "display",
            context.getSystemService(DisplayManager::class.java).createVirtualDisplay(
                "$DISPLAY_NAME-${settings.variant}", size, size, context.resources.displayMetrics.densityDpi,
                reader.surface, DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY,
            ) ?: error("VirtualDisplay unavailable"),
        ) { it.release() }
        val scope = teardown.add("scope", CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)) { it.cancel() }
        val presentation = teardown.add("presentation", Presentation(context, display.display)) { it.dismiss() }
        presentation.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        presentation.window?.setFormat(PixelFormat.TRANSLUCENT)
        var runtime: WebViewSceneRuntime? = null
        val controller = teardown.add(
            "controller",
            PreviewController(1, settings, scope, { runtime?.command(it) }, { runtime?.visible(it) }, { runtime?.release() }),
        ) { it.dispose() }
        val created = WebViewSceneRuntime(presentation.context, 1, settings, controller::event, transparentBackground = true)
        runtime = created
        created.view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        presentation.setContentView(created.view)
        presentation.show()
        created.load()
        controller.ownerResumed(true)
        val state = controller.state.first { it.phase != SessionPhase.LOADING }
        assertEquals("Scene failed: ${state.error}", SessionPhase.READY, state.phase)
        delay(500)
        while (true) {
            reader.acquireLatestImage()?.use { image ->
                val plane = image.planes[0]
                val buffer = plane.buffer
                val packed = ByteArray(size * size * 4)
                for (y in 0 until size) for (x in 0 until size) {
                    val src = y * plane.rowStride + x * plane.pixelStride
                    for (k in 0 until 4) packed[(y * size + x) * 4 + k] = buffer.get(src + k)
                }
                return Frame(size, size, packed)
            }
            delay(100)
        }
    }

    private fun corners(frame: Frame) = listOf(2 to 2, size - 3 to 2, 2 to size - 3, size - 3 to size - 3)
        .map { (x, y) -> frame.alpha(x, y) }

    private fun assertNoDisplaysLeft() {
        val leftover = context.getSystemService(DisplayManager::class.java).displays.filter { it.name.startsWith(DISPLAY_NAME) }
        assertTrue("Virtual displays remain: $leftover", leftover.isEmpty())
    }

    @Test fun miniCornersAreTransparentWhileTheRingIsDrawn() {
        val frame = render(PreviewSettings(SceneId.IDLE, PreviewFps.THIRTY, SceneVariant.MINI))
        assertNoDisplaysLeft()
        // Kept in the app's own files dir only, for a manual `run-as` pull.
        File(context.filesDir, "o01-mini.png").outputStream().use { frame.bitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
        val center = (size * 3 / 10 until size * 7 / 10).flatMap { y -> (size * 3 / 10 until size * 7 / 10).map { x -> frame.alpha(x, y) } }
        val drawn = center.count { it > 0 }
        Log.i("CielMiniTest", "mini corners=${corners(frame)} centerNonZero=$drawn/${center.size} centerMax=${center.max()}")
        assertEquals(listOf(0, 0, 0, 0), corners(frame))
        assertTrue("Center looks empty: $drawn/${center.size}", drawn > center.size / 10)
    }

    @Test fun fullControlKeepsOpaqueCorners() {
        val frame = render(PreviewSettings(SceneId.IDLE, PreviewFps.THIRTY, SceneVariant.FULL))
        assertNoDisplaysLeft()
        Log.i("CielMiniTest", "full corners=${corners(frame)}")
        assertEquals(listOf(255, 255, 255, 255), corners(frame))
    }

    private companion object { const val DISPLAY_NAME = "CielMiniTest" }
}
