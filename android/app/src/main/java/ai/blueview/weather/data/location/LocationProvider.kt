package ai.blueview.weather.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/** A resolved place: display name plus the coordinates the forecast is fetched for. */
data class LocatedCity(
    val name: String,
    val country: String,
    val lat: Double,
    val lon: Double
)

// A fix must arrive within this budget or we give up and fall back to IP. GPS can
// take minutes with a cold almanac, and the user is staring at a spinner.
private const val FIX_TIMEOUT_MS = 10_000L

// Reverse geocoding hits a remote backend on most devices; cap it well under the
// fix budget so the two together stay inside a tolerable wait.
private const val GEOCODE_TIMEOUT_MS = 5_000L

// Android keeps the last fix indefinitely — across reboots — so a cached location
// says nothing about where the user is now. Accept one only while it is this fresh;
// anything older is worth the cost of a live fix.
private const val MAX_CACHED_FIX_AGE_MS = 5 * 60 * 1000L

// Coordinates are sent to a third-party forecast API as URL query parameters, where
// they persist in access logs. 4 decimal places (~11 m) is the precision SavedCity
// already treats as a distinguishable place, and far more than a city forecast needs,
// so exact GPS never leaves the device.
private const val COORD_SCALE = 10_000.0

private fun Double.coarsened(): Double = Math.round(this * COORD_SCALE) / COORD_SCALE

/**
 * Resolves the user's current city: device location first, IP geolocation as the
 * fallback when permission is denied, no fix is available, or geocoding fails.
 *
 * Deliberately built on the platform [LocationManager] rather than Play Services
 * so the app carries no Google dependency.
 */
@Singleton
class LocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ipGeolocation: IpGeolocation
) {
    private val locationManager: LocationManager? =
        ContextCompat.getSystemService(context, LocationManager::class.java)

    /** Best-effort current city. Main-safe; returns null only if every path fails. */
    suspend fun currentCity(): LocatedCity? {
        if (hasLocationPermission()) {
            val fix = suspendOrNull { deviceLocation() }
            if (fix != null) {
                val city = suspendOrNull { reverseGeocode(fix) }
                if (city != null) return city
            }
        }
        return ipGeolocation.locate()
    }

    /**
     * runCatching for suspend calls. CancellationException must propagate — swallowing
     * it would let a cancelled caller keep running through the fallback chain.
     */
    private suspend fun <T> suspendOrNull(block: suspend () -> T): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    /** True when either coarse or fine location has been granted. */
    fun hasLocationPermission(): Boolean = hasCoarse() || hasFine()

    private fun hasCoarse(): Boolean = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    private fun hasFine(): Boolean = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    // ---- device location -------------------------------------------------------

    private suspend fun deviceLocation(): Location? {
        val manager   = locationManager ?: return null
        val providers = usableProviders(manager)
        if (providers.isEmpty()) return null

        val cached = lastKnown(manager, providers)
        if (cached != null && cached.ageMs() <= MAX_CACHED_FIX_AGE_MS) return cached

        // Nothing recent enough — pay for a live fix, but only once and only briefly.
        val live = withTimeoutOrNull(FIX_TIMEOUT_MS) {
            providers.firstNotNullOfOrNull { provider ->
                suspendOrNull { singleUpdate(manager, provider) }
            }
        }

        // A stale fix still beats showing the user no location at all.
        return live ?: cached
    }

    /** Age from the monotonic clock; [Location.time] is wall clock and skews with NTP. */
    private fun Location.ageMs(): Long =
        (SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos) / 1_000_000L

    /**
     * Enabled providers, cheapest first. GPS and passive need FINE; with only
     * COARSE granted, requesting them throws SecurityException.
     */
    private fun usableProviders(manager: LocationManager): List<String> {
        val candidates = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
            if (hasFine()) add(LocationManager.GPS_PROVIDER)
        }
        return candidates.filter { provider ->
            runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false)
        }
    }

    /** Freshest cached fix across [providers], or null if every one is empty. */
    private fun lastKnown(manager: LocationManager, providers: List<String>): Location? =
        providers.mapNotNull { provider ->
            runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
        }.maxByOrNull { it.elapsedRealtimeNanos }

    /**
     * One fix from [provider]. Cancellation genuinely detaches the callback —
     * a leaked listener keeps the radio awake for the life of the process.
     */
    private suspend fun singleUpdate(manager: LocationManager, provider: String): Location? =
        suspendCancellableCoroutine { cont ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val signal = CancellationSignal()
                cont.invokeOnCancellation { runCatching { signal.cancel() } }
                try {
                    manager.getCurrentLocation(
                        provider,
                        signal,
                        ContextCompat.getMainExecutor(context)
                    ) { location -> if (cont.isActive) cont.resume(location) }
                } catch (e: Exception) {
                    if (cont.isActive) cont.resume(null)
                }
            } else {
                // Pre-R has no getCurrentLocation: subscribe, take the first fix,
                // then unsubscribe by hand. All four callbacks are overridden
                // because the interface's default methods only exist on API 30+.
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        runCatching { manager.removeUpdates(this) }
                        if (cont.isActive) cont.resume(location)
                    }

                    override fun onStatusChanged(p: String?, status: Int, extras: Bundle?) = Unit
                    override fun onProviderEnabled(p: String) = Unit
                    override fun onProviderDisabled(p: String) {
                        runCatching { manager.removeUpdates(this) }
                        if (cont.isActive) cont.resume(null)
                    }
                }
                cont.invokeOnCancellation { runCatching { manager.removeUpdates(listener) } }
                try {
                    manager.requestLocationUpdates(
                        provider, 0L, 0f, listener, Looper.getMainLooper()
                    )
                } catch (e: Exception) {
                    runCatching { manager.removeUpdates(listener) }
                    if (cont.isActive) cont.resume(null)
                }
            }
        }

    // ---- reverse geocoding -----------------------------------------------------

    private suspend fun reverseGeocode(location: Location): LocatedCity? {
        // Geocoder is backed by an optional system service; plenty of devices
        // (and most AOSP/de-Googled builds) ship without it.
        if (!Geocoder.isPresent()) return null
        val geocoder = runCatching { Geocoder(context, Locale.getDefault()) }.getOrNull() ?: return null

        val address = withTimeoutOrNull(GEOCODE_TIMEOUT_MS) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocodeAsync(geocoder, location)
            } else {
                withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    runCatching {
                        geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    }.getOrNull()?.firstOrNull()
                }
            }
        } ?: return null

        val name = address.cityName() ?: return null
        return LocatedCity(
            name    = name,
            country = address.countryCode ?: address.countryName.orEmpty(),
            // Coarsened before it leaves this class: the forecast API only ever sees
            // ~11 m precision, never the raw GPS fix. See COORD_SCALE.
            lat     = location.latitude.coarsened(),
            lon     = location.longitude.coarsened()
        )
    }

    /** API 33+ requires the callback form; the blocking overload is deprecated there. */
    private suspend fun geocodeAsync(geocoder: Geocoder, location: Location): Address? =
        suspendCancellableCoroutine { cont ->
            try {
                geocoder.getFromLocation(
                    location.latitude,
                    location.longitude,
                    1,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (cont.isActive) cont.resume(addresses.firstOrNull())
                        }

                        override fun onError(errorMessage: String?) {
                            if (cont.isActive) cont.resume(null)
                        }
                    }
                )
            } catch (e: Exception) {
                if (cont.isActive) cont.resume(null)
            }
        }

    /** Rural coordinates often have no locality, so widen out until something sticks. */
    private fun Address.cityName(): String? = listOfNotNull(
        locality,
        subAdminArea,
        subLocality,
        adminArea,
        featureName
    ).firstOrNull { it.isNotBlank() }
}
