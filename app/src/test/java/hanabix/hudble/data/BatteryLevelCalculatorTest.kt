package hanabix.hudble.data

import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryLevelCalculatorTest {

    @Test
    fun normalValue() {
        assertEquals(87, BatteryLevelCalculator.calculate(87, 100))
    }

    @Test
    fun roundsCorrectly() {
        assertEquals(88, BatteryLevelCalculator.calculate(7, 8))
    }

    @Test
    fun zeroPercent() {
        assertEquals(0, BatteryLevelCalculator.calculate(0, 100))
    }

    @Test
    fun fullCharge() {
        assertEquals(100, BatteryLevelCalculator.calculate(100, 100))
    }

    @Test
    fun invalidValues() {
        assertEquals(0, BatteryLevelCalculator.calculate(-1, -1))
    }

    @Test
    fun zeroScale() {
        assertEquals(0, BatteryLevelCalculator.calculate(50, 0))
    }
}
