package com.autohub.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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

/**
 * Tesla-style vehicle visualization.
 * Full-bleed car image with floating stat pills around the edges.
 * Dark gradient fades the photo into the background.
 */
@Composable
fun VehicleScreen(car: CarState) {
    var showTireDetail by remember { mutableStateOf(false) }
    var showBatteryDetail by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        // ═══════════════════════════════════════════════════════
        //  HERO: Full-bleed Atlas Cross Sport with gradient fade
        // ═══════════════════════════════════════════════════════
        Box(
            Modifier.fillMaxWidth().height(340.dp)
                .clip(RoundedCornerShape(14.dp))
        ) {
            // Car photo — fills the entire area
            Image(
                painter = painterResource(R.drawable.atlas_cross_sport_2024),
                contentDescription = "2024 VW Atlas Cross Sport",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Edge gradients for clean fade into dark background
            // Left fade
            Box(
                Modifier.fillMaxHeight().width(200.dp).align(Alignment.CenterStart)
                    .background(
                        Brush.horizontalGradient(
                            listOf(C.Background.copy(alpha = 0.9f), Color.Transparent)
                        )
                    )
            )
            // Bottom fade
            Box(
                Modifier.fillMaxWidth().height(120.dp).align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, C.Background.copy(alpha = 0.95f))
                        )
                    )
            )
            // Top fade
            Box(
                Modifier.fillMaxWidth().height(60.dp).align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(C.Background.copy(alpha = 0.7f), Color.Transparent)
                        )
                    )
            )
            // Right fade
            Box(
                Modifier.fillMaxHeight().width(80.dp).align(Alignment.CenterEnd)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, C.Background.copy(alpha = 0.6f))
                        )
                    )
            )

            // ── Title (top-left) ──
            Column(Modifier.align(Alignment.TopStart).padding(12.dp)) {
                Text(
                    "ATLAS CROSS SPORT",
                    style = TextStyle(
                        fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        color = C.Blue, letterSpacing = 2.5.sp
                    )
                )
                Text(
                    "2024 SEL R-LINE  •  2.0T TSI  •  4MOTION",
                    style = TextStyle(
                        fontSize = 8.sp, fontWeight = FontWeight.Bold,
                        color = C.TextMuted, letterSpacing = 1.5.sp
                    )
                )
            }

            // ── Left column: System status pills ──
            Column(
                Modifier.align(Alignment.CenterStart).padding(start = 12.dp, top = 40.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FloatingPill("DOORS", if (car.allDoorsLocked) "LOCKED" else "OPEN",
                    if (car.allDoorsLocked) C.Green else C.Amber)
                FloatingPill("ENGINE", "${car.engineTemp}\u00b0F",
                    if (car.engineTemp > 210) C.Red else C.Green)
                FloatingPill("OIL", "${car.oilLife}%",
                    if (car.oilLife > 30) C.Green else C.Amber)
                FloatingPill("BATTERY", "%.1f".format(car.batteryVoltage) + "V",
                    if (car.batteryVoltage > 12f) C.Blue else C.Red)
                FloatingPill("COOLANT", "${car.coolant}\u00b0",
                    if (car.coolant < 220) C.Green else C.Red)
            }

            // ── Right column: Tire PSI ──
            Column(
                Modifier.align(Alignment.CenterEnd).padding(end = 12.dp, top = 30.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text("TIRES", style = TextStyle(fontSize = 8.sp, fontWeight = FontWeight.Bold,
                    color = C.TextMuted, letterSpacing = 1.5.sp))
                Spacer(Modifier.height(2.dp))
                TirePill("FL", car.tireFl)
                TirePill("FR", car.tireFr)
                TirePill("RL", car.tireRl)
                TirePill("RR", car.tireRr)
            }

            // ── Bottom: Key stats strip ──
            Row(
                Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BottomStat("FUEL", "${car.fuel.toInt()}%", if (car.fuel > 25f) C.Green else C.Red)
                BottomStat("RANGE", "${car.range} mi", if (car.range > 80) C.Green else C.Amber)
                BottomStat("SERVICE", "%,d mi".format(car.serviceIn),
                    if (car.serviceIn > 1500) C.Green else C.Amber)
                BottomStat("ODO", "%,d".format(car.odometer), C.TextSecondary)
                BottomStat("TRANS", "${car.transTemp}\u00b0F", C.Purple)
            }
        }

        // ═══════════════════════════════════════════════════════
        //  SYSTEMS ROW (below the hero, only visible on scroll)
        // ═══════════════════════════════════════════════════════
        Row(
            Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val systems = listOf(
                "Headlights" to car.headlightsOn,
                "DRL" to car.drlOn,
                "Fog" to car.fogLightsOn,
                "Interior" to car.interiorLightsOn,
                "Traction" to car.tractionControl,
                "Hood" to car.hoodClosed,
                "Trunk" to car.trunkClosed,
            )
            for ((name, on) in systems) {
                Box(
                    Modifier.weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (on) C.Glass else Color.Transparent)
                        .padding(vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        StatusDot(if (on) C.Green else C.TextMuted, 3.dp)
                        Text(
                            name,
                            style = TextStyle(
                                fontSize = 8.sp,
                                color = if (on) C.TextSub else C.TextMuted,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }

        if (showTireDetail) TireDetailSheet(car) { showTireDetail = false }
        if (showBatteryDetail) BatteryDetailSheet(car) { showBatteryDetail = false }
    }
}

@Composable
private fun FloatingPill(label: String, value: String, color: Color) {
    Row(
        Modifier.clip(RoundedCornerShape(8.dp))
            .background(C.Background.copy(alpha = 0.75f))
            .border(0.5.dp, color.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        StatusDot(color, 4.dp)
        Text(label, style = TextStyle(fontSize = 8.sp, fontWeight = FontWeight.Bold, color = C.TextMuted, letterSpacing = 0.8.sp))
        Text(value, style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Light, color = C.TextPrimary))
    }
}

@Composable
private fun TirePill(pos: String, psi: Int) {
    val ok = psi in 32..38
    val color = if (ok) C.Green else C.Amber
    Row(
        Modifier.clip(RoundedCornerShape(8.dp))
            .background(C.Background.copy(alpha = 0.75f))
            .border(0.5.dp, color.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(pos, style = TextStyle(fontSize = 8.sp, fontWeight = FontWeight.Bold, color = C.TextMuted))
        Text("$psi", style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Light, color = if (ok) C.TextPrimary else C.Amber))
        Text("PSI", style = TextStyle(fontSize = 7.sp, fontWeight = FontWeight.Bold, color = C.TextMuted))
    }
}

@Composable
private fun BottomStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Light, color = C.TextPrimary))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            StatusDot(color, 3.dp)
            Text(label, style = TextStyle(fontSize = 7.sp, fontWeight = FontWeight.Bold, color = C.TextMuted, letterSpacing = 0.8.sp))
        }
    }
}
