package hanabix.hudble.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class BleInfoTest {

    @Test
    fun `scan result info uses device address as id`() {
        val result = mockk<ScanResult>()
        val device = mockk<BluetoothDevice>()
        every { result.device } returns device
        every { device.address } returns "38:F9:F5:18:BC:57"

        assertEquals("38:F9:F5:18:BC:57", ScanResultBleInfo.id(result))
    }

    @Test
    fun `scan result info uses scan record name`() {
        val result = mockk<ScanResult>()
        val device = mockk<BluetoothDevice>()
        val record = mockk<ScanRecord>()
        every { result.device } returns device
        every { result.scanRecord } returns record
        every { device.address } returns "38:F9:F5:18:BC:57"
        every { record.deviceName } returns "Enduro 2"

        assertEquals("Enduro 2", ScanResultBleInfo.name(result))
    }

    @Test
    fun `scan result info falls back to address`() {
        val result = mockk<ScanResult>()
        val device = mockk<BluetoothDevice>()
        every { result.device } returns device
        every { result.scanRecord } returns null
        every { device.address } returns "38:F9:F5:18:BC:57"

        assertEquals("38:F9:F5:18:BC:57", ScanResultBleInfo.name(result))
    }
}
