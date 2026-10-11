package com.agrovalle.cosecha.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ============================================================================
// AgroValle Cosecha - Tema Material 3 de Alto Contraste
// Fondo oscuro #121212 con acentos amarillos #FFC107
// ============================================================================

private val AgroDarkColorScheme = darkColorScheme(
    primary = AgroPrimary,
    onPrimary = AgroOnPrimary,
    primaryContainer = AgroPrimaryDark,
    secondary = AgroWarning,
    onSecondary = AgroOnPrimary,
    background = AgroBackground,
    onBackground = AgroTextPrimary,
    surface = AgroSurface,
    onSurface = AgroTextPrimary,
    surfaceVariant = AgroSurfaceVariant,
    onSurfaceVariant = AgroTextSecondary,
    error = AgroError,
    onError = AgroTextPrimary,
    outline = AgroBorderLight,
    outlineVariant = AgroDivider
)

@Composable
fun AgroValleCosechaTheme(content: @Composable () -> Unit) {
    val colorScheme = AgroDarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = AgroBackground.toArgb()
            window.navigationBarColor = AgroBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AgroTypography,
        content = content
    )
}
