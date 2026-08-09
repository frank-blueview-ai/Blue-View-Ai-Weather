package ai.blueview.weather.ui.screens.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ai.blueview.weather.BuildConfig
import ai.blueview.weather.data.update.UpdateChecker
import ai.blueview.weather.data.update.UpdateState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AboutViewModel @Inject constructor(
    private val checker: UpdateChecker
) : ViewModel() {

    private val _update = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val update: StateFlow<UpdateState> = _update.asStateFlow()

    fun checkForUpdate() {
        if (_update.value is UpdateState.Checking) return
        viewModelScope.launch {
            _update.value = UpdateState.Checking
            _update.value = checker.checkLatest(BuildConfig.VERSION_NAME)
        }
    }

    fun reset() { _update.value = UpdateState.Idle }
}
