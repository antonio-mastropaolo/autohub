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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autohub.app.data.CarState
import com.autohub.app.ui.components.*
import com.autohub.app.ui.theme.AutoHubColors as C

@Composable
fun DriveScreen(car: CarState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Gauges row — landscape: side by side
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ArcGauge(
                    value = car.speed.toFloat(), maxValue = 160f,
                    label = "SPEED", unit = "MPH", color = C.Blue,
                    size = 140.dp
                )

                // Gear + Power center column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Gear badge
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(C.BlueDim)
                            .border(1.dp, C.Blue.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            car.gear,
                            style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.ExtraLight, color = C.Blue)
                        )
                    }
                    LabelText("Gear")

                    Text(
                        "${car.power}",
                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.ExtraLight, color = C.TextPrimary)
                    )
                    LabelText("HP")
                }

                ArcGauge(
                    value = car.rpm.toFloat(), maxValue = 8000f,
                    label = "RPM", unit = "×1000",
                    color = if (car.rpm > 5000) C.Red else C.TextSub,
                    size = 140.dp
                )
            }
        }

        // Stat cards row
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            data class StatInfo(val label: String, val value: String, val unit: String, val color: androidx.compose.ui.graphics.Color, val bar: Float, val barMax: Float)
            val stats = listOf(
                StatInfo("Fuel", "${car.fuel.toInt()}", "%", if (car.fuel > 25) C.Blue else C.Red, car.fuel, 100f),
                StatInfo("Range", "${car.range}", "mi", C.Green, car.range.toFloat(), 340f),
                StatInfo("Efficiency", "%.1f".format(car.mpg), "mpg", C.Amber, car.mpg, 45f),
            )
            for (stat in stats) {
                GlassCard(modifier = Modifier.weight(1f)) {
                    LabelText(stat.label)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        MiniRingGauge(value = stat.bar, maxValue = stat.barMax, color = stat.color)
                        Column {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(stat.value, style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.ExtraLight, color = C.TextPrimary))
                                Text(stat.unit, style = TextStyle(fontSize = 7.sp, fontWeight = FontWeight.Bold, color = C.TextMuted), modifier = Modifier.padding(start = 2.dp, bottom = 2.dp))
                            }
                        }
                    }
                }
            }
        }

        // Info strip
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                data class StripItem(val label: String, val value: String, val unit: String)
                val items = listOf(
                    StripItem("Odometer", "%,d".format(car.odometer), "mi"),
                    StripItem("Trip", "%.1f".format(car.trip), "mi"),
                    StripItem("Engine", "${car.engineTemp}", "°F"),
                    StripItem("Battery", "%.1f".format(car.batteryVoltage), "V"),
                )
                items.forEachIndexed { i, item ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LabelText(item.label)
                        Spacer(Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(item.value, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.ExtraLight))
                            Text(item.unit, style = TextStyle(fontSize = 6.sp, fontWeight = FontWeight.Bold, color = C.TextMuted), modifier = Modifier.padding(start = 1.dp, bottom = 1.dp))
                        }
                    }
                    if (i < items.lastIndex) {
                        Box(Modifier.width(1.dp).height(32.dp).background(C.TextFaint))
                    }
                }
            }
        }
    }
}
