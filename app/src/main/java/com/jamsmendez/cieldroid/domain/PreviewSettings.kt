package com.jamsmendez.cieldroid.domain

enum class PreviewFps(val value: Int) {
    THIRTY(30), SIXTY(60);
    companion object {
        fun parse(value: Int?) = entries.firstOrNull { it.value == value }
    }
}

data class PreviewSettings(
    val scene: SceneId = SceneId.PROCESSING,
    val fps: PreviewFps = PreviewFps.THIRTY,
)
