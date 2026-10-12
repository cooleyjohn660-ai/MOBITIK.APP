package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val MobitikDarkColorScheme = darkColorScheme(
    primary = MobitikOrange,
    onPrimary = MobitikBlack,
    primaryContainer = MobitikOrangeDeep,
    onPrimaryContainer = MobitikWhite,
    secondary = MobitikWhite,
    onSecondary = MobitikBlack,
    secondaryContainer = MobitikSurfaceElevated,
    onSecondaryContainer = MobitikWhite,
    tertiary = MobitikGold,
    onTertiary = MobitikBlack,
    background = MobitikBlack,
    onBackground = MobitikWhite,
    surface = MobitikCharcoal,
    onSurface = MobitikWhite,
    surfaceVariant = MobitikSurfaceElevated,
    onSurfaceVariant = MobitikTextSecondary,
    error = MobitikErrorRed,
    onError = MobitikWhite,
    outline = MobitikBorderDark
)

private val MobitikLightColorScheme = lightColorScheme(
    primary = MobitikOrange,
    onPrimary = MobitikWhite,
    primaryContainer = MobitikOrangeLight,
    onPrimaryContainer = MobitikBlack,
    secondary = MobitikLightTextPrimary,
    onSecondary = MobitikWhite,
    secondaryContainer = MobitikLightSurfaceVariant,
    onSecondaryContainer = MobitikLightTextPrimary,
    tertiary = MobitikOrangeDeep,
    onTertiary = MobitikWhite,
    background = MobitikLightBg,
    onBackground = MobitikLightTextPrimary,
    surface = MobitikLightCard,
    onSurface = MobitikLightTextPrimary,
    surfaceVariant = MobitikLightSurfaceVariant,
    onSurfaceVariant = MobitikLightTextSecondary,
    error = MobitikErrorRed,
    onError = MobitikWhite,
    outline = MobitikTextMuted
)

@Composable
fun MyApplicationTheme(
    themeMode: String = "DARK",
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        "LIGHT" -> false
        "SYSTEM" -> isSystemInDarkTheme()
        else -> true
    }

    val colorScheme = if (useDark) MobitikDarkColorScheme else MobitikLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
