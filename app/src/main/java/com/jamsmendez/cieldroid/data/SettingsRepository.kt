package com.jamsmendez.cieldroid.data

import com.jamsmendez.cieldroid.domain.PreviewFps
import com.jamsmendez.cieldroid.domain.PreviewSettings
import com.jamsmendez.cieldroid.domain.SceneId
import kotlinx.coroutines.flow.StateFlow

data class SettingsState(
    val settings: PreviewSettings = PreviewSettings(),
    val loaded: Boolean = false,
    val error: Throwable? = null,
)

interface SettingsRepository {
    val state: StateFlow<SettingsState>
    suspend fun setScene(scene: SceneId)
    suspend fun setFps(fps: PreviewFps)
}
