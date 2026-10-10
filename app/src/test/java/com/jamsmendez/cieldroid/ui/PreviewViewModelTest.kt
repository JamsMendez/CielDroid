package com.jamsmendez.cieldroid.ui

import com.jamsmendez.cieldroid.data.*
import com.jamsmendez.cieldroid.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PreviewViewModelTest {
    private class Repository : SettingsRepository {
        override val state = MutableStateFlow(SettingsState())
        var writes = 0
        var fail = false
        override suspend fun setScene(scene: SceneId) {
            writes++
            if (fail) error("disk unavailable")
            state.value = state.value.copy(settings = state.value.settings.copy(scene = scene))
        }
        override suspend fun setFps(fps: PreviewFps) {
            writes++
            if (fail) error("disk unavailable")
            state.value = state.value.copy(settings = state.value.settings.copy(fps = fps))
        }
        override suspend fun setActiveMode(mode: ActiveMode) {
            writes++
            if (fail) error("disk unavailable")
            state.value = state.value.copy(mode = mode)
        }
        override suspend fun setOverlayMoveMode(enabled: Boolean) {
            writes++
            if (fail) error("disk unavailable")
            state.value = state.value.copy(overlay = state.value.overlay.copy(moveMode = enabled))
        }
        override suspend fun setOverlayPosition(position: OverlayPosition) {
            writes++
            if (fail) error("disk unavailable")
            state.value = state.value.copy(overlay = state.value.overlay.copy(position = position))
        }
    }
    @Test fun notLoadedCannotWriteAndFailedWriteIsNotAcknowledged() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = Repository()
            val vm = PreviewViewModel(repo)
            assertEquals(1L, vm.nextGeneration())
            assertEquals(2L, vm.nextGeneration())
            vm.selectScene(SceneId.IDLE)
            runCurrent()
            assertEquals(0, repo.writes)
            repo.state.value = SettingsState(loaded = true)
            repo.fail = true
            vm.selectScene(SceneId.RAPHAEL)
            runCurrent()
            assertEquals(1, repo.writes)
            assertEquals(SceneId.PROCESSING, vm.settings.value.settings.scene)
            assertNotNull(vm.writeError.value)
            repo.fail = false
            vm.selectFps(PreviewFps.SIXTY)
            runCurrent()
            assertEquals(PreviewFps.SIXTY, vm.settings.value.settings.fps)
            assertNull(vm.writeError.value)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun modeSelectionIsGatedAndLeavesTheSharedConfigAlone() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = Repository()
            val vm = PreviewViewModel(repo)
            assertEquals(ActiveMode.WALLPAPER, vm.settings.value.mode)
            vm.selectMode(ActiveMode.MINI)
            runCurrent()
            assertEquals(0, repo.writes)
            repo.state.value = SettingsState(settings = PreviewSettings(SceneId.IDLE, PreviewFps.SIXTY), loaded = true)
            vm.selectMode(ActiveMode.MINI)
            runCurrent()
            assertEquals(1, repo.writes)
            assertEquals(ActiveMode.MINI, vm.settings.value.mode)
            // One shared config: choosing a mode never changes the scene/FPS the services follow.
            assertEquals(PreviewSettings(SceneId.IDLE, PreviewFps.SIXTY), vm.settings.value.settings)
            vm.selectScene(SceneId.RAPHAEL)
            runCurrent()
            assertEquals(ActiveMode.MINI, vm.settings.value.mode)
            assertEquals(PreviewSettings(SceneId.RAPHAEL, PreviewFps.SIXTY), vm.settings.value.settings)
            repo.fail = true
            vm.selectMode(ActiveMode.WALLPAPER)
            runCurrent()
            assertEquals(ActiveMode.MINI, vm.settings.value.mode)
            assertNotNull(vm.writeError.value)
            repo.fail = false
            vm.selectMode(ActiveMode.WALLPAPER)
            runCurrent()
            assertEquals(ActiveMode.WALLPAPER, vm.settings.value.mode)
            assertNull(vm.writeError.value)
            repo.state.value = repo.state.value.copy(error = IllegalStateException("read failed"))
            vm.selectMode(ActiveMode.MINI)
            runCurrent()
            assertEquals(4, repo.writes) // a read error blocks mode writes too
        } finally { Dispatchers.resetMain() }
    }

    // The UI stops the mini overlay from this callback, so it must run only after the mode is persisted.
    @Test fun modeSavedCallbackRunsOnlyAfterASuccessfulWrite() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = Repository()
            val vm = PreviewViewModel(repo)
            var saved = 0
            vm.selectMode(ActiveMode.WALLPAPER) { saved++ }
            runCurrent()
            assertEquals(0, saved) // not loaded: no write, no callback
            repo.state.value = SettingsState(mode = ActiveMode.MINI, loaded = true)
            repo.fail = true
            vm.selectMode(ActiveMode.WALLPAPER) { saved++ }
            runCurrent()
            assertEquals(1, repo.writes)
            assertEquals(ActiveMode.MINI, vm.settings.value.mode)
            assertEquals(0, saved) // a failed write must not stop the mini
            repo.fail = false
            vm.selectMode(ActiveMode.WALLPAPER) { saved++ }
            assertEquals(0, saved) // nothing runs before the write completes
            runCurrent()
            assertEquals(ActiveMode.WALLPAPER, vm.settings.value.mode)
            assertEquals(1, saved)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun overlayMoveModeToggleIsGatedAndFailuresAreReported() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = Repository()
            val vm = PreviewViewModel(repo)
            vm.setOverlayMoveMode(true)
            runCurrent()
            assertEquals(0, repo.writes)
            repo.state.value = SettingsState(loaded = true)
            vm.setOverlayMoveMode(true)
            runCurrent()
            assertTrue(vm.settings.value.overlay.moveMode)
            assertNull(vm.settings.value.overlay.position) // the overlay service owns the position, not the VM
            assertEquals(PreviewSettings(), vm.settings.value.settings)
            repo.fail = true
            vm.setOverlayMoveMode(false)
            runCurrent()
            assertTrue(vm.settings.value.overlay.moveMode)
            assertNotNull(vm.writeError.value)
            repo.fail = false
            vm.setOverlayMoveMode(false)
            runCurrent()
            assertFalse(vm.settings.value.overlay.moveMode)
            assertNull(vm.writeError.value)
            assertEquals(3, repo.writes) // the gated first call never wrote
        } finally { Dispatchers.resetMain() }
    }
}
