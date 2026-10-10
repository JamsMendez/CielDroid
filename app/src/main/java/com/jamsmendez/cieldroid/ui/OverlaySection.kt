package com.jamsmendez.cieldroid.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jamsmendez.cieldroid.R
import com.jamsmendez.cieldroid.domain.PreviewFps
import com.jamsmendez.cieldroid.domain.PreviewSettings
import com.jamsmendez.cieldroid.domain.SceneId
import com.jamsmendez.cieldroid.overlay.MiniOverlayService

/**
 * Stateless overlay controls; the caller re-checks [granted] on every resume. Scene/FPS chips expose a
 * content description instead of their visible text, like the wallpaper chips, so all chip rows stay distinct.
 */
@Composable
fun OverlaySection(
    content: PreviewSettings,
    contentEnabled: Boolean,
    onSceneSelected: (SceneId) -> Unit,
    onFpsSelected: (PreviewFps) -> Unit,
    granted: Boolean,
    settingsUnavailable: Boolean,
    moveMode: Boolean,
    moveModeEnabled: Boolean,
    onMoveModeChange: (Boolean) -> Unit,
    onGrant: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.overlay_title), style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() })
        Text(stringResource(R.string.overlay_description))
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SceneId.entries.forEach { scene ->
                val description = stringResource(R.string.overlay_scene_chip_description, scene.assetId)
                FilterChip(
                    selected = contentEnabled && content.scene == scene,
                    onClick = { onSceneSelected(scene) }, enabled = contentEnabled,
                    label = { Text(scene.assetId, Modifier.clearAndSetSemantics { }) },
                    modifier = Modifier.fillMaxWidth().testTag("overlay-scene-${scene.assetId}")
                        .semantics { contentDescription = description },
                )
            }
        }
        Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PreviewFps.entries.forEach { fps ->
                val description = stringResource(R.string.overlay_fps_chip_description, fps.value)
                FilterChip(
                    selected = contentEnabled && content.fps == fps,
                    onClick = { onFpsSelected(fps) }, enabled = contentEnabled,
                    label = { Text(stringResource(R.string.fps_label, fps.value), Modifier.clearAndSetSemantics { }) },
                    modifier = Modifier.testTag("overlay-fps-${fps.value}")
                        .semantics { contentDescription = description },
                )
            }
        }
        if (granted) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onStart, modifier = Modifier.testTag("overlay-start")) {
                    Text(stringResource(R.string.overlay_start))
                }
                OutlinedButton(onClick = onStop, modifier = Modifier.testTag("overlay-stop")) {
                    Text(stringResource(R.string.overlay_stop))
                }
            }
        } else {
            Text(stringResource(R.string.overlay_permission_missing), style = MaterialTheme.typography.bodySmall)
            Button(onClick = onGrant, modifier = Modifier.testTag("overlay-grant")) {
                Text(stringResource(R.string.overlay_grant))
            }
        }
        // One toggleable row: the label is the switch's accessible name and the whole row is the touch target.
        Row(
            Modifier.fillMaxWidth().testTag("overlay-move-mode")
                .toggleable(moveMode, enabled = moveModeEnabled, role = Role.Switch, onValueChange = onMoveModeChange),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.overlay_move_mode))
                Text(stringResource(R.string.overlay_move_mode_explanation), style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = moveMode, onCheckedChange = null, enabled = moveModeEnabled)
        }
        if (settingsUnavailable) {
            Text(stringResource(R.string.overlay_settings_unavailable), color = MaterialTheme.colorScheme.error)
        }
    }
}

fun Context.canDrawOverlays(): Boolean = Settings.canDrawOverlays(this)

/** Opens the system screen where the user grants the permission; CielDroid never grants it. */
fun Context.openOverlayPermissionSettings(): Boolean = try {
    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.fromParts("package", packageName, null)))
    true
} catch (_: ActivityNotFoundException) {
    false
}

fun Context.startMiniOverlay() { startService(Intent(this, MiniOverlayService::class.java)) }
fun Context.stopMiniOverlay() { stopService(Intent(this, MiniOverlayService::class.java)) }
