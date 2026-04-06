package com.autohub.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.autohub.app.data.CarState
import com.autohub.app.ui.components.*
import com.autohub.app.ui.theme.C
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun NavScreen(car: CarState) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = "com.autohub.app"
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

        // ═══════════════════════════════════════════════════════
        //  MAP — Full-width, taller, dark-styled
        // ═══════════════════════════════════════════════════════
        Box(
            Modifier.fillMaxWidth().height(320.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, C.GlassBorder, RoundedCornerShape(14.dp))
        ) {
            val mapViewRef = remember { mutableStateOf<MapView?>(null) }
            val markerRef = remember { mutableStateOf<Marker?>(null) }

            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(16.0)
                        val startPoint = GeoPoint(car.latitude.toDouble(), car.longitude.toDouble())
                        controller.setCenter(startPoint)

                        // Dark overlay — deeper blue tint for night-driving feel
                        overlayManager.tilesOverlay.setColorFilter(
                            android.graphics.ColorMatrixColorFilter(
                                floatArrayOf(
                                    0.22f, 0f, 0f, 0f, 0f,
                                    0f, 0.22f, 0f, 0f, 0f,
                                    0f, 0f, 0.38f, 0f, 10f,
                                    0f, 0f, 0f, 1f, 0f
                                )
                            )
                        )

                        val marker = Marker(this)
                        marker.position = startPoint
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        marker.title = "You"
                        marker.rotation = car.heading.toFloat()
                        overlays.add(marker)

                        mapViewRef.value = this
                        markerRef.value = marker
                    }
                },
                update = { mapView ->
                    val pos = GeoPoint(car.latitude.toDouble(), car.longitude.toDouble())
                    mapView.controller.animateTo(pos)
                    markerRef.value?.let { marker ->
                        marker.position = pos
                        marker.rotation = car.heading.toFloat()
                    }
                    mapView.invalidate()
                },
                modifier = Modifier.fillMaxSize()
            )

            // Lifecycle handling
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> mapViewRef.value?.onResume()
                        Lifecycle.Event.ON_PAUSE -> mapViewRef.value?.onPause()
                        else -> {}
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    mapViewRef.value?.onDetach()
                }
            }

            // ── Speed overlay (top-left) ──
            Box(
                Modifier.align(Alignment.TopStart).padding(10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(C.Background.copy(alpha = 0.90f))
                    .border(1.dp, C.GlassBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    val over = car.speed > car.speedLimit
                    Text(
                        "${car.speed}",
                        style = TextStyle(
                            fontSize = 32.sp, fontWeight = FontWeight.Thin,
                            color = if (over) C.Red else C.TextPrimary
                        )
                    )
                    Text(
                        " MPH",
                        style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = C.TextMuted),
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                    if (over) {
                        Spacer(Modifier.width(8.dp))
                        Pill("+${car.speed - car.speedLimit}", C.Red)
                    }
                }
            }

            // ── Heading overlay (top-right) ──
            Box(
                Modifier.align(Alignment.TopEnd).padding(10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(C.Background.copy(alpha = 0.90f))
                    .border(1.dp, C.GlassBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                val dir = when {
                    car.heading < 23 || car.heading >= 338 -> "N"
                    car.heading < 68 -> "NE"; car.heading < 113 -> "E"
                    car.heading < 158 -> "SE"; car.heading < 203 -> "S"
                    car.heading < 248 -> "SW"; car.heading < 293 -> "W"
                    else -> "NW"
                }
                Text(
                    "${car.heading}\u00b0 $dir",
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Light, color = C.TextSecondary)
                )
            }

            // ── GPS status (bottom-left) ──
            Box(
                Modifier.align(Alignment.BottomStart).padding(10.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(C.Background.copy(alpha = 0.90f))
                    .border(1.dp, C.GlassBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusDot(if (car.gpsActive) C.Green else C.Amber, 5.dp)
                    Text(
                        "${car.satellites} SAT  \u2022  ${car.altitude}ft",
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = C.TextSub)
                    )
                }
            }

            // ── Navigate buttons (bottom-right) ──
            Row(
                Modifier.align(Alignment.BottomEnd).padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Waze FAB
                Box(
                    Modifier.size(44.dp).clip(CircleShape)
                        .background(C.Cyan.copy(alpha = 0.15f))
                        .border(1.dp, C.Cyan.copy(alpha = 0.3f), CircleShape)
                        .clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(
                                    "waze://?ll=${car.latitude},${car.longitude}&navigate=yes"
                                ))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                val intent = Intent(Intent.ACTION_VIEW,
                                    Uri.parse("market://details?id=com.waze"))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Navigation,
                        contentDescription = "Waze",
                        tint = C.Cyan,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Google Maps FAB
                Box(
                    Modifier.size(44.dp).clip(CircleShape)
                        .background(C.Green.copy(alpha = 0.15f))
                        .border(1.dp, C.Green.copy(alpha = 0.3f), CircleShape)
                        .clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW,
                                    Uri.parse("google.navigation:q=&mode=d"))
                                intent.setPackage("com.google.android.apps.maps")
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                val intent = Intent(Intent.ACTION_VIEW,
                                    Uri.parse("market://details?id=com.google.android.apps.maps"))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Map,
                        contentDescription = "Google Maps",
                        tint = C.Green,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // ═══════════════════════════════════════════════════════
        //  BOTTOM: Trip stats — clean, single row
        // ═══════════════════════════════════════════════════════
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassCard(Modifier.weight(1f)) {
                LabelText("TRIP")
                Spacer(Modifier.height(4.dp))
                TripRow("Distance", "%.1f mi".format(car.trip))
                TripRow("Duration", "${car.tripTime / 60}h ${car.tripTime % 60}m")
                TripRow("Avg Speed", "${car.avgSpeed} MPH")
            }
            GlassCard(Modifier.weight(1f)) {
                LabelText("POSITION")
                Spacer(Modifier.height(4.dp))
                TripRow("Latitude", "%.4f".format(car.latitude))
                TripRow("Longitude", "%.4f".format(car.longitude))
                TripRow("Altitude", "${car.altitude} ft")
            }
            GlassCard(Modifier.weight(1f)) {
                LabelText("SPEED")
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "${car.speed}",
                        style = TextStyle(
                            fontSize = 32.sp, fontWeight = FontWeight.Thin,
                            color = if (car.speed > car.speedLimit) C.Red else C.TextPrimary
                        )
                    )
                    Text(
                        " / ${car.speedLimit} MPH",
                        style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Thin, color = C.TextMuted),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TripRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        Arrangement.SpaceBetween
    ) {
        Text(label, style = TextStyle(fontSize = 13.sp, color = C.TextSub))
        Text(value, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Light, color = C.TextPrimary))
    }
}
