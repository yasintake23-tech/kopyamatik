package com.kopyamatik.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Always strictly enforce Light Color Scheme regardless of device system settings
private val StrictLightColorScheme = lightColorScheme(
    primary = CharcoalDark,
    onPrimary = Color.White,
    secondary = AccentBlue,
    onSecondary = Color.White,
    background = Color.White,
    onBackground = CharcoalDark,
    surface = Color.White,
    onSurface = CharcoalDark,
    surfaceVariant = OffWhite,
    onSurfaceVariant = SlateGray,
    outline = BorderLight
)

@Composable
fun KopyamatikTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let {
                it.statusBarColor = Color.White.toArgb()
                it.navigationBarColor = Color.White.toArgb()
                val insetsController = WindowCompat.getInsetsController(it, view)
                insetsController.isAppearanceLightStatusBars = true
                insetsController.isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = StrictLightColorScheme,
        typography = Typography,
        content = content
    )
}
