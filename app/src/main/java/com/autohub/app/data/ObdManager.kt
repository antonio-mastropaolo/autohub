package com.autohub.app.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlin.coroutines.resume

/**
 * OBD-II Bluetooth manager for ELM327-compatible adapters.
 * Supports BOTH classic Bluetooth SPP (RFCOMM) AND Bluetooth Low Energy (BLE) GATT connections.
 *
 * Classic BT: standard ELM327/Vgate/BAFX adapters that pair via Bluetooth settings.
 * BLE GATT:   Innova / Hyper Tough HT500 / 3250 adapters that advertise as BLE peripherals
 *             and communicate through GATT service characteristics.
 */
class ObdManager(private val context: Context) {

    companion object {
        private const val TAG = "ObdManager"

        // ----- Classic Bluetooth SPP -----
        /** Standard SPP UUID for RFCOMM */
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        // ----- BLE GATT service/characteristic UUIDs -----
        /** Common BLE OBD service UUIDs (Innova, generic ELM327-BLE clones) */
        private val BLE_SERVICE_UUIDS = listOf(
            UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb"),
            UUID.fromString("000018f0-0000-1000-8000-00805f9b34fb"),
            UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
        )

        /** Write characteristic UUIDs */
        private val BLE_WRITE_CHAR_UUIDS = listOf(
            UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb"),
            UUID.fromString("0000ffe2-0000-1000-8000-00805f9b34fb")
        )

        /** Notify / read characteristic UUIDs */
        private val BLE_NOTIFY_CHAR_UUIDS = listOf(
            UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb"),
            UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
        )

        /** Client Characteristic Configuration Descriptor — required to enable BLE notifications */
        private val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        // ----- Device name matching -----
        /** Classic OBD adapter name fragments */
        private val OBD_NAME_PATTERNS = listOf(
            "OBD", "ELM", "Vgate", "iCar", "Veepeak", "BAFX"
        )

        /** BLE OBD adapter name fragments (Innova / Hyper Tough family) */
        private val BLE_OBD_NAME_PATTERNS = listOf(
            "INNOVA", "HT500", "Hyper", "3250", "INN"
        )

        /** Combined set for any OBD device detection */
        private val ALL_OBD_NAME_PATTERNS = OBD_NAME_PATTERNS + BLE_OBD_NAME_PATTERNS

        // ----- Timing -----
        /** Read timeout in milliseconds */
        private const val READ_TIMEOUT_MS = 2000L

        /** BLE response timeout — BLE can be slower with chunked replies */
        private const val BLE_READ_TIMEOUT_MS = 3000L

        /** Query loop interval in milliseconds */
        private const val QUERY_INTERVAL_MS = 500L

        /** Delay after ATZ reset */
        private const val RESET_DELAY_MS = 1000L

        /** BLE scan duration in milliseconds */
        private const val BLE_SCAN_DURATION_MS = 5000L
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

    // ----- Classic BT state -----
    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    // ----- BLE GATT state -----
    private var gatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var isBle = false

    /** Buffer that accumulates BLE notification chunks until the '>' prompt */
    private val bleResponseBuffer = StringBuilder()

    /** Mutex so only one command-response exchange happens at a time over BLE */
    private val bleMutex = Mutex()

    /** Continuation used to resume the caller once a full BLE response is received */
    private var bleResponseContinuation: CancellableContinuation<String?>? = null

    // ----- Shared state -----
    private var queryJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _reading = MutableStateFlow(ObdReading())
    val reading: StateFlow<ObdReading> = _reading.asStateFlow()

    // ---------------------------------------------------------------
    // BLE GATT Callback
    // ---------------------------------------------------------------

    @SuppressLint("MissingPermission")
    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "BLE GATT connected, discovering services...")
                    g.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.w(TAG, "BLE GATT disconnected (status=$status)")
                    if (_connectionState.value == ConnectionState.CONNECTED ||
                        _connectionState.value == ConnectionState.CONNECTING
                    ) {
                        cleanupConnection()
                        _connectionState.value = ConnectionState.ERROR
                    }
                }
            }
        }

        @Suppress("DEPRECATION")
        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "BLE service discovery failed: status=$status")
                cleanupConnection()
                _connectionState.value = ConnectionState.ERROR
                return
            }

            Log.i(TAG, "BLE services discovered: ${g.services.map { it.uuid }}")

            var foundWrite: BluetoothGattCharacteristic? = null
            var foundNotify: BluetoothGattCharacteristic? = null

            for (service in g.services) {
                if (service.uuid !in BLE_SERVICE_UUIDS) continue
                Log.i(TAG, "Matched BLE OBD service: ${service.uuid}")

                for (char in service.characteristics) {
                    if (char.uuid in BLE_WRITE_CHAR_UUIDS && foundWrite == null) {
                        foundWrite = char
                        Log.i(TAG, "Found BLE write characteristic: ${char.uuid}")
                    }
                    if (char.uuid in BLE_NOTIFY_CHAR_UUIDS && foundNotify == null) {
                        foundNotify = char
                        Log.i(TAG, "Found BLE notify characteristic: ${char.uuid}")
                    }
                }
            }

            if (foundWrite == null || foundNotify == null) {
                Log.e(TAG, "Could not locate required BLE OBD characteristics")
                cleanupConnection()
                _connectionState.value = ConnectionState.ERROR
                return
            }

            writeCharacteristic = foundWrite

            // Enable notifications on the read/notify characteristic
            g.setCharacteristicNotification(foundNotify, true)
            val descriptor = foundNotify.getDescriptor(CCCD_UUID)
            if (descriptor != null) {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                g.writeDescriptor(descriptor)
                Log.i(TAG, "BLE notifications enabled on ${foundNotify.uuid}")
            } else {
                Log.w(TAG, "CCCD descriptor not found on notify characteristic")
            }

            // Mark connection as ready — ELM init will happen on the coroutine that called connect()
            isBle = true
            _connectionState.value = ConnectionState.CONNECTED
        }

        @Deprecated("Deprecated in API 33, still needed for lower API levels")
        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val chunk = characteristic.value ?: return
            val text = String(chunk)
            bleResponseBuffer.append(text)

            // The ELM327 prompt '>' signals end of response
            if (text.contains(">")) {
                val full = bleResponseBuffer.toString().trim()
                bleResponseBuffer.clear()
                bleResponseContinuation?.let {
                    bleResponseContinuation = null
                    val cleaned = full.replace(">", "").trim()
                    if (cleaned.isEmpty() || cleaned.contains("NO DATA") || cleaned.contains("ERROR")) {
                        it.resume(null)
                    } else {
                        it.resume(cleaned)
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------

    /**
     * Returns paired Bluetooth devices whose names match known OBD adapter patterns.
     * This includes both classic BT and BLE-capable devices that are already bonded.
     */
    @SuppressLint("MissingPermission")
    fun getObdDevices(): List<BluetoothDevice> {
        val adapter = bluetoothAdapter ?: return emptyList()
        return adapter.bondedDevices.filter { device ->
            val name = device.name ?: return@filter false
            ALL_OBD_NAME_PATTERNS.any { pattern -> name.contains(other = pattern, ignoreCase = true) }
        }
    }

    /**
     * Scans for nearby BLE OBD devices that may not be paired.
     * This catches devices like the Hyper Tough HT500 that advertise via BLE
     * and do not need to be paired beforehand.
     *
     * Returns discovered devices after [BLE_SCAN_DURATION_MS] milliseconds.
     */
    @SuppressLint("MissingPermission")
    suspend fun scanForBleDevices(): List<BluetoothDevice> {
        val adapter = bluetoothAdapter ?: return emptyList()
        val scanner: BluetoothLeScanner = adapter.bluetoothLeScanner ?: run {
            Log.w(TAG, "BLE scanner not available")
            return emptyList()
        }

        val discovered = mutableMapOf<String, BluetoothDevice>()

        return suspendCancellableCoroutine { cont ->
            val callback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    val device = result.device
                    val name = device.name ?: result.scanRecord?.deviceName ?: return
                    if (ALL_OBD_NAME_PATTERNS.any { name.contains(it, ignoreCase = true) }) {
                        Log.i(TAG, "BLE scan found OBD device: $name [${device.address}]")
                        discovered[device.address] = device
                    }
                }

                override fun onScanFailed(errorCode: Int) {
                    Log.e(TAG, "BLE scan failed: errorCode=$errorCode")
                    if (cont.isActive) cont.resume(emptyList())
                }
            }

            scanner.startScan(callback)
            Log.i(TAG, "BLE scan started")

            // Stop scan after the duration and return results
            scope.launch {
                delay(BLE_SCAN_DURATION_MS)
                try {
                    scanner.stopScan(callback)
                } catch (_: Exception) { }
                Log.i(TAG, "BLE scan stopped, found ${discovered.size} device(s)")
                if (cont.isActive) cont.resume(discovered.values.toList())
            }

            cont.invokeOnCancellation {
                try {
                    scanner.stopScan(callback)
                } catch (_: Exception) { }
            }
        }
    }

    /**
     * Connects to the given Bluetooth OBD device, initialises the ELM327,
     * and starts the continuous query loop.
     *
     * Attempts BLE GATT first. If the device does not support BLE or GATT
     * connection fails, falls back to classic Bluetooth SPP (RFCOMM).
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

        // Cancel any ongoing discovery to speed up connection
        bluetoothAdapter?.cancelDiscovery()

        val deviceType = device.type
        val isBleCapable = deviceType == BluetoothDevice.DEVICE_TYPE_LE ||
                deviceType == BluetoothDevice.DEVICE_TYPE_DUAL

        Log.i(TAG, "Connecting to ${device.name} [${device.address}] type=$deviceType bleCapable=$isBleCapable")

        // Try BLE first for BLE or dual-mode devices
        if (isBleCapable) {
            val bleSuccess = tryConnectBle(device)
            if (bleSuccess) {
                Log.i(TAG, "BLE connection established to ${device.name}")
                return
            }
            Log.w(TAG, "BLE connection failed for ${device.name}, falling back to classic SPP")
            // Reset state before fallback
            _connectionState.value = ConnectionState.CONNECTING
        }

        // Fallback: classic Bluetooth SPP
        tryConnectClassic(device)
    }

    /**
     * Auto-connect flow:
     * 1. Check paired classic BT devices for known OBD adapters.
     * 2. If none found, scan BLE for unpaired OBD devices.
     * 3. Connect to the first OBD device found.
     */
    @SuppressLint("MissingPermission")
    suspend fun connectAny() {
        if (_connectionState.value == ConnectionState.CONNECTING ||
            _connectionState.value == ConnectionState.CONNECTED
        ) {
            return
        }

        // Step 1: Check paired devices
        val pairedDevices = getObdDevices()
        if (pairedDevices.isNotEmpty()) {
            Log.i(TAG, "Found ${pairedDevices.size} paired OBD device(s), connecting to first...")
            connect(pairedDevices.first())
            return
        }

        // Step 2: Scan BLE for unpaired devices
        Log.i(TAG, "No paired OBD devices found, scanning BLE...")
        val bleDevices = scanForBleDevices()
        if (bleDevices.isNotEmpty()) {
            Log.i(TAG, "Found ${bleDevices.size} BLE OBD device(s), connecting to first...")
            connect(bleDevices.first())
            return
        }

        Log.w(TAG, "No OBD devices found via pairing or BLE scan")
        _connectionState.value = ConnectionState.ERROR
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
    // Connection strategies
    // ---------------------------------------------------------------

    /**
     * Attempts a BLE GATT connection. Returns true if the connection was established
     * and services were discovered successfully, false otherwise.
     */
    @SuppressLint("MissingPermission")
    private suspend fun tryConnectBle(device: BluetoothDevice): Boolean {
        isBle = false
        bleResponseBuffer.clear()
        bleResponseContinuation = null

        return withContext(Dispatchers.Main) {
            try {
                // connectGatt must be called on the main thread for reliable behaviour
                val g = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
                gatt = g

                // Wait for onServicesDiscovered to set state to CONNECTED (or ERROR)
                val deadline = System.currentTimeMillis() + 10_000L
                while (_connectionState.value == ConnectionState.CONNECTING &&
                    System.currentTimeMillis() < deadline
                ) {
                    delay(100)
                }

                if (_connectionState.value != ConnectionState.CONNECTED || !isBle) {
                    // Cleanup partial BLE connection
                    try { g.close() } catch (_: Exception) { }
                    gatt = null
                    writeCharacteristic = null
                    isBle = false
                    return@withContext false
                }

                // Small settling delay then initialize ELM327 over BLE
                delay(500)
                withContext(Dispatchers.IO) {
                    initElm327()
                }

                Log.i(TAG, "BLE ELM327 initialised on ${device.name}")
                startQueryLoop()
                true
            } catch (e: Exception) {
                Log.e(TAG, "BLE connect exception", e)
                cleanupBle()
                false
            }
        }
    }

    /**
     * Classic Bluetooth SPP (RFCOMM) connection.
     */
    @SuppressLint("MissingPermission")
    private suspend fun tryConnectClassic(device: BluetoothDevice) {
        withContext(Dispatchers.IO) {
            try {
                val btSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket = btSocket
                btSocket.connect()

                inputStream = btSocket.inputStream
                outputStream = btSocket.outputStream

                initElm327()

                isBle = false
                _connectionState.value = ConnectionState.CONNECTED
                Log.i(TAG, "Classic SPP connected to ${device.name}")

                startQueryLoop()
            } catch (e: IOException) {
                Log.e(TAG, "Classic SPP connection failed", e)
                cleanupConnection()
                _connectionState.value = ConnectionState.ERROR
            }
        }
    }

    // ---------------------------------------------------------------
    // ELM327 init
    // ---------------------------------------------------------------

    private suspend fun initElm327() {
        // ATZ — reset
        sendCommand("ATZ")
        delay(RESET_DELAY_MS)
        // Flush any reset banner text
        if (isBle) {
            // For BLE, give time for notification chunks to arrive and clear them
            delay(500)
            bleResponseBuffer.clear()
        } else {
            drainInput()
        }

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
    // Low-level I/O — dispatches to BLE or classic depending on mode
    // ---------------------------------------------------------------

    @SuppressLint("MissingPermission")
    private fun sendCommand(command: String) {
        if (isBle) {
            sendCommandBle(command)
        } else {
            sendCommandClassic(command)
        }
    }

    private suspend fun sendCommandAndRead(command: String): String? {
        return if (isBle) {
            sendCommandAndReadBle(command)
        } else {
            sendCommandAndReadClassic(command)
        }
    }

    // ---- Classic Bluetooth I/O ----

    private fun sendCommandClassic(command: String) {
        try {
            val os = outputStream ?: return
            os.write("$command\r".toByteArray())
            os.flush()
        } catch (e: IOException) {
            Log.e(TAG, "Classic send failed: $command", e)
        }
    }

    private suspend fun sendCommandAndReadClassic(command: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val os = outputStream ?: return@withContext null
                val ins = inputStream ?: return@withContext null

                os.write("$command\r".toByteArray())
                os.flush()

                readResponseClassic(inputStream = ins)
            } catch (e: IOException) {
                Log.e(TAG, "Classic I/O error for command: $command", e)
                null
            }
        }
    }

    /**
     * Reads bytes from the classic BT input stream until the ELM327 prompt character '>'
     * is received or the read timeout is reached.
     */
    private suspend fun readResponseClassic(inputStream: InputStream): String? {
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
                Log.e(TAG, "Classic read error", e)
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

    // ---- BLE GATT I/O ----

    @SuppressLint("MissingPermission")
    private fun sendCommandBle(command: String) {
        val g = gatt ?: return
        val wc = writeCharacteristic ?: return
        try {
            val payload = "$command\r".toByteArray()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                g.writeCharacteristic(
                    wc,
                    payload,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )
            } else {
                @Suppress("DEPRECATION")
                wc.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                @Suppress("DEPRECATION")
                wc.value = payload
                @Suppress("DEPRECATION")
                g.writeCharacteristic(wc)
            }
        } catch (e: Exception) {
            Log.e(TAG, "BLE send failed: $command", e)
        }
    }

    /**
     * Sends an ELM327 command over BLE and suspends until a full response
     * (terminated by '>') is received via the notification callback,
     * or until the BLE read timeout elapses.
     */
    @SuppressLint("MissingPermission")
    private suspend fun sendCommandAndReadBle(command: String): String? {
        return bleMutex.withLock {
            bleResponseBuffer.clear()

            // Send the command
            sendCommandBle(command)

            // Wait for response via suspendCancellableCoroutine
            withTimeoutOrNull(BLE_READ_TIMEOUT_MS) {
                suspendCancellableCoroutine { cont ->
                    bleResponseContinuation = cont
                    cont.invokeOnCancellation {
                        bleResponseContinuation = null
                    }
                }
            }
        }
    }

    /**
     * Drains any pending bytes in the classic BT input stream (used after ATZ reset).
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

    // ---------------------------------------------------------------
    // Cleanup
    // ---------------------------------------------------------------

    @SuppressLint("MissingPermission")
    private fun cleanupBle() {
        try {
            gatt?.close()
        } catch (_: Exception) { }
        gatt = null
        writeCharacteristic = null
        isBle = false
        bleResponseBuffer.clear()
        bleResponseContinuation?.let {
            bleResponseContinuation = null
            it.resume(null)
        }
    }

    /**
     * Closes all Bluetooth resources (classic + BLE) and nulls out references.
     */
    @SuppressLint("MissingPermission")
    private fun cleanupConnection() {
        // Classic cleanup
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

        // BLE cleanup
        cleanupBle()
    }
}
