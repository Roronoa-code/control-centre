package com.mani.controlcentre.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Restrained black/graphite palette from the reference recording. No gradients, no accent glow. */
private val Graphite = darkColorScheme(
    background = Color(0xFF000000),
    onBackground = Color(0xFFF2F2F2),
    surface = Color(0xFF0E0E10),
    onSurface = Color(0xFFF2F2F2),
    surfaceVariant = Color(0xFF1C1C1F),
    onSurfaceVariant = Color(0xFF9A9AA0),
    primary = Color(0xFFE6E9EF),
    onPrimary = Color(0xFF000000),
    secondary = Color(0xFF8A8F98),
    outline = Color(0xFF2A2A2E),
)

@Composable
fun ControlCentreTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Graphite, content = content)
}
