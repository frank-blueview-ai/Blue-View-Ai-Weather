package ai.blueview.weather.data.api

import ai.blueview.weather.data.api.dto.ForecastResponse
import ai.blueview.weather.data.api.dto.GeocodingResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface GeocodingService {
    @GET("v1/search")
    suspend fun search(
        @Query("name")     name: String,
        @Query("count")    count: Int    = 1,
        @Query("language") language: String = "en",
        @Query("format")   format: String   = "json"
    ): GeocodingResponse
}

interface WeatherService {
    @GET("v1/forecast")
    suspend fun forecast(
        @Query("latitude")          latitude: Double,
        @Query("longitude")         longitude: Double,
        @Query("current")           current: String = CURRENT_PARAMS,
        @Query("hourly")            hourly: String  = HOURLY_PARAMS,
        @Query("daily")             daily: String   = DAILY_PARAMS,
        @Query("temperature_unit")  tempUnit: String,
        @Query("wind_speed_unit")   windUnit: String,
        @Query("timezone")          timezone: String = "auto",
        @Query("forecast_days")     days: Int = 7
    ): ForecastResponse

    companion object {
        const val CURRENT_PARAMS = "temperature_2m,apparent_temperature,weather_code," +
                "wind_speed_10m,wind_direction_10m,relative_humidity_2m,visibility,is_day"
        const val HOURLY_PARAMS  = "temperature_2m,precipitation_probability," +
                "weather_code,wind_speed_10m,is_day"
        const val DAILY_PARAMS   = "weather_code,temperature_2m_max,temperature_2m_min," +
                "precipitation_probability_max"
    }
}
