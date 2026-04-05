package hanabix.hudble.data

import android.bluetooth.BluetoothDevice

/**
 * Represents the BLE device scanning state machine.
 */
sealed interface DeviceStatus {
    val label: String

    /** Actively scanning for devices. */
    data object Scanning : DeviceStatus {
        override val label: String = "Scanning..."
    }

    /** A compatible device was found and scanned successfully. */
    data class Found(val device: BluetoothDevice) : DeviceStatus {
        override val label: String = device.name ?: "Unknown"
    }

    /** Scanning completed without finding any compatible device. Tap to rescan. */
    data object NotFound : DeviceStatus {
        override val label: String = "Tap to rescan"
    }
}
