package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Pure Elegant Light Theme — Strict No-Black UI
val MobitikLightColorScheme = lightColorScheme(
    primary = MobitikOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFF2E5),
    onPrimaryContainer = Color(0xFF8A3B00),
    secondary = Color(0xFF1E293B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF1F5F9),
    onSecondaryContainer = Color(0xFF0F172A),
    tertiary = MobitikGold,
    onTertiary = Color(0xFF1E293B),
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF78350F),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    error = MobitikErrorRed,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0)
)

@Composable
fun MyApplicationTheme(
    themeMode: String = "LIGHT",
    content: @Composable () -> Unit
) {
    // Strictly Light Theme: No black background, ultra-smooth modern interaction
    MaterialTheme(
        colorScheme = MobitikLightColorScheme,
        typography = Typography,
        content = content
    )
}
