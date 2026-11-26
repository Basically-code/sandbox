package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val TradeLinkLightColorScheme = lightColorScheme(
  primary = BrandPrimary,
  onPrimary = Color.White,
  primaryContainer = BrandSecondary,
  onPrimaryContainer = BrandPrimaryDark,
  secondary = AccentWarm,
  onSecondary = TextPrimary,
  secondaryContainer = AccentWarmSoft,
  onSecondaryContainer = TextPrimary,
  tertiary = StatusSafeGreen,
  onTertiary = Color.White,
  tertiaryContainer = StatusSafeGreenSoft,
  onTertiaryContainer = TextPrimary,
  background = BgPrimaryCanvas,
  onBackground = TextPrimary,
  surface = BgSurfaceCard,
  onSurface = TextPrimary,
  surfaceVariant = BgSurfaceSubtle,
  onSurfaceVariant = TextSecondary,
  outline = BorderHairline,
  outlineVariant = BorderInput,
  error = StatusDanger,
  onError = Color.White,
  errorContainer = StatusDangerSoft,
  onErrorContainer = TextPrimary,
)

private val TradeLinkShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(24.dp),
)

@Composable
fun TradeLinkTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = TradeLinkLightColorScheme,
    typography = TradeLinkTypography,
    shapes = TradeLinkShapes,
    content = content,
  )
}
