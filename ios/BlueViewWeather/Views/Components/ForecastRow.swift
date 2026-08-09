import SwiftUI

struct ForecastRow: View {
    let daily: DailyDto
    let units: String
    let selectedDate: String?
    let onDayClick: (String) -> Void

    private var sym: String { units == "imperial" ? "°F" : "°C" }
    private var today: String { isoDateString(Date()) }

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(daily.time.indices, id: \.self) { i in
                    let date = daily.time[i]
                    let selected = date == selectedDate
                    let dayLabel = date == today ? "Today" : shortWeekday(from: date)

                    DayCard(
                        day: dayLabel,
                        icon: wmoIcon(daily.weatherCode[i], isDay: true),
                        hi: "\(Int(daily.tempMax[i]))\(sym)",
                        lo: "\(Int(daily.tempMin[i]))\(sym)",
                        pop: daily.precipProbMax.indices.contains(i) ? (daily.precipProbMax[i] ?? 0) : 0,
                        selected: selected,
                        onClick: { onDayClick(date) }
                    )
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
        }
    }
}

private struct DayCard: View {
    let day: String
    let icon: String
    let hi: String
    let lo: String
    let pop: Int
    let selected: Bool
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            VStack(spacing: 3) {
                Text(day)
                    .font(.caption2)
                    .foregroundColor(.textSecondary)
                    .lineLimit(1)
                Text(icon)
                    .font(.system(size: 26))
                Text(hi)
                    .font(.subheadline.weight(.bold))
                    .foregroundColor(.tempHigh)
                Text(lo)
                    .font(.subheadline)
                    .foregroundColor(.tempLow)
                if pop >= 10 {
                    Text("💧\(pop)%")
                        .font(.caption2)
                        .foregroundColor(.rainBlue)
                }
            }
            .frame(width: 60)
            .padding(.vertical, 10)
            .padding(.horizontal, 4)
            .background(selected ? Color.blueAccent.opacity(0.15) : Color.navyCard)
            .overlay(
                RoundedRectangle(cornerRadius: 10)
                    .stroke(selected ? Color.blueAccent.opacity(0.5) : Color.navyBorder, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 10))
        }
        .buttonStyle(.plain)
    }
}

func isoDateString(_ date: Date) -> String {
    let formatter = DateFormatter()
    formatter.dateFormat = "yyyy-MM-dd"
    formatter.calendar = Calendar(identifier: .gregorian)
    formatter.timeZone = TimeZone.current
    return formatter.string(from: date)
}

func shortWeekday(from isoDate: String) -> String {
    let inFormatter = DateFormatter()
    inFormatter.dateFormat = "yyyy-MM-dd"
    guard let date = inFormatter.date(from: isoDate) else { return isoDate }
    let outFormatter = DateFormatter()
    outFormatter.dateFormat = "EEE"
    return outFormatter.string(from: date)
}
