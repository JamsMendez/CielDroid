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

    @Test fun launcherShowsTruthfulPreviewAndFutureModeStatus() {
        compose.onNodeWithText("CielDroid").assertIsDisplayed()
        compose.onNodeWithText("Original FULL scene preview").assertIsDisplayed()
        compose.onNodeWithText("Device validation pending. Readiness is not proof of visual fidelity.").assertIsDisplayed()
        listOf("processing", "explorer", "idle", "raphael", "30 FPS", "60 FPS", "Live wallpaper", "Set as wallpaper",
            "Future modes unavailable").forEach {
            compose.onNodeWithText(it).performScrollTo().assertIsDisplayed()
        }
    }
}
