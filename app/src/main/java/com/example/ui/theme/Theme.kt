package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CyberColorScheme = darkColorScheme(
    primary = CyberPink,
    secondary = CyberCyan,
    tertiary = CyberPurple,
    background = ObsidianBg,
    surface = SlateSurface,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = ErrorNeon,
    surfaceVariant = LightSlateSurface,
    onSurfaceVariant = TextSecondary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Cyberpunk looks best forced in Dark Mode
    dynamicColor: Boolean = false, // Disable dynamic colors to preserve neon identity
    content: @Composable () -> Unit
) {
    // We enforce our CyberColorScheme representing Cyberpunk Neon
    MaterialTheme(
        colorScheme = CyberColorScheme,
        typography = Typography,
        content = content
    )
}
