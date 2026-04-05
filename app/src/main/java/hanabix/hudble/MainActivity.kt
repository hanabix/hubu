package hanabix.hudble

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.remember
import hanabix.hudble.data.DeviceBatteryObserver
import hanabix.hudble.data.TimeSynchronizer
import hanabix.hudble.ui.HUDScreen
import hanabix.hudble.ui.HUDViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: HUDViewModel = viewModel(
                factory = remember {
                    object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return HUDViewModel(
                                DeviceBatteryObserver(application),
                                TimeSynchronizer()
                            ) as T
                        }
                    }
                }
            )
            HUDScreen(viewModel = viewModel)
        }
    }
}
