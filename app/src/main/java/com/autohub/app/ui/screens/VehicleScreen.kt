package com.autohub.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autohub.app.R
import com.autohub.app.data.CarState
import com.autohub.app.ui.components.*
import com.autohub.app.ui.theme.C

@Composable
fun VehicleScreen(car: CarState) {
    var showTireDetail by remember { mutableStateOf(false) }
    var showBatteryDetail by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

            // ═══════════════════════════════════════════════════════
            //  HERO: Real Atlas Cross Sport 2024 photo with overlays
            // ═══════════════════════════════════════════════════════
            GlassCard(Modifier.fillMaxWidth().clickable { showTireDetail = true }) {
                Box(Modifier.fillMaxWidth().height(180.dp)) {
                    // Car photo
                    Image(
                        painter = painterResource(R.drawable.atlas_cross_sport_2024),
                        contentDescription = "2024 VW Atlas Cross Sport SEL R-Line",
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                    // Dark gradient overlay for readability
                    Box(
                        Modifier.fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        C.Background.copy(alpha = 0.85f),
                                        C.Background.copy(alpha = 0.3f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Stats overlaid on left side
                    Column(
                        Modifier.align(Alignment.CenterStart).padding(start = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "ATLAS CROSS SPORT",
                            style = TextStyle(
                                fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                color = C.Blue, letterSpacing = 2.sp
                            )
                        )
                        Text(
                            "2024 SEL R-LINE 2.0T",
                            style = TextStyle(
                                fontSize = 8.sp, fontWeight = FontWeight.Bold,
                                color = C.TextMuted, letterSpacing = 1.5.sp
                            )
                        )
                        Spacer(Modifier.height(4.dp))
                        OverlayStat("Tires", tireStatus(car), if (tiresOk(car)) C.Green else C.Amber)
                        OverlayStat("Doors", if (car.allDoorsLocked) "LOCKED" else "OPEN",
                            if (car.allDoorsLocked) C.Green else C.Amber)
                        OverlayStat("Engine", "${car.engineTemp}\u00b0F",
                            if (car.engineTemp > 210) C.Red else C.Green)
                        OverlayStat("Oil", "${car.oilLife}%",
                            if (car.oilLife > 30) C.Green else C.Amber)
                    }

                    // Tire PSI overlay on right
                    Column(
                        Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        TirePill("FL", car.tireFl)
                        TirePill("FR", car.tireFr)
                        TirePill("RL", car.tireRl)
                        TirePill("RR", car.tireRr)
                    }
                }
            }

            // ═══════════════════════════════════════════════════════
            //  BOTTOM: Fluids + Service + Electrical
            // ═══════════════════════════════════════════════════════
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GlassCard(Modifier.weight(1f)) {
                    LabelText("FLUIDS")
                    Spacer(Modifier.height(4.dp))
                    FluidRow("Engine Oil", car.oilLife, "${car.oilPressure} PSI")
                    FluidRow("Brake Fluid", car.brakeFluid, null)
                    FluidRow("Coolant", car.coolant, null)
                    FluidRow("Trans", car.transFluid, null)
                    FluidRow("Washer", car.washerFluid, null)
                }
                GlassCard(Modifier.weight(0.7f)) {
                    LabelText("SERVICE")
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("%,d".format(car.serviceIn), style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.ExtraLight))
                        Text(" mi", style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = C.TextMuted),
                            modifier = Modifier.padding(bottom = 2.dp))
                    }
                    Spacer(Modifier.height(3.dp))
                    ProgressBar(car.serviceIn.toFloat(), 5000f,
                        if (car.serviceIn > 1500) C.Green else C.Amber)
                    Spacer(Modifier.height(4.dp))
                    Pill(if (car.serviceIn > 1500) "Good" else "Due Soon",
                        if (car.serviceIn > 1500) C.Green else C.Amber)
                }
                GlassCard(Modifier.weight(0.7f).clickable { showBatteryDetail = true }) {
                    LabelText("ELECTRICAL")
                    Spacer(Modifier.height(4.dp))
                    ElecRow("Battery", "%.1f".format(car.batteryVoltage) + "V", C.Green)
                    ElecRow("Alt", "%.1f".format(car.alternatorVoltage) + "V", C.Blue)
                    ElecRow("Health", "${car.batteryHealth}%",
                        if (car.batteryHealth > 80) C.Green else C.Amber)
                }
            }

            // ═══════════════════════════════════════════════════════
            //  SYSTEMS
            // ═══════════════════════════════════════════════════════
            GlassCard(Modifier.fillMaxWidth()) {
                LabelText("SYSTEMS")
                Spacer(Modifier.height(4.dp))
                val systems = listOf(
                    SysItem("Headlights", car.headlightsOn, null),
                    SysItem("DRL", car.drlOn, null),
                    SysItem("Fog Lights", car.fogLightsOn, null),
                    SysItem("Interior", car.interiorLightsOn, null),
                    SysItem("Doors", car.allDoorsLocked, if (car.allDoorsLocked) "LOCKED" else "OPEN"),
                    SysItem("Hood/Trunk", car.hoodClosed && car.trunkClosed,
                        if (car.hoodClosed && car.trunkClosed) "CLOSED" else "OPEN"),
                    SysItem("Traction", car.tractionControl, null),
                    SysItem("Windows", true, "UP"),
                )
                for (row in systems.chunked(4)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(vertical = 1.dp)) {
                        for (s in row) {
                            Row(
                                Modifier.weight(1f).clip(RoundedCornerShape(6.dp))
                                    .background(if (s.on) C.Glass else Color.Transparent)
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                StatusDot(if (s.on) C.Green else C.TextMuted, 4.dp)
                                Text(s.label, style = TextStyle(fontSize = 10.sp, color = if (s.on) C.TextSub else C.TextMuted),
                                    modifier = Modifier.weight(1f))
                                Text(
                                    s.status ?: if (s.on) "ON" else "OFF",
                                    style = TextStyle(fontSize = 8.sp, fontWeight = FontWeight.Bold,
                                        color = if (s.on) C.Green else C.TextMuted, letterSpacing = 0.4.sp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showTireDetail) TireDetailSheet(car) { showTireDetail = false }
        if (showBatteryDetail) BatteryDetailSheet(car) { showBatteryDetail = false }
    }
}

private data class SysItem(val label: String, val on: Boolean, val status: String?)

@Composable
private fun OverlayStat(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        StatusDot(color, 4.dp)
        Text(label, style = TextStyle(fontSize = 10.sp, color = C.TextSub))
        Text(value, style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Light, color = C.TextPrimary))
    }
}

@Composable
private fun TirePill(pos: String, psi: Int) {
    val ok = psi in 32..38
    val color = if (ok) C.Green else C.Amber
    Row(
        Modifier.clip(RoundedCornerShape(6.dp))
            .background(C.Background.copy(alpha = 0.8f))
            .border(0.5.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(pos, style = TextStyle(fontSize = 8.sp, fontWeight = FontWeight.Bold, color = C.TextMuted))
        Text("$psi", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Light, color = if (ok) C.TextPrimary else C.Amber))
        Text("PSI", style = TextStyle(fontSize = 7.sp, fontWeight = FontWeight.Bold, color = C.TextMuted))
    }
}

@Composable
private fun FluidRow(name: String, value: Int, extra: String?) {
    val c = when { value > 75 -> C.Green; value > 40 -> C.Amber; else -> C.Red }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        StatusDot(c, 4.dp)
        Text(name, style = TextStyle(fontSize = 11.sp, color = C.TextSub), modifier = Modifier.weight(1f))
        if (extra != null) Text(extra, style = TextStyle(fontSize = 9.sp, color = C.TextMuted))
        Text("$value%", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Light, color = C.TextPrimary))
        ProgressBar(value.toFloat(), color = c, modifier = Modifier.width(35.dp))
    }
}

@Composable
private fun ElecRow(label: String, value: String, color: Color) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        Arrangement.SpaceBetween, Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            StatusDot(color, 4.dp)
            Text(label, style = TextStyle(fontSize = 11.sp, color = C.TextSub))
        }
        Text(value, style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Light, color = C.TextPrimary))
    }
}

private fun tireStatus(car: CarState): String {
    val min = minOf(car.tireFl, car.tireFr, car.tireRl, car.tireRr)
    val max = maxOf(car.tireFl, car.tireFr, car.tireRl, car.tireRr)
    return if (min == max) "$min PSI" else "$min-$max PSI"
}

private fun tiresOk(car: CarState): Boolean {
    return listOf(car.tireFl, car.tireFr, car.tireRl, car.tireRr).all { it in 32..38 }
}
