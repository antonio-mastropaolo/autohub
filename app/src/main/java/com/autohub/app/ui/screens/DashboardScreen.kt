package com.autohub.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
fun DashboardScreen(car: CarState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

        // ── Main panels ──
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(IntrinsicSize.Max)
        ) {
            // Left — Speed gauge
            GlassCard(Modifier.weight(0.42f).fillMaxHeight()) {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ArcGauge(
                        car.speed.toFloat(), 160f, "SPEED", "MPH",
                        C.Blue, size = 155.dp
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                                .background(C.BlueDim)
                                .border(1.dp, C.Blue.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(car.gear, style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.ExtraLight, color = C.Blue))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${car.power}", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.ExtraLight, color = C.TextPrimary))
                            LabelText("HP")
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${car.torque}", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.ExtraLight, color = C.TextPrimary))
                            LabelText("LB-FT")
                        }
                    }
                }
            }

            // Right — Stats + Media + Weather
            Column(
                Modifier.weight(0.58f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Stat cards
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatMiniCard("Fuel", "${car.fuel.toInt()}", "%",
                        if (car.fuel > 25) C.Blue else C.Red, car.fuel, 100f, Modifier.weight(1f))
                    StatMiniCard("Range", "${car.range}", "mi",
                        C.Green, car.range.toFloat(), 340f, Modifier.weight(1f))
                    StatMiniCard("Efficiency", "%.1f".format(car.mpg), "mpg",
                        C.Amber, car.mpg, 45f, Modifier.weight(1f))
                }

                // Media + Weather
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassCard(Modifier.weight(1f)) {
                        LabelText("NOW PLAYING")
                        Spacer(Modifier.height(5.dp))
                        Text(car.mediaTitle, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Light, color = C.TextPrimary), maxLines = 1)
                        Text(car.mediaArtist, style = TextStyle(fontSize = 10.sp, color = C.TextSub), maxLines = 1)
                        Spacer(Modifier.height(5.dp))
                        ProgressBar(car.mediaProgress, 1f, C.Purple, 2.dp)
                        Spacer(Modifier.height(2.dp))
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text(car.mediaCurrent, style = TextStyle(fontSize = 7.sp, color = C.TextMuted))
                            Text(car.mediaDuration, style = TextStyle(fontSize = 7.sp, color = C.TextMuted))
                        }
                    }
                    GlassCard(Modifier.weight(0.55f)) {
                        LabelText("WEATHER")
                        Spacer(Modifier.height(3.dp))
                        Text(car.weatherIcon, style = TextStyle(fontSize = 22.sp))
                        Text("${car.outsideTemp}\u00b0F", style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Thin, color = C.TextPrimary))
                        Text(car.weatherCondition, style = TextStyle(fontSize = 9.sp, color = C.TextSub))
                        Spacer(Modifier.height(2.dp))
                        Text("H:${car.weatherHigh}\u00b0 L:${car.weatherLow}\u00b0",
                            style = TextStyle(fontSize = 7.sp, fontWeight = FontWeight.Bold, color = C.TextMuted))
                    }
                }

                // MPG trend
                GlassCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        LabelText("EFFICIENCY TREND")
                        Text("%.1f avg".format(car.avgMpg), style = TextStyle(fontSize = 8.sp, color = C.TextSub))
                    }
                    Spacer(Modifier.height(4.dp))
                    SparkLine(car.mpgHistory, C.Amber, Modifier.fillMaxWidth().height(24.dp))
                }
            }
        }

        // ── Bottom quick strip ──
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            QuickInfoItem("Engine", "${car.engineTemp}", "\u00b0F",
                if (car.engineTemp > 210) C.Red else C.Green, Modifier.weight(1f))
            QuickInfoItem("Tires", "OK", "psi", C.Green, Modifier.weight(1f))
            QuickInfoItem("Battery", "%.1f".format(car.batteryVoltage), "V", C.Blue, Modifier.weight(1f))
            QuickInfoItem("Trip", "%.1f".format(car.trip), "mi", C.TextSecondary, Modifier.weight(1f))
            QuickInfoItem("Score", "${car.drivingScore}", "/100", C.Cyan, Modifier.weight(1f))

            GlassCard(Modifier.weight(1.2f)) {
                LabelText("SPEED TREND")
                Spacer(Modifier.height(3.dp))
                SparkLine(car.speedHistory, C.Blue, Modifier.fillMaxWidth().height(22.dp))
            }
        }
    }
}

@Composable
private fun StatMiniCard(
    label: String, value: String, unit: String,
    color: Color, pct: Float, max: Float, modifier: Modifier,
) {
    GlassCard(modifier) {
        LabelText(label)
        Spacer(Modifier.height(5.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MiniRingGauge(pct, max, color)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.ExtraLight, color = C.TextPrimary))
                Text(unit, style = TextStyle(fontSize = 7.sp, fontWeight = FontWeight.Bold, color = C.TextMuted),
                    modifier = Modifier.padding(start = 2.dp, bottom = 2.dp))
            }
        }
    }
}

@Composable
private fun QuickInfoItem(
    label: String, value: String, unit: String,
    color: Color, modifier: Modifier,
) {
    GlassCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            StatusDot(color, 3.dp)
            LabelText(label)
        }
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.ExtraLight, color = C.TextPrimary))
            Text(unit, style = TextStyle(fontSize = 6.sp, fontWeight = FontWeight.Bold, color = C.TextMuted),
                modifier = Modifier.padding(start = 1.dp, bottom = 1.dp))
        }
    }
}
