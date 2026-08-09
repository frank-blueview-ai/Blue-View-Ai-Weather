import SwiftUI

// Brand palette — mirrors the Android app
extension Color {
    static let blueAccent    = Color(hex: 0xFF52BEE8)
    static let navyDeep      = Color(hex: 0xFF0B0E1C)
    static let navyMid       = Color(hex: 0xFF12162A)
    static let navyCard      = Color(hex: 0xFF181D30)
    static let navyBorder    = Color(hex: 0x16FFFFFF, isARGB: true)
    static let textPrimary   = Color(hex: 0xFFDAE2F8)
    static let textSecondary = Color(hex: 0xFF7887AA)
    static let textMuted     = Color(hex: 0xFF4A5878)
    static let tempHigh      = Color(hex: 0xFFFFB74D)
    static let tempLow       = Color(hex: 0xFF4A5878)
    static let rainBlue      = Color(hex: 0xFF52BEE8)
    static let errorRed      = Color(hex: 0xFFCF6679)
}

extension Color {
    /// hex is 0xAARRGGBB
    init(hex: UInt32, isARGB: Bool = false) {
        let a = Double((hex >> 24) & 0xFF) / 255.0
        let r = Double((hex >> 16) & 0xFF) / 255.0
        let g = Double((hex >> 8) & 0xFF) / 255.0
        let b = Double(hex & 0xFF) / 255.0
        self.init(.sRGB, red: r, green: g, blue: b, opacity: a)
    }
}
