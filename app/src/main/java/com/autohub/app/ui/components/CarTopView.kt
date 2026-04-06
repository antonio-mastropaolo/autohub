package com.autohub.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autohub.app.ui.theme.AutoHubColors as C

@Composable
fun CarTopViewCanvas(
    tireFl: Int, tireFr: Int, tireRl: Int, tireRr: Int,
    allDoorsLocked: Boolean,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier.fillMaxWidth().height(180.dp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        // Car body
        val bodyW = w * 0.3f
        val bodyH = h * 0.75f
        drawRoundRect(
            color = C.Blue.copy(alpha = 0.06f),
            topLeft = Offset(cx - bodyW / 2, cy - bodyH / 2),
            size = Size(bodyW, bodyH),
            cornerRadius = CornerRadius(bodyW * 0.35f)
        )
        drawRoundRect(
            color = C.GlassBorder,
            topLeft = Offset(cx - bodyW / 2, cy - bodyH / 2),
            size = Size(bodyW, bodyH),
            cornerRadius = CornerRadius(bodyW * 0.35f),
            style = Stroke(width = 1.5f)
        )

        // Center line
        drawLine(
            color = C.TextFaint,
            start = Offset(cx, cy - bodyH * 0.3f),
            end = Offset(cx, cy + bodyH * 0.3f),
            strokeWidth = 0.5f,
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
        )

        // Status dot center
        val statusColor = if (allDoorsLocked) C.Green else C.Amber
        drawCircle(color = statusColor.copy(alpha = 0.15f), radius = 10f, center = Offset(cx, cy))
        drawCircle(color = statusColor, radius = 3.5f, center = Offset(cx, cy))

        // Tires
        data class TireInfo(val psi: Int, val label: String, val x: Float, val y: Float)
        val tires = listOf(
            TireInfo(tireFl, "FL", cx - bodyW * 0.9f, cy - bodyH * 0.28f),
            TireInfo(tireFr, "FR", cx + bodyW * 0.9f, cy - bodyH * 0.28f),
            TireInfo(tireRl, "RL", cx - bodyW * 0.9f, cy + bodyH * 0.28f),
            TireInfo(tireRr, "RR", cx + bodyW * 0.9f, cy + bodyH * 0.28f),
        )

        for (tire in tires) {
            val ok = tire.psi in 32..38
            val tireColor = if (ok) C.Green else C.Amber
            val tireW = 22f
            val tireH = 38f

            drawRoundRect(
                color = tireColor.copy(alpha = 0.08f),
                topLeft = Offset(tire.x - tireW / 2, tire.y - tireH / 2),
                size = Size(tireW, tireH),
                cornerRadius = CornerRadius(5f)
            )
            drawRoundRect(
                color = tireColor.copy(alpha = 0.5f),
                topLeft = Offset(tire.x - tireW / 2, tire.y - tireH / 2),
                size = Size(tireW, tireH),
                cornerRadius = CornerRadius(5f),
                style = Stroke(width = 1f)
            )

            // PSI value
            val psiLayout = textMeasurer.measure(
                text = tire.psi.toString(),
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Light,
                    color = if (ok) C.TextPrimary else C.Amber,
                    textAlign = TextAlign.Center,
                )
            )
            drawText(
                textLayoutResult = psiLayout,
                topLeft = Offset(tire.x - psiLayout.size.width / 2f, tire.y - psiLayout.size.height / 2f - 2f)
            )

            // Label
            val lblLayout = textMeasurer.measure(
                text = tire.label,
                style = TextStyle(
                    fontSize = 5.sp,
                    fontWeight = FontWeight.Bold,
                    color = C.TextMuted,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.5.sp,
                )
            )
            drawText(
                textLayoutResult = lblLayout,
                topLeft = Offset(tire.x - lblLayout.size.width / 2f, tire.y + 8f)
            )
        }
    }
}
