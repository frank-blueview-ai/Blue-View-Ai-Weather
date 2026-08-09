import Foundation
import Combine

struct UserPreferences: Equatable {
    var city: String = ""
    var cityLabel: String = ""
    var lat: Double = 0.0
    var lon: Double = 0.0
    var units: String = "metric"
}

/// UserDefaults-backed store, mirrors Android UserPreferencesRepository (DataStore).
final class UserPreferencesStore: ObservableObject {
    static let shared = UserPreferencesStore()

    @Published private(set) var prefs: UserPreferences

    private let defaults: UserDefaults
    private enum Keys {
        static let city = "pref_city"
        static let cityLabel = "pref_city_label"
        static let lat = "pref_lat"
        static let lon = "pref_lon"
        static let units = "pref_units"
    }

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        self.prefs = UserPreferences(
            city: defaults.string(forKey: Keys.city) ?? "",
            cityLabel: defaults.string(forKey: Keys.cityLabel) ?? "",
            lat: defaults.double(forKey: Keys.lat),
            lon: defaults.double(forKey: Keys.lon),
            units: defaults.string(forKey: Keys.units) ?? "metric"
        )
    }

    func saveCity(_ city: String, label: String, lat: Double, lon: Double) {
        defaults.set(city, forKey: Keys.city)
        defaults.set(label, forKey: Keys.cityLabel)
        defaults.set(lat, forKey: Keys.lat)
        defaults.set(lon, forKey: Keys.lon)
        prefs.city = city
        prefs.cityLabel = label
        prefs.lat = lat
        prefs.lon = lon
    }

    func saveUnits(_ units: String) {
        defaults.set(units, forKey: Keys.units)
        prefs.units = units
    }
}
