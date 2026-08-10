package ai.blueview.weather.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

/** Whether the active city tracks the device location or is a city the user pinned. */
enum class LocationMode { AUTOMATIC, PINNED }

data class UserPrefs(
    // Legacy single-city fields, still populated so existing collectors keep working.
    val city: String              = "",
    val units: String             = "imperial", // "metric" | "imperial"
    val lat: Double               = 0.0,
    val lon: Double               = 0.0,
    val cityLabel: String         = "",
    // Multi-city model.
    val savedCities: List<SavedCity> = emptyList(),
    val locationMode: LocationMode   = LocationMode.AUTOMATIC,
    val pinnedCityId: String?        = null
) {
    /** The saved city [pinnedCityId] points at, or null if nothing is pinned/found. */
    val pinnedCity: SavedCity?
        get() = pinnedCityId?.let { id -> savedCities.firstOrNull { it.id == id } }

    /** Best non-location answer: the pinned city, else the first saved city. */
    val fallbackCity: SavedCity?
        get() = pinnedCity ?: savedCities.firstOrNull()
}

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val CITY            = stringPreferencesKey("city")
        val UNITS           = stringPreferencesKey("units")
        val LAT             = doublePreferencesKey("lat")
        val LON             = doublePreferencesKey("lon")
        val CITY_LABEL      = stringPreferencesKey("city_label")
        val SAVED_CITIES    = stringPreferencesKey("saved_cities_json")
        val LOCATION_MODE   = stringPreferencesKey("location_mode")
        val PINNED_CITY_ID  = stringPreferencesKey("pinned_city_id")
        // Set once the legacy city has been copied into SAVED_CITIES, so a city the
        // user later deletes is not resurrected by the migration fallback below.
        val CITIES_MIGRATED = booleanPreferencesKey("cities_migrated")
        // Whether the location permission has ever been requested. Without this the
        // ViewModel re-asks on every cold start after a denial, and on API 26-29 the
        // system re-renders the dialog each launch instead of auto-denying.
        val LOCATION_ASKED  = booleanPreferencesKey("location_permission_asked")
    }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val prefs: Flow<UserPrefs> = context.dataStore.data
        // A corrupt/unreadable store must degrade to defaults, never kill the collector.
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { p ->
            val stored   = decodeCities(p[Keys.SAVED_CITIES])
            val migrated = p[Keys.CITIES_MIGRATED] ?: false
            val legacy   = if (migrated) null else legacyCity(p)

            // Derive the migrated view on read so the user's city is present even
            // before migrateLegacyCityIfNeeded() has had a chance to persist it.
            val cities = if (stored.isEmpty() && legacy != null) listOf(legacy) else stored
            val pinned = p[Keys.PINNED_CITY_ID]
                ?.takeIf { id -> cities.any { it.id == id } }
                ?: legacy?.id?.takeIf { id -> cities.any { it.id == id } }

            UserPrefs(
                city         = p[Keys.CITY]       ?: "",
                units        = p[Keys.UNITS]      ?: "imperial",
                lat          = p[Keys.LAT]        ?: 0.0,
                lon          = p[Keys.LON]        ?: 0.0,
                cityLabel    = p[Keys.CITY_LABEL] ?: "",
                savedCities  = cities,
                locationMode = parseMode(p[Keys.LOCATION_MODE]),
                pinnedCityId = pinned
            )
        }

    /**
     * Copies the pre-multi-city install's single city into the saved list and pins it.
     * Idempotent; safe to call on every app start.
     */
    suspend fun migrateLegacyCityIfNeeded() {
        context.dataStore.edit { p ->
            if (p[Keys.CITIES_MIGRATED] == true) return@edit
            val stored = decodeCities(p[Keys.SAVED_CITIES])
            val legacy = legacyCity(p)
            if (stored.isEmpty() && legacy != null) {
                p[Keys.SAVED_CITIES]   = json.encodeToString(listOf(legacy))
                p[Keys.PINNED_CITY_ID] = legacy.id
                // Pin the mode too, not just the id. The legacy schema had no mode, so
                // an unset key reads as AUTOMATIC — which would let device/IP location
                // silently override the city this user had explicitly chosen. AUTOMATIC
                // stays the default for genuinely new installs, which have no legacy city.
                if (p[Keys.LOCATION_MODE] == null) {
                    p[Keys.LOCATION_MODE] = LocationMode.PINNED.name
                }
            }
            p[Keys.CITIES_MIGRATED] = true
        }
    }

    suspend fun addCity(cityToAdd: SavedCity) {
        context.dataStore.edit { p ->
            val current = decodeCities(p[Keys.SAVED_CITIES])
                .ifEmpty { if (p[Keys.CITIES_MIGRATED] == true) emptyList() else listOfNotNull(legacyCity(p)) }
            // Idempotent on id: re-adding refreshes the stored name/country in place.
            val merged = if (current.any { it.id == cityToAdd.id }) {
                current.map { if (it.id == cityToAdd.id) cityToAdd else it }
            } else {
                current + cityToAdd
            }
            p[Keys.SAVED_CITIES]    = json.encodeToString(merged)
            p[Keys.CITIES_MIGRATED] = true
            if (p[Keys.PINNED_CITY_ID] == null) p[Keys.PINNED_CITY_ID] = cityToAdd.id
        }
    }

    /**
     * Add a city, pin it, and switch to PINNED in ONE edit.
     *
     * Doing this as three separate calls emits three times from the prefs Flow, and
     * each emission starts its own location lookup and forecast fetch — measured as
     * three requests fired for a single city search, two of which were immediately
     * cancelled. One atomic write means one emission.
     */
    suspend fun addAndPinCity(cityToAdd: SavedCity) {
        context.dataStore.edit { p ->
            val current = decodeCities(p[Keys.SAVED_CITIES])
                .ifEmpty { if (p[Keys.CITIES_MIGRATED] == true) emptyList() else listOfNotNull(legacyCity(p)) }
            val merged = if (current.any { it.id == cityToAdd.id }) {
                current.map { if (it.id == cityToAdd.id) cityToAdd else it }
            } else {
                current + cityToAdd
            }
            p[Keys.SAVED_CITIES]    = json.encodeToString(merged)
            p[Keys.CITIES_MIGRATED] = true
            p[Keys.PINNED_CITY_ID]  = cityToAdd.id
            p[Keys.LOCATION_MODE]   = LocationMode.PINNED.name
        }
    }

    suspend fun removeCity(id: String) {
        context.dataStore.edit { p ->
            val current = decodeCities(p[Keys.SAVED_CITIES])
                .ifEmpty { if (p[Keys.CITIES_MIGRATED] == true) emptyList() else listOfNotNull(legacyCity(p)) }
            val remaining = current.filterNot { it.id == id }
            p[Keys.SAVED_CITIES]    = json.encodeToString(remaining)
            p[Keys.CITIES_MIGRATED] = true
            if (p[Keys.PINNED_CITY_ID] == id) {
                val next = remaining.firstOrNull()
                if (next != null) {
                    p[Keys.PINNED_CITY_ID] = next.id
                } else {
                    p.remove(Keys.PINNED_CITY_ID)
                    // Nothing left to pin, so PINNED mode would have no target.
                    p[Keys.LOCATION_MODE] = LocationMode.AUTOMATIC.name
                }
            }
        }
    }

    suspend fun setLocationMode(mode: LocationMode) {
        context.dataStore.edit { p -> p[Keys.LOCATION_MODE] = mode.name }
    }

    suspend fun setPinnedCity(id: String) {
        context.dataStore.edit { p -> p[Keys.PINNED_CITY_ID] = id }
    }

    /** Pin a saved city and switch to PINNED in one edit — see [addAndPinCity]. */
    suspend fun pinCityAndSwitch(id: String) {
        context.dataStore.edit { p ->
            p[Keys.PINNED_CITY_ID] = id
            p[Keys.LOCATION_MODE]  = LocationMode.PINNED.name
        }
    }

    suspend fun saveCity(city: String, label: String, lat: Double, lon: Double) {
        context.dataStore.edit { p ->
            p[Keys.CITY]       = city
            p[Keys.CITY_LABEL] = label
            p[Keys.LAT]        = lat
            p[Keys.LON]        = lon
        }
    }

    /** True once the location permission has been requested at least once. */
    val locationPermissionAsked: Flow<Boolean> =
        context.dataStore.data
            .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .map { it[Keys.LOCATION_ASKED] ?: false }

    suspend fun markLocationPermissionAsked() {
        context.dataStore.edit { p -> p[Keys.LOCATION_ASKED] = true }
    }

    suspend fun saveUnits(units: String) {
        context.dataStore.edit { p -> p[Keys.UNITS] = units }
    }

    private fun decodeCities(raw: String?): List<SavedCity> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<SavedCity>>(raw)
        } catch (e: Exception) {
            emptyList()   // malformed blob must not crash the app or the Flow
        }
    }

    private fun parseMode(raw: String?): LocationMode =
        LocationMode.entries.firstOrNull { it.name == raw } ?: LocationMode.AUTOMATIC

    /** Reconstructs a SavedCity from the pre-migration keys, if one was ever stored. */
    private fun legacyCity(p: Preferences): SavedCity? {
        val lat = p[Keys.LAT] ?: return null
        val lon = p[Keys.LON] ?: return null
        val label = p[Keys.CITY_LABEL]?.takeIf { it.isNotBlank() }
            ?: p[Keys.CITY]?.takeIf { it.isNotBlank() }
            ?: return null
        if (lat == 0.0 && lon == 0.0) return null   // 0,0 is the "never set" default
        val name    = label.substringBefore(",").trim()
        val country = label.substringAfter(",", "").trim()
        return SavedCity(name = name, country = country, lat = lat, lon = lon)
    }
}
