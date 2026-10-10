package com.jamsmendez.cieldroid.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.jamsmendez.cieldroid.domain.OverlayPosition
import com.jamsmendez.cieldroid.domain.OverlaySettings
import com.jamsmendez.cieldroid.domain.PreviewFps
import com.jamsmendez.cieldroid.domain.PreviewSettings
import com.jamsmendez.cieldroid.domain.SceneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

/** The caller owns the store and scope; share one DataStore per file, not a global renderer. */
class PreferencesSettingsRepository(
    private val store: DataStore<Preferences>,
    scope: CoroutineScope,
) : SettingsRepository {
    private val sceneKey = stringPreferencesKey("preview_scene")
    private val fpsKey = intPreferencesKey("preview_fps")
    private val wallpaperSceneKey = stringPreferencesKey("wallpaper_scene")
    private val wallpaperFpsKey = intPreferencesKey("wallpaper_fps")
    private val overlayMoveModeKey = booleanPreferencesKey("overlay_move_mode")
    private val overlayXKey = intPreferencesKey("overlay_x")
    private val overlayYKey = intPreferencesKey("overlay_y")

    // Wrong preference types are invalid inputs too, not storage failures.
    private fun decode(
        values: Map<String, Any>, scene: Preferences.Key<String>, fps: Preferences.Key<Int>, default: PreviewSettings,
    ) = PreviewSettings(
        SceneId.parse(values[scene.name] as? String) ?: default.scene,
        PreviewFps.parse(values[fps.name] as? Int) ?: default.fps,
    )

    // A position needs both non-negative coordinates; anything else means the default corner.
    private fun decodeOverlay(values: Map<String, Any>): OverlaySettings {
        val x = values[overlayXKey.name] as? Int
        val y = values[overlayYKey.name] as? Int
        return OverlaySettings(
            moveMode = values[overlayMoveModeKey.name] as? Boolean ?: false,
            position = if (x != null && y != null && x >= 0 && y >= 0) OverlayPosition(x, y) else null,
        )
    }

    private fun decode(preferences: Preferences): SettingsState {
        val values = preferences.asMap().entries.associate { it.key.name to it.value }
        return SettingsState(
            decode(values, sceneKey, fpsKey, PreviewSettings()),
            decode(values, wallpaperSceneKey, wallpaperFpsKey, PreviewSettings.WALLPAPER_DEFAULT),
            decodeOverlay(values),
            loaded = true,
        )
    }

    override val state: StateFlow<SettingsState> = store.data
        .map(::decode)
        .catch { emit(SettingsState(error = it)) }
        .stateIn(scope, SharingStarted.Eagerly, SettingsState())

    // edit is atomic, preserves unrelated keys and propagates IO failures to the caller.
    override suspend fun setScene(scene: SceneId) { store.edit { it[sceneKey] = scene.assetId } }
    override suspend fun setFps(fps: PreviewFps) { store.edit { it[fpsKey] = fps.value } }
    override suspend fun setWallpaperScene(scene: SceneId) { store.edit { it[wallpaperSceneKey] = scene.assetId } }
    override suspend fun setWallpaperFps(fps: PreviewFps) { store.edit { it[wallpaperFpsKey] = fps.value } }
    override suspend fun setOverlayMoveMode(enabled: Boolean) { store.edit { it[overlayMoveModeKey] = enabled } }
    override suspend fun setOverlayPosition(position: OverlayPosition) {
        store.edit {
            it[overlayXKey] = position.x
            it[overlayYKey] = position.y
        }
    }
}
