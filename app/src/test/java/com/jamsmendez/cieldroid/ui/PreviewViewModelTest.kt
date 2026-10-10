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
        override suspend fun setWallpaperScene(scene: SceneId) {
            writes++
            if (fail) error("disk unavailable")
            state.value = state.value.copy(wallpaper = state.value.wallpaper.copy(scene = scene))
        }
        override suspend fun setWallpaperFps(fps: PreviewFps) {
            writes++
            if (fail) error("disk unavailable")
            state.value = state.value.copy(wallpaper = state.value.wallpaper.copy(fps = fps))
        }
        override suspend fun setOverlayScene(scene: SceneId) {
            writes++
            if (fail) error("disk unavailable")
            state.value = state.value.copy(overlay = state.value.overlay.copy(content = state.value.overlay.content.copy(scene = scene)))
        }
        override suspend fun setOverlayFps(fps: PreviewFps) {
            writes++
            if (fail) error("disk unavailable")
            state.value = state.value.copy(overlay = state.value.overlay.copy(content = state.value.overlay.content.copy(fps = fps)))
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

    @Test fun wallpaperSelectionIsGatedAndIndependentOfPreview() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = Repository()
            val vm = PreviewViewModel(repo)
            vm.selectWallpaperScene(SceneId.RAPHAEL)
            runCurrent()
            assertEquals(0, repo.writes)
            repo.state.value = SettingsState(loaded = true)
            vm.selectWallpaperScene(SceneId.RAPHAEL)
            vm.selectWallpaperFps(PreviewFps.SIXTY)
            runCurrent()
            assertEquals(2, repo.writes)
            assertEquals(PreviewSettings(SceneId.RAPHAEL, PreviewFps.SIXTY), vm.settings.value.wallpaper)
            assertEquals(PreviewSettings(), vm.settings.value.settings)
            repo.fail = true
            vm.selectWallpaperScene(SceneId.EXPLORER)
            runCurrent()
            assertEquals(SceneId.RAPHAEL, vm.settings.value.wallpaper.scene)
            assertNotNull(vm.writeError.value)
            repo.fail = false
            repo.state.value = repo.state.value.copy(error = IllegalStateException("read failed"))
            vm.selectWallpaperFps(PreviewFps.THIRTY)
            runCurrent()
            assertEquals(3, repo.writes) // a read error blocks wallpaper writes too
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

    @Test fun overlaySceneAndFpsSelectionIsGatedAndIndependentOfWallpaper() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = Repository()
            val vm = PreviewViewModel(repo)
            vm.selectOverlayScene(SceneId.RAPHAEL)
            runCurrent()
            assertEquals(0, repo.writes)
            repo.state.value = SettingsState(loaded = true)
            vm.selectOverlayScene(SceneId.RAPHAEL)
            vm.selectOverlayFps(PreviewFps.SIXTY)
            runCurrent()
            assertEquals(2, repo.writes)
            assertEquals(PreviewSettings(SceneId.RAPHAEL, PreviewFps.SIXTY), vm.settings.value.overlay.content)
            assertEquals(PreviewSettings.WALLPAPER_DEFAULT, vm.settings.value.wallpaper)
            assertEquals(PreviewSettings(), vm.settings.value.settings)
            assertFalse(vm.settings.value.overlay.moveMode)
            repo.fail = true
            vm.selectOverlayFps(PreviewFps.THIRTY)
            runCurrent()
            assertEquals(PreviewFps.SIXTY, vm.settings.value.overlay.content.fps)
            assertNotNull(vm.writeError.value)
            repo.fail = false
            vm.selectOverlayScene(SceneId.EXPLORER)
            runCurrent()
            assertEquals(SceneId.EXPLORER, vm.settings.value.overlay.content.scene)
            assertNull(vm.writeError.value)
            repo.state.value = repo.state.value.copy(error = IllegalStateException("read failed"))
            vm.selectOverlayFps(PreviewFps.THIRTY)
            runCurrent()
            assertEquals(4, repo.writes) // a read error blocks overlay writes too
        } finally { Dispatchers.resetMain() }
    }
}
