package com.autohub.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autohub.app.ui.theme.AutoHubColors as C
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArcGauge(
    value: Float,
    maxValue: Float,
    label: String,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 150.dp,
) {
    val animatedPct by animateFloatAsState(
        targetValue = (value / maxValue).coerceIn(0f, 1f),
        animationSpec = tween(1200),
        label = "gauge"
    )
    val textMeasurer = rememberTextMeasurer()

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Canvas(modifier = Modifier.size(size)) {
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val radius = this.size.minDimension / 2f - 14f
            val strokeW = 7f
            val sweepAngle = 260f
            val startAngle = 140f

            // Background track
            drawArc(
                color = Color.White.copy(alpha = 0.04f),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeW, cap = StrokeCap.Round),
                topLeft = Offset(cx - radius, cy - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )

            // Active arc
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(color.copy(alpha = 0.7f), color),
                    center = Offset(cx, cy)
                ),
                startAngle = startAngle,
                sweepAngle = sweepAngle * animatedPct,
                useCenter = false,
                style = Stroke(width = strokeW, cap = StrokeCap.Round),
                topLeft = Offset(cx - radius, cy - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )

            // Glow arc
            drawArc(
                color = color.copy(alpha = 0.12f),
                startAngle = startAngle,
                sweepAngle = sweepAngle * animatedPct,
                useCenter = false,
                style = Stroke(width = strokeW + 8f, cap = StrokeCap.Round),
                topLeft = Offset(cx - radius, cy - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )

            // Tick marks
            val numTicks = 13
            for (i in 0..numTicks) {
                val angle = Math.toRadians((startAngle + (i.toFloat() / numTicks) * sweepAngle).toDouble())
                val isMajor = i % 2 == 0
                val innerR = radius - if (isMajor) 14f else 10f
                val outerR = radius - 4f
                val tickColor = if (i.toFloat() / numTicks <= animatedPct) color else C.TextFaint

                drawLine(
                    color = tickColor,
                    start = Offset(
                        cx + cos(angle).toFloat() * innerR,
                        cy + sin(angle).toFloat() * innerR
                    ),
                    end = Offset(
                        cx + cos(angle).toFloat() * outerR,
                        cy + sin(angle).toFloat() * outerR
                    ),
                    strokeWidth = if (isMajor) 1.5f else 0.8f,
                    cap = StrokeCap.Round
                )
            }

            // Needle
            val needleAngle = Math.toRadians((startAngle + animatedPct * sweepAngle).toDouble())
            val needleLen = radius - 20f
            val needleTip = Offset(
                cx + cos(needleAngle).toFloat() * needleLen,
                cy + sin(needleAngle).toFloat() * needleLen
            )

            drawLine(
                color = color.copy(alpha = 0.9f),
                start = Offset(cx, cy),
                end = needleTip,
                strokeWidth = 2.5f,
                cap = StrokeCap.Round
            )

            // Center dot
            drawCircle(color = C.Surface, radius = 6f, center = Offset(cx, cy))
            drawCircle(
                color = color,
                radius = 6f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5f)
            )

            // Value text
            val valueText = value.toInt().toString()
            val valueLayout = textMeasurer.measure(
                text = valueText,
                style = TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraLight,
                    color = C.TextPrimary,
                    textAlign = TextAlign.Center
                )
            )
            drawText(
                textLayoutResult = valueLayout,
                topLeft = Offset(
                    cx - valueLayout.size.width / 2f,
                    cy + 16f
                )
            )

            // Unit text
            val unitLayout = textMeasurer.measure(
                text = unit,
                style = TextStyle(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = C.TextMuted,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )
            )
            drawText(
                textLayoutResult = unitLayout,
                topLeft = Offset(
                    cx - unitLayout.size.width / 2f,
                    cy + 46f
                )
            )
        }

        Text(
            text = label,
            style = TextStyle(
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = C.TextSub,
                letterSpacing = 1.2.sp
            ),
            modifier = Modifier.offset(y = (-4).dp)
        )
    }
}

@Composable
fun MiniRingGauge(
    value: Float,
    maxValue: Float,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
) {
    val animatedPct by animateFloatAsState(
        targetValue = (value / maxValue).coerceIn(0f, 1f),
        animationSpec = tween(1200),
        label = "miniGauge"
    )

    Canvas(modifier = modifier.size(size)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val radius = this.size.minDimension / 2f - 4f
        val sweep = 270f
        val start = 135f

        drawArc(
            color = Color.White.copy(alpha = 0.05f),
            startAngle = start, sweepAngle = sweep, useCenter = false,
            style = Stroke(width = 3f, cap = StrokeCap.Round),
            topLeft = Offset(cx - radius, cy - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
        )
        drawArc(
            color = color,
            startAngle = start, sweepAngle = sweep * animatedPct, useCenter = false,
            style = Stroke(width = 3f, cap = StrokeCap.Round),
            topLeft = Offset(cx - radius, cy - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
        )
    }
}

@Composable
fun ProgressBar(
    value: Float,
    maxValue: Float = 100f,
    color: Color = C.Blue,
    height: Dp = 3.dp,
    modifier: Modifier = Modifier,
) {
    val animatedPct by animateFloatAsState(
        targetValue = (value / maxValue).coerceIn(0f, 1f),
        animationSpec = tween(1200),
        label = "bar"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height))
            .background(Color.White.copy(alpha = 0.04f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedPct)
                .clip(RoundedCornerShape(height))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(color.copy(alpha = 0.7f), color)
                    )
                )
        )
    }
}

@Composable
fun StatusDot(color: Color, size: Dp = 5.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    accentColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(C.SurfaceAlpha)
            .border(1.dp, C.GlassBorder, RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp, 14.dp)) {
            content()
        }
    }
}

@Composable
fun LabelText(text: String) {
    Text(
        text = text.uppercase(),
        style = TextStyle(
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            color = C.TextMuted,
            letterSpacing = 1.2.sp
        )
    )
}

@Composable
fun Pill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(
            text = text.uppercase(),
            style = TextStyle(
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                letterSpacing = 0.8.sp
            )
        )
    }
}
