import Foundation

enum UpdateState: Equatable {
    case idle
    case checking
    case upToDate
    case available(version: String)
    case failed(String)
}

private struct GitHubRelease: Decodable {
    let tagName: String
    let draft: Bool?
    let prerelease: Bool?

    enum CodingKeys: String, CodingKey {
        case tagName = "tag_name"
        case draft
        case prerelease
    }
}

/// Mirrors the Android UpdateChecker, minus the download/install half: iOS cannot
/// self-install, so the newest ios-v tag only routes the user to TestFlight.
@MainActor
final class UpdateChecker: ObservableObject {
    @Published private(set) var state: UpdateState = .idle

    // The monorepo releases all three platforms on their own tag prefixes, so
    // /releases/latest is unusable here — it could return an android- or desktop- tag.
    private static let releasesURL =
        "https://api.github.com/repos/frank-blueview-ai/Blue-View-Ai-Weather/releases?per_page=30"
    private static let tagPrefix = "ios-v"

    static let testFlightURL = URL(string: "itms-beta://testflight.apple.com/")!
    static let testFlightFallbackURL = URL(string: "https://apps.apple.com/app/testflight/id899247664")!

    static var currentVersion: String {
        Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "0.0.0"
    }

    private let session: URLSession
    private let decoder = JSONDecoder()

    init(session: URLSession = .shared) {
        self.session = session
    }

    func check() async {
        state = .checking

        var request = URLRequest(url: URL(string: Self.releasesURL)!)
        request.setValue("application/vnd.github+json", forHTTPHeaderField: "Accept")

        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await session.data(for: request)
        } catch {
            state = .failed("Network error: \(error.localizedDescription)")
            return
        }

        guard let http = response as? HTTPURLResponse else {
            state = .failed("Unexpected server response")
            return
        }
        guard (200..<300).contains(http.statusCode) else {
            // 403 here is almost always the unauthenticated hourly rate limit.
            state = .failed(http.statusCode == 403
                ? "GitHub rate limit reached — try again later"
                : "Update check failed (HTTP \(http.statusCode))")
            return
        }

        let releases: [GitHubRelease]
        do {
            releases = try decoder.decode([GitHubRelease].self, from: data)
        } catch {
            state = .failed("Failed to parse release list")
            return
        }

        // The API's ordering is not guaranteed to be semver order, so pick the max.
        let versions = releases
            .filter { $0.tagName.hasPrefix(Self.tagPrefix) && $0.draft != true && $0.prerelease != true }
            .map { String($0.tagName.dropFirst(Self.tagPrefix.count)) }

        guard let latest = versions.max(by: { Self.isNewer($1, than: $0) }) else {
            state = .failed("No iOS release found")
            return
        }

        state = Self.isNewer(latest, than: Self.currentVersion) ? .available(version: latest) : .upToDate
    }

    /// Numeric per-component compare — "1.0.10" is newer than "1.0.9". Non-numeric
    /// components count as 0 so malformed tags degrade instead of throwing.
    static func isNewer(_ candidate: String, than current: String) -> Bool {
        let lhs = components(of: candidate)
        let rhs = components(of: current)
        for i in 0..<max(lhs.count, rhs.count) {
            let l = i < lhs.count ? lhs[i] : 0
            let r = i < rhs.count ? rhs[i] : 0
            if l != r { return l > r }
        }
        return false
    }

    private static func components(of version: String) -> [Int] {
        version
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .drop(while: { $0 == "v" || $0 == "V" })
            .split(separator: ".")
            .map { Int($0.prefix(while: { $0.isNumber })) ?? 0 }
    }
}
