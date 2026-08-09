import SwiftUI

struct CollapsibleSection<Content: View>: View {
    let title: String
    let expanded: Bool
    let onToggle: () -> Void
    @ViewBuilder let content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Button(action: onToggle) {
                HStack {
                    Text(title)
                        .font(.headline)
                        .foregroundColor(.textPrimary)
                    Spacer()
                    Image(systemName: expanded ? "chevron.up" : "chevron.down")
                        .foregroundColor(.textSecondary)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
            }
            .buttonStyle(.plain)

            if expanded {
                content()
            }
        }
        .background(Color.navyMid)
        .overlay(
            RoundedRectangle(cornerRadius: 10)
                .stroke(Color.navyBorder, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 10))
        .padding(.horizontal, 12)
        .padding(.vertical, 6)
    }
}
