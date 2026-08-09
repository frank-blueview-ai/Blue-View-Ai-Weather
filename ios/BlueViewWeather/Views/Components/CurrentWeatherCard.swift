import SwiftUI

struct CurrentWeatherCard: View {
    let city: String
    let current: CurrentDto
    let daily: DailyDto
    let units: String

    private var sym: String { units == "imperial" ? "°F" : "°C" }
    private var spdU: String { units == "imperial" ? "mph" : "km/h" }
    private var isDay: Bool { current.isDay == 1 }
    private var hiLo: String {
        guard !daily.time.isEmpty else { return "" }
        return "↑ \(Int(daily.tempMax[0]))  ↓ \(Int(daily.tempMin[0]))  \(sym)"
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(city)
                .font(.title3)
                .foregroundColor(.blueAccent)

            HStack(alignment: .center, spacing: 16) {
                Text(wmoIcon(current.weatherCode, isDay: isDay))
                    .font(.system(size: 64))

                VStack(alignment: .leading, spacing: 2) {
                    Text("\(Int(current.temperature))\(sym)")
                        .font(.system(size: 44, weight: .regular))
                        .foregroundColor(.textPrimary)
                    Text(wmoDescription(current.weatherCode))
                        .font(.body)
                        .foregroundColor(.textSecondary)
                    if !hiLo.isEmpty {
                        Text(hiLo)
                            .font(.subheadline.weight(.semibold))
                            .foregroundColor(.tempHigh)
                    }
                }
            }

            HStack(alignment: .top, spacing: 16) {
                DetailChip("Feels \(Int(current.apparentTemperature))\(sym)")
                DetailChip("💧 \(current.humidity)%")
                DetailChip("💨 \(Int(current.windSpeed)) \(spdU) \(windDirLabel(current.windDirection))")
                DetailChip("👁 \(Int(current.visibility / 1000)) km")
            }
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 16)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct DetailChip: View {
    let text: String
    init(_ text: String) { self.text = text }
    var body: some View {
        Text(text)
            .font(.subheadline)
            .foregroundColor(.textSecondary)
            .fixedSize(horizontal: false, vertical: true)
    }
}
