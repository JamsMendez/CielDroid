package com.jamsmendez.cieldroid.wallpaper

import org.junit.Assert.*
import org.junit.Test

class TeardownTest {
    @Test fun partialConstructionReleasesOnlyCreatedPartsInReverseOrder() {
        val released = mutableListOf<String>()
        val failures = mutableListOf<String>()
        val boom = IllegalStateException("presentation failed")
        try {
            Teardown.build({ name, _ -> failures += name }) { t ->
                t.add("scope", "s") { released += it }
                t.add("display", "d") { released += it }
                throw boom
            }
            fail("Construction failure must propagate")
        } catch (e: IllegalStateException) { assertSame(boom, e) }
        assertEquals(listOf("d", "s"), released)
        assertTrue(failures.isEmpty())
    }

    @Test fun releaseContinuesPastFailuresAndIsIdempotent() {
        val released = mutableListOf<String>()
        val failures = mutableListOf<String>()
        val teardown = Teardown.build({ name, _ -> failures += name }) { t ->
            t.add("scope", "s") { released += it }
            t.add("display", "d") { error("release failed") }
            t.add("controller", "c") { released += it }
            t
        }
        assertTrue(released.isEmpty())
        teardown.releaseAll()
        teardown.releaseAll()
        assertEquals(listOf("c", "s"), released)
        assertEquals(listOf("display"), failures)
    }

    @Test fun partAddedAfterReleaseIsReleasedImmediatelyAndRejected() {
        val released = mutableListOf<String>()
        val teardown = Teardown { _, _ -> }
        teardown.releaseAll()
        assertThrows(IllegalStateException::class.java) { teardown.add("late", "l") { released += it } }
        assertEquals(listOf("l"), released)
    }
}
