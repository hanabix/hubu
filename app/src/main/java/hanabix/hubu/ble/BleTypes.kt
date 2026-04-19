package hanabix.hubu.ble

import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow

internal fun interface BleScan<T> {
    operator fun invoke(metrics: List<BleMetric>): Flow<T>
}

internal fun interface BleConnect<T> {
    operator fun invoke(metrics: List<BleMetric>): (T) -> Flow<Event<T>>

    sealed interface Event<out T> {
        data class Connected(
            val unsupported: List<BleMetric>,
        ) : Event<Nothing>

        data class Abandon<T>(
            val device: T,
            val unsupported: List<BleMetric>,
        ) : Event<T>

        data class Notify<T>(
            val device: T,
            val meter: BleMeter,
        ) : Event<T>

        data class Disconnected<T>(
            val device: T,
            val cause: String,
        ) : Event<T>
    }
}

internal fun interface BleGather {
    operator fun invoke(metrics: List<BleMetric>): Flow<BleEvent>
}

internal interface BleChannel<A> {
    fun emit(a: A)

    fun close()
}

internal data class ScannedDevice(
    val device: BluetoothDevice,
    val name: String,
)

internal const val STATUS_CONNECTING = "Connecting"
internal const val STATUS_TAP_TO_RECONNECT = "Tap to Reconnect"

internal enum class BleMetric(
    val service: java.util.UUID,
    val characteristic: java.util.UUID,
) {
    HeartRate(
        service = java.util.UUID.fromString("0000180D-0000-1000-8000-00805F9B34FB"),
        characteristic = java.util.UUID.fromString("00002A37-0000-1000-8000-00805F9B34FB"),
    ),
    RunSpeedCadence(
        service = java.util.UUID.fromString("00001814-0000-1000-8000-00805F9B34FB"),
        characteristic = java.util.UUID.fromString("00002A53-0000-1000-8000-00805F9B34FB"),
    ),
}

internal data class BleMeter(
    val metric: BleMetric,
    val data: ByteArray,
)

internal sealed interface BleEvent {
    data class Available(
        val device: String,
        val meter: BleMeter,
    ) : BleEvent

    data object Unavailable : BleEvent
}
