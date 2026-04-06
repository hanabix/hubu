package hanabix.hudble

import android.util.Log
import android.view.KeyEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hanabix.hudble.ble.BluetoothScanner
import hanabix.hudble.ble.GattNotifier
import hanabix.hudble.ble.GattServices
import hanabix.hudble.ble.HeartRateService
import hanabix.hudble.ble.RunningSpeedCadenceService
import hanabix.hudble.model.DeviceStatus
import hanabix.hudble.model.DeviceStatus.Found
import hanabix.hudble.model.DeviceStatus.NotFound
import hanabix.hudble.model.DeviceStatus.Scanning
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/**
 * Manages BLE device scanning state machine and tap-to-rescan logic.
 *
 * State transitions:
 * ```
 * (start) → Scanning → Found(name)
 *                  → NotFound → (tap) → Scanning → ...
 * ```
 */
class DeviceViewModel(
    private val context: android.content.Context,
) : ViewModel() {

    private val bluetoothScanner = BluetoothScanner(
        context,
        listOf(GattServices.HEART_RATE, GattServices.RUNNING_SPEED_CADENCE),
    )

    private val _deviceStatus = MutableStateFlow<DeviceStatus>(Scanning)
    val deviceStatus = _deviceStatus.asStateFlow()

    private val _heartRate = MutableStateFlow<String?>(null)
    val heartRate = _heartRate.asStateFlow()

    private val _pace = MutableStateFlow<String?>(null)
    val pace = _pace.asStateFlow()

    private val _cadence = MutableStateFlow<String?>(null)
    val cadence = _cadence.asStateFlow()

    private var hrJob: Job? = null
    private var rscsJob: Job? = null

    private val keyEventChannel = Channel<KeyEvent>(Channel.UNLIMITED)

    init {
        keyEventChannel.receiveAsFlow()
            .filter { it.action == KeyEvent.ACTION_UP }
            .filter { it.keyCode in TapKeyCodes }
            .filter { _deviceStatus.value is NotFound }
            .onEach { scan() }
            .launchIn(viewModelScope)
    }

    /** Forward hardware key events from the Activity. */
    fun onKeyEvent(event: KeyEvent) {
        keyEventChannel.trySend(event)
    }

    /** Start the initial BLE device scan. */
    fun startScan() {
        scan()
    }

    private fun scan() {
        _deviceStatus.value = Scanning
        viewModelScope.launch {
            val device = bluetoothScanner.scan(10.seconds).firstOrNull() ?: run {
                _deviceStatus.value = NotFound
                return@launch
            }

            _deviceStatus.value = Found(device)

            val n = try {
                GattNotifier.connect(context = context, device = device) { status ->
                    Log.w(TAG, "Device disconnected, status=$status")
                    clearSensorValues()
                    _deviceStatus.value = NotFound
                }
            } catch (e: Exception) {
                Log.e(TAG, "Connection failed for ${device.address}: ${e.message}", e)
                _deviceStatus.value = NotFound
                return@launch
            }

            trySubscribeHrs(n)
            trySubscribeRscs(n)
        }
    }

    private fun trySubscribeHrs(notifier: GattNotifier) {
        hrJob?.cancel()
        hrJob = viewModelScope.launch {
            notifier.subscribe(HEART_RATE_SERVICE, HEART_RATE_MEASUREMENT)
                .mapNotNull { HeartRateService.read(it) }
                .onEach { bpm ->
                    Log.d(TAG, "Heart rate received: $bpm")
                    _heartRate.value = bpm.toString()
                }
                .catch { e -> Log.e(TAG, "Heart rate subscription error: ${e.message}") }
                .launchIn(this)
        }
    }

    private fun trySubscribeRscs(notifier: GattNotifier) {
        rscsJob?.cancel()
        rscsJob = viewModelScope.launch {
            notifier.subscribe(RSC_SERVICE, RSC_MEASUREMENT)
                .mapNotNull { RunningSpeedCadenceService.read(it) }
                .onEach { measurement ->
                    _cadence.value = cadence(measurement.cadenceRpm)
                    _pace.value = pace(measurement.speedMs)
                }
                .catch { e -> Log.w(TAG, "RSCS subscription ended: ${e.message}") }
                .launchIn(this)
        }
    }

    private fun clearSensorValues() {
        _heartRate.value = null
        _pace.value = null
        _cadence.value = null
    }

    override fun onCleared() {
        super.onCleared()
        hrJob?.cancel()
        rscsJob?.cancel()
    }

    companion object {
        private const val TAG = "DeviceViewModel"

        // Standard GATT UUIDs
        private val HEART_RATE_SERVICE = java.util.UUID.fromString("0000180D-0000-1000-8000-00805F9B34FB")
        private val HEART_RATE_MEASUREMENT = java.util.UUID.fromString("00002A37-0000-1000-8000-00805F9B34FB")
        private val RSC_SERVICE = java.util.UUID.fromString("00001814-0000-1000-8000-00805F9B34FB")
        private val RSC_MEASUREMENT = java.util.UUID.fromString("00002A53-0000-1000-8000-00805F9B34FB")

        private val TapKeyCodes = setOf(
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_SPACE,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_DPAD_CENTER,
        )

        /** Converts speed in m/s to pace in "M'SS\"" format. */
        internal fun pace(speedMs: Float): String? {
            if (speedMs <= 0f) return null

            val paceMinKm = 1000.0 / (speedMs * 60.0)
            val minutes = paceMinKm.toInt()
            val seconds = ((paceMinKm - minutes) * 60).roundToInt()

            return if (seconds >= 60) {
                "${minutes + 1}'00\""
            } else {
                "${minutes}'${seconds.toString().padStart(2, '0')}\""
            }
        }

        /** Converts single-leg cadence to bilateral cadence. */
        internal fun cadence(rpm: Int): String? = if (rpm > 0) (rpm * 2).toString() else null
    }
}
