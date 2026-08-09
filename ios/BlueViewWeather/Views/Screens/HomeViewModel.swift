import Foundation
import Combine

@MainActor
final class HomeViewModel: ObservableObject {
    @Published var isLoading = false
    @Published var forecast: ForecastResponse?
    @Published var cityLabel = ""
    @Published var lat = 0.0
    @Published var lon = 0.0
    @Published var units = "metric"
    @Published var error: String?
    @Published var selectedDate: String?
    @Published var expandForecast = true
    @Published var expandHourly = false
    @Published var expandRadar = true
    @Published var needsCitySetup = false

    private let repository: WeatherRepository
    private let prefsStore: UserPreferencesStore
    private var cancellables = Set<AnyCancellable>()

    init(repository: WeatherRepository = .shared, prefsStore: UserPreferencesStore = .shared) {
        self.repository = repository
        self.prefsStore = prefsStore

        prefsStore.$prefs
            .sink { [weak self] p in
                guard let self else { return }
                self.units = p.units
                self.cityLabel = p.cityLabel
                if p.city.isEmpty {
                    self.needsCitySetup = true
                } else {
                    Task { await self.refresh(lat: p.lat, lon: p.lon, units: p.units) }
                }
            }
            .store(in: &cancellables)
    }

    func refresh() {
        let p = prefsStore.prefs
        guard !p.city.isEmpty else { needsCitySetup = true; return }
        Task { await refresh(lat: p.lat, lon: p.lon, units: p.units) }
    }

    private func refresh(lat: Double, lon: Double, units: String) async {
        isLoading = true
        error = nil
        switch await repository.forecast(latitude: lat, longitude: lon, units: units) {
        case .success(let data):
            forecast = data
            self.lat = lat
            self.lon = lon
            needsCitySetup = false
        case .failure(let message):
            error = message
        }
        isLoading = false
    }

    func searchCity(_ query: String) {
        Task {
            isLoading = true
            error = nil
            switch await repository.geocode(query: query) {
            case .success(let result):
                let label = "\(result.name), \(result.countryCode ?? "")"
                prefsStore.saveCity(query, label: label, lat: result.latitude, lon: result.longitude)
                cityLabel = label
                needsCitySetup = false
                await refresh(lat: result.latitude, lon: result.longitude, units: units)
            case .failure(let message):
                error = message
                isLoading = false
            }
        }
    }

    func setUnits(_ units: String) {
        prefsStore.saveUnits(units)
        refresh()
    }

    func onDaySelected(_ date: String) {
        let alreadySelected = selectedDate == date && expandHourly
        selectedDate = alreadySelected ? nil : date
        expandHourly = !alreadySelected
    }

    func toggleForecast() { expandForecast.toggle() }
    func toggleHourly() { expandHourly.toggle() }
    func toggleRadar() { expandRadar.toggle() }
    func dismissError() { error = nil }
}
