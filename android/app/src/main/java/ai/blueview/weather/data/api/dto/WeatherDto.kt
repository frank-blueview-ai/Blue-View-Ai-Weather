package ai.blueview.weather.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeocodingResponse(
    val results: List<GeocodingResult>? = null
)

@Serializable
data class GeocodingResult(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    @SerialName("country_code") val countryCode: String = "",
    val country: String = "",
    val admin1: String? = null
)

@Serializable
data class ForecastResponse(
    val current: CurrentDto,
    val hourly: HourlyDto,
    val daily: DailyDto
)

@Serializable
data class CurrentDto(
    @SerialName("temperature_2m")        val temperature: Double,
    @SerialName("apparent_temperature")  val apparentTemperature: Double,
    @SerialName("weather_code")          val weatherCode: Int,
    @SerialName("wind_speed_10m")        val windSpeed: Double,
    @SerialName("wind_direction_10m")    val windDirection: Double,
    @SerialName("relative_humidity_2m")  val humidity: Int,
    val visibility: Double,
    @SerialName("is_day")                val isDay: Int
)

@Serializable
data class HourlyDto(
    val time: List<String>,
    @SerialName("temperature_2m")              val temperature: List<Double>,
    @SerialName("weather_code")                val weatherCode: List<Int>,
    @SerialName("precipitation_probability")   val precipProb: List<Int?>,
    @SerialName("wind_speed_10m")              val windSpeed: List<Double>,
    @SerialName("is_day")                      val isDay: List<Int>
)

@Serializable
data class DailyDto(
    val time: List<String>,
    @SerialName("weather_code")                    val weatherCode: List<Int>,
    @SerialName("temperature_2m_max")              val tempMax: List<Double>,
    @SerialName("temperature_2m_min")              val tempMin: List<Double>,
    @SerialName("precipitation_probability_max")   val precipProbMax: List<Int?>
)
