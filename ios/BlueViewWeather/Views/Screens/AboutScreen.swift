import SwiftUI

struct AboutScreen: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL

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
                    Text("Version 1.0.0")
                        .font(.body)
                        .foregroundColor(.textSecondary)
                    Text("Live radar · 7-day forecast · Hourly drill-down")
                        .font(.subheadline)
                        .foregroundColor(.blueAccent)
                        .multilineTextAlignment(.center)

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
