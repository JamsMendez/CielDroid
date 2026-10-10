package com.jamsmendez.cieldroid

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Device smoke test; APK compilation does not execute these assertions. */
@RunWith(AndroidJUnit4::class)
class BootstrapTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun launcherShowsEndUserSectionsWithFriendlySceneNames() {
        compose.onNodeWithText("CielDroid").assertIsDisplayed()
        listOf("Preview", "Processing", "Explorer", "Idle", "Raphael", "30 FPS", "60 FPS", "Pause", "Retry",
            "Mode", "Scene Wallpaper", "Scene Mini").forEach {
            compose.onNodeWithText(it).performScrollTo().assertIsDisplayed()
        }
        // One mode section replaces the separate wallpaper/overlay cards and their selectors.
        listOf("Live wallpaper", "Floating mini scene").forEach { compose.onAllNodesWithText(it).assertCountEquals(0) }
        listOf("wallpaper-content", "overlay-content").forEach { compose.onNodeWithTag(it).assertDoesNotExist() }
    }

    private fun selected(tag: String) =
        compose.onAllNodes(hasTestTag(tag) and isSelected()).fetchSemanticsNodes().isNotEmpty()

    private fun selectMode(tag: String) {
        compose.onNodeWithTag(tag).performScrollTo().performClick()
        compose.waitUntil(20_000) { selected(tag) }
    }

    // Switches the persisted mode and restores it; never sets the wallpaper or starts the overlay.
    @Test fun onlyTheSelectedModeControlsAreShownAndTheModePersists() {
        compose.waitUntil(20_000) { selected("mode-wallpaper") || selected("mode-mini") }
        val original = if (selected("mode-wallpaper")) "mode-wallpaper" else "mode-mini"
        try {
            selectMode("mode-mini")
            compose.onNodeWithTag("mode-wallpaper").assertIsNotSelected()
            compose.onNodeWithTag("overlay-move-mode").performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("wallpaper-set").assertDoesNotExist()
            selectMode("mode-wallpaper")
            compose.onNodeWithTag("mode-mini").assertIsNotSelected()
            compose.onNodeWithTag("wallpaper-set").performScrollTo().assertIsDisplayed()
            listOf("overlay-move-mode", "overlay-start", "overlay-grant", "wallpaper-change")
                .forEach { compose.onNodeWithTag(it).assertDoesNotExist() }
        } finally {
            selectMode(original)
        }
    }

    @Test fun launcherHidesDeveloperTextAndRawSceneIds() {
        compose.waitUntil(20_000) { compose.onAllNodesWithTag("preview-status").fetchSemanticsNodes().isNotEmpty() }
        listOf("Original FULL scene preview", "Device validation pending", "visual validation pending",
            "Preview instance", "Future modes", "Prototype", "not validated").forEach {
            compose.onAllNodesWithText(it, substring = true, ignoreCase = true).assertCountEquals(0)
        }
        listOf("processing", "explorer", "idle", "raphael").forEach {
            compose.onAllNodesWithText(it).assertCountEquals(0)
        }
        compose.onAllNodesWithTag("preview-generation").assertCountEquals(0)
    }
}
