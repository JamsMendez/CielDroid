package com.jamsmendez.cieldroid.data

import com.jamsmendez.cieldroid.domain.ActiveMode
import com.jamsmendez.cieldroid.domain.OverlayPosition
import com.jamsmendez.cieldroid.domain.OverlaySettings
import com.jamsmendez.cieldroid.domain.PreviewFps
import com.jamsmendez.cieldroid.domain.PreviewSettings
import com.jamsmendez.cieldroid.domain.SceneId
import kotlinx.coroutines.flow.StateFlow

data class SettingsState(
    /** Shared by the preview, the wallpaper engine and the mini overlay. */
    val settings: PreviewSettings = PreviewSettings(),
    val mode: ActiveMode = ActiveMode.DEFAULT,
    val overlay: OverlaySettings = OverlaySettings(),
    val loaded: Boolean = false,
    val error: Throwable? = null,
)

interface SettingsRepository {
    val state: StateFlow<SettingsState>
    suspend fun setScene(scene: SceneId)
    suspend fun setFps(fps: PreviewFps)
    suspend fun setActiveMode(mode: ActiveMode)
    suspend fun setOverlayMoveMode(enabled: Boolean)
    /** Writes x and y in one atomic edit. */
    suspend fun setOverlayPosition(position: OverlayPosition)
}
