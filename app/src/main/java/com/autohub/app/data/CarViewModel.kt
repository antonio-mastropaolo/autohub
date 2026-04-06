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
    val speed: Int = 47,
    val rpm: Int = 2400,
    val fuel: Float = 72f,
    val range: Int = 248,
    val mpg: Float = 28.4f,
    val odometer: Int = 12847,
    val trip: Float = 67.3f,
    val gear: String = "D",
    val power: Int = 42,

    // Temperatures
    val cabinTemp: Int = 68,
    val outsideTemp: Int = 72,
    val engineTemp: Int = 148,
    val batteryTemp: Int = 82,
    val oilTemp: Int = 198,

    // Tires (PSI)
    val tireFl: Int = 35,
    val tireFr: Int = 35,
    val tireRl: Int = 34,
    val tireRr: Int = 34,

    // Fluids (%)
    val oilLife: Int = 68,
    val brakeFluid: Int = 94,
    val coolant: Int = 91,
    val transFluid: Int = 87,
    val washerFluid: Int = 55,

    // Systems
    val serviceIn: Int = 3200,
    val batteryVoltage: Float = 12.6f,
    val headlightsOn: Boolean = true,
    val fogLightsOn: Boolean = false,
    val interiorLightsOn: Boolean = true,
    val drlOn: Boolean = true,
    val allDoorsLocked: Boolean = true,
    val trunkClosed: Boolean = true,
    val hoodClosed: Boolean = true,

    // Climate
    val acOn: Boolean = true,
    val acTarget: Int = 68,
    val fanSpeed: Int = 3,
    val driverSeatHeat: Int = 1,  // 0-3
    val passSeatHeat: Int = 0,
    val frontDefrost: Boolean = false,
    val rearDefrost: Boolean = true,
)

class CarViewModel : ViewModel() {
    var state by mutableStateOf(CarState())
        private set

    init {
        viewModelScope.launch {
            while (true) {
                delay(2000L)
                updateSimulation()
            }
        }
    }

    private fun updateSimulation() {
        val s = state
        val newSpeed = max(0f, min(130f, s.speed + (Random.nextFloat() - 0.46f) * 5f))
        val spd = newSpeed.roundToInt()

        state = s.copy(
            speed = spd,
            rpm = (750 + spd * 40 + Random.nextInt(350)).coerceIn(750, 7500),
            fuel = max(0f, s.fuel - Random.nextFloat() * 0.004f),
            range = max(0, s.range - if (Random.nextFloat() < 0.3f) 1 else 0),
            trip = s.trip + spd * 0.00015f,
            power = (20 + spd * 0.6f + Random.nextFloat() * 15f).roundToInt(),
            engineTemp = 142 + Random.nextInt(20),
            batteryTemp = 78 + Random.nextInt(10),
            oilTemp = 190 + Random.nextInt(15),
            batteryVoltage = 12.4f + Random.nextFloat() * 0.4f,
        )
    }
}
