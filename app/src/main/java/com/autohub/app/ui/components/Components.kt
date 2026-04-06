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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autohub.app.ui.theme.C
import kotlin.math.cos
import kotlin.math.sin

// ═══════════════════════════════════════════════════════════════
//  BASIC BUILDING BLOCKS
// ═══════════════════════════════════════════════════════════════

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(C.SurfaceAlpha)
            .border(1.dp, C.GlassBorder, RoundedCornerShape(14.dp))
    ) {
        Column(Modifier.padding(12.dp)) { content() }
    }
}

@Composable
fun LabelText(text: String) {
    Text(
        text.uppercase(),
        style = TextStyle(
            fontSize = 7.sp, fontWeight = FontWeight.Bold,
            color = C.TextMuted, letterSpacing = 1.2.sp
        )
    )
}

@Composable
fun StatusDot(color: Color, size: Dp = 5.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color))
}

@Composable
fun Pill(text: String, color: Color) {
    Box(
        Modifier.clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(
            text.uppercase(),
            style = TextStyle(color = color, fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
        )
    }
}

@Composable
fun ProgressBar(
    value: Float, maxValue: Float = 100f, color: Color = C.Blue,
    height: Dp = 3.dp, modifier: Modifier = Modifier,
) {
    val pct by animateFloatAsState(
        (value / maxValue).coerceIn(0f, 1f), tween(1200), label = "bar"
    )
    Box(
        modifier.fillMaxWidth().height(height)
            .clip(RoundedCornerShape(height))
            .background(Color.White.copy(alpha = 0.04f))
    ) {
        Box(
            Modifier.fillMaxHeight().fillMaxWidth(pct)
                .clip(RoundedCornerShape(height))
                .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.7f), color)))
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  ARC GAUGE — large gauge with arc, ticks, needle
// ═══════════════════════════════════════════════════════════════

@Composable
fun ArcGauge(
    value: Float, maxValue: Float, label: String, unit: String,
    color: Color, modifier: Modifier = Modifier, size: Dp = 150.dp,
) {
    val pct by animateFloatAsState(
        (value / maxValue).coerceIn(0f, 1f), tween(1200), label = "gauge"
    )
    val tm = rememberTextMeasurer()

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Canvas(Modifier.size(size)) {
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val r = this.size.minDimension / 2f - 14f
            val sw = 6f; val sweep = 260f; val start = 140f

            // Outer subtle ring
            drawCircle(Color.White.copy(alpha = 0.02f), r + 6f, Offset(cx, cy), style = Stroke(0.5f))

            // Background track
            drawArc(color = Color.White.copy(alpha = 0.04f), startAngle = start, sweepAngle = sweep, useCenter = false,
                style = Stroke(sw, cap = StrokeCap.Round),
                topLeft = Offset(cx - r, cy - r), size = Size(r * 2, r * 2))

            // Glow arc
            drawArc(color = color.copy(alpha = 0.1f), startAngle = start, sweepAngle = sweep * pct, useCenter = false,
                style = Stroke(sw + 10f, cap = StrokeCap.Round),
                topLeft = Offset(cx - r, cy - r), size = Size(r * 2, r * 2))

            // Active arc
            drawArc(
                brush = Brush.sweepGradient(listOf(color.copy(alpha = 0.6f), color), Offset(cx, cy)),
                startAngle = start, sweepAngle = sweep * pct, useCenter = false,
                style = Stroke(sw, cap = StrokeCap.Round),
                topLeft = Offset(cx - r, cy - r), size = Size(r * 2, r * 2))

            // Tick marks
            for (i in 0..26) {
                val a = Math.toRadians((start + i / 26f * sweep).toDouble())
                val major = i % 2 == 0
                val iR = r - if (major) 14f else 9f
                val oR = r - 3f
                val tc = if (i / 26f <= pct) color else C.TextFaint
                drawLine(tc,
                    Offset(cx + cos(a).toFloat() * iR, cy + sin(a).toFloat() * iR),
                    Offset(cx + cos(a).toFloat() * oR, cy + sin(a).toFloat() * oR),
                    if (major) 1.5f else 0.6f, StrokeCap.Round)
            }

            // Needle
            val na = Math.toRadians((start + pct * sweep).toDouble())
            drawLine(color.copy(alpha = 0.85f), Offset(cx, cy),
                Offset(cx + cos(na).toFloat() * (r - 18f), cy + sin(na).toFloat() * (r - 18f)),
                2f, StrokeCap.Round)

            // Center hub
            drawCircle(C.Surface, 7f, Offset(cx, cy))
            drawCircle(color, 7f, Offset(cx, cy), style = Stroke(1.5f))

            // Value text
            val vl = tm.measure(value.toInt().toString(),
                TextStyle(color = C.TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.ExtraLight, textAlign = TextAlign.Center))
            drawText(vl, Offset(cx - vl.size.width / 2f, cy + 14f))

            // Unit text
            val ul = tm.measure(unit,
                TextStyle(color = C.TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, textAlign = TextAlign.Center))
            drawText(ul, Offset(cx - ul.size.width / 2f, cy + 42f))
        }

        Text(label, style = TextStyle(color = C.TextSub, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
            modifier = Modifier.offset(y = (-4).dp))
    }
}

// ═══════════════════════════════════════════════════════════════
//  MINI RING GAUGE — compact ring for stat cards
// ═══════════════════════════════════════════════════════════════

@Composable
fun MiniRingGauge(
    value: Float, maxValue: Float, color: Color,
    modifier: Modifier = Modifier, size: Dp = 32.dp,
) {
    val pct by animateFloatAsState(
        (value / maxValue).coerceIn(0f, 1f), tween(1200), label = "mini"
    )
    Canvas(modifier.size(size)) {
        val cx = this.size.width / 2f; val cy = this.size.height / 2f
        val r = this.size.minDimension / 2f - 4f
        drawArc(color = Color.White.copy(alpha = 0.05f), startAngle = 135f, sweepAngle = 270f, useCenter = false,
            style = Stroke(3f, cap = StrokeCap.Round),
            topLeft = Offset(cx - r, cy - r), size = Size(r * 2, r * 2))
        drawArc(color = color, startAngle = 135f, sweepAngle = 270f * pct, useCenter = false,
            style = Stroke(3f, cap = StrokeCap.Round),
            topLeft = Offset(cx - r, cy - r), size = Size(r * 2, r * 2))
    }
}

// ═══════════════════════════════════════════════════════════════
//  SPARK LINE — tiny area chart for trends
// ═══════════════════════════════════════════════════════════════

@Composable
fun SparkLine(data: List<Float>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (data.size < 2) return@Canvas
        val w = size.width; val h = size.height
        val mx = data.max(); val mn = data.min()
        val range = if (mx - mn > 0) mx - mn else 1f
        val step = w / (data.size - 1)
        val line = Path(); val fill = Path()

        data.forEachIndexed { i, v ->
            val x = i * step
            val y = h - ((v - mn) / range) * h * 0.85f - h * 0.05f
            if (i == 0) { line.moveTo(x, y); fill.moveTo(x, h); fill.lineTo(x, y) }
            else { line.lineTo(x, y); fill.lineTo(x, y) }
        }
        fill.lineTo(w, h); fill.close()
        drawPath(fill, color.copy(alpha = 0.08f))
        drawPath(line, color, style = Stroke(1.5f, cap = StrokeCap.Round))
    }
}

// ═══════════════════════════════════════════════════════════════
//  G-FORCE INDICATOR — circular display with position dot
// ═══════════════════════════════════════════════════════════════

@Composable
fun GForceIndicator(
    gX: Float, gY: Float, color: Color,
    modifier: Modifier = Modifier, size: Dp = 100.dp,
) {
    val tm = rememberTextMeasurer()
    Canvas(modifier.size(size)) {
        val cx = this.size.width / 2f; val cy = this.size.height / 2f
        val r = this.size.minDimension / 2f - 8f

        for (i in 1..3) drawCircle(Color.White.copy(alpha = 0.025f), r * i / 3f, Offset(cx, cy), style = Stroke(0.5f))
        drawLine(Color.White.copy(alpha = 0.04f), Offset(cx - r, cy), Offset(cx + r, cy), 0.5f)
        drawLine(Color.White.copy(alpha = 0.04f), Offset(cx, cy - r), Offset(cx, cy + r), 0.5f)

        val dx = cx + (gX / 1.5f).coerceIn(-1f, 1f) * r
        val dy = cy - (gY / 1.5f).coerceIn(-1f, 1f) * r
        drawCircle(color.copy(alpha = 0.2f), 10f, Offset(dx, dy))
        drawCircle(color, 4f, Offset(dx, dy))

        val gl = tm.measure("G", TextStyle(color = C.TextMuted, fontSize = 6.sp, fontWeight = FontWeight.Bold))
        drawText(gl, Offset(cx - gl.size.width / 2f, cy + r + 2f))
    }
}

// ═══════════════════════════════════════════════════════════════
//  COMPASS ROSE — heading display with cardinal directions
// ═══════════════════════════════════════════════════════════════

@Composable
fun CompassRose(
    heading: Int, color: Color,
    modifier: Modifier = Modifier, size: Dp = 140.dp,
) {
    val tm = rememberTextMeasurer()
    Canvas(modifier.size(size)) {
        val cx = this.size.width / 2f; val cy = this.size.height / 2f
        val r = this.size.minDimension / 2f - 16f

        drawCircle(Color.White.copy(alpha = 0.04f), r + 4f, Offset(cx, cy), style = Stroke(1f))

        // Rotated tick ring
        rotate(-heading.toFloat(), Offset(cx, cy)) {
            for (i in 0 until 72) {
                val deg = i * 5f
                val a = Math.toRadians((deg - 90).toDouble())
                val cardinal = deg % 90f == 0f
                val major = deg % 45f == 0f
                val iR = r - if (cardinal) 16f else if (major) 12f else 5f
                val oR = r - 1f
                val tc = if (cardinal) color else if (major) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f)
                drawLine(tc,
                    Offset(cx + cos(a).toFloat() * iR, cy + sin(a).toFloat() * iR),
                    Offset(cx + cos(a).toFloat() * oR, cy + sin(a).toFloat() * oR),
                    if (cardinal) 2f else if (major) 1f else 0.5f)
            }
        }

        // Cardinal labels — kept upright
        for ((lbl, bearing) in listOf("N" to 0f, "E" to 90f, "S" to 180f, "W" to 270f)) {
            val a = Math.toRadians((bearing - heading - 90).toDouble())
            val lr = r - 26f
            val isN = lbl == "N"
            val lay = tm.measure(lbl, TextStyle(color = if (isN) color else C.TextSub, fontSize = if (isN) 13.sp else 9.sp, fontWeight = FontWeight.Bold))
            val px = cx + cos(a).toFloat() * lr - lay.size.width / 2f
            val py = cy + sin(a).toFloat() * lr - lay.size.height / 2f
            drawText(lay, Offset(px, py))
        }

        // Center heading value
        val hl = tm.measure("$heading\u00b0", TextStyle(color = C.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Thin, textAlign = TextAlign.Center))
        drawText(hl, Offset(cx - hl.size.width / 2f, cy - hl.size.height / 2f))
    }
}

// ═══════════════════════════════════════════════════════════════
//  TEMPERATURE KNOB — climate dial
// ═══════════════════════════════════════════════════════════════

@Composable
fun TempKnob(
    value: Int, target: Int, color: Color = C.Cyan,
    modifier: Modifier = Modifier, size: Dp = 110.dp,
) {
    val pct by animateFloatAsState(
        ((value - 40f) / 60f).coerceIn(0f, 1f), tween(1200), label = "knob"
    )
    val tm = rememberTextMeasurer()
    Canvas(modifier.size(size)) {
        val cx = this.size.width / 2f; val cy = this.size.height / 2f
        val r = this.size.minDimension / 2f - 12f

        drawCircle(color.copy(alpha = 0.04f), r + 10f, Offset(cx, cy))
        drawArc(color = C.Glass, startAngle = 120f, sweepAngle = 300f, useCenter = false, style = Stroke(4f, cap = StrokeCap.Round),
            topLeft = Offset(cx - r, cy - r), size = Size(r * 2, r * 2))
        drawArc(color = color, startAngle = 120f, sweepAngle = 300f * pct, useCenter = false, style = Stroke(4f, cap = StrokeCap.Round),
            topLeft = Offset(cx - r, cy - r), size = Size(r * 2, r * 2))

        val vl = tm.measure("$value", TextStyle(color = C.TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.Thin, textAlign = TextAlign.Center))
        drawText(vl, Offset(cx - vl.size.width / 2f, cy - vl.size.height / 2f - 4f))
        val ul = tm.measure("\u00b0F", TextStyle(color = C.TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center))
        drawText(ul, Offset(cx - ul.size.width / 2f, cy + vl.size.height / 2f - 10f))
    }
}

// ═══════════════════════════════════════════════════════════════
//  EQ VISUALIZER — animated equalizer bars
// ═══════════════════════════════════════════════════════════════

@Composable
fun EQVisualizer(bands: List<Float>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val gap = 3f
        val bw = (size.width - gap * (bands.size - 1)) / bands.size
        bands.forEachIndexed { i, level ->
            val x = i * (bw + gap)
            val bh = level * size.height
            drawRoundRect(color.copy(alpha = 0.5f + level * 0.5f),
                Offset(x, size.height - bh), Size(bw, bh), CornerRadius(2f))
        }
    }
}
