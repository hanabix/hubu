package hanabix.hudble.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hanabix.hudble.data.BatteryDataReader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HudViewModel(
    private val batteryDataReader: BatteryDataReader,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HudUiState.preview())
    val uiState: StateFlow<HudUiState> = _uiState.asStateFlow()

    init {
        observeBatteryLevel()
    }

    private fun observeBatteryLevel() {
        viewModelScope.launch {
            batteryDataReader.observeBatteryLevel().collect { batteryLevel ->
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
