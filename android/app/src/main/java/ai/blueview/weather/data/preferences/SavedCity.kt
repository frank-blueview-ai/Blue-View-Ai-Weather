package ai.blueview.weather.data.preferences

import kotlinx.serialization.Serializable
import java.util.Locale

/**
 * A city the user has saved. Persisted inside the single JSON blob held by
 * [UserPreferencesRepository]; see Keys.SAVED_CITIES.
 */
@Serializable
data class SavedCity(
    val name: String,
    val country: String,
    val lat: Double,
    val lon: Double
) {
    /**
     * Identity is the rounded coordinate pair, not the name: geocoders return
     * slightly different names/casing for the same place, so coordinates are the
     * only stable key. 4 decimals is ~11 m — close enough that two saves of the
     * same place collapse, far enough that neighbouring towns stay distinct.
     */
    val id: String
        get() = "%.4f,%.4f".format(Locale.US, lat, lon)

    /** "Lynn, US" — falls back to the bare name when the geocoder gave no country. */
    val displayLabel: String
        get() = if (country.isBlank()) name else "$name, $country"

    companion object {
        /** Same rounding rule as [id], for callers holding raw coordinates. */
        fun idOf(lat: Double, lon: Double): String = "%.4f,%.4f".format(Locale.US, lat, lon)
    }
}
