import Foundation

struct WeatherInfo {
    let dayIcon: String
    let nightIcon: String
    let description: String
}

private let wmoMap: [Int: WeatherInfo] = [
    0:  WeatherInfo(dayIcon: "☀️", nightIcon: "🌙", description: "Clear Sky"),
    1:  WeatherInfo(dayIcon: "🌤️", nightIcon: "🌙", description: "Mainly Clear"),
    2:  WeatherInfo(dayIcon: "⛅", nightIcon: "⛅", description: "Partly Cloudy"),
    3:  WeatherInfo(dayIcon: "☁️", nightIcon: "☁️", description: "Overcast"),
    45: WeatherInfo(dayIcon: "🌫️", nightIcon: "🌫️", description: "Fog"),
    48: WeatherInfo(dayIcon: "🌫️", nightIcon: "🌫️", description: "Freezing Fog"),
    51: WeatherInfo(dayIcon: "🌦️", nightIcon: "🌧️", description: "Light Drizzle"),
    53: WeatherInfo(dayIcon: "🌦️", nightIcon: "🌧️", description: "Drizzle"),
    55: WeatherInfo(dayIcon: "🌧️", nightIcon: "🌧️", description: "Heavy Drizzle"),
    61: WeatherInfo(dayIcon: "🌧️", nightIcon: "🌧️", description: "Light Rain"),
    63: WeatherInfo(dayIcon: "🌧️", nightIcon: "🌧️", description: "Rain"),
    65: WeatherInfo(dayIcon: "🌧️", nightIcon: "🌧️", description: "Heavy Rain"),
    66: WeatherInfo(dayIcon: "🌧️", nightIcon: "🌧️", description: "Light Freezing Rain"),
    67: WeatherInfo(dayIcon: "🌧️", nightIcon: "🌧️", description: "Freezing Rain"),
    71: WeatherInfo(dayIcon: "❄️", nightIcon: "❄️", description: "Light Snow"),
    73: WeatherInfo(dayIcon: "❄️", nightIcon: "❄️", description: "Snow"),
    75: WeatherInfo(dayIcon: "❄️", nightIcon: "❄️", description: "Heavy Snow"),
    77: WeatherInfo(dayIcon: "❄️", nightIcon: "❄️", description: "Snow Grains"),
    80: WeatherInfo(dayIcon: "🌦️", nightIcon: "🌧️", description: "Light Showers"),
    81: WeatherInfo(dayIcon: "🌦️", nightIcon: "🌧️", description: "Showers"),
    82: WeatherInfo(dayIcon: "🌧️", nightIcon: "🌧️", description: "Heavy Showers"),
    85: WeatherInfo(dayIcon: "❄️", nightIcon: "❄️", description: "Snow Showers"),
    86: WeatherInfo(dayIcon: "❄️", nightIcon: "❄️", description: "Heavy Snow Showers"),
    95: WeatherInfo(dayIcon: "⛈️", nightIcon: "⛈️", description: "Thunderstorm"),
    96: WeatherInfo(dayIcon: "⛈️", nightIcon: "⛈️", description: "Thunderstorm + Hail"),
    99: WeatherInfo(dayIcon: "⛈️", nightIcon: "⛈️", description: "Thunderstorm + Heavy Hail"),
]

func wmoIcon(_ code: Int, isDay: Bool) -> String {
    let info = wmoMap[code] ?? wmoMap[3]!
    return isDay ? info.dayIcon : info.nightIcon
}

func wmoDescription(_ code: Int) -> String {
    wmoMap[code]?.description ?? "Unknown"
}

let windDirections = ["N", "NE", "E", "SE", "S", "SW", "W", "NW"]
func windDirLabel(_ degrees: Double) -> String {
    windDirections[Int((degrees + 22.5) / 45) % 8]
}
