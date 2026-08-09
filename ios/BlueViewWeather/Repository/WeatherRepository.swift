import Foundation

enum WeatherResult<T> {
    case success(T)
    case failure(String)
}

/// Thin wrapper over WeatherAPI, mirrors Android WeatherRepository — maps errors to display strings.
final class WeatherRepository {
    static let shared = WeatherRepository()
    private let api: WeatherAPI

    init(api: WeatherAPI = .shared) {
        self.api = api
    }

    func forecast(latitude: Double, longitude: Double, units: String) async -> WeatherResult<ForecastResponse> {
        do {
            let response = try await api.forecast(latitude: latitude, longitude: longitude, units: units)
            return .success(response)
        } catch {
            return .failure((error as? WeatherAPIError)?.errorDescription ?? error.localizedDescription)
        }
    }

    func geocode(query: String) async -> WeatherResult<GeocodingResult> {
        do {
            let result = try await api.geocode(name: query)
            return .success(result)
        } catch {
            return .failure((error as? WeatherAPIError)?.errorDescription ?? error.localizedDescription)
        }
    }
}
