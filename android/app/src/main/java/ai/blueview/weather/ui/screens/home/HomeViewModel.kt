package ai.blueview.weather.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ai.blueview.weather.data.api.dto.ForecastResponse
import ai.blueview.weather.data.location.LocationProvider
import ai.blueview.weather.data.preferences.LocationMode
import ai.blueview.weather.data.preferences.SavedCity
import ai.blueview.weather.data.preferences.UserPrefs
import ai.blueview.weather.data.preferences.UserPreferencesRepository
import ai.blueview.weather.data.radar.RadarRepository
import ai.blueview.weather.data.repository.WeatherRepository
import ai.blueview.weather.data.repository.WeatherResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean            = false,
    val forecast: ForecastResponse?   = null,
    val cityLabel: String             = "",
    val lat: Double                   = 0.0,
    val lon: Double                   = 0.0,
    val units: String                 = "imperial",
    val error: String?                = null,
    val selectedDate: String?         = null,      // hourly drill-down
    val expandForecast: Boolean       = true,
    val expandHourly: Boolean         = false,
    val expandRadar: Boolean          = true,
    val needsCitySetup: Boolean       = false,
    val radarTileUrl: String?         = null,
    // Multi-city / location
    val savedCities: List<SavedCity>  = emptyList(),
    val locationMode: LocationMode    = LocationMode.AUTOMATIC,
    val activeCityId: String?         = null,
    val askLocationPermission: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: WeatherRepository,
    private val prefs: UserPreferencesRepository,
    private val radar: RadarRepository,
    private val locationProvider: LocationProvider
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    /** Forces a re-resolve even when prefs did not change. See the collector in init. */
    private val refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        viewModelScope.launch {
            prefs.migrateLegacyCityIfNeeded()

            // Ask for the grant exactly once, ever — persisted, not per-process. Asking
            // again on every cold start after a denial re-renders the system dialog on
            // API 26-29. A denial is not a failure: the IP fallback still yields a city.
            val initial = prefs.prefs.first()
            if (initial.locationMode == LocationMode.AUTOMATIC &&
                !locationProvider.hasLocationPermission() &&
                !prefs.locationPermissionAsked.first()
            ) {
                prefs.markLocationPermissionAsked()
                _state.update { it.copy(askLocationPermission = true) }
            }

            // Every apply goes through this one collectLatest so there is exactly one
            // in-flight resolve at a time. Manual refreshes are merged in rather than
            // launched separately: a separate coroutine would race the collector, and
            // with a 10s location timeout the loser could finish last and overwrite
            // newer state with a stale forecast.
            //
            // distinctUntilChanged keeps redundant DataStore emissions from restarting
            // the work, but it also means an idempotent write (re-adding a city that is
            // already saved and pinned) emits nothing at all — so anything that sets
            // isLoading and waits for the collector must also poke refreshTrigger, or
            // the spinner never clears.
            merge(
                prefs.prefs.distinctUntilChanged(),
                refreshTrigger.map { prefs.prefs.first() }
            ).collectLatest { p -> applyPrefs(p) }
        }
    }

    fun refresh() {
        refreshTrigger.tryEmit(Unit)
    }

    /** Mirrors prefs into the UI, resolves the active city, and fetches for it. */
    private suspend fun applyPrefs(p: UserPrefs) {
        _state.update {
            it.copy(
                isLoading    = true,
                error        = null,
                units        = p.units,
                savedCities  = p.savedCities,
                locationMode = p.locationMode
            )
        }
        val active = resolveActiveCity(p)
        if (active == null) {
            // isLoading must clear here — HomeScreen tests isLoading before
            // needsCitySetup, so leaving it set hides the setup prompt forever.
            _state.update { it.copy(isLoading = false, needsCitySetup = true, activeCityId = null) }
            return
        }
        _state.update {
            it.copy(
                cityLabel      = active.displayLabel,
                activeCityId   = active.id,
                needsCitySetup = false
            )
        }
        refresh(active.lat, active.lon, p.units)
    }

    /**
     * Single source of truth for "which city are we showing".
     * PINNED falls back to fallbackCity because setPinnedCity does not validate the
     * id and the prefs Flow nulls a pin it cannot resolve — without this, a stale pin
     * would strand the user on the setup screen with cities already saved.
     */
    private suspend fun resolveActiveCity(p: UserPrefs): SavedCity? = when (p.locationMode) {
        LocationMode.AUTOMATIC -> locationProvider.currentCity()?.let { loc ->
            SavedCity(name = loc.name, country = loc.country, lat = loc.lat, lon = loc.lon)
        } ?: p.fallbackCity
        LocationMode.PINNED    -> p.pinnedCity ?: p.fallbackCity
    }

    private suspend fun refresh(lat: Double, lon: Double, units: String) {
        _state.update { it.copy(isLoading = true, error = null) }
        // Genuinely concurrent. These were sequential despite the old comment, which put
        // the radar round trip on the critical path of a load the whole screen blocks on,
        // for a value only the collapsible radar section consumes.
        val forecastResult: WeatherResult<ForecastResponse>
        val tileUrl: String?
        coroutineScope {
            val forecastAsync = async { repository.forecast(lat, lon, units) }
            val tileAsync     = async { radar.latestTileUrl() }
            forecastResult = forecastAsync.await()
            tileUrl        = tileAsync.await()
        }
        when (forecastResult) {
            is WeatherResult.Success -> _state.update {
                it.copy(isLoading = false, forecast = forecastResult.data,
                    lat = lat, lon = lon, needsCitySetup = false,
                    radarTileUrl = tileUrl)
            }
            is WeatherResult.Error   -> _state.update { it.copy(isLoading = false, error = forecastResult.message) }
        }
    }

    fun searchCity(query: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val geo = repository.geocode(query)) {
                is WeatherResult.Success -> {
                    val r    = geo.data
                    val city = SavedCity(
                        name    = r.name,
                        country = r.countryCode,
                        lat     = r.latitude,
                        lon     = r.longitude
                    )
                    // One atomic write, so the collector fires once instead of three times.
                    prefs.addAndPinCity(city)
                    // Poke the trigger as well. Re-searching a city that is already saved
                    // and pinned writes byte-identical prefs, so distinctUntilChanged
                    // emits nothing — and isLoading, set above, would never clear. This
                    // is the ordinary "search failed, user retries the same name" path.
                    refresh()
                }
                is WeatherResult.Error -> _state.update { it.copy(isLoading = false, error = geo.message) }
            }
        }
    }

    /** Switch the active city to a saved one, pinning it. */
    fun selectCity(id: String) {
        viewModelScope.launch { prefs.pinCityAndSwitch(id) }
    }

    fun removeCity(id: String) {
        viewModelScope.launch { prefs.removeCity(id) }
    }

    /** Go back to tracking the device location. */
    fun useCurrentLocation() {
        viewModelScope.launch {
            prefs.setLocationMode(LocationMode.AUTOMATIC)
            // Already in AUTOMATIC? The write is a no-op and the collector stays silent,
            // so tapping "Current location" would do nothing visible. Force a re-resolve
            // — which is exactly what the user asked for by tapping it.
            refresh()
        }
    }

    /**
     * Denial is not an error state: the IP fallback already produced a city. A grant
     * is worth a re-resolve so the coarse IP guess is replaced by the real fix.
     */
    fun onLocationPermissionResult(granted: Boolean) {
        _state.update { it.copy(askLocationPermission = false) }
        if (granted) refresh()
    }

    fun setUnits(units: String) {
        // units is part of UserPrefs, so the collector re-fires and refetches.
        viewModelScope.launch { prefs.saveUnits(units) }
    }

    fun onDaySelected(date: String) {
        _state.update { current ->
            val alreadySelected = current.selectedDate == date && current.expandHourly
            current.copy(
                selectedDate  = if (alreadySelected) null else date,
                expandHourly  = !alreadySelected
            )
        }
    }

    fun toggleForecast() = _state.update { it.copy(expandForecast = !it.expandForecast) }
    fun toggleHourly()   = _state.update { it.copy(expandHourly   = !it.expandHourly) }
    fun toggleRadar()    = _state.update { it.copy(expandRadar    = !it.expandRadar) }
    fun dismissError()   = _state.update { it.copy(error = null) }
}
