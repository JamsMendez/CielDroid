package com.jamsmendez.cieldroid.domain

/** Window top-left in pixels from the display's top-left (Gravity.TOP or Gravity.START). */
data class OverlayPosition(val x: Int, val y: Int)

data class OverlaySettings(
    val moveMode: Boolean = true,
    /** Null means the default top-end corner. */
    val position: OverlayPosition? = null,
    /** Scene and FPS, independent of the wallpaper; the service always renders them as MINI. */
    val content: PreviewSettings = PreviewSettings.OVERLAY_DEFAULT,
)

data class OverlaySize(val webSide: Int, val windowSide: Int)

object OverlayPlacement {
    /** Scene (WebView) side as a fraction of the screen short side: 30% smaller than the first 0.6 prototype. */
    const val SCENE_FRACTION = 0.42f

    /**
     * Window side as a fraction of the scene side. The mini ring disc ends at 0.40 of the scene side and its
     * fade at 0.48, so 0.95 keeps content up to radius ~0.475 (outer glow fades before the edge) and crops the rest.
     */
    const val WINDOW_CROP = 0.95f

    /** Default distance from the top and end screen edges. */
    const val EDGE_MARGIN_DP = 4

    fun size(shortSide: Int): OverlaySize {
        val webSide = (shortSide * SCENE_FRACTION).toInt()
        return OverlaySize(webSide, (webSide * WINDOW_CROP).toInt())
    }

    /** The top-end corner expressed as an absolute top-left position. */
    fun defaultPosition(screenWidth: Int, windowSide: Int, margin: Int, topInset: Int = 0): OverlayPosition =
        OverlayPosition((screenWidth - windowSide - margin).coerceAtLeast(0), topInset + margin)

    /** Keeps the whole window on screen; a window larger than the screen pins to the top-left. */
    fun clamp(position: OverlayPosition, windowSide: Int, screenWidth: Int, screenHeight: Int): OverlayPosition =
        OverlayPosition(
            position.x.coerceIn(0, (screenWidth - windowSide).coerceAtLeast(0)),
            position.y.coerceIn(0, (screenHeight - windowSide).coerceAtLeast(0)),
        )
}
