package hanabix.hubu.android

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.content.Context
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

class BleFindTest {

    @Test
    fun `scan callback emits device source once and uses deviceName`() {
        val channel = Channel<hanabix.hubu.model.Find.Status<DeviceSource>>(Channel.UNLIMITED)
        val callback = ScanCallbackBridge(
            channel = channel,
            logger = NoopLogger,
        )
        val result = result(
            address = "AA:BB:CC:DD:EE:FF",
            name = "watch",
        )

        callback.onScanResult(0, result)
        callback.onScanResult(0, result)
        channel.close()

        assertEquals(
            listOf(
                hanabix.hubu.model.Find.Status.Found(
                    DeviceSource(
                        name = "watch",
                        device = result.device,
                    ),
                ),
            ),
            runBlocking { channel.receiveAsFlow().toList() },
        )
    }

    @Test
    fun `scan callback falls back to address when deviceName is null`() {
        val channel = Channel<hanabix.hubu.model.Find.Status<DeviceSource>>(Channel.UNLIMITED)
        val callback = ScanCallbackBridge(
            channel = channel,
            logger = NoopLogger,
        )
        val result = result(
            address = "AA:BB:CC:DD:EE:FF",
            name = null,
        )

        callback.onScanResult(0, result)
        channel.close()

        assertEquals(
            listOf(
                hanabix.hubu.model.Find.Status.Found(
                    DeviceSource(
                        name = "AA:BB:CC:DD:EE:FF",
                        device = result.device,
                    ),
                ),
            ),
            runBlocking { channel.receiveAsFlow().toList() },
        )
    }

    @Test
    fun `scan callback emits done on scan failed`() = runBlocking {
        val channel = Channel<hanabix.hubu.model.Find.Status<DeviceSource>>(Channel.UNLIMITED)
        val logger = mockk<Logger>(relaxed = true)
        val callback = ScanCallbackBridge(channel, logger)

        callback.onScanFailed(7)

        val done = channel.receiveAsFlow().toList().single() as hanabix.hubu.model.Find.Status.Done
        assertTrue(done.cause?.message == "BLE scan failed: code=7")
        verify(exactly = 1) { logger.e("BleFind", "BLE scan failed: code=7") }
    }

    @Test
    fun `ble find completes on timeout`() = runBlocking {
        val context = mockk<Context>()
        val manager = mockk<BluetoothManager>()
        val adapter = mockk<BluetoothAdapter>()
        val scanner = mockk<android.bluetooth.le.BluetoothLeScanner>()
        every { context.getSystemService(Context.BLUETOOTH_SERVICE) } returns manager
        every { manager.adapter } returns adapter
        every { adapter.bluetoothLeScanner } returns scanner
        every { scanner.startScan(any<ScanCallback>()) } just Runs
        every { scanner.stopScan(any<ScanCallback>()) } just Runs

        val events = BleFind(context, 100.milliseconds).invoke(emptySet()).toList()

        assertEquals(
            listOf(hanabix.hubu.model.Find.Status.Done(null)),
            events,
        )
        verify(exactly = 1) { scanner.startScan(any<ScanCallback>()) }
        verify(exactly = 1) { scanner.stopScan(any<ScanCallback>()) }
    }

    private fun result(
        address: String,
        name: String?,
    ): ScanResult {
        val device = mockk<BluetoothDevice>()
        val record = mockk<ScanRecord>()
        val result = mockk<ScanResult>()
        every { device.address } returns address
        every { result.device } returns device
        every { result.scanRecord } returns record
        every { record.deviceName } returns name
        return result
    }
}
