package com.kopyamatik.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = CharcoalDark,
    onPrimary = PureWhite,
    secondary = AccentBlue,
    onSecondary = PureWhite,
    background = PureWhite,
    onBackground = CharcoalDark,
    surface = PureWhite,
    onSurface = CharcoalDark,
    surfaceVariant = OffWhite,
    onSurfaceVariant = SlateGray,
    outline = BorderLight
)

@Composable
fun KopyamatikTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
