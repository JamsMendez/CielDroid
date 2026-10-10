package com.jamsmendez.cieldroid.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.widget.FrameLayout

/**
 * Overlay window root. It intercepts every touch so the scene WebView never receives input, and forwards
 * the gesture as raw-screen points; the drag math lives in [com.jamsmendez.cieldroid.domain.OverlayDrag].
 * The window itself only receives touches in move mode (FLAG_NOT_TOUCHABLE is cleared); its children are
 * clipped to the (smaller) window bounds.
 */
class OverlayFrame(context: Context) : FrameLayout(context) {
    var onDown: ((rawX: Float, rawY: Float) -> Unit)? = null
    var onMove: ((rawX: Float, rawY: Float) -> Unit)? = null
    /** Called once when the gesture ends (ACTION_UP or ACTION_CANCEL); returns true if it was a drag. */
    var onEnd: (() -> Boolean)? = null

    init {
        clipChildren = true
        clipToPadding = true
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean = true

    @SuppressLint("ClickableViewAccessibility") // Not a click target: performClick runs for taps below.
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> onDown?.invoke(event.rawX, event.rawY)
            MotionEvent.ACTION_MOVE -> onMove?.invoke(event.rawX, event.rawY)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val dragged = onEnd?.invoke() ?: false
                if (event.actionMasked == MotionEvent.ACTION_UP && !dragged) performClick()
            }
        }
        return true
    }

    override fun performClick(): Boolean = super.performClick()
}
