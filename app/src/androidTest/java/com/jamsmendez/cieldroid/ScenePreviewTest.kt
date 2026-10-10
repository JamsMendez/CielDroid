package com.jamsmendez.cieldroid

import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Native readiness/ownership checks, not screenshot or GPU fidelity comparisons. */
@RunWith(AndroidJUnit4::class)
class ScenePreviewTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun ready() {
        compose.waitUntil(20_000) { compose.onAllNodesWithTag("preview-status").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("scene-viewport").performScrollTo()
        compose.waitUntil(20_000) {
            compose.onAllNodes(hasText("Draw and bridge ready — visual validation pending")).fetchSemanticsNodes().isNotEmpty()
        }
    }
    private fun instance() = compose.onNodeWithTag("preview-generation").fetchSemanticsNode()
        .config[androidx.compose.ui.semantics.SemanticsProperties.Text].joinToString { it.text }

    private fun webView(): WebView {
        fun descendants(view: View): List<WebView> = when (view) {
            is WebView -> listOf(view)
            is ViewGroup -> (0 until view.childCount).flatMap { descendants(view.getChildAt(it)) }
            else -> emptyList()
        }
        var result: WebView? = null
        compose.runOnIdle {
            result = descendants(compose.activity.window.decorView).single()
        }
        return checkNotNull(result)
    }

    private fun select(label: String) {
        compose.onNodeWithText(label).performScrollTo().performClick()
        compose.waitUntil(20_000) {
            compose.onAllNodes(hasText(label) and isSelected()).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test fun fourOriginalScenesReachBothReadinessMarkersAtBothRates() {
        compose.waitUntil(20_000) { compose.onAllNodesWithTag("preview-status").fetchSemanticsNodes().isNotEmpty() }
        listOf("processing", "explorer", "idle", "raphael").forEach { scene ->
            listOf("30 FPS", "60 FPS").forEach { fps ->
                select(scene)
                select(fps)
                ready()
                compose.onNodeWithText(scene).performScrollTo().assertIsSelected()
                compose.onNodeWithText(fps).performScrollTo().assertIsSelected()
            }
        }
    }

    @Test fun retryReplacesOwnerAndRecreationRetainsPersistedSettings() {
        compose.waitUntil(20_000) { compose.onAllNodesWithTag("preview-status").fetchSemanticsNodes().isNotEmpty() }
        select("idle")
        select("60 FPS")
        ready()
        val before = instance()
        val originalView = webView()
        compose.onNodeWithText("Pause").performScrollTo().performClick()
        compose.onNodeWithText("Resume").assertIsDisplayed()
        compose.onNodeWithText("Retry").performClick()
        compose.onNodeWithText("Resume").assertIsDisplayed()
        assertNotEquals(before, instance()) // Same ViewModel: retry allocates a fresh generation.
        val retriedView = webView()
        assertNotSame(originalView, retriedView)
        // A fresh WebView starts paused: resume before waiting for both readiness markers.
        compose.onNodeWithText("Resume").performClick()
        ready()
        compose.onNodeWithText("Pause").performScrollTo().performClick()
        compose.activityRule.scenario.recreate()
        compose.waitUntil(20_000) {
            compose.onAllNodesWithTag("preview-status").fetchSemanticsNodes().isNotEmpty()
        }
        // Identity proves replacement without assuming cross-ViewModel generation ordering.
        assertNotSame(retriedView, webView())
        compose.onNodeWithText("idle").performScrollTo().assertIsSelected()
        compose.onNodeWithText("60 FPS").performScrollTo().assertIsSelected()
        compose.onNodeWithText("Resume").performScrollTo().assertIsDisplayed().performClick()
        ready()
        compose.onNodeWithText("Pause").performScrollTo().assertIsDisplayed()
    }
}
