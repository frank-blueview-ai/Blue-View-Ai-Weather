package ai.blueview.weather.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ai.blueview.weather.data.preferences.LocationMode
import ai.blueview.weather.data.preferences.SavedCity
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
    var citiesOpen by remember { mutableStateOf(false) }

    val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    val context = LocalContext.current
    fun hasLocationGrant() = locationPermissions.any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        viewModel.onLocationPermissionResult(
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
                grants[Manifest.permission.ACCESS_FINE_LOCATION] == true
        )
    }

    // The ViewModel decides when asking is warranted (AUTOMATIC mode, no grant yet);
    // the launcher has to live in the composable, so this only relays the request.
    LaunchedEffect(state.askLocationPermission) {
        if (state.askLocationPermission) permissionLauncher.launch(locationPermissions)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (showSearch) {
                        // Focus and raise the keyboard on open — otherwise the field
                        // appears ready but silently swallows typing until tapped.
                        val focusRequester = remember { FocusRequester() }
                        LaunchedEffect(Unit) { focusRequester.requestFocus() }
                        TextField(
                            value         = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder   = { Text("City name…") },
                            singleLine    = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                if (searchQuery.isNotBlank()) {
                                    viewModel.searchCity(searchQuery)
                                    searchQuery = ""
                                }
                                showSearch = false
                            }),
                            colors        = TextFieldDefaults.colors(
                                focusedContainerColor   = NavyMid,
                                unfocusedContainerColor = NavyMid,
                                focusedTextColor        = TextPrimary,
                                unfocusedTextColor      = TextPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                    } else {
                        Box {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier          = Modifier.clickable { citiesOpen = true }
                            ) {
                                Text(
                                    text  = state.cityLabel.ifBlank { "🌤  Blue View Weather" },
                                    style = MaterialTheme.typography.titleLarge,
                                    color = BlueAccent
                                )
                                Icon(Icons.Default.ArrowDropDown, "Switch city", tint = TextSecondary)
                            }
                            CitySwitcherMenu(
                                expanded     = citiesOpen,
                                savedCities  = state.savedCities,
                                locationMode = state.locationMode,
                                activeCityId = state.activeCityId,
                                onDismiss    = { citiesOpen = false },
                                onSelect     = { id -> citiesOpen = false; viewModel.selectCity(id) },
                                onRemove     = { id -> viewModel.removeCity(id) },
                                onUseCurrentLocation = {
                                    citiesOpen = false
                                    viewModel.useCurrentLocation()
                                    // Only prompt when the grant is actually missing.
                                    // Launching regardless returns an instant "granted"
                                    // that triggers a second, redundant refresh.
                                    if (!hasLocationGrant()) {
                                        permissionLauncher.launch(locationPermissions)
                                    }
                                },
                                onAddCity    = { citiesOpen = false; showSearch = true }
                            )
                        }
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
                                    modifier = Modifier.fillMaxWidth()
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

@Composable
private fun CitySwitcherMenu(
    expanded: Boolean,
    savedCities: List<SavedCity>,
    locationMode: LocationMode,
    activeCityId: String?,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onRemove: (String) -> Unit,
    onUseCurrentLocation: () -> Unit,
    onAddCity: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text        = { Text("Current location", color = TextPrimary) },
            onClick     = onUseCurrentLocation,
            leadingIcon = {
                Icon(Icons.Default.MyLocation, null,
                    tint = if (locationMode == LocationMode.AUTOMATIC) BlueAccent else TextMuted)
            }
        )
        if (savedCities.isNotEmpty()) {
            HorizontalDivider(color = TextMuted.copy(alpha = 0.3f))
        }
        savedCities.forEach { city ->
            // Only highlight a saved row in PINNED mode: the AUTOMATIC city is
            // synthesised from the device fix and is never a member of this list.
            val isActive = locationMode == LocationMode.PINNED && city.id == activeCityId
            DropdownMenuItem(
                text         = {
                    Text(city.displayLabel, color = if (isActive) BlueAccent else TextPrimary)
                },
                onClick      = { onSelect(city.id) },
                trailingIcon = {
                    IconButton(onClick = { onRemove(city.id) }) {
                        Icon(Icons.Default.Delete, "Remove ${city.displayLabel}", tint = TextMuted)
                    }
                }
            )
        }
        HorizontalDivider(color = TextMuted.copy(alpha = 0.3f))
        DropdownMenuItem(
            text        = { Text("Add city…", color = TextPrimary) },
            onClick     = onAddCity,
            leadingIcon = { Icon(Icons.Default.Add, null, tint = TextSecondary) }
        )
    }
}
