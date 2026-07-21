package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val FlappyCrowColorScheme = darkColorScheme(
    primary = NightPrimary,
    secondary = NightSecondary,
    tertiary = NightTertiary,
    background = NightBackground,
    surface = NightSurface,
    onPrimary = NightBackground,
    onSecondary = NightBackground,
    onTertiary = NightBackground,
    onBackground = NightOnBackground,
    onSurface = NightOnSurface
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FlappyCrowColorScheme,
        typography = Typography,
        content = content
    )
}
