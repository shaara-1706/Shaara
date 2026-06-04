package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * An elegant minimalist line-art Mogra Jasmine Flower logo drawn dynamically.
 * Perfectly balances light and dark schemes and acts as a central botanical logo.
 */
@Composable
fun MograFlowerLogo(
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    strokeColor: Color = MaterialTheme.colorScheme.secondary,
    shadowColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
) {
    Canvas(
        modifier = modifier.size(size)
    ) {
        val centerX = this.size.width / 2f
        val centerY = this.size.height / 2f
        val outerRadius = this.size.width * 0.42f
        val innerCircleRadius = this.size.width * 0.08f
        val strokeWidth = this.size.width * 0.024f

        // Draw delicate background halo / shadow
        drawCircle(
            color = shadowColor,
            radius = outerRadius * 1.15f,
            center = this.center
        )

        // Draw 8 elegant star-like rounded jasmine petals rotating radially
        val numPetals = 8
        for (i in 0 until numPetals) {
            val angleRad = Math.toRadians((i * (360.0 / numPetals))).toFloat()
            
            // Outer point of petal
            val tipX = centerX + outerRadius * cos(angleRad)
            val tipY = centerY + outerRadius * sin(angleRad)
            
            // Side arc control coordinates to form leaf/jasmine drop shapes
            val angleLeft = angleRad - 0.28f
            val angleRight = angleRad + 0.28f
            val midRadius = outerRadius * 0.58f
            
            val leftX = centerX + midRadius * cos(angleLeft)
            val leftY = centerY + midRadius * sin(angleLeft)
            
            val rightX = centerX + midRadius * cos(angleRight)
            val rightY = centerY + midRadius * sin(angleRight)

            // Draw left edge
            drawLine(
                color = strokeColor,
                start = androidx.compose.ui.geometry.Offset(centerX, centerY),
                end = androidx.compose.ui.geometry.Offset(leftX, leftY),
                strokeWidth = strokeWidth * 0.65f,
                cap = StrokeCap.Round
            )
            
            // Draw right edge
            drawLine(
                color = strokeColor,
                start = androidx.compose.ui.geometry.Offset(centerX, centerY),
                end = androidx.compose.ui.geometry.Offset(rightX, rightY),
                strokeWidth = strokeWidth * 0.65f,
                cap = StrokeCap.Round
            )

            // Dynamic Petal Loops
            drawOval(
                color = strokeColor,
                topLeft = androidx.compose.ui.geometry.Offset(
                    centerX + (outerRadius * 0.25f) * cos(angleRad) - (outerRadius * 0.16f),
                    centerY + (outerRadius * 0.25f) * sin(angleRad) - (outerRadius * 0.16f)
                ),
                size = androidx.compose.ui.geometry.Size(outerRadius * 0.32f, outerRadius * 0.32f),
                style = Stroke(width = strokeWidth * 0.8f)
            )

            // Floral center connecting line-art
            drawLine(
                color = strokeColor,
                start = androidx.compose.ui.geometry.Offset(centerX, centerY),
                end = androidx.compose.ui.geometry.Offset(tipX, tipY),
                strokeWidth = strokeWidth * 0.7f,
                cap = StrokeCap.Round
            )
        }

        // Draw botanical golden center bud circle
        drawCircle(
            color = strokeColor,
            radius = innerCircleRadius,
            style = Stroke(width = strokeWidth)
        )
        
        drawCircle(
            color = shadowColor,
            radius = innerCircleRadius * 0.5f
        )
    }
}
