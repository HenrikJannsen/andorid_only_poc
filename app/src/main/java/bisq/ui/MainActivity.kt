package bisq.ui

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bisq.app.AndroidApp
import bisq.ui.ui.theme.MobileTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import lombok.extern.slf4j.Slf4j


@Slf4j
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val userDataDir = getFilesDir().getAbsolutePath()
        var androidApp = AndroidApp(
            userDataDir,
            isRunningInAndroidEmulator()
        )

        // map observable log message from androidApp to our UI
        var logViewModel = LogViewModel()
        androidApp.logMessage.addObserver { info ->
            logViewModel.update(info)
        }

        setContent {
            MobileTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Display(
                        logViewModel = logViewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    fun isRunningInAndroidEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("google/sdk_gphone_") ||
                Build.FINGERPRINT.contains("generic") ||
                Build.MODEL.contains("Emulator") ||
                Build.MANUFACTURER.contains("Google") ||
                Build.PRODUCT.contains("sdk_gphone") ||
                Build.HARDWARE == "goldfish" ||
                Build.HARDWARE == "ranchu")
    }
}

class LogViewModel : ViewModel() {
    private val _value = MutableStateFlow("N/A")
    val value: StateFlow<String> = _value
    fun update(newValue: String) {
        viewModelScope.launch {
            _value.value = newValue
        }
    }
}

@Composable
fun Display(logViewModel: LogViewModel, modifier: Modifier = Modifier) {
    val value by logViewModel.value.collectAsState()
    Text(
        text = value,
        modifier = modifier
    )
}
