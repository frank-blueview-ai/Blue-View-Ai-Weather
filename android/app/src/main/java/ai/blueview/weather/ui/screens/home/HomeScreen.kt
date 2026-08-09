package ai.blueview.weather.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ai.blueview.weather.ui.components.*
import ai.blueview.weather.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (showSearch) {
                        TextField(
                            value         = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder   = { Text("City name…") },
                            singleLine    = true,
                            colors        = TextFieldDefaults.colors(
                                focusedContainerColor   = NavyMid,
                                unfocusedContainerColor = NavyMid,
                                focusedTextColor        = TextPrimary,
                                unfocusedTextColor      = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text("🌤  Blue View Weather",
                            style = MaterialTheme.typography.titleLarge,
                            color = BlueAccent)
                    }
                },
                actions = {
                    if (showSearch) {
                        IconButton(onClick = {
                            if (searchQuery.isNotBlank()) {
                                viewModel.searchCity(searchQuery)
                                searchQuery = ""
                            }
                            showSearch = false
                        }) { Icon(Icons.Default.Check, "Search") }
                    } else {
                        IconButton(onClick = { showSearch = true }) {
                            Icon(Icons.Default.Search, "Search city", tint = TextSecondary)
                        }
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Default.Refresh, "Refresh", tint = TextSecondary)
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, "Settings", tint = TextSecondary)
                        }
                        IconButton(onClick = onNavigateToAbout) {
                            Icon(Icons.Default.Info, "About", tint = TextSecondary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyDeep)
            )
        },
        containerColor = NavyDeep
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(NavyDeep)
        ) {
            when {
                state.isLoading && state.forecast == null -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color    = BlueAccent
                    )
                }
                state.needsCitySetup -> {
                    CitySetupPrompt(
                        onSearch = { city ->
                            viewModel.searchCity(city)
                            showSearch = false
                        },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Current conditions
                        state.forecast?.let { fc ->
                            CurrentWeatherCard(
                                city    = state.cityLabel,
                                current = fc.current,
                                daily   = fc.daily,
                                units   = state.units
                            )
                        }

                        // 7-Day Forecast
                        CollapsibleSection(
                            title    = "7-Day Forecast",
                            expanded = state.expandForecast,
                            onToggle = viewModel::toggleForecast
                        ) {
                            state.forecast?.let { fc ->
                                ForecastRow(
                                    daily        = fc.daily,
                                    units        = state.units,
                                    selectedDate = state.selectedDate,
                                    onDayClick   = viewModel::onDaySelected
                                )
                            }
                            // Hourly sub-section
                            CollapsibleSection(
                                title    = if (state.selectedDate != null)
                                    "Hourly — ${state.selectedDate}" else "Hourly",
                                expanded = state.expandHourly,
                                onToggle = viewModel::toggleHourly
                            ) {
                                state.forecast?.let { fc ->
                                    state.selectedDate?.let { date ->
                                        HourlyRow(
                                            hourly       = fc.hourly,
                                            selectedDate = date,
                                            units        = state.units
                                        )
                                    }
                                }
                            }
                        }

                        // Radar Map
                        CollapsibleSection(
                            title    = "Radar Map",
                            expanded = state.expandRadar,
                            onToggle = viewModel::toggleRadar
                        ) {
                            val tileUrl = state.radarTileUrl
                            if (state.lat != 0.0 && state.lon != 0.0 && tileUrl != null) {
                                RadarWebView(
                                    lat      = state.lat,
                                    lon      = state.lon,
                                    city     = state.cityLabel,
                                    tileUrl  = tileUrl,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(280.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                    }

                    // Loading overlay
                    if (state.isLoading) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter),
                            color = BlueAccent
                        )
                    }
                }
            }

            // Error snackbar
            state.error?.let { msg ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action   = {
                        TextButton(onClick = viewModel::dismissError) { Text("Dismiss") }
                    },
                    containerColor = ErrorRed.copy(alpha = 0.9f)
                ) { Text(msg) }
            }
        }
    }
}

@Composable
private fun CitySetupPrompt(onSearch: (String) -> Unit, modifier: Modifier = Modifier) {
    var query by remember { mutableStateOf("") }
    Column(
        modifier            = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("🌤", style = MaterialTheme.typography.displayLarge)
        Text("Welcome to Blue View Weather",
            style     = MaterialTheme.typography.headlineMedium,
            color     = TextPrimary,
            textAlign = TextAlign.Center)
        Text("Enter your city to get started.\nNo API key needed.",
            style     = MaterialTheme.typography.bodyLarge,
            color     = TextSecondary,
            textAlign = TextAlign.Center)
        OutlinedTextField(
            value         = query,
            onValueChange = { query = it },
            label         = { Text("City name") },
            placeholder   = { Text("e.g. Miami, London, Tokyo") },
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
            onClick  = { if (query.isNotBlank()) onSearch(query) },
            colors   = ButtonDefaults.buttonColors(containerColor = BlueAccent),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Get Weather", color = NavyDeep) }
    }
}
