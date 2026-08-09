package ai.blueview.weather.data.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

private const val RELEASES_API =
    "https://api.github.com/repos/frank-blueview-ai/Blue-View-Ai-Weather-Android/releases/latest"

@Serializable
data class GithubRelease(
    @SerialName("tag_name") val tagName: String,
    @SerialName("assets")   val assets: List<GithubAsset>
)

@Serializable
data class GithubAsset(
    @SerialName("name")                 val name: String,
    @SerialName("browser_download_url") val downloadUrl: String
)

sealed class UpdateState {
    object Idle                                                 : UpdateState()
    object Checking                                             : UpdateState()
    object UpToDate                                             : UpdateState()
    data class Available(val version: String, val url: String)  : UpdateState()
    data class Error(val message: String)                       : UpdateState()
}

@Singleton
class UpdateChecker @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun checkLatest(currentVersion: String): UpdateState = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(RELEASES_API)
                .header("Accept", "application/vnd.github+json")
                .build()
            val body = okHttpClient.newCall(request).execute().use { it.body?.string() }
                ?: return@withContext UpdateState.Error("Empty response from server")
            val release = json.decodeFromString<GithubRelease>(body)
            val latest  = release.tagName.trimStart('v')
            val current = currentVersion.trimStart('v')
            if (isNewer(latest, current)) {
                val apk = release.assets.firstOrNull { it.name.endsWith(".apk") }
                    ?: return@withContext UpdateState.Error("No APK found in release")
                UpdateState.Available(release.tagName, apk.downloadUrl)
            } else {
                UpdateState.UpToDate
            }
        } catch (e: Exception) {
            UpdateState.Error(e.message ?: "Check failed")
        }
    }

    private fun isNewer(latest: String, current: String): Boolean {
        val l = latest.split(".").mapNotNull { it.toIntOrNull() }
        val c = current.split(".").mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(l.size, c.size)) {
            val lv = l.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (lv != cv) return lv > cv
        }
        return false
    }
}
