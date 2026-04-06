package com.autohub.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autohub.app.data.CarViewModel
import com.autohub.app.ui.components.StatusDot
import com.autohub.app.ui.components.AlertBanner
import com.autohub.app.ui.components.AlertOverlay
import com.autohub.app.ui.components.checkAlerts
import com.autohub.app.ui.screens.*
import com.autohub.app.ui.theme.C
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val ctrl = WindowInsetsControllerCompat(window, window.decorView)
        ctrl.hide(WindowInsetsCompat.Type.systemBars())
        ctrl.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent { AutoHubOS() }
    }
}

@Composable
fun AutoHubOS(vm: CarViewModel = viewModel()) {
    val car = vm.state
    var tab by remember { mutableStateOf("dashboard") }
    var previousTab by remember { mutableStateOf("dashboard") }
    var hudActive by remember { mutableStateOf(false) }
    var time by remember { mutableStateOf("") }
    var ampm by remember { mutableStateOf("") }

    // Auto-HUD: activate at >45 MPH (only when OBD is live), deactivate at <10 MPH
    val activeAlerts = remember(car) { checkAlerts(car) }
    LaunchedEffect(car.speed, car.obdConnected) {
        if (car.obdConnected && car.speed > 45 && !hudActive) {
            kotlinx.coroutines.delay(5000)
            if (car.speed > 45) { previousTab = tab; hudActive = true; tab = "hud" }
        } else if (car.speed < 10 && hudActive) {
            kotlinx.coroutines.delay(5000)
            if (car.speed < 10) { hudActive = false; tab = previousTab }
        }
    }

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* permissions granted, OBD will auto-connect from ViewModel */ }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(arrayOf(
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ))
    }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Calendar.getInstance()
            val h = now.get(Calendar.HOUR).let { if (it == 0) 12 else it }
            time = "$h:%02d".format(now.get(Calendar.MINUTE))
            ampm = if (now.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
            kotlinx.coroutines.delay(5000)
        }
    }

    Box(Modifier.fillMaxSize().background(C.Background)) {
        // Ambient glows
        Box(
            Modifier.offset((-60).dp, (-120).dp).size(400.dp).clip(CircleShape)
                .background(Brush.radialGradient(listOf(C.Blue.copy(alpha = 0.08f), Color.Transparent)))
        )
        Box(
            Modifier.align(Alignment.BottomEnd).offset(80.dp, 60.dp).size(300.dp).clip(CircleShape)
                .background(Brush.radialGradient(listOf(C.Purple.copy(alpha = 0.05f), Color.Transparent)))
        )

        Column(Modifier.fillMaxSize()) {

            // ════════════════════════════════════════════════
            //  STATUS BAR — persistent driving info
            // ════════════════════════════════════════════════
            Row(
                Modifier.fillMaxWidth()
                    .background(C.DockBg)
                    .border(width = 0.5.dp, C.DockBorder, RoundedCornerShape(0.dp))
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Speed + Gear (left cluster)
                Text("${car.speed}", style = TextStyle(color = C.TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Thin, letterSpacing = (-0.5).sp))
                Text(" MPH", style = TextStyle(color = C.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold), modifier = Modifier.padding(bottom = 2.dp))
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier.size(28.dp).clip(RoundedCornerShape(6.dp))
                        .background(C.BlueDim)
                        .border(0.5.dp, C.Blue.copy(alpha = 0.2f), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) { Text(car.gear, style = TextStyle(color = C.Blue, fontSize = 15.sp, fontWeight = FontWeight.Light)) }

                Spacer(Modifier.weight(1f))

                // OBD status (center)
                StatusDot(if (car.obdConnected) C.Green else Color(0xFFFFB300), 5.dp)
                Spacer(Modifier.width(4.dp))
                Text(
                    if (car.obdConnected) "OBD" else "NO OBD",
                    style = TextStyle(
                        color = if (car.obdConnected) C.Green else Color(0xFFFFB300),
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp
                    )
                )

                Spacer(Modifier.weight(1f))

                // Temperature + Clock (right)
                Text("${car.outsideTemp}\u00b0F", style = TextStyle(fontSize = 13.sp, color = C.TextSecondary))
                Spacer(Modifier.width(14.dp))
                Text(time, style = TextStyle(color = C.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Thin, letterSpacing = (-0.5).sp))
                Text(" $ampm", style = TextStyle(color = C.TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold))
            }

            // ════════════════════════════════════════════════
            //  BODY — Dock + Content
            // ════════════════════════════════════════════════
            Row(Modifier.weight(1f)) {

                // ── Navigation Dock — narrow for 720p ──
                Column(
                    Modifier.width(62.dp).fillMaxHeight()
                        .background(C.DockBg)
                        .border(width = 0.5.dp, C.DockBorder, RoundedCornerShape(0.dp))
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Logo
                    Box(
                        Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                            .background(C.BlueDim)
                            .border(0.5.dp, C.Blue.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Hub,
                            contentDescription = "AutoHub",
                            tint = C.Blue,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Nav items
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        DockItem(Icons.Outlined.Dashboard, "HOME", "dashboard", tab) { hudActive = false; tab = it }
                        DockItem(Icons.Outlined.Speed, "PERF", "performance", tab) { hudActive = false; tab = it }
                        DockItem(Icons.Outlined.DirectionsCar, "CAR", "vehicle", tab) { hudActive = false; tab = it }
                        DockItem(Icons.Outlined.AcUnit, "HVAC", "climate", tab) { hudActive = false; tab = it }
                        DockItem(Icons.Outlined.MusicNote, "MEDIA", "media", tab) { hudActive = false; tab = it }
                        DockItem(Icons.Outlined.Explore, "NAV", "nav", tab) { hudActive = false; tab = it }
                        DockItem(Icons.Outlined.Build, "DIAG", "diagnostics", tab) { hudActive = false; tab = it }
                        DockItem(Icons.Outlined.Visibility, "HUD", "hud", tab) { hudActive = true; tab = it }
                    }

                    // Settings
                    DockItem(Icons.Outlined.Settings, "SET", "settings", tab) { hudActive = false; tab = it }
                }

                // ── Main Content ──
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    // Alert banner (between status bar and content)
                    if (activeAlerts.isNotEmpty()) {
                        AlertBanner(
                            alerts = activeAlerts,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Box(
                        Modifier.weight(1f).fillMaxWidth()
                            .padding(start = 8.dp, end = 12.dp, top = if (activeAlerts.isEmpty()) 8.dp else 4.dp, bottom = 4.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        AnimatedContent(
                            targetState = tab,
                            transitionSpec = {
                                fadeIn(tween(200)) + slideInVertically(tween(250)) { 20 } togetherWith
                                    fadeOut(tween(150))
                            },
                            label = "screen"
                        ) { t ->
                            when (t) {
                                "dashboard" -> DashboardScreen(car)
                                "performance" -> PerformanceScreen(car)
                                "vehicle" -> VehicleScreen(car)
                                "climate" -> ClimateScreen(car)
                                "media" -> MediaScreen(car, vm)
                                "nav" -> NavScreen(car)
                                "diagnostics" -> DiagnosticsScreen(car)
                                "hud" -> HudScreen(car)
                                "settings" -> SettingsScreen(car, vm)
                            }
                        }
                    }

                    // Critical alert overlay
                    val criticalAlerts = activeAlerts.filter { it.rule.tier == 3 }
                    if (criticalAlerts.isNotEmpty()) {
                        AlertOverlay(alerts = criticalAlerts, modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            // Footer removed — maximize vertical space on 720p
        }
    }
}

@Composable
private fun DockItem(icon: ImageVector, label: String, id: String, activeTab: String, onTap: (String) -> Unit) {
    val active = activeTab == id
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).clickable { onTap(id) }
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (active) {
                Box(Modifier.width(2.5.dp).height(22.dp).clip(RoundedCornerShape(2.dp)).background(C.Blue))
                Spacer(Modifier.width(2.dp))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (active) C.Blue else C.TextMuted,
                    modifier = Modifier.size(24.dp)
                )
                Text(label, style = TextStyle(color = if (active) C.Blue else C.TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp))
            }
        }
    }
}
