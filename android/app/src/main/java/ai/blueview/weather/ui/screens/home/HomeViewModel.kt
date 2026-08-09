package ai.blueview.weather.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ai.blueview.weather.data.api.dto.ForecastResponse
import ai.blueview.weather.data.preferences.UserPreferencesRepository
import ai.blueview.weather.data.radar.RadarRepository
import ai.blueview.weather.data.repository.WeatherRepository
import ai.blueview.weather.data.repository.WeatherResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean           = false,
    val forecast: ForecastResponse?  = null,
    val cityLabel: String            = "",
    val lat: Double                  = 0.0,
    val lon: Double                  = 0.0,
    val units: String                = "metric",
    val error: String?               = null,
    val selectedDate: String?        = null,      // hourly drill-down
    val expandForecast: Boolean      = true,
    val expandHourly: Boolean        = false,
    val expandRadar: Boolean         = true,
    val needsCitySetup: Boolean      = false,
    val radarTileUrl: String?        = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: WeatherRepository,
    private val prefs: UserPreferencesRepository,
    private val radar: RadarRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.prefs.collect { p ->
                _state.update { it.copy(units = p.units, cityLabel = p.cityLabel) }
                if (p.city.isBlank()) {
                    _state.update { it.copy(needsCitySetup = true) }
                } else {
                    refresh(p.lat, p.lon, p.units)
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val p = prefs.prefs.first()
            if (p.city.isBlank()) { _state.update { it.copy(needsCitySetup = true) }; return@launch }
            refresh(p.lat, p.lon, p.units)
        }
    }

    private suspend fun refresh(lat: Double, lon: Double, units: String) {
        _state.update { it.copy(isLoading = true, error = null) }
        // Fetch forecast and radar tile URL concurrently
        val forecastResult = repository.forecast(lat, lon, units)
        val tileUrl = radar.latestTileUrl()
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
                    val result = geo.data
                    val label  = "${result.name}, ${result.countryCode}"
                    prefs.saveCity(query, label, result.latitude, result.longitude)
                    _state.update { it.copy(cityLabel = label, needsCitySetup = false) }
                    refresh(result.latitude, result.longitude, _state.value.units)
                }
                is WeatherResult.Error -> _state.update { it.copy(isLoading = false, error = geo.message) }
            }
        }
    }

    fun setUnits(units: String) {
        viewModelScope.launch {
            prefs.saveUnits(units)
            refresh()
        }
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
