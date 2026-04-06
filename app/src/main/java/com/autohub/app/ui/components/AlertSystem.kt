package com.autohub.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.TireRepair
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autohub.app.data.CarState
import com.autohub.app.ui.theme.C
import kotlinx.coroutines.delay

// =================================================================
//  SMART ALERT SYSTEM — Severity-Tiered Alerts
//  Monitors OBD parameters against VW Atlas Cross Sport 2024
//  thresholds and delivers warnings in three tiers.
// =================================================================

// ── Data Models ──────────────────────────────────────────────────

data class AlertRule(
    val id: String,
    val label: String,
    val check: (CarState) -> Boolean,
    val tier: Int,
    val message: String,
    val icon: ImageVector,
)

data class ActiveAlert(
    val rule: AlertRule,
    val triggeredAt: Long,
)

// ── Alert Rules ──────────────────────────────────────────────────
// Ordered so that higher-severity (tier 3) rules are listed first
// for each parameter group. checkAlerts returns them sorted anyway.

val alertRules: List<AlertRule> = listOf(
    // Coolant temperature
    AlertRule(
        id = "coolant_critical",
        label = "Engine Overheat",
        check = { it.engineTemp > 230 },
        tier = 3,
        message = "ENGINE OVERHEATING",
        icon = Icons.Outlined.Thermostat,
    ),
    AlertRule(
        id = "coolant_warning",
        label = "Coolant High",
        check = { it.engineTemp in 211..230 },
        tier = 2,
        message = "Coolant temperature high",
        icon = Icons.Outlined.Thermostat,
    ),

    // Oil temperature
    AlertRule(
        id = "oil_critical",
        label = "Oil Overheat",
        check = { it.oilTemp > 260 },
        tier = 3,
        message = "OIL OVERHEATING",
        icon = Icons.Outlined.Thermostat,
    ),
    AlertRule(
        id = "oil_warning",
        label = "Oil Temp High",
        check = { it.oilTemp in 241..260 },
        tier = 2,
        message = "Oil temperature elevated",
        icon = Icons.Outlined.Thermostat,
    ),

    // Battery voltage
    AlertRule(
        id = "battery_critical",
        label = "Battery Critical",
        check = { it.batteryVoltage < 11.5f },
        tier = 3,
        message = "BATTERY CRITICAL",
        icon = Icons.Outlined.BatteryAlert,
    ),
    AlertRule(
        id = "battery_warning",
        label = "Battery Low",
        check = { it.batteryVoltage in 11.5f..11.99f },
        tier = 2,
        message = "Battery voltage low",
        icon = Icons.Outlined.BatteryAlert,
    ),

    // RPM
    AlertRule(
        id = "rpm_critical",
        label = "RPM Redline",
        check = { it.rpm > 6500 },
        tier = 3,
        message = "RPM REDLINE",
        icon = Icons.Outlined.Speed,
    ),
    AlertRule(
        id = "rpm_warning",
        label = "RPM High",
        check = { it.rpm in 6001..6500 },
        tier = 2,
        message = "High RPM \u2014 shift up",
        icon = Icons.Outlined.Speed,
    ),

    // Speed limit
    AlertRule(
        id = "speed_critical",
        label = "Speed Critical",
        check = { it.speed > it.speedLimit + 20 },
        tier = 3,
        message = "SIGNIFICANTLY OVER SPEED LIMIT",
        icon = Icons.Outlined.Speed,
    ),
    AlertRule(
        id = "speed_warning",
        label = "Speeding",
        check = { it.speed in (it.speedLimit + 11)..(it.speedLimit + 20) },
        tier = 2,
        message = "Exceeding speed limit",
        icon = Icons.Outlined.Speed,
    ),

    // Fuel level
    AlertRule(
        id = "fuel_critical",
        label = "Fuel Critical",
        check = { it.fuel < 5f },
        tier = 3,
        message = "FUEL CRITICAL \u2014 refuel immediately",
        icon = Icons.Outlined.LocalGasStation,
    ),
    AlertRule(
        id = "fuel_warning",
        label = "Fuel Low",
        check = { it.fuel in 5f..9.99f },
        tier = 2,
        message = "Fuel level low",
        icon = Icons.Outlined.LocalGasStation,
    ),

    // Tire pressure — any tire
    AlertRule(
        id = "tire_critical",
        label = "Tire Pressure Critical",
        check = { car ->
            listOf(car.tireFl, car.tireFr, car.tireRl, car.tireRr).any { it < 28 }
        },
        tier = 3,
        message = "TIRE PRESSURE CRITICAL",
        icon = Icons.Outlined.TireRepair,
    ),
    AlertRule(
        id = "tire_warning",
        label = "Tire Pressure Low",
        check = { car ->
            listOf(car.tireFl, car.tireFr, car.tireRl, car.tireRr).any { it in 28..29 }
        },
        tier = 2,
        message = "Tire pressure low",
        icon = Icons.Outlined.TireRepair,
    ),
)

// ── Alert Evaluation ─────────────────────────────────────────────

fun checkAlerts(car: CarState): List<ActiveAlert> {
    val now = System.currentTimeMillis()
    return alertRules
        .filter { it.check(car) }
        .map { ActiveAlert(rule = it, triggeredAt = now) }
        .sortedByDescending { it.rule.tier }
}

// ── Composables ──────────────────────────────────────────────────

/**
 * AlertBanner — shows the highest-severity active alert as a banner.
 *
 * Tier 2: amber background, amber border, amber text, warning icon.
 * Tier 3: red background, red border, pulsing red glow, error icon.
 *
 * Auto-cycles through multiple alerts every 3 seconds.
 * Animated entry via slideInVertically.
 */
@Composable
fun AlertBanner(alerts: List<ActiveAlert>, modifier: Modifier = Modifier) {
    if (alerts.isEmpty()) return

    // Cycle through alerts every 3 seconds
    var currentIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(alerts.size) {
        currentIndex = 0
        if (alerts.size > 1) {
            while (true) {
                delay(3000L)
                currentIndex = (currentIndex + 1) % alerts.size
            }
        }
    }

    val safeIndex = currentIndex.coerceIn(0, alerts.lastIndex)
    val alert = alerts[safeIndex]
    val isCritical = alert.rule.tier == 3

    val bgColor: Color
    val borderColor: Color
    val textColor: Color
    val iconTint: Color

    if (isCritical) {
        bgColor = C.Red.copy(alpha = 0.20f)
        borderColor = C.Red.copy(alpha = 0.40f)
        textColor = C.Red
        iconTint = C.Red
    } else {
        bgColor = C.Amber.copy(alpha = 0.15f)
        borderColor = C.Amber.copy(alpha = 0.30f)
        textColor = C.Amber
        iconTint = C.Amber
    }

    // Pulse animation for tier 3
    val pulseAlpha = if (isCritical) {
        val infiniteTransition = rememberInfiniteTransition(label = "alertPulse")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.0f,
            targetValue = 0.25f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 600,
                    easing = FastOutSlowInEasing,
                ),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "pulseGlow",
        )
        alpha
    } else {
        0f
    }

    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        ),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(durationMillis = 300),
        ),
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .then(
                    if (isCritical) {
                        Modifier.drawBehind {
                            drawRoundRect(
                                color = C.Red.copy(alpha = pulseAlpha),
                                cornerRadius = CornerRadius(10.dp.toPx()),
                            )
                        }
                    } else {
                        Modifier
                    }
                )
                .background(bgColor)
                .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = if (isCritical) Icons.Outlined.Warning else alert.rule.icon,
                    contentDescription = alert.rule.label,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = alert.rule.message,
                    style = TextStyle(
                        color = textColor,
                        fontSize = if (isCritical) 13.sp else 12.sp,
                        fontWeight = if (isCritical) FontWeight.Bold else FontWeight.SemiBold,
                        letterSpacing = if (isCritical) 1.0.sp else 0.4.sp,
                    ),
                )
                if (alerts.size > 1) {
                    Text(
                        text = "${safeIndex + 1}/${alerts.size}",
                        style = TextStyle(
                            color = textColor.copy(alpha = 0.5f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
            }
        }
    }
}

/**
 * AlertOverlay — full-width red flash banner for Tier 3 critical alerts.
 *
 * Pulses with a red glow animation. Shows icon + large bold text.
 * Stays visible until the condition resolves.
 * Intended to overlay content at the top of the screen.
 */
@Composable
fun AlertOverlay(alerts: List<ActiveAlert>, modifier: Modifier = Modifier) {
    val criticalAlerts = alerts.filter { it.rule.tier == 3 }
    if (criticalAlerts.isEmpty()) return

    // Cycle through critical alerts every 3 seconds
    var currentIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(criticalAlerts.size) {
        currentIndex = 0
        if (criticalAlerts.size > 1) {
            while (true) {
                delay(3000L)
                currentIndex = (currentIndex + 1) % criticalAlerts.size
            }
        }
    }

    val safeIndex = currentIndex.coerceIn(0, criticalAlerts.lastIndex)
    val alert = criticalAlerts[safeIndex]

    // Pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "overlayPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 500,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "overlayGlow",
    )

    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        ),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(durationMillis = 250),
        ),
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRoundRect(
                        color = C.Red.copy(alpha = pulseAlpha),
                        cornerRadius = CornerRadius(12.dp.toPx()),
                    )
                }
                .background(
                    color = C.Red.copy(alpha = 0.20f),
                    shape = RoundedCornerShape(12.dp),
                )
                .border(1.dp, C.Red.copy(alpha = 0.50f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = alert.rule.label,
                    tint = C.Red,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = alert.rule.message,
                    style = TextStyle(
                        color = C.Red,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                    ),
                )
            }
        }
    }
}
