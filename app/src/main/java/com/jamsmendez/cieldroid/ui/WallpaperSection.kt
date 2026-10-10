package com.jamsmendez.cieldroid.ui

import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.jamsmendez.cieldroid.R
import com.jamsmendez.cieldroid.wallpaper.SceneWallpaperService

/** Stateless Wallpaper mode controls; the engine follows the preview's shared scene/FPS. */
@Composable
fun WallpaperControls(enabled: Boolean, pickerUnavailable: Boolean, onSetWallpaper: () -> Unit) {
    Button(onClick = onSetWallpaper, enabled = enabled, modifier = Modifier.testTag("wallpaper-set")) {
        Text(stringResource(R.string.wallpaper_set))
    }
    Text(stringResource(R.string.wallpaper_set_hint), style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (pickerUnavailable) {
        Text(stringResource(R.string.wallpaper_picker_unavailable), color = MaterialTheme.colorScheme.error)
    }
}

/**
 * Opens the system live wallpaper preview for [SceneWallpaperService]; the user applies it there.
 * Returns false when no activity handles the request.
 */
fun Context.openLiveWallpaperPicker(): Boolean = try {
    startActivity(
        Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(
            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
            ComponentName(this, SceneWallpaperService::class.java),
        ),
    )
    true
} catch (_: ActivityNotFoundException) {
    false
}

/**
 * True when CielDroid's live wallpaper is the current home or lock screen wallpaper. Needs no permission
 * on API 34+: an app always has package visibility of itself, so its own WallpaperInfo is never hidden.
 */
fun Context.isCielWallpaperActive(): Boolean {
    val manager = WallpaperManager.getInstance(this)
    return ownsAnyWallpaper(packageName, listOf(WallpaperManager.FLAG_SYSTEM, WallpaperManager.FLAG_LOCK)
        .map { which -> { manager.getWallpaperInfo(which)?.packageName } })
}

/**
 * True when any reader returns [ownPackage]. A reader that throws a platform [RuntimeException] (for example a
 * SecurityException or a failed wallpaper service call) counts as "not ours", so detection never crashes the UI.
 */
internal fun ownsAnyWallpaper(ownPackage: String, readers: List<() -> String?>): Boolean =
    readers.any { read ->
        try { read() == ownPackage } catch (_: RuntimeException) { false }
    }

/** Opens the system wallpaper chooser; the user picks another wallpaper there. CielDroid never clears it. */
fun Context.openWallpaperChooser(): Boolean = try {
    startActivity(Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), null))
    true
} catch (_: ActivityNotFoundException) {
    false
}
