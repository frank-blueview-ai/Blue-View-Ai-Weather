import Foundation
import Combine

struct UserPreferences: Equatable {
    var cities: [SavedCity] = []
    var units: String = "imperial"
    var locationEnabled: Bool = true
}

/// UserDefaults-backed store, mirrors Android UserPreferencesRepository (DataStore).
final class UserPreferencesStore: ObservableObject {
    static let shared = UserPreferencesStore()

    @Published private(set) var prefs: UserPreferences

    private let defaults: UserDefaults
    private enum Keys {
        static let cities = "pref_cities"
        static let units = "pref_units"
        static let locationEnabled = "pref_location_enabled"
        // Legacy single-city keys, read once for migration.
        static let city = "pref_city"
        static let cityLabel = "pref_city_label"
        static let lat = "pref_lat"
        static let lon = "pref_lon"
    }

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        self.prefs = UserPreferences(
            cities: Self.loadCities(from: defaults),
            // Imperial is the product default, but a unit the user explicitly picked wins.
            units: defaults.string(forKey: Keys.units) ?? "imperial",
            // Auto-location is the default, so an unset key means enabled. bool(forKey:) can't
            // tell "never set" from "user switched it off", hence the object(forKey:) cast.
            locationEnabled: (defaults.object(forKey: Keys.locationEnabled) as? Bool) ?? true
        )
        // Persist a migrated list immediately so the legacy keys are only read once.
        if defaults.data(forKey: Keys.cities) == nil && !prefs.cities.isEmpty {
            persistCities()
        }
    }

    // MARK: - Mutations

    func addCity(_ city: SavedCity) {
        guard !city.isCurrentLocation else { return }
        guard !prefs.cities.contains(where: { $0.label == city.label || ($0.lat == city.lat && $0.lon == city.lon) }) else { return }
        prefs.cities.append(city)
        persistCities()
    }

    func removeCity(id: String) {
        guard id != SavedCity.currentLocationId else { return }
        prefs.cities.removeAll { $0.id == id && !$0.isCurrentLocation }
        persistCities()
    }

    func updateCurrentLocation(label: String, lat: Double, lon: Double) {
        let entry = SavedCity.currentLocation(label: label, lat: lat, lon: lon)
        if let index = prefs.cities.firstIndex(where: { $0.isCurrentLocation }) {
            prefs.cities.remove(at: index)
        }
        prefs.cities.insert(entry, at: 0)
        persistCities()
    }

    func setLocationEnabled(_ enabled: Bool) {
        defaults.set(enabled, forKey: Keys.locationEnabled)
        prefs.locationEnabled = enabled
        if !enabled {
            prefs.cities.removeAll { $0.isCurrentLocation }
            persistCities()
        }
    }

    func saveUnits(_ units: String) {
        defaults.set(units, forKey: Keys.units)
        prefs.units = units
    }

    // MARK: - Storage

    private func persistCities() {
        if let data = try? JSONEncoder().encode(prefs.cities) {
            defaults.set(data, forKey: Keys.cities)
        }
    }

    private static func loadCities(from defaults: UserDefaults) -> [SavedCity] {
        if let data = defaults.data(forKey: Keys.cities),
           let decoded = try? JSONDecoder().decode([SavedCity].self, from: data) {
            return decoded
        }
        // Migration: before multi-city support the app stored one city in flat keys.
        // Convert it once so an existing install does not come back empty.
        let name = defaults.string(forKey: Keys.city) ?? ""
        let label = defaults.string(forKey: Keys.cityLabel) ?? ""
        let lat = defaults.double(forKey: Keys.lat)
        let lon = defaults.double(forKey: Keys.lon)
        guard !name.isEmpty || !label.isEmpty, lat != 0.0 || lon != 0.0 else { return [] }
        return [SavedCity(name: name.isEmpty ? label : name,
                          label: label.isEmpty ? name : label,
                          lat: lat,
                          lon: lon)]
    }
}
