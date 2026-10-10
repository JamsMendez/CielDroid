package com.jamsmendez.cieldroid.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.jamsmendez.cieldroid.R
import com.jamsmendez.cieldroid.domain.ActiveMode

/**
 * One card with a single-choice "Scene Wallpaper | Scene Mini" control; only the selected mode's controls
 * are composed. Both modes follow the preview's shared scene/FPS, so neither shows its own selector.
 */
@Composable
fun ModeSection(
    mode: ActiveMode,
    enabled: Boolean,
    onModeSelected: (ActiveMode) -> Unit,
    wallpaper: @Composable ColumnScope.() -> Unit,
    mini: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(R.string.mode_title, modifier) {
        // The row is a selectableGroup and each segment a RadioButton-role selectable.
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            ActiveMode.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = mode == option, onClick = { onModeSelected(option) }, enabled = enabled,
                    shape = SegmentedButtonDefaults.itemShape(index, ActiveMode.entries.size),
                    modifier = Modifier.testTag("mode-${option.storageId}"),
                    label = { Text(stringResource(modeLabel(option))) },
                )
            }
        }
        when (mode) {
            ActiveMode.WALLPAPER -> wallpaper()
            ActiveMode.MINI -> mini()
        }
        Text(stringResource(R.string.mode_shared_hint), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun modeLabel(mode: ActiveMode) = when (mode) {
    ActiveMode.WALLPAPER -> R.string.mode_wallpaper
    ActiveMode.MINI -> R.string.mode_mini
}
