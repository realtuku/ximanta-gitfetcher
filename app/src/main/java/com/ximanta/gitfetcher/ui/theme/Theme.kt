package com.ximanta.gitfetcher.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// -----------------------------------------------------------------------------
// Light scheme
// -----------------------------------------------------------------------------

private val LightColors = lightColorScheme(
    primary              = XimantaVioletLight,
    onPrimary            = XimantaVioletOnLight,
    primaryContainer     = XimantaVioletContainer,
    onPrimaryContainer   = XimantaOnVioletContainer,

    secondary            = GithubSlateLight,
    onSecondary          = GithubSlateOnLight,
    secondaryContainer   = GithubSlateContainer,
    onSecondaryContainer = GithubOnSlateContainer,

    tertiary             = MintPulseLight,
    onTertiary           = MintPulseOnLight,
    tertiaryContainer    = MintPulseContainer,
    onTertiaryContainer  = MintOnPulseContainer,

    error                = SignalRedLight,
    onError              = SignalRedOnLight,
    errorContainer       = SignalRedContainer,
    onErrorContainer     = SignalOnRedContainer,

    background           = BackgroundLight,
    onBackground         = OnBackgroundLight,

    surface              = SurfaceLight,
    onSurface            = OnSurfaceLight,
    surfaceVariant       = SurfaceVariantLight,
    onSurfaceVariant     = OnSurfaceVariantLight,

    outline              = OutlineLight,
    outlineVariant       = OutlineVariantLight,

    scrim                = Color(0x99000000),
    inverseSurface       = GithubSlateLight,
    inverseOnSurface     = Color(0xFFF0F0F3),
    inversePrimary       = XimantaVioletDark,
    surfaceTint          = XimantaVioletLight
)

// -----------------------------------------------------------------------------
// Dark scheme
// -----------------------------------------------------------------------------

private val DarkColors = darkColorScheme(
    primary              = XimantaVioletDark,
    onPrimary            = XimantaVioletOnDark,
    primaryContainer     = XimantaVioletContainerDk,
    onPrimaryContainer   = XimantaOnVioletContainerDk,

    secondary            = GithubSlateDark,
    onSecondary          = GithubSlateOnDark,
    secondaryContainer   = GithubSlateContainerDk,
    onSecondaryContainer = GithubOnSlateContainerDk,

    tertiary             = MintPulseDark,
    onTertiary           = MintPulseOnDark,
    tertiaryContainer    = MintPulseContainerDk,
    onTertiaryContainer  = MintOnPulseContainerDk,

    error                = SignalRedDark,
    onError              = SignalRedOnDark,
    errorContainer       = SignalRedContainerDk,
    onErrorContainer     = SignalOnRedContainerDk,

    background           = BackgroundDark,
    onBackground         = OnBackgroundDark,

    surface              = SurfaceDark,
    onSurface            = OnSurfaceDark,
    surfaceVariant       = SurfaceVariantDark,
    onSurfaceVariant     = OnSurfaceVariantDark,

    outline              = OutlineDark,
    outlineVariant       = OutlineVariantDark,

    scrim                = Color(0xCC000000),
    inverseSurface       = Color(0xFFE1E4E8),
    inverseOnSurface     = Color(0xFF24292F),
    inversePrimary       = XimantaVioletLight,
    surfaceTint          = XimantaVioletDark
)

// -----------------------------------------------------------------------------
// Public theme composable
// -----------------------------------------------------------------------------

/**
 * Root theme wrapper.
 *
 * @param darkTheme  Whether to use the dark colour scheme. Defaults to the
 *                   system setting, but can be forced for previews.
 * @param dynamicColor  When true and running on Android 12+, adopt the user's
 *                   wallpaper-derived Material You palette instead of the
 *                   Ximanta brand palette.
 *                   Defaults to FALSE so the Ximanta identity is preserved.
 * @param content    The composable content to theme.
 */
@Composable
fun XimantaGitFetcherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Dynamic colour support was added in Android 12 (S).
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else      -> LightColors
    }

    // Sync the status-bar / nav-bar icon colours with our scheme.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()

            // WindowCompat handles the modern API cleanly (API 23+).
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = XimantaTypography,
        content = content
    )
}
