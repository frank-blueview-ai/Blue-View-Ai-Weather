import SwiftUI

struct SettingsScreen: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject private var prefsStore = UserPreferencesStore.shared
    @StateObject private var locationManager = LocationManager()
    @State private var cityInput = ""
    @State private var isAdding = false
    @State private var addError: String?

    var body: some View {
        NavigationStack {
            ZStack {
                Color.navyDeep.ignoresSafeArea()

                ScrollView {
                    VStack(alignment: .leading, spacing: 20) {
                        locationSection
                        Divider().background(Color.textMuted.opacity(0.3))
                        citiesSection
                        Divider().background(Color.textMuted.opacity(0.3))
                        unitsSection
                    }
                    .padding(24)
                }
            }
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

    // MARK: - Sections

    private var locationSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Location")
                .font(.title3)
                .foregroundColor(.blueAccent)

            Toggle(isOn: locationBinding) {
                Text("Use current location")
                    .foregroundColor(.textPrimary)
            }
            .tint(.blueAccent)

            if locationManager.authorizationStatus == .denied || locationManager.authorizationStatus == .restricted {
                Text("Location access is off. Enable it in iOS Settings to pin your current location.")
                    .font(.caption)
                    .foregroundColor(.textMuted)
            }
        }
    }

    private var citiesSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Cities")
                .font(.title3)
                .foregroundColor(.blueAccent)

            if prefsStore.prefs.cities.isEmpty {
                Text("No cities yet — add one below.")
                    .font(.subheadline)
                    .foregroundColor(.textMuted)
            }

            ForEach(prefsStore.prefs.cities) { city in
                CityRow(city: city) { prefsStore.removeCity(id: city.id) }
            }

            HStack {
                TextField("Add a city", text: $cityInput)
                    .textFieldStyle(.roundedBorder)
                    .autocorrectionDisabled()
                    .onSubmit { addCity() }
                Button(isAdding ? "Adding…" : "Add") { addCity() }
                    .buttonStyle(.borderedProminent)
                    .tint(.blueAccent)
                    .foregroundColor(.navyDeep)
                    .disabled(isAdding)
            }
            .padding(.top, 4)

            if let addError = addError {
                Text(addError)
                    .font(.caption)
                    .foregroundColor(.errorRed)
            }
        }
    }

    private var unitsSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Units")
                .font(.title3)
                .foregroundColor(.blueAccent)

            ForEach([("imperial", "Imperial (°F, mph)"), ("metric", "Metric (°C, km/h)")], id: \.0) { value, label in
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
        }
    }

    // MARK: - Actions

    private var locationBinding: Binding<Bool> {
        Binding(
            get: { prefsStore.prefs.locationEnabled },
            set: { enabled in
                if enabled {
                    Task {
                        if let fix = await locationManager.resolveCurrentCity() {
                            prefsStore.setLocationEnabled(true)
                            prefsStore.updateCurrentLocation(label: fix.label, lat: fix.lat, lon: fix.lon)
                        }
                    }
                } else {
                    prefsStore.setLocationEnabled(false)
                }
            }
        )
    }

    private func addCity() {
        let trimmed = cityInput.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty, !isAdding else { return }
        isAdding = true
        addError = nil
        Task {
            switch await WeatherRepository.shared.geocode(query: trimmed) {
            case .success(let result):
                let label = "\(result.name), \(result.countryCode ?? "")"
                prefsStore.addCity(SavedCity(name: result.name,
                                             label: label,
                                             lat: result.latitude,
                                             lon: result.longitude))
                cityInput = ""
            case .failure(let message):
                addError = message
            }
            isAdding = false
        }
    }
}

private struct CityRow: View {
    let city: SavedCity
    let onDelete: () -> Void

    var body: some View {
        HStack {
            if city.isCurrentLocation {
                Image(systemName: "location.fill")
                    .font(.caption)
                    .foregroundColor(.blueAccent)
            }
            Text(city.label)
                .foregroundColor(.textPrimary)
            Spacer()
            if city.isCurrentLocation {
                Text("Pinned")
                    .font(.caption)
                    .foregroundColor(.textMuted)
            } else {
                Button(action: onDelete) {
                    Image(systemName: "trash")
                        .foregroundColor(.errorRed)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.vertical, 6)
        .padding(.horizontal, 12)
        .background(Color.navyCard)
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }
}
