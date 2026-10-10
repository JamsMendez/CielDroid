package com.jamsmendez.cieldroid.data

import com.jamsmendez.cieldroid.domain.OverlayPosition
import com.jamsmendez.cieldroid.domain.OverlaySettings
import com.jamsmendez.cieldroid.domain.PreviewFps
import com.jamsmendez.cieldroid.domain.PreviewSettings
import com.jamsmendez.cieldroid.domain.SceneId
import kotlinx.coroutines.flow.StateFlow

data class SettingsState(
    val settings: PreviewSettings = PreviewSettings(),
    val wallpaper: PreviewSettings = PreviewSettings.WALLPAPER_DEFAULT,
    val overlay: OverlaySettings = OverlaySettings(),
    val loaded: Boolean = false,
    val error: Throwable? = null,
)

interface SettingsRepository {
    val state: StateFlow<SettingsState>
    suspend fun setScene(scene: SceneId)
    suspend fun setFps(fps: PreviewFps)
    suspend fun setWallpaperScene(scene: SceneId)
    suspend fun setWallpaperFps(fps: PreviewFps)
    suspend fun setOverlayScene(scene: SceneId)
    suspend fun setOverlayFps(fps: PreviewFps)
    suspend fun setOverlayMoveMode(enabled: Boolean)
    /** Writes x and y in one atomic edit. */
    suspend fun setOverlayPosition(position: OverlayPosition)
}
