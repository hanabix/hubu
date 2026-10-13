package hanabix.hubu

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionPolicyTest {
    @Test
    fun `Android 12 requests bluetooth permissions`() {
        assertEquals(
            setOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
            ),
            permissionsFor(32),
        )
    }

    @Test
    fun `Android 11 requests location permission`() {
        assertEquals(
            setOf(Manifest.permission.ACCESS_FINE_LOCATION),
            permissionsFor(30),
        )
    }
}
