package com.example.petradar.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * PetRadar color palette.
 *
 * Organised in three groups:
 *  - Primary   (vibrant, friendly blue)
 *  - Secondary (warm, energetic orange)
 *  - Tertiary  (calm, natural green)
 *
 * "80" variants are lighter (for dark theme or container backgrounds).
 * "40" variants are more saturated (for light theme or primary actions).
 *
 * Assigned to [androidx.compose.material3.MaterialTheme] in Theme.kt.
 */

// ── Primary: Blue ─────────────────────────────────────────────────────────────
val PetTeal80  = Color(0xFF64B5F6) // Light blue — primary in dark theme
val PetTeal40  = Color(0xFF2196F3) // Main blue  — primary in light theme

// ── Secondary: Orange ─────────────────────────────────────────────────────────
val PetTealGrey80 = Color(0xFFFFB74D) // Light orange — secondary in dark theme
val PetTealGrey40 = Color(0xFFF57C00) // Dark orange  — secondary in light theme

// ── Tertiary: Green ───────────────────────────────────────────────────────────
val PetGreen80 = Color(0xFF81C784) // Light green — tertiary in dark theme
val PetGreen40 = Color(0xFF388E3C) // Dark green  — tertiary in light theme

// ── Accent ────────────────────────────────────────────────────────────────────
/** Golden orange; used for accent elements such as badges or highlights. */
val PetAccent  = Color(0xFFFF9800)

// ── Light theme surfaces ──────────────────────────────────────────────────────
val PetBackground  = Color(0xFFF5F5F5) // App-wide background in light theme
val PetSurface     = Color(0xFFFFFFFF) // Card and dialog surfaces
val PetSurfaceVar  = Color(0xFFEEEEEE) // Surface variant (inputs, chips)

// ── Dark theme surfaces ───────────────────────────────────────────────────────
val DarkBackground = Color(0xFF121212) // App-wide background in dark theme
val DarkSurface    = Color(0xFF1E1E1E) // Card surfaces in dark theme
