package id.icantartil.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Green = Color(0xFF0E3B2C)
val DeepGreen = Color(0xFF0A2E22)
val Gold = Color(0xFFD9B45A)
val LightGold = Color(0xFFF0D78C)
val Tint = Color(0xFFFFF3D0)
val Ink = Color(0xFF1B3A2D)
val Mist = Color(0xFFC9D8CF)

@Composable
fun IcanTartilTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(primary = Gold, onPrimary = DeepGreen, background = Green, surface = Green),
        content = content,
    )
}
