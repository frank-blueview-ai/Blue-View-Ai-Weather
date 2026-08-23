package ai.blueview.weather.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.blueview.weather.data.api.dto.CurrentDto
import ai.blueview.weather.data.api.dto.DailyDto
import ai.blueview.weather.data.preferences.ClockFormat
import ai.blueview.weather.ui.theme.*
import ai.blueview.weather.util.*

@Composable
fun CurrentWeatherCard(
    current: CurrentDto,
    daily: DailyDto,
    units: String,
    timezone: String,
    utcOffsetSeconds: Int,
    clockFormat: ClockFormat,
    modifier: Modifier = Modifier
) {
    val sym    = if (units == "imperial") "°F" else "°C"
    val spdU   = if (units == "imperial") "mph" else "km/h"
    val isDay  = current.isDay == 1
    val hiLo   = if (daily.time.isNotEmpty())
        "↑ ${daily.tempMax[0].toInt()}  ↓ ${daily.tempMin[0].toInt()}  $sym" else ""

    // The time where the CITY is, which is the whole point when the user is looking
    // at a city in another timezone. Re-reads every 20s so it ticks over the minute
    // without waking the UI once a second.
    val zone     = remember(timezone, utcOffsetSeconds) {
        CityClock.zoneOf(timezone, utcOffsetSeconds)
    }
    val localTime by produceState(
        initialValue = CityClock.nowIn(zone, clockFormat),
        zone, clockFormat
    ) {
        while (true) {
            value = CityClock.nowIn(zone, clockFormat)
            delay(20_000)
        }
    }

    Column(modifier = modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        // The active city is named by the switcher in the top bar, so repeating
        // it here just duplicated it on screen.
        Text(
            text  = localTime,
            style = MaterialTheme.typography.titleMedium,
            color = TextSecondary
        )
        Spacer(Modifier.height(8.dp))
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
