package ai.blueview.weather.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary          = BlueAccent,
    onPrimary        = NavyDeep,
    primaryContainer = NavyCard,
    background       = NavyDeep,
    surface          = NavyMid,
    surfaceVariant   = NavyCard,
    onBackground     = TextPrimary,
    onSurface        = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline          = TextMuted,
    error            = ErrorRed,
)

@Composable
fun BlueViewWeatherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = Typography,
        content     = content
    )
}
