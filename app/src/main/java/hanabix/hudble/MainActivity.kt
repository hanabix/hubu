package hanabix.hudble

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import hanabix.hudble.data.Clock
import hanabix.hudble.data.HostBatteryObserver
import hanabix.hudble.ui.HUDScreen

class MainActivity : ComponentActivity() {

    private val hostBatteryObserver by lazy { HostBatteryObserver(application) }
    private val clock by lazy { Clock() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val batteryLevel by remember { hostBatteryObserver.observe() }
                .collectAsState(initial = "")
            val currentTime by remember { clock.now() }
                .collectAsState(initial = "")

            HUDScreen(
                pace = "6‘12\"",
                heartRate = "152",
                cadence = "178",
                deviceName = "Enduro 2",
                currentTime = currentTime,
                batteryLevel = batteryLevel,
            )
        }
    }
}
