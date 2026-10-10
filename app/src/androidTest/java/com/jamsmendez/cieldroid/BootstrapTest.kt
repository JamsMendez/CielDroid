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
            "Live wallpaper", "Set as wallpaper", "Floating mini scene").forEach {
            compose.onNodeWithText(it).performScrollTo().assertIsDisplayed()
        }
        // Wallpaper and overlay show one compact summary row each instead of their own chip sets.
        listOf("wallpaper-content", "overlay-content").forEach {
            compose.onNodeWithTag(it).performScrollTo().assertIsDisplayed().assertHasClickAction()
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
