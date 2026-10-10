package com.jamsmendez.cieldroid.ui

import androidx.compose.foundation.layout.Column
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
            // Immediate background pause, without waiting for recomposition.
            if (!resumed) controller.ownerResumed(false)
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            controller.dispose()
        }
    }
    LaunchedEffect(resumed, inViewport, manuallyPaused) {
        controller.manualPause(manuallyPaused)
        controller.ownerResumed(resumed && inViewport)
    }
    Column {
        Text(
            when (session.phase) {
                SessionPhase.LOADING -> stringResource(R.string.scene_loading)
                SessionPhase.READY -> stringResource(R.string.scene_ready)
                SessionPhase.ERROR -> stringResource(R.string.scene_failed, session.error.orEmpty())
                SessionPhase.DISPOSED -> stringResource(R.string.scene_released)
            },
            modifier = Modifier.testTag("preview-status"),
        )
        Text(stringResource(R.string.preview_instance, generation), modifier = Modifier.testTag("preview-generation"))
        AndroidView(
            factory = { runtime.view.also { runtime.load() } },
            modifier = Modifier.fillMaxWidth().height(300.dp).testTag("scene-viewport")
                .onGloballyPositioned { coordinates ->
                    val bounds = coordinates.boundsInRoot()
                    val root = coordinates.findRootCoordinates().size
                    inViewport = bounds.width > 0 && bounds.height > 0 &&
                        bounds.bottom > 0 && bounds.top < root.height &&
                        bounds.right > 0 && bounds.left < root.width
                },
            onRelease = { controller.dispose() },
            update = {},
        )
    }
}
