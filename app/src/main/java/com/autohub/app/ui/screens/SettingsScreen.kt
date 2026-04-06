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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autohub.app.data.CarState
import com.autohub.app.data.CarViewModel
import com.autohub.app.ui.components.*
import com.autohub.app.ui.theme.C

@Composable
fun SettingsScreen(car: CarState, vm: CarViewModel) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("autohub_settings", 0) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

        // ═══════════════════════════════════════════════
        //  CONNECTION
        // ═══════════════════════════════════════════════
        GlassCard(Modifier.fillMaxWidth()) {
            LabelText("CONNECTION")
            Spacer(Modifier.height(4.dp))
            SettingRow("OBD-II", if (car.obdConnected) "Connected" else "Disconnected",
                if (car.obdConnected) C.Green else C.Amber)
            SettingRow("GPS", if (car.gpsActive) "${car.satellites} satellites" else "Inactive",
                if (car.gpsActive) C.Green else C.Amber)
            SettingRow("Spotify", if (car.mediaPlaying) "Playing" else "Idle",
                if (car.mediaPlaying) C.Green else C.TextMuted)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ActionButton("RECONNECT OBD", C.Blue, Modifier.weight(1f)) { vm.reconnectObd() }
                ActionButton("OPEN SPOTIFY", C.Green, Modifier.weight(1f)) { vm.openSpotify() }
            }
        }

        // ═══════════════════════════════════════════════
        //  DASHBOARD CONFIG
        // ═══════════════════════════════════════════════
        GlassCard(Modifier.fillMaxWidth()) {
            LabelText("DASHBOARD")
            Spacer(Modifier.height(4.dp))

            var speedUnit by remember { mutableStateOf(prefs.getString("speed_unit", "MPH") ?: "MPH") }
            var tempUnit by remember { mutableStateOf(prefs.getString("temp_unit", "\u00b0F") ?: "\u00b0F") }
            var pressureUnit by remember { mutableStateOf(prefs.getString("pressure_unit", "PSI") ?: "PSI") }
            var autoHudEnabled by remember { mutableStateOf(prefs.getBoolean("auto_hud", true)) }
            var autoHudSpeed by remember { mutableStateOf(prefs.getInt("auto_hud_speed", 45)) }
            var speedLimitVal by remember { mutableStateOf(prefs.getInt("speed_limit", 65)) }

            ToggleSetting("Speed Unit", speedUnit, listOf("MPH", "KMH")) {
                speedUnit = it; prefs.edit().putString("speed_unit", it).apply()
            }
            ToggleSetting("Temperature", tempUnit, listOf("\u00b0F", "\u00b0C")) {
                tempUnit = it; prefs.edit().putString("temp_unit", it).apply()
            }
            ToggleSetting("Tire Pressure", pressureUnit, listOf("PSI", "BAR", "KPA")) {
                pressureUnit = it; prefs.edit().putString("pressure_unit", it).apply()
            }
            Spacer(Modifier.height(2.dp))
            ToggleSetting("Auto-HUD", if (autoHudEnabled) "ON" else "OFF", listOf("ON", "OFF")) {
                autoHudEnabled = it == "ON"; prefs.edit().putBoolean("auto_hud", autoHudEnabled).apply()
            }
            NumberSetting("HUD Threshold", autoHudSpeed, "MPH", 20, 80) {
                autoHudSpeed = it; prefs.edit().putInt("auto_hud_speed", it).apply()
            }
            NumberSetting("Speed Limit", speedLimitVal, "MPH", 25, 85) {
                speedLimitVal = it; prefs.edit().putInt("speed_limit", it).apply()
            }
        }

        // ═══════════════════════════════════════════════
        //  VEHICLE + ABOUT
        // ═══════════════════════════════════════════════
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GlassCard(Modifier.weight(1f)) {
                LabelText("VEHICLE")
                Spacer(Modifier.height(4.dp))
                InfoRow("Make", "Volkswagen")
                InfoRow("Model", "Atlas Cross Sport")
                InfoRow("Year", "2024")
                InfoRow("Engine", "2.0T TSI 235HP")
                InfoRow("Trans", "8-Speed Aisin")
                InfoRow("Drive", "4MOTION AWD")
                InfoRow("Tank", "18.6 gal")
                InfoRow("Odometer", "%,d mi".format(car.odometer))
            }
            GlassCard(Modifier.weight(1f)) {
                LabelText("ABOUT")
                Spacer(Modifier.height(4.dp))
                InfoRow("App", "AutoHub OS 2.0")
                InfoRow("Platform", "Ottocast P3 Pro")
                InfoRow("Android", "13 (API 33)")
                InfoRow("OBD", "ELM327 BLE")
                InfoRow("Adapter", "Hyper Tough HT500")
                InfoRow("GPS", "Device GNSS")
                InfoRow("Media", "Spotify Broadcast")
                InfoRow("Map", "OpenStreetMap")
            }
        }
    }
}

@Composable
private fun SettingRow(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        Arrangement.SpaceBetween, Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            StatusDot(color, 4.dp)
            Text(label, style = TextStyle(fontSize = 11.sp, color = C.TextSub))
        }
        Text(value, style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Light, color = color))
    }
}

@Composable
private fun ToggleSetting(label: String, current: String, options: List<String>, onChange: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        Arrangement.SpaceBetween, Alignment.CenterVertically
    ) {
        Text(label, style = TextStyle(fontSize = 11.sp, color = C.TextSub))
        Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
            for (opt in options) {
                val selected = opt == current
                Box(
                    Modifier.clip(RoundedCornerShape(6.dp))
                        .background(if (selected) C.Blue.copy(alpha = 0.15f) else Color.Transparent)
                        .border(0.5.dp, if (selected) C.Blue.copy(alpha = 0.3f) else C.TextFaint, RoundedCornerShape(6.dp))
                        .clickable { onChange(opt) }
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        opt,
                        style = TextStyle(
                            fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            color = if (selected) C.Blue else C.TextMuted,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun NumberSetting(label: String, value: Int, unit: String, min: Int, max: Int, onChange: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        Arrangement.SpaceBetween, Alignment.CenterVertically
    ) {
        Text(label, style = TextStyle(fontSize = 11.sp, color = C.TextSub))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                Modifier.size(22.dp).clip(RoundedCornerShape(4.dp))
                    .background(C.Glass)
                    .clickable { if (value > min) onChange(value - 5) },
                contentAlignment = Alignment.Center
            ) { Text("-", style = TextStyle(fontSize = 14.sp, color = C.TextSub)) }
            Text("$value $unit", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Light, color = C.TextPrimary))
            Box(
                Modifier.size(22.dp).clip(RoundedCornerShape(4.dp))
                    .background(C.Glass)
                    .clickable { if (value < max) onChange(value + 5) },
                contentAlignment = Alignment.Center
            ) { Text("+", style = TextStyle(fontSize = 14.sp, color = C.TextSub)) }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 1.dp),
        Arrangement.SpaceBetween, Alignment.CenterVertically
    ) {
        Text(label, style = TextStyle(fontSize = 10.sp, color = C.TextMuted))
        Text(value, style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Light, color = C.TextPrimary))
    }
}

@Composable
private fun ActionButton(text: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .border(0.5.dp, color.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = color, letterSpacing = 1.sp))
    }
}
