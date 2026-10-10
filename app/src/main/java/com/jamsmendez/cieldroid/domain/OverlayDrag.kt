package com.jamsmendez.cieldroid.domain

/** How a drag gesture ended: [dragged] is false for a tap; [persist] is the moved position, if any. */
data class DragEnd(val dragged: Boolean, val persist: OverlayPosition?)

/**
 * Pure drag math for the overlay window: a gesture starts at a raw-screen down point and the window
 * origin; it only becomes a drag beyond [touchSlop] pixels, then moves the origin by the raw delta,
 * clamped to the screen. A tap, or a drag that ends at its origin, has nothing to persist.
 */
class OverlayDrag(private val touchSlop: Int) {
    private var downX = 0f
    private var downY = 0f
    private var origin: OverlayPosition? = null
    private var last: OverlayPosition? = null

    var isDragging = false
        private set

    fun down(rawX: Float, rawY: Float, origin: OverlayPosition) {
        downX = rawX
        downY = rawY
        this.origin = origin
        last = null
        isDragging = false
    }

    /** The clamped new window position once the gesture is beyond touch slop; null while it is still a tap. */
    fun move(rawX: Float, rawY: Float, windowSide: Int, screenWidth: Int, screenHeight: Int): OverlayPosition? {
        val start = origin ?: return null
        val dx = rawX - downX
        val dy = rawY - downY
        if (!isDragging && dx * dx + dy * dy > touchSlop.toFloat() * touchSlop) isDragging = true
        if (!isDragging) return null
        val next = OverlayPlacement.clamp(
            OverlayPosition(start.x + dx.toInt(), start.y + dy.toInt()), windowSide, screenWidth, screenHeight,
        )
        last = next
        return next
    }

    /** Ends the gesture (up or cancel). */
    fun end(): DragEnd {
        val result = DragEnd(isDragging, last?.takeIf { isDragging && it != origin })
        origin = null
        last = null
        isDragging = false
        return result
    }
}
