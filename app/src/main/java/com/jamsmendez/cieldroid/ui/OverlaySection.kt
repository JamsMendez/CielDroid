package com.jamsmendez.cieldroid.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jamsmendez.cieldroid.R
import com.jamsmendez.cieldroid.overlay.MiniOverlayService

/** Stateless overlay controls; the caller re-checks [granted] on every resume. */
@Composable
fun OverlaySection(
    granted: Boolean,
    settingsUnavailable: Boolean,
    onGrant: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.overlay_title), style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() })
        Text(stringResource(R.string.overlay_description))
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
