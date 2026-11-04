package id.icantartil.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import id.icantartil.app.ui.AppRoot
import id.icantartil.app.ui.IcanTartilTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { IcanTartilTheme { AppRoot() } }
    }
}
