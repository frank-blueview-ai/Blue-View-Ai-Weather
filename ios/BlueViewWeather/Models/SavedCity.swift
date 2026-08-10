import Foundation

struct SavedCity: Codable, Equatable, Identifiable {
    static let currentLocationId = "current-location"

    let id: String
    var name: String
    var label: String
    var lat: Double
    var lon: Double
    var isCurrentLocation: Bool

    init(id: String = UUID().uuidString,
         name: String,
         label: String,
         lat: Double,
         lon: Double,
         isCurrentLocation: Bool = false) {
        self.id = id
        self.name = name
        self.label = label
        self.lat = lat
        self.lon = lon
        self.isCurrentLocation = isCurrentLocation
    }

    /// The pinned first entry. Its id is a constant so it survives re-encoding and
    /// can be identified without scanning coordinates.
    static func currentLocation(label: String, lat: Double, lon: Double) -> SavedCity {
        SavedCity(id: currentLocationId,
                  name: label,
                  label: label,
                  lat: lat,
                  lon: lon,
                  isCurrentLocation: true)
    }
}
