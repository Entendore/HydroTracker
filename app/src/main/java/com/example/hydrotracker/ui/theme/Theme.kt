package com.example.hydrotracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF0277BD), onPrimary = Color.White,
    primaryContainer = Color(0xFFB3E5FC),
    secondary = Color(0xFF00838F), tertiary = Color(0xFF00695C)
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF4FC3F7), onPrimary = Color(0xFF00344D),
    primaryContainer = Color(0xFF01579B)
)

@Composable
fun HydroTrackerTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
