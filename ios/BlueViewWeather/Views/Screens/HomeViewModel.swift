import Foundation
import Combine

/// Per-city view state. Paging must not let one city's fetch clobber another's,
/// so every city owns its forecast, its spinner, its error and its expand flags.
struct CityWeatherState {
    var forecast: ForecastResponse?
    var isLoading = false
    var error: String?
    var selectedDate: String?
    var expandForecast = true
    var expandHourly = false
    var expandRadar = true
    var loadedAt: Date?
}

@MainActor
final class HomeViewModel: ObservableObject {
    @Published private(set) var cities: [SavedCity] = []
    @Published private(set) var states: [String: CityWeatherState] = [:]
    @Published private(set) var units = "imperial"
    @Published private(set) var needsCitySetup = false
    @Published private(set) var isSearching = false
    @Published private(set) var isResolvingLocation = false
    @Published var searchError: String?
    @Published var selectedIndex = 0

    let locationManager = LocationManager()

    private let repository: WeatherRepository
    private let prefsStore: UserPreferencesStore
    private var cancellables = Set<AnyCancellable>()
    private var didBootstrap = false
    /// A newly added city should become the visible page, but the prefs publish that
    /// carries it arrives asynchronously — remember which label to jump to.
    private var pendingSelectionLabel: String?

    /// Swiping back to a page should reuse what was fetched a moment ago instead of
    /// re-hitting Open-Meteo; a manual refresh always bypasses this.
    private let cacheTTL: TimeInterval = 10 * 60

    init(repository: WeatherRepository = .shared, prefsStore: UserPreferencesStore = .shared) {
        self.repository = repository
        self.prefsStore = prefsStore

        cities = prefsStore.prefs.cities
        units = prefsStore.prefs.units
        needsCitySetup = cities.isEmpty

        prefsStore.$prefs
            .dropFirst()
            .sink { [weak self] p in
                Task { @MainActor in self?.apply(p) }
            }
            .store(in: &cancellables)
    }

    // MARK: - Derived

    var selectedCity: SavedCity? {
        guard selectedIndex >= 0 && selectedIndex < cities.count else { return nil }
        return cities[selectedIndex]
    }

    func state(for cityId: String) -> CityWeatherState {
        states[cityId] ?? CityWeatherState()
    }

    // MARK: - Lifecycle

    func onAppear() {
        guard !didBootstrap else {
            loadSelectedIfNeeded()
            return
        }
        didBootstrap = true
        Task { await bootstrap() }
    }

    private func bootstrap() async {
        // Auto-location is the default: ask on first launch, and honour a previous yes.
        let status = locationManager.authorizationStatus
        if status == .notDetermined || (prefsStore.prefs.locationEnabled && locationManager.isAuthorized) {
            await resolveCurrentLocation()
        }
        needsCitySetup = cities.isEmpty
        loadSelectedIfNeeded()
    }

    func resolveCurrentLocation() async {
        isResolvingLocation = true
        defer { isResolvingLocation = false }

        guard let fix = await locationManager.resolveCurrentCity() else {
            // Denied or no fix — the app stays usable with manually added cities.
            if !locationManager.isAuthorized && prefsStore.prefs.locationEnabled {
                prefsStore.setLocationEnabled(false)
            }
            return
        }
        if !prefsStore.prefs.locationEnabled {
            prefsStore.setLocationEnabled(true)
        }
        prefsStore.updateCurrentLocation(label: fix.label, lat: fix.lat, lon: fix.lon)
    }

    // MARK: - Prefs

    private func apply(_ p: UserPreferences) {
        let unitsChanged = p.units != units
        let previous = cities

        units = p.units
        cities = p.cities

        let liveIds = Set(p.cities.map { $0.id })
        states = states.filter { liveIds.contains($0.key) }

        for city in p.cities {
            // A moved current-location pin or a unit switch makes the cached forecast wrong.
            let moved = previous.first(where: { $0.id == city.id }).map { $0.lat != city.lat || $0.lon != city.lon } ?? false
            if unitsChanged || moved {
                states[city.id]?.forecast = nil
                states[city.id]?.loadedAt = nil
            }
        }

        if let label = pendingSelectionLabel, let index = cities.firstIndex(where: { $0.label == label }) {
            pendingSelectionLabel = nil
            selectedIndex = index
        } else if selectedIndex >= cities.count {
            selectedIndex = max(0, cities.count - 1)
        }

        needsCitySetup = cities.isEmpty
        loadSelectedIfNeeded()
    }

    // MARK: - Loading

    func loadSelectedIfNeeded() {
        guard let city = selectedCity else { return }
        let state = state(for: city.id)
        guard !state.isLoading else { return }
        if state.forecast != nil, let loadedAt = state.loadedAt, Date().timeIntervalSince(loadedAt) < cacheTTL {
            return
        }
        Task { await load(city) }
    }

    func refresh() {
        guard let city = selectedCity else {
            needsCitySetup = cities.isEmpty
            return
        }
        Task { await load(city) }
    }

    private func load(_ city: SavedCity) async {
        mutate(city.id) {
            $0.isLoading = true
            $0.error = nil
        }
        let result = await repository.forecast(latitude: city.lat, longitude: city.lon, units: units)
        // The city may have been deleted mid-flight; mutate would resurrect it.
        guard cities.contains(where: { $0.id == city.id }) else { return }
        switch result {
        case .success(let data):
            mutate(city.id) {
                $0.forecast = data
                $0.loadedAt = Date()
                $0.error = nil
                $0.isLoading = false
            }
        case .failure(let message):
            mutate(city.id) {
                $0.error = message
                $0.isLoading = false
            }
        }
    }

    // MARK: - City management

    func addCity(_ query: String) {
        let trimmed = query.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty else { return }
        Task {
            isSearching = true
            searchError = nil
            switch await repository.geocode(query: trimmed) {
            case .success(let result):
                let label = "\(result.name), \(result.countryCode ?? "")"
                pendingSelectionLabel = label
                prefsStore.addCity(SavedCity(name: result.name,
                                             label: label,
                                             lat: result.latitude,
                                             lon: result.longitude))
            case .failure(let message):
                searchError = message
            }
            isSearching = false
        }
    }

    func removeCity(id: String) {
        guard let index = cities.firstIndex(where: { $0.id == id }), !cities[index].isCurrentLocation else { return }
        if selectedIndex >= index && selectedIndex > 0 { selectedIndex -= 1 }
        prefsStore.removeCity(id: id)
    }

    func removeSelectedCity() {
        guard let city = selectedCity else { return }
        removeCity(id: city.id)
    }

    func setUnits(_ units: String) {
        prefsStore.saveUnits(units)
    }

    func setLocationEnabled(_ enabled: Bool) {
        if enabled {
            Task { await resolveCurrentLocation() }
        } else {
            prefsStore.setLocationEnabled(false)
        }
    }

    // MARK: - Per-city interaction

    func onDaySelected(_ date: String, cityId: String) {
        var state = state(for: cityId)
        let alreadySelected = state.selectedDate == date && state.expandHourly
        state.selectedDate = alreadySelected ? nil : date
        state.expandHourly = !alreadySelected
        states[cityId] = state
    }

    func toggleForecast(_ cityId: String) { mutate(cityId) { $0.expandForecast.toggle() } }
    func toggleHourly(_ cityId: String) { mutate(cityId) { $0.expandHourly.toggle() } }
    func toggleRadar(_ cityId: String) { mutate(cityId) { $0.expandRadar.toggle() } }
    func dismissError(_ cityId: String) { mutate(cityId) { $0.error = nil } }

    private func mutate(_ cityId: String, _ change: (inout CityWeatherState) -> Void) {
        var state = states[cityId] ?? CityWeatherState()
        change(&state)
        states[cityId] = state
    }
}
