package com.autohub.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

data class CarState(
    // ── Driving ──
    val speed: Int = 47, val rpm: Int = 2400, val gear: String = "D",
    val power: Int = 42, val torque: Int = 156, val throttle: Float = 22f,

    // ── Fuel & Efficiency ──
    val fuel: Float = 72f, val range: Int = 248,
    val mpg: Float = 28.4f, val avgMpg: Float = 26.8f,

    // ── Trip ──
    val odometer: Int = 12847, val trip: Float = 67.3f,
    val tripTime: Int = 84, val avgSpeed: Int = 48,

    // ── Temperatures ──
    val cabinTemp: Int = 68, val outsideTemp: Int = 72,
    val engineTemp: Int = 148, val oilTemp: Int = 198,
    val transTemp: Int = 165, val intakeTemp: Int = 88, val batteryTemp: Int = 82,

    // ── Tires (PSI + Temp °F) ──
    val tireFl: Int = 35, val tireFr: Int = 35,
    val tireRl: Int = 34, val tireRr: Int = 34,
    val tireTempFl: Int = 92, val tireTempFr: Int = 94,
    val tireTempRl: Int = 88, val tireTempRr: Int = 90,

    // ── Fluids (%) ──
    val oilLife: Int = 68, val oilPressure: Int = 42,
    val brakeFluid: Int = 94, val coolant: Int = 91,
    val transFluid: Int = 87, val washerFluid: Int = 55, val powerSteering: Int = 88,

    // ── Electrical ──
    val batteryVoltage: Float = 12.6f, val alternatorVoltage: Float = 14.2f,
    val batteryHealth: Int = 96,

    // ── Performance ──
    val gForceX: Float = 0.1f, val gForceY: Float = -0.02f,
    val peakSpeed: Int = 78, val peakRpm: Int = 5200, val drivingScore: Int = 87,

    // ── History (sparklines) ──
    val speedHistory: List<Float> = listOf(42f,45f,43f,47f,44f,48f,46f,47f,50f,48f,47f,45f,46f,48f,47f),
    val mpgHistory: List<Float> = listOf(26f,27f,28f,29f,28f,27f,28f,29f,28f,28f,27f,29f,28f,28f,29f),

    // ── Systems ──
    val serviceIn: Int = 3200,
    val headlightsOn: Boolean = true, val fogLightsOn: Boolean = false,
    val interiorLightsOn: Boolean = true, val drlOn: Boolean = true,
    val allDoorsLocked: Boolean = true, val trunkClosed: Boolean = true,
    val hoodClosed: Boolean = true, val tractionControl: Boolean = true,

    // ── Climate ──
    val acOn: Boolean = true, val acTarget: Int = 68, val acTargetPass: Int = 70,
    val fanSpeed: Int = 3, val driverSeatHeat: Int = 1, val passSeatHeat: Int = 0,
    val steeringHeat: Boolean = false,
    val frontDefrost: Boolean = false, val rearDefrost: Boolean = true,
    val recirculate: Boolean = false, val airQuality: Int = 92, val humidity: Int = 45,
    val airflowMode: String = "Face",

    // ── Media ──
    val mediaPlaying: Boolean = true,
    val mediaTitle: String = "Blinding Lights",
    val mediaArtist: String = "The Weeknd",
    val mediaProgress: Float = 0.67f,
    val mediaCurrent: String = "2:14", val mediaDuration: String = "3:20",
    val mediaSource: String = "Bluetooth", val volume: Int = 65,
    val eqBands: List<Float> = List(16) { 0.3f + Random.nextFloat() * 0.7f },

    // ── Navigation ──
    val heading: Int = 247, val altitude: Int = 342,
    val latitude: Float = 40.7128f, val longitude: Float = -74.006f,
    val satellites: Int = 12, val speedLimit: Int = 55,

    // ── Weather ──
    val weatherIcon: String = "\u2600", val weatherCondition: String = "Clear",
    val weatherHigh: Int = 78, val weatherLow: Int = 58,

    // ── Connectivity ──
    val signalStrength: Int = 4, val phoneBattery: Int = 72,
)

class CarViewModel : ViewModel() {
    var state by mutableStateOf(CarState())
        private set

    init {
        viewModelScope.launch {
            while (true) {
                delay(2000L)
                simulate()
            }
        }
    }

    private fun simulate() {
        val s = state
        val spd = max(0f, min(130f, s.speed + (Random.nextFloat() - 0.46f) * 5f)).roundToInt()
        val newRpm = (750 + spd * 40 + Random.nextInt(350)).coerceIn(750, 7500)
        val newProg = if (s.mediaProgress >= 1f) 0f else s.mediaProgress + 0.01f
        val progSec = (newProg * 200).toInt()

        state = s.copy(
            speed = spd,
            rpm = newRpm,
            power = (20 + spd * 0.6f + Random.nextFloat() * 15f).roundToInt(),
            torque = (100 + spd * 1.2f + Random.nextFloat() * 20f).roundToInt(),
            throttle = (spd * 0.6f + Random.nextFloat() * 10f).coerceIn(0f, 100f),
            fuel = max(0f, s.fuel - Random.nextFloat() * 0.005f),
            range = max(0, s.range - if (Random.nextFloat() < 0.3f) 1 else 0),
            mpg = (s.mpg + (Random.nextFloat() - 0.48f) * 0.5f).coerceIn(15f, 38f),
            trip = s.trip + spd * 0.00015f,
            engineTemp = 142 + Random.nextInt(20),
            oilTemp = 190 + Random.nextInt(15),
            transTemp = 158 + Random.nextInt(14),
            intakeTemp = 82 + Random.nextInt(12),
            batteryTemp = 78 + Random.nextInt(10),
            batteryVoltage = 12.4f + Random.nextFloat() * 0.4f,
            alternatorVoltage = 13.8f + Random.nextFloat() * 0.6f,
            oilPressure = 38 + Random.nextInt(10),
            gForceX = (Random.nextFloat() - 0.5f) * 0.6f,
            gForceY = (Random.nextFloat() - 0.5f) * 0.3f,
            peakSpeed = max(s.peakSpeed, spd),
            peakRpm = max(s.peakRpm, newRpm),
            drivingScore = (82 + Random.nextInt(10)).coerceIn(70, 99),
            speedHistory = (s.speedHistory + spd.toFloat()).takeLast(20),
            mpgHistory = (s.mpgHistory + s.mpg).takeLast(20),
            mediaProgress = newProg,
            mediaCurrent = "${progSec / 60}:${"%02d".format(progSec % 60)}",
            eqBands = if (s.mediaPlaying) List(16) { 0.15f + Random.nextFloat() * 0.85f } else List(16) { 0.1f },
            heading = (s.heading + Random.nextInt(3) - 1).mod(360),
            altitude = s.altitude + Random.nextInt(3) - 1,
        )
    }
}
