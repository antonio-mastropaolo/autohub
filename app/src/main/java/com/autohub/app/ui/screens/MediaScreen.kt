package com.autohub.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.Icon
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
import com.autohub.app.data.CarViewModel
import com.autohub.app.ui.components.*
import com.autohub.app.ui.theme.C

private val SpotifyGreen = Color(0xFF1DB954)
private val SpotifyGreenDim = Color(0x141DB954)

@Composable
fun MediaScreen(car: CarState, vm: CarViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

        // ═══════════════════════════════════════════════════════
        //  NOW PLAYING — Large album art + track info + controls
        // ═══════════════════════════════════════════════════════
        GlassCard(Modifier.fillMaxWidth()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album art placeholder
                Box(
                    Modifier.size(110.dp).clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    SpotifyGreen.copy(alpha = 0.3f),
                                    C.Purple.copy(alpha = 0.2f)
                                )
                            )
                        )
                        .border(1.dp, SpotifyGreen.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "\u266b",
                            style = TextStyle(fontSize = 36.sp, color = SpotifyGreen.copy(alpha = 0.6f))
                        )
                        if (car.mediaAlbum.isNotEmpty()) {
                            Text(
                                car.mediaAlbum,
                                style = TextStyle(
                                    fontSize = 9.sp,
                                    color = C.TextMuted,
                                    fontWeight = FontWeight.Medium,
                                ),
                                maxLines = 2,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                }

                // Track info + progress + controls
                Column(Modifier.weight(1f)) {
                    // Source badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StatusDot(SpotifyGreen, 5.dp)
                        Text(
                            car.mediaSource.uppercase(),
                            style = TextStyle(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SpotifyGreen,
                                letterSpacing = 1.5.sp
                            )
                        )
                        if (car.mediaPlaying) {
                            Pill("PLAYING", SpotifyGreen)
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    // Track title
                    Text(
                        car.mediaTitle,
                        style = TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Light,
                            color = C.TextPrimary,
                        ),
                        maxLines = 1
                    )
                    Spacer(Modifier.height(2.dp))
                    // Artist
                    Text(
                        car.mediaArtist,
                        style = TextStyle(fontSize = 16.sp, color = C.TextSub),
                        maxLines = 1
                    )

                    Spacer(Modifier.height(12.dp))

                    // Progress bar
                    ProgressBar(car.mediaProgress, 1f, SpotifyGreen, 4.dp)
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text(car.mediaCurrent, style = TextStyle(fontSize = 12.sp, color = C.TextMuted))
                        Text(car.mediaDuration, style = TextStyle(fontSize = 12.sp, color = C.TextMuted))
                    }

                    Spacer(Modifier.height(12.dp))

                    // Playback controls
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous
                        Box(
                            Modifier.size(44.dp).clip(CircleShape)
                                .background(C.Glass)
                                .clickable { vm.spotifyPrevious() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.SkipPrevious,
                                contentDescription = "Previous",
                                tint = C.TextSub,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(Modifier.width(20.dp))

                        // Play/Pause
                        Box(
                            Modifier.size(56.dp).clip(CircleShape)
                                .background(SpotifyGreen.copy(alpha = 0.15f))
                                .border(1.dp, SpotifyGreen.copy(alpha = 0.3f), CircleShape)
                                .clickable { vm.spotifyPlayPause() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (car.mediaPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                                contentDescription = if (car.mediaPlaying) "Pause" else "Play",
                                tint = SpotifyGreen,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Spacer(Modifier.width(20.dp))

                        // Next
                        Box(
                            Modifier.size(44.dp).clip(CircleShape)
                                .background(C.Glass)
                                .clickable { vm.spotifyNext() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.SkipNext,
                                contentDescription = "Next",
                                tint = C.TextSub,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
        }

        // ═══════════════════════════════════════════════════════
        //  BOTTOM: EQ + Volume + Open Spotify
        // ═══════════════════════════════════════════════════════
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // EQ Visualizer
            GlassCard(Modifier.weight(1f)) {
                LabelText("EQUALIZER")
                Spacer(Modifier.height(8.dp))
                EQVisualizer(car.eqBands, SpotifyGreen, Modifier.fillMaxWidth().height(60.dp))
            }

            // Volume
            GlassCard(Modifier.weight(0.5f)) {
                LabelText("VOLUME")
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "${car.volume}",
                        style = TextStyle(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Thin,
                            color = C.TextPrimary
                        )
                    )
                    Text(
                        "%",
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = C.TextMuted
                        ),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                ProgressBar(car.volume.toFloat(), 100f, SpotifyGreen)
            }

            // Open Spotify
            GlassCard(
                modifier = Modifier.weight(0.5f)
                    .clickable { vm.openSpotify() }
            ) {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier.size(44.dp).clip(CircleShape)
                            .background(SpotifyGreenDim)
                            .border(1.dp, SpotifyGreen.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "\u266b",
                            style = TextStyle(fontSize = 22.sp, color = SpotifyGreen)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "SPOTIFY",
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SpotifyGreen,
                            letterSpacing = 1.5.sp
                        )
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Open App",
                        style = TextStyle(fontSize = 11.sp, color = C.TextSub)
                    )
                }
            }
        }
    }
}
