import SwiftUI

public struct PriceCompareView: View {
    @ObservedObject var store: TradeeStore
    public var focusedProduct: Product?
    var onOrderPlaced: () -> Void

    @State private var buyerDistanceKm: Double = 4.0

    public init(
        store: TradeeStore,
        focusedProduct: Product? = nil,
        onOrderPlaced: @escaping () -> Void
    ) {
        self.store = store
        self.focusedProduct = focusedProduct
        self.onOrderPlaced = onOrderPlaced
    }

    private var comparisonGroups: [ProductComparisonGroup] {
        let built = TradeeCalculator.buildPriceComparisonGroups(
            products: store.products,
            baseDistanceKm: buyerDistanceKm
        )
        guard let focusedProduct else { return built }
        let targetKey = TradeeCalculator.normalizeComparisonKey(focusedProduct.title)
        return built.sorted { a, b in
            if (a.groupKey == targetKey) != (b.groupKey == targetKey) {
                return a.groupKey == targetKey
            }
            return a.sellerCount > b.sellerCount
        }
    }

    public var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                // Hero Ocean Gradient Header
                VStack(alignment: .leading, spacing: 10) {
                    HStack {
                        Image(systemName: "arrow.left.arrow.right.circle.fill")
                            .font(.title2)
                            .foregroundStyle(.white)
                        Text("Tradee Multi-Seller Price Comparison")
                            .font(.title3.weight(.heavy))
                            .foregroundStyle(.white)
                    }

                    Text("Compare Sticker Prices + Bolt Transport Fees across Dar es Salaam market hubs to see the true lowest landed cost.")
                        .font(.subheadline)
                        .foregroundStyle(TradeeTheme.brandSecondary)

                    HStack {
                        Text("Delivery Distance: \(Int(buyerDistanceKm)) km")
                            .font(.caption.weight(.bold))
                            .foregroundStyle(.white)
                        Spacer()
                        Text("\(comparisonGroups.count) Product Families")
                            .font(.caption.weight(.bold))
                            .foregroundStyle(TradeeTheme.accentWarmSoft)
                    }

                    Slider(value: $buyerDistanceKm, in: 1...20, step: 1)
                        .tint(TradeeTheme.brandSecondary)
                }
                .padding(16)
                .background(TradeeTheme.heroOceanGradient)
                .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))

                ForEach(comparisonGroups) { group in
                    VStack(alignment: .leading, spacing: 12) {
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(group.category.displayName.uppercased())
                                    .font(.caption2.weight(.bold))
                                    .foregroundStyle(TradeeTheme.brandPrimary)
                                Text(group.title)
                                    .font(.headline.weight(.bold))
                                    .foregroundStyle(TradeeTheme.textPrimary)
                            }
                            Spacer()
                            Text("\(group.sellerCount) Sellers")
                                .font(.caption.weight(.bold))
                                .padding(.horizontal, 10)
                                .padding(.vertical, 5)
                                .background(group.sellerCount > 1 ? TradeeTheme.accentWarmSoft : TradeeTheme.brandSecondary)
                                .clipShape(Capsule())
                        }

                        if group.priceSpreadTzs > 0 {
                            Text("Sticker Spread: \(TradeeTheme.formatTzs(group.lowestStickerTzs)) – \(TradeeTheme.formatTzs(group.highestStickerTzs)) (Save up to \(TradeeTheme.formatTzs(group.priceSpreadTzs)))")
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(TradeeTheme.statusSafeGreenDark)
                        }

                        Divider()

                        ForEach(group.offers) { offer in
                            VStack(alignment: .leading, spacing: 6) {
                                HStack {
                                    Text("#\(offer.rank) \(offer.product.providerName)")
                                        .font(.subheadline.weight(.bold))
                                        .foregroundStyle(TradeeTheme.textPrimary)
                                    Spacer()
                                    if offer.isBestLandedValue {
                                        Text("★ BEST TOTAL VALUE")
                                            .font(.caption2.weight(.heavy))
                                            .foregroundStyle(.white)
                                            .padding(.horizontal, 8)
                                            .padding(.vertical, 3)
                                            .background(TradeeTheme.statusSafeGreen)
                                            .clipShape(Capsule())
                                    } else if offer.isLowestStickerPrice {
                                        Text("Lowest Sticker")
                                            .font(.caption2.weight(.bold))
                                            .foregroundStyle(TradeeTheme.textPrimary)
                                            .padding(.horizontal, 8)
                                            .padding(.vertical, 3)
                                            .background(TradeeTheme.accentWarmSoft)
                                            .clipShape(Capsule())
                                    }
                                }

                                Text("Hub: \(offer.product.pickupLabel) (\(String(format: "%.1f", offer.effectiveDistanceKm)) km) · \(offer.product.sizeSpec)")
                                    .font(.caption)
                                    .foregroundStyle(TradeeTheme.textSecondary)

                                HStack {
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text("Item: \(TradeeTheme.formatTzs(offer.unitPriceTzs)) + Bolt: \(TradeeTheme.formatTzs(offer.transportFeeTzs))")
                                            .font(.caption)
                                            .foregroundStyle(TradeeTheme.textSecondary)
                                        Text("Landed Total: \(TradeeTheme.formatTzs(offer.landedTotalTzs))")
                                            .font(.subheadline.weight(.heavy))
                                            .foregroundStyle(TradeeTheme.brandPrimaryDark)
                                    }
                                    Spacer()
                                    Button {
                                        store.placeOrder(product: offer.product, distanceKm: offer.effectiveDistanceKm)
                                        onOrderPlaced()
                                    } label: {
                                        Text("Buy Deal")
                                            .font(.caption.weight(.bold))
                                            .foregroundStyle(.white)
                                            .padding(.horizontal, 12)
                                            .padding(.vertical, 8)
                                            .background(TradeeTheme.brandPrimary)
                                            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                                    }
                                }
                            }
                            .padding(12)
                            .background(offer.isBestLandedValue ? TradeeTheme.statusSafeGreenSoft.opacity(0.55) : TradeeTheme.bgPrimaryCanvas)
                            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                            .overlay(
                                RoundedRectangle(cornerRadius: 14, style: .continuous)
                                    .stroke(offer.isBestLandedValue ? TradeeTheme.statusSafeGreen : TradeeTheme.borderHairline, lineWidth: offer.isBestLandedValue ? 1.5 : 1)
                            )
                        }
                    }
                    .padding(16)
                    .background(TradeeTheme.bgSurfaceCard)
                    .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: 18, style: .continuous)
                            .stroke(TradeeTheme.borderHairline, lineWidth: 1)
                    )
                }
            }
            .padding(16)
        }
        .background(TradeeTheme.bgPrimaryCanvas)
    }
}
