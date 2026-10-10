package com.jamsmendez.cieldroid

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Wallpaper settings section checks. "Set as wallpaper" is never clicked: the intent hand-off to the
 * system picker is not asserted here (no espresso-intents dependency), so nothing is ever applied.
 */
@RunWith(AndroidJUnit4::class)
class WallpaperSectionTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private val scenes = listOf("processing", "explorer", "idle", "raphael")

    private fun launched() {
        compose.waitUntil(20_000) { compose.onAllNodesWithTag("preview-status").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun selectedTag(tags: List<String>) =
        tags.single { compose.onAllNodes(hasTestTag(it) and isSelected()).fetchSemanticsNodes().isNotEmpty() }

    private fun selectTag(tag: String) {
        compose.onNodeWithTag(tag).performScrollTo().performClick()
        compose.waitUntil(20_000) {
            compose.onAllNodes(hasTestTag(tag) and isSelected()).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun previewScene() = scenes.single {
        compose.onAllNodes(hasText(it) and isSelected()).fetchSemanticsNodes().isNotEmpty()
    }

    @Test fun sectionHasHeadingAndEnabledSetButton() {
        launched()
        compose.onNode(hasText("Live wallpaper") and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("wallpaper-set").performScrollTo()
            .assertIsDisplayed().assertIsEnabled().assertHasClickAction()
        compose.onNodeWithText("Set as wallpaper").assertExists()
    }

    @Test fun wallpaperChipsSelectIndependentlyOfPreviewChips() {
        launched()
        val sceneTags = scenes.map { "wallpaper-scene-$it" }
        val fpsTags = listOf("wallpaper-fps-30", "wallpaper-fps-60")
        compose.waitUntil(20_000) {
            sceneTags.any { compose.onAllNodes(hasTestTag(it) and isSelected()).fetchSemanticsNodes().isNotEmpty() }
        }
        val originalScene = selectedTag(sceneTags)
        val originalFps = selectedTag(fpsTags)
        val preview = previewScene()
        val target = scenes.first { it != preview && "wallpaper-scene-$it" != originalScene }
        val targetFps = fpsTags.first { it != originalFps }

        try {
            selectTag("wallpaper-scene-$target")
            selectTag(targetFps)

            compose.onNodeWithTag("wallpaper-scene-$target").assertIsSelected()
            compose.onNodeWithTag(targetFps).assertIsSelected()
            compose.onNodeWithTag(originalFps).assertIsNotSelected()
            // The preview keeps its own scene: the wallpaper choice never moves the preview chips.
            compose.onNodeWithText(preview).performScrollTo().assertIsSelected()
            compose.onNodeWithText(target).performScrollTo().assertIsNotSelected()
        } finally {
            // Restore the persisted wallpaper settings even when an assertion above failed.
            selectTag(originalScene)
            selectTag(originalFps)
        }
    }
}
