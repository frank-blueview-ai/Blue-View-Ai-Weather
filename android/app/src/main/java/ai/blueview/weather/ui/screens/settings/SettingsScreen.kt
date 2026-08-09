package ai.blueview.weather.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // City
            var cityInput by remember(prefs?.city) { mutableStateOf(prefs?.city ?: "") }
            Text("City", style = MaterialTheme.typography.titleMedium, color = BlueAccent)
            OutlinedTextField(
                value         = cityInput,
                onValueChange = { cityInput = it },
                label         = { Text("City name") },
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
                onClick = { if (cityInput.isNotBlank()) viewModel.searchCity(cityInput) },
                colors  = ButtonDefaults.buttonColors(containerColor = BlueAccent)
            ) { Text("Update City", color = NavyDeep) }

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
