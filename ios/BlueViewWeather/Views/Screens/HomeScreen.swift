import SwiftUI

struct HomeScreen: View {
    @StateObject private var viewModel = HomeViewModel()
    @State private var showSearch = false
    @State private var searchQuery = ""
    @State private var showSettings = false
    @State private var showAbout = false

    var body: some View {
        NavigationStack {
            ZStack {
                Color.navyDeep.ignoresSafeArea()

                Group {
                    if viewModel.isLoading && viewModel.forecast == nil {
                        ProgressView()
                            .tint(.blueAccent)
                    } else if viewModel.needsCitySetup {
                        CitySetupPrompt { city in
                            viewModel.searchCity(city)
                            showSearch = false
                        }
                    } else {
                        ScrollView {
                            VStack(spacing: 0) {
                                if let fc = viewModel.forecast {
                                    CurrentWeatherCard(
                                        city: viewModel.cityLabel,
                                        current: fc.current,
                                        daily: fc.daily,
                                        units: viewModel.units
                                    )
                                }

                                CollapsibleSection(
                                    title: "7-Day Forecast",
                                    expanded: viewModel.expandForecast,
                                    onToggle: viewModel.toggleForecast
                                ) {
                                    if let fc = viewModel.forecast {
                                        ForecastRow(
                                            daily: fc.daily,
                                            units: viewModel.units,
                                            selectedDate: viewModel.selectedDate,
                                            onDayClick: viewModel.onDaySelected
                                        )
                                    }

                                    CollapsibleSection(
                                        title: viewModel.selectedDate.map { "Hourly — \($0)" } ?? "Hourly",
                                        expanded: viewModel.expandHourly,
                                        onToggle: viewModel.toggleHourly
                                    ) {
                                        if let fc = viewModel.forecast, let date = viewModel.selectedDate {
                                            HourlyRow(hourly: fc.hourly, selectedDate: date, units: viewModel.units)
                                        }
                                    }
                                }

                                CollapsibleSection(
                                    title: "Radar Map",
                                    expanded: viewModel.expandRadar,
                                    onToggle: viewModel.toggleRadar
                                ) {
                                    if viewModel.lat != 0.0 && viewModel.lon != 0.0 {
                                        RadarWebView(lat: viewModel.lat, lon: viewModel.lon, city: viewModel.cityLabel)
                                            .frame(height: 280)
                                            .frame(maxWidth: .infinity)
                                    }
                                }

                                Spacer(minLength: 16)
                            }
                        }

                        if viewModel.isLoading {
                            VStack {
                                ProgressView()
                                    .tint(.blueAccent)
                                    .frame(maxWidth: .infinity)
                                Spacer()
                            }
                        }
                    }
                }

                if let message = viewModel.error {
                    VStack {
                        Spacer()
                        HStack {
                            Text(message)
                                .foregroundColor(.white)
                            Spacer()
                            Button("Dismiss") { viewModel.dismissError() }
                                .foregroundColor(.white)
                        }
                        .padding()
                        .background(Color.errorRed.opacity(0.9))
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                        .padding(16)
                    }
                }
            }
            .navigationTitle("")
            .toolbar {
                ToolbarItem(placement: .principal) {
                    if showSearch {
                        TextField("City name…", text: $searchQuery)
                            .textFieldStyle(.roundedBorder)
                            .frame(minWidth: 180)
                    } else {
                        Text("🌤  Blue View Weather")
                            .font(.headline)
                            .foregroundColor(.blueAccent)
                    }
                }
                ToolbarItemGroup(placement: .navigationBarTrailing) {
                    if showSearch {
                        Button {
                            if !searchQuery.trimmingCharacters(in: .whitespaces).isEmpty {
                                viewModel.searchCity(searchQuery)
                                searchQuery = ""
                            }
                            showSearch = false
                        } label: {
                            Image(systemName: "checkmark")
                        }
                        .tint(.textSecondary)
                    } else {
                        Button { showSearch = true } label: {
                            Image(systemName: "magnifyingglass")
                        }
                        .tint(.textSecondary)

                        Button { viewModel.refresh() } label: {
                            Image(systemName: "arrow.clockwise")
                        }
                        .tint(.textSecondary)

                        Button { showSettings = true } label: {
                            Image(systemName: "gearshape")
                        }
                        .tint(.textSecondary)

                        Button { showAbout = true } label: {
                            Image(systemName: "info.circle")
                        }
                        .tint(.textSecondary)
                    }
                }
            }
            .toolbarBackground(Color.navyDeep, for: .navigationBar)
            .toolbarBackground(.visible, for: .navigationBar)
            .sheet(isPresented: $showSettings) { SettingsScreen() }
            .sheet(isPresented: $showAbout) { AboutScreen() }
        }
        .tint(.blueAccent)
        .preferredColorScheme(.dark)
    }
}

private struct CitySetupPrompt: View {
    let onSearch: (String) -> Void
    @State private var query = ""

    var body: some View {
        VStack(spacing: 16) {
            Text("🌤").font(.system(size: 56))
            Text("Welcome to Blue View Weather")
                .font(.title2.weight(.semibold))
                .foregroundColor(.textPrimary)
                .multilineTextAlignment(.center)
            Text("Enter your city to get started.\nNo API key needed.")
                .font(.body)
                .foregroundColor(.textSecondary)
                .multilineTextAlignment(.center)

            TextField("e.g. Miami, London, Tokyo", text: $query)
                .textFieldStyle(.roundedBorder)
                .autocorrectionDisabled()

            Button {
                if !query.trimmingCharacters(in: .whitespaces).isEmpty { onSearch(query) }
            } label: {
                Text("Get Weather")
                    .foregroundColor(.navyDeep)
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .tint(.blueAccent)
        }
        .padding(32)
    }
}
