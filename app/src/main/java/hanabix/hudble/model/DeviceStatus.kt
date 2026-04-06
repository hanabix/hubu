package hanabix.hudble.model

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice

/**
 * Represents the BLE device scanning state machine.
 */
sealed interface DeviceStatus {

    /** Returns the display label for this state. */
    fun label(): String

    /** Actively scanning for devices. */
    data object Scanning : DeviceStatus {
        override fun label(): String = "Scanning..."
    }

    /** A compatible device was found and scanned successfully. */
    data class Found(val device: BluetoothDevice) : DeviceStatus {
        @SuppressLint("MissingPermission")
        override fun label(): String = device.name ?: "Unknown"
    }

    /** Scanning completed without finding any compatible device. Tap to rescan. */
    data object NotFound : DeviceStatus {
        override fun label(): String = "Tap to rescan"
    }
}
