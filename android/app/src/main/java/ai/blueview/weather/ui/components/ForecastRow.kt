package ai.blueview.weather.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import ai.blueview.weather.data.api.dto.DailyDto
import ai.blueview.weather.ui.theme.*
import ai.blueview.weather.util.wmoIcon
import java.time.LocalDate
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

@Composable
fun ForecastRow(
    daily: DailyDto,
    units: String,
    selectedDate: String?,
    onDayClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sym   = if (units == "imperial") "°F" else "°C"
    val today = LocalDate.now().toString()

    LazyRow(
        modifier            = modifier.fillMaxWidth(),
        contentPadding      = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(daily.time.indices.toList()) { i ->
            val date     = daily.time[i]
            val selected = date == selectedDate
            val dayLabel = if (date == today) "Today"
            else LocalDate.parse(date).dayOfWeek
                .getDisplayName(JTextStyle.SHORT, Locale.getDefault())

            DayCard(
                day      = dayLabel,
                icon     = wmoIcon(daily.weatherCode[i], isDay = true),
                hi       = "${daily.tempMax[i].toInt()}$sym",
                lo       = "${daily.tempMin[i].toInt()}$sym",
                pop      = daily.precipProbMax.getOrNull(i) ?: 0,
                selected = selected,
                onClick  = { onDayClick(date) }
            )
        }
    }
}

@Composable
private fun DayCard(
    day: String, icon: String, hi: String, lo: String,
    pop: Int, selected: Boolean, onClick: () -> Unit
) {
    val shape  = RoundedCornerShape(10.dp)
    val bgColor = if (selected) BlueAccent.copy(alpha = 0.15f)
                  else NavyCard
    val border  = if (selected) BlueAccent.copy(alpha = 0.5f)
                  else NavyBorder

    Column(
        modifier = Modifier
            .width(60.dp)
            .clip(shape)
            .background(bgColor)
            .border(1.dp, border, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(day,  style = MaterialTheme.typography.labelSmall, color = TextSecondary,
            textAlign = TextAlign.Center, maxLines = 1)
        Text(icon, fontSize = 26.sp, textAlign = TextAlign.Center)
        Text(hi,   style = MaterialTheme.typography.bodyMedium, color = TempHigh,
            fontWeight = FontWeight.Bold)
        Text(lo,   style = MaterialTheme.typography.bodyMedium, color = TempLow)
        if (pop >= 10)
            Text("💧$pop%", style = MaterialTheme.typography.labelSmall, color = RainBlue)
    }
}
