package com.autohub.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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

// ═══════════════════════════════════════════════════════════════
//  DETAIL SHEETS — bottom-sheet / overlay views for dashboard cards
//  Vehicle: 2024 VW Atlas Cross Sport 2.0T TSI
// ═══════════════════════════════════════════════════════════════

// ── Shared overlay wrapper ──────────────────────────────────────

@Composable
private fun SheetOverlay(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    AnimatedVisibility(visible = true, enter = fadeIn()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { onDismiss() },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.85f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(C.Surface)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { /* consume click so overlay doesn't dismiss */ },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            title,
                            style = TextStyle(
                                color = C.TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Light,
                            ),
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Close",
                                tint = C.TextMuted,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    content()
                }
            }
        }
    }
}

// Helper row for label + value pairs
@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = C.TextPrimary,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = TextStyle(color = C.TextSub, fontSize = 13.sp, fontWeight = FontWeight.Normal))
        Text(value, style = TextStyle(color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.Light))
    }
}

// Helper section label
@Composable
private fun SectionLabel(text: String) {
    Spacer(Modifier.height(12.dp))
    LabelText(text)
    Spacer(Modifier.height(6.dp))
}

// ═══════════════════════════════════════════════════════════════
//  1. FUEL DETAIL SHEET
// ═══════════════════════════════════════════════════════════════

@Composable
fun FuelDetailSheet(car: CarState, onDismiss: () -> Unit) {
    val tankGallons = 18.6f
    val gallonsRemaining = car.fuel * tankGallons / 100f
    val gallonsNeeded = tankGallons - gallonsRemaining
    val estimatedRefuelCost = gallonsNeeded * 3.50f
    val tripFuelUsed = if (car.avgMpg > 0f) car.trip / car.avgMpg else 0f

    SheetOverlay(title = "Fuel Details", onDismiss = onDismiss) {

        // Large fuel gauge
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            ArcGauge(
                value = car.fuel,
                maxValue = 100f,
                label = "FUEL LEVEL",
                unit = "%",
                color = if (car.fuel > 25f) C.Blue else C.Red,
                size = 180.dp,
            )
        }

        Spacer(Modifier.height(8.dp))

        // Fuel level & gallons
        GlassCard(Modifier.fillMaxWidth()) {
            DetailRow("Current Level", "${car.fuel.toInt()}%")
            DetailRow("Gallons Remaining", "%.1f gal".format(gallonsRemaining))
            DetailRow("Range Remaining", "${car.range} mi")
        }

        SectionLabel("CONSUMPTION")

        GlassCard(Modifier.fillMaxWidth()) {
            DetailRow("Average MPG", "%.1f mpg".format(car.avgMpg))
            DetailRow("Instant MPG", "%.1f mpg".format(car.mpg))
            DetailRow(
                "Estimated Refuel Cost",
                "$%.2f".format(estimatedRefuelCost),
                valueColor = C.Amber,
            )
            DetailRow("Gallons to Fill", "%.1f gal".format(gallonsNeeded))
        }

        SectionLabel("TRIP FUEL")

        GlassCard(Modifier.fillMaxWidth()) {
            DetailRow("Trip Distance", "%.1f mi".format(car.trip))
            DetailRow("Trip Fuel Used", "%.2f gal".format(tripFuelUsed))
        }

        SectionLabel("MPG HISTORY")

        GlassCard(Modifier.fillMaxWidth()) {
            SparkLine(
                data = car.mpgHistory,
                color = C.Amber,
                modifier = Modifier.fillMaxWidth().height(60.dp),
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  2. ENGINE DETAIL SHEET
// ═══════════════════════════════════════════════════════════════

@Composable
fun EngineDetailSheet(car: CarState, onDismiss: () -> Unit) {
    val maxHp = 235f
    val maxTorque = 258f
    val engineLoadPct = (car.power / maxHp * 100f).coerceIn(0f, 100f)

    SheetOverlay(title = "Engine Details", onDismiss = onDismiss) {

        // Engine name
        Text(
            "2.0L TSI Turbocharged I4",
            style = TextStyle(
                color = C.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Light,
            ),
        )
        Text(
            "2024 VW Atlas Cross Sport",
            style = TextStyle(
                color = C.TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
            ),
        )

        SectionLabel("POWER OUTPUT")

        GlassCard(Modifier.fillMaxWidth()) {
            DetailRow("Power", "${car.power} HP / ${maxHp.toInt()} HP")
            ProgressBar(
                value = car.power.toFloat(),
                maxValue = maxHp,
                color = C.Blue,
                height = 4.dp,
            )
            Spacer(Modifier.height(8.dp))

            DetailRow("Torque", "${car.torque} lb-ft / ${maxTorque.toInt()} lb-ft")
            ProgressBar(
                value = car.torque.toFloat(),
                maxValue = maxTorque,
                color = C.Cyan,
                height = 4.dp,
            )
            Spacer(Modifier.height(8.dp))

            DetailRow("Throttle Position", "%.0f%%".format(car.throttle))
            ProgressBar(
                value = car.throttle,
                maxValue = 100f,
                color = C.Purple,
                height = 4.dp,
            )
            Spacer(Modifier.height(8.dp))

            DetailRow("Engine Load", "%.0f%%".format(engineLoadPct))
            ProgressBar(
                value = engineLoadPct,
                maxValue = 100f,
                color = C.Amber,
                height = 4.dp,
            )
        }

        SectionLabel("TEMPERATURES")

        GlassCard(Modifier.fillMaxWidth()) {
            // Coolant temperature
            val coolantColor = when {
                car.engineTemp > 230 -> C.Red
                car.engineTemp > 210 -> C.Amber
                else -> C.Green
            }
            DetailRow("Coolant", "${car.engineTemp}\u00b0F", valueColor = coolantColor)
            ProgressBar(
                value = car.engineTemp.toFloat(),
                maxValue = 260f,
                color = coolantColor,
                height = 4.dp,
            )
            Spacer(Modifier.height(8.dp))

            // Oil temperature
            val oilTempColor = when {
                car.oilTemp > 250 -> C.Red
                car.oilTemp > 230 -> C.Amber
                else -> C.Green
            }
            DetailRow("Oil Temperature", "${car.oilTemp}\u00b0F", valueColor = oilTempColor)
            ProgressBar(
                value = car.oilTemp.toFloat(),
                maxValue = 280f,
                color = oilTempColor,
                height = 4.dp,
            )
            Spacer(Modifier.height(8.dp))

            // Intake air temperature
            DetailRow("Intake Air", "${car.intakeTemp}\u00b0F")
            ProgressBar(
                value = car.intakeTemp.toFloat(),
                maxValue = 180f,
                color = C.Blue,
                height = 4.dp,
            )
        }

        SectionLabel("OIL SYSTEM")

        GlassCard(Modifier.fillMaxWidth()) {
            // Oil pressure
            val oilPressureColor = when {
                car.oilPressure < 25 -> C.Red
                car.oilPressure > 65 -> C.Amber
                else -> C.Green
            }
            DetailRow(
                "Oil Pressure",
                "${car.oilPressure} PSI",
                valueColor = oilPressureColor,
            )
            ProgressBar(
                value = car.oilPressure.toFloat(),
                maxValue = 80f,
                color = oilPressureColor,
                height = 4.dp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Normal range: 25\u201365 PSI",
                style = TextStyle(color = C.TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Normal),
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  3. TIRE DETAIL SHEET
// ═══════════════════════════════════════════════════════════════

@Composable
fun TireDetailSheet(car: CarState, onDismiss: () -> Unit) {
    SheetOverlay(title = "Tire Details", onDismiss = onDismiss) {

        // Car top view diagram
        GlassCard(Modifier.fillMaxWidth()) {
            CarTopViewCanvas(
                tireFl = car.tireFl,
                tireFr = car.tireFr,
                tireRl = car.tireRl,
                tireRr = car.tireRr,
                tireTempFl = car.tireTempFl,
                tireTempFr = car.tireTempFr,
                tireTempRl = car.tireTempRl,
                tireTempRr = car.tireTempRr,
                allDoorsLocked = car.allDoorsLocked,
            )
        }

        SectionLabel("INDIVIDUAL TIRES")

        // 2x2 tire grid
        data class TireInfo(
            val label: String,
            val psi: Int,
            val temp: Int,
        )

        val tires = listOf(
            TireInfo("FL", car.tireFl, car.tireTempFl),
            TireInfo("FR", car.tireFr, car.tireTempFr),
            TireInfo("RL", car.tireRl, car.tireTempRl),
            TireInfo("RR", car.tireRr, car.tireTempRr),
        )

        // Front row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tires.take(2).forEach { tire ->
                GlassCard(Modifier.weight(1f)) {
                    TireCardContent(tire.label, tire.psi, tire.temp)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Rear row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tires.drop(2).forEach { tire ->
                GlassCard(Modifier.weight(1f)) {
                    TireCardContent(tire.label, tire.psi, tire.temp)
                }
            }
        }

        SectionLabel("TPMS SUMMARY")

        GlassCard(Modifier.fillMaxWidth()) {
            val allOk = tires.all { it.psi in 32..38 }
            val anyLow = tires.any { it.psi < 32 }
            val anyHigh = tires.any { it.psi > 38 }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatusDot(
                    color = when {
                        anyLow -> C.Red
                        anyHigh -> C.Amber
                        else -> C.Green
                    },
                    size = 6.dp,
                )
                Text(
                    when {
                        anyLow -> "Low pressure detected"
                        anyHigh -> "High pressure detected"
                        allOk -> "All tires nominal"
                        else -> "Check tire pressures"
                    },
                    style = TextStyle(
                        color = C.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Light,
                    ),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "VW Recommended: 36 PSI (all four)",
                style = TextStyle(color = C.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Normal),
            )
        }
    }
}

@Composable
private fun TireCardContent(label: String, psi: Int, temp: Int) {
    val status: String
    val statusColor: Color
    when {
        psi < 32 -> { status = "LOW"; statusColor = C.Red }
        psi > 38 -> { status = "HIGH"; statusColor = C.Amber }
        else -> { status = "OK"; statusColor = C.Green }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                label,
                style = TextStyle(
                    color = C.TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                ),
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "$psi",
                    style = TextStyle(
                        color = C.TextPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraLight,
                    ),
                )
                Text(
                    " PSI",
                    style = TextStyle(
                        color = C.TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                "$temp\u00b0F",
                style = TextStyle(
                    color = C.TextSub,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                ),
            )
        }
        Pill(text = status, color = statusColor)
    }
}

// ═══════════════════════════════════════════════════════════════
//  4. BATTERY DETAIL SHEET
// ═══════════════════════════════════════════════════════════════

@Composable
fun BatteryDetailSheet(car: CarState, onDismiss: () -> Unit) {
    val isCharging = car.alternatorVoltage > 13.5f
    val voltageColor = when {
        car.batteryVoltage < 12.0f -> C.Red
        car.batteryVoltage <= 12.8f -> C.Green
        car.alternatorVoltage in 13.5f..14.8f -> C.Blue
        else -> C.Green
    }

    SheetOverlay(title = "Battery Details", onDismiss = onDismiss) {

        // Large voltage display
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "%.1f".format(car.batteryVoltage),
                    style = TextStyle(
                        color = voltageColor,
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Thin,
                    ),
                )
                Text(
                    "VOLTS",
                    style = TextStyle(
                        color = C.TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                    ),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Status pill
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Pill(
                text = if (isCharging) "Charging" else "Discharging",
                color = if (isCharging) C.Blue else C.Amber,
            )
        }

        SectionLabel("DETAILS")

        GlassCard(Modifier.fillMaxWidth()) {
            DetailRow("Battery Voltage", "%.2fV".format(car.batteryVoltage), valueColor = voltageColor)
            ProgressBar(
                value = car.batteryVoltage,
                maxValue = 15f,
                color = voltageColor,
                height = 4.dp,
            )
            Spacer(Modifier.height(8.dp))

            DetailRow("Alternator Voltage", "%.2fV".format(car.alternatorVoltage))
            ProgressBar(
                value = car.alternatorVoltage,
                maxValue = 15f,
                color = if (isCharging) C.Blue else C.Amber,
                height = 4.dp,
            )
            Spacer(Modifier.height(8.dp))

            DetailRow("Battery Health", "${car.batteryHealth}%")
            ProgressBar(
                value = car.batteryHealth.toFloat(),
                maxValue = 100f,
                color = when {
                    car.batteryHealth > 80 -> C.Green
                    car.batteryHealth > 50 -> C.Amber
                    else -> C.Red
                },
                height = 4.dp,
            )
            Spacer(Modifier.height(8.dp))

            DetailRow("Battery Temperature", "${car.batteryTemp}\u00b0F")
        }

        SectionLabel("VOLTAGE RANGES")

        GlassCard(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    StatusDot(color = C.Red, size = 5.dp)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Low",
                        style = TextStyle(color = C.TextSub, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    )
                    Text(
                        "< 12.0V",
                        style = TextStyle(color = C.TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Normal),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    StatusDot(color = C.Green, size = 5.dp)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Normal",
                        style = TextStyle(color = C.TextSub, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    )
                    Text(
                        "12.0\u201312.8V",
                        style = TextStyle(color = C.TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Normal),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    StatusDot(color = C.Blue, size = 5.dp)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Charging",
                        style = TextStyle(color = C.TextSub, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    )
                    Text(
                        "13.5\u201314.8V",
                        style = TextStyle(color = C.TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Normal),
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  5. TRIP DETAIL SHEET
// ═══════════════════════════════════════════════════════════════

@Composable
fun TripDetailSheet(car: CarState, onDismiss: () -> Unit) {
    val tripHours = car.tripTime / 3600
    val tripMinutes = (car.tripTime % 3600) / 60
    val fuelUsed = if (car.avgMpg > 0f) car.trip / car.avgMpg else 0f

    SheetOverlay(title = "Trip Details", onDismiss = onDismiss) {

        // Trip summary header
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "%.1f".format(car.trip),
                    style = TextStyle(
                        color = C.TextPrimary,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Thin,
                    ),
                )
                Text(
                    "MILES",
                    style = TextStyle(
                        color = C.TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                    ),
                )
            }
        }

        SectionLabel("TRIP STATS")

        GlassCard(Modifier.fillMaxWidth()) {
            DetailRow("Distance", "%.1f mi".format(car.trip))
            DetailRow(
                "Duration",
                when {
                    tripHours > 0 -> "${tripHours}h ${tripMinutes}m"
                    tripMinutes > 0 -> "${tripMinutes}m"
                    else -> "${car.tripTime}s"
                },
            )
            DetailRow("Average Speed", "${car.avgSpeed} mph")
            DetailRow("Average MPG", "%.1f mpg".format(car.avgMpg))
            DetailRow("Fuel Used", "%.2f gal".format(fuelUsed))
        }

        SectionLabel("SPEED HISTORY")

        GlassCard(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Current: ${car.speed} mph",
                    style = TextStyle(color = C.TextSub, fontSize = 12.sp, fontWeight = FontWeight.Normal),
                )
                Text(
                    "Peak: ${car.peakSpeed} mph",
                    style = TextStyle(color = C.TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Normal),
                )
            }
            Spacer(Modifier.height(6.dp))
            SparkLine(
                data = car.speedHistory,
                color = C.Blue,
                modifier = Modifier.fillMaxWidth().height(60.dp),
            )
        }

        SectionLabel("DRIVING SCORE")

        GlassCard(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MiniRingGauge(
                    value = car.drivingScore.toFloat(),
                    maxValue = 100f,
                    color = when {
                        car.drivingScore >= 85 -> C.Green
                        car.drivingScore >= 70 -> C.Amber
                        else -> C.Red
                    },
                    size = 48.dp,
                )
                Column {
                    Text(
                        "${car.drivingScore}/100",
                        style = TextStyle(
                            color = C.TextPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraLight,
                        ),
                    )
                    Text(
                        when {
                            car.drivingScore >= 85 -> "Excellent driving"
                            car.drivingScore >= 70 -> "Good driving"
                            else -> "Needs improvement"
                        },
                        style = TextStyle(
                            color = C.TextSub,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Score breakdown
            val scoreColor = when {
                car.drivingScore >= 85 -> C.Green
                car.drivingScore >= 70 -> C.Amber
                else -> C.Red
            }

            Text(
                "BREAKDOWN",
                style = TextStyle(
                    color = C.TextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                ),
            )
            Spacer(Modifier.height(6.dp))

            DetailRow("Smoothness", "${(car.drivingScore * 0.95f).toInt()}%")
            ProgressBar(
                value = car.drivingScore * 0.95f,
                maxValue = 100f,
                color = scoreColor,
                height = 3.dp,
            )
            Spacer(Modifier.height(6.dp))

            DetailRow("Efficiency", "${(car.drivingScore * 1.02f).coerceAtMost(100f).toInt()}%")
            ProgressBar(
                value = (car.drivingScore * 1.02f).coerceAtMost(100f),
                maxValue = 100f,
                color = scoreColor,
                height = 3.dp,
            )
            Spacer(Modifier.height(6.dp))

            DetailRow("Speed Consistency", "${(car.drivingScore * 0.98f).toInt()}%")
            ProgressBar(
                value = car.drivingScore * 0.98f,
                maxValue = 100f,
                color = scoreColor,
                height = 3.dp,
            )
        }
    }
}
