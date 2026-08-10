import Foundation
import CoreLocation
import Combine

/// One-shot location + reverse geocoding for the pinned "Current Location" entry.
/// Every path resolves — a caller awaiting this never hangs on a denied prompt or a
/// CoreLocation callback that simply never arrives.
@MainActor
final class LocationManager: NSObject, ObservableObject {
    @Published private(set) var authorizationStatus: CLAuthorizationStatus = .notDetermined
    @Published private(set) var isResolving = false

    private let manager = CLLocationManager()
    private let geocoder = CLGeocoder()
    private var locationContinuation: CheckedContinuation<CLLocation?, Never>?
    private var authorizationContinuation: CheckedContinuation<Void, Never>?

    private let locationTimeout: UInt64 = 15 * NSEC_PER_SEC
    private let authorizationTimeout: UInt64 = 60 * NSEC_PER_SEC

    override init() {
        super.init()
        manager.delegate = self
        // Weather is a city-level product; kilometre accuracy fixes far faster and
        // costs a fraction of the battery of the best-accuracy modes.
        manager.desiredAccuracy = kCLLocationAccuracyKilometer
        authorizationStatus = manager.authorizationStatus
    }

    var isAuthorized: Bool {
        authorizationStatus == .authorizedWhenInUse || authorizationStatus == .authorizedAlways
    }

    func requestPermission() {
        guard manager.authorizationStatus == .notDetermined else { return }
        manager.requestWhenInUseAuthorization()
    }

    /// Returns nil when permission is unavailable or the fix fails; never throws.
    func resolveCurrentCity() async -> (label: String, lat: Double, lon: Double)? {
        if authorizationStatus == .notDetermined {
            manager.requestWhenInUseAuthorization()
            await awaitAuthorizationDecision()
        }
        guard isAuthorized else { return nil }

        isResolving = true
        defer { isResolving = false }

        guard let location = await requestLocation() else { return nil }
        let label = await reverseGeocodedLabel(for: location)
        return (label, location.coordinate.latitude, location.coordinate.longitude)
    }

    // MARK: - Continuations

    private func requestLocation() async -> CLLocation? {
        // Only one outstanding fix; a second caller would strand the first continuation.
        guard locationContinuation == nil else { return nil }
        return await withCheckedContinuation { continuation in
            locationContinuation = continuation
            manager.requestLocation()
            Task { @MainActor [weak self] in
                try? await Task.sleep(nanoseconds: self?.locationTimeout ?? 15 * NSEC_PER_SEC)
                self?.finishLocation(with: nil)
            }
        }
    }

    private func awaitAuthorizationDecision() async {
        guard authorizationContinuation == nil else { return }
        await withCheckedContinuation { (continuation: CheckedContinuation<Void, Never>) in
            authorizationContinuation = continuation
            Task { @MainActor [weak self] in
                try? await Task.sleep(nanoseconds: self?.authorizationTimeout ?? 60 * NSEC_PER_SEC)
                self?.finishAuthorization()
            }
        }
    }

    /// CoreLocation can report success and then failure (or fire twice) for the same
    /// request; clearing the stored continuation first makes a double resume impossible.
    private func finishLocation(with location: CLLocation?) {
        guard let continuation = locationContinuation else { return }
        locationContinuation = nil
        continuation.resume(returning: location)
    }

    private func finishAuthorization() {
        guard let continuation = authorizationContinuation else { return }
        authorizationContinuation = nil
        continuation.resume()
    }

    private func handleAuthorizationChange(_ status: CLAuthorizationStatus) {
        authorizationStatus = status
        guard status != .notDetermined else { return }
        finishAuthorization()
        if status == .denied || status == .restricted {
            finishLocation(with: nil)
        }
    }

    // MARK: - Geocoding

    private func reverseGeocodedLabel(for location: CLLocation) async -> String {
        let placemarks = try? await geocoder.reverseGeocodeLocation(location)
        guard let placemark = placemarks?.first else { return "Current Location" }
        let name = placemark.locality ?? placemark.administrativeArea ?? placemark.country
        guard let name = name, !name.isEmpty else { return "Current Location" }
        // Matches the geocoding search format used elsewhere: "Miami, US".
        if let code = placemark.isoCountryCode, !code.isEmpty {
            return "\(name), \(code)"
        }
        return name
    }
}

extension LocationManager: CLLocationManagerDelegate {
    nonisolated func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        let location = locations.last
        Task { @MainActor [weak self] in self?.finishLocation(with: location) }
    }

    nonisolated func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        Task { @MainActor [weak self] in self?.finishLocation(with: nil) }
    }

    nonisolated func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        let status = manager.authorizationStatus
        Task { @MainActor [weak self] in self?.handleAuthorizationChange(status) }
    }
}
