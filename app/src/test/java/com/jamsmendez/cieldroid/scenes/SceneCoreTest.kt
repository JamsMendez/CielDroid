package com.jamsmendez.cieldroid.scenes

import com.jamsmendez.cieldroid.domain.*
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class SceneCoreTest {
    private val origin = ScenePolicy.ORIGIN
    private val defaults = PreviewSettings()
    private fun original(scene: SceneId) = File("src/main/assets/scenes/${scene.assetId}/index.html").readText()
    private fun rejected(block: () -> Unit) {
        try { block(); fail("Expected fail closed") } catch (_: IllegalArgumentException) { }
    }

    @Test fun inventoryAndOriginalEntrypointsMatch() {
        // The CielScenes submodule gitlink pins the bytes; the allowlist must exist in it.
        val scenes = File("src/main/assets/scenes")
        assertTrue("Run git submodule update --init app/src/main/assets/scenes",
            scenes.resolve("shared").isDirectory)
        assertEquals(56, SceneAssets.paths.size)
        SceneAssets.paths.forEach { path ->
            val file = File("src/main/assets/$path")
            assertTrue(path, file.isFile && !java.nio.file.Files.isSymbolicLink(file.toPath()))
        }
        SceneId.entries.forEach {
            assertTrue(original(it).contains(SceneHtml.ORIGINAL_CSP))
            assertTrue(original(it).contains("src=\"js/${it.readyScript}\""))
            val source = File("src/main/assets/scenes/${it.assetId}/js/${it.readyScript}").readText()
            assertTrue(source.contains("CIELINUX_SCENE_DRAW_READY_V1 ${it.assetId}"))
        }
    }

    @Test fun canonicalEntryAndResourceMatrix() {
        SceneId.entries.forEach { scene ->
            PreviewFps.entries.forEach { fps ->
                val settings = PreviewSettings(scene, fps)
                val url = ScenePolicy.entry(settings)
                assertTrue(url, ScenePolicy.navigation(url, settings))
                assertEquals("scenes/${scene.assetId}/index.html", ScenePolicy.resource(url))
            }
        }
        SceneAssets.paths.forEach { path ->
            assertEquals(path, ScenePolicy.resource("$origin/assets/$path"))
            assertFalse(ScenePolicy.navigation("$origin/assets/$path", defaults))
        }
        val entry = ScenePolicy.entry(defaults)
        val denied = listOf(
            entry.replace("https:", "http:"), entry.replace("appassets.androidplatform.net", "evil.test"),
            entry.replace(".net/", ".net:443/"), entry.replace("https://", "https://user@"),
            entry.replace("/processing/", "/processing/../processing/"),
            entry.replace("processing", "%70rocessing"), entry.replace("/index", "%2findex"),
            "$entry#fragment", "$entry&fps=60", entry.replace("fps=30", "fps=030"),
            entry.replace("fps=30", "mini=1"), entry.replace("index.html", "missing.js"),
            entry.replace("/assets/", "//assets/"), "file:///assets/scenes/processing/index.html",
            entry.replace("fps=30", "fps=30&"), entry.replace("https", "HTTPS"),
        )
        denied.forEach { assertNull(it, ScenePolicy.resource(it)); assertFalse(it, ScenePolicy.navigation(it, defaults)) }
        assertNull(ScenePolicy.resource("$origin/assets/scenes/processing/js/main.js?fps=30"))
        assertFalse(ScenePolicy.navigation(entry, PreviewSettings(SceneId.IDLE)))
        assertFalse(ScenePolicy.navigation(entry, PreviewSettings(fps = PreviewFps.SIXTY)))
        assertNull(ScenePolicy.resource("not a URI"))
        try {
            (SceneAssets.paths as MutableSet<String>).add("scenes/unknown.js")
            fail("Inventory must be immutable")
        } catch (_: UnsupportedOperationException) { }
    }

    @Test fun cspTransformPreservesEveryOtherCharacter() {
        SceneId.entries.forEach {
            val html = original(it)
            assertEquals(html.replace(SceneHtml.ORIGINAL_CSP, SceneHtml.LOCAL_CSP), SceneHtml.serve(html))
            assertEquals(html.replace(SceneHtml.ORIGINAL_CSP, ""), SceneHtml.serve(html).replace(SceneHtml.LOCAL_CSP, ""))
        }
        rejected { SceneHtml.serve("<html></html>") }
        rejected { SceneHtml.serve(SceneHtml.ORIGINAL_CSP.repeat(2)) }
        rejected { SceneHtml.serve(SceneHtml.ORIGINAL_CSP.replace("qrc:", "'self'")) }
        rejected { SceneHtml.serve(SceneHtml.ORIGINAL_CSP + "<meta http-equiv='content-security-policy' content='x'>") }
        assertFalse(SceneHtml.LOCAL_CSP.contains("unsafe-"))
        assertTrue(SceneHtml.LOCAL_CSP.contains("connect-src 'none'"))
    }

    private fun draw(s: SceneSession) = SceneEvent.Console(ScenePolicy.script(s.settings.scene), "CIELINUX_SCENE_DRAW_READY_V1 ${s.settings.scene.assetId}")
    private val bridge = SceneEvent.Console("$origin/assets/scenes/shared/js/alert-overlay.js", "CIELINUX_ALERT_READY_V1")

    @Test fun readinessRequiresExactGenerationSourceAndBothMarkers() {
        SceneId.entries.forEach { scene ->
            val s = SceneSession(2, PreviewSettings(scene))
            assertEquals(s, s.reduce(1, draw(s)).state)
            assertEquals(s, s.reduce(2, SceneEvent.Console("evil", draw(s).message)).state)
            assertEquals(s, s.reduce(2, SceneEvent.Console(ScenePolicy.script(scene), "CIELINUX_SCENE_DRAW_READY_V1 unknown")).state)
            val drawing = s.reduce(2, draw(s)).state
            assertTrue(drawing.drawReady)
            assertEquals(SessionPhase.LOADING, drawing.phase)
            val ready = drawing.reduce(2, bridge)
            assertEquals(SessionPhase.READY, ready.state.phase)
            assertEquals(SceneCommand.PAUSE, ready.command)
            assertNull(ready.state.reduce(2, bridge).command)
        }
    }

    @Test fun lifecycleFailuresTimeoutAndRetryAreTerminalPerGeneration() {
        val s = SceneSession(1, defaults)
        val resumed = s.reduce(1, SceneEvent.OwnerResumed(true)).state
        assertEquals(SceneCommand.RESUME, resumed.reduce(1, bridge).command)
        val ready = resumed.reduce(1, bridge).state.reduce(1, draw(s)).state
        assertEquals(SceneCommand.PAUSE, ready.reduce(1, SceneEvent.OwnerResumed(false)).command)
        assertNull(ready.reduce(1, SceneEvent.Timeout).state.error)
        val error = ready.reduce(1, SceneEvent.Failure("renderer")).state
        assertEquals(SessionPhase.ERROR, error.phase)
        assertEquals(error, error.reduce(1, draw(s)).state)
        assertEquals(error, error.reduce(1, bridge).state)
        assertEquals(error, error.reduce(1, SceneEvent.Failure("later")).state)
        val timed = s.reduce(1, SceneEvent.Timeout).state
        assertEquals(SessionPhase.ERROR, timed.phase)
        assertEquals(timed, timed.reduce(1, bridge).state)
        val retry = error.retry(2)
        assertEquals(SessionPhase.LOADING, retry.phase)
        assertFalse(retry.bridgeReady)
        assertEquals(retry, retry.reduce(1, draw(s)).state)
        rejected { error.retry(1) }
        val loadingError = s.reduce(1, SceneEvent.Failure("resource")).state
        assertEquals(SessionPhase.ERROR, loadingError.phase)
        assertEquals(loadingError, loadingError.reduce(1, draw(s)).state)
        val pausedError = error.reduce(1, SceneEvent.OwnerResumed(false))
        assertNull(pausedError.command)
        assertFalse(pausedError.state.retry(2).ownerResumed)
        val disposed = ready.reduce(1, SceneEvent.Dispose).state
        assertEquals(SessionPhase.DISPOSED, disposed.phase)
        assertEquals(disposed, disposed.reduce(1, SceneEvent.Failure("late")).state)
        assertEquals(disposed, disposed.reduce(1, SceneEvent.OwnerResumed(false)).state)
        rejected { disposed.retry(2) }
        assertEquals("window.cielinuxAlertCommand('{\"type\":\"pause\"}');", SceneCommand.PAUSE.javascript())
        assertEquals("window.cielinuxAlertCommand('{\"type\":\"resume\"}');", SceneCommand.RESUME.javascript())
    }
}
