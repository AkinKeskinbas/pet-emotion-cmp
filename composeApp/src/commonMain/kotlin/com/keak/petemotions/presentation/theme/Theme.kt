package com.keak.petemotions.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Elegant Black & White Color Palette
private val White = Color(0xFFFFFFFF)
private val Black = Color(0xFF000000)
private val Gray100 = Color(0xFFF5F5F5)
private val Gray200 = Color(0xFFEEEEEE)
private val Gray300 = Color(0xFFE0E0E0)
private val Gray400 = Color(0xFFBDBDBD)
private val Gray500 = Color(0xFF9E9E9E)
private val Gray600 = Color(0xFF757575)
private val Gray700 = Color(0xFF616161)
private val Gray800 = Color(0xFF424242)
private val Gray900 = Color(0xFF212121)

// Elegant Light Theme
private val LightColorScheme = lightColorScheme(
    primary = Black,
    onPrimary = White,
    primaryContainer = Gray200,
    onPrimaryContainer = Gray900,

    secondary = Gray700,
    onSecondary = White,
    secondaryContainer = Gray300,
    onSecondaryContainer = Gray900,

    tertiary = Gray600,
    onTertiary = White,
    tertiaryContainer = Gray200,
    onTertiaryContainer = Gray800,

    error = Gray800,
    onError = White,
    errorContainer = Gray300,
    onErrorContainer = Gray900,

    background = White,
    onBackground = Black,

    surface = White,
    onSurface = Black,
    surfaceVariant = Gray100,
    onSurfaceVariant = Gray700,

    outline = Gray400,
    outlineVariant = Gray300,

    scrim = Black,
    inverseSurface = Gray900,
    inverseOnSurface = Gray100,
    inversePrimary = Gray400,

    surfaceTint = Black,
    surfaceContainer = Gray100,
    surfaceContainerHigh = Gray200,
    surfaceContainerHighest = Gray300,
    surfaceContainerLow = Gray100,
    surfaceContainerLowest = White,

    surfaceBright = White,
    surfaceDim = Gray100
)

// Elegant Dark Theme
private val DarkColorScheme = darkColorScheme(
    primary = White,
    onPrimary = Black,
    primaryContainer = Gray800,
    onPrimaryContainer = Gray100,

    secondary = Gray300,
    onSecondary = Gray900,
    secondaryContainer = Gray700,
    onSecondaryContainer = Gray200,

    tertiary = Gray400,
    onTertiary = Gray900,
    tertiaryContainer = Gray800,
    onTertiaryContainer = Gray300,

    error = Gray300,
    onError = Gray900,
    errorContainer = Gray700,
    onErrorContainer = Gray200,

    background = Black,
    onBackground = White,

    surface = Black,
    onSurface = White,
    surfaceVariant = Gray900,
    onSurfaceVariant = Gray400,

    outline = Gray600,
    outlineVariant = Gray700,

    scrim = Black,
    inverseSurface = Gray200,
    inverseOnSurface = Gray800,
    inversePrimary = Gray600,

    surfaceTint = White,
    surfaceContainer = Gray900,
    surfaceContainerHigh = Gray800,
    surfaceContainerHighest = Gray700,
    surfaceContainerLow = Gray900,
    surfaceContainerLowest = Black,

    surfaceBright = Gray800,
    surfaceDim = Black
)

@Composable
fun PetEmotionsTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}