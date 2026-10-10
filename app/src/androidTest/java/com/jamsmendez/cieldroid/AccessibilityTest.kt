package com.jamsmendez.cieldroid

import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Semantics baseline checks; not an automated accessibility scan or a TalkBack pass. */
@RunWith(AndroidJUnit4::class)
class AccessibilityTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun string(id: Int, vararg args: Any) = compose.activity.getString(id, *args)

    private fun launched() {
        compose.waitUntil(20_000) { compose.onAllNodesWithTag("preview-status").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun select(label: String) {
        compose.onNodeWithText(label).performScrollTo().performClick()
        compose.waitUntil(20_000) {
            compose.onAllNodes(hasText(label) and isSelected()).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun webView(): WebView {
        fun descendants(view: View): List<WebView> = when (view) {
            is WebView -> listOf(view)
            is ViewGroup -> (0 until view.childCount).flatMap { descendants(view.getChildAt(it)) }
            else -> emptyList()
        }
        var result: WebView? = null
        compose.runOnIdle { result = descendants(compose.activity.window.decorView).single() }
        return checkNotNull(result)
    }

    @Test fun sectionTitlesAreHeadings() {
        launched()
        val heading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)
        listOf(R.string.app_name, R.string.preview_title, R.string.scene_label, R.string.wallpaper_title,
            R.string.future_modes)
            .forEach { compose.onNode(hasText(string(it)) and heading).assertExists() }
    }

    @Test fun sceneChipsShareOneSelectableGroup() {
        launched()
        val group = hasParent(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup))
        listOf("processing", "explorer", "idle", "raphael")
            .forEach { compose.onNode(hasText(it) and group).assertExists() }
    }

    @Test fun wallpaperChipsAreGroupedAndDescribedApartFromPreviewChips() {
        launched()
        val group = hasParent(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup))
        listOf("processing", "explorer", "idle", "raphael").forEach {
            compose.onNode(hasTestTag("wallpaper-scene-$it") and group)
                .assertContentDescriptionEquals(string(R.string.wallpaper_scene_chip_description, it))
        }
        listOf(30, 60).forEach {
            compose.onNode(hasTestTag("wallpaper-fps-$it") and group)
                .assertContentDescriptionEquals(string(R.string.wallpaper_fps_chip_description, it))
        }
    }

    @Test fun previewStatusIsPoliteLiveRegion() {
        launched()
        compose.onNodeWithTag("preview-status")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test fun viewportDescribesSceneAndStateWhileWebContentIsHidden() {
        launched()
        select("processing")
        select("30 FPS")
        compose.onNodeWithTag("scene-viewport").performScrollTo()
            .assertContentDescriptionEquals(string(R.string.scene_viewport_description, "processing", 30))
            .assert(SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription, string(R.string.scene_state_running)))
        assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS, webView().importantForAccessibility)
    }
}
