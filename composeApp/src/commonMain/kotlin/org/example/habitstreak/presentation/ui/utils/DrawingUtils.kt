package org.example.habitstreak.presentation.ui.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import kotlin.math.sqrt

/**
 * Common drawing utilities for UI components
 */

/**
 * Draws a striped pattern for inactive elements
 * @param color Color of the stripes
 * @param boxSize Size of the container box
 * @param cornerRadius Corner radius of the container (currently unused but kept for future use)
 * @param stripeWidth Width of each stripe line (increased for thicker appearance)
 * @param stripeSpacing Spacing between stripe lines (increased for better visibility)
 */
fun DrawScope.drawStripedPattern(
    color: Color,
    boxSize: Dp,
    cornerRadius: Dp,
    stripeWidth: Float = 3f, // Increased from 2f to 3f for thicker stripes
    stripeSpacing: Float = 10f // Increased from 8f to 10f for more spacing
) {
    val boxSizePx = boxSize.toPx()

    // Calculate the number of stripes needed to cover the diagonal of the box
    val diagonal = sqrt((boxSizePx * boxSizePx * 2).toDouble()).toFloat()
    val totalStripes = (diagonal / stripeSpacing).toInt() + 2

    // Draw 45 degree diagonal stripes
    for (i in -totalStripes..totalStripes) {
        val startX = i * stripeSpacing
        val startY = 0f
        val endX = startX + boxSizePx
        val endY = boxSizePx

        drawLine(
            color = color,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = stripeWidth
        )
    }
}