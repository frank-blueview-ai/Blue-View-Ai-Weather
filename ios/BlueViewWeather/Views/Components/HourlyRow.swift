import SwiftUI

struct HourlyRow: View {
    let hourly: HourlyDto
    let selectedDate: String
    let units: String

    private var sym: String { units == "imperial" ? "°F" : "°C" }
    private var slots: [Int] {
        hourly.time.indices.filter { hourly.time[$0].hasPrefix(selectedDate) }
    }

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 6) {
                ForEach(slots, id: \.self) { i in
                    let timeLabel = hourLabel(from: hourly.time[i])
                    let pop = hourly.precipProb.indices.contains(i) ? (hourly.precipProb[i] ?? 0) : 0
                    let isDay = hourly.isDay.indices.contains(i) ? hourly.isDay[i] == 1 : true

                    VStack(spacing: 2) {
                        Text(timeLabel)
                            .font(.caption2)
                            .foregroundColor(.textSecondary)
                        Text(wmoIcon(hourly.weatherCode[i], isDay: isDay))
                            .font(.system(size: 22))
                        Text("\(Int(hourly.temperature[i]))\(sym)")
                            .font(.subheadline.weight(.semibold))
                            .foregroundColor(.textPrimary)
                        if pop >= 10 {
                            Text("💧\(pop)%")
                                .font(.caption2)
                                .foregroundColor(.rainBlue)
                        }
                    }
                    .frame(width: 56)
                    .padding(.vertical, 8)
                    .padding(.horizontal, 4)
                    .background(Color.navyCard)
                    .overlay(
                        RoundedRectangle(cornerRadius: 8)
                            .stroke(Color.navyBorder, lineWidth: 1)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
        }
    }
}

private func hourLabel(from isoTime: String) -> String {
    let inFormatter = DateFormatter()
    inFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm"
    guard let date = inFormatter.date(from: isoTime) else { return String(isoTime.suffix(5)) }
    let outFormatter = DateFormatter()
    outFormatter.dateFormat = "h a"
    return outFormatter.string(from: date)
}
