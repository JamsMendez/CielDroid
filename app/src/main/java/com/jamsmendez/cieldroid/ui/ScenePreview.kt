package com.jamsmendez.cieldroid.ui

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.jamsmendez.cieldroid.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jamsmendez.cieldroid.domain.PreviewSettings
import com.jamsmendez.cieldroid.scenes.*

/** Caller keys this composition by immutable scene/FPS/retry generation. */
@Composable
fun ScenePreview(settings: PreviewSettings, generation: Long, manuallyPaused: Boolean) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    val owner = remember {
        var runtime: WebViewSceneRuntime? = null
        val controller = PreviewController(
            generation, settings, scope,
            { runtime?.command(it) }, { runtime?.visible(it) }, { runtime?.release() },
        )
        runtime = WebViewSceneRuntime(context, generation, settings, controller::event)
        controller to runtime
    }
    val controller = owner.first
    val runtime = owner.second
    val session by controller.state.collectAsState()
    var inViewport by remember { mutableStateOf(false) }
    var resumed by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }

    DisposableEffect(lifecycle, controller) {
        val observer = LifecycleEventObserver { _, _ ->
            resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            // Immediate pause and resume, without waiting for recomposition: a pause/resume
            // pair coalesced before the next frame would leave the effect keys unchanged.
            controller.ownerResumed(resumed && inViewport)
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            controller.dispose()
        }
    }
    LaunchedEffect(resumed, inViewport, manuallyPaused) {
        val visible = resumed && inViewport
        // Apply pauses before resumes so no input change transiently resumes the scene.
        if (!visible) controller.ownerResumed(false)
        controller.manualPause(manuallyPaused)
        controller.ownerResumed(visible)
    }
    val viewportDescription = stringResource(R.string.scene_viewport_description, settings.scene.assetId, settings.fps.value)
    val viewportState = stringResource(if (session.ownerResumed) R.string.scene_state_running else R.string.scene_state_paused)
    Column {
        Text(
            when (session.phase) {
                SessionPhase.LOADING -> stringResource(R.string.scene_loading)
                SessionPhase.READY -> stringResource(R.string.scene_ready)
                SessionPhase.ERROR -> stringResource(R.string.scene_failed, session.error.orEmpty())
                SessionPhase.DISPOSED -> stringResource(R.string.scene_released)
            },
            modifier = Modifier.testTag("preview-status").semantics { liveRegion = LiveRegionMode.Polite },
        )
        Text(stringResource(R.string.preview_instance, generation), modifier = Modifier.testTag("preview-generation"))
        Box(
            // Semantics live on this wrapper: the interop view replaces the AndroidView's own
            // node in the platform accessibility tree, dropping semantics set on AndroidView.
            modifier = Modifier.fillMaxWidth().height(300.dp).testTag("scene-viewport")
                // Owner state for tests and assistive technology; not a visible control.
                .semantics {
                    contentDescription = viewportDescription
                    stateDescription = viewportState
                }
                .onGloballyPositioned { coordinates ->
                    val bounds = coordinates.boundsInRoot()
                    val root = coordinates.findRootCoordinates().size
                    inViewport = bounds.width > 0 && bounds.height > 0 &&
                        bounds.bottom > 0 && bounds.top < root.height &&
                        bounds.right > 0 && bounds.left < root.width
                },
        ) {
            AndroidView(
                factory = {
                    // Decorative animation without interactive content: TalkBack announces only
                    // the wrapper description and state, never the upstream DOM.
                    runtime.view.also {
                        it.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                        runtime.load()
                    }
                },
                modifier = Modifier.fillMaxSize(),
                onRelease = { controller.dispose() },
                update = {},
            )
        }
    }
}
