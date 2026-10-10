package com.jamsmendez.cieldroid.wallpaper

import org.junit.Assert.*
import org.junit.Test

class DisplayPlanTest {
    @Test fun createResizeUnchangedAndInvalidSurfaces() {
        val portrait = DisplaySpec(1080, 2400, 400)
        assertEquals(DisplayPlan.Create(portrait), DisplayPlan.of(null, 1080, 2400, 400))
        assertEquals(DisplayPlan.Unchanged, DisplayPlan.of(portrait, 1080, 2400, 400))
        // Rotation swaps the surface size: resize the existing display, never recreate it.
        assertEquals(DisplayPlan.Resize(DisplaySpec(2400, 1080, 400)), DisplayPlan.of(portrait, 2400, 1080, 400))
        assertEquals(DisplayPlan.Resize(DisplaySpec(1080, 2400, 420)), DisplayPlan.of(portrait, 1080, 2400, 420))
        assertEquals(DisplayPlan.Invalid, DisplayPlan.of(null, 0, 2400, 400))
        assertEquals(DisplayPlan.Invalid, DisplayPlan.of(portrait, 1080, -1, 400))
        assertEquals(DisplayPlan.Invalid, DisplayPlan.of(portrait, 1080, 2400, 0))
    }

    @Test fun replacedSurfaceIsSwappedWithoutRecreatingTheGeneration() {
        val portrait = DisplaySpec(1080, 2400, 400)
        assertEquals(DisplayPlan.SwapSurface, DisplayPlan.of(portrait, 1080, 2400, 400, surfaceChanged = true))
        assertEquals(
            DisplayPlan.Resize(DisplaySpec(2400, 1080, 400), swapSurface = true),
            DisplayPlan.of(portrait, 2400, 1080, 400, surfaceChanged = true),
        )
        assertEquals(DisplayPlan.Create(portrait), DisplayPlan.of(null, 1080, 2400, 400, surfaceChanged = true))
        assertEquals(DisplayPlan.Invalid, DisplayPlan.of(portrait, 0, 2400, 400, surfaceChanged = true))
    }
}
