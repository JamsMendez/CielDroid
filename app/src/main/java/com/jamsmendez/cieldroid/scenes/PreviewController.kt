package com.jamsmendez.cieldroid.scenes

import com.jamsmendez.cieldroid.domain.PreviewSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Duration.Companion.milliseconds

/** Main-thread owner. Android effects belong only to this immutable generation. */
class PreviewController(
    generation: Long,
    settings: PreviewSettings,
    private val scope: CoroutineScope,
    private val command: (SceneCommand) -> Unit,
    private val visibility: (Boolean) -> Unit,
    private val release: () -> Unit,
    private val timeoutMillis: Long = 15_000,
) {
    private val mutableState = MutableStateFlow(SceneSession(generation, settings))
    val state = mutableState.asStateFlow()
    private var resumed = false
    private var paused = false
    private var visible = false
    private var timer: Job? = null

    fun ownerResumed(value: Boolean) { resumed = value; updateVisibility() }
    fun manualPause(value: Boolean) { paused = value; updateVisibility() }

    private fun updateVisibility() {
        if (state.value.phase == SessionPhase.DISPOSED) return
        val next = resumed && !paused
        // Send the full bridge pause before native onPause; resume native before JS.
        if (next && !visible) visibility(true)
        event(state.value.generation, SceneEvent.OwnerResumed(next))
        if (!next && visible) visibility(false)
        visible = next
        updateTimer()
    }

    fun event(generation: Long, event: SceneEvent) {
        val transition = state.value.reduce(generation, event)
        mutableState.value = transition.state
        transition.command?.let(command)
        updateTimer()
    }

    private fun updateTimer() {
        val loadingVisible = state.value.ownerResumed && state.value.phase == SessionPhase.LOADING
        if (!loadingVisible) { timer?.cancel(); timer = null }
        else if (timer == null) {
            val generation = state.value.generation
            timer = scope.launch {
                delay(timeoutMillis.milliseconds)
                event(generation, SceneEvent.Timeout)
            }
        }
    }

    fun dispose() {
        if (state.value.phase == SessionPhase.DISPOSED) return
        ownerResumed(false)
        event(state.value.generation, SceneEvent.Dispose)
        timer?.cancel()
        timer = null
        release()
    }
}
