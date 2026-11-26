import SwiftUI

public struct MarketplaceView: View {
    @ObservedObject var store: TradeeStore
    var onOpenPriceCompare: (Product?) -> Void
    var onOpenBoltTransport: () -> Void
    var onOpenTelemetry: () -> Void

    public init(
        store: TradeeStore,
        onOpenPriceCompare: @escaping (Product?) -> Void,
        onOpenBoltTransport: @escaping () -> Void,
        onOpenTelemetry: @escaping () -> Void
    ) {
        self.store = store
        self.onOpenPriceCompare = onOpenPriceCompare
        self.onOpenBoltTransport = onOpenBoltTransport
        self.onOpenTelemetry = onOpenTelemetry
    }

    public var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                // Pinned Search Input
                HStack(spacing: 10) {
                    Image(systemName: "magnifyingglass")
                        .foregroundStyle(TradeeTheme.brandPrimary)
                    TextField("Search by product name, seller or category", text: $store.searchQuery)
                        .foregroundStyle(TradeeTheme.textPrimary)
                    if !store.searchQuery.isEmpty {
                        Button {
                            store.searchQuery = ""
                        } label: {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundStyle(TradeeTheme.textSecondary)
                        }
                    }
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
                .background(TradeeTheme.bgSurfaceSubtle)
                .clipShape(Capsule())
                .overlay(Capsule().stroke(TradeeTheme.borderHairline, lineWidth: 1))

                // 8-Category Filter Chips Row
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        CategoryFilterChip(
                            title: "All (\(store.products.count))",
                            isSelected: store.selectedCategory == nil && !store.dealsOnly
                        ) {
                            store.selectedCategory = nil
                            store.dealsOnly = false
                        }

                        ForEach(ProductCategory.allCases) { cat in
                            CategoryFilterChip(
                                title: cat.displayName,
                                icon: cat.sfSymbol,
                                isSelected: store.selectedCategory == cat
                            ) {
                                store.selectedCategory = (store.selectedCategory == cat) ? nil : cat
                            }
                        }
                    }
                }

                // Warm Amber Gradient Deals of Today Banner
                Button {
                    store.dealsOnly.toggle()
                } label: {
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("TANZANIA FLASH MARKET · TZS")
                                .font(.caption.weight(.bold))
                                .foregroundStyle(TradeeTheme.textSecondary)
                            Text("Deals of Today")
                                .font(.title2.weight(.heavy))
                                .foregroundStyle(TradeeTheme.textPrimary)
                            Text("Verified 100% refund protection & verbal secret code release")
                                .font(.caption)
                                .foregroundStyle(TradeeTheme.textSecondary)
                        }
                        Spacer()
                        Text(store.dealsOnly ? "Show All" : "Explore")
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(.white)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 10)
                            .background(TradeeTheme.brandPrimary)
                            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    }
                    .padding(16)
                    .background(TradeeTheme.warmDealGradient)
                    .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: 18, style: .continuous)
                            .stroke(TradeeTheme.accentWarm, lineWidth: 1.5)
                    )
                }
                .buttonStyle(.plain)

                // Quick Action Bar: Compare Prices + Order Bolt Transport
                HStack(spacing: 10) {
                    Button {
                        onOpenPriceCompare(nil)
                    } label: {
                        HStack(spacing: 6) {
                            Image(systemName: "arrow.left.arrow.right.circle.fill")
                            Text("Compare Prices")
                                .font(.subheadline.weight(.bold))
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 13)
                        .foregroundStyle(TradeeTheme.brandPrimaryDark)
                        .background(TradeeTheme.brandSecondary)
                        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                        .overlay(
                            RoundedRectangle(cornerRadius: 14, style: .continuous)
                                .stroke(TradeeTheme.brandPrimary, lineWidth: 1.2)
                        )
                    }

                    Button {
                        onOpenBoltTransport()
                    } label: {
                        HStack(spacing: 6) {
                            Image(systemName: "bicycle")
                            Text("Bolt Transport")
                                .font(.subheadline.weight(.bold))
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 13)
                        .foregroundStyle(.white)
                        .background(TradeeTheme.boltTransportGradient)
                        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                    }
                }

                // Product Cards List
                LazyVStack(spacing: 12) {
                    ForEach(store.filteredProducts) { product in
                        MarketplaceProductCardView(
                            product: product,
                            isFavorite: store.favoriteIds.contains(product.id),
                            onToggleFavorite: { store.toggleFavorite(product) },
                            onCompareClicked: { onOpenPriceCompare(product) },
                            onAddToCart: { store.addToCart(product) },
                            onBuyNow: {
                                store.placeOrder(product: product)
                                onOpenTelemetry()
                            }
                        )
                    }
                }
            }
            .padding(16)
        }
        .background(TradeeTheme.bgPrimaryCanvas)
    }
}

private struct CategoryFilterChip: View {
    let title: String
    var icon: String? = nil
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                if let icon {
                    Image(systemName: icon)
                        .font(.caption.weight(.bold))
                }
                Text(title)
                    .font(.caption.weight(.bold))
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .foregroundStyle(isSelected ? TradeeTheme.brandPrimaryDark : TradeeTheme.textPrimary)
            .background(isSelected ? TradeeTheme.brandSecondary : TradeeTheme.bgSurfaceCard)
            .clipShape(Capsule())
            .overlay(
                Capsule()
                    .stroke(isSelected ? TradeeTheme.brandPrimary : TradeeTheme.borderHairline, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }
}

private struct MarketplaceProductCardView: View {
    let product: Product
    let isFavorite: Bool
    let onToggleFavorite: () -> Void
    let onCompareClicked: () -> Void
    let onAddToCart: () -> Void
    let onBuyNow: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(product.category.displayName)
                    .font(.caption.weight(.bold))
                    .foregroundStyle(TradeeTheme.brandPrimaryDark)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(TradeeTheme.brandSecondary)
                    .clipShape(Capsule())

                Spacer()

                if product.isDailyDeal {
                    Text("Daily Deal")
                        .font(.caption2.weight(.bold))
                        .foregroundStyle(TradeeTheme.textPrimary)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(TradeeTheme.accentWarmSoft)
                        .clipShape(Capsule())
                }

                Text(product.requiresTruck ? "Truck (\(Int(product.weightKg))kg)" : "Moto Box")
                    .font(.caption2.weight(.semibold))
                    .foregroundStyle(TradeeTheme.textPrimary)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(product.requiresTruck ? TradeeTheme.accentWarmSoft : TradeeTheme.statusSafeGreenSoft)
                    .clipShape(Capsule())

                Button(action: onToggleFavorite) {
                    Image(systemName: isFavorite ? "heart.fill" : "heart")
                        .foregroundStyle(isFavorite ? TradeeTheme.statusDanger : TradeeTheme.textSecondary)
                }
            }

            Text(product.title)
                .font(.headline.weight(.bold))
                .foregroundStyle(TradeeTheme.textPrimary)

            if !product.description.isEmpty {
                Text(product.description)
                    .font(.subheadline)
                    .foregroundStyle(TradeeTheme.textSecondary)
                    .lineLimit(2)
            }

            HStack {
                Text("Size: \(product.sizeSpec)")
                Spacer()
                Text("\(product.quantityAvailable) in stock · \(product.pickupLabel)")
            }
            .font(.caption)
            .foregroundStyle(TradeeTheme.textSecondary)

            Divider()

            HStack(alignment: .center) {
                VStack(alignment: .leading, spacing: 3) {
                    Text(TradeeTheme.formatTzs(product.priceTzs))
                        .font(.title3.weight(.heavy))
                        .foregroundStyle(TradeeTheme.textPrimary)
                    Text("Seller: \(product.providerName)")
                        .font(.caption)
                        .foregroundStyle(TradeeTheme.textSecondary)
                    Button(action: onCompareClicked) {
                        HStack(spacing: 4) {
                            Image(systemName: "arrow.left.arrow.right")
                            Text("Compare Sellers")
                        }
                        .font(.caption2.weight(.bold))
                        .foregroundStyle(TradeeTheme.brandPrimaryDark)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(TradeeTheme.brandSecondary)
                        .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                }

                Spacer()

                HStack(spacing: 8) {
                    Button(action: onAddToCart) {
                        Image(systemName: "cart.badge.plus")
                            .padding(10)
                            .foregroundStyle(TradeeTheme.brandPrimary)
                            .background(TradeeTheme.bgSurfaceSubtle)
                            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                    }
                    Button(action: onBuyNow) {
                        Text("Buy Now")
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(.white)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 10)
                            .background(TradeeTheme.brandPrimary)
                            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                    }
                }
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
