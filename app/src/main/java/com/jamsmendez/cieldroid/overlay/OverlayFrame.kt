package com.jamsmendez.cieldroid.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout

/**
 * Overlay window root. It intercepts every touch so the scene WebView never receives input, and reports
 * drags as raw-screen deltas while [onDrag] is set. The window itself only receives touches in move mode
 * (FLAG_NOT_TOUCHABLE is cleared); its children are clipped to the (smaller) window bounds.
 */
class OverlayFrame(context: Context) : FrameLayout(context) {
    /** Called with the raw-screen delta since ACTION_DOWN. */
    var onDrag: ((dx: Int, dy: Int) -> Unit)? = null
    /** Called once when the gesture ends (ACTION_UP or ACTION_CANCEL). */
    var onDragEnd: (() -> Unit)? = null
    var onDragStart: (() -> Unit)? = null

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var dragging = false

    init {
        clipChildren = true
        clipToPadding = true
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean = true

    @SuppressLint("ClickableViewAccessibility") // Not a click target: performClick runs for taps below.
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                dragging = false
                onDragStart?.invoke()
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - downX
                val dy = event.rawY - downY
                if (!dragging && dx * dx + dy * dy > touchSlop * touchSlop) dragging = true
                if (dragging) onDrag?.invoke(dx.toInt(), dy.toInt())
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (event.actionMasked == MotionEvent.ACTION_UP && !dragging) performClick()
                dragging = false
                onDragEnd?.invoke()
            }
        }
        return true
    }

    override fun performClick(): Boolean = super.performClick()
}
