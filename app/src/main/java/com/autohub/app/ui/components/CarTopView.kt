package com.autohub.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autohub.app.ui.theme.C

/**
 * Atlas Cross Sport side-profile silhouette with stats overlay.
 * Draws a recognizable SUV/crossover shape with tire PSI, temps,
 * door status, and fluid connection lines.
 */
@Composable
fun CarTopViewCanvas(
    tireFl: Int, tireFr: Int, tireRl: Int, tireRr: Int,
    tireTempFl: Int, tireTempFr: Int, tireTempRl: Int, tireTempRr: Int,
    allDoorsLocked: Boolean,
    modifier: Modifier = Modifier,
) {
    val tm = rememberTextMeasurer()

    Canvas(modifier.fillMaxWidth().height(200.dp)) {
        val w = size.width; val h = size.height
        val cx = w / 2f; val cy = h / 2f

        // ── Atlas Cross Sport side-profile silhouette ──
        val carLeft = cx - w * 0.38f
        val carRight = cx + w * 0.38f
        val carTop = cy - h * 0.18f
        val carBottom = cy + h * 0.15f
        val roofTop = cy - h * 0.38f
        val carW = carRight - carLeft
        val wheelRadius = h * 0.10f

        // Car body path — SUV crossover shape
        val body = Path().apply {
            // Start at front bumper bottom
            moveTo(carLeft, carBottom)
            // Front bumper curve
            quadraticBezierTo(carLeft - carW * 0.02f, carBottom, carLeft - carW * 0.01f, carBottom - h * 0.06f)
            // Front hood
            lineTo(carLeft + carW * 0.05f, carTop)
            // Hood slope up to windshield
            lineTo(carLeft + carW * 0.25f, carTop)
            // Windshield
            lineTo(carLeft + carW * 0.32f, roofTop)
            // Roof
            lineTo(carLeft + carW * 0.72f, roofTop + h * 0.02f)
            // Rear window slope (Cross Sport coupe-like)
            lineTo(carLeft + carW * 0.88f, carTop + h * 0.04f)
            // Rear pillar
            lineTo(carLeft + carW * 0.92f, carTop + h * 0.08f)
            // Trunk/rear
            lineTo(carRight, carBottom - h * 0.04f)
            // Rear bumper
            quadraticBezierTo(carRight + carW * 0.01f, carBottom, carRight, carBottom)
            // Bottom line (with wheel wells)
            lineTo(carLeft + carW * 0.80f, carBottom)
            // Rear wheel well
            quadraticBezierTo(carLeft + carW * 0.77f, carBottom - wheelRadius * 1.1f,
                carLeft + carW * 0.70f, carBottom)
            // Mid bottom
            lineTo(carLeft + carW * 0.32f, carBottom)
            // Front wheel well
            quadraticBezierTo(carLeft + carW * 0.28f, carBottom - wheelRadius * 1.1f,
                carLeft + carW * 0.20f, carBottom)
            // Back to start
            lineTo(carLeft, carBottom)
            close()
        }

        // Glow fill
        drawPath(body, C.Blue.copy(alpha = 0.06f))
        // Outline
        drawPath(body, C.Blue.copy(alpha = 0.35f), style = Stroke(1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Windshield line
        drawLine(C.Blue.copy(alpha = 0.15f),
            Offset(carLeft + carW * 0.25f, carTop),
            Offset(carLeft + carW * 0.32f, roofTop), 1f)

        // Rear window line
        drawLine(C.Blue.copy(alpha = 0.15f),
            Offset(carLeft + carW * 0.72f, roofTop + h * 0.02f),
            Offset(carLeft + carW * 0.88f, carTop + h * 0.04f), 1f)

        // Door line
        drawLine(C.Blue.copy(alpha = 0.10f),
            Offset(carLeft + carW * 0.48f, roofTop + h * 0.01f),
            Offset(carLeft + carW * 0.48f, carBottom - h * 0.02f),
            0.8f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))

        // Headlight
        drawRoundRect(C.Amber.copy(alpha = 0.4f),
            Offset(carLeft + carW * 0.02f, carTop + h * 0.02f),
            Size(carW * 0.06f, h * 0.04f), CornerRadius(3f))

        // Taillight
        drawRoundRect(C.Red.copy(alpha = 0.4f),
            Offset(carRight - carW * 0.06f, carTop + h * 0.06f),
            Size(carW * 0.05f, h * 0.04f), CornerRadius(3f))

        // ── Wheels ──
        val frontWheelX = carLeft + carW * 0.25f
        val rearWheelX = carLeft + carW * 0.75f
        val wheelY = carBottom

        // Front wheel
        drawCircle(C.TextFaint, wheelRadius, Offset(frontWheelX, wheelY))
        drawCircle(C.Blue.copy(alpha = 0.3f), wheelRadius, Offset(frontWheelX, wheelY), style = Stroke(1.5f))
        drawCircle(C.Blue.copy(alpha = 0.15f), wheelRadius * 0.5f, Offset(frontWheelX, wheelY))

        // Rear wheel
        drawCircle(C.TextFaint, wheelRadius, Offset(rearWheelX, wheelY))
        drawCircle(C.Blue.copy(alpha = 0.3f), wheelRadius, Offset(rearWheelX, wheelY), style = Stroke(1.5f))
        drawCircle(C.Blue.copy(alpha = 0.15f), wheelRadius * 0.5f, Offset(rearWheelX, wheelY))

        // ── Status dot (center of car) ──
        val sc = if (allDoorsLocked) C.Green else C.Amber
        drawCircle(sc.copy(alpha = 0.12f), 12f, Offset(cx, cy - h * 0.05f))
        drawCircle(sc, 4f, Offset(cx, cy - h * 0.05f))

        // ── Tire PSI labels ──
        data class TireLabel(val psi: Int, val temp: Int, val label: String, val x: Float, val y: Float)
        val tires = listOf(
            TireLabel(tireFl, tireTempFl, "FL", frontWheelX - w * 0.12f, cy - h * 0.32f),
            TireLabel(tireFr, tireTempFr, "FR", frontWheelX + w * 0.04f, cy + h * 0.32f),
            TireLabel(tireRl, tireTempRl, "RL", rearWheelX - w * 0.04f, cy - h * 0.32f),
            TireLabel(tireRr, tireTempRr, "RR", rearWheelX + w * 0.12f, cy + h * 0.32f),
        )

        for (t in tires) {
            val ok = t.psi in 32..38
            val tc = if (ok) C.Green else C.Amber

            // Connection line from label to wheel
            val wheelX = if (t.label.startsWith("F")) frontWheelX else rearWheelX
            drawLine(tc.copy(alpha = 0.2f), Offset(t.x, t.y), Offset(wheelX, wheelY), 0.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f)))

            // PSI value
            val psiL = tm.measure("${t.psi}",
                TextStyle(color = if (ok) C.TextPrimary else C.Amber, fontSize = 14.sp,
                    fontWeight = FontWeight.Light, textAlign = TextAlign.Center))
            drawText(psiL, topLeft = Offset(t.x - psiL.size.width / 2f, t.y - psiL.size.height / 2f - 2f))

            // Unit
            val unitL = tm.measure("PSI",
                TextStyle(color = C.TextMuted, fontSize = 7.sp,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center))
            drawText(unitL, topLeft = Offset(t.x - unitL.size.width / 2f, t.y + psiL.size.height / 2f - 4f))
        }

        // ── VW badge (center of car, above status dot) ──
        val vwL = tm.measure("VW",
            TextStyle(color = C.Blue.copy(alpha = 0.5f), fontSize = 10.sp,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, letterSpacing = 1.sp))
        drawText(vwL, topLeft = Offset(cx - vwL.size.width / 2f, cy - h * 0.18f))

        // ── Model name at bottom ──
        val nameL = tm.measure("ATLAS CROSS SPORT",
            TextStyle(color = C.TextFaint, fontSize = 8.sp,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, letterSpacing = 2.sp))
        drawText(nameL, topLeft = Offset(cx - nameL.size.width / 2f, h - nameL.size.height - 4f))
    }
}
