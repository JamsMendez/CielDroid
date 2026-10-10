package com.jamsmendez.cieldroid

import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jamsmendez.cieldroid.domain.PreviewSettings
import com.jamsmendez.cieldroid.ui.ScenePreview
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Owner-lifecycle auto-pause checks on the viewport state, not pixel or cadence proof. */
@RunWith(AndroidJUnit4::class)
class ScenePreviewLifecycleTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private lateinit var registry: LifecycleRegistry

    @Before fun setUp() {
        compose.runOnUiThread {
            val owner = object : LifecycleOwner {
                override val lifecycle: Lifecycle get() = registry
            }
            registry = LifecycleRegistry(owner).apply { currentState = Lifecycle.State.RESUMED }
            // Replaces the launcher content with one preview owned by a test-controlled lifecycle.
            compose.activity.setContent {
                CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                    ScenePreview(PreviewSettings(), generation = 1, manuallyPaused = false)
                }
            }
        }
        awaitViewport(RUNNING)
    }

    private fun awaitViewport(state: String) {
        val matcher = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, state)
        compose.waitForIdle()
        compose.onNodeWithTag("scene-viewport").assert(matcher)
    }

    private fun move(vararg states: Lifecycle.State) = compose.runOnUiThread {
        states.forEach { registry.currentState = it }
    }

    @Test fun pauseAndResumeBeforeRecompositionLeavesPreviewRunning() {
        move(Lifecycle.State.STARTED, Lifecycle.State.RESUMED)
        awaitViewport(RUNNING)
    }

    @Test fun backgroundPausesAndReturnResumes() {
        move(Lifecycle.State.CREATED)
        awaitViewport(PAUSED)
        move(Lifecycle.State.RESUMED)
        awaitViewport(RUNNING)
    }

    private companion object {
        const val RUNNING = "running"
        const val PAUSED = "paused"
    }
}
