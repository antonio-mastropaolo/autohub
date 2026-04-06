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
import org.osmdroid.views.overlay.compass.CompassOverlay

@Composable
fun NavScreen(car: CarState) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Configure osmdroid once
    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = "com.autohub.app"
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

        // ── Live Map ──
        GlassCard(modifier = Modifier.fillMaxWidth().height(280.dp)) {
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp))) {
                // Embedded OSM map
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

                            // Dark overlay tint for the glass theme
                            overlayManager.tilesOverlay.setColorFilter(
                                android.graphics.ColorMatrixColorFilter(
                                    floatArrayOf(
                                        0.3f, 0f, 0f, 0f, 0f,
                                        0f, 0.3f, 0f, 0f, 0f,
                                        0f, 0f, 0.45f, 0f, 0f,
                                        0f, 0f, 0f, 1f, 0f
                                    )
                                )
                            )

                            // Location marker
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

                // Lifecycle handling for map
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
                    Modifier.align(Alignment.TopStart).padding(8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(C.Background.copy(alpha = 0.85f))
                        .border(1.dp, C.GlassBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        val over = car.speed > car.speedLimit
                        Text(
                            "${car.speed}",
                            style = TextStyle(
                                fontSize = 28.sp, fontWeight = FontWeight.Thin,
                                color = if (over) C.Red else C.TextPrimary
                            )
                        )
                        Text(
                            " MPH",
                            style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = C.TextMuted),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }

                // ── Heading overlay (top-right) ──
                Box(
                    Modifier.align(Alignment.TopEnd).padding(8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(C.Background.copy(alpha = 0.85f))
                        .border(1.dp, C.GlassBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
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
                        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Light, color = C.TextSecondary)
                    )
                }

                // ── GPS status (bottom-left) ──
                Box(
                    Modifier.align(Alignment.BottomStart).padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(C.Background.copy(alpha = 0.85f))
                        .border(1.dp, C.GlassBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        StatusDot(if (car.gpsActive) C.Green else C.Amber, 5.dp)
                        Text(
                            "${car.satellites} SAT",
                            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = C.TextSub)
                        )
                        Text(
                            "${car.altitude}ft",
                            style = TextStyle(fontSize = 11.sp, color = C.TextMuted)
                        )
                    }
                }

                // ── Altitude overlay (bottom-right) ──
                Box(
                    Modifier.align(Alignment.BottomEnd).padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(C.Background.copy(alpha = 0.85f))
                        .border(1.dp, C.GlassBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "%.4f, %.4f".format(car.latitude, car.longitude),
                        style = TextStyle(fontSize = 10.sp, color = C.TextMuted)
                    )
                }
            }
        }

        // ── Bottom: Quick actions + Trip stats ──
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Speed + Limit
            GlassCard(Modifier.weight(1f)) {
                LabelText("SPEED")
                Spacer(Modifier.height(4.dp))
                val over = car.speed > car.speedLimit
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "${car.speed}",
                        style = TextStyle(
                            fontSize = 26.sp, fontWeight = FontWeight.Thin,
                            color = if (over) C.Red else C.TextPrimary
                        )
                    )
                    Text(
                        " / ${car.speedLimit}",
                        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Thin, color = C.TextMuted),
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                if (over) {
                    Spacer(Modifier.height(2.dp))
                    Pill("+${car.speed - car.speedLimit} OVER", C.Red)
                }
            }

            // Trip stats
            GlassCard(Modifier.weight(1.2f)) {
                LabelText("TRIP")
                Spacer(Modifier.height(4.dp))
                TripRow("Distance", "%.1f mi".format(car.trip))
                TripRow("Duration", "${car.tripTime / 60}h ${car.tripTime % 60}m")
                TripRow("Avg Speed", "${car.avgSpeed} MPH")
            }

            // Launch Waze
            GlassCard(
                modifier = Modifier.weight(0.8f)
                    .clickable {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("waze://"))
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=com.waze")
                            )
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        }
                    }
            ) {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Navigation,
                        contentDescription = "Waze",
                        tint = C.Cyan,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "WAZE",
                        style = TextStyle(
                            fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            color = C.Cyan, letterSpacing = 1.sp
                        )
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Navigate",
                        style = TextStyle(fontSize = 11.sp, color = C.TextSub)
                    )
                }
            }

            // Launch Google Maps
            GlassCard(
                modifier = Modifier.weight(0.8f)
                    .clickable {
                        try {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("google.navigation:q=&mode=d")
                            )
                            intent.setPackage("com.google.android.apps.maps")
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=com.google.android.apps.maps")
                            )
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        }
                    }
            ) {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Map,
                        contentDescription = "Google Maps",
                        tint = C.Green,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "MAPS",
                        style = TextStyle(
                            fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            color = C.Green, letterSpacing = 1.sp
                        )
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Navigate",
                        style = TextStyle(fontSize = 11.sp, color = C.TextSub)
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
