package com.example.petradar.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary         = PetTeal80,
    onPrimary       = Color(0xFF003135),
    secondary       = PetTealGrey80,
    onSecondary     = Color(0xFF1A0047),
    tertiary        = PetGreen80,
    background      = DarkBackground,
    surface         = DarkSurface,
    onBackground    = Color(0xFFE0F7FA),
    onSurface       = Color(0xFFE0F7FA),
    surfaceVariant  = Color(0xFF1C3050)
)

private val LightColorScheme = lightColorScheme(
    primary         = PetTeal40,
    onPrimary       = Color.White,
    primaryContainer= PetTeal80,
    secondary       = PetTealGrey40,
    onSecondary     = Color.White,
    tertiary        = PetGreen40,
    background      = PetBackground,
    surface         = PetSurface,
    onBackground    = Color(0xFF00363A),
    onSurface       = Color(0xFF00363A),
    surfaceVariant  = PetSurfaceVar,
    outline         = PetTeal40.copy(alpha = 0.4f)
)

@Composable
fun PetRadarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}