import Foundation

public enum UserRole: String, CaseIterable, Codable, Identifiable {
    case BUYER
    case PROVIDER
    case DRIVER

    public var id: String { rawValue }

    public var title: String {
        switch self {
        case .BUYER: return "Buyer Marketplace"
        case .PROVIDER: return "Seller & Provider"
        case .DRIVER: return "Fleet Driver"
        }
    }

    public var subtitle: String {
        switch self {
        case .BUYER: return "Browse 8 categories, compare prices & order Bolt delivery"
        case .PROVIDER: return "List inventory in TSh & dispatch Bolt couriers"
        case .DRIVER: return "06:00-21:00 shift, cargo box & 2-year lease-to-own"
        }
    }
}

public enum ProductCategory: String, CaseIterable, Codable, Identifiable {
    case SPORTS_AND_OUTDOORS
    case ELECTRONICS
    case FOOD
    case BOOKS
    case CLOTHING
    case SHOES
    case JEWELLERY
    case BEAUTY_AND_PERSONAL_CARE

    public var id: String { rawValue }

    public var displayName: String {
        switch self {
        case .SPORTS_AND_OUTDOORS: return "Sports & Outdoors"
        case .ELECTRONICS: return "Electronics"
        case .FOOD: return "Food"
        case .BOOKS: return "Books"
        case .CLOTHING: return "Clothing"
        case .SHOES: return "Shoes"
        case .JEWELLERY: return "Jewellery"
        case .BEAUTY_AND_PERSONAL_CARE: return "Beauty & Care"
        }
    }

    public var tagline: String {
        switch self {
        case .SPORTS_AND_OUTDOORS: return "Fitness gear, balls & outdoor equipment"
        case .ELECTRONICS: return "Phones, controllers, audio & smart tech"
        case .FOOD: return "Fresh groceries, grains & packaged meals"
        case .BOOKS: return "Textbooks, novels & professional guides"
        case .CLOTHING: return "Everyday fashion, tailored & streetwear"
        case .SHOES: return "Sneakers, boots & formal footwear"
        case .JEWELLERY: return "Watches, rings & handcrafted pieces"
        case .BEAUTY_AND_PERSONAL_CARE: return "Skincare, grooming & wellness essentials"
        }
    }

    public var sfSymbol: String {
        switch self {
        case .SPORTS_AND_OUTDOORS: return "figure.run"
        case .ELECTRONICS: return "gamecontroller.fill"
        case .FOOD: return "basket.fill"
        case .BOOKS: return "book.fill"
        case .CLOTHING: return "tshirt.fill"
        case .SHOES: return "shoe.fill"
        case .JEWELLERY: return "sparkles"
        case .BEAUTY_AND_PERSONAL_CARE: return "leaf.fill"
        }
    }
}

public enum PaymentMethod: String, CaseIterable, Codable, Identifiable {
    case CASH_BY_HAND_WITH_RECEIPT
    case FIREWALL_CARD_PAYMENT

    public var id: String { rawValue }

    public var title: String {
        switch self {
        case .CASH_BY_HAND_WITH_RECEIPT: return "Cash by Hand + Official Receipt"
        case .FIREWALL_CARD_PAYMENT: return "Firewall-Protected Card / Mobile Pay"
        }
    }

    public var detail: String {
        switch self {
        case .CASH_BY_HAND_WITH_RECEIPT: return "Pay TSh on arrival after verifying your verbal secret safety code"
        case .FIREWALL_CARD_PAYMENT: return "TLS 1.3 tokenized checkout with 100% escrow protection"
        }
    }
}

public enum OrderStatus: String, CaseIterable, Codable, Identifiable {
    case PENDING_DISPATCH
    case DRIVER_ASSIGNED
    case BOX_ATTACHED_PICKED_UP
    case IN_TRANSIT
    case ARRIVED_AWAITING_SECRET_CODE
    case DELIVERED_COMPLETED
    case DAMAGED_OR_LOST_REFUNDED
    case ROBBERY_OR_HARM_REFUNDED

    public var id: String { rawValue }

    public var label: String {
        switch self {
        case .PENDING_DISPATCH: return "Awaiting Courier Dispatch"
        case .DRIVER_ASSIGNED: return "Driver Assigned (06:00-21:00 Shift)"
        case .BOX_ATTACHED_PICKED_UP: return "Cargo Box Locked & Picked Up"
        case .IN_TRANSIT: return "In Transit · Live GPS Active"
        case .ARRIVED_AWAITING_SECRET_CODE: return "At Gate · Speak Secret Code"
        case .DELIVERED_COMPLETED: return "Delivered & Receipt Confirmed"
        case .DAMAGED_OR_LOST_REFUNDED: return "Refunded (100% + TSh 1,000 Disturbance)"
        case .ROBBERY_OR_HARM_REFUNDED: return "Refunded (100% Robbery/Safety Protection)"
        }
    }

    public var isTerminal: Bool {
        switch self {
        case .DELIVERED_COMPLETED, .DAMAGED_OR_LOST_REFUNDED, .ROBBERY_OR_HARM_REFUNDED:
            return true
        default:
            return false
        }
    }
}

public enum VehicleStatus: String, CaseIterable, Codable, Identifiable {
    case STATION_CHECKED_IN
    case ACTIVE_PACKAGE_DELIVERY
    case ACTIVE_PASSENGER_IDLE_MODE
    case CURFEW_BREACH_STOLEN_ALERT
    case MAINTENANCE

    public var id: String { rawValue }

    public var label: String {
        switch self {
        case .STATION_CHECKED_IN: return "Checked In at Station (Safe)"
        case .ACTIVE_PACKAGE_DELIVERY: return "Mode A · Cargo Box Package Delivery"
        case .ACTIVE_PASSENGER_IDLE_MODE: return "Mode B · Idle Passenger Ride-Hail"
        case .CURFEW_BREACH_STOLEN_ALERT: return "21:00 CURFEW BREACH · 1Hz Secret GPS"
        case .MAINTENANCE: return "Station Maintenance"
        }
    }
}

public enum RideHailPartner: String, CaseIterable, Codable, Identifiable {
    case NONE
    case STREET_HAIL
    case BOLT
    case UBER
    case LINKER

    public var id: String { rawValue }

    public var label: String {
        switch self {
        case .NONE: return "Tradee Package Mode Only"
        case .STREET_HAIL: return "Direct Street Passenger"
        case .BOLT: return "Bolt Bodaboda Partner"
        case .UBER: return "Uber Moto Partner"
        case .LINKER: return "Linker Ride Partner"
        }
    }
}

public enum TransportTier: String, CaseIterable, Codable, Identifiable {
    case TRADEE_BODA
    case TRADEE_MOTO_BOX
    case TRADEE_BAJAJI
    case TRADEE_TRUCK

    public var id: String { rawValue }

    public var displayName: String {
        switch self {
        case .TRADEE_BODA: return "Tradee Boda Ride"
        case .TRADEE_MOTO_BOX: return "Tradee Moto Cargo Box"
        case .TRADEE_BAJAJI: return "Tradee Bajaji (3-Wheeler)"
        case .TRADEE_TRUCK: return "Tradee Commercial Truck"
        }
    }

    public var subtitle: String {
        switch self {
        case .TRADEE_BODA: return "Express Buyer Ride or Light Parcel (<= 15 kg)"
        case .TRADEE_MOTO_BOX: return "Locked Rear Box for Marketplace Orders (<= 35 kg)"
        case .TRADEE_BAJAJI: return "Covered Three-Wheeler for Multi-Sack / Seller Restock (<= 250 kg)"
        case .TRADEE_TRUCK: return "Phase 2 Heavy Commercial Truck for Appliances & Bulk (<= 5,000 kg)"
        }
    }

    public var baseFareTzs: Double {
        switch self {
        case .TRADEE_BODA: return 1_500.0
        case .TRADEE_MOTO_BOX: return 2_500.0
        case .TRADEE_BAJAJI: return 5_000.0
        case .TRADEE_TRUCK: return 8_000.0
        }
    }

    public var baseFeeTzs: Double { baseFareTzs }

    public var perKmRateTzs: Double {
        switch self {
        case .TRADEE_BODA: return 400.0
        case .TRADEE_MOTO_BOX: return 500.0
        case .TRADEE_BAJAJI: return 800.0
        case .TRADEE_TRUCK: return 500.0
        }
    }

    public var maxWeightKg: Double {
        switch self {
        case .TRADEE_BODA: return 15.0
        case .TRADEE_MOTO_BOX: return 35.0
        case .TRADEE_BAJAJI: return 250.0
        case .TRADEE_TRUCK: return 5_000.0
        }
    }

    public var requiresTruck: Bool {
        self == .TRADEE_TRUCK
    }
}

public struct Product: Identifiable, Codable, Equatable {
    public let id: String
    public var providerId: String
    public var providerName: String
    public var category: ProductCategory
    public var title: String
    public var description: String
    public var priceTzs: Double
    public var quantityAvailable: Int
    public var sizeSpec: String
    public var weightKg: Double
    public var requiresTruck: Bool
    public var isDailyDeal: Bool
    public var isActive: Bool
    public var pickupLabel: String
    public var sellerRating: Double

    public init(
        id: String,
        providerId: String = "seller_default",
        providerName: String = "Kariakoo Official Merchant",
        category: ProductCategory = .ELECTRONICS,
        title: String,
        description: String = "",
        priceTzs: Double,
        quantityAvailable: Int = 10,
        sizeSpec: String = "Standard",
        weightKg: Double = 1.0,
        requiresTruck: Bool = false,
        isDailyDeal: Bool = false,
        isActive: Bool = true,
        pickupLabel: String = "Kariakoo Market Block A",
        sellerRating: Double = 4.8
    ) {
        self.id = id
        self.providerId = providerId
        self.providerName = providerName
        self.category = category
        self.title = title
        self.description = description
        self.priceTzs = priceTzs
        self.quantityAvailable = quantityAvailable
        self.sizeSpec = sizeSpec
        self.weightKg = weightKg
        self.requiresTruck = requiresTruck || (weightKg > 35.0)
        self.isDailyDeal = isDailyDeal
        self.isActive = isActive
        self.pickupLabel = pickupLabel
        self.sellerRating = sellerRating
    }
}

public struct CartItem: Identifiable, Codable, Equatable {
    public var id: String { productId }
    public let productId: String
    public var providerName: String
    public var category: ProductCategory
    public var title: String
    public var priceTzs: Double
    public var quantity: Int
    public var sizeSpec: String
    public var weightKg: Double
    public var requiresTruck: Bool
    public var pickupLabel: String

    public init(
        productId: String,
        providerName: String,
        category: ProductCategory = .ELECTRONICS,
        title: String,
        priceTzs: Double,
        quantity: Int = 1,
        sizeSpec: String = "Standard",
        weightKg: Double = 1.0,
        requiresTruck: Bool = false,
        pickupLabel: String = "Kariakoo Market Block A"
    ) {
        self.productId = productId
        self.providerName = providerName
        self.category = category
        self.title = title
        self.priceTzs = priceTzs
        self.quantity = quantity
        self.sizeSpec = sizeSpec
        self.weightKg = weightKg
        self.requiresTruck = requiresTruck
        self.pickupLabel = pickupLabel
    }
}

public struct CartTotalsSummary: Equatable {
    public let totalQuantity: Int
    public let itemsSubtotalTzs: Double
    public let transportFeeTzs: Double
    public let grandTotalTzs: Double
    public let anyRequiresTruck: Bool
}

public struct BoltTransportQuote: Identifiable, Equatable {
    public var id: String { tier.rawValue }
    public let tier: TransportTier
    public let distanceKm: Double
    public let cargoWeightKg: Double
    public let fareTzs: Double
    public let estimatedMinutes: Int
    public let isRecommended: Bool
    public let isEligible: Bool
    public let ineligibleReason: String?
}

public struct SellerOfferComparison: Identifiable, Equatable {
    public var id: String { product.id }
    public let product: Product
    public let rank: Int
    public let effectiveDistanceKm: Double
    public let unitPriceTzs: Double
    public let transportFeeTzs: Double
    public let landedTotalTzs: Double
    public let savingsVsHighestTzs: Double
    public let isBestLandedValue: Bool
    public let isLowestStickerPrice: Bool
}

public struct ProductComparisonGroup: Identifiable, Equatable {
    public var id: String { groupKey }
    public let groupKey: String
    public let title: String
    public let category: ProductCategory
    public let offers: [SellerOfferComparison]
    public let lowestStickerTzs: Double
    public let highestStickerTzs: Double
    public let lowestLandedTzs: Double
    public let highestLandedTzs: Double

    public var sellerCount: Int { offers.count }
    public var priceSpreadTzs: Double { max(0.0, highestStickerTzs - lowestStickerTzs) }
}

public struct CategoryPriceBenchmark: Equatable {
    public let count: Int
    public let lowestTzs: Double
    public let averageTzs: Double
    public let highestTzs: Double

    public init(count: Int, lowestTzs: Double, averageTzs: Double, highestTzs: Double) {
        self.count = count
        self.lowestTzs = lowestTzs
        self.averageTzs = averageTzs
        self.highestTzs = highestTzs
    }
}

public struct Order: Identifiable, Codable, Equatable {
    public let id: String
    public var productId: String
    public var productTitle: String
    public var category: ProductCategory
    public var selectedSize: String
    public var quantity: Int
    public var unitPriceTzs: Double
    public var itemSubtotalTzs: Double
    public var transportFeeTzs: Double
    public var totalAmountTzs: Double
    public var paymentMethod: PaymentMethod
    public var secretSafetyCode: String
    public var receiptNumber: String
    public var driverName: String
    public var driverPlate: String
    public var requiresTruck: Bool
    public var cargoBoxAttached: Bool
    public var status: OrderStatus
    public var refundAmountTzs: Double
    public var disturbanceCompTzs: Double
    public var driverLat: Double
    public var driverLng: Double
    public var destLat: Double
    public var destLng: Double
}

public struct DriverContract: Identifiable, Codable, Equatable {
    public var id: String { driverId }
    public var driverId: String
    public var driverName: String
    public var vehiclePlate: String
    public var secretGpsImei: String
    public var stationName: String
    public var vehicleStatus: VehicleStatus
    public var cargoBoxAttached: Bool
    public var rideHailPartner: RideHailPartner
    public let weeklyFeeTzs: Int64 = 70_000
    public let totalWeeksRequired: Int = 104
    public var weeksPaidCount: Int
    public var totalPaidTzs: Double
    public var ownershipTransferred: Bool
    public var curfewViolated: Bool
    public var lastCheckedOutLabel: String
    public var lastCheckedInLabel: String
}
