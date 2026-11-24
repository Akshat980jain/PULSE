package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CobaltPulse,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E3A6E),
    onPrimaryContainer = Color(0xFFE0EEFF),
    secondary = ShutterAmber,
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF47330B),
    onSecondaryContainer = Color(0xFFFEF3C7),
    tertiary = SignalCrimson,
    onTertiary = Color.White,
    background = ObsidianStudio,
    onBackground = SilverHalideText,
    surface = ObsidianSurface,
    onSurface = SilverHalideText,
    surfaceVariant = SlateEmulsion,
    onSurfaceVariant = MutedFilmText,
    surfaceContainerLowest = Color(0xFF111620),
    surfaceContainerLow = ObsidianStudio,
    surfaceContainer = ObsidianSurface,
    surfaceContainerHigh = SlateEmulsion,
    surfaceContainerHighest = SlateElevated,
    outline = SlateHairline,
    outlineVariant = SlateDivider
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = Color(0xFFD97706),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = Color(0xFFE11D48),
    onTertiary = Color.White,
    background = AlabasterGallery,
    onBackground = InkGalleryText,
    surface = AlabasterSurface,
    onSurface = InkGalleryText,
    surfaceVariant = LightEmulsion,
    onSurfaceVariant = MutedGalleryText,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = AlabasterGallery,
    surfaceContainer = LightEmulsion,
    surfaceContainerHigh = Color(0xFFE2E8F0),
    surfaceContainerHighest = Color(0xFFD5DCE6),
    outline = LightHairline,
    outlineVariant = Color(0xFFE2E8F0)
)

@Composable
fun PulseTheme(
    darkTheme: Boolean = true, // Elevated Dark Slate studio theme with crisp high contrast
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
