package com.jamsmendez.cieldroid.scenes

import com.jamsmendez.cieldroid.domain.PreviewSettings

enum class SessionPhase { LOADING, READY, ERROR, DISPOSED }
enum class SceneCommand {
    PAUSE, RESUME;
    fun javascript() = "window.cielinuxAlertCommand('{\"type\":\"${name.lowercase()}\"}');"
}

sealed interface SceneEvent {
    data class Console(val source: String, val message: String) : SceneEvent
    data class Failure(val reason: String) : SceneEvent
    data object Timeout : SceneEvent
    data class OwnerResumed(val resumed: Boolean) : SceneEvent
    data object Dispose : SceneEvent
}

data class SessionTransition(val state: SceneSession, val command: SceneCommand? = null)

data class SceneSession(
    val generation: Long,
    val settings: PreviewSettings,
    val phase: SessionPhase = SessionPhase.LOADING,
    val drawReady: Boolean = false,
    val bridgeReady: Boolean = false,
    val ownerResumed: Boolean = false,
    val error: String? = null,
) {
    init { require(generation > 0) }
    /** The adapter supplies the originating instance's generation, never the current one. */
    fun reduce(generation: Long, event: SceneEvent): SessionTransition {
        if (generation != this.generation || phase == SessionPhase.DISPOSED) return SessionTransition(this)
        if (event == SceneEvent.Dispose) return SessionTransition(copy(phase = SessionPhase.DISPOSED))
        if (event is SceneEvent.OwnerResumed) {
            val changed = event.resumed != ownerResumed
            val state = copy(ownerResumed = event.resumed)
            val command = if (changed && bridgeReady && phase != SessionPhase.ERROR) {
                if (event.resumed) SceneCommand.RESUME else SceneCommand.PAUSE
            } else null
            return SessionTransition(state, command)
        }
        if (phase == SessionPhase.ERROR) return SessionTransition(this)
        when (event) {
            is SceneEvent.Failure -> return SessionTransition(copy(phase = SessionPhase.ERROR, error = event.reason))
            SceneEvent.Timeout -> return if (phase == SessionPhase.LOADING) {
                SessionTransition(copy(phase = SessionPhase.ERROR, error = "Readiness timeout"))
            } else SessionTransition(this)
            is SceneEvent.Console -> {
                val draw = event.source == ScenePolicy.script(settings.scene) &&
                    event.message == "CIELINUX_SCENE_DRAW_READY_V1 ${settings.scene.assetId}"
                val bridge = event.source == "${ScenePolicy.ORIGIN}/assets/scenes/shared/js/alert-overlay.js" &&
                    event.message == "CIELINUX_ALERT_READY_V1"
                if (!draw && !bridge) return SessionTransition(this)
                val state = copy(drawReady = drawReady || draw, bridgeReady = bridgeReady || bridge)
                val command = if (bridge && !bridgeReady) {
                    if (ownerResumed) SceneCommand.RESUME else SceneCommand.PAUSE
                } else null
                return SessionTransition(
                    state.copy(phase = if (state.drawReady && state.bridgeReady) SessionPhase.READY else SessionPhase.LOADING),
                    command,
                )
            }
            else -> return SessionTransition(this)
        }
    }
    fun retry(nextGeneration: Long): SceneSession {
        require(nextGeneration > generation && phase != SessionPhase.DISPOSED)
        return SceneSession(nextGeneration, settings, ownerResumed = ownerResumed)
    }
}
