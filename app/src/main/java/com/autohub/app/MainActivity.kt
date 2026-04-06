package com.autohub.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
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
import androidx.compose.ui.draw.blur
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
import com.autohub.app.ui.screens.ClimateScreen
import com.autohub.app.ui.screens.DriveScreen
import com.autohub.app.ui.screens.VehicleScreen
import com.autohub.app.ui.theme.AutoHubColors as C
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Immersive fullscreen — optimized for P3 Pro car screen
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Keep screen on — critical for car dashboard
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            AutoHubApp()
        }
    }
}

@Composable
fun AutoHubApp(vm: CarViewModel = viewModel()) {
    val car = vm.state
    var currentTab by remember { mutableStateOf("drive") }
    var time by remember { mutableStateOf("") }
    var ampm by remember { mutableStateOf("") }

    // Clock updater
    LaunchedEffect(Unit) {
        while (true) {
            val now = Calendar.getInstance()
            val h = now.get(Calendar.HOUR)
            if (h == 0) time = "12:%02d".format(now.get(Calendar.MINUTE))
            else time = "$h:%02d".format(now.get(Calendar.MINUTE))
            ampm = if (now.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
            kotlinx.coroutines.delay(5000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(C.Background)
    ) {
        // Ambient glow effects
        Box(
            modifier = Modifier
                .offset(x = (-50).dp, y = (-100).dp)
                .size(350.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(C.Blue.copy(alpha = 0.1f), Color.Transparent)
                    )
                )
                .blur(60.dp)
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-50).dp)
                .size(200.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(C.Cyan.copy(alpha = 0.06f), Color.Transparent)
                    )
                )
                .blur(40.dp)
        )

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // ─── Header ───
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(C.BlueDim)
                            .border(1.dp, C.Blue.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("◈", style = TextStyle(fontSize = 12.sp, color = C.Blue))
                    }
                    Text("AutoHub", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = C.TextPrimary))
                    Text("P3 PRO", style = TextStyle(fontSize = 7.sp, fontWeight = FontWeight.Bold, color = C.TextMuted, letterSpacing = 1.sp))
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Live indicator
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(C.Green)
                    )
                    Text("LIVE", style = TextStyle(fontSize = 7.sp, fontWeight = FontWeight.Bold, color = C.TextSub, letterSpacing = 0.6.sp))

                    Box(Modifier.width(1.dp).height(14.dp).background(C.TextFaint))

                    Text(time, style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Thin, color = C.TextPrimary, letterSpacing = (-0.5).sp))
                    Text(ampm, style = TextStyle(fontSize = 7.sp, fontWeight = FontWeight.Bold, color = C.TextMuted))
                }
            }

            Spacer(Modifier.height(8.dp))

            // ─── Tab Bar ───
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.025f))
                    .border(1.dp, C.GlassBorder, RoundedCornerShape(10.dp))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                data class TabInfo(val id: String, val icon: String, val label: String)
                val tabs = listOf(
                    TabInfo("drive", "◉", "Drive"),
                    TabInfo("vehicle", "◈", "Vehicle"),
                    TabInfo("climate", "❋", "Climate"),
                )

                for (tab in tabs) {
                    val active = currentTab == tab.id
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (active) C.Surface else Color.Transparent
                            )
                            .clickable { currentTab = tab.id }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${tab.icon} ${tab.label.uppercase()}",
                            style = TextStyle(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) C.Blue else C.TextMuted,
                                letterSpacing = 0.8.sp
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // ─── Content ───
            Box(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        fadeIn(tween(200)) + slideInVertically(tween(250)) { 20 } togetherWith
                            fadeOut(tween(150))
                    },
                    label = "tabSwitch"
                ) { tab ->
                    when (tab) {
                        "drive" -> DriveScreen(car)
                        "vehicle" -> VehicleScreen(car)
                        "climate" -> ClimateScreen(car)
                    }
                }
            }

            // ─── Footer ───
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .border(width = 0.5.dp, color = C.TextFaint, shape = RoundedCornerShape(0.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "AUTOHUB V1.0  •  OTTOCAST P3 PRO  •  ANDROID 13  •  SNAPDRAGON 6225",
                    style = TextStyle(
                        fontSize = 6.sp, fontWeight = FontWeight.Bold,
                        color = C.TextMuted, letterSpacing = 1.4.sp
                    ),
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
        }
    }
}
