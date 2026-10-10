package com.jamsmendez.cieldroid

import android.graphics.Bitmap
import android.hardware.display.DisplayManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jamsmendez.cieldroid.domain.PreviewFps
import com.jamsmendez.cieldroid.domain.PreviewSettings
import com.jamsmendez.cieldroid.domain.SceneId
import com.jamsmendez.cieldroid.widget.SceneSnapshotRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Renders one widget snapshot off-screen; never adds a widget or touches the launcher. */
@RunWith(AndroidJUnit4::class)
class SceneSnapshotTest {
    @Test fun idleSnapshotIsNonBlankAndReleasesItsDisplay() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bitmap = runBlocking {
            withContext(Dispatchers.Main) {
                SceneSnapshotRenderer.render(context, PreviewSettings(SceneId.IDLE, PreviewFps.THIRTY), 400, 400, 15_000)
            }
        }
        val leftover = context.getSystemService(DisplayManager::class.java).displays
            .filter { it.name.startsWith(SceneSnapshotRenderer.DISPLAY_NAME) }
        assertTrue("Snapshot displays remain: $leftover", leftover.isEmpty())
        assertNotNull("Snapshot render returned null", bitmap)
        checkNotNull(bitmap)
        assertEquals(400, bitmap.width)
        assertEquals(400, bitmap.height)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val distinct = pixels.toSet().size
        val luminance = pixels.map { ((it shr 16 and 0xFF) * 299 + (it shr 8 and 0xFF) * 587 + (it and 0xFF) * 114) / 1000 }
        val spread = luminance.max() - luminance.min()
        // Kept in the app's own files dir only, for a manual `run-as` pull.
        File(context.filesDir, "w05-test-snapshot.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue("Snapshot looks blank: distinct=$distinct spread=$spread", distinct > 32 && spread > 40)
    }
}
