package com.autohub.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autohub.app.data.CarState
import com.autohub.app.ui.components.*
import com.autohub.app.ui.theme.C

@Composable
fun PerformanceScreen(car: CarState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

        // ── Main gauges + G-force ──
        GlassCard(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ArcGauge(car.speed.toFloat(), 160f, "SPEED", "MPH", C.Blue, size = 155.dp)
                GForceIndicator(car.gForceX, car.gForceY, C.Cyan, size = 90.dp)
                ArcGauge(car.rpm.toFloat(), 8000f, "RPM", "\u00d71000",
                    if (car.rpm > 5000) C.Red else C.TextSub, size = 155.dp)
            }
        }

        // ── Power / Torque / Throttle ──
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PerfStatCard("Power", "${car.power}", "HP", C.Blue, car.power / 200f, Modifier.weight(1f))
            PerfStatCard("Torque", "${car.torque}", "lb-ft", C.Cyan, car.torque / 300f, Modifier.weight(1f))
            PerfStatCard("Throttle", "${car.throttle.toInt()}", "%", C.Amber, car.throttle / 100f, Modifier.weight(1f))
        }

        // ── Records + Trend ──
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassCard(Modifier.weight(1f)) {
                LabelText("PERFORMANCE RECORDS")
                Spacer(Modifier.height(6.dp))
                RecordRow("Peak Speed", "${car.peakSpeed} MPH", C.Blue)
                RecordRow("Peak RPM", "${car.peakRpm}", C.Red)
                RecordRow("0-60 EST", "7.2s", C.Green)
                RecordRow("\u00bc Mile EST", "15.4s", C.Amber)
                RecordRow("Drive Score", "${car.drivingScore}/100", C.Cyan)
            }

            GlassCard(Modifier.weight(0.7f)) {
                LabelText("SPEED HISTORY")
                Spacer(Modifier.height(8.dp))
                SparkLine(
                    car.speedHistory, C.Blue,
                    Modifier.fillMaxWidth().height(60.dp)
                )
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("2m ago", style = TextStyle(fontSize = 10.sp, color = C.TextMuted))
                    Text("now", style = TextStyle(fontSize = 10.sp, color = C.TextMuted))
                }
            }
        }
    }
}

@Composable
private fun PerfStatCard(
    label: String, value: String, unit: String,
    color: Color, pct: Float, modifier: Modifier,
) {
    GlassCard(modifier) {
        LabelText(label)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.ExtraLight, color = C.TextPrimary))
            Text(unit, style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = C.TextMuted),
                modifier = Modifier.padding(start = 2.dp, bottom = 3.dp))
        }
        Spacer(Modifier.height(4.dp))
        ProgressBar(pct, 1f, color)
    }
}

@Composable
private fun RecordRow(label: String, value: String, color: Color) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        Arrangement.SpaceBetween, Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            StatusDot(color, 4.dp)
            Text(label, style = TextStyle(fontSize = 14.sp, color = C.TextSub, fontWeight = FontWeight.Medium))
        }
        Text(value, style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Light, color = C.TextPrimary))
    }
}
