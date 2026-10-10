package com.jamsmendez.cieldroid

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jamsmendez.cieldroid.domain.PreviewFps
import com.jamsmendez.cieldroid.domain.PreviewSettings
import com.jamsmendez.cieldroid.domain.SceneId
import com.jamsmendez.cieldroid.ui.OverlaySection
import com.jamsmendez.cieldroid.ui.theme.CielDroidTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Stateless overlay section checks: the launcher content is replaced by the section alone, so no settings
 * are written and the overlay service is never started (Start/Stop/Grant are not clicked).
 */
@RunWith(AndroidJUnit4::class)
class OverlaySectionTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private val scenes = mutableListOf<SceneId>()
    private val rates = mutableListOf<PreviewFps>()

    private fun show(content: PreviewSettings, enabled: Boolean) {
        compose.runOnUiThread {
            compose.activity.setContent {
                // A scroll container like the launcher's, so performScrollTo works on small screens.
                CielDroidTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
                    OverlaySection(
                        content = content, contentEnabled = enabled,
                        onSceneSelected = { scenes += it }, onFpsSelected = { rates += it },
                        granted = true, settingsUnavailable = false, moveMode = false, moveModeEnabled = enabled,
                        onMoveModeChange = { }, onGrant = { }, onStart = { }, onStop = { },
                    )
                } }
            }
        }
        compose.waitForIdle()
    }

    private fun description(node: SemanticsNodeInteraction) =
        node.fetchSemanticsNode().config[SemanticsProperties.ContentDescription].single()

    @Test fun chipsExposeTagsDescriptionsAndSelection() {
        show(PreviewSettings(SceneId.EXPLORER, PreviewFps.SIXTY), enabled = true)
        SceneId.entries.forEach { scene ->
            val chip = compose.onNodeWithTag("overlay-scene-${scene.assetId}").performScrollTo()
            assertEquals("Overlay scene ${scene.assetId}", description(chip))
            if (scene == SceneId.EXPLORER) chip.assertIsSelected() else chip.assertIsNotSelected()
            chip.assertIsEnabled()
        }
        PreviewFps.entries.forEach { fps ->
            val chip = compose.onNodeWithTag("overlay-fps-${fps.value}").performScrollTo()
            assertEquals("Overlay ${fps.value} FPS", description(chip))
            if (fps == PreviewFps.SIXTY) chip.assertIsSelected() else chip.assertIsNotSelected()
        }
        // Visible chip text is cleared from semantics, so the labels never collide with the preview chips.
        compose.onAllNodesWithText("explorer").assertCountEquals(0)
        compose.onNodeWithTag("overlay-start").assertExists()
        compose.onNodeWithTag("overlay-move-mode").assertExists()
    }

    @Test fun chipsReportSelectionsThroughCallbacks() {
        show(PreviewSettings.OVERLAY_DEFAULT, enabled = true)
        compose.onNodeWithTag("overlay-scene-raphael").performScrollTo().performClick()
        compose.onNodeWithTag("overlay-fps-60").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(listOf(SceneId.RAPHAEL), scenes.toList())
            assertEquals(listOf(PreviewFps.SIXTY), rates.toList())
        }
    }

    @Test fun disabledChipsShowNoSelectionAndIgnoreClicks() {
        show(PreviewSettings.OVERLAY_DEFAULT, enabled = false)
        compose.onNodeWithTag("overlay-scene-idle").performScrollTo().assertIsNotEnabled().assertIsNotSelected()
        compose.onNodeWithTag("overlay-fps-30").performScrollTo().assertIsNotEnabled().assertIsNotSelected()
        compose.onNodeWithTag("overlay-scene-raphael").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(emptyList<SceneId>(), scenes.toList()) }
    }
}
