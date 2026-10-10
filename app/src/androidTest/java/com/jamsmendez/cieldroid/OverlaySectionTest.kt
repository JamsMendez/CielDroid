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
 * Stateless mode section in Mini mode: the launcher content is replaced by the section alone, so no settings
 * are written, the overlay service is never started and the wallpaper chooser is never opened (callbacks only).
 */
@RunWith(AndroidJUnit4::class)
class OverlaySectionTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private val modes = mutableListOf<ActiveMode>()
    private var changeClicks = 0
    private var startClicks = 0

    private fun show(
        granted: Boolean = true,
        wallpaperActive: Boolean = false,
        chooserUnavailable: Boolean = false,
    ) {
        compose.runOnUiThread {
            compose.activity.setContent {
                CielDroidTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
                    ModeSection(
                        mode = ActiveMode.MINI, enabled = true, onModeSelected = { modes += it },
                        wallpaper = { WallpaperControls(true, false, onSetWallpaper = { }) },
                        mini = {
                            MiniControls(
                                granted = granted, settingsUnavailable = false, moveMode = false, moveModeEnabled = true,
                                onMoveModeChange = { }, onGrant = { }, onStart = { startClicks++ }, onStop = { },
                                wallpaperActive = wallpaperActive, chooserUnavailable = chooserUnavailable,
                                onChangeWallpaper = { changeClicks++ },
                            )
                        },
                    )
                } }
            }
        }
        compose.waitForIdle()
    }

    @Test fun miniModeShowsOnlyItsControlsAndTheSharedHint() {
        show()
        compose.onNodeWithTag("mode-mini").assertIsSelected()
        compose.onNodeWithTag("mode-wallpaper").assertIsNotSelected()
        compose.onNodeWithTag("overlay-start").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("overlay-stop").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("overlay-move-mode").performScrollTo()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
        compose.onNodeWithText("Uses the preview's scene and frame rate.").assertExists()
        compose.onNodeWithTag("wallpaper-set").assertDoesNotExist()
        // No CielDroid wallpaper is active here, so there is no notice.
        compose.onNodeWithTag("wallpaper-change").assertDoesNotExist()
        compose.onAllNodesWithText("still active", substring = true).assertCountEquals(0)
        listOf("overlay-content", "overlay-selector", "overlay-scene-idle", "overlay-fps-30")
            .forEach { compose.onNodeWithTag(it).assertDoesNotExist() }
    }

    @Test fun activeCielWallpaperShowsNoticeAndChangeButton() {
        show(wallpaperActive = true)
        compose.onNodeWithText("Your CielDroid live wallpaper is still active.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("wallpaper-change").performScrollTo().assertIsDisplayed()
            .assert(hasText("Change wallpaper")).performClick()
        compose.onNodeWithTag("overlay-start").performScrollTo().performClick()
        compose.onNodeWithTag("mode-wallpaper").performClick()
        compose.runOnIdle {
            assertEquals(1, changeClicks)
            assertEquals(1, startClicks)
            assertEquals(listOf(ActiveMode.WALLPAPER), modes.toList())
        }
    }

    @Test fun missingPermissionShowsGrantAndUnavailableChooserShowsError() {
        show(granted = false, wallpaperActive = true, chooserUnavailable = true)
        compose.onNodeWithTag("overlay-grant").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("overlay-start").assertDoesNotExist()
        compose.onNodeWithText("The system wallpaper chooser is unavailable on this device.").assertExists()
    }
}
