package hanabix.hubu

import android.Manifest

internal fun permissionsFor(sdk: Int): Set<String> =
    if (sdk >= 31) {
        setOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
        )
    } else {
        setOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
