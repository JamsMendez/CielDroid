package com.jamsmendez.cieldroid.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
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

    // Wrong preference types are invalid inputs too, not storage failures.
    private fun decode(preferences: Preferences): PreviewSettings {
        val values = preferences.asMap().entries.associate { it.key.name to it.value }
        return PreviewSettings(
            SceneId.parse(values[sceneKey.name] as? String) ?: SceneId.PROCESSING,
            PreviewFps.parse(values[fpsKey.name] as? Int) ?: PreviewFps.THIRTY,
        )
    }

    override val state: StateFlow<SettingsState> = store.data
        .map { SettingsState(decode(it), loaded = true) }
        .catch { emit(SettingsState(error = it)) }
        .stateIn(scope, SharingStarted.Eagerly, SettingsState())

    // edit is atomic, preserves unrelated keys and propagates IO failures to the caller.
    override suspend fun setScene(scene: SceneId) { store.edit { it[sceneKey] = scene.assetId } }
    override suspend fun setFps(fps: PreviewFps) { store.edit { it[fpsKey] = fps.value } }
}
