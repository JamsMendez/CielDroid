package com.jamsmendez.cieldroid.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jamsmendez.cieldroid.R
import com.jamsmendez.cieldroid.domain.*

@Composable
fun CielDroidApp() {
    val context = LocalContext.current
    val app = context.applicationContext
    val factory = remember(app) { PreviewViewModel.factory(app) }
    val vm: PreviewViewModel = viewModel(factory = factory)
    val settings by vm.settings.collectAsStateWithLifecycle()
    val writeError by vm.writeError.collectAsStateWithLifecycle()
    var manuallyPaused by rememberSaveable { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var pickerUnavailable by rememberSaveable { mutableStateOf(false) }
    var overlayGranted by remember { mutableStateOf(context.canDrawOverlays()) }
    var overlaySettingsUnavailable by rememberSaveable { mutableStateOf(false) }
    var cielWallpaperActive by remember { mutableStateOf(context.isCielWallpaperActive()) }
    var chooserUnavailable by rememberSaveable { mutableStateOf(false) }
    // The user grants the permission and changes the wallpaper in system screens; re-check on every resume.
    LifecycleResumeEffect(Unit) {
        overlayGranted = context.canDrawOverlays()
        cielWallpaperActive = context.isCielWallpaperActive()
        onPauseOrDispose { }
    }
    val generation = remember(settings.loaded, settings.settings, retry) { vm.nextGeneration() }
    val enabled = settings.loaded && settings.error == null

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AppHeader()
            if (!settings.loaded) Text(stringResource(if (settings.error == null)
                R.string.settings_loading else R.string.settings_read_error))
            writeError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            SectionCard(R.string.preview_title) {
                if (enabled) key(generation) {
                    ScenePreview(settings.settings, generation, manuallyPaused)
                }
                FlowRow(
                    Modifier.selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SceneId.entries.forEach { scene ->
                        FilterChip(
                            selected = enabled && settings.settings.scene == scene,
                            onClick = { vm.selectScene(scene) }, enabled = enabled,
                            label = { Text(stringResource(sceneLabel(scene))) },
                        )
                    }
                }
                Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PreviewFps.entries.forEach { fps ->
                        FilterChip(selected = enabled && settings.settings.fps == fps,
                            onClick = { vm.selectFps(fps) }, enabled = enabled,
                            label = { Text(stringResource(R.string.fps_label, fps.value)) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { manuallyPaused = !manuallyPaused }, enabled = enabled) {
                        Text(stringResource(if (manuallyPaused) R.string.resume else R.string.pause))
                    }
                    OutlinedButton(onClick = { retry++ }, enabled = enabled) { Text(stringResource(R.string.retry)) }
                }
            }
            ModeSection(
                mode = settings.mode, enabled = enabled,
                onModeSelected = { mode ->
                    // Wallpaper mode never leaves the floating mini scene running.
                    if (mode == ActiveMode.WALLPAPER) context.stopMiniOverlay()
                    vm.selectMode(mode)
                },
                wallpaper = {
                    WallpaperControls(
                        enabled = enabled, pickerUnavailable = pickerUnavailable,
                        onSetWallpaper = { pickerUnavailable = !context.openLiveWallpaperPicker() },
                    )
                },
                mini = {
                    MiniControls(
                        granted = overlayGranted, settingsUnavailable = overlaySettingsUnavailable,
                        moveMode = settings.overlay.moveMode, moveModeEnabled = enabled,
                        onMoveModeChange = vm::setOverlayMoveMode,
                        onGrant = { overlaySettingsUnavailable = !context.openOverlayPermissionSettings() },
                        onStart = { if (context.canDrawOverlays()) context.startMiniOverlay() else overlayGranted = false },
                        onStop = { context.stopMiniOverlay() },
                        wallpaperActive = cielWallpaperActive, chooserUnavailable = chooserUnavailable,
                        onChangeWallpaper = { chooserUnavailable = !context.openWallpaperChooser() },
                    )
                },
            )
        }
    }
}

/** Decorative logo beside the app name; the name alone is the heading TalkBack reads. */
@Composable
private fun AppHeader() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Image(painterResource(R.drawable.ciel_logo), contentDescription = null, modifier = Modifier.size(56.dp))
        Column {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.semantics { heading() })
            Text(stringResource(R.string.app_tagline), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
