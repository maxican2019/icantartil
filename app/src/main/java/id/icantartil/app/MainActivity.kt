package id.icantartil.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import id.icantartil.app.ui.AppRoot
import id.icantartil.app.ui.IcanTartilTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Target API 35+ memaksa tampilan penuh layar; ikon bilah sistem dibuat terang di atas hijau tua.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent { IcanTartilTheme { AppRoot() } }
    }
}
