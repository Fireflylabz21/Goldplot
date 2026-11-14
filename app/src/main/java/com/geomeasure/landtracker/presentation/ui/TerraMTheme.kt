package com.geomeasure.landtracker.presentation.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TerraMColors = darkColorScheme(
    primary = Color(0xFFFFC16C),
    onPrimary = Color(0xFF33200B),
    secondary = Color(0xFFB4BC83),
    onSecondary = Color(0xFF202515),
    tertiary = Color(0xFFD58D5D),
    onTertiary = Color(0xFF311A0B),
    background = Color(0xFF181A12),
    onBackground = Color(0xFFF2E5CC),
    surface = Color(0xFF24271D),
    onSurface = Color(0xFFF2E5CC),
    surfaceVariant = Color(0xFF34382A),
    onSurfaceVariant = Color(0xFFC5B99F),
    error = Color(0xFFE68A78),
    onError = Color(0xFF34130E)
)

@Composable
fun TerraMTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TerraMColors,
        content = content
    )
}
