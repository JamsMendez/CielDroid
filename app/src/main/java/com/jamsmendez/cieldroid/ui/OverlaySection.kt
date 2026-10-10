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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.jamsmendez.cieldroid.R
import com.jamsmendez.cieldroid.overlay.MiniOverlayService

/**
 * Stateless Mini mode controls; the caller re-checks [granted] and [wallpaperActive] on every resume.
 * The overlay follows the preview's shared scene/FPS. When CielDroid's live wallpaper is still active, a
 * notice offers the system wallpaper chooser; the app itself never clears the wallpaper.
 */
@Composable
fun MiniControls(
    granted: Boolean,
    settingsUnavailable: Boolean,
    moveMode: Boolean,
    moveModeEnabled: Boolean,
    onMoveModeChange: (Boolean) -> Unit,
    onGrant: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    wallpaperActive: Boolean,
    chooserUnavailable: Boolean,
    onChangeWallpaper: () -> Unit,
) {
    Text(stringResource(R.string.overlay_description), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    if (wallpaperActive) {
        Text(stringResource(R.string.wallpaper_still_active))
        OutlinedButton(onClick = onChangeWallpaper, modifier = Modifier.testTag("wallpaper-change")) {
            Text(stringResource(R.string.wallpaper_change))
        }
        if (chooserUnavailable) {
            Text(stringResource(R.string.wallpaper_chooser_unavailable), color = MaterialTheme.colorScheme.error)
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
