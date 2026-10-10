package com.jamsmendez.cieldroid.widget

import com.jamsmendez.cieldroid.domain.PreviewSettings
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class SnapshotSize(val width: Int, val height: Int)

/** Pure geometry for widget snapshots; no Android types so the decisions are host-testable. */
object SnapshotGeometry {
    const val MAX_SIDE = 512
    const val FALLBACK_DP = 180
    private const val MIN_CSS_SIDE = 360
    private const val MIN_DPI = 120

    /** Widget size in dp (0 when the host reported no options) to snapshot pixels, aspect preserved. */
    fun target(widthDp: Int, heightDp: Int, density: Float, maxSide: Int = MAX_SIDE): SnapshotSize {
        val w = if (widthDp > 0) widthDp else FALLBACK_DP
        val h = if (heightDp > 0) heightDp else FALLBACK_DP
        return fitWithin((w * density).roundToInt(), (h * density).roundToInt(), maxSide)
    }

    /** Downscales only; both sides stay at least 1 px. */
    fun fitWithin(width: Int, height: Int, maxSide: Int = MAX_SIDE): SnapshotSize {
        val w = max(1, width)
        val h = max(1, height)
        val longest = max(w, h)
        if (longest <= maxSide) return SnapshotSize(w, h)
        val scale = maxSide.toDouble() / longest
        return SnapshotSize(max(1, (w * scale).roundToInt()), max(1, (h * scale).roundToInt()))
    }

    /**
     * Density for the private snapshot display: a small bitmap at device density would give the
     * desktop-sized scene a tiny CSS viewport, so lower it until the short side spans ~360 CSS px.
     */
    fun renderDensityDpi(width: Int, height: Int, deviceDpi: Int): Int =
        min(deviceDpi, max(MIN_DPI, 160 * min(width, height) / MIN_CSS_SIDE))
}

/** RGBA_8888 image plane bytes (rows padded to [rowStride]) to packed ARGB pixels. */
object SnapshotPixels {
    fun rgbaToArgb(bytes: ByteArray, width: Int, height: Int, rowStride: Int, pixelStride: Int = 4): IntArray {
        require(width > 0 && height > 0 && pixelStride >= 4 && rowStride >= width * pixelStride)
        require(bytes.size >= rowStride * (height - 1) + width * pixelStride)
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            var offset = y * rowStride
            for (x in 0 until width) {
                val r = bytes[offset].toInt() and 0xFF
                val g = bytes[offset + 1].toInt() and 0xFF
                val b = bytes[offset + 2].toInt() and 0xFF
                val a = bytes[offset + 3].toInt() and 0xFF
                pixels[y * width + x] = (a shl 24) or (r shl 16) or (g shl 8) or b
                offset += pixelStride
            }
        }
        return pixels
    }
}

/** One cached PNG per scene+fps: re-render only when the key changes or its file is missing. */
object SnapshotCache {
    const val PREFIX = "widget-snapshot-"

    fun fileName(settings: PreviewSettings): String = "$PREFIX${settings.scene.assetId}-${settings.fps.value}.png"

    fun isStale(name: String, existing: Collection<String>): Boolean = name !in existing

    /** Snapshots of earlier keys; unrelated files are never selected. */
    fun obsolete(name: String, existing: Collection<String>): List<String> =
        existing.filter { it.startsWith(PREFIX) && it.endsWith(".png") && it != name }
}
