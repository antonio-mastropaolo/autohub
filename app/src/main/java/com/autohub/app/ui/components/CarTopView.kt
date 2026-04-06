package com.autohub.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autohub.app.ui.theme.C

@Composable
fun CarTopViewCanvas(
    tireFl: Int, tireFr: Int, tireRl: Int, tireRr: Int,
    tireTempFl: Int, tireTempFr: Int, tireTempRl: Int, tireTempRr: Int,
    allDoorsLocked: Boolean,
    modifier: Modifier = Modifier,
) {
    val tm = rememberTextMeasurer()

    Canvas(modifier.fillMaxWidth().height(180.dp)) {
        val w = size.width; val h = size.height
        val cx = w / 2f; val cy = h / 2f
        val bodyW = w * 0.3f; val bodyH = h * 0.75f

        // Car body
        drawRoundRect(C.Blue.copy(alpha = 0.06f),
            Offset(cx - bodyW / 2, cy - bodyH / 2), Size(bodyW, bodyH),
            CornerRadius(bodyW * 0.35f))
        drawRoundRect(C.GlassBorder,
            Offset(cx - bodyW / 2, cy - bodyH / 2), Size(bodyW, bodyH),
            CornerRadius(bodyW * 0.35f), style = Stroke(1.5f))

        // Center line
        drawLine(C.TextFaint,
            Offset(cx, cy - bodyH * 0.3f), Offset(cx, cy + bodyH * 0.3f),
            0.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))

        // Status dot
        val sc = if (allDoorsLocked) C.Green else C.Amber
        drawCircle(sc.copy(alpha = 0.15f), 10f, Offset(cx, cy))
        drawCircle(sc, 3.5f, Offset(cx, cy))

        // Tires with PSI + temperature
        data class Tire(val psi: Int, val temp: Int, val label: String, val x: Float, val y: Float)
        val tires = listOf(
            Tire(tireFl, tireTempFl, "FL", cx - bodyW * 0.9f, cy - bodyH * 0.28f),
            Tire(tireFr, tireTempFr, "FR", cx + bodyW * 0.9f, cy - bodyH * 0.28f),
            Tire(tireRl, tireTempRl, "RL", cx - bodyW * 0.9f, cy + bodyH * 0.28f),
            Tire(tireRr, tireTempRr, "RR", cx + bodyW * 0.9f, cy + bodyH * 0.28f),
        )

        for (t in tires) {
            val ok = t.psi in 32..38
            val tc = if (ok) C.Green else C.Amber
            val tw = 24f; val th = 42f

            // Tire rectangle
            drawRoundRect(tc.copy(alpha = 0.08f),
                Offset(t.x - tw / 2, t.y - th / 2), Size(tw, th), CornerRadius(5f))
            drawRoundRect(tc.copy(alpha = 0.5f),
                Offset(t.x - tw / 2, t.y - th / 2), Size(tw, th), CornerRadius(5f),
                style = Stroke(1f))

            // PSI value
            val psiL = tm.measure(t.psi.toString(),
                style = TextStyle(color = if (ok) C.TextPrimary else C.Amber, fontSize = 13.sp, fontWeight = FontWeight.Light, textAlign = TextAlign.Center))
            drawText(psiL, topLeft = Offset(t.x - psiL.size.width / 2f, t.y - psiL.size.height / 2f - 4f))

            // Temperature
            val tempL = tm.measure("${t.temp}\u00b0",
                style = TextStyle(color = C.TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center))
            drawText(tempL, topLeft = Offset(t.x - tempL.size.width / 2f, t.y + 4f))

            // Label
            val lblL = tm.measure(t.label,
                style = TextStyle(color = C.TextMuted, fontSize = 7.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, letterSpacing = 0.5.sp))
            drawText(lblL, topLeft = Offset(t.x - lblL.size.width / 2f, t.y + th / 2f + 3f))
        }
    }
}
