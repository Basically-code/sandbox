package com.example.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.random.Random

private val HavelLightColorScheme = lightColorScheme(
    primary = DesaturatedSage,
    onPrimary = WarmAlabaster,
    primaryContainer = SageContainerLight,
    onPrimaryContainer = EspressoCharcoal,
    secondary = DustyTerracotta,
    onSecondary = EspressoCharcoal,
    secondaryContainer = TerracottaContainerLight,
    onSecondaryContainer = EspressoCharcoal,
    tertiary = WarmStone,
    onTertiary = WarmAlabaster,
    background = WarmAlabaster,
    onBackground = EspressoCharcoal,
    surface = SoftStationeryCard,
    onSurface = EspressoCharcoal,
    surfaceVariant = WarmSandSurface,
    onSurfaceVariant = WarmStone,
    outline = WarmBorderLight,
    outlineVariant = OatMist,
    error = MutedClayError,
    onError = WarmAlabaster,
    errorContainer = ErrorContainerLight,
    onErrorContainer = EspressoCharcoal
)

private val HavelDarkColorScheme = darkColorScheme(
    primary = DesaturatedSageDark,
    onPrimary = MossCharcoal,
    primaryContainer = SageContainerDark,
    onPrimaryContainer = WarmIvory,
    secondary = DustyTerracottaDark,
    onSecondary = MossCharcoal,
    secondaryContainer = TerracottaContainerDark,
    onSecondaryContainer = WarmIvory,
    tertiary = MutedAsh,
    onTertiary = MossCharcoal,
    background = MutedSlate,
    onBackground = WarmIvory,
    surface = DuskSurfaceCard,
    onSurface = WarmIvory,
    surfaceVariant = DuskElevatedSurface,
    onSurfaceVariant = MutedAsh,
    outline = DuskBorder,
    outlineVariant = MossCharcoal,
    error = MutedClayErrorDark,
    onError = MossCharcoal,
    errorContainer = ErrorContainerDark,
    onErrorContainer = WarmIvory
)

val HavelShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

val LocalIsHavelDark = staticCompositionLocalOf { false }

@Composable
fun HavelTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) HavelDarkColorScheme else HavelLightColorScheme
    CompositionLocalProvider(LocalIsHavelDark provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = HavelTypography,
            shapes = HavelShapes,
            content = content
        )
    }
}

/**
 * Renders Havel's signature 135-degree matte stationery gradient with subtle 2% organic grain
 * to prevent banding and eliminate visual glare.
 */
@Composable
fun HavelStationeryBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = LocalIsHavelDark.current
    val startColor = if (isDark) MutedSlate else WarmAlabaster
    val endColor = if (isDark) MossCharcoal else OatMist
    val grainColor = if (isDark) Color.White.copy(alpha = 0.025f) else EspressoCharcoal.copy(alpha = 0.022f)

    val grainSeeds = remember {
        val rng = Random(42)
        List(280) { Pair(rng.nextFloat(), rng.nextFloat()) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(startColor, endColor),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                )
            )
            grainSeeds.forEach { (nx, ny) ->
                drawCircle(
                    color = grainColor,
                    radius = 1.4f,
                    center = Offset(nx * size.width, ny * size.height)
                )
            }
        }
        content()
    }
}
