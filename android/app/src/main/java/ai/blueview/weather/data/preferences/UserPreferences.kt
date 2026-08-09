package ai.blueview.weather.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

data class UserPrefs(
    val city: String   = "",
    val units: String  = "metric",   // "metric" | "imperial"
    val lat: Double    = 0.0,
    val lon: Double    = 0.0,
    val cityLabel: String = ""
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val CITY       = stringPreferencesKey("city")
        val UNITS      = stringPreferencesKey("units")
        val LAT        = doublePreferencesKey("lat")
        val LON        = doublePreferencesKey("lon")
        val CITY_LABEL = stringPreferencesKey("city_label")
    }

    val prefs: Flow<UserPrefs> = context.dataStore.data.map { p ->
        UserPrefs(
            city      = p[Keys.CITY]       ?: "",
            units     = p[Keys.UNITS]      ?: "metric",
            lat       = p[Keys.LAT]        ?: 0.0,
            lon       = p[Keys.LON]        ?: 0.0,
            cityLabel = p[Keys.CITY_LABEL] ?: ""
        )
    }

    suspend fun saveCity(city: String, label: String, lat: Double, lon: Double) {
        context.dataStore.edit { p ->
            p[Keys.CITY]       = city
            p[Keys.CITY_LABEL] = label
            p[Keys.LAT]        = lat
            p[Keys.LON]        = lon
        }
    }

    suspend fun saveUnits(units: String) {
        context.dataStore.edit { p -> p[Keys.UNITS] = units }
    }
}
