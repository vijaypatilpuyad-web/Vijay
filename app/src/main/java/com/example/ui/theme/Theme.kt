package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = CBTPrimary,
    secondary = CBTSecondary,
    tertiary = CBTTertiary,
    background = CBTBackground,
    surface = CBTSurface,
    onPrimary = CBTOnPrimary,
    onSecondary = CBTOnSecondary,
    onBackground = CBTOnBackground,
    onSurface = CBTOnSurface
)

private val DarkColorScheme = darkColorScheme(
    primary = CBTPrimaryDark,
    secondary = CBTSecondaryDark,
    tertiary = CBTTertiaryDark,
    background = CBTBackgroundDark,
    surface = CBTSurfaceDark,
    onPrimary = CBTOnPrimaryDark,
    onSecondary = CBTOnSecondaryDark,
    onBackground = CBTOnBackgroundDark,
    onSurface = CBTOnSurfaceDark
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set false to preserve our premium custom CBT colors strictly
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
