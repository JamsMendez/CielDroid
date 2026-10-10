package com.jamsmendez.cieldroid.domain

enum class PreviewFps(val value: Int) {
    THIRTY(30), SIXTY(60);
    companion object {
        fun parse(value: Int?) = entries.firstOrNull { it.value == value }
    }
}

/** The one shared scene/FPS config: the preview, the live wallpaper and the mini overlay all follow it. */
data class PreviewSettings(
    val scene: SceneId = SceneId.PROCESSING,
    val fps: PreviewFps = PreviewFps.THIRTY,
    /** Never persisted: preview and wallpaper stay FULL; only the overlay requests MINI. */
    val variant: SceneVariant = SceneVariant.FULL,
)
