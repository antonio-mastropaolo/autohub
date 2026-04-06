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
fun NavScreen(car: CarState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

        // ── Compass + GPS ──
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassCard(Modifier.weight(1f)) {
                LabelText("COMPASS")
                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth(), Alignment.Center) {
                    CompassRose(car.heading, C.Purple, size = 150.dp)
                }
                Spacer(Modifier.height(4.dp))
                val dir = when {
                    car.heading < 23 || car.heading >= 338 -> "N"
                    car.heading < 68 -> "NE"; car.heading < 113 -> "E"
                    car.heading < 158 -> "SE"; car.heading < 203 -> "S"
                    car.heading < 248 -> "SW"; car.heading < 293 -> "W"
                    else -> "NW"
                }
                Row(Modifier.fillMaxWidth(), Arrangement.Center) {
                    Text("${car.heading}\u00b0 $dir", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Light, color = C.TextSecondary))
                }
            }

            GlassCard(Modifier.weight(1f)) {
                LabelText("GPS INFORMATION")
                Spacer(Modifier.height(8.dp))
                GpsRow("Latitude", "%.4f\u00b0N".format(car.latitude), C.Purple)
                GpsRow("Longitude", "%.4f\u00b0W".format(-car.longitude), C.Purple)
                GpsRow("Altitude", "${car.altitude} ft", C.Blue)
                GpsRow("Satellites", "${car.satellites}", C.Green)
                GpsRow("Accuracy", "\u00b13m", C.Cyan)
                GpsRow("HDOP", "0.8", C.Green)

                Spacer(Modifier.height(8.dp))
                LabelText("SATELLITE SIGNAL")
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    for (i in 1..12) {
                        Box(
                            Modifier.weight(1f)
                                .height(if (i <= car.satellites) 12.dp else 4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (i <= car.satellites) C.Green else C.Glass)
                        )
                    }
                }
            }
        }

        // ── Speed + Limit + Trip ──
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassCard(Modifier.weight(1f)) {
                LabelText("CURRENT SPEED")
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${car.speed}", style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Thin, color = C.TextPrimary))
                    Text(" MPH", style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = C.TextMuted),
                        modifier = Modifier.padding(bottom = 4.dp))
                }
            }
            GlassCard(Modifier.weight(1f)) {
                LabelText("SPEED LIMIT")
                Spacer(Modifier.height(4.dp))
                val over = car.speed > car.speedLimit
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${car.speedLimit}", style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Thin,
                        color = if (over) C.Red else C.TextPrimary))
                    Text(" MPH", style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = C.TextMuted),
                        modifier = Modifier.padding(bottom = 4.dp))
                }
                if (over) {
                    Spacer(Modifier.height(3.dp))
                    Pill("+${car.speed - car.speedLimit} OVER", C.Red)
                }
            }
            GlassCard(Modifier.weight(1.5f)) {
                LabelText("TRIP STATISTICS")
                Spacer(Modifier.height(6.dp))
                TripRow("Distance", "%.1f mi".format(car.trip))
                TripRow("Duration", "${car.tripTime / 60}h ${car.tripTime % 60}m")
                TripRow("Avg Speed", "${car.avgSpeed} MPH")
                TripRow("Avg MPG", "%.1f".format(car.avgMpg))
            }
        }
    }
}

@Composable
private fun GpsRow(label: String, value: String, color: Color) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        Arrangement.SpaceBetween, Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            StatusDot(color, 3.dp)
            Text(label, style = TextStyle(fontSize = 10.sp, color = C.TextSub, fontWeight = FontWeight.Medium))
        }
        Text(value, style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Light, color = C.TextPrimary))
    }
}

@Composable
private fun TripRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        Arrangement.SpaceBetween
    ) {
        Text(label, style = TextStyle(fontSize = 9.sp, color = C.TextSub))
        Text(value, style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Light, color = C.TextPrimary))
    }
}
