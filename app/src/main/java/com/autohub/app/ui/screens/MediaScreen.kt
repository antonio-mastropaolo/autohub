package com.autohub.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autohub.app.data.CarState
import com.autohub.app.ui.components.*
import com.autohub.app.ui.theme.C

@Composable
fun MediaScreen(car: CarState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

        // ── Now Playing ──
        GlassCard(Modifier.fillMaxWidth()) {
            LabelText("NOW PLAYING")
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album art
                Box(
                    Modifier.size(85.dp).clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(C.Purple, C.Blue))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("\u266a", TextStyle(32.sp, color = Color.White.copy(alpha = 0.25f)))
                }

                Column(Modifier.weight(1f)) {
                    Text(car.mediaTitle, TextStyle(16.sp, FontWeight.Light, C.TextPrimary))
                    Spacer(Modifier.height(2.dp))
                    Text(car.mediaArtist, TextStyle(11.sp, color = C.TextSub))

                    Spacer(Modifier.height(10.dp))
                    ProgressBar(car.mediaProgress, 1f, C.Purple, 3.dp)
                    Spacer(Modifier.height(3.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text(car.mediaCurrent, TextStyle(8.sp, color = C.TextMuted))
                        Text(car.mediaDuration, TextStyle(8.sp, color = C.TextMuted))
                    }

                    Spacer(Modifier.height(10.dp))

                    // Controls
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("\u23ee", TextStyle(20.sp, color = C.TextSub))
                        Spacer(Modifier.width(24.dp))
                        Box(
                            Modifier.size(38.dp).clip(CircleShape)
                                .background(C.Purple.copy(alpha = 0.15f))
                                .border(1.dp, C.Purple.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (car.mediaPlaying) "\u23f8" else "\u25b6",
                                TextStyle(16.sp, color = C.Purple)
                            )
                        }
                        Spacer(Modifier.width(24.dp))
                        Text("\u23ed", TextStyle(20.sp, color = C.TextSub))
                    }
                }
            }
        }

        // ── EQ + Volume + Source ──
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassCard(Modifier.weight(1f)) {
                LabelText("EQUALIZER")
                Spacer(Modifier.height(8.dp))
                EQVisualizer(car.eqBands, C.Purple, Modifier.fillMaxWidth().height(55.dp))
            }
            GlassCard(Modifier.weight(0.5f)) {
                LabelText("VOLUME")
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${car.volume}", TextStyle(22.sp, FontWeight.Thin, C.TextPrimary))
                    Text("%", TextStyle(8.sp, FontWeight.Bold, C.TextMuted),
                        Modifier.padding(bottom = 3.dp))
                }
                Spacer(Modifier.height(4.dp))
                ProgressBar(car.volume.toFloat(), 100f, C.Purple)
            }
            GlassCard(Modifier.weight(0.6f)) {
                LabelText("SOURCE")
                Spacer(Modifier.height(6.dp))
                SourceRow("Bluetooth", car.mediaSource == "Bluetooth")
                SourceRow("USB", car.mediaSource == "USB")
                SourceRow("Radio", car.mediaSource == "Radio")
                SourceRow("Streaming", car.mediaSource == "Streaming")
            }
        }
    }
}

@Composable
private fun SourceRow(name: String, active: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        StatusDot(if (active) C.Purple else C.TextMuted, 4.dp)
        Text(name, TextStyle(10.sp, color = if (active) C.TextPrimary else C.TextMuted))
    }
}
