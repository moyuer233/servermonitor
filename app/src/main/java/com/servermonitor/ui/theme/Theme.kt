package com.servermonitor.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val FlClashGreen = Color(0xFF2DD4A8)
val BgDark = Color(0xFF0F1115)
val SurfaceDark = Color(0xFF1A1D24)
val SurfaceVariant = Color(0xFF242831)
val TextPrimary = Color(0xFFE8EAED)
val TextSecondary = Color(0xFFA6ADB8)

private val DarkColors = darkColorScheme(
    primary = FlClashGreen,
    onPrimary = Color(0xFF04342A),
    primaryContainer = Color(0xFF0E3D35),
    onPrimaryContainer = Color(0xFFA2F2DE),
    secondary = Color(0xFF7FD6C1),
    onSecondary = Color(0xFF06382F),
    background = BgDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF2B0000)
)

@Composable
fun ServerMonitorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content
    )
}