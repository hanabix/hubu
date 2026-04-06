package hanabix.hudble.util

import org.junit.Assert.assertEquals
import org.junit.Test

class HostBatteryTest {

    @Test
    fun `calc normal value`() {
        assertEquals(87, HostBattery.calc(87, 100))
    }

    @Test
    fun `calc rounds correctly`() {
        assertEquals(88, HostBattery.calc(7, 8))
    }

    @Test
    fun `calc zero percent`() {
        assertEquals(0, HostBattery.calc(0, 100))
    }

    @Test
    fun `calc full charge`() {
        assertEquals(100, HostBattery.calc(100, 100))
    }

    @Test
    fun `calc invalid values`() {
        assertEquals(0, HostBattery.calc(-1, -1))
    }

    @Test
    fun `calc zero scale`() {
        assertEquals(0, HostBattery.calc(50, 0))
    }
}
