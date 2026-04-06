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
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

        // ── Connection ──
        GlassCard(Modifier.fillMaxWidth()) {
            LabelText("CONNECTION")
            Spacer(Modifier.height(6.dp))
            SettingRow("OBD-II Status", if (car.obdConnected) "Connected" else "Disconnected",
                if (car.obdConnected) C.Green else C.Amber)
            SettingRow("GPS", if (car.gpsActive) "Active (${car.satellites} satellites)" else "Inactive",
                if (car.gpsActive) C.Green else C.Amber)
            SettingRow("Spotify", if (car.mediaPlaying) "Playing" else "Idle",
                if (car.mediaPlaying) C.Green else C.TextMuted)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton("Reconnect OBD", C.Blue, Modifier.weight(1f)) { vm.reconnectObd() }
                ActionButton("Open Spotify", C.Green, Modifier.weight(1f)) { vm.openSpotify() }
            }
        }

        // ── Vehicle Info ──
        GlassCard(Modifier.fillMaxWidth()) {
            LabelText("VEHICLE")
            Spacer(Modifier.height(6.dp))
            InfoRow("Make / Model", "Volkswagen Atlas Cross Sport")
            InfoRow("Year", "2024")
            InfoRow("Engine", "2.0T TSI (235 HP / 258 lb-ft)")
            InfoRow("Transmission", "8-Speed Aisin Automatic")
            InfoRow("Drivetrain", "4MOTION AWD")
            InfoRow("Tank", "18.6 gallons")
            InfoRow("Odometer", "%,d mi".format(car.odometer))
        }

        // ── Display ──
        GlassCard(Modifier.fillMaxWidth()) {
            LabelText("DISPLAY")
            Spacer(Modifier.height(6.dp))
            InfoRow("Speed Limit", "${car.speedLimit} MPH")
            InfoRow("Auto-HUD", "> 45 MPH (OBD required)")
            InfoRow("HUD Deactivate", "< 10 MPH")
            InfoRow("Units", "MPH / \u00b0F / PSI")
            InfoRow("Theme", "Dark")
        }

        // ── About ──
        GlassCard(Modifier.fillMaxWidth()) {
            LabelText("ABOUT")
            Spacer(Modifier.height(6.dp))
            InfoRow("App", "AutoHub OS 2.0")
            InfoRow("Platform", "Ottocast P3 Pro")
            InfoRow("Android", "13 (API 33)")
            InfoRow("OBD Protocol", "ELM327 / ISO 14230")
            InfoRow("GPS Provider", "Device GPS")
            InfoRow("Media", "Spotify Broadcast API")
        }
    }
}

@Composable
private fun SettingRow(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        Arrangement.SpaceBetween, Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            StatusDot(color, 5.dp)
            Text(label, style = TextStyle(fontSize = 12.sp, color = C.TextSub))
        }
        Text(value, style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Light, color = color))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 1.dp),
        Arrangement.SpaceBetween, Alignment.CenterVertically
    ) {
        Text(label, style = TextStyle(fontSize = 11.sp, color = C.TextMuted))
        Text(value, style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Light, color = C.TextPrimary))
    }
}

@Composable
private fun ActionButton(text: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.1f))
            .border(0.5.dp, color.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text.uppercase(),
            style = TextStyle(
                fontSize = 10.sp, fontWeight = FontWeight.Bold,
                color = color, letterSpacing = 1.sp
            )
        )
    }
}
