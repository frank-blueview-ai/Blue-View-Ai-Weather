import Foundation

enum WeatherAPIError: Error, LocalizedError {
    case network(Error)
    case decoding(Error)
    case badResponse
    case noResults

    var errorDescription: String? {
        switch self {
        case .network(let e):   return "Network error: \(e.localizedDescription)"
        case .decoding(let e):  return "Failed to parse response: \(e.localizedDescription)"
        case .badResponse:      return "Unexpected server response"
        case .noResults:        return "City not found"
        }
    }
}

/// Mirrors the Android WeatherService/GeocodingService (Retrofit) — Open-Meteo, no API key.
final class WeatherAPI {
    static let shared = WeatherAPI()

    private let geocodingBase = "https://geocoding-api.open-meteo.com/v1/search"
    private let forecastBase  = "https://api.open-meteo.com/v1/forecast"

    private static let currentParams = "temperature_2m,apparent_temperature,weather_code," +
        "wind_speed_10m,wind_direction_10m,relative_humidity_2m,visibility,is_day"
    private static let hourlyParams  = "temperature_2m,precipitation_probability," +
        "weather_code,wind_speed_10m,is_day"
    private static let dailyParams   = "weather_code,temperature_2m_max,temperature_2m_min," +
        "precipitation_probability_max"

    private let session: URLSession
    private let decoder = JSONDecoder()

    private init(session: URLSession = .shared) {
        self.session = session
    }

    func geocode(name: String) async throws -> GeocodingResult {
        var components = URLComponents(string: geocodingBase)!
        components.queryItems = [
            URLQueryItem(name: "name", value: name),
            URLQueryItem(name: "count", value: "1"),
            URLQueryItem(name: "language", value: "en"),
            URLQueryItem(name: "format", value: "json"),
        ]
        let response: GeocodingResponse = try await fetch(url: components.url!)
        guard let result = response.results?.first else { throw WeatherAPIError.noResults }
        return result
    }

    func forecast(latitude: Double, longitude: Double, units: String) async throws -> ForecastResponse {
        let isImperial = units == "imperial"
        var components = URLComponents(string: forecastBase)!
        components.queryItems = [
            URLQueryItem(name: "latitude", value: String(latitude)),
            URLQueryItem(name: "longitude", value: String(longitude)),
            URLQueryItem(name: "current", value: Self.currentParams),
            URLQueryItem(name: "hourly", value: Self.hourlyParams),
            URLQueryItem(name: "daily", value: Self.dailyParams),
            URLQueryItem(name: "temperature_unit", value: isImperial ? "fahrenheit" : "celsius"),
            URLQueryItem(name: "wind_speed_unit", value: isImperial ? "mph" : "kmh"),
            URLQueryItem(name: "timezone", value: "auto"),
            URLQueryItem(name: "forecast_days", value: "7"),
        ]
        return try await fetch(url: components.url!)
    }

    private func fetch<T: Decodable>(url: URL) async throws -> T {
        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await session.data(from: url)
        } catch {
            throw WeatherAPIError.network(error)
        }
        guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
            throw WeatherAPIError.badResponse
        }
        do {
            return try decoder.decode(T.self, from: data)
        } catch {
            throw WeatherAPIError.decoding(error)
        }
    }
}
