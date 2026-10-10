package com.jamsmendez.cieldroid.scenes

import com.jamsmendez.cieldroid.domain.*
import java.io.File
import org.junit.Assert.*
import org.junit.Test

/** O01: the MINI variant and its separate transparency adaptation; originals stay untouched. */
class SceneVariantTest {
    private val origin = ScenePolicy.ORIGIN
    private val link = "<link rel=\"stylesheet\" href=\"/assets/adaptations/mini-transparent.css\" />"
    private fun original(scene: SceneId) = File("src/main/assets/scenes/${scene.assetId}/index.html").readText()

    @Test fun fullEntryIsUnchangedAndMiniAppendsTheVariant() {
        SceneId.entries.forEach { scene ->
            PreviewFps.entries.forEach { fps ->
                val base = "$origin/assets/scenes/${scene.assetId}/index.html?fps=${fps.value}"
                val full = PreviewSettings(scene, fps)
                val mini = full.copy(variant = SceneVariant.MINI)
                assertEquals(base, ScenePolicy.entry(full))
                assertEquals("$base&variant=mini", ScenePolicy.entry(mini))
                assertTrue(ScenePolicy.navigation(ScenePolicy.entry(mini), mini))
                assertEquals("scenes/${scene.assetId}/index.html", ScenePolicy.resource(ScenePolicy.entry(mini)))
                assertFalse(ScenePolicy.navigation(ScenePolicy.entry(mini), full))
                assertFalse(ScenePolicy.navigation(ScenePolicy.entry(full), mini))
            }
        }
        assertEquals(SceneVariant.FULL, PreviewSettings().variant)
        assertEquals(SceneVariant.FULL, PreviewSettings.WALLPAPER_DEFAULT.variant)
    }

    @Test fun entrypointsAcceptExactlyFourQueryForms() {
        val index = "$origin/assets/scenes/idle/index.html"
        listOf("fps=30", "fps=60", "fps=30&variant=mini", "fps=60&variant=mini").forEach {
            assertEquals(it, "scenes/idle/index.html", ScenePolicy.resource("$index?$it"))
        }
        listOf(
            "variant=mini&fps=30", "fps=30&variant=full", "variant=mini", "fps=30&variant=mini&x=1",
            "fps=30&variant=MINI", "fps=30&variant=mini&", "fps=30&&variant=mini", "fps=45&variant=mini",
            "fps=30&variant=mini#x", "fps=30#variant=mini", "fps=30&variant=mini&variant=mini",
        ).forEach { assertNull(it, ScenePolicy.resource("$index?$it")) }
        assertNull(ScenePolicy.resource("$origin/assets/scenes/idle/styles.css?fps=30&variant=mini"))
        assertNull(ScenePolicy.resource("$origin/assets/${SceneAdaptations.MINI_TRANSPARENT}?fps=30"))
    }

    @Test fun adaptationIsServedButKeptOutOfTheOriginalInventory() {
        val path = SceneAdaptations.MINI_TRANSPARENT
        assertEquals(setOf(path), SceneAdaptations.paths)
        assertEquals(56, SceneAssets.paths.size)
        assertFalse(path in SceneAssets.paths)
        assertEquals(SceneAssets.paths + path, SceneAdaptations.served)
        assertEquals(path, ScenePolicy.resource("$origin/assets/$path"))
        assertFalse(ScenePolicy.navigation("$origin/assets/$path", PreviewSettings(variant = SceneVariant.MINI)))
        val css = File("src/main/assets/$path").readText()
        assertTrue(css.contains("html.scene-mini {\n  background: transparent !important;\n}"))
        assertFalse(css.contains("url("))
        assertFalse(css.contains("@import"))
    }

    @Test fun miniHtmlGainsOnlyTheAdaptationLinkBesideTheCspRewrite() {
        SceneId.entries.forEach {
            val html = original(it)
            val full = html.replace(SceneHtml.ORIGINAL_CSP, SceneHtml.LOCAL_CSP)
            assertEquals(full, SceneHtml.serve(html))
            assertEquals(full, SceneHtml.serve(html, SceneVariant.FULL))
            val mini = SceneHtml.serve(html, SceneVariant.MINI)
            assertEquals(full.replace("</head>", "$link\n</head>"), mini)
            assertEquals(1, Regex(Regex.escape(link)).findAll(mini).count())
            assertTrue(mini.indexOf(link) > mini.indexOf("href=\"styles.css\""))
        }
        try {
            SceneHtml.serve(SceneHtml.ORIGINAL_CSP + "</head></head>", SceneVariant.MINI)
            fail("Expected fail closed")
        } catch (_: IllegalArgumentException) { }
    }
}
