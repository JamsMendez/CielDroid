package com.jamsmendez.cieldroid.ui

import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.jamsmendez.cieldroid.R
import com.jamsmendez.cieldroid.domain.PreviewFps
import com.jamsmendez.cieldroid.domain.PreviewSettings
import com.jamsmendez.cieldroid.domain.SceneId
import com.jamsmendez.cieldroid.wallpaper.SceneWallpaperService

/**
 * Stateless wallpaper settings: one summary row opens a selector whose options write the wallpaper keys
 * only, independent of the preview chips.
 */
@Composable
fun WallpaperSection(
    wallpaper: PreviewSettings,
    enabled: Boolean,
    pickerUnavailable: Boolean,
    onSceneSelected: (SceneId) -> Unit,
    onFpsSelected: (PreviewFps) -> Unit,
    onSetWallpaper: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selecting by rememberSaveable { mutableStateOf(false) }
    SectionCard(R.string.wallpaper_title, modifier) {
        SelectionSummaryRow(wallpaper, enabled, onClick = { selecting = true }, Modifier.testTag("wallpaper-content"))
        Button(onClick = onSetWallpaper, enabled = enabled, modifier = Modifier.testTag("wallpaper-set")) {
            Text(stringResource(R.string.wallpaper_set))
        }
        Text(stringResource(R.string.wallpaper_set_hint), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (pickerUnavailable) {
            Text(stringResource(R.string.wallpaper_picker_unavailable), color = MaterialTheme.colorScheme.error)
        }
    }
    if (selecting && enabled) {
        SceneSelectorDialog(
            tagPrefix = "wallpaper", title = R.string.wallpaper_selector_title,
            sceneDescription = R.string.wallpaper_scene_chip_description,
            fpsDescription = R.string.wallpaper_fps_chip_description,
            settings = wallpaper, onSceneSelected = onSceneSelected, onFpsSelected = onFpsSelected,
            onDismiss = { selecting = false },
        )
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
