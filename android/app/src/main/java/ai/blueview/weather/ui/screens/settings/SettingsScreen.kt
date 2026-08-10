package ai.blueview.weather.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import ai.blueview.weather.data.preferences.LocationMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ai.blueview.weather.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val prefs by viewModel.prefs.collectAsStateWithLifecycle(initialValue = null)

    Scaffold(
        topBar = {
            TopAppBar(
                title  = { Text("Settings", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyDeep)
            )
        },
        containerColor = NavyDeep
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                // Content is far taller than a phone screen, and a Column clips
                // overflow silently — without this the Units section is unreachable.
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Location mode
            Text("Location", style = MaterialTheme.typography.titleMedium, color = BlueAccent)
            val currentMode = prefs?.locationMode ?: LocationMode.AUTOMATIC
            listOf(
                LocationMode.AUTOMATIC to "Automatic (use my location)",
                LocationMode.PINNED    to "Pinned city"
            ).forEach { (mode, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = currentMode == mode,
                            onClick  = { viewModel.setLocationMode(mode) }
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentMode == mode,
                        onClick  = { viewModel.setLocationMode(mode) },
                        colors   = RadioButtonDefaults.colors(selectedColor = BlueAccent)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(label, color = TextPrimary)
                }
            }

            HorizontalDivider(color = TextMuted.copy(alpha = 0.3f))

            // Saved cities
            Text("Saved Cities", style = MaterialTheme.typography.titleMedium, color = BlueAccent)
            val cities = prefs?.savedCities.orEmpty()
            if (cities.isEmpty()) {
                Text("No cities saved yet.", color = TextSecondary)
            } else {
                // Plain Column rather than LazyColumn: a lazy list inside a
                // verticalScroll parent is measured with infinite height and
                // crashes. The saved-city list is short, so laying it out
                // eagerly and letting the page scroll costs nothing.
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    cities.forEach { city ->
                        val isPinned = city.id == prefs?.pinnedCityId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.pinCity(city.id) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isPinned) {
                                Icon(Icons.Default.PushPin, "Pinned", tint = BlueAccent)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(
                                text     = city.displayLabel,
                                color    = if (isPinned) BlueAccent else TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.removeCity(city.id) }) {
                                Icon(Icons.Default.Delete, "Remove ${city.displayLabel}", tint = TextMuted)
                            }
                        }
                    }
                }
            }

            // Add a city. Not keyed on prefs — this is an "add" box, not an
            // "edit current city" box, so it must not reset itself from prefs.
            var cityInput by remember { mutableStateOf("") }
            OutlinedTextField(
                value         = cityInput,
                onValueChange = { cityInput = it },
                label         = { Text("Add a city") },
                singleLine    = true,
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = BlueAccent,
                    unfocusedBorderColor = TextMuted,
                    focusedTextColor     = TextPrimary,
                    unfocusedTextColor   = TextPrimary,
                    focusedLabelColor    = BlueAccent,
                    unfocusedLabelColor  = TextSecondary,
                    cursorColor          = BlueAccent
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    if (cityInput.isNotBlank()) { viewModel.searchCity(cityInput); cityInput = "" }
                },
                colors  = ButtonDefaults.buttonColors(containerColor = BlueAccent)
            ) { Text("Add City", color = NavyDeep) }

            HorizontalDivider(color = TextMuted.copy(alpha = 0.3f))

            // Units
            Text("Units", style = MaterialTheme.typography.titleMedium, color = BlueAccent)
            val currentUnits = prefs?.units ?: "metric"
            listOf("metric" to "Metric (°C, km/h)", "imperial" to "Imperial (°F, mph)")
                .forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = currentUnits == value,
                                onClick  = { viewModel.setUnits(value) }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentUnits == value,
                            onClick  = { viewModel.setUnits(value) },
                            colors   = RadioButtonDefaults.colors(selectedColor = BlueAccent)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(label, color = TextPrimary)
                    }
                }
        }
    }
}
