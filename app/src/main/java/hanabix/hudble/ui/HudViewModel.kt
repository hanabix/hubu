package hanabix.hudble.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hanabix.hudble.data.DeviceBatteryObserver
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class HudViewModel(
    private val deviceBatteryObserver: DeviceBatteryObserver,
) : ViewModel() {

    // 使用 Locale.ROOT 确保时间始终使用 ASCII 数字显示，避免某些区域设置（如阿拉伯语）使用非标准数字符号
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)

    private val _uiState = MutableStateFlow(HudUiState.preview())
    val uiState: StateFlow<HudUiState> = _uiState.asStateFlow()

    init {
        observeBatteryLevel()
        observeCurrentTime()
    }

    private fun observeCurrentTime() {
        viewModelScope.launch {
            while (isActive) {
                val now = LocalTime.now()
                _uiState.update { it.copy(currentTime = now.format(timeFormatter)) }
                // 计算到下一分钟的剩余毫秒数，避免每秒轮询
                val delayMs = 60_000L - (now.second * 1000L + now.nano / 1_000_000L)
                delay(delayMs)
            }
        }
    }

    private fun observeBatteryLevel() {
        viewModelScope.launch {
            deviceBatteryObserver.observeBatteryLevel().collect { batteryLevel ->
                updateBatteryLevel(batteryLevel)
            }
        }
    }

    private fun updateBatteryLevel(level: Int) {
        _uiState.update { currentState ->
            currentState.copy(batteryLevel = "$level%")
        }
    }
}
