package com.lamti.capturetheflag.presentation.ui.style

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable

private val DarkColorPalette = darkColors(
    primary = Blue,
    primaryVariant = DarkBlue,
    secondary = TextColor,
    background = Black,
    onBackground = White,
    surface = LightGray,
    onSurface = TextColor
)

private val LightColorPalette = lightColors(
    primary = Blue,
    primaryVariant = Blue900,
    secondary = TextColor,
    background = White,
    onBackground = Black,
    surface = LightGray,
    onSurface = TextColor
)

@Composable
fun CaptureTheFlagTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
