package com.mehdiacho.filmarr.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/** Single cinema-dark scheme — no light variant (Ratio Spec §1.1). */
private val FilmarrDarkScheme = darkColorScheme(
    primary = FilmarrColors.Rose,
    onPrimary = FilmarrColors.TextPrimary,
    secondary = FilmarrColors.Blue,
    onSecondary = FilmarrColors.TextPrimary,
    tertiary = FilmarrColors.Green,
    onTertiary = FilmarrColors.OnGreen,
    background = FilmarrColors.Bg,
    onBackground = FilmarrColors.TextPrimary,
    surface = FilmarrColors.Bg,
    onSurface = FilmarrColors.TextPrimary,
    surfaceVariant = FilmarrColors.SurfaceCard,
    onSurfaceVariant = FilmarrColors.TextTertiary,
    surfaceContainer = FilmarrColors.SurfaceRaised,
    surfaceContainerHigh = FilmarrColors.SurfaceCard,
    surfaceContainerHighest = FilmarrColors.SurfaceLifted,
    error = FilmarrColors.Rose,
    onError = FilmarrColors.TextPrimary,
    outline = FilmarrColors.TextMuted,
    outlineVariant = FilmarrColors.HairlineStrong,
)

@Composable
fun FilmarrTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FilmarrDarkScheme,
        typography = FilmarrTypography,
        content = content,
    )
}
