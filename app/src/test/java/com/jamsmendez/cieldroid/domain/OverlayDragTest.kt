package com.jamsmendez.cieldroid.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayDragTest {
    private val side = 389
    private val width = 1080
    private val height = 2400

    private fun OverlayDrag.moveTo(rawX: Float, rawY: Float) = move(rawX, rawY, side, width, height)

    @Test fun aTapWithinTouchSlopNeverMovesOrPersists() {
        val drag = OverlayDrag(touchSlop = 10)
        drag.down(500f, 500f, OverlayPosition(680, 11))
        assertNull(drag.moveTo(507f, 507f)) // 7² + 7² = 98 ≤ 100: still a tap
        assertNull(drag.moveTo(510f, 500f)) // exactly the slop is not beyond it
        assertFalse(drag.isDragging)
        assertEquals(DragEnd(dragged = false, persist = null), drag.end())
        // A finished gesture ignores stray moves until the next down.
        assertNull(drag.moveTo(900f, 900f))
        assertEquals(DragEnd(dragged = false, persist = null), drag.end())
    }

    @Test fun aDragBeyondSlopMovesFromTheWindowOriginAndPersistsTheFinalPosition() {
        val drag = OverlayDrag(touchSlop = 10)
        drag.down(500f, 500f, OverlayPosition(100, 200))
        assertEquals(OverlayPosition(111, 200), drag.moveTo(511f, 500f))
        assertTrue(drag.isDragging)
        // Once dragging, small deltas keep tracking (the slop only gates the start); fractions truncate.
        assertEquals(OverlayPosition(103, 202), drag.moveTo(503.9f, 502.9f))
        assertEquals(OverlayPosition(70, 450), drag.moveTo(470f, 750f))
        assertEquals(DragEnd(dragged = true, persist = OverlayPosition(70, 450)), drag.end())
        assertFalse(drag.isDragging)
    }

    @Test fun movesAreClampedToTheScreen() {
        val drag = OverlayDrag(touchSlop = 10)
        drag.down(500f, 500f, OverlayPosition(600, 100))
        assertEquals(OverlayPosition(width - side, 0), drag.moveTo(1500f, -400f))
        assertEquals(DragEnd(dragged = true, persist = OverlayPosition(width - side, 0)), drag.end())
    }

    @Test fun aDragThatEndsWhereItStartedDoesNotPersist() {
        // Default corner pressed against the edge: the window never left its origin, so nothing is stored.
        val drag = OverlayDrag(touchSlop = 10)
        drag.down(500f, 500f, OverlayPosition(width - side, 0))
        assertEquals(OverlayPosition(width - side, 0), drag.moveTo(600f, 300f))
        assertEquals(DragEnd(dragged = true, persist = null), drag.end())
    }

    @Test fun aNewDownResetsTheGesture() {
        val drag = OverlayDrag(touchSlop = 10)
        drag.down(500f, 500f, OverlayPosition(100, 100))
        drag.moveTo(600f, 600f)
        drag.down(0f, 0f, OverlayPosition(300, 300))
        assertFalse(drag.isDragging)
        assertNull(drag.moveTo(5f, 5f))
        assertEquals(DragEnd(dragged = false, persist = null), drag.end())
    }
}
