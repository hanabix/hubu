package hanabix.hudble.data

import kotlin.math.roundToInt

internal object BatteryLevelCalculator {

    fun calculate(level: Int, scale: Int): Int {
        return if (level >= 0 && scale > 0) {
            (level * 100f / scale).roundToInt()
        } else {
            0
        }
    }
}
