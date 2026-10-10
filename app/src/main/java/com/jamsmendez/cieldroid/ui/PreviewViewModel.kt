package com.jamsmendez.cieldroid.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jamsmendez.cieldroid.data.PreferencesSettingsRepository
import com.jamsmendez.cieldroid.data.SettingsRepository
import com.jamsmendez.cieldroid.data.settingsPreferences
import com.jamsmendez.cieldroid.domain.ActiveMode
import com.jamsmendez.cieldroid.domain.PreviewFps
import com.jamsmendez.cieldroid.domain.SceneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PreviewViewModel(createRepository: (CoroutineScope) -> SettingsRepository) : ViewModel() {
    constructor(repository: SettingsRepository) : this({ repository })
    private val repository = createRepository(viewModelScope)
    val settings = repository.state
    private val mutableWriteError = MutableStateFlow<String?>(null)
    val writeError = mutableWriteError.asStateFlow()
    private var generation = 0L
    fun nextGeneration(): Long = ++generation

    fun selectScene(scene: SceneId) = write(PREVIEW_ERROR) { repository.setScene(scene) }
    fun selectFps(fps: PreviewFps) = write(PREVIEW_ERROR) { repository.setFps(fps) }
    fun selectMode(mode: ActiveMode) = write(MODE_ERROR) { repository.setActiveMode(mode) }

    fun setOverlayMoveMode(enabled: Boolean) = write(OVERLAY_ERROR) { repository.setOverlayMoveMode(enabled) }

    private fun write(errorMessage: String, update: suspend () -> Unit) {
        if (!settings.value.loaded || settings.value.error != null) return
        viewModelScope.launch {
            try {
                update()
                mutableWriteError.value = null
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableWriteError.value = errorMessage }
        }
    }

    companion object {
        private const val PREVIEW_ERROR = "Could not save preview settings. Try selecting again."
        private const val OVERLAY_ERROR = "Could not save overlay settings. Try again."
        private const val MODE_ERROR = "Could not save the mode. Try selecting again."

        fun factory(context: Context): ViewModelProvider.Factory {
            val app = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass == PreviewViewModel::class.java)
                    return PreviewViewModel { scope -> PreferencesSettingsRepository(app.settingsPreferences, scope) } as T
                }
            }
        }
    }
}
