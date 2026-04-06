package com.autohub.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autohub.app.data.CarState
import com.autohub.app.ui.components.*
import com.autohub.app.ui.theme.C
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

// ═══════════════════════════════════════════════════════════════
//  DTC Code Data
// ═══════════════════════════════════════════════════════════════

data class DtcCode(
    val code: String,
    val title: String,
    val description: String,
    val severity: String, // "Critical", "Moderate", "Info"
    val causes: List<String>,
)

private val DTC_DATABASE = listOf(
    DtcCode(
        code = "P0300",
        title = "Random/Multiple Cylinder Misfire",
        description = "The ECU has detected misfires occurring across multiple cylinders without a clear pattern. This can cause rough idle, loss of power, and increased emissions.",
        severity = "Critical",
        causes = listOf(
            "Worn or fouled spark plugs",
            "Failing ignition coils",
            "Vacuum leak in intake manifold",
            "Low fuel pressure or clogged injectors",
            "Worn timing chain (common on EA888 Gen3)"
        )
    ),
    DtcCode(
        code = "P0301",
        title = "Cylinder 1 Misfire Detected",
        description = "The ECU has detected repeated misfires on cylinder 1. This typically points to a localized ignition or fueling issue on that specific cylinder.",
        severity = "Critical",
        causes = listOf(
            "Faulty ignition coil on cylinder 1",
            "Worn spark plug on cylinder 1",
            "Leaking fuel injector",
            "Low compression (valve or ring issue)"
        )
    ),
    DtcCode(
        code = "P0302",
        title = "Cylinder 2 Misfire Detected",
        description = "Repeated misfires detected on cylinder 2. The ECU monitors crankshaft acceleration and has identified cylinder 2 as consistently underperforming.",
        severity = "Critical",
        causes = listOf(
            "Faulty ignition coil on cylinder 2",
            "Worn spark plug on cylinder 2",
            "Carbon buildup on intake valve (direct injection)",
            "Injector circuit fault"
        )
    ),
    DtcCode(
        code = "P0303",
        title = "Cylinder 3 Misfire Detected",
        description = "The ECU has flagged cylinder 3 for persistent misfires. This can result in rough running and potential catalytic converter damage if not addressed.",
        severity = "Critical",
        causes = listOf(
            "Faulty ignition coil on cylinder 3",
            "Worn spark plug on cylinder 3",
            "Intake manifold runner stuck",
            "Compression loss on cylinder 3"
        )
    ),
    DtcCode(
        code = "P0304",
        title = "Cylinder 4 Misfire Detected",
        description = "Cylinder 4 is experiencing consistent misfires. Unburned fuel entering the exhaust can overheat and damage the catalytic converter over time.",
        severity = "Critical",
        causes = listOf(
            "Faulty ignition coil on cylinder 4",
            "Worn spark plug on cylinder 4",
            "Fuel injector failure",
            "Valve seal leak on cylinder 4"
        )
    ),
    DtcCode(
        code = "P0171",
        title = "System Too Lean (Bank 1)",
        description = "The air-fuel mixture on bank 1 is running leaner than the ECU can compensate for. Long-term fuel trims are excessively positive, indicating too much air or too little fuel.",
        severity = "Moderate",
        causes = listOf(
            "Vacuum leak (crankcase vent valve, PCV)",
            "Faulty mass airflow sensor (MAF)",
            "Weak fuel pump or clogged filter",
            "Leaking intake manifold gasket",
            "Cracked or disconnected vacuum hose"
        )
    ),
    DtcCode(
        code = "P0420",
        title = "Catalyst System Efficiency Below Threshold",
        description = "The downstream oxygen sensor indicates the catalytic converter is not reducing emissions effectively. The converter may be degraded or failing.",
        severity = "Moderate",
        causes = listOf(
            "Aging or failed catalytic converter",
            "Exhaust leak near oxygen sensors",
            "Contaminated catalyst (oil or coolant leak)",
            "Faulty downstream O2 sensor",
            "Engine running rich for extended period"
        )
    ),
    DtcCode(
        code = "P0507",
        title = "Idle Control System RPM Higher Than Expected",
        description = "The engine idle speed is consistently higher than the ECU target. The throttle body or idle air control system is allowing too much air at idle.",
        severity = "Info",
        causes = listOf(
            "Dirty or sticking throttle body",
            "Vacuum leak raising idle speed",
            "Throttle body adaptation needed",
            "Faulty idle air control valve"
        )
    ),
    DtcCode(
        code = "P2187",
        title = "System Too Lean at Idle",
        description = "At idle, the fuel mixture is too lean. This is distinct from P0171 as it only occurs at idle, often pointing to a small vacuum leak or PCV issue common on VW/Audi EA888 engines.",
        severity = "Moderate",
        causes = listOf(
            "PCV valve or diaphragm failure (very common on EA888)",
            "Small vacuum leak at idle",
            "MAF sensor reading low at idle",
            "Intake manifold runner flap issues"
        )
    ),
    DtcCode(
        code = "P0456",
        title = "EVAP System Small Leak",
        description = "The evaporative emission system has detected a small leak. The system that captures fuel vapors from the tank is not holding pressure as expected.",
        severity = "Info",
        causes = listOf(
            "Loose or worn gas cap",
            "Cracked EVAP hose or canister",
            "Faulty purge valve or vent valve",
            "Small leak in fuel tank filler neck"
        )
    ),
    DtcCode(
        code = "P0101",
        title = "MAF Sensor Range/Performance",
        description = "The mass airflow sensor signal is outside expected parameters. The ECU is receiving airflow readings that do not match expected values for current operating conditions.",
        severity = "Moderate",
        causes = listOf(
            "Dirty or contaminated MAF sensor element",
            "Air filter restriction",
            "Intake air leak after the MAF sensor",
            "Faulty MAF sensor",
            "Rodent damage to wiring harness"
        )
    ),
    DtcCode(
        code = "P2015",
        title = "Intake Manifold Runner Position Sensor",
        description = "The intake manifold flap position sensor is reporting an incorrect position or is out of range. This is a well-known issue on VW/Audi 2.0T engines.",
        severity = "Moderate",
        causes = listOf(
            "Worn intake manifold flap motor (VW TSB item)",
            "Broken manifold runner linkage",
            "Failed position sensor",
            "Carbon buildup restricting flap movement"
        )
    ),
    DtcCode(
        code = "P0016",
        title = "Crankshaft/Camshaft Position Correlation",
        description = "The crankshaft and camshaft position sensors are not in the expected correlation. This can indicate timing chain stretch, a common concern on EA888 engines.",
        severity = "Critical",
        causes = listOf(
            "Stretched timing chain (EA888 Gen1/Gen2)",
            "Worn timing chain tensioner",
            "Faulty camshaft position sensor",
            "Incorrect valve timing after service",
            "VVT solenoid malfunction"
        )
    ),
    DtcCode(
        code = "P0299",
        title = "Turbo/Supercharger Underboost",
        description = "The turbocharger is not producing the expected boost pressure. The ECU has detected actual boost falling below the target under load.",
        severity = "Critical",
        causes = listOf(
            "Boost leak in charge piping (very common VW TSI issue)",
            "Failing wastegate actuator",
            "Worn turbo bearings or shaft play",
            "Diverter valve failure",
            "Clogged intercooler"
        )
    ),
    DtcCode(
        code = "P0411",
        title = "Secondary Air Injection System Incorrect Flow",
        description = "The secondary air injection system is not flowing the expected amount of air during cold start. This system helps the catalytic converter reach operating temperature quickly.",
        severity = "Info",
        causes = listOf(
            "Failed secondary air pump",
            "Clogged or stuck combi valve",
            "Blocked air injection passages",
            "Faulty air pump relay"
        )
    ),
    DtcCode(
        code = "P2181",
        title = "Cooling System Performance",
        description = "The engine coolant is not reaching operating temperature within the expected time, or the thermostat is not regulating temperature properly.",
        severity = "Moderate",
        causes = listOf(
            "Thermostat stuck open",
            "Coolant temperature sensor fault",
            "Low coolant level",
            "Radiator fan running continuously"
        )
    ),
)

// ═══════════════════════════════════════════════════════════════
//  Scan State
// ═══════════════════════════════════════════════════════════════

private enum class ScanState { IDLE, SCANNING, RESULTS }

// ═══════════════════════════════════════════════════════════════
//  DiagnosticsScreen
// ═══════════════════════════════════════════════════════════════

@Composable
fun DiagnosticsScreen(car: CarState) {
    var scanState by remember { mutableStateOf(ScanState.IDLE) }
    var foundCodes by remember { mutableStateOf<List<DtcCode>>(emptyList()) }
    var lastScanTime by remember { mutableStateOf<String?>(null) }
    var expandedCode by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    // Scanning coroutine
    LaunchedEffect(key1 = scanState) {
        if (scanState == ScanState.SCANNING) {
            delay(timeMillis = 2000L)
            // 70% chance of 0 codes, otherwise 1-3 random codes
            val roll = Random.nextFloat()
            foundCodes = if (roll < 0.70f) {
                emptyList()
            } else {
                val count = Random.nextInt(from = 1, until = 4)
                DTC_DATABASE.shuffled().take(count)
            }
            val sdf = SimpleDateFormat("hh:mm:ss a", Locale.US)
            lastScanTime = sdf.format(Date())
            scanState = ScanState.RESULTS
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.verticalScroll(scrollState)
    ) {

        // ── Top Section: Status Card ──────────────────────────────
        GlassCard(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    LabelText("DIAGNOSTICS")
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StatusDot(
                            color = if (car.obdConnected) C.Green else C.Amber,
                            size = 6.dp
                        )
                        Text(
                            text = if (car.obdConnected) "Connected" else "Not Connected",
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (car.obdConnected) C.Green else C.Amber
                            )
                        )
                    }
                    if (lastScanTime != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Last scan: $lastScanTime",
                            style = TextStyle(
                                fontSize = 10.sp,
                                color = C.TextMuted
                            )
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // SCAN button
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(C.CyanDim)
                            .border(1.dp, C.Cyan.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                            .clickable {
                                if (scanState != ScanState.SCANNING) {
                                    scanState = ScanState.SCANNING
                                    expandedCode = null
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = "Scan",
                                tint = C.Cyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "SCAN",
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = C.Cyan,
                                    letterSpacing = 1.sp
                                )
                            )
                        }
                    }

                    // CLEAR CODES button
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(C.RedDim)
                            .border(1.dp, C.Red.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                            .clickable {
                                foundCodes = emptyList()
                                scanState = ScanState.RESULTS
                                expandedCode = null
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Error,
                                contentDescription = "Clear Codes",
                                tint = C.Red,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "CLEAR",
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = C.Red,
                                    letterSpacing = 1.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        // ── Middle Section: Scanning Animation or DTC List ────────
        when (scanState) {
            ScanState.IDLE -> {
                // Initial state — prompt to scan
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Ready to scan",
                            tint = C.TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Ready to Scan",
                            style = TextStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Light,
                                color = C.TextPrimary
                            )
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Tap SCAN to read diagnostic trouble codes via OBD-II Mode 03",
                            style = TextStyle(
                                fontSize = 11.sp,
                                color = C.TextSub
                            )
                        )
                    }
                }
            }

            ScanState.SCANNING -> {
                // Pulsing scanning animation
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ScanningAnimation()
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Scanning ECU...",
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Light,
                                color = C.Cyan
                            )
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Reading diagnostic trouble codes",
                            style = TextStyle(
                                fontSize = 11.sp,
                                color = C.TextSub
                            )
                        )
                    }
                }
            }

            ScanState.RESULTS -> {
                if (foundCodes.isEmpty()) {
                    // No trouble codes — all clear
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = "No trouble codes",
                                tint = C.Green,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "No Trouble Codes",
                                style = TextStyle(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Light,
                                    color = C.Green
                                )
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Your vehicle is running clean",
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    color = C.TextSub
                                )
                            )
                        }
                    }
                } else {
                    // DTC code cards
                    for (dtc in foundCodes) {
                        val isExpanded = expandedCode == dtc.code
                        val severityColor = when (dtc.severity) {
                            "Critical" -> C.Red
                            "Moderate" -> C.Amber
                            else -> C.Blue
                        }

                        GlassCard(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedCode = if (isExpanded) null else dtc.code
                                }
                        ) {
                            // Code + Title row
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Severity dot
                                StatusDot(color = severityColor, size = 8.dp)

                                // Code in large text
                                Text(
                                    text = dtc.code,
                                    style = TextStyle(
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Light,
                                        color = C.TextPrimary
                                    )
                                )

                                // Severity pill
                                Pill(text = dtc.severity, color = severityColor)
                            }

                            Spacer(Modifier.height(6.dp))

                            // Plain-English title
                            Text(
                                text = dtc.title,
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = C.TextPrimary
                                )
                            )

                            Spacer(Modifier.height(4.dp))

                            // Description paragraph
                            Text(
                                text = dtc.description,
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    color = C.TextSub,
                                    lineHeight = 16.sp
                                )
                            )

                            Spacer(Modifier.height(8.dp))

                            // Common Causes — expandable
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Warning,
                                    contentDescription = "Common causes",
                                    tint = C.TextMuted,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = if (isExpanded) "COMMON CAUSES" else "TAP FOR COMMON CAUSES",
                                    style = TextStyle(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = C.TextMuted,
                                        letterSpacing = 1.sp
                                    )
                                )
                            }

                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = expandVertically(),
                                exit = shrinkVertically()
                            ) {
                                Column(Modifier.padding(top = 6.dp)) {
                                    for (cause in dtc.causes) {
                                        Row(
                                            Modifier.padding(vertical = 2.dp),
                                            verticalAlignment = Alignment.Top,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "\u2022",
                                                style = TextStyle(
                                                    fontSize = 12.sp,
                                                    color = severityColor
                                                )
                                            )
                                            Text(
                                                text = cause,
                                                style = TextStyle(
                                                    fontSize = 11.sp,
                                                    color = C.TextSecondary,
                                                    lineHeight = 15.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Bottom Section: Vehicle Health Score ──────────────────
        if (scanState == ScanState.RESULTS) {
            val healthScore = calculateHealthScore(codes = foundCodes)
            val healthColor = when {
                healthScore >= 80 -> C.Green
                healthScore >= 50 -> C.Amber
                else -> C.Red
            }
            val healthLabel = when {
                healthScore >= 80 -> "Good"
                healthScore >= 50 -> "Fair"
                else -> "Poor"
            }

            GlassCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        LabelText("VEHICLE HEALTH")
                        Spacer(Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "$healthScore",
                                style = TextStyle(
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.ExtraLight,
                                    color = C.TextPrimary
                                )
                            )
                            Text(
                                text = "/100",
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = C.TextMuted
                                ),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Pill(text = healthLabel, color = healthColor)
                    }

                    MiniRingGauge(
                        value = healthScore.toFloat(),
                        maxValue = 100f,
                        color = healthColor,
                        size = 48.dp
                    )
                }

                if (foundCodes.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${foundCodes.size} code${if (foundCodes.size > 1) "s" else ""} found \u2014 " +
                                "${foundCodes.count { it.severity == "Critical" }} critical, " +
                                "${foundCodes.count { it.severity == "Moderate" }} moderate, " +
                                "${foundCodes.count { it.severity == "Info" }} info",
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = C.TextSub
                        )
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  Scanning Animation — three pulsing dots
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ScanningAnimation() {
    val transition = rememberInfiniteTransition(label = "scanPulse")

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        for (i in 0..2) {
            val alpha by transition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 600, delayMillis = i * 200),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$i"
            )
            Box(
                Modifier
                    .size(12.dp)
                    .alpha(alpha)
                    .clip(CircleShape)
                    .background(C.Cyan)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  Health Score Calculation
// ═══════════════════════════════════════════════════════════════

private fun calculateHealthScore(codes: List<DtcCode>): Int {
    if (codes.isEmpty()) return 100
    var deduction = 0
    for (code in codes) {
        deduction += when (code.severity) {
            "Critical" -> 25
            "Moderate" -> 15
            else -> 5
        }
    }
    return (100 - deduction).coerceIn(minimumValue = 0, maximumValue = 100)
}
