package ai.blueview.weather.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.blueview.weather.data.api.dto.HourlyDto
import ai.blueview.weather.ui.theme.*
import ai.blueview.weather.util.wmoIcon
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun HourlyRow(
    hourly: HourlyDto,
    selectedDate: String,
    units: String,
    modifier: Modifier = Modifier
) {
    val sym    = if (units == "imperial") "°F" else "°C"
    val slots  = hourly.time.indices.filter { hourly.time[it].startsWith(selectedDate) }
    val shape  = RoundedCornerShape(8.dp)
    val fmt    = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
    val timeFmt = DateTimeFormatter.ofPattern("h a")

    LazyRow(
        modifier              = modifier.fillMaxWidth(),
        contentPadding        = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(slots) { i ->
            val timeLabel = try {
                LocalDateTime.parse(hourly.time[i], fmt).format(timeFmt)
            } catch (e: Exception) { hourly.time[i].takeLast(5) }
            val pop = hourly.precipProb.getOrNull(i) ?: 0

            Column(
                modifier = Modifier
                    .width(56.dp)
                    .clip(shape)
                    .background(NavyCard)
                    .border(1.dp, NavyBorder, shape)
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(timeLabel, style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary, textAlign = TextAlign.Center)
                Text(wmoIcon(hourly.weatherCode[i], hourly.isDay.getOrElse(i) { 1 } == 1),
                    fontSize = 22.sp, textAlign = TextAlign.Center)
                Text("${hourly.temperature[i].toInt()}$sym",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold, color = TextPrimary)
                if ((pop ?: 0) >= 10)
                    Text("💧${pop}%", style = MaterialTheme.typography.labelSmall, color = RainBlue)
            }
        }
    }
}
