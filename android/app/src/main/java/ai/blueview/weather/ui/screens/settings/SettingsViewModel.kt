package ai.blueview.weather.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ai.blueview.weather.data.preferences.LocationMode
import ai.blueview.weather.data.preferences.SavedCity
import ai.blueview.weather.data.preferences.UserPrefs
import ai.blueview.weather.data.preferences.UserPreferencesRepository
import ai.blueview.weather.data.repository.WeatherRepository
import ai.blueview.weather.data.repository.WeatherResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: WeatherRepository,
    private val prefsRepo: UserPreferencesRepository
) : ViewModel() {

    val prefs: Flow<UserPrefs> = prefsRepo.prefs

    fun searchCity(city: String) {
        viewModelScope.launch {
            when (val geo = repository.geocode(city)) {
                is WeatherResult.Success -> {
                    val r = geo.data
                    // addCity is idempotent on id, so re-adding refreshes rather than duplicates.
                    prefsRepo.addCity(
                        SavedCity(
                            name    = r.name,
                            country = r.countryCode,
                            lat     = r.latitude,
                            lon     = r.longitude
                        )
                    )
                }
                is WeatherResult.Error -> Unit
            }
        }
    }

    fun removeCity(id: String) {
        viewModelScope.launch { prefsRepo.removeCity(id) }
    }

    fun pinCity(id: String) {
        viewModelScope.launch { prefsRepo.pinCityAndSwitch(id) }
    }

    fun setLocationMode(mode: LocationMode) {
        viewModelScope.launch { prefsRepo.setLocationMode(mode) }
    }

    fun setUnits(units: String) {
        viewModelScope.launch { prefsRepo.saveUnits(units) }
    }
}
