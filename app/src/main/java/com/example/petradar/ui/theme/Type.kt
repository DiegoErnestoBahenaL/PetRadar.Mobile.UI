package com.example.petradar.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * PetRadar typography configuration for Material3.
 *
 * Only [Typography.bodyLarge] is customised explicitly;
 * the remaining styles (`titleLarge`, `labelSmall`, etc.) use
 * the default Material3 values.
 *
 * To customise more styles, uncomment the example blocks below
 * and adjust the font family, size, etc.
 *
 * Passed to [androidx.compose.material3.MaterialTheme] in [PetRadarTheme].
 */
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily   = FontFamily.Default, // System font (Roboto on Android)
        fontWeight   = FontWeight.Normal,
        fontSize     = 16.sp,
        lineHeight   = 24.sp,
        letterSpacing = 0.5.sp
    )
    /* Examples of additional styles that can be customised:

    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
)