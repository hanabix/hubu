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
import hanabix.hudble.ui.HudScreen
import hanabix.hudble.ui.HudViewModel
import hanabix.hudble.ui.theme.HudbleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HudbleTheme {
                val viewModel: HudViewModel = viewModel(
                    factory = remember {
                        object : ViewModelProvider.Factory {
                            @Suppress("UNCHECKED_CAST")
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                return HudViewModel(
                                    DeviceBatteryObserver(application),
                                    TimeSynchronizer()
                                ) as T
                            }
                        }
                    }
                )
                HudScreen(viewModel = viewModel)
            }
        }
    }
}
