package com.autohub.app.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * OBD-II Bluetooth manager for ELM327-compatible adapters.
 * Connects via classic Bluetooth RFCOMM and queries standard PIDs
 * for real-time vehicle telemetry on a 2024 VW Atlas Cross Sport.
 */
class ObdManager(context: Context) {

    companion object {
        private const val TAG = "ObdManager"

        /** Standard SPP UUID for RFCOMM */
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        /** Known OBD adapter name fragments */
        private val OBD_NAME_PATTERNS = listOf("OBD", "ELM", "Vgate", "iCar", "Veepeak", "BAFX")

        /** Read timeout in milliseconds */
        private const val READ_TIMEOUT_MS = 2000L

        /** Query loop interval in milliseconds */
        private const val QUERY_INTERVAL_MS = 500L

        /** Delay after ATZ reset */
        private const val RESET_DELAY_MS = 1000L
    }

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        ERROR
    }

    data class ObdReading(
        val speedMph: Float? = null,
        val rpm: Int? = null,
        val engineLoadPercent: Float? = null,
        val coolantTempF: Float? = null,
        val intakeTempF: Float? = null,
        val throttlePercent: Float? = null,
        val fuelLevelPercent: Float? = null,
        val controlModuleVoltage: Float? = null,
        val ambientTempF: Float? = null,
        val oilTempF: Float? = null,
        val mafRateGps: Float? = null,
        val runTimeSeconds: Int? = null
    )

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private var queryJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _reading = MutableStateFlow(ObdReading())
    val reading: StateFlow<ObdReading> = _reading.asStateFlow()

    // ---------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------

    /**
     * Returns paired Bluetooth devices whose names match known OBD adapter patterns.
     */
    @SuppressLint("MissingPermission")
    fun getObdDevices(): List<BluetoothDevice> {
        val adapter = bluetoothAdapter ?: return emptyList()
        return adapter.bondedDevices.filter { device ->
            val name = device.name ?: return@filter false
            OBD_NAME_PATTERNS.any { pattern -> name.contains(other = pattern, ignoreCase = true) }
        }
    }

    /**
     * Connects to the given Bluetooth OBD device, initialises the ELM327,
     * and starts the continuous query loop.
     */
    @SuppressLint("MissingPermission")
    suspend fun connect(device: BluetoothDevice) {
        if (_connectionState.value == ConnectionState.CONNECTING ||
            _connectionState.value == ConnectionState.CONNECTED
        ) {
            return
        }

        _connectionState.value = ConnectionState.CONNECTING
        _reading.value = ObdReading()

        withContext(Dispatchers.IO) {
            try {
                // Cancel any ongoing discovery to speed up connection
                bluetoothAdapter?.cancelDiscovery()

                val btSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket = btSocket
                btSocket.connect()

                inputStream = btSocket.inputStream
                outputStream = btSocket.outputStream

                initElm327()

                _connectionState.value = ConnectionState.CONNECTED
                Log.i(TAG, "Connected to ${device.name}")

                startQueryLoop()
            } catch (e: IOException) {
                Log.e(TAG, "Connection failed", e)
                cleanupConnection()
                _connectionState.value = ConnectionState.ERROR
            }
        }
    }

    /**
     * Disconnects from the current OBD device and stops polling.
     */
    fun disconnect() {
        queryJob?.cancel()
        queryJob = null
        cleanupConnection()
        _connectionState.value = ConnectionState.DISCONNECTED
        _reading.value = ObdReading()
        Log.i(TAG, "Disconnected")
    }

    // ---------------------------------------------------------------
    // ELM327 init
    // ---------------------------------------------------------------

    private suspend fun initElm327() {
        // ATZ — reset
        sendCommand("ATZ")
        delay(RESET_DELAY_MS)
        // Flush any reset banner text
        drainInput()

        // ATE0 — echo off
        sendCommandAndRead("ATE0")
        // ATL0 — linefeeds off
        sendCommandAndRead("ATL0")
        // ATS0 — spaces off
        sendCommandAndRead("ATS0")
        // ATH0 — headers off
        sendCommandAndRead("ATH0")
        // ATSP6 — CAN 11-bit 500 kbaud (ISO 15765-4) — correct for VW
        sendCommandAndRead("ATSP6")
    }

    // ---------------------------------------------------------------
    // Query loop
    // ---------------------------------------------------------------

    private fun startQueryLoop() {
        queryJob = scope.launch {
            while (isActive && _connectionState.value == ConnectionState.CONNECTED) {
                try {
                    val current = _reading.value

                    val speed = queryPid("010D")
                    val rpm = queryPid("010C")
                    val load = queryPid("0104")
                    val coolant = queryPid("0105")
                    val intake = queryPid("010F")
                    val throttle = queryPid("0111")
                    val fuel = queryPid("012F")
                    val voltage = queryPid("0142")
                    val ambient = queryPid("0146")
                    val oil = queryPid("015C")
                    val maf = queryPid("0110")
                    val runTime = queryPid("011F")

                    _reading.value = current.copy(
                        speedMph = speed?.let { parseSpeed(it) } ?: current.speedMph,
                        rpm = rpm?.let { parseRpm(it) } ?: current.rpm,
                        engineLoadPercent = load?.let { parsePercent255(it) } ?: current.engineLoadPercent,
                        coolantTempF = coolant?.let { parseTempF(it) } ?: current.coolantTempF,
                        intakeTempF = intake?.let { parseTempF(it) } ?: current.intakeTempF,
                        throttlePercent = throttle?.let { parsePercent255(it) } ?: current.throttlePercent,
                        fuelLevelPercent = fuel?.let { parsePercent255(it) } ?: current.fuelLevelPercent,
                        controlModuleVoltage = voltage?.let { parseVoltage(it) } ?: current.controlModuleVoltage,
                        ambientTempF = ambient?.let { parseTempF(it) } ?: current.ambientTempF,
                        oilTempF = oil?.let { parseTempF(it) } ?: current.oilTempF,
                        mafRateGps = maf?.let { parseMaf(it) } ?: current.mafRateGps,
                        runTimeSeconds = runTime?.let { parseRunTime(it) } ?: current.runTimeSeconds
                    )

                    delay(QUERY_INTERVAL_MS)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Query loop error", e)
                    cleanupConnection()
                    _connectionState.value = ConnectionState.ERROR
                    break
                }
            }
        }
    }

    // ---------------------------------------------------------------
    // PID query + response parsing
    // ---------------------------------------------------------------

    /**
     * Sends a PID command and returns the raw hex data bytes (after the "41 XX" prefix),
     * or null if the response was invalid / timed out.
     */
    private suspend fun queryPid(pid: String): String? {
        val raw = sendCommandAndRead(pid) ?: return null
        return parseRawResponse(raw = raw, pid = pid)
    }

    /**
     * Parses an ELM327 response string. Strips whitespace, locates the
     * "41XX" echo prefix, and returns the data bytes that follow it.
     */
    private fun parseRawResponse(raw: String, pid: String): String? {
        // Strip spaces and control chars
        val cleaned = raw.replace(" ", "").replace("\r", "").replace("\n", "").trim()
        // Expected echo prefix, e.g. "410D" for PID 010D
        val prefix = "41" + pid.takeLast(n = 2).uppercase()
        val idx = cleaned.indexOf(string = prefix)
        if (idx == -1) return null
        val dataStart = idx + prefix.length
        if (dataStart >= cleaned.length) return null
        return cleaned.substring(startIndex = dataStart)
    }

    // ---- Individual PID parsers ----

    /** 010D: Vehicle speed — single byte km/h, converted to mph */
    private fun parseSpeed(data: String): Float? {
        val a = hexByte(data = data, index = 0) ?: return null
        return a * 0.621371f
    }

    /** 010C: RPM — two bytes, (A*256+B)/4 */
    private fun parseRpm(data: String): Int? {
        val a = hexByte(data = data, index = 0) ?: return null
        val b = hexByte(data = data, index = 1) ?: return null
        return (a * 256 + b) / 4
    }

    /** Generic A*100/255 percent parser (0104, 0111, 012F) */
    private fun parsePercent255(data: String): Float? {
        val a = hexByte(data = data, index = 0) ?: return null
        return a * 100f / 255f
    }

    /** Temperature in Celsius (A-40) converted to Fahrenheit (0105, 010F, 0146, 015C) */
    private fun parseTempF(data: String): Float? {
        val a = hexByte(data = data, index = 0) ?: return null
        val celsius = a - 40f
        return celsius * 9f / 5f + 32f
    }

    /** 0142: Control module voltage — (A*256+B)/1000 V */
    private fun parseVoltage(data: String): Float? {
        val a = hexByte(data = data, index = 0) ?: return null
        val b = hexByte(data = data, index = 1) ?: return null
        return (a * 256 + b) / 1000f
    }

    /** 0110: MAF rate — (A*256+B)/100 g/s */
    private fun parseMaf(data: String): Float? {
        val a = hexByte(data = data, index = 0) ?: return null
        val b = hexByte(data = data, index = 1) ?: return null
        return (a * 256 + b) / 100f
    }

    /** 011F: Run time — A*256+B seconds */
    private fun parseRunTime(data: String): Int? {
        val a = hexByte(data = data, index = 0) ?: return null
        val b = hexByte(data = data, index = 1) ?: return null
        return a * 256 + b
    }

    /**
     * Extracts a single hex byte from a data string at the given byte index.
     * Each byte is two hex characters (index 0 = chars 0-1, index 1 = chars 2-3, ...).
     */
    private fun hexByte(data: String, index: Int): Int? {
        val charStart = index * 2
        if (charStart + 2 > data.length) return null
        return try {
            data.substring(startIndex = charStart, endIndex = charStart + 2)
                .toInt(radix = 16)
        } catch (_: NumberFormatException) {
            null
        }
    }

    // ---------------------------------------------------------------
    // Low-level Bluetooth I/O
    // ---------------------------------------------------------------

    private fun sendCommand(command: String) {
        try {
            val os = outputStream ?: return
            os.write("$command\r".toByteArray())
            os.flush()
        } catch (e: IOException) {
            Log.e(TAG, "Send failed: $command", e)
        }
    }

    private suspend fun sendCommandAndRead(command: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val os = outputStream ?: return@withContext null
                val ins = inputStream ?: return@withContext null

                os.write("$command\r".toByteArray())
                os.flush()

                readResponse(inputStream = ins)
            } catch (e: IOException) {
                Log.e(TAG, "I/O error for command: $command", e)
                null
            }
        }
    }

    /**
     * Reads bytes from the input stream until the ELM327 prompt character '>'
     * is received or the read timeout is reached.
     */
    private suspend fun readResponse(inputStream: InputStream): String? {
        return withContext(Dispatchers.IO) {
            val buffer = StringBuilder()
            val deadline = System.currentTimeMillis() + READ_TIMEOUT_MS
            try {
                while (System.currentTimeMillis() < deadline) {
                    if (inputStream.available() > 0) {
                        val byte = inputStream.read()
                        if (byte == -1) break
                        val char = byte.toChar()
                        if (char == '>') break
                        buffer.append(char)
                    } else {
                        delay(timeMillis = 10)
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Read error", e)
                return@withContext null
            }
            val result = buffer.toString().trim()
            if (result.isEmpty() || result.contains(other = "NO DATA") || result.contains(other = "ERROR")) {
                null
            } else {
                result
            }
        }
    }

    /**
     * Drains any pending bytes in the input stream (used after ATZ reset).
     */
    private fun drainInput() {
        try {
            val ins = inputStream ?: return
            while (ins.available() > 0) {
                ins.read()
            }
        } catch (_: IOException) {
            // Ignore
        }
    }

    /**
     * Closes the Bluetooth socket and nulls out stream references.
     */
    private fun cleanupConnection() {
        try {
            inputStream?.close()
        } catch (_: IOException) { }
        try {
            outputStream?.close()
        } catch (_: IOException) { }
        try {
            socket?.close()
        } catch (_: IOException) { }
        inputStream = null
        outputStream = null
        socket = null
    }
}
