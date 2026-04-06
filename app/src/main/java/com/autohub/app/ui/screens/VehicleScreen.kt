package com.autohub.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autohub.app.data.CarState
import com.autohub.app.ui.components.*
import com.autohub.app.ui.theme.C

@Composable
fun VehicleScreen(car: CarState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

        // ── Tires + Fluids ──
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassCard(Modifier.weight(1f)) {
                LabelText("TIRES & STATUS")
                Spacer(Modifier.height(4.dp))
                CarTopViewCanvas(
                    car.tireFl, car.tireFr, car.tireRl, car.tireRr,
                    car.tireTempFl, car.tireTempFr, car.tireTempRl, car.tireTempRr,
                    car.allDoorsLocked,
                )
            }
            GlassCard(Modifier.weight(1f)) {
                LabelText("FLUID LEVELS")
                Spacer(Modifier.height(6.dp))
                FluidRow("Engine Oil", car.oilLife, "${car.oilPressure} PSI")
                FluidRow("Brake Fluid", car.brakeFluid, null)
                FluidRow("Coolant", car.coolant, null)
                FluidRow("Transmission", car.transFluid, null)
                FluidRow("Washer", car.washerFluid, null)
                FluidRow("Power Steer", car.powerSteering, null)
            }
        }

        // ── Service + Electrical ──
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassCard(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Column {
                        LabelText("NEXT SERVICE")
                        Spacer(Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("%,d".format(car.serviceIn), TextStyle(20.sp, FontWeight.ExtraLight))
                            Text("mi", TextStyle(7.sp, FontWeight.Bold, C.TextMuted),
                                Modifier.padding(start = 2.dp, bottom = 2.dp))
                        }
                    }
                    ProgressBar(car.serviceIn.toFloat(), 5000f,
                        if (car.serviceIn > 1500) C.Green else C.Amber,
                        modifier = Modifier.width(60.dp))
                    Pill(if (car.serviceIn > 1500) "Good" else "Due Soon",
                        if (car.serviceIn > 1500) C.Green else C.Amber)
                }
            }
            GlassCard(Modifier.weight(0.8f)) {
                LabelText("ELECTRICAL")
                Spacer(Modifier.height(4.dp))
                ElecRow("Battery", "%.1fV".format(car.batteryVoltage), C.Green)
                ElecRow("Alternator", "%.1fV".format(car.alternatorVoltage), C.Blue)
                ElecRow("Health", "${car.batteryHealth}%",
                    if (car.batteryHealth > 80) C.Green else C.Amber)
            }
        }

        // ── Systems ──
        GlassCard(Modifier.fillMaxWidth()) {
            LabelText("SYSTEMS")
            Spacer(Modifier.height(6.dp))
            val systems = listOf(
                SysItem("Headlights", car.headlightsOn, null),
                SysItem("DRL", car.drlOn, null),
                SysItem("Fog Lights", car.fogLightsOn, null),
                SysItem("Interior", car.interiorLightsOn, null),
                SysItem("Doors", car.allDoorsLocked, if (car.allDoorsLocked) "LOCKED" else "OPEN"),
                SysItem("Hood/Trunk", car.hoodClosed && car.trunkClosed,
                    if (car.hoodClosed && car.trunkClosed) "CLOSED" else "OPEN"),
                SysItem("Traction Ctrl", car.tractionControl, null),
                SysItem("Windows", true, "UP"),
            )
            for (row in systems.chunked(2)) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.padding(vertical = 2.dp)) {
                    for (s in row) {
                        Row(
                            Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                .background(if (s.on) C.Glass else Color.Transparent)
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            StatusDot(if (s.on) C.Green else C.TextMuted, 4.dp)
                            Text(s.label, TextStyle(9.sp, color = if (s.on) C.TextSub else C.TextMuted),
                                Modifier.weight(1f))
                            Text(
                                s.status ?: if (s.on) "ON" else "OFF",
                                TextStyle(7.sp, FontWeight.Bold,
                                    if (s.on) C.Green else C.TextMuted, letterSpacing = 0.6.sp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class SysItem(val label: String, val on: Boolean, val status: String?)

@Composable
private fun FluidRow(name: String, value: Int, extra: String?) {
    val c = when { value > 75 -> C.Green; value > 40 -> C.Amber; else -> C.Red }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        StatusDot(c, 4.dp)
        Text(name, TextStyle(9.sp, color = C.TextSub, fontWeight = FontWeight.Medium), Modifier.weight(1f))
        if (extra != null) Text(extra, TextStyle(7.sp, color = C.TextMuted))
        Text("$value%", TextStyle(10.sp, FontWeight.Light, C.TextPrimary))
        ProgressBar(value.toFloat(), color = c, modifier = Modifier.width(40.dp))
    }
}

@Composable
private fun ElecRow(label: String, value: String, color: Color) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        Arrangement.SpaceBetween, Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            StatusDot(color, 3.dp)
            Text(label, TextStyle(9.sp, color = C.TextSub))
        }
        Text(value, TextStyle(10.sp, FontWeight.Light, C.TextPrimary))
    }
}
