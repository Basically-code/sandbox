import SwiftUI

public struct SellerPortalView: View {
    @ObservedObject var store: TradeeStore
    var onOpenBoltDispatch: () -> Void

    @State private var selectedCategory: ProductCategory = .ELECTRONICS
    @State private var title: String = ""
    @State private var description: String = ""
    @State private var priceInput: String = "65000"
    @State private var quantityInput: String = "10"
    @State private var sizeSpec: String = "Standard Box"
    @State private var weightInput: String = "1.5"
    @State private var pickupLabel: String = "Kariakoo Market Block A"
    @State private var isDailyDeal: Bool = false

    public init(store: TradeeStore, onOpenBoltDispatch: @escaping () -> Void) {
        self.store = store
        self.onOpenBoltDispatch = onOpenBoltDispatch
    }

    private var parsedWeight: Double { Double(weightInput) ?? 1.0 }
    private var parsedPrice: Double { Double(priceInput) ?? 0.0 }
    private var parsedQty: Int { Int(quantityInput) ?? 1 }
    private var requiresTruck: Bool { parsedWeight > TradeeCalculator.motorcycleMaxWeightKg }

    public var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                // KPI Cards
                HStack(spacing: 10) {
                    SellerKpiCard(title: "Listings", value: "\(store.products.count)")
                    SellerKpiCard(title: "Active Orders", value: "\(store.orders.filter { !$0.status.isTerminal }.count)")
                    SellerKpiCard(title: "Hubs", value: "6")
                }

                // Seller Bolt Transport Shortcut
                Button(action: onOpenBoltDispatch) {
                    HStack {
                        Image(systemName: "truck.box.badge.clock.fill")
                        Text("ORDER BOLT TRANSPORT (SELLER CARGO / RESTOCK)")
                            .font(.caption.weight(.heavy))
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .foregroundStyle(.white)
                    .background(TradeeTheme.boltTransportGradient)
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                }

                // Publish New Listing Form
                VStack(alignment: .leading, spacing: 12) {
                    Text("Publish New Item on Tradee")
                        .font(.headline.weight(.bold))
                        .foregroundStyle(TradeeTheme.textPrimary)

                    Picker("Category", selection: $selectedCategory) {
                        ForEach(ProductCategory.allCases) { cat in
                            Text(cat.displayName).tag(cat)
                        }
                    }
                    .pickerStyle(.menu)

                    TextField("Product Title *", text: $title)
                        .padding(10)
                        .background(TradeeTheme.bgSurfaceSubtle)
                        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))

                    TextField("Description", text: $description)
                        .padding(10)
                        .background(TradeeTheme.bgSurfaceSubtle)
                        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))

                    HStack(spacing: 10) {
                        TextField("Price (TSh)", text: $priceInput)
                            .padding(10)
                            .background(TradeeTheme.bgSurfaceSubtle)
                            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                        TextField("Weight (kg)", text: $weightInput)
                            .padding(10)
                            .background(TradeeTheme.bgSurfaceSubtle)
                            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                    }

                    // Live Market Price Benchmark Card (Revisit #1 Section 3.3)
                    let benchmark = TradeeCalculator.computeCategoryPriceBenchmark(
                        products: store.products,
                        category: selectedCategory
                    )
                    if benchmark.count > 0 {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("Live Market Price Benchmark (\(selectedCategory.displayName) · \(benchmark.count) listings)")
                                .font(.caption.weight(.bold))
                                .foregroundStyle(TradeeTheme.textPrimary)
                            Text("Lowest: \(TradeeFormat.tzs(benchmark.lowestTzs)) · Avg: \(TradeeFormat.tzs(benchmark.averageTzs)) · Highest: \(TradeeFormat.tzs(benchmark.highestTzs))")
                                .font(.caption2)
                                .foregroundStyle(TradeeTheme.textSecondary)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(10)
                        .background(TradeeTheme.bgSurfaceSubtle)
                        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                    }

                    Text(
                        requiresTruck
                            ? "Auto-Classifier (>35 kg): Phase 2 Commercial Truck Required"
                            : "Auto-Classifier (<=35 kg): Motorcycle Cargo Box"
                    )
                    .font(.caption.weight(.bold))
                    .foregroundStyle(TradeeTheme.textPrimary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(10)
                    .background(requiresTruck ? TradeeTheme.accentWarmSoft : TradeeTheme.statusSafeGreenSoft)
                    .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))

                    Toggle("Feature in Deals of Today", isOn: $isDailyDeal)
                        .font(.subheadline)

                    Button {
                        guard !title.trimmingCharacters(in: .whitespaces).isEmpty, parsedPrice > 0 else { return }
                        store.publishSellerProduct(
                            category: selectedCategory,
                            title: title,
                            description: description,
                            priceTzs: parsedPrice,
                            quantity: parsedQty,
                            sizeSpec: sizeSpec,
                            weightKg: parsedWeight,
                            pickupLabel: pickupLabel,
                            isDailyDeal: isDailyDeal
                        )
                        title = ""
                        description = ""
                    } label: {
                        Text("PUBLISH TO TRADEE MARKETPLACE")
                            .font(.subheadline.weight(.heavy))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .foregroundStyle(.white)
                            .background(TradeeTheme.brandPrimary)
                            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    }
                }
                .padding(16)
                .background(TradeeTheme.bgSurfaceCard)
                .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                        .stroke(TradeeTheme.brandPrimary, lineWidth: 1.2)
                )
            }
            .padding(16)
        }
        .background(TradeeTheme.bgPrimaryCanvas)
    }
}

private struct SellerKpiCard: View {
    let title: String
    let value: String

    var body: some View {
        VStack(spacing: 4) {
            Text(value)
                .font(.title2.weight(.heavy))
                .foregroundStyle(TradeeTheme.textPrimary)
            Text(title)
                .font(.caption)
                .foregroundStyle(TradeeTheme.textSecondary)
        }
        .frame(maxWidth: .infinity)
        .padding(12)
        .background(TradeeTheme.bgSurfaceCard)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .stroke(TradeeTheme.borderHairline, lineWidth: 1)
        )
    }
}
