import SwiftUI

struct SettingsScreen: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject private var prefsStore = UserPreferencesStore.shared
    @State private var cityInput: String = ""

    var body: some View {
        NavigationStack {
            ZStack {
                Color.navyDeep.ignoresSafeArea()

                VStack(alignment: .leading, spacing: 24) {
                    Text("City")
                        .font(.title3)
                        .foregroundColor(.blueAccent)
                    TextField("City name", text: $cityInput)
                        .textFieldStyle(.roundedBorder)
                        .autocorrectionDisabled()

                    Button("Update City") {
                        let trimmed = cityInput.trimmingCharacters(in: .whitespaces)
                        guard !trimmed.isEmpty else { return }
                        Task {
                            switch await WeatherRepository.shared.geocode(query: trimmed) {
                            case .success(let result):
                                let label = "\(result.name), \(result.countryCode ?? "")"
                                prefsStore.saveCity(trimmed, label: label, lat: result.latitude, lon: result.longitude)
                            case .failure:
                                break
                            }
                        }
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(.blueAccent)
                    .foregroundColor(.navyDeep)

                    Divider().background(Color.textMuted.opacity(0.3))

                    Text("Units")
                        .font(.title3)
                        .foregroundColor(.blueAccent)

                    ForEach([("metric", "Metric (°C, km/h)"), ("imperial", "Imperial (°F, mph)")], id: \.0) { value, label in
                        Button {
                            prefsStore.saveUnits(value)
                        } label: {
                            HStack {
                                Image(systemName: prefsStore.prefs.units == value ? "largecircle.fill.circle" : "circle")
                                    .foregroundColor(.blueAccent)
                                Text(label)
                                    .foregroundColor(.textPrimary)
                                Spacer()
                            }
                        }
                        .buttonStyle(.plain)
                    }

                    Spacer()
                }
                .padding(24)
            }
            .onAppear { cityInput = prefsStore.prefs.city }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button { dismiss() } label: {
                        Image(systemName: "chevron.left")
                    }
                    .tint(.textSecondary)
                }
            }
            .toolbarBackground(Color.navyDeep, for: .navigationBar)
            .toolbarBackground(.visible, for: .navigationBar)
        }
        .preferredColorScheme(.dark)
    }
}
