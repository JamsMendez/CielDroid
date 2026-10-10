package com.jamsmendez.cieldroid.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayPlacementTest {
    @Test fun sizeKeepsTheSceneAtTheRequestedFractionAndCropsTheWindow() {
        // 1080 px short side: scene 0.42 × 1080 = 453 px, window 0.86 × 453 = 389 px.
        assertEquals(OverlaySize(webSide = 453, windowSide = 389), OverlayPlacement.size(1080))
        assertEquals(0.42f, OverlayPlacement.SCENE_FRACTION)
        assertEquals(0.86f, OverlayPlacement.WINDOW_CROP)
        assertEquals(4, OverlayPlacement.EDGE_MARGIN_DP)
        assertEquals(OverlaySize(0, 0), OverlayPlacement.size(0))
    }

    @Test fun defaultPositionIsTheTopEndCornerWithTheMarginInAbsoluteCoordinates() {
        // Former Gravity.TOP|END with x = y = margin becomes x = width - side - margin from the left.
        assertEquals(OverlayPosition(1080 - 389 - 11, 11), OverlayPlacement.defaultPosition(1080, 389, 11))
        assertEquals(OverlayPosition(0, 11), OverlayPlacement.defaultPosition(300, 389, 11))
        // Absolute display coordinates: the default sits below the status bar inset.
        assertEquals(OverlayPosition(680, 105 + 11), OverlayPlacement.defaultPosition(1080, 389, 11, topInset = 105))
    }

    @Test fun clampKeepsTheWholeWindowOnScreen() {
        val clamp = { x: Int, y: Int -> OverlayPlacement.clamp(OverlayPosition(x, y), 389, 1080, 2400) }
        assertEquals(OverlayPosition(100, 200), clamp(100, 200)) // inside
        assertEquals(OverlayPosition(0, 0), clamp(-50, -1)) // negative
        assertEquals(OverlayPosition(1080 - 389, 2400 - 389), clamp(5000, 9000)) // beyond right/bottom
        assertEquals(OverlayPosition(1080 - 389, 0), clamp(1080 - 389, 0)) // exact edges
        // A window larger than the screen pins to the top-left instead of failing.
        assertEquals(OverlayPosition(0, 0), OverlayPlacement.clamp(OverlayPosition(40, 40), 500, 300, 400))
    }
}
