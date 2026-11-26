import Foundation
import SwiftUI

public enum TradeeCalculator {
    public static let weeklyLeaseFeeTzs: Int64 = 70_000
    public static let totalLeaseWeeks: Int = 104
    public static let disturbanceFeeTzs: Double = 1_000.0
    public static let motorcycleMaxWeightKg: Double = 35.0

    public static func computeTransportFeeTzs(requiresTruck: Bool, distanceKm: Double) -> Double {
        let km = max(1.0, distanceKm)
        return requiresTruck ? (8_000.0 + km * 500.0) : (2_500.0 + km * 500.0)
    }

    public static func normalizeComparisonKey(_ title: String) -> String {
        let lowered = title.lowercased()
        let allowed = lowered.unicodeScalars.map { scalar -> Character in
            if CharacterSet.alphanumerics.contains(scalar) || scalar == " " {
                return Character(scalar)
            }
            return " "
        }
        return String(allowed)
            .split(separator: " ")
            .joined(separator: " ")
    }

    public static func estimateHubDistanceKm(pickupLabel: String, baseDistanceKm: Double) -> Double {
        let base = max(1.0, baseDistanceKm)
        let hub = pickupLabel.lowercased()
        let delta: Double
        if hub.contains("kariakoo") {
            delta = 0.0
        } else if hub.contains("samora") || hub.contains("posta") {
            delta = 0.8
        } else if hub.contains("uhuru") || hub.contains("kinondoni") {
            delta = 1.4
        } else if hub.contains("nyerere") {
            delta = 2.5
        } else if hub.contains("mwenge") || hub.contains("mlimani") {
            delta = 3.2
        } else if hub.contains("slipway") || hub.contains("masaki") {
            delta = 4.5
        } else {
            delta = 1.0
        }
        return ((base + delta) * 10.0).rounded() / 10.0
    }

    public static func compareProductOffers(
        offers: [Product],
        baseDistanceKm: Double = 4.0
    ) -> [SellerOfferComparison] {
        if offers.isEmpty { return [] }
        let lowestSticker = offers.map(\.priceTzs).min() ?? 0.0
        let raw = offers.map { product -> (Product, Double, Double, Double) in
            let distKm = estimateHubDistanceKm(pickupLabel: product.pickupLabel, baseDistanceKm: baseDistanceKm)
            let transportFee = computeTransportFeeTzs(requiresTruck: product.requiresTruck, distanceKm: distKm)
            let landedTotal = product.priceTzs + transportFee
            return (product, distKm, transportFee, landedTotal)
        }
        let lowestLanded = raw.map { $0.3 }.min() ?? 0.0
        let highestLanded = raw.map { $0.3 }.max() ?? lowestLanded

        let sorted = raw.sorted { a, b in
            if abs(a.3 - b.3) > 0.001 { return a.3 < b.3 }
            return a.0.priceTzs < b.0.priceTzs
        }

        return sorted.enumerated().map { idx, tuple in
            let (product, distKm, transportFee, landedTotal) = tuple
            return SellerOfferComparison(
                product: product,
                rank: idx + 1,
                effectiveDistanceKm: distKm,
                unitPriceTzs: product.priceTzs,
                transportFeeTzs: transportFee,
                landedTotalTzs: landedTotal,
                savingsVsHighestTzs: max(0.0, highestLanded - landedTotal),
                isBestLandedValue: abs(landedTotal - lowestLanded) < 0.01,
                isLowestStickerPrice: abs(product.priceTzs - lowestSticker) < 0.01
            )
        }
    }

    public static func buildPriceComparisonGroups(
        products: [Product],
        baseDistanceKm: Double = 4.0
    ) -> [ProductComparisonGroup] {
        let active = products.filter { $0.isActive && $0.quantityAvailable > 0 }
        var grouped: [String: [Product]] = [:]
        for item in active {
            let key = normalizeComparisonKey(item.title)
            if !key.isEmpty {
                grouped[key, default: []].append(item)
            }
        }

        let groups: [ProductComparisonGroup] = grouped.compactMap { key, list in
            guard let lead = list.first else { return nil }
            let ranked = compareProductOffers(offers: list, baseDistanceKm: baseDistanceKm)
            return ProductComparisonGroup(
                groupKey: key,
                title: lead.title,
                category: lead.category,
                offers: ranked,
                lowestStickerTzs: ranked.map(\.unitPriceTzs).min() ?? lead.priceTzs,
                highestStickerTzs: ranked.map(\.unitPriceTzs).max() ?? lead.priceTzs,
                lowestLandedTzs: ranked.map(\.landedTotalTzs).min() ?? lead.priceTzs,
                highestLandedTzs: ranked.map(\.landedTotalTzs).max() ?? lead.priceTzs
            )
        }

        return groups.sorted { a, b in
            if a.sellerCount != b.sellerCount { return a.sellerCount > b.sellerCount }
            if abs(a.priceSpreadTzs - b.priceSpreadTzs) > 0.01 { return a.priceSpreadTzs > b.priceSpreadTzs }
            return a.title < b.title
        }
    }

    public static func computeCategoryPriceBenchmark(
        products: [Product],
        category: ProductCategory?
    ) -> CategoryPriceBenchmark {
        let matching = products.filter {
            $0.isActive && (category == nil || $0.category == category) && $0.priceTzs > 0.0
        }
        guard !matching.isEmpty else {
            return CategoryPriceBenchmark(count: 0, lowestTzs: 0.0, averageTzs: 0.0, highestTzs: 0.0)
        }
        let prices = matching.map(\.priceTzs)
        let lowest = prices.min() ?? 0.0
        let highest = prices.max() ?? 0.0
        let avg = (prices.reduce(0.0, +) / Double(prices.count)).rounded()
        return CategoryPriceBenchmark(
            count: matching.count,
            lowestTzs: lowest,
            averageTzs: avg,
            highestTzs: highest
        )
    }

    public static func computeBoltTierQuote(
        tier: TransportTier,
        distanceKm: Double,
        cargoWeightKg: Double
    ) -> BoltTransportQuote {
        let km = max(1.0, distanceKm)
        let wt = max(0.1, cargoWeightKg)
        let eligible = wt <= tier.maxWeightKg
        let reason: String? = eligible
            ? nil
            : "Load (\(Int(wt)) kg) exceeds \(tier.displayName) limit (\(Int(tier.maxWeightKg)) kg)"
        let fare = tier.baseFareTzs + km * tier.perKmRateTzs
        let speedMultiplier: Double
        switch tier {
        case .TRADEE_BODA: speedMultiplier = 2.2
        case .TRADEE_MOTO_BOX: speedMultiplier = 2.5
        case .TRADEE_BAJAJI: speedMultiplier = 3.1
        case .TRADEE_TRUCK: speedMultiplier = 4.2
        }
        let eta = max(4, Int((4.0 + km * speedMultiplier).rounded()))
        let recommended: Bool
        switch tier {
        case .TRADEE_BODA: recommended = wt <= 3.0
        case .TRADEE_MOTO_BOX: recommended = wt > 3.0 && wt <= 35.0
        case .TRADEE_BAJAJI: recommended = wt > 35.0 && wt <= 250.0
        case .TRADEE_TRUCK: recommended = wt > 250.0
        }
        return BoltTransportQuote(
            tier: tier,
            distanceKm: km,
            cargoWeightKg: wt,
            fareTzs: fare,
            estimatedMinutes: eta,
            isRecommended: eligible && recommended,
            isEligible: eligible,
            ineligibleReason: reason
        )
    }

    public static func compareAllBoltTiers(
        distanceKm: Double,
        cargoWeightKg: Double
    ) -> [BoltTransportQuote] {
        TransportTier.allCases.map { computeBoltTierQuote(tier: $0, distanceKm: distanceKm, cargoWeightKg: cargoWeightKg) }
    }

    public static func calculateCartTotals(
        items: [CartItem],
        distanceKm: Double
    ) -> CartTotalsSummary {
        if items.isEmpty {
            return CartTotalsSummary(
                totalQuantity: 0,
                itemsSubtotalTzs: 0.0,
                transportFeeTzs: 0.0,
                grandTotalTzs: 0.0,
                anyRequiresTruck: false
            )
        }
        let totalQty = items.reduce(0) { $0 + max(1, $1.quantity) }
        let subtotal = items.reduce(0.0) { $0 + ($1.priceTzs * Double(max(1, $1.quantity))) }
        let transportFee = items.enumerated().reduce(0.0) { acc, pair in
            let (idx, item) = pair
            let legKm = idx == 0 ? distanceKm : 1.0
            return acc + computeTransportFeeTzs(requiresTruck: item.requiresTruck, distanceKm: legKm)
        }
        return CartTotalsSummary(
            totalQuantity: totalQty,
            itemsSubtotalTzs: subtotal,
            transportFeeTzs: transportFee,
            grandTotalTzs: subtotal + transportFee,
            anyRequiresTruck: items.contains(where: \.requiresTruck)
        )
    }

    public static func generateSecretSafetyCode() -> String {
        let digits = Int.random(in: 1000...9999)
        let words = ["SIMBA", "KILIMA", "ZANZIBAR", "SERENGETI", "DAR", "NYOTA", "AMANI"]
        let word = words.randomElement() ?? "SIMBA"
        let tailDigit = Int.random(in: 1...9)
        return "\(digits) — \"\(word)-\(tailDigit)\""
    }

    public static func generateReceiptNumber() -> String {
        let code = Int.random(in: 100000...999999)
        return "RCPT-TZ-\(code)"
    }
}

@MainActor
public final class TradeeStore: ObservableObject {
    @Published public var activeRole: UserRole = .BUYER
    @Published public var products: [Product] = []
    @Published public var favoriteIds: Set<String> = []
    @Published public var cartItems: [CartItem] = []
    @Published public var orders: [Order] = []
    @Published public var driverContract: DriverContract
    @Published public var searchQuery: String = ""
    @Published public var selectedCategory: ProductCategory? = nil
    @Published public var dealsOnly: Bool = false
    @Published public var bannerMessage: String? = nil

    public init() {
        self.driverContract = DriverContract(
            driverId: "drv_juma_01",
            driverName: "Juma Hassan",
            vehiclePlate: "MC 482 DTR",
            secretGpsImei: "TZ-SEC-GPS-883920",
            stationName: "Kariakoo Market Block A",
            vehicleStatus: .ACTIVE_PACKAGE_DELIVERY,
            cargoBoxAttached: true,
            rideHailPartner: .NONE,
            weeksPaidCount: 32,
            totalPaidTzs: 32.0 * 70_000.0,
            ownershipTransferred: false,
            curfewViolated: false,
            lastCheckedOutLabel: "06:02 EAT",
            lastCheckedInLabel: "20:41 EAT (Yesterday)"
        )
        seedDefaultCatalog()
    }

    public func seedDefaultCatalog() {
        self.products = [
            Product(id: "p_gamepad_1", providerId: "s_kariakoo", providerName: "Kariakoo Official Merchant", category: .ELECTRONICS, title: "Pro Wireless Gamepad DualShock", description: "Low-latency Bluetooth gamepad with textured grip for mobile & console.", priceTzs: 85_000, quantityAvailable: 14, sizeSpec: "16 x 11 cm", weightKg: 0.5, isDailyDeal: true, pickupLabel: "Kariakoo Market Block A", sellerRating: 4.9),
            Product(id: "p_gamepad_2", providerId: "s_samora", providerName: "Samora Digital Hub", category: .ELECTRONICS, title: "Pro Wireless Gamepad DualShock", description: "Official dual-vibration wireless controller + phone clip mount.", priceTzs: 81_000, quantityAvailable: 9, sizeSpec: "16 x 11 cm + Clip", weightKg: 0.55, isDailyDeal: false, pickupLabel: "Samora Tech Hub", sellerRating: 4.7),
            Product(id: "p_gamepad_3", providerId: "s_mlimani", providerName: "Mlimani Gaming Store", category: .ELECTRONICS, title: "Pro Wireless Gamepad DualShock", description: "Retail showroom pro controller pack with braided USB-C cable.", priceTzs: 89_000, quantityAvailable: 20, sizeSpec: "16 x 11 cm Pro", weightKg: 0.5, isDailyDeal: false, pickupLabel: "Mlimani City Retail Plaza", sellerRating: 4.8),
            Product(id: "p_phone_1", providerId: "s_samora", providerName: "Samora Digital Hub", category: .ELECTRONICS, title: "5G Rugged Smartphone 128GB", description: "Daylight-bright 6.7\" AMOLED display, 5000mAh battery, dual SIM.", priceTzs: 420_000, quantityAvailable: 7, sizeSpec: "162 x 76 mm", weightKg: 0.4, isDailyDeal: true, pickupLabel: "Samora Tech Hub", sellerRating: 4.9),
            Product(id: "p_phone_2", providerId: "s_kariakoo", providerName: "Kariakoo Official Merchant", category: .ELECTRONICS, title: "5G Rugged Smartphone 128GB", description: "Wholesale sealed box with 33W fast charger included.", priceTzs: 408_000, quantityAvailable: 12, sizeSpec: "128GB + Charger", weightKg: 0.45, isDailyDeal: true, pickupLabel: "Kariakoo Market Block A", sellerRating: 4.8),
            Product(id: "p_tv_1", providerId: "s_nyerere", providerName: "Nyerere Appliance Depot", category: .ELECTRONICS, title: "55-Inch 4K Smart LED TV", description: "Requires Phase 2 Commercial Light Truck due to box dimensions.", priceTzs: 980_000, quantityAvailable: 3, sizeSpec: "124 x 75 cm (Truck)", weightKg: 42.0, requiresTruck: true, isDailyDeal: false, pickupLabel: "Nyerere Rd Warehouse", sellerRating: 4.9),
            Product(id: "p_ball_1", providerId: "s_uhuru", providerName: "Uhuru Sports Hub", category: .SPORTS_AND_OUTDOORS, title: "Dar Premier Match Football", description: "FIFA-grade stitched synthetic leather match ball + pump.", priceTzs: 45_000, quantityAvailable: 20, sizeSpec: "Size 5 Official", weightKg: 0.7, isDailyDeal: true, pickupLabel: "Uhuru Sports Stall 4", sellerRating: 4.8),
            Product(id: "p_rice_1", providerId: "s_kariakoo", providerName: "Kariakoo Harvest Granary", category: .FOOD, title: "Kilombero Aromatic Rice (10 kg)", description: "Fresh harvest Grade-1 aromatic super rice, sealed woven sack.", priceTzs: 32_000, quantityAvailable: 35, sizeSpec: "10 kg Sack", weightKg: 10.0, isDailyDeal: true, pickupLabel: "Kariakoo Market Block A", sellerRating: 4.9),
            Product(id: "p_rice_2", providerId: "s_nyerere", providerName: "Nyerere Millers Co-op", category: .FOOD, title: "Kilombero Aromatic Rice (10 kg)", description: "Mill-direct wholesale 10 kg sack of pure Kilombero rice.", priceTzs: 29_500, quantityAvailable: 60, sizeSpec: "10 kg Mill Sack", weightKg: 10.0, isDailyDeal: true, pickupLabel: "Nyerere Rd Warehouse", sellerRating: 4.7),
            Product(id: "p_book_1", providerId: "s_posta", providerName: "Posta Bookshop", category: .BOOKS, title: "East Africa Business & Trade Law", description: "Comprehensive 2026 commercial edition with Tanzania statutes.", priceTzs: 38_000, quantityAvailable: 11, sizeSpec: "Paperback 420p", weightKg: 0.8, isDailyDeal: false, pickupLabel: "Posta Bookshop Stall 2", sellerRating: 4.9),
            Product(id: "p_shirt_1", providerId: "s_mwenge", providerName: "Mwenge Fashion Arcade", category: .CLOTHING, title: "Kitenge Tailored Linen Shirt", description: "Breathable cotton-linen collar shirt crafted in Dar es Salaam.", priceTzs: 55_000, quantityAvailable: 16, sizeSpec: "M / L / XL", weightKg: 0.4, isDailyDeal: true, pickupLabel: "Mwenge Crafts Arcade", sellerRating: 4.8),
            Product(id: "p_shirt_2", providerId: "s_kariakoo", providerName: "Kariakoo Textile House", category: .CLOTHING, title: "Kitenge Tailored Linen Shirt", description: "Bespoke Kariakoo textile edition in royal blue and gold print.", priceTzs: 49_000, quantityAvailable: 22, sizeSpec: "M / L / XL", weightKg: 0.4, isDailyDeal: false, pickupLabel: "Kariakoo Market Block A", sellerRating: 4.7),
            Product(id: "p_shoes_1", providerId: "s_mlimani", providerName: "Mlimani Footwear Hub", category: .SHOES, title: "All-Weather Trail Sneakers", description: "Slip-resistant sole built for city commutes and rain.", priceTzs: 92_000, quantityAvailable: 9, sizeSpec: "EU 41 - 45", weightKg: 1.1, isDailyDeal: false, pickupLabel: "Mlimani City Retail Plaza", sellerRating: 4.8),
            Product(id: "p_shoes_2", providerId: "s_kariakoo", providerName: "Kariakoo Footwear Depot", category: .SHOES, title: "All-Weather Trail Sneakers", description: "Direct wholesale import with cushioned insole.", priceTzs: 86_000, quantityAvailable: 15, sizeSpec: "EU 41 - 44", weightKg: 1.1, isDailyDeal: true, pickupLabel: "Kariakoo Market Block A", sellerRating: 4.7),
            Product(id: "p_ring_1", providerId: "s_slipway", providerName: "Slipway Artisan Guild", category: .JEWELLERY, title: "Zanzibar Silver Signet Ring", description: "Hand-engraved 925 sterling silver coastal motif ring.", priceTzs: 110_000, quantityAvailable: 6, sizeSpec: "Ring Size 8-10", weightKg: 0.1, isDailyDeal: true, pickupLabel: "Slipway Artisan Guild", sellerRating: 5.0),
            Product(id: "p_beauty_1", providerId: "s_kinondoni", providerName: "Kinondoni Botanics", category: .BEAUTY_AND_PERSONAL_CARE, title: "Cold-Pressed Baobab & Shea Kit", description: "Natural daytime moisture balm and botanical cleanser.", priceTzs: 28_000, quantityAvailable: 24, sizeSpec: "250 ml + 100 g", weightKg: 0.6, isDailyDeal: false, pickupLabel: "Kinondoni Botanics", sellerRating: 4.8)
        ]
    }

    public var filteredProducts: [Product] {
        let q = searchQuery.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        return products.filter { item in
            let matchesQuery = q.isEmpty ||
                item.title.lowercased().contains(q) ||
                item.category.displayName.lowercased().contains(q) ||
                item.providerName.lowercased().contains(q) ||
                item.description.lowercased().contains(q)
            let matchesCat = selectedCategory == nil || item.category == selectedCategory
            let matchesDeals = !dealsOnly || item.isDailyDeal
            return matchesQuery && matchesCat && matchesDeals && item.isActive
        }
    }

    public func toggleFavorite(_ product: Product) {
        if favoriteIds.contains(product.id) {
            favoriteIds.remove(product.id)
            bannerMessage = "Removed \"\(product.title)\" from favorites"
        } else {
            favoriteIds.insert(product.id)
            bannerMessage = "Saved \"\(product.title)\" to favorites"
        }
    }

    public func addToCart(_ product: Product, quantity: Int = 1) {
        if let idx = cartItems.firstIndex(where: { $0.productId == product.id }) {
            cartItems[idx].quantity += quantity
        } else {
            cartItems.append(
                CartItem(
                    productId: product.id,
                    providerName: product.providerName,
                    category: product.category,
                    title: product.title,
                    priceTzs: product.priceTzs,
                    quantity: max(1, quantity),
                    sizeSpec: product.sizeSpec,
                    weightKg: product.weightKg,
                    requiresTruck: product.requiresTruck,
                    pickupLabel: product.pickupLabel
                )
            )
        }
        bannerMessage = "Added \"\(product.title)\" to cart"
    }

    @discardableResult
    public func placeOrder(
        product: Product,
        quantity: Int = 1,
        distanceKm: Double = 4.0,
        paymentMethod: PaymentMethod = .CASH_BY_HAND_WITH_RECEIPT
    ) -> Order {
        let qty = max(1, quantity)
        let subtotal = product.priceTzs * Double(qty)
        let transport = TradeeCalculator.computeTransportFeeTzs(requiresTruck: product.requiresTruck, distanceKm: distanceKm)
        let code = TradeeCalculator.generateSecretSafetyCode()
        let order = Order(
            id: "ord_\(UUID().uuidString.prefix(8))",
            productId: product.id,
            productTitle: product.title,
            category: product.category,
            selectedSize: product.sizeSpec,
            quantity: qty,
            unitPriceTzs: product.priceTzs,
            itemSubtotalTzs: subtotal,
            transportFeeTzs: transport,
            totalAmountTzs: subtotal + transport,
            paymentMethod: paymentMethod,
            secretSafetyCode: code,
            receiptNumber: TradeeCalculator.generateReceiptNumber(),
            driverName: driverContract.driverName,
            driverPlate: product.requiresTruck ? "T 884 DTR" : driverContract.vehiclePlate,
            requiresTruck: product.requiresTruck,
            cargoBoxAttached: true,
            status: .IN_TRANSIT,
            refundAmountTzs: 0.0,
            disturbanceCompTzs: 0.0,
            driverLat: -6.8180,
            driverLng: 39.2750,
            destLat: -6.7920,
            destLng: 39.2680
        )
        orders.insert(order, at: 0)
        bannerMessage = "Order placed! Verbal Secret Code: \(code)"
        return order
    }

    @discardableResult
    public func dispatchBoltTransport(
        tier: TransportTier,
        isSellerMode: Bool,
        pickupHub: String,
        dropoffLabel: String,
        distanceKm: Double,
        weightKg: Double,
        paymentMethod: PaymentMethod = .CASH_BY_HAND_WITH_RECEIPT
    ) -> Order {
        let quote = TradeeCalculator.computeBoltTierQuote(tier: tier, distanceKm: distanceKm, cargoWeightKg: weightKg)
        let code = TradeeCalculator.generateSecretSafetyCode()
        let title = "Bolt \(tier.displayName) · \(isSellerMode ? "Seller Cargo" : "Buyer Ride") (\(pickupHub))"
        let order = Order(
            id: "bolt_\(UUID().uuidString.prefix(8))",
            productId: "bolt_service_\(tier.rawValue)",
            productTitle: title,
            category: .SPORTS_AND_OUTDOORS,
            selectedSize: "\(Int(quote.distanceKm)) km → \(dropoffLabel)",
            quantity: 1,
            unitPriceTzs: tier.baseFareTzs,
            itemSubtotalTzs: tier.baseFareTzs,
            transportFeeTzs: max(0.0, quote.fareTzs - tier.baseFareTzs),
            totalAmountTzs: quote.fareTzs,
            paymentMethod: paymentMethod,
            secretSafetyCode: code,
            receiptNumber: TradeeCalculator.generateReceiptNumber(),
            driverName: driverContract.driverName,
            driverPlate: tier.requiresTruck ? "T 884 DTR" : driverContract.vehiclePlate,
            requiresTruck: tier.requiresTruck,
            cargoBoxAttached: tier != .TRADEE_BODA,
            status: .IN_TRANSIT,
            refundAmountTzs: 0.0,
            disturbanceCompTzs: 0.0,
            driverLat: -6.8180,
            driverLng: 39.2750,
            destLat: -6.7920,
            destLng: 39.2680
        )
        orders.insert(order, at: 0)
        bannerMessage = "\(tier.displayName) Dispatched! Safety Code: \(code)"
        return order
    }

    public func publishSellerProduct(
        category: ProductCategory,
        title: String,
        description: String,
        priceTzs: Double,
        quantity: Int,
        sizeSpec: String,
        weightKg: Double,
        pickupLabel: String,
        isDailyDeal: Bool
    ) {
        let newProduct = Product(
            id: "p_\(UUID().uuidString.prefix(8))",
            providerId: "s_current_seller",
            providerName: "Kariakoo Official Merchant",
            category: category,
            title: title,
            description: description,
            priceTzs: priceTzs,
            quantityAvailable: max(1, quantity),
            sizeSpec: sizeSpec,
            weightKg: max(0.1, weightKg),
            requiresTruck: weightKg > TradeeCalculator.motorcycleMaxWeightKg,
            isDailyDeal: isDailyDeal,
            isActive: true,
            pickupLabel: pickupLabel
        )
        products.insert(newProduct, at: 0)
        bannerMessage = newProduct.requiresTruck
            ? "Published (Auto-flagged: Commercial Truck Required > 35 kg)"
            : "Published for Motorcycle Cargo Box Delivery"
    }

    public func claimRuleADamagedOrLostRefund(orderId: String) {
        guard let idx = orders.firstIndex(where: { $0.id == orderId }),
              !orders[idx].status.isTerminal else { return }
        orders[idx].status = .DAMAGED_OR_LOST_REFUNDED
        orders[idx].disturbanceCompTzs = TradeeCalculator.disturbanceFeeTzs
        orders[idx].refundAmountTzs = orders[idx].totalAmountTzs + TradeeCalculator.disturbanceFeeTzs
        bannerMessage = "Rule A Refund Approved: \(TradeeTheme.formatTzs(orders[idx].refundAmountTzs)) (100% + TSh 1,000 Disturbance)"
    }

    public func claimRuleBRobberyOrHarmRefund(orderId: String) {
        guard let idx = orders.firstIndex(where: { $0.id == orderId }),
              !orders[idx].status.isTerminal else { return }
        orders[idx].status = .ROBBERY_OR_HARM_REFUNDED
        orders[idx].disturbanceCompTzs = 0.0
        orders[idx].refundAmountTzs = orders[idx].totalAmountTzs
        bannerMessage = "Rule B Safety Protocol: Route Frozen & 100% Refund (\(TradeeTheme.formatTzs(orders[idx].refundAmountTzs))) Issued"
    }

    public func confirmSafeReceipt(orderId: String) {
        guard let idx = orders.firstIndex(where: { $0.id == orderId }),
              !orders[idx].status.isTerminal else { return }
        orders[idx].status = .DELIVERED_COMPLETED
        bannerMessage = "Receipt \(orders[idx].receiptNumber) Confirmed!"
    }

    public func recordWeeklyLeasePayment(weeks: Int = 1) {
        let nextWeeks = min(104, driverContract.weeksPaidCount + max(1, weeks))
        driverContract.weeksPaidCount = nextWeeks
        driverContract.totalPaidTzs = Double(nextWeeks) * Double(TradeeCalculator.weeklyLeaseFeeTzs)
        driverContract.ownershipTransferred = nextWeeks >= 104
        bannerMessage = driverContract.ownershipTransferred
            ? "104/104 Weeks Complete! Permanent Motorcycle Ownership Transferred!"
            : "Recorded +\(weeks) week(s) · \(nextWeeks)/104 weeks paid"
    }
}
