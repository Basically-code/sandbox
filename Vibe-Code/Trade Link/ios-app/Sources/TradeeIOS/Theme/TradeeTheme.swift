import Foundation
import SwiftUI

public enum TradeeTheme {
    // Core Surface Tokens (Daylight-First High-Contrast Mode)
    public static let bgPrimaryCanvas = Color(red: 248.0 / 255.0, green: 250.0 / 255.0, blue: 252.0 / 255.0)
    public static let bgSurfaceCard = Color.white
    public static let bgSurfaceSubtle = Color(red: 241.0 / 255.0, green: 245.0 / 255.0, blue: 249.0 / 255.0)

    // Typography Tokens
    public static let textPrimary = Color(red: 15.0 / 255.0, green: 23.0 / 255.0, blue: 42.0 / 255.0)      // #0F172A
    public static let textSecondary = Color(red: 51.0 / 255.0, green: 65.0 / 255.0, blue: 85.0 / 255.0)    // #334155
    public static let textMuted = Color(red: 100.0 / 255.0, green: 116.0 / 255.0, blue: 139.0 / 255.0)     // #64748B

    // Brand & Accent Tokens
    public static let brandPrimary = Color(red: 2.0 / 255.0, green: 132.0 / 255.0, blue: 199.0 / 255.0)    // #0284C7
    public static let brandPrimaryDark = Color(red: 3.0 / 255.0, green: 105.0 / 255.0, blue: 161.0 / 255.0)// #0369A1
    public static let brandSkyBright = Color(red: 56.0 / 255.0, green: 189.0 / 255.0, blue: 248.0 / 255.0) // #38BDF8
    public static let brandSecondary = Color(red: 224.0 / 255.0, green: 242.0 / 255.0, blue: 254.0 / 255.0)// #E0F2FE

    public static let accentWarm = Color(red: 245.0 / 255.0, green: 158.0 / 255.0, blue: 11.0 / 255.0)     // #F59E0B
    public static let accentWarmDeep = Color(red: 234.0 / 255.0, green: 88.0 / 255.0, blue: 12.0 / 255.0)  // #EA580C
    public static let accentWarmSoft = Color(red: 254.0 / 255.0, green: 243.0 / 255.0, blue: 199.0 / 255.0)// #FEF3C7

    // Semantic Status Tokens
    public static let statusSafeGreen = Color(red: 22.0 / 255.0, green: 163.0 / 255.0, blue: 74.0 / 255.0) // #16A34A
    public static let statusSafeGreenDark = Color(red: 21.0 / 255.0, green: 128.0 / 255.0, blue: 61.0 / 255.0) // #15803D
    public static let statusSafeGreenSoft = Color(red: 220.0 / 255.0, green: 252.0 / 255.0, blue: 231.0 / 255.0) // #DCFCE7
    public static let statusWarning = Color(red: 217.0 / 255.0, green: 119.0 / 255.0, blue: 6.0 / 255.0)   // #D97706
    public static let statusWarningSoft = Color(red: 254.0 / 255.0, green: 243.0 / 255.0, blue: 199.0 / 255.0)
    public static let statusDanger = Color(red: 220.0 / 255.0, green: 38.0 / 255.0, blue: 38.0 / 255.0)   // #DC2626
    public static let statusDangerSoft = Color(red: 254.0 / 255.0, green: 226.0 / 255.0, blue: 226.0 / 255.0)

    // Borders
    public static let borderHairline = Color(red: 226.0 / 255.0, green: 232.0 / 255.0, blue: 240.0 / 255.0)// #E2E8F0

    // Multi-Stop Linear Gradients (Matching Website & Android Codebases)
    public static let brandPrimaryGradient = LinearGradient(
        colors: [textPrimary, brandPrimaryDark, brandPrimary, brandSkyBright],
        startPoint: .topLeading,
        endPoint: .bottomTrailing
    )

    public static let heroOceanGradient = LinearGradient(
        colors: [textPrimary, brandPrimaryDark, brandPrimary],
        startPoint: .topLeading,
        endPoint: .bottomTrailing
    )

    public static let warmDealGradient = LinearGradient(
        colors: [accentWarmSoft, Color(red: 253.0 / 255.0, green: 230.0 / 255.0, blue: 138.0 / 255.0), accentWarm],
        startPoint: .leading,
        endPoint: .trailing
    )

    public static let boltTransportGradient = LinearGradient(
        colors: [statusSafeGreenDark, statusSafeGreen, brandPrimary],
        startPoint: .topLeading,
        endPoint: .bottomTrailing
    )

    public static let safeEmeraldGradient = LinearGradient(
        colors: [statusSafeGreenDark, statusSafeGreen, Color(red: 34.0 / 255.0, green: 197.0 / 255.0, blue: 94.0 / 255.0)],
        startPoint: .leading,
        endPoint: .trailing
    )

    public static let cardSheenGradient = LinearGradient(
        colors: [bgSurfaceCard, bgPrimaryCanvas, brandSecondary],
        startPoint: .top,
        endPoint: .bottom
    )

    public static func formatTzs(_ amount: Double) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.maximumFractionDigits = 0
        formatter.groupingSeparator = ","
        let rounded = Int64(amount.rounded())
        let formatted = formatter.string(from: NSNumber(value: rounded)) ?? "\(rounded)"
        return "TSh \(formatted)"
    }
}

/// Redesigned Professional Tradee Logo View combining:
/// 1. Bidirectional Buyer-Seller Exchange Arrows (Ocean Sky Blue & Warm Amber Gold)
/// 2. Upward-pointing Bolt-Style GPS Route Pin forming the Monogram "T"
public struct TradeeLogoBadgeView: View {
    public var size: CGFloat = 40

    public init(size: CGFloat = 40) {
        self.size = size
    }

    public var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: size * 0.28, style: .continuous)
                .fill(TradeeTheme.brandPrimaryGradient)
                .frame(width: size, height: size)
                .overlay(
                    RoundedRectangle(cornerRadius: size * 0.28, style: .continuous)
                        .stroke(Color.white.opacity(0.35), lineWidth: 1.5)
                )

            VStack(spacing: 1) {
                HStack(spacing: 2) {
                    Image(systemName: "arrow.left.arrow.right")
                        .font(.system(size: size * 0.30, weight: .black))
                        .foregroundStyle(TradeeTheme.accentWarmSoft)
                }
                Image(systemName: "location.north.fill")
                    .font(.system(size: size * 0.34, weight: .bold))
                    .foregroundStyle(Color.white)
            }
        }
        .accessibilityLabel("Tradee Official Logo")
    }
}
