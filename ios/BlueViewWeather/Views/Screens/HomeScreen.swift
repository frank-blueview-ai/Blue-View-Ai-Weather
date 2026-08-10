import SwiftUI

struct HomeScreen: View {
    @StateObject private var viewModel = HomeViewModel()
    @State private var showSearch = false
    @State private var searchQuery = ""
    @State private var showSettings = false
    @State private var showAbout = false
    @State private var confirmDelete = false

    var body: some View {
        NavigationStack {
            ZStack {
                Color.navyDeep.ignoresSafeArea()

                if viewModel.needsCitySetup {
                    CitySetupPrompt(isBusy: viewModel.isSearching, error: viewModel.searchError) { city in
                        viewModel.addCity(city)
                    }
                } else {
                    pages
                }
            }
            .navigationTitle("")
            .toolbar {
                ToolbarItem(placement: .principal) { titleArea }
                ToolbarItemGroup(placement: .navigationBarTrailing) { trailingButtons }
            }
            .toolbarBackground(Color.navyDeep, for: .navigationBar)
            .toolbarBackground(.visible, for: .navigationBar)
            .sheet(isPresented: $showSettings) { SettingsScreen() }
            .sheet(isPresented: $showAbout) { AboutScreen() }
            .confirmationDialog("Remove this city?",
                                isPresented: $confirmDelete,
                                titleVisibility: .visible) {
                Button("Remove \(viewModel.selectedCity?.label ?? "")", role: .destructive) {
                    viewModel.removeSelectedCity()
                }
                Button("Cancel", role: .cancel) {}
            }
            .onAppear { viewModel.onAppear() }
        }
        .tint(.blueAccent)
        .preferredColorScheme(.dark)
    }

    private var pages: some View {
        TabView(selection: $viewModel.selectedIndex) {
            ForEach(Array(viewModel.cities.enumerated()), id: \.element.id) { index, city in
                CityWeatherPage(city: city, viewModel: viewModel)
                    .tag(index)
            }
        }
        .tabViewStyle(.page(indexDisplayMode: .always))
        .indexViewStyle(.page(backgroundDisplayMode: .always))
        .onChange(of: viewModel.selectedIndex) { _ in
            viewModel.loadSelectedIfNeeded()
        }
    }

    private var titleArea: some View {
        Group {
            if showSearch {
                TextField("Add a city…", text: $searchQuery)
                    .textFieldStyle(.roundedBorder)
                    .autocorrectionDisabled()
                    .frame(minWidth: 180)
                    .onSubmit { commitSearch() }
            } else {
                VStack(spacing: 1) {
                    HStack(spacing: 4) {
                        if viewModel.selectedCity?.isCurrentLocation == true {
                            Image(systemName: "location.fill")
                                .font(.caption2)
                                .foregroundColor(.blueAccent)
                        }
                        Text(viewModel.selectedCity?.label ?? "Blue View Weather")
                            .font(.headline)
                            .foregroundColor(.blueAccent)
                            .lineLimit(1)
                    }
                    if viewModel.cities.count > 1 {
                        Text("\(viewModel.selectedIndex + 1) of \(viewModel.cities.count)")
                            .font(.caption2)
                            .foregroundColor(.textMuted)
                    }
                }
            }
        }
    }

    private var trailingButtons: some View {
        Group {
            if showSearch {
                Button { commitSearch() } label: {
                    Image(systemName: "checkmark")
                }
                .tint(.textSecondary)
            } else {
                Button { showSearch = true } label: {
                    Image(systemName: "plus.magnifyingglass")
                }
                .tint(.textSecondary)

                Button { viewModel.refresh() } label: {
                    Image(systemName: "arrow.clockwise")
                }
                .tint(.textSecondary)

                Button { confirmDelete = true } label: {
                    Image(systemName: "trash")
                }
                .tint(.textSecondary)
                .disabled(viewModel.selectedCity == nil || viewModel.selectedCity?.isCurrentLocation == true)

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

    private func commitSearch() {
        let trimmed = searchQuery.trimmingCharacters(in: .whitespaces)
        if !trimmed.isEmpty {
            viewModel.addCity(trimmed)
            searchQuery = ""
        }
        showSearch = false
    }
}

/// One full-screen page. Extracted so the paging body stays small and every
/// container here stays well under the 10-child ViewBuilder limit.
private struct CityWeatherPage: View {
    let city: SavedCity
    @ObservedObject var viewModel: HomeViewModel

    var body: some View {
        let state = viewModel.state(for: city.id)
        return ZStack {
            Color.navyDeep.ignoresSafeArea()

            ScrollView {
                VStack(spacing: 0) {
                    if let fc = state.forecast {
                        CurrentWeatherCard(
                            city: city.label,
                            current: fc.current,
                            daily: fc.daily,
                            units: viewModel.units
                        )
                    }
                    forecastSection(state)
                    radarSection(state)
                    Spacer(minLength: 48)
                }
            }

            if state.isLoading && state.forecast == nil {
                ProgressView().tint(.blueAccent)
            } else if state.isLoading {
                VStack {
                    ProgressView()
                        .tint(.blueAccent)
                        .frame(maxWidth: .infinity)
                    Spacer()
                }
            }

            if let message = state.error {
                errorBanner(message)
            }
        }
    }

    private func forecastSection(_ state: CityWeatherState) -> some View {
        CollapsibleSection(
            title: "7-Day Forecast",
            expanded: state.expandForecast,
            onToggle: { viewModel.toggleForecast(city.id) }
        ) {
            if let fc = state.forecast {
                ForecastRow(
                    daily: fc.daily,
                    units: viewModel.units,
                    selectedDate: state.selectedDate,
                    onDayClick: { date in viewModel.onDaySelected(date, cityId: city.id) }
                )
            }

            CollapsibleSection(
                title: state.selectedDate.map { "Hourly — \($0)" } ?? "Hourly",
                expanded: state.expandHourly,
                onToggle: { viewModel.toggleHourly(city.id) }
            ) {
                if let fc = state.forecast, let date = state.selectedDate {
                    HourlyRow(hourly: fc.hourly, selectedDate: date, units: viewModel.units)
                }
            }
        }
    }

    private func radarSection(_ state: CityWeatherState) -> some View {
        CollapsibleSection(
            title: "Radar Map",
            expanded: state.expandRadar,
            onToggle: { viewModel.toggleRadar(city.id) }
        ) {
            if city.lat != 0.0 || city.lon != 0.0 {
                RadarWebView(lat: city.lat, lon: city.lon, city: city.label)
                    .frame(height: 280)
                    .frame(maxWidth: .infinity)
            }
        }
    }

    private func errorBanner(_ message: String) -> some View {
        VStack {
            Spacer()
            HStack {
                Text(message)
                    .foregroundColor(.white)
                Spacer()
                Button("Dismiss") { viewModel.dismissError(city.id) }
                    .foregroundColor(.white)
            }
            .padding()
            .background(Color.errorRed.opacity(0.9))
            .clipShape(RoundedRectangle(cornerRadius: 8))
            .padding(.horizontal, 16)
            .padding(.bottom, 48)
        }
    }
}

private struct CitySetupPrompt: View {
    let isBusy: Bool
    let error: String?
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
                Text(isBusy ? "Searching…" : "Get Weather")
                    .foregroundColor(.navyDeep)
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .tint(.blueAccent)
            .disabled(isBusy)

            if let error = error {
                Text(error)
                    .font(.subheadline)
                    .foregroundColor(.errorRed)
                    .multilineTextAlignment(.center)
            }
        }
        .padding(32)
    }
}
