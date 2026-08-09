package ai.blueview.weather.data.repository

import ai.blueview.weather.data.api.GeocodingService
import ai.blueview.weather.data.api.WeatherService
import ai.blueview.weather.data.api.dto.ForecastResponse
import ai.blueview.weather.data.api.dto.GeocodingResult
import javax.inject.Inject
import javax.inject.Singleton

sealed class WeatherResult<out T> {
    data class Success<T>(val data: T) : WeatherResult<T>()
    data class Error(val message: String) : WeatherResult<Nothing>()
}

@Singleton
class WeatherRepository @Inject constructor(
    private val geocodingService: GeocodingService,
    private val weatherService: WeatherService
) {
    suspend fun geocode(city: String): WeatherResult<GeocodingResult> = runCatching {
        val response = geocodingService.search(city)
        response.results?.firstOrNull()
            ?: return WeatherResult.Error("City not found: $city")
    }.fold(
        onSuccess = { WeatherResult.Success(it) },
        onFailure = { WeatherResult.Error(it.message ?: "Network error") }
    )

    suspend fun forecast(
        lat: Double,
        lon: Double,
        units: String
    ): WeatherResult<ForecastResponse> = runCatching {
        weatherService.forecast(
            latitude  = lat,
            longitude = lon,
            tempUnit  = if (units == "imperial") "fahrenheit" else "celsius",
            windUnit  = if (units == "imperial") "mph" else "kmh"
        )
    }.fold(
        onSuccess = { WeatherResult.Success(it) },
        onFailure = { WeatherResult.Error(it.message ?: "Network error") }
    )
}
