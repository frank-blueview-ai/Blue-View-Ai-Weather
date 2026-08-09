package ai.blueview.weather.di

import ai.blueview.weather.data.api.GeocodingService
import ai.blueview.weather.data.api.WeatherService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues  = true
    }

    @Provides @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", "BlueViewWeather/1.0.0 Android")
                    .build()
            )
        }
        .build()

    @Provides @Singleton @Named("geocoding")
    fun provideGeocodingRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://geocoding-api.open-meteo.com/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides @Singleton @Named("weather")
    fun provideWeatherRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides @Singleton
    fun provideGeocodingService(@Named("geocoding") retrofit: Retrofit): GeocodingService =
        retrofit.create(GeocodingService::class.java)

    @Provides @Singleton
    fun provideWeatherService(@Named("weather") retrofit: Retrofit): WeatherService =
        retrofit.create(WeatherService::class.java)
}
