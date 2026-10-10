package com.jamsmendez.cieldroid.scenes

import com.jamsmendez.cieldroid.domain.PreviewSettings
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PreviewControllerTest {
    @Test fun visibilityManualPauseAndStaleCallbacks() = runTest {
        val commands = mutableListOf<SceneCommand>()
        val visible = mutableListOf<Boolean>()
        var releases = 0
        val c = PreviewController(1, PreviewSettings(), backgroundScope, commands::add, visible::add, { releases++ }, 100)
        c.ownerResumed(true)
        c.event(0, SceneEvent.Failure("stale"))
        assertEquals(SessionPhase.LOADING, c.state.value.phase)
        c.event(1, SceneEvent.Console("${ScenePolicy.ORIGIN}/assets/scenes/shared/js/alert-overlay.js", "CIELINUX_ALERT_READY_V1"))
        assertEquals(listOf(SceneCommand.RESUME), commands)
        c.manualPause(true)
        c.ownerResumed(false)
        c.ownerResumed(true)
        assertEquals(listOf(SceneCommand.RESUME, SceneCommand.PAUSE), commands)
        c.manualPause(false)
        assertEquals(SceneCommand.RESUME, commands.last())
        c.dispose()
        c.dispose()
        c.event(1, SceneEvent.OwnerResumed(true))
        assertEquals(SessionPhase.DISPOSED, c.state.value.phase)
        assertEquals(1, releases)
    }

    @Test fun timeoutOnlyWhileResumedLoadingAndCleanup() = runTest {
        val c = PreviewController(2, PreviewSettings(), backgroundScope, {}, {}, {}, 100)
        advanceTimeBy(200)
        assertEquals(SessionPhase.LOADING, c.state.value.phase)
        c.ownerResumed(true)
        advanceTimeBy(50)
        c.ownerResumed(false)
        advanceTimeBy(200)
        assertEquals(SessionPhase.LOADING, c.state.value.phase)
        c.ownerResumed(true)
        advanceTimeBy(101)
        runCurrent()
        assertEquals(SessionPhase.ERROR, c.state.value.phase)
        c.event(2, SceneEvent.Console(ScenePolicy.script(c.state.value.settings.scene), "CIELINUX_SCENE_DRAW_READY_V1 processing"))
        assertEquals(SessionPhase.ERROR, c.state.value.phase)
        c.dispose()
        advanceTimeBy(200)
        assertEquals(SessionPhase.DISPOSED, c.state.value.phase)
    }

    @Test fun readyCancelsTimerAndNewOwnerHasFreshGeneration() = runTest {
        val c = PreviewController(3, PreviewSettings(), backgroundScope, {}, {}, {}, 100)
        c.ownerResumed(true)
        c.event(3, SceneEvent.Console(ScenePolicy.script(c.state.value.settings.scene), "CIELINUX_SCENE_DRAW_READY_V1 processing"))
        c.event(3, SceneEvent.Console("${ScenePolicy.ORIGIN}/assets/scenes/shared/js/alert-overlay.js", "CIELINUX_ALERT_READY_V1"))
        advanceTimeBy(200)
        assertEquals(SessionPhase.READY, c.state.value.phase)
        c.dispose()
        val next = PreviewController(4, c.state.value.settings, backgroundScope, {}, {}, {}, 100)
        next.event(3, SceneEvent.Failure("old runtime"))
        assertEquals(4L, next.state.value.generation)
        assertEquals(SessionPhase.LOADING, next.state.value.phase)
        next.dispose()
    }
}
