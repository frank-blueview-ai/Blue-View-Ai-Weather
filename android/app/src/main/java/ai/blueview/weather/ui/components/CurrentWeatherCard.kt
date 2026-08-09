package ai.blueview.weather.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.blueview.weather.data.api.dto.CurrentDto
import ai.blueview.weather.data.api.dto.DailyDto
import ai.blueview.weather.ui.theme.*
import ai.blueview.weather.util.*

@Composable
fun CurrentWeatherCard(
    city: String,
    current: CurrentDto,
    daily: DailyDto,
    units: String,
    modifier: Modifier = Modifier
) {
    val sym    = if (units == "imperial") "°F" else "°C"
    val spdU   = if (units == "imperial") "mph" else "km/h"
    val isDay  = current.isDay == 1
    val hiLo   = if (daily.time.isNotEmpty())
        "↑ ${daily.tempMax[0].toInt()}  ↓ ${daily.tempMin[0].toInt()}  $sym" else ""

    Column(modifier = modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        // City
        Text(
            text  = city,
            style = MaterialTheme.typography.titleMedium,
            color = BlueAccent
        )
        Spacer(Modifier.height(12.dp))

        // Icon + Temperature row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text     = wmoIcon(current.weatherCode, isDay),
                fontSize = 64.sp
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text  = "${current.temperature.toInt()}$sym",
                    style = MaterialTheme.typography.displayLarge,
                    color = TextPrimary
                )
                Text(
                    text  = wmoDescription(current.weatherCode),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )
                if (hiLo.isNotBlank()) {
                    Text(
                        text  = hiLo,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TempHigh,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Details row
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            DetailChip("Feels ${current.apparentTemperature.toInt()}$sym")
            DetailChip("💧 ${current.humidity}%")
            DetailChip("💨 ${current.windSpeed.toInt()} $spdU ${windDirLabel(current.windDirection)}")
            DetailChip("👁 ${(current.visibility / 1000).toInt()} km")
        }
    }
}

@Composable
private fun DetailChip(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
}
