package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Tradee (TradeLink) Daylight-First High-Contrast Color Tokens & Gradients
val BgPrimaryCanvas = Color(0xFFF8FAFC)
val BgSurfaceCard = Color(0xFFFFFFFF)
val BgSurfaceSubtle = Color(0xFFF1F5F9)

val TextPrimary = Color(0xFF0F172A)
val TextSecondary = Color(0xFF475569)
val TextMuted = Color(0xFF94A3B8)

val BrandPrimary = Color(0xFF0284C7)
val BrandPrimaryDark = Color(0xFF0369A1)
val BrandSkyLight = Color(0xFF38BDF8)
val BrandSecondary = Color(0xFFE0F2FE)
val AccentWarm = Color(0xFFF59E0B)
val AccentWarmDeep = Color(0xFFEA580C)
val AccentWarmSoft = Color(0xFFFEF3C7)

val StatusSafeGreen = Color(0xFF10B981)
val StatusSafeGreenDark = Color(0xFF059669)
val StatusSafeGreenSoft = Color(0xFFD1FAE5)
val StatusWarning = Color(0xFFF97316)
val StatusWarningSoft = Color(0xFFFFEDD5)
val StatusDanger = Color(0xFFEF4444)
val StatusDangerSoft = Color(0xFFFEE2E2)

val BorderHairline = Color(0xFFE2E8F0)
val BorderInput = Color(0xFFCBD5E1)

// Daylight-Safe Multi-Stop Color Gradients
val BrandPrimaryGradient = Brush.linearGradient(
  colors = listOf(BrandSkyLight, BrandPrimary, BrandPrimaryDark),
)

val HeroOceanGradient = Brush.linearGradient(
  colors = listOf(TextPrimary, BrandPrimaryDark, BrandPrimary),
)

val WarmDealGradient = Brush.linearGradient(
  colors = listOf(AccentWarmSoft, Color(0xFFFDE68A), AccentWarm.copy(alpha = 0.45f)),
)

val BoltTransportGradient = Brush.linearGradient(
  colors = listOf(StatusSafeGreenDark, StatusSafeGreen, BrandPrimary),
)

val CardSheenGradient = Brush.linearGradient(
  colors = listOf(BgSurfaceCard, BgPrimaryCanvas, BrandSecondary.copy(alpha = 0.55f)),
)

