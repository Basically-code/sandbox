import SwiftUI

public struct BoltTransportView: View {
    @ObservedObject var store: TradeeStore
    var onDispatched: () -> Void

    @State private var isSellerMode: Bool = false
    @State private var pickupHub: String = "Kariakoo Market Block A"
    @State private var dropoffLabel: String = "Mikocheni B Gate #14, Dar es Salaam"
    @State private var distanceKm: Double = 4.5
    @State private var cargoWeightKg: Double = 5.0
    @State private var selectedTier: TransportTier = .TRADEE_MOTO_BOX
    @State private var paymentMethod: PaymentMethod = .CASH_BY_HAND_WITH_RECEIPT

    private let hubs = [
        "Kariakoo Market Block A",
        "Samora Tech Hub",
        "Mlimani City Retail Plaza",
        "Nyerere Rd Warehouse",
        "Mwenge Crafts Arcade",
        "Slipway Artisan Guild"
    ]

    public init(store: TradeeStore, onDispatched: @escaping () -> Void) {
        self.store = store
        self.onDispatched = onDispatched
    }

    private var tierQuotes: [BoltTransportQuote] {
        TradeeCalculator.compareAllBoltTiers(distanceKm: distanceKm, cargoWeightKg: cargoWeightKg)
    }

    private var activeQuote: BoltTransportQuote {
        tierQuotes.first(where: { $0.tier == selectedTier && $0.isEligible })
            ?? tierQuotes.first(where: \.isRecommended)
            ?? tierQuotes.last!
    }

    public var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                // Bolt Emerald-to-Ocean Hero Banner
                VStack(alignment: .leading, spacing: 10) {
                    HStack {
                        Image(systemName: "bicycle")
                            .font(.title2.weight(.bold))
                            .foregroundStyle(.white)
                        Text("Tradee Bolt Transport & Cargo Dispatch")
                            .font(.title3.weight(.heavy))
                            .foregroundStyle(.white)
                    }

                    Text("Order on-demand rides, motorcycle cargo boxes, Bajaji 3-wheelers, or commercial trucks — available for both Buyers and Sellers.")
                        .font(.subheadline)
                        .foregroundStyle(.white.opacity(0.92))

                    HStack(spacing: 8) {
                        Button {
                            isSellerMode = false
                            cargoWeightKg = 5.0
                        } label: {
                            Text("Buyer Ride / Parcel Pickup")
                                .font(.caption.weight(.bold))
                                .padding(.horizontal, 12)
                                .padding(.vertical, 8)
                                .foregroundStyle(isSellerMode ? .white : TradeeTheme.brandPrimaryDark)
                                .background(isSellerMode ? Color.white.opacity(0.18) : .white)
                                .clipShape(Capsule())
                        }

                        Button {
                            isSellerMode = true
                            cargoWeightKg = 25.0
                        } label: {
                            Text("Seller Inventory / Restock")
                                .font(.caption.weight(.bold))
                                .padding(.horizontal, 12)
                                .padding(.vertical, 8)
                                .foregroundStyle(isSellerMode ? TradeeTheme.brandPrimaryDark : .white)
                                .background(isSellerMode ? .white : Color.white.opacity(0.18))
                                .clipShape(Capsule())
                        }
                    }
                }
                .padding(16)
                .background(TradeeTheme.boltTransportGradient)
                .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))

                // Route & Load Configuration Card
                VStack(alignment: .leading, spacing: 12) {
                    Text(isSellerMode ? "1. Seller Pickup Hub & Destination" : "1. Buyer Pickup & Gate Pin")
                        .font(.headline.weight(.bold))
                        .foregroundStyle(TradeeTheme.textPrimary)

                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(hubs, id: \.self) { hub in
                                Button {
                                    pickupHub = hub
                                } label: {
                                    Text(hub)
                                        .font(.caption.weight(.semibold))
                                        .padding(.horizontal, 10)
                                        .padding(.vertical, 6)
                                        .foregroundStyle(pickupHub == hub ? TradeeTheme.brandPrimaryDark : TradeeTheme.textPrimary)
                                        .background(pickupHub == hub ? TradeeTheme.brandSecondary : TradeeTheme.bgSurfaceSubtle)
                                        .clipShape(Capsule())
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }

                    TextField("Dropoff Destination / Buyer Gate", text: $dropoffLabel)
                        .padding(10)
                        .background(TradeeTheme.bgSurfaceSubtle)
                        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))

                    HStack {
                        Text("Trip Distance: \(String(format: "%.1f", distanceKm)) km")
                            .font(.subheadline.weight(.semibold))
                        Spacer()
                        Text("Cargo Weight: \(Int(cargoWeightKg)) kg")
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(TradeeTheme.brandPrimary)
                    }

                    Slider(value: $distanceKm, in: 1...25, step: 0.5)
                        .tint(TradeeTheme.brandPrimary)

                    Slider(value: $cargoWeightKg, in: 1...300, step: 1)
                        .tint(TradeeTheme.statusSafeGreen)
                }
                .padding(16)
                .background(TradeeTheme.bgSurfaceCard)
                .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                        .stroke(TradeeTheme.borderHairline, lineWidth: 1)
                )

                // 4-Tier Vehicle Comparison Matrix
                VStack(alignment: .leading, spacing: 10) {
                    Text("2. Compare Tradee Vehicle Tiers (Live TSh Fares)")
                        .font(.headline.weight(.bold))
                        .foregroundStyle(TradeeTheme.textPrimary)

                    ForEach(tierQuotes) { quote in
                        let isSelected = activeQuote.tier == quote.tier
                        Button {
                            if quote.isEligible {
                                selectedTier = quote.tier
                            }
                        } label: {
                            HStack {
                                VStack(alignment: .leading, spacing: 4) {
                                    HStack(spacing: 6) {
                                        Text(quote.tier.displayName)
                                            .font(.subheadline.weight(.bold))
                                            .foregroundStyle(TradeeTheme.textPrimary)
                                        if quote.isRecommended {
                                            Text("★ Best Fit")
                                                .font(.caption2.weight(.bold))
                                                .foregroundStyle(.white)
                                                .padding(.horizontal, 6)
                                                .padding(.vertical, 2)
                                                .background(TradeeTheme.statusSafeGreen)
                                                .clipShape(Capsule())
                                        }
                                    }
                                    Text(quote.tier.subtitle)
                                        .font(.caption)
                                        .foregroundStyle(TradeeTheme.textSecondary)
                                    if let reason = quote.ineligibleReason {
                                        Text(reason)
                                            .font(.caption2.weight(.semibold))
                                            .foregroundStyle(TradeeTheme.statusDanger)
                                    } else {
                                        Text("ETA ~\(quote.estimatedMinutes) mins · Max \(Int(quote.tier.maxWeightKg)) kg")
                                            .font(.caption2.weight(.bold))
                                            .foregroundStyle(TradeeTheme.brandPrimaryDark)
                                    }
                                }
                                Spacer()
                                VStack(alignment: .trailing, spacing: 2) {
                                    Text(TradeeTheme.formatTzs(quote.fareTzs))
                                        .font(.headline.weight(.heavy))
                                        .foregroundStyle(TradeeTheme.textPrimary)
                                    Text("\(Int(quote.distanceKm)) km")
                                        .font(.caption2)
                                        .foregroundStyle(TradeeTheme.textSecondary)
                                }
                            }
                            .padding(14)
                            .background(
                                !quote.isEligible
                                    ? TradeeTheme.bgSurfaceSubtle.opacity(0.55)
                                    : (isSelected ? TradeeTheme.statusSafeGreenSoft.opacity(0.6) : TradeeTheme.bgSurfaceCard)
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                            .overlay(
                                RoundedRectangle(cornerRadius: 14, style: .continuous)
                                    .stroke(isSelected ? TradeeTheme.statusSafeGreen : TradeeTheme.borderHairline, lineWidth: isSelected ? 1.8 : 1)
                            )
                        }
                        .buttonStyle(.plain)
                        .disabled(!quote.isEligible)
                    }
                }

                // Dispatch CTA
                Button {
                    store.dispatchBoltTransport(
                        tier: activeQuote.tier,
                        isSellerMode: isSellerMode,
                        pickupHub: pickupHub,
                        dropoffLabel: dropoffLabel,
                        distanceKm: distanceKm,
                        weightKg: cargoWeightKg,
                        paymentMethod: paymentMethod
                    )
                    onDispatched()
                } label: {
                    HStack {
                        Image(systemName: "bolt.shield.fill")
                        Text("DISPATCH \(activeQuote.tier.displayName.uppercased()) · \(TradeeTheme.formatTzs(activeQuote.fareTzs))")
                            .font(.subheadline.weight(.heavy))
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
                    .foregroundStyle(.white)
                    .background(TradeeTheme.safeEmeraldGradient)
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                }
            }
            .padding(16)
        }
        .background(TradeeTheme.bgPrimaryCanvas)
    }
}
