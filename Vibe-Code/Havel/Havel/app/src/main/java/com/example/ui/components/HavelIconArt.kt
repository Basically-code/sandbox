package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

/**
 * Renders the redesigned Havel App Icon emblem:
 * - 135-degree diagonal split backdrop in Moss Charcoal (#1C2021) and Desaturated Sage (#5E7F70)
 * - Stacked Oat Mist & Warm Alabaster stationery note cards with a crisp folded top-right corner
 * - Fountain pen nib morphing into 4 voice-to-note waveform bars (Sage -> Taupe -> Terracotta)
 * - Top-left 4-point Dusty Terracotta AI synthesis spark
 * - Bottom-right minimalist open lock glyph symbolizing Lock Screen Availability
 */
@Composable
fun HavelAppIconEmblem(
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.clip(RoundedCornerShape(24))
    ) {
        val w = size.width
        val h = size.height

        // 1. Upper-left Moss Charcoal background
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF1C2021), Color(0xFF262C2E)),
                start = Offset.Zero,
                end = Offset(w * 0.75f, h * 0.75f)
            ),
            size = size
        )

        // 2. Lower-right Desaturated Sage 135-degree architectural triangle
        val sageTriangle = Path().apply {
            moveTo(w, 0f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            path = sageTriangle,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF6E8F80), Color(0xFF537264)),
                start = Offset(w * 0.3f, h * 0.3f),
                end = Offset(w, h)
            )
        )

        // 3. Secondary tilted Oat Mist card underneath (-8 deg)
        rotate(degrees = -8f, pivot = Offset(w * 0.5f, h * 0.52f)) {
            drawRoundRect(
                color = Color(0xFFE3DDD2),
                topLeft = Offset(w * 0.25f, h * 0.24f),
                size = Size(w * 0.46f, h * 0.52f),
                cornerRadius = CornerRadius(w * 0.05f, w * 0.05f)
            )
        }

        // 4. Primary Warm Alabaster stationery sheet with folded top-right corner
        val cardLeft = w * 0.27f
        val cardTop = h * 0.23f
        val cardRight = w * 0.73f
        val cardBottom = h * 0.75f
        val foldSize = w * 0.13f

        val sheetPath = Path().apply {
            moveTo(cardLeft, cardTop)
            lineTo(cardRight - foldSize, cardTop)
            lineTo(cardRight, cardTop + foldSize)
            lineTo(cardRight, cardBottom)
            lineTo(cardLeft, cardBottom)
            close()
        }
        drawPath(
            path = sheetPath,
            color = Color(0xFFF6F5F0)
        )

        // Folded corner flap
        val flapPath = Path().apply {
            moveTo(cardRight - foldSize, cardTop)
            lineTo(cardRight - foldSize, cardTop + foldSize)
            lineTo(cardRight, cardTop + foldSize)
            close()
        }
        drawPath(
            path = flapPath,
            color = Color(0xFFE2DDD3)
        )

        // 5. Top-left 4-point Dusty Terracotta AI Spark
        val sparkCx = w * 0.38f
        val sparkCy = h * 0.33f
        val sparkOuter = w * 0.045f
        val sparkInner = w * 0.013f
        val sparkPath = Path().apply {
            moveTo(sparkCx, sparkCy - sparkOuter)
            lineTo(sparkCx + sparkInner, sparkCy - sparkInner)
            lineTo(sparkCx + sparkOuter, sparkCy)
            lineTo(sparkCx + sparkInner, sparkCy + sparkInner)
            lineTo(sparkCx, sparkCy + sparkOuter)
            lineTo(sparkCx - sparkInner, sparkCy + sparkInner)
            lineTo(sparkCx - sparkOuter, sparkCy)
            lineTo(sparkCx - sparkInner, sparkCy - sparkInner)
            close()
        }
        drawPath(path = sparkPath, color = Color(0xFFC89D84))

        // 6. Fountain Pen Nib (left) morphing into 4 Waveform Bars (right)
        val midY = h * 0.50f
        val nibPath = Path().apply {
            moveTo(w * 0.33f, midY)
            lineTo(w * 0.41f, midY - h * 0.045f)
            lineTo(w * 0.455f, midY - h * 0.03f)
            lineTo(w * 0.455f, midY + h * 0.03f)
            lineTo(w * 0.41f, midY + h * 0.045f)
            close()
        }
        drawPath(path = nibPath, color = Color(0xFF5B786B))
        // Nib breather hole
        drawCircle(
            color = Color(0xFFF6F5F0),
            radius = w * 0.012f,
            center = Offset(w * 0.41f, midY)
        )
        // Nib collar
        drawRoundRect(
            color = Color(0xFF5B786B),
            topLeft = Offset(w * 0.468f, midY - h * 0.028f),
            size = Size(w * 0.024f, h * 0.056f),
            cornerRadius = CornerRadius(w * 0.008f, w * 0.008f)
        )

        // Waveform Bar 1 (Sage)
        val barW = w * 0.028f
        drawRoundRect(
            color = Color(0xFF678678),
            topLeft = Offset(w * 0.51f, midY - h * 0.075f),
            size = Size(barW, h * 0.15f),
            cornerRadius = CornerRadius(barW / 2f, barW / 2f)
        )
        // Waveform Bar 2 (Tall Warm Taupe)
        drawRoundRect(
            color = Color(0xFFA79482),
            topLeft = Offset(w * 0.555f, midY - h * 0.11f),
            size = Size(barW, h * 0.22f),
            cornerRadius = CornerRadius(barW / 2f, barW / 2f)
        )
        // Waveform Bar 3 (Terracotta)
        drawRoundRect(
            color = Color(0xFFC89D84),
            topLeft = Offset(w * 0.60f, midY - h * 0.065f),
            size = Size(barW, h * 0.13f),
            cornerRadius = CornerRadius(barW / 2f, barW / 2f)
        )
        // Waveform Bar 4 (Short Terracotta)
        drawRoundRect(
            color = Color(0xFFC89D84),
            topLeft = Offset(w * 0.645f, midY - h * 0.032f),
            size = Size(barW, h * 0.064f),
            cornerRadius = CornerRadius(barW / 2f, barW / 2f)
        )

        // 7. Bottom-right Minimalist Unlocked Padlock (Lock Screen Availability)
        val lockX = w * 0.635f
        val lockY = h * 0.665f
        val lockW = w * 0.052f
        val lockH = h * 0.038f
        drawRoundRect(
            color = Color(0xFFC89D84),
            topLeft = Offset(lockX, lockY),
            size = Size(lockW, lockH),
            cornerRadius = CornerRadius(w * 0.01f, w * 0.01f)
        )
        drawArc(
            color = Color(0xFFC89D84),
            startAngle = 180f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = Offset(lockX + lockW * 0.15f, lockY - lockH * 0.7f),
            size = Size(lockW * 0.7f, lockH * 1.1f),
            style = Stroke(width = w * 0.012f, cap = StrokeCap.Round)
        )
    }
}
