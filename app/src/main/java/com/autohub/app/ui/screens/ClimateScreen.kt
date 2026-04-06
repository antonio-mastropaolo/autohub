package com.autohub.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autohub.app.data.CarState
import com.autohub.app.ui.components.*
import com.autohub.app.ui.theme.C

@Composable
fun ClimateScreen(car: CarState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

        // ── Dual zone climate ──
        GlassCard(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusDot(if (car.acOn) C.Cyan else C.TextMuted, 6.dp)
                Spacer(Modifier.width(6.dp))
                Text(
                    "DUAL ZONE CLIMATE ${if (car.acOn) "ACTIVE" else "OFF"}",
                    style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (car.acOn) C.Cyan else C.TextMuted, letterSpacing = 1.sp)
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Driver
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LabelText("DRIVER")
                    Spacer(Modifier.height(4.dp))
                    TempKnob(car.cabinTemp, car.acTarget, C.Cyan, size = 100.dp)
                    Text("Target: ${car.acTarget}\u00b0F", style = TextStyle(fontSize = 11.sp, color = C.TextSub))
                }

                // Fan + Airflow center
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
                        for (i in 1..5) {
                            Box(
                                Modifier.width(10.dp).height((4 + i * 2.5f).dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (i <= car.fanSpeed) C.Cyan else C.Glass)
                            )
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    LabelText("FAN")
                    Spacer(Modifier.height(8.dp))
                    Pill(car.airflowMode, C.Cyan)
                    Spacer(Modifier.height(3.dp))
                    LabelText("AIRFLOW")
                }

                // Passenger
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LabelText("PASSENGER")
                    Spacer(Modifier.height(4.dp))
                    TempKnob(car.cabinTemp + 2, car.acTargetPass, C.Blue, size = 100.dp)
                    Text("Target: ${car.acTargetPass}\u00b0F", style = TextStyle(fontSize = 11.sp, color = C.TextSub))
                }
            }
        }

        // ── Air quality + Humidity + Outside ──
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassCard(Modifier.weight(1f)) {
                LabelText("AIR QUALITY")
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${car.airQuality}", style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Thin, color = C.TextPrimary))
                    Text(" AQI", style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = C.TextMuted), modifier = Modifier.padding(bottom = 3.dp))
                }
                Spacer(Modifier.height(3.dp))
                Pill(if (car.airQuality > 80) "Good" else "Moderate",
                    if (car.airQuality > 80) C.Green else C.Amber)
            }
            GlassCard(Modifier.weight(1f)) {
                LabelText("HUMIDITY")
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${car.humidity}", style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Thin, color = C.TextPrimary))
                    Text("%", style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = C.TextMuted), modifier = Modifier.padding(bottom = 3.dp))
                }
                Spacer(Modifier.height(3.dp))
                ProgressBar(car.humidity.toFloat(), 100f, C.Cyan, 2.dp)
            }
            GlassCard(Modifier.weight(1f)) {
                LabelText("OUTSIDE")
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${car.outsideTemp}", style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Thin))
                    Text("\u00b0F", style = TextStyle(fontSize = 12.sp, color = C.TextMuted), modifier = Modifier.padding(bottom = 2.dp))
                }
            }
        }

        // ── Seat heat + Defrost + Thermal ──
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassCard(Modifier.weight(0.5f)) {
                LabelText("SEAT HEAT")
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                    SeatHeatCol("D", car.driverSeatHeat)
                    Box(Modifier.width(1.dp).height(24.dp).background(C.TextFaint))
                    SeatHeatCol("P", car.passSeatHeat)
                }
                Spacer(Modifier.height(6.dp))
                LabelText("STEERING")
                Spacer(Modifier.height(3.dp))
                ToggleRow(car.steeringHeat)
            }
            GlassCard(Modifier.weight(0.4f)) {
                LabelText("DEFROST")
                Spacer(Modifier.height(6.dp))
                DefrostRow("Front", car.frontDefrost)
                Spacer(Modifier.height(4.dp))
                DefrostRow("Rear", car.rearDefrost)
                Spacer(Modifier.height(6.dp))
                LabelText("RECIRCULATE")
                Spacer(Modifier.height(3.dp))
                ToggleRow(car.recirculate)
            }
            GlassCard(Modifier.weight(1f)) {
                LabelText("THERMAL OVERVIEW")
                Spacer(Modifier.height(6.dp))
                ThermalRow("Cabin", car.cabinTemp, 100f, C.Cyan)
                ThermalRow("Engine", car.engineTemp, 260f, if (car.engineTemp > 210) C.Red else C.Amber)
                ThermalRow("Oil", car.oilTemp, 280f, if (car.oilTemp > 230) C.Red else C.Green)
                ThermalRow("Trans", car.transTemp, 250f, C.Purple)
                ThermalRow("Battery", car.batteryTemp, 150f, C.Blue)
            }
        }
    }
}

@Composable
private fun SeatHeatCol(label: String, level: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = C.TextMuted))
        Spacer(Modifier.height(3.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            for (i in 1..3) {
                Box(Modifier.size(7.dp).clip(RoundedCornerShape(2.dp))
                    .background(if (i <= level) C.Amber else C.Glass))
            }
        }
    }
}

@Composable
private fun DefrostRow(label: String, on: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        StatusDot(if (on) C.Amber else C.TextMuted, 3.dp)
        Text(label, style = TextStyle(fontSize = 12.sp, color = if (on) C.TextSub else C.TextMuted))
    }
}

@Composable
private fun ToggleRow(on: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        StatusDot(if (on) C.Cyan else C.TextMuted, 3.dp)
        Text(if (on) "ON" else "OFF", style = TextStyle(fontSize = 11.sp, color = if (on) C.TextSub else C.TextMuted))
    }
}

@Composable
private fun ThermalRow(label: String, value: Int, max: Float, color: androidx.compose.ui.graphics.Color) {
    Column(Modifier.padding(bottom = 4.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text(label, style = TextStyle(fontSize = 10.sp, color = C.TextSub, fontWeight = FontWeight.Medium))
            Text("$value\u00b0F", style = TextStyle(fontSize = 10.sp, color = C.TextPrimary))
        }
        Spacer(Modifier.height(2.dp))
        ProgressBar(value.toFloat(), max, color, 2.dp)
    }
}
