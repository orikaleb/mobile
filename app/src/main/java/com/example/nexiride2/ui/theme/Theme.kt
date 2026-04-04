package com.example.nexiride2.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue, onPrimary = SurfaceLight,
    primaryContainer = PrimaryBlueLight, onPrimaryContainer = SurfaceLight,
    secondary = AccentGreen, onSecondary = TextPrimaryLight,
    secondaryContainer = AccentGreenLight, onSecondaryContainer = TextPrimaryLight,
    tertiary = SecondaryOrange, onTertiary = SurfaceLight,
    background = BackgroundLight, onBackground = TextPrimaryLight,
    surface = SurfaceLight, onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight, onSurfaceVariant = TextSecondaryLight,
    error = StatusError, onError = SurfaceLight
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlueLight, onPrimary = SurfaceLight,
    primaryContainer = PrimaryBlue, onPrimaryContainer = SurfaceLight,
    secondary = AccentGreen, onSecondary = TextPrimaryLight,
    secondaryContainer = AccentGreenDark, onSecondaryContainer = TextPrimaryDark,
    tertiary = SecondaryOrange, onTertiary = SurfaceLight,
    background = BackgroundDark, onBackground = TextPrimaryDark,
    surface = SurfaceDark, onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark, onSurfaceVariant = TextSecondaryDark,
    error = StatusError, onError = SurfaceLight
)

@Composable
fun NexiRideTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    MaterialTheme(colorScheme = colorScheme, typography = NexiRideTypography, content = content)
}
