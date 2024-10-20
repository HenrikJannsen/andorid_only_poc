package bisq.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import bisq.mobile.ui.theme.MobileTheme
import lombok.extern.slf4j.Slf4j
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

@Slf4j
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val userDataDir = getFilesDir().getAbsolutePath()
        System.setProperty("bisq.userDataDir", userDataDir)

        var androidApp = AndroidApp()

        setContent {
            MobileTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Display(
                        info = androidApp.getInfo(),
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Display(info: String, modifier: Modifier = Modifier) {
    Text(
        text = info,
        modifier = modifier
    )
}
