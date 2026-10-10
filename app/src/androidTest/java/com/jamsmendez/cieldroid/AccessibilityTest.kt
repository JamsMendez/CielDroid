package com.jamsmendez.cieldroid

import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
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
        listOf(R.string.app_name, R.string.preview_title, R.string.mode_title)
            .forEach { compose.onNode(hasText(string(it)) and heading).assertExists() }
    }

    private fun modeSelected(tag: String) =
        compose.onAllNodes(hasTestTag(tag) and isSelected()).fetchSemanticsNodes().isNotEmpty()

    private fun selectMode(tag: String) {
        compose.onNodeWithTag(tag).performScrollTo().performClick()
        compose.waitUntil(20_000) { modeSelected(tag) }
    }

    // The switch is not clicked so the device's persisted move mode stays untouched; the mode is restored.
    @Test fun overlayMoveModeIsOneLabelledSwitch() {
        launched()
        val original = if (modeSelected("mode-mini")) "mode-mini" else "mode-wallpaper"
        try {
            selectMode("mode-mini")
            val row = compose.onNodeWithTag("overlay-move-mode").performScrollTo()
            row.assert(isToggleable())
            row.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            row.assert(hasText(string(R.string.overlay_move_mode)))
            row.assert(hasText(string(R.string.overlay_move_mode_explanation)))
        } finally { selectMode(original) }
    }

    @Test fun sceneChipsShareOneSelectableGroup() {
        launched()
        val group = hasParent(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup))
        listOf("Processing", "Explorer", "Idle", "Raphael", "30 FPS", "60 FPS")
            .forEach { compose.onNode(hasText(it) and group).assertExists() }
    }

    @Test fun modeOptionsAreOneSelectableGroupOfRadioButtons() {
        launched()
        val group = hasParent(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup))
        val radio = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
        mapOf("mode-wallpaper" to R.string.mode_wallpaper, "mode-mini" to R.string.mode_mini).forEach { (tag, label) ->
            compose.onNode(hasTestTag(tag) and group and radio and hasText(string(label))).performScrollTo()
                .assertIsDisplayed()
        }
        // Exactly one mode is selected.
        compose.onAllNodes(hasTestTag("mode-wallpaper") and isSelected()).fetchSemanticsNodes().size
            .plus(compose.onAllNodes(hasTestTag("mode-mini") and isSelected()).fetchSemanticsNodes().size)
            .let { assertEquals(1, it) }
    }

    @Test fun previewStatusIsPoliteLiveRegion() {
        launched()
        compose.onNodeWithTag("preview-status")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test fun viewportDescribesSceneAndStateWhileWebContentIsHidden() {
        launched()
        select("Processing")
        select("30 FPS")
        compose.onNodeWithTag("scene-viewport").performScrollTo()
            .assertContentDescriptionEquals(string(R.string.scene_viewport_description, "Processing", 30))
            .assert(SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription, string(R.string.scene_state_running)))
        assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS, webView().importantForAccessibility)
    }
}
