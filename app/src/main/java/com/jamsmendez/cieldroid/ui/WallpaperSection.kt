package com.jamsmendez.cieldroid.ui

import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jamsmendez.cieldroid.R
import com.jamsmendez.cieldroid.domain.PreviewFps
import com.jamsmendez.cieldroid.domain.PreviewSettings
import com.jamsmendez.cieldroid.domain.SceneId
import com.jamsmendez.cieldroid.wallpaper.SceneWallpaperService

/**
 * Stateless wallpaper settings. Chips expose a content description instead of their visible text so
 * they stay distinguishable from the preview chips with the same labels (for TalkBack and tests).
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
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.wallpaper_title), style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() })
        Text(stringResource(R.string.wallpaper_section_description))
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SceneId.entries.forEach { scene ->
                val description = stringResource(R.string.wallpaper_scene_chip_description, scene.assetId)
                FilterChip(
                    selected = enabled && wallpaper.scene == scene,
                    onClick = { onSceneSelected(scene) }, enabled = enabled,
                    label = { Text(scene.assetId, Modifier.clearAndSetSemantics { }) },
                    modifier = Modifier.fillMaxWidth().testTag("wallpaper-scene-${scene.assetId}")
                        .semantics { contentDescription = description },
                )
            }
        }
        Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PreviewFps.entries.forEach { fps ->
                val description = stringResource(R.string.wallpaper_fps_chip_description, fps.value)
                FilterChip(
                    selected = enabled && wallpaper.fps == fps,
                    onClick = { onFpsSelected(fps) }, enabled = enabled,
                    label = { Text(stringResource(R.string.fps_label, fps.value), Modifier.clearAndSetSemantics { }) },
                    modifier = Modifier.testTag("wallpaper-fps-${fps.value}")
                        .semantics { contentDescription = description },
                )
            }
        }
        Button(onClick = onSetWallpaper, enabled = enabled, modifier = Modifier.testTag("wallpaper-set")) {
            Text(stringResource(R.string.wallpaper_set))
        }
        Text(stringResource(R.string.wallpaper_set_hint), style = MaterialTheme.typography.bodySmall)
        if (pickerUnavailable) {
            Text(stringResource(R.string.wallpaper_picker_unavailable), color = MaterialTheme.colorScheme.error)
        }
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
