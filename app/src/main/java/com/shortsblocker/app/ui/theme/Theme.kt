package com.shortsblocker.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(primary = androidx.compose.ui.graphics.Color(0xFF4C8DFF))
private val DarkColors = darkColorScheme(primary = androidx.compose.ui.graphics.Color(0xFF4C8DFF))

@Composable
fun ShortsBlockerTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
