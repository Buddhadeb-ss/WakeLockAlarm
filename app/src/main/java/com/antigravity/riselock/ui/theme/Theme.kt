package com.antigravity.riselock.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = TerracottaAccent,
    onPrimary = WarmCanvas,
    primaryContainer = TerracottaSubtle,
    onPrimaryContainer = TerracottaAccentDark,
    background = WarmCanvas,
    onBackground = CharcoalPrimary,
    surface = WarmSurface,
    onSurface = CharcoalPrimary,
    surfaceVariant = WarmSurfaceElevated,
    onSurfaceVariant = TaupeSecondary,
    outline = WarmBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = TerracottaAccent,
    onPrimary = WarmCanvasDark,
    primaryContainer = WarmSurfaceElevatedDark,
    onPrimaryContainer = TerracottaAccent,
    background = WarmCanvasDark,
    onBackground = CharcoalPrimaryDark,
    surface = WarmSurfaceDark,
    onSurface = CharcoalPrimaryDark,
    surfaceVariant = WarmSurfaceElevatedDark,
    onSurfaceVariant = TaupeSecondaryDark,
    outline = WarmBorderDark
)

@Composable
fun RiseLockTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let {
                it.statusBarColor = colorScheme.background.toArgb()
                it.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(it, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

