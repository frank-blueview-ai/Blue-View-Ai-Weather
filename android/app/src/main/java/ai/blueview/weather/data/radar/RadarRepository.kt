package ai.blueview.weather.data.radar

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

private const val RAINVIEWER_API = "https://api.rainviewer.com/public/weather-maps.json"

@Serializable
private data class RainViewerResponse(
    val host: String,
    val radar: RadarData
)

@Serializable
private data class RadarData(
    val past: List<RadarFrame>
)

@Serializable
private data class RadarFrame(
    val time: Long,
    val path: String
)

@Singleton
class RadarRepository @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** Returns a Leaflet tile URL template, or null on failure. */
    suspend fun latestTileUrl(): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(RAINVIEWER_API).build()
            val body = okHttpClient.newCall(request).execute().use { it.body?.string() }
                ?: return@withContext null
            val rv = json.decodeFromString<RainViewerResponse>(body)
            val last = rv.radar.past.lastOrNull() ?: return@withContext null
            "${rv.host}${last.path}/256/{z}/{x}/{y}/4/1_1.png"
        } catch (e: Exception) {
            null
        }
    }
}
