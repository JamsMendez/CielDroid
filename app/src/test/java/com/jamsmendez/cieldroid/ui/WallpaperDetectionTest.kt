package com.jamsmendez.cieldroid.ui

import org.junit.Assert.*
import org.junit.Test

class WallpaperDetectionTest {
    private val own = "com.jamsmendez.cieldroid"

    @Test fun anyReaderReportingTheOwnPackageIsActive() {
        assertTrue(ownsAnyWallpaper(own, listOf({ null }, { own })))
        assertFalse(ownsAnyWallpaper(own, listOf({ null }, { "com.other" })))
    }

    // A platform failure while reading one wallpaper must not crash the UI; it counts as "not ours".
    @Test fun aFailingReaderCountsAsNotActive() {
        assertFalse(ownsAnyWallpaper(own, listOf({ throw SecurityException("denied") }, { null })))
        assertFalse(ownsAnyWallpaper(own, listOf({ throw IllegalStateException("service died") })))
        assertTrue(ownsAnyWallpaper(own, listOf({ throw SecurityException("denied") }, { own })))
    }
}
