package ai.blueview.weather.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    displayLarge  = TextStyle(fontWeight = FontWeight.Light,   fontSize = 72.sp, letterSpacing = (-2).sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold,    fontSize = 24.sp),
    headlineMedium= TextStyle(fontWeight = FontWeight.SemiBold,fontSize = 18.sp),
    titleLarge    = TextStyle(fontWeight = FontWeight.Bold,    fontSize = 16.sp, letterSpacing = 0.5.sp),
    titleMedium   = TextStyle(fontWeight = FontWeight.SemiBold,fontSize = 14.sp),
    bodyLarge     = TextStyle(fontWeight = FontWeight.Normal,  fontSize = 14.sp),
    bodyMedium    = TextStyle(fontWeight = FontWeight.Normal,  fontSize = 12.sp),
    labelSmall    = TextStyle(fontWeight = FontWeight.Bold,    fontSize = 10.sp, letterSpacing = 1.sp),
)
