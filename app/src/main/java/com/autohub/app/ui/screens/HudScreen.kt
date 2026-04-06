package com.autohub.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autohub.app.data.CarState
import com.autohub.app.ui.components.ProgressBar
import com.autohub.app.ui.components.StatusDot
import com.autohub.app.ui.theme.C
import java.util.Calendar

// ═══════════════════════════════════════════════════════════════════
//  HUD Screen — Adaptive Drive Mode Heads-Up Display
//  Minimalist, maximum-glanceability view optimized for highway speed.
//  Shows only critical driving info in the largest possible text.
// ═══════════════════════════════════════════════════════════════════

@Composable
fun HudScreen(car: CarState) {
    val speeding = car.speed > car.speedLimit
    val throttleAnimated by animateFloatAsState(
        targetValue = car.throttle / 100f,
        animationSpec = tween(durationMillis = 600),
        label = "throttle"
    )

    // Subtle pulse when speeding
    val infiniteTransition = rememberInfiniteTransition(label = "hud_pulse")
    val warningAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "warning_alpha"
    )

    // Clock
    val now = Calendar.getInstance()
    val hour = now.get(Calendar.HOUR).let { if (it == 0) 12 else it }
    val minute = "%02d".format(now.get(Calendar.MINUTE))
    val amPm = if (now.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
    val clockText = "$hour:$minute $amPm"

    // Speed color
    val speedColor = if (speeding) {
        C.Red.copy(alpha = warningAlpha)
    } else {
        C.TextPrimary
    }

    // Format RPM as "2.4K" style
    val rpmDisplay = if (car.rpm >= 1000) {
        "%.1fK".format(car.rpm / 1000f)
    } else {
        "${car.rpm}"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(C.Background)
    ) {
        // ── Ambient glow circles (subtle) ──
        Box(
            modifier = Modifier
                .offset(x = (-80).dp, y = (-80).dp)
                .size(300.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(C.Blue.copy(alpha = 0.04f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 60.dp)
                .size(250.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(C.Purple.copy(alpha = 0.03f), Color.Transparent)
                    )
                )
        )

        // Speeding warning: subtle red ambient glow
        if (speeding) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-40).dp)
                    .size(500.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                C.Red.copy(alpha = 0.06f * warningAlpha),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // ── Main content ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // ═══════════════════════════════════════════════
            //  TOP REGION — Speed + RPM (the hero numbers)
            // ═══════════════════════════════════════════════
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ── Speed (left) ──
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "${car.speed}",
                        style = TextStyle(
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Thin,
                            color = speedColor,
                            letterSpacing = (-2).sp,
                            textAlign = TextAlign.Center
                        )
                    )
                    Text(
                        text = "MPH",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = C.TextMuted,
                            letterSpacing = 3.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                }

                // ── Gear (center) ──
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(0.6f)
                ) {
                    Text(
                        text = car.gear,
                        style = TextStyle(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraLight,
                            color = C.Blue,
                            textAlign = TextAlign.Center
                        )
                    )
                }

                // ── RPM (right) ──
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = rpmDisplay,
                        style = TextStyle(
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Thin,
                            color = if (car.rpm > 5000) C.Red else C.TextPrimary,
                            letterSpacing = (-1).sp,
                            textAlign = TextAlign.Center
                        )
                    )
                    Text(
                        text = "RPM",
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = C.TextMuted,
                            letterSpacing = 2.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }

            // ═══════════════════════════════════════════════
            //  MIDDLE — Temp, Throttle bar, Range
            // ═══════════════════════════════════════════════
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ── Outside temp + weather icon (left) ──
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(80.dp)
                ) {
                    Text(
                        text = "${car.outsideTemp}\u00b0F",
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraLight,
                            color = C.TextPrimary,
                            textAlign = TextAlign.Center
                        )
                    )
                    Text(
                        text = car.weatherIcon,
                        style = TextStyle(
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                }

                // ── Throttle bar (center) ──
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 24.dp)
                ) {
                    ProgressBar(
                        value = throttleAnimated,
                        maxValue = 1f,
                        color = when {
                            car.throttle > 80f -> C.Red
                            car.throttle > 50f -> C.Amber
                            else -> C.Blue
                        },
                        height = 4.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "THROTTLE",
                        style = TextStyle(
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = C.TextFaint,
                            letterSpacing = 2.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                }

                // ── Range (right) ──
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(90.dp)
                ) {
                    Text(
                        text = "${car.range} mi",
                        style = TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraLight,
                            color = if (car.range < 50) C.Amber else C.TextPrimary,
                            textAlign = TextAlign.Center
                        )
                    )
                    Text(
                        text = "RANGE",
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = C.TextMuted,
                            letterSpacing = 2.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }

            // ═══════════════════════════════════════════════
            //  BOTTOM — OBD status, Speed limit, Clock
            // ═══════════════════════════════════════════════
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ── OBD status (left) ──
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusDot(
                        color = if (car.obdConnected) C.Green else C.Amber,
                        size = 6.dp
                    )
                    Text(
                        text = if (car.obdConnected) "OBD LIVE" else "DEMO",
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (car.obdConnected) C.Green else C.Amber,
                            letterSpacing = 1.sp
                        )
                    )
                }

                // ── Speed limit (center) ──
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Speed,
                        contentDescription = "Speed limit",
                        tint = if (speeding) C.Red.copy(alpha = warningAlpha) else C.TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "LIMIT ${car.speedLimit}",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (speeding) C.Red.copy(alpha = warningAlpha) else C.TextSub,
                            letterSpacing = 1.sp
                        )
                    )
                }

                // ── Clock (right) ──
                Text(
                    text = clockText,
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Thin,
                        color = C.TextSecondary,
                        letterSpacing = (-0.5).sp
                    )
                )
            }
        }
    }
}
