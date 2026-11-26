package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class UserRole(val key: String, val title: String, val subtitle: String) {
  BUYER("BUYER", "Buyer", "Shop, Compare Prices & Order Transport"),
  PROVIDER("PROVIDER", "Provider / Seller", "Post Products, Compare & Dispatch"),
  DRIVER("DRIVER", "Fleet Driver", "Deliveries, Ride-Hail & 2-Yr Lease Contract");

  companion object {
    fun fromKey(key: String?): UserRole = entries.firstOrNull { it.key == key } ?: BUYER
  }
}

enum class ProductCategory(
  val key: String,
  val displayName: String,
  val carouselLabel: String,
  val tagline: String,
) {
  SPORTS_AND_OUTDOORS(
    "SPORTS_AND_OUTDOORS",
    "Sports & Outdoors",
    "SPORTS",
    "Gear, Fitness & Trail",
  ),
  ELECTRONICS(
    "ELECTRONICS",
    "Electronics",
    "ELECTRONIC",
    "Phones, Gaming & Audio",
  ),
  FOOD(
    "FOOD",
    "Food",
    "FOOD",
    "Fresh Market & Meals",
  ),
  BOOKS(
    "BOOKS",
    "Books",
    "BOOKS",
    "Study, Fiction & Guides",
  ),
  CLOTHING(
    "CLOTHING",
    "Clothing",
    "CLOTHING",
    "Everyday & Tailored Wear",
  ),
  SHOES(
    "SHOES",
    "Shoes",
    "SHOES",
    "Sneakers, Boots & Sandals",
  ),
  JEWELLERY(
    "JEWELLERY",
    "Jewellery",
    "JEWELLERY",
    "Watches, Rings & Craft",
  ),
  BEAUTY_AND_PERSONAL_CARE(
    "BEAUTY_AND_PERSONAL_CARE",
    "Beauty & Personal Care",
    "BEAUTY",
    "Skincare & Grooming",
  );

  companion object {
    fun fromKey(key: String?): ProductCategory =
      entries.firstOrNull { it.key == key } ?: ELECTRONICS
  }
}

enum class PaymentMethod(val key: String, val label: String, val badge: String) {
  CASH_BY_HAND_WITH_RECEIPT(
    "CASH_BY_HAND_WITH_RECEIPT",
    "Cash by Hand on Delivery (with Receipt)",
    "Phase 1 Standard",
  ),
  FIREWALL_CARD_PAYMENT(
    "FIREWALL_CARD_PAYMENT",
    "Firewall-Protected Card Payment",
    "Phase 2 Tokenized",
  );

  companion object {
    fun fromKey(key: String?): PaymentMethod =
      entries.firstOrNull { it.key == key } ?: CASH_BY_HAND_WITH_RECEIPT
  }
}

enum class OrderStatus(val key: String, val label: String, val isTerminal: Boolean) {
  PENDING_DISPATCH("PENDING_DISPATCH", "Pending Dispatch", false),
  DRIVER_ASSIGNED("DRIVER_ASSIGNED", "Driver Assigned", false),
  BOX_ATTACHED_PICKED_UP("BOX_ATTACHED_PICKED_UP", "Cargo Box Mounted & Picked Up", false),
  IN_TRANSIT("IN_TRANSIT", "In Transit to Gate", false),
  ARRIVED_AWAITING_SECRET_CODE("ARRIVED_AWAITING_SECRET_CODE", "Arrived — Verify Secret Code", false),
  DELIVERED_COMPLETED("DELIVERED_COMPLETED", "Delivered & Receipt Issued", true),
  DAMAGED_OR_LOST_REFUNDED("DAMAGED_OR_LOST_REFUNDED", "Refunded (100%)", true),
  ROBBERY_OR_HARM_REFUNDED("ROBBERY_OR_HARM_REFUNDED", "Incident Refunded (100%)", true);

  companion object {
    fun fromKey(key: String?): OrderStatus =
      entries.firstOrNull { it.key == key } ?: PENDING_DISPATCH
  }
}

enum class VehicleStatus(val key: String, val label: String) {
  STATION_CHECKED_IN("STATION_CHECKED_IN", "Checked In at Station"),
  ACTIVE_PACKAGE_DELIVERY("ACTIVE_PACKAGE_DELIVERY", "Mode A: Package Delivery"),
  ACTIVE_PASSENGER_IDLE_MODE("ACTIVE_PASSENGER_IDLE_MODE", "Mode B: Idle Passenger Mode"),
  CURFEW_BREACH_STOLEN_ALERT("CURFEW_BREACH_STOLEN_ALERT", "CURFEW BREACH · 1Hz GPS LOCK"),
  MAINTENANCE("MAINTENANCE", "Station Maintenance");

  companion object {
    fun fromKey(key: String?): VehicleStatus =
      entries.firstOrNull { it.key == key } ?: STATION_CHECKED_IN
  }
}

enum class RideHailPartner(val key: String, val label: String) {
  NONE("NONE", "Off Duty"),
  STREET_HAIL("STREET_HAIL", "Street Hail"),
  BOLT("BOLT", "Bolt"),
  UBER("UBER", "Uber"),
  LINKER("LINKER", "Linker");

  companion object {
    fun fromKey(key: String?): RideHailPartner =
      entries.firstOrNull { it.key == key } ?: NONE
  }
}

enum class TransportTier(
  val key: String,
  val displayName: String,
  val subtitle: String,
  val baseFeeTzs: Double,
  val perKmRateTzs: Double,
  val maxWeightKg: Double,
  val baseEtaMinutes: Int,
  val requiresTruck: Boolean,
  val badge: String,
) {
  TRADEE_BODA(
    key = "TRADEE_BODA",
    displayName = "Tradee Boda Express",
    subtitle = "Passenger Ride or Express Small Parcel",
    baseFeeTzs = 1_500.0,
    perKmRateTzs = 400.0,
    maxWeightKg = 15.0,
    baseEtaMinutes = 4,
    requiresTruck = false,
    badge = "Fastest · Bolt Style",
  ),
  TRADEE_MOTO_BOX(
    key = "TRADEE_MOTO_BOX",
    displayName = "Tradee Moto Cargo Box",
    subtitle = "Detachable Rear Box (55×45×45 cm)",
    baseFeeTzs = 2_500.0,
    perKmRateTzs = 500.0,
    maxWeightKg = 35.0,
    baseEtaMinutes = 6,
    requiresTruck = false,
    badge = "Phase 1 Standard",
  ),
  TRADEE_BAJAJI(
    key = "TRADEE_BAJAJI",
    displayName = "Tradee Bajaji Tri-Cargo",
    subtitle = "Covered 3-Wheeler for Stall Restock & Medium Loads",
    baseFeeTzs = 5_000.0,
    perKmRateTzs = 800.0,
    maxWeightKg = 250.0,
    baseEtaMinutes = 9,
    requiresTruck = false,
    badge = "Seller Favorite",
  ),
  TRADEE_TRUCK(
    key = "TRADEE_TRUCK",
    displayName = "Tradee Canter Commercial Truck",
    subtitle = "Heavy Appliances, Bulk Sacks & Warehouse Freight",
    baseFeeTzs = 8_000.0,
    perKmRateTzs = 500.0,
    maxWeightKg = 5_000.0,
    baseEtaMinutes = 15,
    requiresTruck = true,
     badge = "Phase 2 Heavy Fleet",
  );

  val baseFareTzs: Double
    get() = baseFeeTzs

  companion object {
    fun fromKey(key: String?): TransportTier =
      entries.firstOrNull { it.key == key } ?: TRADEE_MOTO_BOX
  }
}

@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val fullName: String = "",
  val phoneNumber: String = "",
  val activeRole: String = UserRole.BUYER.key,
  val termsAccepted: Boolean = false,
  val stationName: String = "Kariakoo Market Block A",
  val vehiclePlate: String = "MC 482 DTR",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class Product(
  @DocumentId val id: String = "",
  val providerId: String = "",
  val providerName: String = "",
  val category: String = ProductCategory.ELECTRONICS.key,
  val title: String = "",
  val description: String = "",
  val priceTzs: Double = 0.0,
  val quantityAvailable: Long = 0L,
  val sizeSpec: String = "",
  val weightKg: Double = 0.5,
  val requiresTruck: Boolean = false,
  val isDailyDeal: Boolean = false,
  val isActive: Boolean = true,
  val pickupLabel: String = "Kariakoo Market Block A",
  val photoUri: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class Order(
  @DocumentId val id: String = "",
  val buyerId: String = "",
  val providerId: String = "",
  val driverId: String = "",
  val participantIds: List<String> = emptyList(),
  val productId: String = "",
  val productTitle: String = "",
  val category: String = ProductCategory.ELECTRONICS.key,
  val selectedSize: String = "",
  val quantity: Long = 1L,
  val unitPriceTzs: Double = 0.0,
  val itemSubtotalTzs: Double = 0.0,
  val transportFeeTzs: Double = 0.0,
  val totalAmountTzs: Double = 0.0,
  val paymentMethod: String = PaymentMethod.CASH_BY_HAND_WITH_RECEIPT.key,
  val secretSafetyCode: String = "",
  val receiptNumber: String = "",
  val driverName: String = "",
  val driverPlate: String = "",
  val requiresTruck: Boolean = false,
  val cargoBoxAttached: Boolean = true,
  val status: String = OrderStatus.PENDING_DISPATCH.key,
  val refundAmountTzs: Double = 0.0,
  val disturbanceCompTzs: Double = 0.0,
  val driverLat: Double = -6.8180,
  val driverLng: Double = 39.2750,
  val destLat: Double = -6.7920,
  val destLng: Double = 39.2680,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class DriverContract(
  @DocumentId val id: String = "",
  val driverId: String = "",
  val driverName: String = "",
  val vehiclePlate: String = "MC 482 DTR",
  val secretGpsImei: String = "IMEI-8642091100482",
  val stationName: String = "Kariakoo Market Block A",
  val vehicleStatus: String = VehicleStatus.STATION_CHECKED_IN.key,
  val cargoBoxAttached: Boolean = false,
  val rideHailPartner: String = RideHailPartner.NONE.key,
  val weeklyFeeTzs: Long = 70_000L,
  val totalWeeksRequired: Long = 104L,
  val weeksPaidCount: Long = 0L,
  val totalPaidTzs: Double = 0.0,
  val ownershipTransferred: Boolean = false,
  val curfewViolated: Boolean = false,
  val lastCheckedOutLabel: String? = null,
  val lastCheckedInLabel: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class CartItem(
  @DocumentId val id: String = "",
  val productId: String = "",
  val providerId: String = "",
  val providerName: String = "",
  val category: String = ProductCategory.ELECTRONICS.key,
  val title: String = "",
  val priceTzs: Double = 0.0,
  val quantity: Long = 1L,
  val sizeSpec: String = "Standard",
  val weightKg: Double = 0.5,
  val requiresTruck: Boolean = false,
  val pickupLabel: String = "Kariakoo Market Block A",
  val photoUri: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

data class BoltTransportQuote(
  val tier: TransportTier,
  val isEligible: Boolean,
  val isRecommended: Boolean,
  val distanceKm: Double,
  val weightKg: Double,
  val fareTzs: Double,
  val estimatedMinutes: Int,
  val ineligibleReason: String? = null,
)

data class SellerOfferComparison(
  val product: Product,
  val rank: Int,
  val effectiveDistanceKm: Double,
  val unitPriceTzs: Double,
  val transportFeeTzs: Double,
  val landedTotalTzs: Double,
  val isBestLandedValue: Boolean,
  val isLowestStickerPrice: Boolean,
  val savingsVsHighestTzs: Double,
)

data class ProductComparisonGroup(
  val groupKey: String,
  val title: String,
  val category: String,
  val sellerCount: Int,
  val lowestStickerTzs: Double,
  val highestStickerTzs: Double,
  val priceSpreadTzs: Double,
  val bestLandedOffer: SellerOfferComparison,
  val offers: List<SellerOfferComparison>,
)

data class CategoryPriceBenchmark(
  val count: Int,
  val lowestTzs: Double,
  val averageTzs: Double,
  val highestTzs: Double,
)
