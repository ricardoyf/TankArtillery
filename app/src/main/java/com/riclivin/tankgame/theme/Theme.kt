package com.riclivin.tankgame.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7CC8FF),
    secondary = Color(0xFFFFB199),
    background = Color(0xFF0B1020),
    surface = Color(0xFF11182E),
    onSurface = Color(0xFFE6EEF8),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF005B8F),
    secondary = Color(0xFFB04322),
)

@Composable
fun TankArtilleryTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
