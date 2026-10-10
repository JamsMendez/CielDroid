package com.jamsmendez.cieldroid.ui

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jamsmendez.cieldroid.data.PreferencesSettingsRepository
import com.jamsmendez.cieldroid.data.SettingsRepository
import com.jamsmendez.cieldroid.domain.PreviewFps
import com.jamsmendez.cieldroid.domain.SceneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Application-owned singleton per preferences file; observation is ViewModel-owned.
private val Context.previewPreferences by preferencesDataStore(name = "preview")

class PreviewViewModel(createRepository: (CoroutineScope) -> SettingsRepository) : ViewModel() {
    constructor(repository: SettingsRepository) : this({ repository })
    private val repository = createRepository(viewModelScope)
    val settings = repository.state
    private val mutableWriteError = MutableStateFlow<String?>(null)
    val writeError = mutableWriteError.asStateFlow()
    private var generation = 0L
    fun nextGeneration(): Long = ++generation

    fun selectScene(scene: SceneId) = write { repository.setScene(scene) }
    fun selectFps(fps: PreviewFps) = write { repository.setFps(fps) }

    private fun write(update: suspend () -> Unit) {
        if (!settings.value.loaded || settings.value.error != null) return
        viewModelScope.launch {
            try {
                update()
                mutableWriteError.value = null
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableWriteError.value = "Could not save preview settings. Try selecting again." }
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val app = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass == PreviewViewModel::class.java)
                    return PreviewViewModel { scope -> PreferencesSettingsRepository(app.previewPreferences, scope) } as T
                }
            }
        }
    }
}
