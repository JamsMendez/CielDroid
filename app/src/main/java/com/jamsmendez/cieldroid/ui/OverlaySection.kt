package com.jamsmendez.cieldroid.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.jamsmendez.cieldroid.R
import com.jamsmendez.cieldroid.domain.PreviewFps
import com.jamsmendez.cieldroid.domain.PreviewSettings
import com.jamsmendez.cieldroid.domain.SceneId
import com.jamsmendez.cieldroid.overlay.MiniOverlayService

/**
 * Stateless overlay controls; the caller re-checks [granted] on every resume. Scene/FPS live in a selector
 * opened from one summary row, like the wallpaper section, so only the preview shows a visible chip set.
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
    var selecting by rememberSaveable { mutableStateOf(false) }
    SectionCard(R.string.overlay_title, modifier) {
        Text(stringResource(R.string.overlay_description), color = MaterialTheme.colorScheme.onSurfaceVariant)
        SelectionSummaryRow(content, contentEnabled, onClick = { selecting = true }, Modifier.testTag("overlay-content"))
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
            Text(stringResource(R.string.overlay_permission_missing), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text(stringResource(R.string.overlay_move_mode_explanation), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = moveMode, onCheckedChange = null, enabled = moveModeEnabled)
        }
        if (settingsUnavailable) {
            Text(stringResource(R.string.overlay_settings_unavailable), color = MaterialTheme.colorScheme.error)
        }
    }
    if (selecting && contentEnabled) {
        SceneSelectorDialog(
            tagPrefix = "overlay", title = R.string.overlay_selector_title,
            sceneDescription = R.string.overlay_scene_chip_description,
            fpsDescription = R.string.overlay_fps_chip_description,
            settings = content, onSceneSelected = onSceneSelected, onFpsSelected = onFpsSelected,
            onDismiss = { selecting = false },
        )
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
