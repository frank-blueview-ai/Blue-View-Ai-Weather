package ai.blueview.weather.util

data class WeatherInfo(val dayIcon: String, val nightIcon: String, val description: String)

private val wmoMap = mapOf(
    0  to WeatherInfo("☀️", "🌙", "Clear Sky"),
    1  to WeatherInfo("🌤️", "🌙", "Mainly Clear"),
    2  to WeatherInfo("⛅", "⛅",  "Partly Cloudy"),
    3  to WeatherInfo("☁️", "☁️", "Overcast"),
    45 to WeatherInfo("🌫️", "🌫️", "Fog"),
    48 to WeatherInfo("🌫️", "🌫️", "Freezing Fog"),
    51 to WeatherInfo("🌦️", "🌧️", "Light Drizzle"),
    53 to WeatherInfo("🌦️", "🌧️", "Drizzle"),
    55 to WeatherInfo("🌧️", "🌧️", "Heavy Drizzle"),
    61 to WeatherInfo("🌧️", "🌧️", "Light Rain"),
    63 to WeatherInfo("🌧️", "🌧️", "Rain"),
    65 to WeatherInfo("🌧️", "🌧️", "Heavy Rain"),
    66 to WeatherInfo("🌧️", "🌧️", "Light Freezing Rain"),
    67 to WeatherInfo("🌧️", "🌧️", "Freezing Rain"),
    71 to WeatherInfo("❄️", "❄️", "Light Snow"),
    73 to WeatherInfo("❄️", "❄️", "Snow"),
    75 to WeatherInfo("❄️", "❄️", "Heavy Snow"),
    77 to WeatherInfo("❄️", "❄️", "Snow Grains"),
    80 to WeatherInfo("🌦️", "🌧️", "Light Showers"),
    81 to WeatherInfo("🌦️", "🌧️", "Showers"),
    82 to WeatherInfo("🌧️", "🌧️", "Heavy Showers"),
    85 to WeatherInfo("❄️", "❄️", "Snow Showers"),
    86 to WeatherInfo("❄️", "❄️", "Heavy Snow Showers"),
    95 to WeatherInfo("⛈️", "⛈️", "Thunderstorm"),
    96 to WeatherInfo("⛈️", "⛈️", "Thunderstorm + Hail"),
    99 to WeatherInfo("⛈️", "⛈️", "Thunderstorm + Heavy Hail"),
)

fun wmoIcon(code: Int, isDay: Boolean): String {
    val info = wmoMap[code] ?: wmoMap[3]!!
    return if (isDay) info.dayIcon else info.nightIcon
}

fun wmoDescription(code: Int): String = wmoMap[code]?.description ?: "Unknown"

val windDirections = listOf("N","NE","E","SE","S","SW","W","NW")
fun windDirLabel(degrees: Double): String = windDirections[((degrees + 22.5) / 45).toInt() % 8]
