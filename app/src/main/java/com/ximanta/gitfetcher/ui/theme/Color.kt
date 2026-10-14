package com.ximanta.gitfetcher.ui.theme

import androidx.compose.ui.graphics.Color

// -----------------------------------------------------------------------------
// Ximanta GitFetcher — brand palette
// -----------------------------------------------------------------------------
//
// The Ximanta identity is built around a deep indigo / electric violet primary
// with a GitHub-flavoured slate secondary and a vibrant mint tertiary accent.
//
//  Primary   → #6C4CF1  (Ximanta Violet)     — main brand, buttons, links
//  Secondary → #24292F  (GitHub Slate)       — surfaces, chips, dividers
//  Tertiary  → #2DD4BF  (Mint Pulse)         — highlights, success states
//  Error     → #EF4444  (Signal Red)         — destructive actions
// -----------------------------------------------------------------------------

// ----------------------------- LIGHT THEME -----------------------------------

val XimantaVioletLight        = Color(0xFF6C4CF1)
val XimantaVioletLightSoft    = Color(0xFFE6E0FF)
val XimantaVioletOnLight      = Color(0xFFFFFFFF)
val XimantaVioletContainer    = Color(0xFFE6E0FF)
val XimantaOnVioletContainer  = Color(0xFF1E0060)

val GithubSlateLight          = Color(0xFF24292F)
val GithubSlateOnLight        = Color(0xFFFFFFFF)
val GithubSlateContainer      = Color(0xFFE1E4E8)
val GithubOnSlateContainer    = Color(0xFF111418)

val MintPulseLight            = Color(0xFF0E9F8A)
val MintPulseOnLight          = Color(0xFFFFFFFF)
val MintPulseContainer        = Color(0xFFB4F1E4)
val MintOnPulseContainer      = Color(0xFF00201B)

val SignalRedLight            = Color(0xFFDC2626)
val SignalRedOnLight          = Color(0xFFFFFFFF)
val SignalRedContainer        = Color(0xFFFEE2E2)
val SignalOnRedContainer      = Color(0xFF5A0000)

// Neutral surfaces — light
val SurfaceLight              = Color(0xFFFBFBFE)
val OnSurfaceLight            = Color(0xFF111114)
val SurfaceVariantLight       = Color(0xFFEBEAF2)
val OnSurfaceVariantLight     = Color(0xFF474651)
val OutlineLight              = Color(0xFF7A7885)
val OutlineVariantLight       = Color(0xFFCAC7D3)

val BackgroundLight           = Color(0xFFFBFBFE)
val OnBackgroundLight         = Color(0xFF111114)

// ----------------------------- DARK THEME ------------------------------------

val XimantaVioletDark         = Color(0xFFCDBDFF)
val XimantaVioletDarkSoft     = Color(0xFF3B2A78)
val XimantaVioletOnDark       = Color(0xFF21005D)
val XimantaVioletContainerDk  = Color(0xFF3B2A78)
val XimantaOnVioletContainerDk= Color(0xFFE6E0FF)

val GithubSlateDark           = Color(0xFFCBD2D9)
val GithubSlateOnDark         = Color(0xFF1F2429)
val GithubSlateContainerDk    = Color(0xFF2C333A)
val GithubOnSlateContainerDk  = Color(0xFFE1E4E8)

val MintPulseDark             = Color(0xFF6EE7D3)
val MintPulseOnDark           = Color(0xFF00382F)
val MintPulseContainerDk      = Color(0xFF005046)
val MintOnPulseContainerDk    = Color(0xFFB4F1E4)

val SignalRedDark             = Color(0xFFFFB4AB)
val SignalRedOnDark           = Color(0xFF690005)
val SignalRedContainerDk      = Color(0xFF93000A)
val SignalOnRedContainerDk    = Color(0xFFFFDAD6)

// Neutral surfaces — dark
val SurfaceDark               = Color(0xFF0F0F13)
val OnSurfaceDark             = Color(0xFFE6E5EC)
val SurfaceVariantDark        = Color(0xFF2A2A32)
val OnSurfaceVariantDark      = Color(0xFFCAC7D3)
val OutlineDark               = Color(0xFF948F9F)
val OutlineVariantDark        = Color(0xFF474651)

val BackgroundDark            = Color(0xFF0A0A0E)
val OnBackgroundDark          = Color(0xFFE6E5EC)

// ----------------------------- ACCENT UTILITIES ------------------------------

/**
 * Used for the "Fetching…" spinner and the primary Fetch button glow.
 */
val XimantaAccentGlow         = Color(0x336C4CF1)

/**
 * Semantic colours used by the Files screen — differ slightly from the theme
 * because we want consistent "success" / "warn" / "info" chips regardless of
 * the active Material theme.
 */
val SuccessGreen              = Color(0xFF16A34A)
val WarningAmber              = Color(0xFFF59E0B)
val InfoBlue                  = Color(0xFF2563EB)

/**
 * Code-ish neutrals used for file-row monospace text.
 */
val CodeTextLight             = Color(0xFF1F2328)
val CodeTextDark              = Color(0xFFD0D7DE)
