package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val MazeMinerDarkScheme = darkColorScheme(
    primary = ForgedGold,
    onPrimary = ObsidianDeep,
    primaryContainer = ForgedGoldDim,
    onPrimaryContainer = TextPrimary,
    secondary = CrystalCyan,
    onSecondary = ObsidianDeep,
    secondaryContainer = CrystalDeep,
    onSecondaryContainer = TextPrimary,
    tertiary = EmeraldGem,
    onTertiary = ObsidianDeep,
    tertiaryContainer = EmeraldDark,
    onTertiaryContainer = EmeraldGem,
    background = ObsidianDeep,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianDeep,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianCard,
    surfaceContainerHigh = ObsidianElevated,
    surfaceContainerHighest = ObsidianBorder,
    outline = ObsidianBorder,
    outlineVariant = Color(0xFF1E293B),
    error = RivalCrimson,
    onError = TextPrimary
)

val MazeMinerShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MazeMinerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MazeMinerDarkScheme,
        typography = AppTypography,
        shapes = MazeMinerShapes,
        content = content
    )
}
