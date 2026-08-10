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

// github flavour only. The Play artifact must not contain any of this: Play's
// Device and Network Abuse policy forbids an app it distributes from updating
// itself outside Play or pointing users at another download of the same app.
// See src/play/.../UpdateChecker.kt for the stub that replaces it.

// The monorepo releases all three platforms, each on its own tag prefix, so
// /releases/latest is not usable here — it could return an ios- or desktop- tag.
// Fetch the list (newest first) and pick the newest android- release.
private const val RELEASES_API =
    "https://api.github.com/repos/frank-blueview-ai/Blue-View-Ai-Weather/releases?per_page=30"

private const val TAG_PREFIX = "android-v"

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
            val release = json.decodeFromString<List<GithubRelease>>(body)
                .firstOrNull { it.tagName.startsWith(TAG_PREFIX) }
                ?: return@withContext UpdateState.Error("No Android release found")
            val latest  = release.tagName.removePrefix(TAG_PREFIX)
            val current = currentVersion.trimStart('v')
            if (isNewer(latest, current)) {
                val apk = release.assets.firstOrNull { it.name.endsWith(".apk") }
                    ?: return@withContext UpdateState.Error("No APK found in release")
                UpdateState.Available(latest, apk.downloadUrl)
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
