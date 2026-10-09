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
}
