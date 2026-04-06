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
import com.autohub.app.ui.theme.AutoHubColors as C

@Composable
fun VehicleScreen(car: CarState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Tires + Fluids side by side
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassCard(modifier = Modifier.weight(1f)) {
                LabelText("Tires & Status")
                Spacer(Modifier.height(4.dp))
                CarTopViewCanvas(
                    tireFl = car.tireFl, tireFr = car.tireFr,
                    tireRl = car.tireRl, tireRr = car.tireRr,
                    allDoorsLocked = car.allDoorsLocked,
                )
            }

            GlassCard(modifier = Modifier.weight(1f)) {
                LabelText("Fluid Levels")
                Spacer(Modifier.height(6.dp))

                data class Fluid(val label: String, val value: Int)
                val fluids = listOf(
                    Fluid("Engine Oil", car.oilLife),
                    Fluid("Brake Fluid", car.brakeFluid),
                    Fluid("Coolant", car.coolant),
                    Fluid("Transmission", car.transFluid),
                    Fluid("Washer", car.washerFluid),
                )

                for (f in fluids) {
                    val color = when {
                        f.value > 75 -> C.Green
                        f.value > 40 -> C.Amber
                        else -> C.Red
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusDot(color = color, size = 4.dp)
                        Text(f.label, style = TextStyle(fontSize = 10.sp, color = C.TextSub, fontWeight = FontWeight.Medium), modifier = Modifier.weight(1f))
                        Text("${f.value}%", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Light, color = C.TextPrimary))
                        ProgressBar(value = f.value.toFloat(), color = color, modifier = Modifier.width(45.dp))
                    }
                }
            }
        }

        // Service card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    LabelText("Next Service")
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "%,d".format(car.serviceIn),
                            style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.ExtraLight)
                        )
                        Text("mi", style = TextStyle(fontSize = 7.sp, fontWeight = FontWeight.Bold, color = C.TextMuted), modifier = Modifier.padding(start = 2.dp, bottom = 2.dp))
                    }
                }
                ProgressBar(
                    value = car.serviceIn.toFloat(), maxValue = 5000f,
                    color = if (car.serviceIn > 1500) C.Green else C.Amber,
                    modifier = Modifier.width(80.dp)
                )
                Pill(
                    text = if (car.serviceIn > 1500) "Good" else "Due Soon",
                    color = if (car.serviceIn > 1500) C.Green else C.Amber
                )
            }
        }

        // Systems
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            LabelText("Systems")
            Spacer(Modifier.height(6.dp))

            data class SysInfo(val label: String, val on: Boolean, val status: String? = null)
            val systems = listOf(
                SysInfo("Headlights", car.headlightsOn),
                SysInfo("DRL", car.drlOn),
                SysInfo("Fog Lights", car.fogLightsOn),
                SysInfo("Interior", car.interiorLightsOn),
                SysInfo("Doors", car.allDoorsLocked, if (car.allDoorsLocked) "LOCKED" else "OPEN"),
                SysInfo("Hood / Trunk", car.hoodClosed && car.trunkClosed, if (car.hoodClosed && car.trunkClosed) "CLOSED" else "OPEN"),
            )

            // 2-column grid
            val rows = systems.chunked(2)
            for (row in rows) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(vertical = 2.dp)) {
                    for (sys in row) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (sys.on) C.Glass else androidx.compose.ui.graphics.Color.Transparent)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            StatusDot(color = if (sys.on) C.Green else C.TextMuted, size = 4.dp)
                            Text(sys.label, style = TextStyle(fontSize = 10.sp, color = if (sys.on) C.TextSub else C.TextMuted), modifier = Modifier.weight(1f))
                            Text(
                                sys.status ?: if (sys.on) "ON" else "OFF",
                                style = TextStyle(fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (sys.on) C.Green else C.TextMuted, letterSpacing = 0.6.sp)
                            )
                        }
                    }
                }
            }
        }
    }
}
