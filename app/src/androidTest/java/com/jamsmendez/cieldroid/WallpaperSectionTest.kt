package com.jamsmendez.cieldroid

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jamsmendez.cieldroid.domain.ActiveMode
import com.jamsmendez.cieldroid.ui.MiniControls
import com.jamsmendez.cieldroid.ui.ModeSection
import com.jamsmendez.cieldroid.ui.WallpaperControls
import com.jamsmendez.cieldroid.ui.theme.CielDroidTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Stateless mode section in Wallpaper mode: the launcher content is replaced by the section alone, so no
 * settings are written and "Set as wallpaper" only reports a callback; nothing is ever applied.
 */
@RunWith(AndroidJUnit4::class)
class WallpaperSectionTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private val modes = mutableListOf<ActiveMode>()
    private var setClicks = 0

    private fun show(enabled: Boolean = true, pickerUnavailable: Boolean = false) {
        compose.runOnUiThread {
            compose.activity.setContent {
                CielDroidTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
                    ModeSection(
                        mode = ActiveMode.WALLPAPER, enabled = enabled, onModeSelected = { modes += it },
                        wallpaper = { WallpaperControls(enabled, pickerUnavailable, onSetWallpaper = { setClicks++ }) },
                        mini = {
                            MiniControls(
                                granted = true, settingsUnavailable = false, moveMode = false, moveModeEnabled = enabled,
                                onMoveModeChange = { }, onGrant = { }, onStart = { }, onStop = { },
                                wallpaperActive = true, chooserUnavailable = false, onChangeWallpaper = { },
                            )
                        },
                    )
                } }
            }
        }
        compose.waitForIdle()
    }

    @Test fun wallpaperModeShowsOnlyItsControlsAndTheSharedHint() {
        show()
        compose.onNode(hasText("Mode") and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)).assertExists()
        compose.onNodeWithTag("mode-wallpaper").assertIsSelected().assert(hasText("Scene Wallpaper"))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        compose.onNodeWithTag("mode-mini").assertIsNotSelected().assert(hasText("Scene Mini"))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        compose.onNodeWithTag("wallpaper-set").performScrollTo().assertIsDisplayed().assertIsEnabled()
            .assert(hasText("Set as wallpaper"))
        compose.onNodeWithText("Uses the preview's scene and frame rate.").assertExists()
        // The mini controls and the still-active notice belong to Mini mode only.
        listOf("overlay-start", "overlay-stop", "overlay-grant", "overlay-move-mode", "wallpaper-change")
            .forEach { compose.onNodeWithTag(it).assertDoesNotExist() }
        compose.onAllNodesWithText("still active", substring = true).assertCountEquals(0)
        // The per-section scene/FPS selectors are gone: the preview owns the only scene/FPS choice.
        listOf("wallpaper-content", "wallpaper-selector", "wallpaper-scene-idle", "wallpaper-fps-30")
            .forEach { compose.onNodeWithTag(it).assertDoesNotExist() }
    }

    @Test fun modeAndSetButtonReportThroughCallbacks() {
        show()
        compose.onNodeWithTag("wallpaper-set").performScrollTo().performClick()
        compose.onNodeWithTag("mode-mini").performClick()
        compose.runOnIdle {
            assertEquals(1, setClicks)
            assertEquals(listOf(ActiveMode.MINI), modes.toList())
        }
    }

    @Test fun unavailablePickerShowsInlineErrorAndDisabledModesDoNotReport() {
        show(enabled = false, pickerUnavailable = true)
        compose.onNodeWithText("The system live wallpaper picker is unavailable on this device.").assertExists()
        compose.onNodeWithTag("mode-mini").assertIsNotEnabled().performClick()
        compose.onNodeWithTag("wallpaper-set").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(emptyList<ActiveMode>(), modes.toList()) }
    }
}
