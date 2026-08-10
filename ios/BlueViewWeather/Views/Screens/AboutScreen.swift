import SwiftUI

struct AboutScreen: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL
    @StateObject private var updateChecker = UpdateChecker()

    var body: some View {
        NavigationStack {
            ZStack {
                Color.navyDeep.ignoresSafeArea()

                VStack(spacing: 12) {
                    Text("🌤").font(.system(size: 56))
                    Text("Blue View Weather")
                        .font(.title.weight(.bold))
                        .foregroundColor(.textPrimary)
                        .multilineTextAlignment(.center)
                    Text(Self.versionText)
                        .font(.body)
                        .foregroundColor(.textSecondary)
                    Text("Live radar · 7-day forecast · Hourly drill-down")
                        .font(.subheadline)
                        .foregroundColor(.blueAccent)
                        .multilineTextAlignment(.center)

                    Group {
                        Divider().background(Color.textMuted.opacity(0.3)).padding(.vertical, 4)
                        UpdateSection(state: updateChecker.state,
                                      onCheck: { Task { await updateChecker.check() } },
                                      onOpenTestFlight: openTestFlight)
                    }

                    Divider().background(Color.textMuted.opacity(0.3)).padding(.vertical, 4)

                    Group {
                        AboutRow(label: "Author", value: "Frank Perez")
                        AboutLinkRow(label: "Email", value: "frank@blueview.ai") {
                            openURL(URL(string: "mailto:frank@blueview.ai")!)
                        }
                        AboutLinkRow(label: "OS", value: "bvos.blueview.ai") {
                            openURL(URL(string: "https://bvos.blueview.ai")!)
                        }
                        AboutLinkRow(label: "Paper", value: "mypapertrail.co") {
                            openURL(URL(string: "https://mypapertrail.co")!)
                        }
                        AboutLinkRow(label: "Read2Me", value: "read2me.co") {
                            openURL(URL(string: "https://read2me.co")!)
                        }
                        AboutLinkRow(label: "Web", value: "blueview.ai") {
                            openURL(URL(string: "https://blueview.ai")!)
                        }
                    }

                    Divider().background(Color.textMuted.opacity(0.3)).padding(.vertical, 4)

                    Group {
                        Text("Powered by Open-Meteo · RainViewer")
                            .font(.subheadline)
                            .foregroundColor(.textMuted)
                            .multilineTextAlignment(.center)
                        Text("© 2026 BlueView / Frank Perez")
                            .font(.subheadline)
                            .foregroundColor(.textMuted)
                            .multilineTextAlignment(.center)
                    }
                }
                .padding(28)
            }
            .navigationTitle("About")
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

    private static var versionText: String {
        let build = Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String
        let version = UpdateChecker.currentVersion
        guard let build = build, !build.isEmpty, build != version else { return "Version \(version)" }
        return "Version \(version) (\(build))"
    }

    /// itms-beta:// only resolves when TestFlight is installed; otherwise send the
    /// user to its App Store page so the button never dead-ends.
    private func openTestFlight() {
        openURL(UpdateChecker.testFlightURL) { accepted in
            if !accepted { openURL(UpdateChecker.testFlightFallbackURL) }
        }
    }
}

private struct UpdateSection: View {
    let state: UpdateState
    let onCheck: () -> Void
    let onOpenTestFlight: () -> Void

    var body: some View {
        VStack(spacing: 8) {
            HStack {
                Text("Updates")
                    .font(.subheadline)
                    .foregroundColor(.textMuted)
                    .frame(width: 80, alignment: .leading)
                Text(statusText)
                    .font(.subheadline)
                    .foregroundColor(statusColor)
                Spacer()
                Button(action: onCheck) {
                    Text("Check")
                        .font(.subheadline)
                        .foregroundColor(.blueAccent)
                }
                .buttonStyle(.plain)
                .disabled(state == .checking)
            }

            if case .available = state {
                Button(action: onOpenTestFlight) {
                    Text("Open TestFlight")
                        .foregroundColor(.navyDeep)
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .tint(.blueAccent)
            }
        }
    }

    private var statusText: String {
        switch state {
        case .idle: return "Not checked"
        case .checking: return "Checking…"
        case .upToDate: return "Up to date"
        case .available(let version): return "Version \(version) available"
        case .failed(let message): return message
        }
    }

    private var statusColor: Color {
        switch state {
        case .available: return .tempHigh
        case .failed: return .errorRed
        default: return .textPrimary
        }
    }
}

private struct AboutRow: View {
    let label: String
    let value: String
    var body: some View {
        HStack {
            Text(label)
                .font(.subheadline)
                .foregroundColor(.textMuted)
                .frame(width: 80, alignment: .leading)
            Text(value)
                .font(.subheadline)
                .foregroundColor(.textPrimary)
            Spacer()
        }
    }
}

private struct AboutLinkRow: View {
    let label: String
    let value: String
    let onTap: () -> Void
    var body: some View {
        Button(action: onTap) {
            HStack {
                Text(label)
                    .font(.subheadline)
                    .foregroundColor(.textMuted)
                    .frame(width: 80, alignment: .leading)
                Text(value)
                    .font(.subheadline)
                    .foregroundColor(.blueAccent)
                Spacer()
                Image(systemName: "arrow.up.forward.app")
                    .foregroundColor(.textMuted)
                    .font(.caption)
            }
        }
        .buttonStyle(.plain)
    }
}
