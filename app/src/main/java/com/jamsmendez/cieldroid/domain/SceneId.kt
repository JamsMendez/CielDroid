package com.jamsmendez.cieldroid.domain

enum class SceneId(val assetId: String, val readyScript: String) {
    PROCESSING("processing", "main.js"),
    EXPLORER("explorer", "animate.js"),
    IDLE("idle", "animate.js"),
    RAPHAEL("raphael", "main.js");

    companion object {
        fun parse(value: String?) = entries.firstOrNull { it.assetId == value }
    }
}
