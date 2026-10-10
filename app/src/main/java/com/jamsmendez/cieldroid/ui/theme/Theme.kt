package com.jamsmendez.cieldroid.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Fixed dark scheme: the launcher always matches the scenes, whatever the system setting. */
private val CielColors = darkColorScheme(
    primary = CielCyan,
    onPrimary = CielOnCyan,
    primaryContainer = CielSurfaceHigh,
    onPrimaryContainer = CielCyan,
    secondary = CielMagenta,
    onSecondary = Color(0xFF3A0022),
    secondaryContainer = Color(0xFF3A1430),
    onSecondaryContainer = CielIce,
    tertiary = CielGold,
    onTertiary = Color(0xFF2E2800),
    background = CielBackground,
    onBackground = CielIce,
    surface = CielBackground,
    onSurface = CielIce,
    surfaceVariant = CielSurfaceHigh,
    onSurfaceVariant = CielIceMuted,
    surfaceContainerLowest = CielBackground,
    surfaceContainerLow = CielSurface,
    surfaceContainer = CielSurface,
    surfaceContainerHigh = CielSurfaceHigh,
    surfaceContainerHighest = CielSurfaceHigh,
    outline = CielOutline,
    outlineVariant = CielOutline,
    error = CielError,
    onError = Color(0xFF3A0010),
)

@Composable
fun CielDroidTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = CielColors, content = content)
}
