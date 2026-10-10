package com.jamsmendez.cieldroid.domain

import org.junit.Assert.*
import org.junit.Test

class PreviewSettingsTest {
    @Test fun closedModelDefaultsAndExactParsing() {
        assertEquals(setOf("processing", "explorer", "idle", "raphael"), SceneId.entries.map { it.assetId }.toSet())
        assertEquals(setOf(30, 60), PreviewFps.entries.map { it.value }.toSet())
        assertEquals(SceneId.PROCESSING, PreviewSettings().scene)
        assertEquals(PreviewFps.THIRTY, PreviewSettings().fps)
        SceneId.entries.forEach { assertEquals(it, SceneId.parse(it.assetId)) }
        PreviewFps.entries.forEach { assertEquals(it, PreviewFps.parse(it.value)) }
        listOf(null, "", "Processing", "processing ", "../idle", "mini").forEach { assertNull(SceneId.parse(it)) }
        listOf(null, -1, 0, 29, 31, 120).forEach { assertNull(PreviewFps.parse(it)) }
    }
}
