package ai.blueview.weather.data.update

import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

/**
 * play flavour: deliberately does nothing.
 *
 * Google Play's Device and Network Abuse policy forbids an app distributed through
 * Play from updating itself by any mechanism other than Play, and from directing
 * users to another distribution source for the same app. So the Play artifact
 * carries no release feed, no tag parsing, no download URL and no .apk handling —
 * Play is the updater.
 *
 * This exists only so the shared `main` code (Hilt's graph, AboutViewModel) keeps
 * compiling for both flavours: same package, same class name, same constructor,
 * same public API as the github-flavour checker. UpdateState is shared and lives
 * in `main`.
 */
@Singleton
class UpdateChecker @Inject constructor(
    // Unused here, but the constructor must match the github flavour so the same
    // Hilt binding satisfies both builds.
    @Suppress("unused") private val okHttpClient: OkHttpClient
) {
    /** Always UpToDate — the Play build never advertises an out-of-band update. */
    @Suppress("UNUSED_PARAMETER", "RedundantSuspendModifier")
    suspend fun checkLatest(currentVersion: String): UpdateState = UpdateState.UpToDate
}
