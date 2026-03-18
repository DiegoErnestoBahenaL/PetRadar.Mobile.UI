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

/**
 * Color scheme for PetRadar's dark theme.
 * Applied automatically when the device is in dark mode.
 * Uses "80" variants (lighter) as primary colors to ensure
 * sufficient contrast against dark backgrounds.
 */
private val DarkColorScheme = darkColorScheme(
    primary         = PetTeal80,            // Light blue on dark background
    onPrimary       = Color.Black,
    secondary       = PetTealGrey80,        // Light orange on dark background
    onSecondary     = Color.Black,
    tertiary        = PetGreen80,           // Light green
    background      = DarkBackground,       // Dark background (#121212)
    surface         = DarkSurface,          // Card surface (#1E1E1E)
    onBackground    = Color(0xFFE0E0E0),    // Light text on dark background
    onSurface       = Color(0xFFE0E0E0),
    surfaceVariant  = Color(0xFF333333)
)

/**
 * Color scheme for PetRadar's light theme.
 * Applied when the device is in light mode (default).
 * Uses "40" variants (more saturated) as primary colors.
 */
private val LightColorScheme = lightColorScheme(
    primary          = PetTeal40,           // Main blue (#2196F3)
    onPrimary        = Color.White,
    primaryContainer = PetTeal80,           // Light blue container (chips, badges)
    secondary        = PetTealGrey40,       // Dark orange (#F57C00)
    onSecondary      = Color.White,
    tertiary         = PetGreen40,          // Dark green (#388E3C)
    background       = PetBackground,       // Very light grey background (#F5F5F5)
    surface          = PetSurface,          // White surface (#FFFFFF)
    onBackground     = Color(0xFF121212),   // Dark text on light background
    onSurface        = Color(0xFF121212),
    surfaceVariant   = PetSurfaceVar,       // Inputs, chips (#EEEEEE)
    outline          = PetTeal40.copy(alpha = 0.4f)  // Text field borders
)

/**
 * Main Compose theme for PetRadar.
 *
 * Applies the color scheme, typography and Material3 shapes.
 * Also sets up edge-to-edge mode (content drawn behind system bars)
 * via [WindowCompat.setDecorFitsSystemWindows].
 *
 * Usage:
 * ```kotlin
 * PetRadarTheme {
 *     // Your Compose content here
 * }
 * ```
 *
 * @param darkTheme If true, uses [DarkColorScheme]; defaults to the system preference.
 * @param content   Composable content that inherits this theme.
 */
@Composable
fun PetRadarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        // SideEffect ensures this block runs after every recomposition.
        // Disables "fit windows" so content can be drawn behind
        // the status bar and navigation bar (edge-to-edge).
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,  // Defined in Type.kt
        content = content
    )
}