package com.jamsmendez.cieldroid.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerationSlotTest {
    private class Part(val name: String)

    @Test fun installedGenerationIsReleasedExactlyOnceByItsOwnFailure() {
        val released = mutableListOf<String>()
        val slot = GenerationSlot<Part> { released += it.name }
        val one = Part("one")
        slot.begin(1)
        assertTrue(slot.install(1, one))
        assertSame(one, slot.current)
        assertEquals(1L, slot.currentId)
        assertFalse(slot.fail(2)) // a stale or unknown id is ignored
        assertTrue(slot.fail(1))
        assertFalse(slot.fail(1)) // a second error callback is a no-op
        assertFalse(slot.clear())
        assertNull(slot.current)
        assertEquals(listOf("one"), released)
    }

    @Test fun anErrorArrivingDuringReleaseDoesNotReleaseAgain() {
        val released = mutableListOf<String>()
        lateinit var slot: GenerationSlot<Part>
        var nestedFail: Boolean? = null
        var nestedClear: Boolean? = null
        slot = GenerationSlot { part ->
            released += part.name
            // Disposing the controller can emit its ERROR phase synchronously while releasing.
            nestedFail = slot.fail(1)
            nestedClear = slot.clear()
        }
        slot.begin(1)
        slot.install(1, Part("one"))
        assertTrue(slot.clear())
        assertEquals(false, nestedFail)
        assertEquals(false, nestedClear)
        assertEquals(listOf("one"), released)
    }

    @Test fun aFailureWhileBuildingReleasesTheGenerationAsSoonAsItIsInstalled() {
        val released = mutableListOf<String>()
        val slot = GenerationSlot<Part> { released += it.name }
        slot.begin(1)
        assertTrue(slot.fail(1)) // e.g. a synchronous load error before the generation is installed
        assertFalse(slot.install(1, Part("one")))
        assertNull(slot.current)
        assertEquals(listOf("one"), released)
        // The next generation starts clean.
        slot.begin(2)
        assertTrue(slot.install(2, Part("two")))
        assertEquals(2L, slot.currentId)
        assertEquals(listOf("one"), released)
    }

    @Test fun beginReleasesThePreviousGenerationAndAbortForgetsAFailedBuild() {
        val released = mutableListOf<String>()
        val slot = GenerationSlot<Part> { released += it.name }
        slot.begin(1)
        slot.install(1, Part("one"))
        slot.begin(2)
        assertEquals(listOf("one"), released)
        assertNull(slot.current)
        assertTrue(slot.fail(2))
        slot.abort(2) // the build threw: its own teardown already released the parts
        assertFalse(slot.fail(2))
        slot.begin(3)
        assertTrue(slot.install(3, Part("three")))
        assertFalse(slot.install(4, Part("stray"))) // an install for an id that is not being built is rejected
        assertEquals(listOf("one", "stray"), released)
        assertEquals(3L, slot.currentId)
    }
}
