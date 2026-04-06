package com.autohub.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autohub.app.data.CarViewModel
import com.autohub.app.ui.components.LabelText
import com.autohub.app.ui.components.ProgressBar
import com.autohub.app.ui.components.StatusDot
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
    var time by remember { mutableStateOf("") }
    var ampm by remember { mutableStateOf("") }

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

            // ═════════════��═════════════════════════════════
            //  STATUS BAR — persistent driving info
            // ════════════════════════════════════════��══════
            Row(
                Modifier.fillMaxWidth()
                    .background(C.DockBg)
                    .border(width = 0.5.dp, C.DockBorder, RoundedCornerShape(0.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Speed
                Text("${car.speed}", TextStyle(22.sp, FontWeight.Thin, C.TextPrimary, letterSpacing = (-0.5).sp))
                Text(" MPH", TextStyle(8.sp, FontWeight.Bold, C.TextMuted), Modifier.padding(bottom = 2.dp))
                Spacer(Modifier.width(8.dp))

                // Gear
                Box(
                    Modifier.size(20.dp).clip(RoundedCornerShape(5.dp))
                        .background(C.BlueDim)
                        .border(0.5.dp, C.Blue.copy(alpha = 0.2f), RoundedCornerShape(5.dp)),
                    contentAlignment = Alignment.Center
                ) { Text(car.gear, TextStyle(10.sp, FontWeight.Light, C.Blue)) }
                Spacer(Modifier.width(8.dp))

                // RPM bar
                Box(Modifier.width(80.dp)) {
                    ProgressBar(car.rpm.toFloat(), 8000f,
                        if (car.rpm > 5000) C.Red else C.Blue, 4.dp)
                }
                Text(" %.1fK".format(car.rpm / 1000f),
                    TextStyle(7.sp, FontWeight.Bold, C.TextMuted), Modifier.padding(start = 4.dp))

                Spacer(Modifier.weight(1f))

                // Weather
                Text("${car.weatherIcon} ${car.outsideTemp}\u00b0",
                    TextStyle(10.sp, color = C.TextSecondary))
                Spacer(Modifier.width(12.dp))
                Box(Modifier.width(0.5.dp).height(14.dp).background(C.TextFaint))
                Spacer(Modifier.width(12.dp))

                // Signal bars
                Row(horizontalArrangement = Arrangement.spacedBy(1.5.dp), verticalAlignment = Alignment.Bottom) {
                    for (i in 1..5) {
                        Box(
                            Modifier.width(3.dp).height((3 + i * 1.5f).dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(if (i <= car.signalStrength) C.Green else C.TextFaint)
                        )
                    }
                }
                Text(" ${car.phoneBattery}%", TextStyle(8.sp, color = C.TextSub), Modifier.padding(start = 4.dp))

                Spacer(Modifier.width(12.dp))
                Box(Modifier.width(0.5.dp).height(14.dp).background(C.TextFaint))
                Spacer(Modifier.width(12.dp))

                // Live
                StatusDot(C.Green, 4.dp)
                Text(" LIVE", TextStyle(6.sp, FontWeight.Bold, C.TextMuted, letterSpacing = 0.6.sp))
                Spacer(Modifier.width(8.dp))

                // Clock
                Text(time, TextStyle(16.sp, FontWeight.Thin, C.TextPrimary, letterSpacing = (-0.5).sp))
                Text(" $ampm", TextStyle(6.sp, FontWeight.Bold, C.TextMuted))
            }

            // ════════════════════════════���══════════════════
            //  BODY — Dock + Content
            // ════════════════════════════════════════��══════
            Row(Modifier.weight(1f)) {

                // ── Navigation Dock ──
                Column(
                    Modifier.width(48.dp).fillMaxHeight()
                        .background(C.DockBg)
                        .border(width = 0.5.dp, C.DockBorder, RoundedCornerShape(0.dp))
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Logo
                    Box(
                        Modifier.size(30.dp).clip(RoundedCornerShape(8.dp))
                            .background(C.BlueDim)
                            .border(0.5.dp, C.Blue.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) { Text("\u25c8", TextStyle(12.sp, color = C.Blue)) }

                    // Nav items
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        DockItem("\u2302", "HOME", "dashboard", tab) { tab = it }
                        DockItem("\u2299", "PERF", "performance", tab) { tab = it }
                        DockItem("\u25c7", "CAR", "vehicle", tab) { tab = it }
                        DockItem("\u2731", "HVAC", "climate", tab) { tab = it }
                        DockItem("\u266a", "MEDIA", "media", tab) { tab = it }
                        DockItem("\u25ce", "NAV", "nav", tab) { tab = it }
                    }

                    // Settings
                    Text("\u2699", TextStyle(14.sp, color = C.TextMuted))
                }

                // ── Main Content ──
                Box(
                    Modifier.weight(1f).fillMaxHeight()
                        .padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 4.dp)
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
                            "media" -> MediaScreen(car)
                            "nav" -> NavScreen(car)
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════════
            //  FOOTER
            // ═══════════════════════════════════════════��═══
            Box(Modifier.fillMaxWidth().padding(vertical = 4.dp), Alignment.Center) {
                Text(
                    "AUTOHUB OS 2.0  \u2022  OTTOCAST P3 PRO  \u2022  SNAPDRAGON 6225  \u2022  ANDROID 13",
                    TextStyle(5.sp, FontWeight.Bold, C.TextMuted, letterSpacing = 1.4.sp)
                )
            }
        }
    }
}

@Composable
private fun DockItem(icon: String, label: String, id: String, activeTab: String, onTap: (String) -> Unit) {
    val active = activeTab == id
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).clickable { onTap(id) }
            .padding(vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (active) {
                Box(Modifier.width(2.dp).height(16.dp).clip(RoundedCornerShape(2.dp)).background(C.Blue))
                Spacer(Modifier.width(3.dp))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(icon, TextStyle(14.sp, color = if (active) C.Blue else C.TextMuted))
                Text(label, TextStyle(5.sp, FontWeight.Bold, if (active) C.Blue else C.TextMuted, letterSpacing = 0.5.sp))
            }
        }
    }
}
