package com.autohub.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.autohub.app.data.CarState
import com.autohub.app.ui.components.*
import com.autohub.app.ui.theme.C
import io.github.sceneview.SceneView
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import io.github.sceneview.loaders.ModelLoader

/**
 * Tesla-style vehicle screen with real 3D model you can spin with your finger.
 * Uses SceneView (Google Filament) to render a .glb model.
 */
@Composable
fun VehicleScreen(car: CarState) {
    var showTireDetail by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {

            // ═══════════════════════════════════════════════════════
            //  3D CAR MODEL — drag to spin, pinch to zoom
            // ═══════════════════════════════════════════════════════
            Box(
                Modifier.fillMaxWidth().height(320.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(C.Background)
            ) {
                // SceneView 3D renderer
                AndroidView(
                    factory = { ctx ->
                        SceneView(ctx).apply {
                            setBackgroundColor(android.graphics.Color.argb(255, 5, 5, 8))

                            // Load the car model
                            val modelLoader = ModelLoader(engine, ctx)
                            val modelInstance = modelLoader.createModelInstance(
                                assetFileLocation = "models/car.glb"
                            )
                            val modelNode = ModelNode(
                                modelInstance = modelInstance,
                                scaleToUnits = 2.0f,
                            ).apply {
                                position = Position(0f, -0.5f, 0f)
                            }
                            addChildNode(modelNode)

                            // Camera position for a nice 3/4 view
                            cameraNode.position = Position(0f, 1.5f, 4.0f)
                            cameraNode.lookAt(Position(0f, 0.3f, 0f))
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Title overlay
                Column(Modifier.align(Alignment.TopStart).padding(14.dp)) {
                    Text(
                        "ATLAS CROSS SPORT",
                        style = TextStyle(
                            fontSize = 16.sp, fontWeight = FontWeight.Bold,
                            color = C.Blue, letterSpacing = 3.sp
                        )
                    )
                    Text(
                        "2024 SEL R-LINE  •  2.0T TSI  •  4MOTION",
                        style = TextStyle(
                            fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            color = C.TextMuted, letterSpacing = 1.5.sp
                        )
                    )
                }

                // Drag hint
                Text(
                    "DRAG TO ROTATE  •  PINCH TO ZOOM",
                    style = TextStyle(
                        fontSize = 8.sp, fontWeight = FontWeight.Bold,
                        color = C.TextFaint, letterSpacing = 1.5.sp
                    ),
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 6.dp)
                )

                // Left: Status pills
                Column(
                    Modifier.align(Alignment.CenterStart).padding(start = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FloatingPill("DOORS", if (car.allDoorsLocked) "LOCKED" else "OPEN",
                        if (car.allDoorsLocked) C.Green else C.Amber)
                    FloatingPill("ENGINE", "${car.engineTemp}\u00b0F",
                        if (car.engineTemp > 210) C.Red else C.Green)
                    FloatingPill("OIL", "${car.oilLife}%",
                        if (car.oilLife > 30) C.Green else C.Amber)
                    FloatingPill("BATTERY", "%.1f".format(car.batteryVoltage) + "V",
                        if (car.batteryVoltage > 12f) C.Blue else C.Red)
                }

                // Right: Tire PSI
                Column(
                    Modifier.align(Alignment.CenterEnd).padding(end = 10.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text("TIRES", style = TextStyle(fontSize = 8.sp, fontWeight = FontWeight.Bold, color = C.TextMuted, letterSpacing = 1.5.sp))
                    TirePill("FL", car.tireFl)
                    TirePill("FR", car.tireFr)
                    TirePill("RL", car.tireRl)
                    TirePill("RR", car.tireRr)
                }

                // Bottom stats
                Row(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BottomStat("FUEL", "${car.fuel.toInt()}%", if (car.fuel > 25f) C.Green else C.Red)
                    BottomStat("RANGE", "${car.range} mi", if (car.range > 80) C.Green else C.Amber)
                    BottomStat("SERVICE", "%,d mi".format(car.serviceIn),
                        if (car.serviceIn > 1500) C.Green else C.Amber)
                    BottomStat("ODO", "%,d".format(car.odometer), C.TextSecondary)
                    BottomStat("TRANS", "${car.transTemp}\u00b0F", C.Purple)
                }
            }

            // ═══════════════════════════════════════════════════════
            //  SYSTEMS strip
            // ═══════════════════════════════════════════════════════
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                val systems = listOf(
                    "Headlights" to car.headlightsOn,
                    "DRL" to car.drlOn,
                    "Fog" to car.fogLightsOn,
                    "Interior" to car.interiorLightsOn,
                    "Traction" to car.tractionControl,
                    "Hood" to car.hoodClosed,
                    "Trunk" to car.trunkClosed,
                )
                for ((name, on) in systems) {
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(6.dp))
                            .background(if (on) C.Glass else Color.Transparent)
                            .padding(vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            StatusDot(if (on) C.Green else C.TextMuted, 3.dp)
                            Text(name, style = TextStyle(fontSize = 8.sp, color = if (on) C.TextSub else C.TextMuted, fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }

        if (showTireDetail) TireDetailSheet(car) { showTireDetail = false }
    }
}

@Composable
private fun FloatingPill(label: String, value: String, color: Color) {
    Row(
        Modifier.clip(RoundedCornerShape(10.dp))
            .background(C.Background.copy(alpha = 0.75f))
            .border(0.5.dp, color.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        StatusDot(color, 5.dp)
        Text(label, style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = C.TextMuted, letterSpacing = 0.8.sp))
        Text(value, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Light, color = C.TextPrimary))
    }
}

@Composable
private fun TirePill(pos: String, psi: Int) {
    val ok = psi in 32..38
    val color = if (ok) C.Green else C.Amber
    Row(
        Modifier.clip(RoundedCornerShape(10.dp))
            .background(C.Background.copy(alpha = 0.75f))
            .border(0.5.dp, color.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(pos, style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = C.TextMuted))
        Text("$psi", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Light, color = if (ok) C.TextPrimary else C.Amber))
        Text("PSI", style = TextStyle(fontSize = 8.sp, fontWeight = FontWeight.Bold, color = C.TextMuted))
    }
}

@Composable
private fun BottomStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Light, color = C.TextPrimary))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            StatusDot(color, 3.dp)
            Text(label, style = TextStyle(fontSize = 8.sp, fontWeight = FontWeight.Bold, color = C.TextMuted, letterSpacing = 0.8.sp))
        }
    }
}
