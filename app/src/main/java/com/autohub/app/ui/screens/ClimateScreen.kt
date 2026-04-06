package com.autohub.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autohub.app.data.CarState
import com.autohub.app.ui.components.*
import com.autohub.app.ui.theme.AutoHubColors as C

@Composable
fun ClimateScreen(car: CarState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Main climate card with temp knob
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusDot(color = if (car.acOn) C.Cyan else C.TextMuted, size = 6.dp)
                    Text(
                        "CLIMATE CONTROL ${if (car.acOn) "ACTIVE" else "OFF"}",
                        style = TextStyle(
                            fontSize = 8.sp, fontWeight = FontWeight.Bold,
                            color = if (car.acOn) C.Cyan else C.TextMuted,
                            letterSpacing = 1.sp
                        )
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Temperature knob
                TempKnob(value = car.cabinTemp, target = car.acTarget)

                Spacer(Modifier.height(4.dp))
                Text(
                    "Target: ${car.acTarget}°F",
                    style = TextStyle(fontSize = 10.sp, color = C.TextSub)
                )

                Spacer(Modifier.height(10.dp))

                // Fan speed bars
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    for (i in 1..5) {
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .height((3 + i * 2.5f).dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (i <= car.fanSpeed) C.Cyan else C.Glass)
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                LabelText("Fan Speed")
            }
        }

        // Bottom row: Outside, Seat Heat, Defrost
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassCard(modifier = Modifier.weight(1f)) {
                LabelText("Outside")
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${car.outsideTemp}", style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Thin))
                    Text("°F", style = TextStyle(fontSize = 10.sp, color = C.TextMuted), modifier = Modifier.padding(bottom = 2.dp))
                }
            }

            GlassCard(modifier = Modifier.weight(1f)) {
                LabelText("Seat Heat")
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SeatHeatIndicator("D", car.driverSeatHeat)
                    Box(Modifier.width(1.dp).height(24.dp).background(C.TextFaint))
                    SeatHeatIndicator("P", car.passSeatHeat)
                }
            }

            GlassCard(modifier = Modifier.weight(1f)) {
                LabelText("Defrost")
                Spacer(Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DefrostRow("Front", car.frontDefrost)
                    DefrostRow("Rear", car.rearDefrost)
                }
            }
        }

        // Thermal overview
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            LabelText("Thermal Overview")
            Spacer(Modifier.height(8.dp))

            data class ThermalItem(val label: String, val value: Int, val max: Float, val color: androidx.compose.ui.graphics.Color)
            val thermals = listOf(
                ThermalItem("Cabin", car.cabinTemp, 100f, C.Cyan),
                ThermalItem("Engine", car.engineTemp, 260f, if (car.engineTemp > 210) C.Red else C.Amber),
                ThermalItem("Oil", car.oilTemp, 280f, if (car.oilTemp > 230) C.Red else C.Green),
                ThermalItem("Battery", car.batteryTemp, 150f, C.Blue),
            )

            for (t in thermals) {
                Column(modifier = Modifier.padding(bottom = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(t.label, style = TextStyle(fontSize = 9.sp, color = C.TextSub, fontWeight = FontWeight.Medium))
                        Text("${t.value}°F", style = TextStyle(fontSize = 9.sp, color = C.TextPrimary))
                    }
                    Spacer(Modifier.height(2.dp))
                    ProgressBar(value = t.value.toFloat(), maxValue = t.max, color = t.color, height = 2.dp)
                }
            }
        }
    }
}

@Composable
private fun TempKnob(value: Int, target: Int) {
    val pct by animateFloatAsState(
        targetValue = ((value - 40f) / 60f).coerceIn(0f, 1f),
        animationSpec = tween(1200),
        label = "tempKnob"
    )
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = Modifier.size(130.dp)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = size.minDimension / 2f - 14f
        val sweep = 300f
        val start = 120f

        // Bg glow
        drawCircle(color = C.Cyan.copy(alpha = 0.04f), radius = radius + 12f, center = Offset(cx, cy))

        // Track
        drawArc(
            color = C.Glass,
            startAngle = start, sweepAngle = sweep, useCenter = false,
            style = Stroke(width = 5f, cap = StrokeCap.Round),
            topLeft = Offset(cx - radius, cy - radius),
            size = Size(radius * 2, radius * 2)
        )

        // Active
        drawArc(
            color = C.Cyan,
            startAngle = start, sweepAngle = sweep * pct, useCenter = false,
            style = Stroke(width = 5f, cap = StrokeCap.Round),
            topLeft = Offset(cx - radius, cy - radius),
            size = Size(radius * 2, radius * 2)
        )

        // Value
        val valLayout = textMeasurer.measure(
            "$value",
            style = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Thin, color = C.TextPrimary, textAlign = TextAlign.Center)
        )
        drawText(valLayout, topLeft = Offset(cx - valLayout.size.width / 2f, cy - valLayout.size.height / 2f - 4f))

        val unitLayout = textMeasurer.measure(
            "°F",
            style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = C.TextMuted, textAlign = TextAlign.Center)
        )
        drawText(unitLayout, topLeft = Offset(cx - unitLayout.size.width / 2f, cy + valLayout.size.height / 2f - 8f))
    }
}

@Composable
private fun SeatHeatIndicator(label: String, level: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = TextStyle(fontSize = 7.sp, fontWeight = FontWeight.Bold, color = C.TextMuted))
        Spacer(Modifier.height(3.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            for (i in 1..3) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (i <= level) C.Amber else C.Glass)
                )
            }
        }
    }
}

@Composable
private fun DefrostRow(label: String, on: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        StatusDot(color = if (on) C.Amber else C.TextMuted, size = 3.dp)
        Text(label, style = TextStyle(fontSize = 10.sp, color = if (on) C.TextSub else C.TextMuted))
    }
}
