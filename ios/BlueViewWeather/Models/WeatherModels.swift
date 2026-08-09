import Foundation

struct GeocodingResponse: Decodable {
    let results: [GeocodingResult]?
}

struct GeocodingResult: Decodable {
    let name: String
    let latitude: Double
    let longitude: Double
    let countryCode: String?
    let country: String?
    let admin1: String?

    enum CodingKeys: String, CodingKey {
        case name, latitude, longitude, country, admin1
        case countryCode = "country_code"
    }
}

struct ForecastResponse: Decodable {
    let current: CurrentDto
    let hourly: HourlyDto
    let daily: DailyDto
}

struct CurrentDto: Decodable {
    let temperature: Double
    let apparentTemperature: Double
    let weatherCode: Int
    let windSpeed: Double
    let windDirection: Double
    let humidity: Int
    let visibility: Double
    let isDay: Int

    enum CodingKeys: String, CodingKey {
        case temperature = "temperature_2m"
        case apparentTemperature = "apparent_temperature"
        case weatherCode = "weather_code"
        case windSpeed = "wind_speed_10m"
        case windDirection = "wind_direction_10m"
        case humidity = "relative_humidity_2m"
        case visibility
        case isDay = "is_day"
    }
}

struct HourlyDto: Decodable {
    let time: [String]
    let temperature: [Double]
    let weatherCode: [Int]
    let precipProb: [Int?]
    let windSpeed: [Double]
    let isDay: [Int]

    enum CodingKeys: String, CodingKey {
        case time
        case temperature = "temperature_2m"
        case weatherCode = "weather_code"
        case precipProb = "precipitation_probability"
        case windSpeed = "wind_speed_10m"
        case isDay = "is_day"
    }
}

struct DailyDto: Decodable {
    let time: [String]
    let weatherCode: [Int]
    let tempMax: [Double]
    let tempMin: [Double]
    let precipProbMax: [Int?]

    enum CodingKeys: String, CodingKey {
        case time
        case weatherCode = "weather_code"
        case tempMax = "temperature_2m_max"
        case tempMin = "temperature_2m_min"
        case precipProbMax = "precipitation_probability_max"
    }
}
