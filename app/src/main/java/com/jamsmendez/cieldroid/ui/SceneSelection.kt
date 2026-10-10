package com.jamsmendez.cieldroid.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jamsmendez.cieldroid.R
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
