package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CyanPrimaryLight,
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = IndigoAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF373A75),
    onSecondaryContainer = Color(0xFFE0E0FF),
    tertiary = PurpleAccent,
    background = TechDarkBackground,
    onBackground = TextPrimaryDark,
    surface = TechDarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = TechDarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    error = StatusBlockedRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = CyanPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = IndigoAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E0FF),
    onSecondaryContainer = Color(0xFF14144B),
    tertiary = PurpleAccent,
    background = TechLightBackground,
    onBackground = TextPrimaryLight,
    surface = TechLightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = TechLightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    error = StatusBlockedRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to sleek obsidian dark dashboard for private admin panel
    dynamicColor: Boolean = false, // Set to false to preserve the sleek dark/cyan cybersecurity brand aesthetic
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
