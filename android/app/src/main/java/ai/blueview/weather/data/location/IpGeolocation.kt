package ai.blueview.weather.data.location

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

// ipapi.co is HTTPS and key-less. HTTPS is mandatory: the app manifest sets
// android:usesCleartextTraffic="false", so an http:// endpoint would be blocked.
private const val IPAPI_URL = "https://ipapi.co/json/"

@Serializable
private data class IpApiResponse(
    @SerialName("city")         val city: String?      = null,
    @SerialName("country_code") val countryCode: String? = null,
    @SerialName("latitude")     val latitude: Double?  = null,
    @SerialName("longitude")    val longitude: Double? = null,
    // ipapi.co answers 200 with {"error": true, "reason": "..."} on rate limits,
    // so a successful HTTP status alone is not proof of a usable payload.
    @SerialName("error")        val error: Boolean     = false
)

/**
 * Coarse city lookup from the caller's public IP. Used only as a fallback when
 * device location is denied or unavailable.
 */
@Singleton
class IpGeolocation @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** Returns the IP-derived city, or null on any failure. Never throws. */
    suspend fun locate(): LocatedCity? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(IPAPI_URL).build()
            val body = okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                response.body?.string()
            } ?: return@withContext null

            val dto = json.decodeFromString<IpApiResponse>(body)
            if (dto.error) return@withContext null

            val city = dto.city?.takeIf { it.isNotBlank() } ?: return@withContext null
            val lat  = dto.latitude  ?: return@withContext null
            val lon  = dto.longitude ?: return@withContext null

            LocatedCity(
                name    = city,
                country = dto.countryCode.orEmpty(),
                lat     = lat,
                lon     = lon
            )
        } catch (e: Exception) {
            null
        }
    }
}
