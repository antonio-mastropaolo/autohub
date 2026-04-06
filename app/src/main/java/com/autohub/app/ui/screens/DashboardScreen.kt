package com.autohub.app.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autohub.app.data.CarState
import com.autohub.app.ui.components.*
import com.autohub.app.ui.theme.C

@Composable
fun DashboardScreen(car: CarState) {
    var showFuelDetail by remember { mutableStateOf(false) }
    var showEngineDetail by remember { mutableStateOf(false) }
    var showTireDetail by remember { mutableStateOf(false) }
    var showBatteryDetail by remember { mutableStateOf(false) }
    var showTripDetail by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

            // ═══════════════════════════════════════════════════════
            //  PRIMARY: Speed + RPM + Gear — Atlas Cross Sport style
            // ═══════════════════════════════════════════════════════
            GlassCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // RPM gauge (left)
                    ArcGauge(
                        car.rpm.toFloat(), 8000f, "RPM", "\u00d71000",
                        if (car.rpm > 5000) C.Red else C.TextSub, size = 120.dp
                    )

                    // Speed — large central readout
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${car.speed}",
                            style = TextStyle(
                                fontSize = 64.sp,
                                fontWeight = FontWeight.Thin,
                                color = C.TextPrimary,
                                letterSpacing = (-2).sp,
                            )
                        )
                        Text(
                            "MPH",
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = C.TextMuted,
                                letterSpacing = 3.sp,
                            )
                        )
                        Spacer(Modifier.height(6.dp))
                        // Gear badge
                        Box(
                            Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                                .background(C.BlueDim)
                                .border(1.dp, C.Blue.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                car.gear,
                                style = TextStyle(
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraLight,
                                    color = C.Blue
                                )
                            )
                        }
                    }

                    // Fuel gauge (right)
                    ArcGauge(
                        car.fuel, 100f, "FUEL", "%",
                        if (car.fuel > 25f) C.Green else C.Red, size = 120.dp
                    )
                }
            }

            // ═══════════════════════════════════════════════════════
            //  SECONDARY: Key stats — clean horizontal strip
            // ═══════════════════════════════════════════════════════
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CleanStatCard(
                    "Range", "${car.range}", "mi",
                    if (car.range > 80) C.Green else C.Amber,
                    Modifier.weight(1f).clickable { showFuelDetail = true }
                )
                CleanStatCard(
                    "Engine", "${car.engineTemp}", "\u00b0F",
                    if (car.engineTemp > 210) C.Red else C.Green,
                    Modifier.weight(1f).clickable { showEngineDetail = true }
                )
                CleanStatCard(
                    "Tires", tireStatus(car), "PSI",
                    if (tiresOk(car)) C.Green else C.Amber,
                    Modifier.weight(1f).clickable { showTireDetail = true }
                )
                CleanStatCard(
                    "Battery", "%.1f".format(car.batteryVoltage), "V",
                    if (car.batteryVoltage > 12.0f) C.Blue else C.Red,
                    Modifier.weight(1f).clickable { showBatteryDetail = true }
                )
                CleanStatCard(
                    "Trip", "%.1f".format(car.trip), "mi",
                    C.TextSecondary,
                    Modifier.weight(1f).clickable { showTripDetail = true }
                )
            }

            // ═══════════════════════════════════════════════════════
            //  MEDIA: Compact now-playing strip
            // ═══════════════════════════════════════════════════════
            GlassCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Track info
                    Column(Modifier.weight(1f)) {
                        Text(
                            car.mediaTitle,
                            style = TextStyle(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Light,
                                color = C.TextPrimary
                            ),
                            maxLines = 1
                        )
                        Text(
                            car.mediaArtist,
                            style = TextStyle(fontSize = 12.sp, color = C.TextSub),
                            maxLines = 1
                        )
                    }
                    // Progress
                    Column(Modifier.weight(1f)) {
                        ProgressBar(car.mediaProgress, 1f, Color(0xFF1DB954), 3.dp)
                        Spacer(Modifier.height(2.dp))
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text(car.mediaCurrent, style = TextStyle(fontSize = 10.sp, color = C.TextMuted))
                            Text(car.mediaDuration, style = TextStyle(fontSize = 10.sp, color = C.TextMuted))
                        }
                    }
                    // Source badge
                    Text(
                        car.mediaSource.uppercase(),
                        style = TextStyle(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1DB954),
                            letterSpacing = 1.sp
                        )
                    )
                }
            }
        }

        // Detail sheet overlays
        if (showFuelDetail) FuelDetailSheet(car) { showFuelDetail = false }
        if (showEngineDetail) EngineDetailSheet(car) { showEngineDetail = false }
        if (showTireDetail) TireDetailSheet(car) { showTireDetail = false }
        if (showBatteryDetail) BatteryDetailSheet(car) { showBatteryDetail = false }
        if (showTripDetail) TripDetailSheet(car) { showTripDetail = false }
    }
}

@Composable
private fun CleanStatCard(
    label: String, value: String, unit: String,
    color: Color, modifier: Modifier,
) {
    GlassCard(modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            StatusDot(color, 4.dp)
            LabelText(label)
        }
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                value,
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Thin,
                    color = C.TextPrimary
                )
            )
            Text(
                unit,
                style = TextStyle(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = C.TextMuted
                ),
                modifier = Modifier.padding(start = 2.dp, bottom = 1.dp)
            )
        }
    }
}

private fun tireStatus(car: CarState): String {
    val min = minOf(car.tireFl, car.tireFr, car.tireRl, car.tireRr)
    val max = maxOf(car.tireFl, car.tireFr, car.tireRl, car.tireRr)
    return if (min == max) "$min" else "$min-$max"
}

private fun tiresOk(car: CarState): Boolean {
    return listOf(car.tireFl, car.tireFr, car.tireRl, car.tireRr).all { it in 32..38 }
}
