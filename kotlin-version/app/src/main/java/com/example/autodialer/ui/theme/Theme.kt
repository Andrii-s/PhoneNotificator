package com.example.autodialer.ui.theme

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// =============================================================================
// Light colour scheme
// =============================================================================
private val LightColorScheme = lightColorScheme(
    primary                = Blue40,
    onPrimary              = Grey99,
    primaryContainer       = Blue90,
    onPrimaryContainer     = Blue10,

    secondary              = DarkBlue40,
    onSecondary            = Grey99,
    secondaryContainer     = DarkBlue90,
    onSecondaryContainer   = DarkBlue10,

    tertiary               = Teal40,
    onTertiary             = Grey99,
    tertiaryContainer      = Teal90,
    onTertiaryContainer    = Teal10,

    error                  = Red40,
    onError                = Grey99,
    errorContainer         = Red90,
    onErrorContainer       = Red10,

    background             = Grey99,
    onBackground           = Grey10,

    surface                = Grey99,
    onSurface              = Grey10,
    surfaceVariant         = BlueGrey90,
    onSurfaceVariant       = BlueGrey30,

    outline                = BlueGrey50,
)

// =============================================================================
// Dark colour scheme
// =============================================================================
private val DarkColorScheme = darkColorScheme(
    primary                = Blue80,
    onPrimary              = Blue20,
    primaryContainer       = Blue30,
    onPrimaryContainer     = Blue90,

    secondary              = DarkBlue80,
    onSecondary            = DarkBlue20,
    secondaryContainer     = DarkBlue30,
    onSecondaryContainer   = DarkBlue90,

    tertiary               = Teal80,
    onTertiary             = Teal20,
    tertiaryContainer      = Teal30,
    onTertiaryContainer    = Teal90,

    error                  = Red80,
    onError                = Red20,
    errorContainer         = Red40,
    onErrorContainer       = Red90,

    background             = Grey10,
    onBackground           = Grey90,

    surface                = Grey10,
    onSurface              = Grey90,
    surfaceVariant         = BlueGrey20,
    onSurfaceVariant       = BlueGrey80,

    outline                = BlueGrey60,
)

// =============================================================================
// Theme entry-point
// =============================================================================

/**
 * AutoDialer Material 3 theme.
 *
 * @param darkTheme       Follow system dark-mode setting when `true` (default).
 * @param dynamicColor    Use Android 12+ dynamic colour (Material You) when available.
 * @param content         Composable content to be themed.
 */
@Composable
fun AutoDialerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    // Keep status bar transparent (edge-to-edge) and update its icon tint
    // to match the current colour scheme. This is consistent with the
    // android:statusBarColor="@android:color/transparent" declaration in themes.xml.
    val view = LocalView.current
    if (!view.isInEditMode) {
        val currentWindow = (view.context as? Activity)?.window
        SideEffect {
            currentWindow?.let { window ->
                // Maintain transparency set by the XML theme; only adjust icon contrast.
                WindowCompat.getInsetsController(window, view)
                    .isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AutoDialerTypography,
        content     = content,
    )
}
