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
    /** Never persisted: preview and wallpaper stay FULL; only the overlay requests MINI. */
    val variant: SceneVariant = SceneVariant.FULL,
) {
    companion object {
        /** The W01 spike's measured Canvas2D scene; WebGL scenes stay opt-in for the wallpaper. */
        val WALLPAPER_DEFAULT = PreviewSettings(SceneId.IDLE, PreviewFps.THIRTY)

        /** The overlay's own default; an unset overlay key never falls back to the wallpaper value. */
        val OVERLAY_DEFAULT = PreviewSettings(SceneId.IDLE, PreviewFps.THIRTY)
    }
}
