package com.jamsmendez.cieldroid.widget

import com.jamsmendez.cieldroid.domain.PreviewFps
import com.jamsmendez.cieldroid.domain.PreviewSettings
import com.jamsmendez.cieldroid.domain.SceneId
import org.junit.Assert.*
import org.junit.Test

class SnapshotMathTest {
    @Test fun targetPreservesAspectAndCapsLongestSide() {
        assertEquals(SnapshotSize(300, 450), SnapshotGeometry.target(100, 150, 3f, maxSide = 512))
        assertEquals(SnapshotSize(512, 256), SnapshotGeometry.target(400, 200, 2.625f))
        assertEquals(SnapshotSize(256, 512), SnapshotGeometry.target(200, 400, 2.625f))
    }

    @Test fun targetFallsBackForMissingOptionsAndNeverReturnsZero() {
        val fallback = SnapshotGeometry.target(0, 0, 2f)
        assertEquals(SnapshotSize(360, 360), fallback)
        val sliver = SnapshotGeometry.target(1000, 1, 3f)
        assertEquals(512, sliver.width)
        assertTrue(sliver.height >= 1)
    }

    @Test fun fitWithinOnlyDownscales() {
        assertEquals(SnapshotSize(400, 400), SnapshotGeometry.fitWithin(400, 400))
        assertEquals(SnapshotSize(512, 288), SnapshotGeometry.fitWithin(1080, 607))
    }

    @Test fun renderDensityWidensSmallViewportsWithinBounds() {
        assertEquals(177, SnapshotGeometry.renderDensityDpi(400, 400, 420))
        assertEquals(120, SnapshotGeometry.renderDensityDpi(512, 100, 420))
        assertEquals(160, SnapshotGeometry.renderDensityDpi(2000, 2000, 160))
    }

    @Test fun rowStridePaddingIsSkippedAndChannelsAreReordered() {
        // 2x2 image, rowStride 12 (8 pixel bytes + 4 padding bytes per row).
        val bytes = byteArrayOf(
            1, 2, 3, 255.toByte(), 4, 5, 6, 128.toByte(), 9, 9, 9, 9,
            7, 8, 9, 0, 10, 11, 12, 255.toByte(), 9, 9, 9, 9,
        )
        val pixels = SnapshotPixels.rgbaToArgb(bytes, 2, 2, rowStride = 12)
        assertArrayEquals(
            intArrayOf(
                (0xFF shl 24) or 0x010203, (0x80 shl 24) or 0x040506,
                0x070809, (0xFF shl 24) or 0x0A0B0C,
            ),
            pixels,
        )
    }

    @Test fun cacheKeyIsSceneAndFpsAndStaleOnlyWhenMissing() {
        val name = SnapshotCache.fileName(PreviewSettings(SceneId.IDLE, PreviewFps.THIRTY))
        assertEquals("widget-snapshot-idle-30.png", name)
        assertNotEquals(name, SnapshotCache.fileName(PreviewSettings(SceneId.IDLE, PreviewFps.SIXTY)))
        assertTrue(SnapshotCache.isStale(name, emptyList()))
        assertTrue(SnapshotCache.isStale(name, listOf("widget-snapshot-raphael-30.png")))
        assertFalse(SnapshotCache.isStale(name, listOf(name, "other.png")))
        assertEquals(
            listOf("widget-snapshot-raphael-30.png"),
            SnapshotCache.obsolete(name, listOf(name, "widget-snapshot-raphael-30.png", "unrelated.png")),
        )
    }
}
