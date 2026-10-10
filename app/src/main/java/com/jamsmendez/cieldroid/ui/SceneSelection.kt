package com.jamsmendez.cieldroid.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

/** User-facing scene name; asset ids stay internal (tags, persisted keys, URLs). */
@StringRes
fun sceneLabel(scene: SceneId): Int = when (scene) {
    SceneId.PROCESSING -> R.string.scene_name_processing
    SceneId.EXPLORER -> R.string.scene_name_explorer
    SceneId.IDLE -> R.string.scene_name_idle
    SceneId.RAPHAEL -> R.string.scene_name_raphael
}

/** One launcher card with a heading title. */
@Composable
fun SectionCard(@StringRes title: Int, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() })
            content()
        }
    }
}

/** Compact "Scene: Idle · 30 FPS" row; tapping it opens the selector. Shows no selection while disabled. */
@Composable
fun SelectionSummaryRow(settings: PreviewSettings, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (enabled) stringResource(R.string.selection_summary, stringResource(sceneLabel(settings.scene)), settings.fps.value)
            else stringResource(R.string.selection_unavailable),
            modifier = Modifier.weight(1f),
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (enabled) Text(stringResource(R.string.selection_change), color = MaterialTheme.colorScheme.primary)
    }
}

/**
 * Scene and FPS options for one destination. Options expose a "<prefix> scene <id>" / "<prefix> <n> FPS"
 * style content description instead of their visible text, so they never collide with the preview chips.
 */
@Composable
fun SceneSelectorDialog(
    tagPrefix: String,
    @StringRes title: Int,
    @StringRes sceneDescription: Int,
    @StringRes fpsDescription: Int,
    settings: PreviewSettings,
    onSceneSelected: (SceneId) -> Unit,
    onFpsSelected: (PreviewFps) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("$tagPrefix-selector"),
        title = { Text(stringResource(title)) },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("$tagPrefix-selector-done")) {
                Text(stringResource(R.string.selection_done))
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.selection_scene), style = MaterialTheme.typography.titleSmall)
                Column(Modifier.selectableGroup()) {
                    SceneId.entries.forEach { scene ->
                        val description = stringResource(sceneDescription, scene.assetId)
                        FilterChip(
                            selected = settings.scene == scene, onClick = { onSceneSelected(scene) },
                            label = { Text(stringResource(sceneLabel(scene)), Modifier.clearAndSetSemantics { }) },
                            modifier = Modifier.fillMaxWidth().testTag("$tagPrefix-scene-${scene.assetId}")
                                .semantics { contentDescription = description },
                        )
                    }
                }
                Text(stringResource(R.string.selection_frame_rate), style = MaterialTheme.typography.titleSmall)
                Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PreviewFps.entries.forEach { fps ->
                        val description = stringResource(fpsDescription, fps.value)
                        FilterChip(
                            selected = settings.fps == fps, onClick = { onFpsSelected(fps) },
                            label = { Text(stringResource(R.string.fps_label, fps.value), Modifier.clearAndSetSemantics { }) },
                            modifier = Modifier.testTag("$tagPrefix-fps-${fps.value}")
                                .semantics { contentDescription = description },
                        )
                    }
                }
            }
        },
    )
}
