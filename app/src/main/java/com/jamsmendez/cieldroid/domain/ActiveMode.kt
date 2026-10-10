package com.jamsmendez.cieldroid.domain

/** Which home-screen surface the launcher manages; both use the one shared scene/FPS config. */
enum class ActiveMode(val storageId: String) {
    WALLPAPER("wallpaper"), MINI("mini");

    companion object {
        val DEFAULT = WALLPAPER

        /** Unset, unknown or wrongly typed values mean the default. */
        fun parse(value: String?) = entries.firstOrNull { it.storageId == value } ?: DEFAULT
    }
}
