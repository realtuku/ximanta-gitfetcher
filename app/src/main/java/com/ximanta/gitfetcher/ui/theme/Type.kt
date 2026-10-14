package com.ximanta.gitfetcher.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

// -----------------------------------------------------------------------------
// Ximanta GitFetcher — typography
// -----------------------------------------------------------------------------
//
// We rely on the platform's default `FontFamily.SansSerif` (which resolves to
// Roboto on stock Android, San Francisco on some OEMs, and the user's system
// font elsewhere). This means:
//
//   • Zero font assets to ship  → smaller APK
//   • Perfect CJK / Arabic / Cyrillic rendering out of the box
//   • Typography that always matches the user's device
//
// Monospace is used for file paths and sizes in the Files screen.
// -----------------------------------------------------------------------------

private val Default = FontFamily.SansSerif
private val Mono    = FontFamily.Monospace

/**
 * Refined type scale. Material 3 defaults are good, but we tighten line-heights
 * slightly and bump weights so headings feel more branded.
 */
val XimantaTypography = Typography(

    // ---------- Display ----------------------------------------------------
    displayLarge = TextStyle(
        fontFamily = Default,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 54.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = Default,
        fontWeight = FontWeight.Bold,
        fontSize = 38.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.25).sp
    ),
    displaySmall = TextStyle(
        fontFamily = Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = 0.sp
    ),

    // ---------- Headline ---------------------------------------------------
    headlineLarge = TextStyle(
        fontFamily = Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),

    // ---------- Title ------------------------------------------------------
    titleLarge = TextStyle(
        fontFamily = Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontFamily = Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),

    // ---------- Body -------------------------------------------------------
    bodyLarge = TextStyle(
        fontFamily = Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),

    // ---------- Label ------------------------------------------------------
    labelLarge = TextStyle(
        fontFamily = Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

// -----------------------------------------------------------------------------
// Ad-hoc styles used outside Material's scale
// -----------------------------------------------------------------------------

/** Used for file paths, sizes and other code-ish strings. */
val MonoStyle = TextStyle(
    fontFamily = Mono,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.sp,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None
    )
)

/** Used for the Ximanta brand wordmark on the Terms screen. */
val BrandWordmarkStyle = TextStyle(
    fontFamily = Default,
    fontWeight = FontWeight.Black,
    fontSize = 34.sp,
    lineHeight = 40.sp,
    letterSpacing = (-0.75).sp
)

/** Small uppercase "tagline" style used below the brand wordmark. */
val TaglineStyle = TextStyle(
    fontFamily = Default,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 2.sp
)
