package com.autohub.app.data

import android.annotation.SuppressLint
import android.app.Application
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

// ═══════════════════════════════════════════════════════════════════
//  CarState — full vehicle telemetry snapshot
//  Defaults tuned for 2024 VW Atlas Cross Sport 2.0T TSI
// ═══════════════════════════════════════════════════════════════════

data class CarState(
    // ── Driving ──
    val speed: Int = 0, val rpm: Int = 800, val gear: String = "P",
    val power: Int = 0, val torque: Int = 0, val throttle: Float = 0f,

    // ── Fuel & Efficiency ──
    // 18.6 gallon tank, EPA combined ~23 MPG → ~350 mi full range
    val fuel: Float = 100f, val range: Int = 350,
    val mpg: Float = 23.0f, val avgMpg: Float = 23.0f,

    // ── Trip ──
    val odometer: Int = 12847, val trip: Float = 0f,
    val tripTime: Int = 0, val avgSpeed: Int = 0,

    // ── Temperatures ──
    val cabinTemp: Int = 68, val outsideTemp: Int = 72,
    val engineTemp: Int = 195, val oilTemp: Int = 180,
    val transTemp: Int = 165, val intakeTemp: Int = 88, val batteryTemp: Int = 82,

    // ── Tires (PSI + Temp °F) — VW recommended 36 PSI all four ──
    val tireFl: Int = 36, val tireFr: Int = 36,
    val tireRl: Int = 36, val tireRr: Int = 36,
    val tireTempFl: Int = 92, val tireTempFr: Int = 94,
    val tireTempRl: Int = 88, val tireTempRr: Int = 90,

    // ── Fluids (%) ──
    val oilLife: Int = 68, val oilPressure: Int = 42,
    val brakeFluid: Int = 94, val coolant: Int = 195,
    val transFluid: Int = 87, val washerFluid: Int = 55, val powerSteering: Int = 88,

    // ── Electrical ──
    val batteryVoltage: Float = 12.6f, val alternatorVoltage: Float = 14.2f,
    val batteryHealth: Int = 96,

    // ── Performance ──
    // 2.0T TSI: 235 HP / 258 lb-ft
    val gForceX: Float = 0f, val gForceY: Float = 0f,
    val peakSpeed: Int = 0, val peakRpm: Int = 800, val drivingScore: Int = 87,

    // ── History (sparklines) ──
    val speedHistory: List<Float> = listOf(0f),
    val mpgHistory: List<Float> = listOf(23f),

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
    val eqBands: List<Float> = List(size = 16) { 0.3f + Random.nextFloat() * 0.7f },

    // ── Navigation / GPS ──
    val heading: Int = 247, val altitude: Int = 342,
    val latitude: Float = 40.7128f, val longitude: Float = -74.006f,
    val satellites: Int = 0, val speedLimit: Int = 65,

    // ── Weather ──
    val weatherIcon: String = "\u2600", val weatherCondition: String = "Clear",
    val weatherHigh: Int = 78, val weatherLow: Int = 58,

    // ── Connectivity ──
    val signalStrength: Int = 4, val phoneBattery: Int = 72,

    // ── OBD / GPS status ──
    val obdConnected: Boolean = false,
    val gpsActive: Boolean = false,
)

// ═══════════════════════════════════════════════════════════════════
//  CarViewModel — OBD + GPS integration with simulation fallback
//  Designed for Ottocast P3 Pro (Android 13) in a 2024 VW Atlas CS
// ═══════════════════════════════════════════════════════════════════

class CarViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "CarViewModel"

        // 2024 VW Atlas Cross Sport 2.0T TSI
        private const val MAX_HP = 235f
        private const val MAX_TORQUE = 258f
        private const val TANK_GALLONS = 18.6f

        // Approximate speed/rpm ratio thresholds for VW 8-speed Aisin
        private val GEAR_RATIOS = listOf(
            0.000 to "P",
            0.005 to "1",
            0.010 to "2",
            0.015 to "3",
            0.020 to "4",
            0.025 to "5",
            0.030 to "6",
            0.035 to "7",
            0.040 to "8",
        )
    }

    private val obdManager = ObdManager(context = application.applicationContext)
    private val locationManager: LocationManager? =
        application.getSystemService(android.content.Context.LOCATION_SERVICE) as? LocationManager

    var state by mutableStateOf(CarState())
        private set

    /** True when OBD is connected and delivering live data. */
    private var obdLive = false

    // ── Location listener ────────────────────────────────────────

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            try {
                state = state.copy(
                    latitude = location.latitude.toFloat(),
                    longitude = location.longitude.toFloat(),
                    altitude = location.altitude.toInt(),
                    heading = location.bearing.toInt(),
                    gpsActive = true,
                )
                // Use GPS speed when OBD is not providing it
                if (!obdLive && location.hasSpeed()) {
                    val gpsSpeedMph = (location.speed * 2.23694f).roundToInt()
                    state = state.copy(speed = gpsSpeedMph)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Location update error", e)
            }
        }

        override fun onProviderEnabled(provider: String) {}

        override fun onProviderDisabled(provider: String) {
            state = state.copy(gpsActive = false)
        }

        @Deprecated("Deprecated in API 29+", ReplaceWith(""))
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    }

    private val gnssStatusCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            var usedCount = 0
            for (i in 0 until status.satelliteCount) {
                if (status.usedInFix(i)) usedCount++
            }
            state = state.copy(satellites = usedCount)
        }
    }

    // ── Initialisation ───────────────────────────────────────────

    init {
        // Simulation loop — always runs; produces DEMO data when OBD is offline,
        // media-only updates when OBD is live
        viewModelScope.launch {
            while (true) {
                delay(timeMillis = 2000L)
                try {
                    if (!obdLive) {
                        simulate()
                    } else {
                        simulateMedia()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Simulation tick error", e)
                }
            }
        }

        // Collect OBD connection state
        viewModelScope.launch {
            obdManager.connectionState.collect { connState ->
                val connected = connState == ObdManager.ConnectionState.CONNECTED
                obdLive = connected
                state = state.copy(obdConnected = connected)
                if (!connected) {
                    Log.i(TAG, "OBD disconnected — falling back to DEMO mode")
                }
            }
        }

        // Collect OBD readings and map to CarState
        viewModelScope.launch {
            obdManager.reading.collect { reading ->
                try {
                    if (obdLive) {
                        applyObdReading(reading = reading)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "OBD reading apply error", e)
                }
            }
        }

        // Auto-connect to OBD adapter after a short delay
        // Uses connectAny() which checks paired devices first, then scans BLE
        // for unpaired adapters like the Hyper Tough HT500
        viewModelScope.launch {
            delay(timeMillis = 3000L)
            try {
                Log.i(TAG, "Starting OBD auto-connect (paired + BLE scan)...")
                obdManager.connectAny()
            } catch (e: Exception) {
                Log.e(TAG, "OBD auto-connect failed", e)
            }
            // Retry once after 10s if first attempt failed
            if (!obdLive) {
                delay(timeMillis = 10000L)
                try {
                    Log.i(TAG, "OBD retry auto-connect...")
                    obdManager.connectAny()
                } catch (e: Exception) {
                    Log.e(TAG, "OBD retry auto-connect failed", e)
                }
            }
        }

        // Start GPS updates
        startGps()
    }

    // ── OBD reading → CarState mapping ───────────────────────────

    private fun applyObdReading(reading: ObdManager.ObdReading) {
        val s = state

        // Speed (ObdManager already converts to mph)
        val speedMph = reading.speedMph?.roundToInt() ?: s.speed

        // RPM
        val rpm = reading.rpm ?: s.rpm

        // Gear from speed/RPM ratio
        val gear = estimateGear(speedMph = speedMph, rpm = rpm)

        // Power: engine load% * 235 HP
        val load = reading.engineLoadPercent ?: 0f
        val power = (load / 100f * MAX_HP).roundToInt()
            .coerceIn(minimumValue = 0, maximumValue = MAX_HP.toInt())

        // Torque: engine load% * 258 lb-ft
        val torque = (load / 100f * MAX_TORQUE).roundToInt()
            .coerceIn(minimumValue = 0, maximumValue = MAX_TORQUE.toInt())

        // Throttle
        val throttle = reading.throttlePercent ?: s.throttle

        // Fuel level
        val fuelPercent = reading.fuelLevelPercent ?: s.fuel

        // Range estimate: fuel% → gallons → miles
        val rangeEstimate = (fuelPercent / 100f * TANK_GALLONS * s.avgMpg).roundToInt()

        // MPG from MAF: mpg = (speed_mph * 7.718) / maf_grams_per_sec
        val maf = reading.mafRateGps ?: 0f
        val instantMpg = if (maf > 0f && speedMph > 0) {
            (speedMph * 7.718f / maf).coerceIn(minimumValue = 0f, maximumValue = 99f)
        } else {
            s.mpg
        }

        // Rolling average MPG (exponential moving average)
        val avgMpg = s.avgMpg * 0.95f + instantMpg * 0.05f

        // Temperatures (ObdManager returns Fahrenheit)
        val coolantF = reading.coolantTempF?.roundToInt() ?: s.engineTemp
        val oilF = reading.oilTempF?.roundToInt() ?: s.oilTemp
        val intakeF = reading.intakeTempF?.roundToInt() ?: s.intakeTemp
        val outsideF = reading.ambientTempF?.roundToInt() ?: s.outsideTemp

        // Battery voltage
        val voltage = reading.controlModuleVoltage ?: s.batteryVoltage

        // History arrays
        val speedHist = (s.speedHistory + speedMph.toFloat()).takeLast(n = 20)
        val mpgHist = (s.mpgHistory + instantMpg).takeLast(n = 20)

        state = s.copy(
            speed = speedMph,
            rpm = rpm,
            gear = gear,
            power = power,
            torque = torque,
            throttle = throttle,
            fuel = fuelPercent,
            range = rangeEstimate,
            mpg = instantMpg,
            avgMpg = avgMpg,
            trip = s.trip + speedMph * 0.00028f,
            engineTemp = coolantF,
            oilTemp = oilF,
            intakeTemp = intakeF,
            outsideTemp = outsideF,
            batteryVoltage = voltage,
            oilPressure = (38 + (rpm / 200f)).roundToInt()
                .coerceIn(minimumValue = 20, maximumValue = 80),
            peakSpeed = max(a = s.peakSpeed, b = speedMph),
            peakRpm = max(a = s.peakRpm, b = rpm),
            speedHistory = speedHist,
            mpgHistory = mpgHist,
        )
    }

    /**
     * Estimates the current gear from the speed/RPM ratio.
     * Uses approximate thresholds for the VW 8-speed Aisin automatic.
     */
    private fun estimateGear(speedMph: Int, rpm: Int): String {
        if (speedMph < 2) return if (rpm < 900) "P" else "N"
        if (rpm <= 0) return "D"

        val ratio = speedMph.toFloat() / rpm.toFloat()
        var bestGear = "1"
        for ((threshold, gearLabel) in GEAR_RATIOS) {
            if (ratio >= threshold) {
                bestGear = gearLabel
            }
        }
        // Never show "P" while moving
        if (bestGear == "P") bestGear = "1"
        return bestGear
    }

    // ── GPS integration ──────────────────────────────────────────

    @SuppressLint("MissingPermission")
    private fun startGps() {
        try {
            val lm = locationManager ?: return

            if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Log.w(TAG, "GPS provider not enabled")
                return
            }

            lm.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L,
                1f,
                locationListener
            )

            lm.registerGnssStatusCallback(
                gnssStatusCallback,
                null
            )

            state = state.copy(gpsActive = true)
            Log.i(TAG, "GPS updates started")
        } catch (e: SecurityException) {
            Log.w(TAG, "GPS permission not granted — skipping GPS", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start GPS", e)
        }
    }

    // ── Simulation (DEMO mode) ───────────────────────────────────

    /**
     * Full simulation tick — used when OBD is not connected.
     * Speed oscillates around 45 MPH with realistic variation.
     * RPM correlates to speed for the VW 8-speed:
     *   idle ~800, 45 mph ~2000, 70 mph ~3500
     */
    private fun simulate() {
        val s = state

        // Speed drifts toward 45 MPH
        val targetSpeed = 45f
        val currentSpeed = s.speed.toFloat()
        val drift = (targetSpeed - currentSpeed) * 0.1f + (Random.nextFloat() - 0.48f) * 4f
        val spd = max(a = 0f, b = min(a = 130f, b = currentSpeed + drift)).roundToInt()

        // RPM correlates to speed for 8-speed automatic
        val newRpm = when {
            spd < 2 -> 800 + Random.nextInt(50)
            spd < 25 -> (800 + spd * 60 + Random.nextInt(200))
                .coerceIn(minimumValue = 800, maximumValue = 3500)
            spd < 50 -> (1200 + spd * 20 + Random.nextInt(200))
                .coerceIn(minimumValue = 1400, maximumValue = 3000)
            else -> (1000 + spd * 35 + Random.nextInt(250))
                .coerceIn(minimumValue = 2000, maximumValue = 6500)
        }

        val gear = estimateGear(speedMph = spd, rpm = newRpm)

        // Engine load simulation
        val simLoad = (spd * 0.8f + Random.nextFloat() * 15f)
            .coerceIn(minimumValue = 5f, maximumValue = 95f)
        val simPower = (simLoad / 100f * MAX_HP).roundToInt()
        val simTorque = (simLoad / 100f * MAX_TORQUE).roundToInt()

        // MPG — realistic for 2.0T at ~45 mph
        val simMpg = (s.mpg + (Random.nextFloat() - 0.48f) * 0.5f)
            .coerceIn(minimumValue = 15f, maximumValue = 38f)

        // Media progress
        val newProg = if (s.mediaProgress >= 1f) 0f else s.mediaProgress + 0.01f
        val progSec = (newProg * 200).toInt()

        state = s.copy(
            speed = spd,
            rpm = newRpm,
            gear = gear,
            power = simPower,
            torque = simTorque,
            throttle = (simLoad + Random.nextFloat() * 5f)
                .coerceIn(minimumValue = 0f, maximumValue = 100f),
            fuel = max(a = 0f, b = s.fuel - Random.nextFloat() * 0.005f),
            range = max(a = 0, b = s.range - if (Random.nextFloat() < 0.3f) 1 else 0),
            mpg = simMpg,
            avgMpg = s.avgMpg * 0.99f + simMpg * 0.01f,
            trip = s.trip + spd * 0.00015f,
            engineTemp = 190 + Random.nextInt(10),
            oilTemp = 175 + Random.nextInt(10),
            transTemp = 158 + Random.nextInt(14),
            intakeTemp = 82 + Random.nextInt(12),
            batteryTemp = 78 + Random.nextInt(10),
            batteryVoltage = 12.4f + Random.nextFloat() * 0.4f,
            alternatorVoltage = 13.8f + Random.nextFloat() * 0.6f,
            oilPressure = 38 + Random.nextInt(10),
            gForceX = (Random.nextFloat() - 0.5f) * 0.6f,
            gForceY = (Random.nextFloat() - 0.5f) * 0.3f,
            peakSpeed = max(a = s.peakSpeed, b = spd),
            peakRpm = max(a = s.peakRpm, b = newRpm),
            drivingScore = (82 + Random.nextInt(10))
                .coerceIn(minimumValue = 70, maximumValue = 99),
            speedHistory = (s.speedHistory + spd.toFloat()).takeLast(n = 20),
            mpgHistory = (s.mpgHistory + simMpg).takeLast(n = 20),
            mediaProgress = newProg,
            mediaCurrent = "${progSec / 60}:${"%02d".format(progSec % 60)}",
            eqBands = if (s.mediaPlaying) {
                List(size = 16) { 0.15f + Random.nextFloat() * 0.85f }
            } else {
                List(size = 16) { 0.1f }
            },
            heading = (s.heading + Random.nextInt(3) - 1).mod(other = 360),
            altitude = s.altitude + Random.nextInt(3) - 1,
        )
    }

    /**
     * Media-only simulation — runs when OBD is connected.
     * We cannot read actual media state without NotificationListenerService.
     */
    private fun simulateMedia() {
        val s = state
        val newProg = if (s.mediaProgress >= 1f) 0f else s.mediaProgress + 0.01f
        val progSec = (newProg * 200).toInt()

        state = s.copy(
            mediaProgress = newProg,
            mediaCurrent = "${progSec / 60}:${"%02d".format(progSec % 60)}",
            eqBands = if (s.mediaPlaying) {
                List(size = 16) { 0.15f + Random.nextFloat() * 0.85f }
            } else {
                List(size = 16) { 0.1f }
            },
        )
    }

    // ── Cleanup ──────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        obdManager.disconnect()
        try {
            locationManager?.removeUpdates(locationListener)
            locationManager?.unregisterGnssStatusCallback(gnssStatusCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Error removing GPS listeners", e)
        }
    }
}
