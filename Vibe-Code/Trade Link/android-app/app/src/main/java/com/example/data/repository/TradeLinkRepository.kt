package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.BoltTransportQuote
import com.example.data.model.CartItem
import com.example.data.model.CategoryPriceBenchmark
import com.example.data.model.DriverContract
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.ProductComparisonGroup
import com.example.data.model.RideHailPartner
import com.example.data.model.SellerOfferComparison
import com.example.data.model.TransportTier
import com.example.data.model.UserProfile
import com.example.data.model.VehicleStatus
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class TradeLinkRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    FirebaseAuth.getInstance(),
  )

  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("User must sign in with Google before accessing Trade Link.")

  private fun unwrapFirestoreException(error: Throwable): FirebaseFirestoreException? =
    generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()

  // --- USER PROFILE ---

  fun observeUserProfile(userId: String): Flow<UserProfile?> =
    db.collection("users")
      .document(userId)
      .snapshots()
      .map { snap ->
        if (snap.exists()) {
          snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } else {
          null
        }
      }
      .catch { error ->
        val fsError = unwrapFirestoreException(error)
        if (fsError == null && error is CancellationException) throw error
        if (auth.currentUser?.uid == userId) {
          handleFirestoreError(fsError ?: error, OperationType.GET, "users/$userId")
        }
        throw error
      }

  suspend fun saveUserProfile(profile: UserProfile): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    val existingSnap = docRef.get().await()
    val payload = buildMap<String, Any> {
      put("userId", uid)
      put("fullName", profile.fullName.trim().ifEmpty { "Trade Link User" }.take(120))
      if (profile.phoneNumber.isNotBlank()) put("phoneNumber", profile.phoneNumber.trim().take(25))
      put("activeRole", profile.activeRole)
      put("termsAccepted", profile.termsAccepted)
      if (profile.stationName.isNotBlank()) put("stationName", profile.stationName.trim().take(120))
      if (profile.vehiclePlate.isNotBlank()) put("vehiclePlate", profile.vehiclePlate.trim().take(25))
      if (existingSnap.exists()) {
        put("createdAt", existingSnap.getTimestamp("createdAt") ?: FieldValue.serverTimestamp())
      } else {
        put("createdAt", FieldValue.serverTimestamp())
      }
      put("updatedAt", FieldValue.serverTimestamp())
    }
    try {
      docRef.set(payload).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      throw e
    }
  }

  // --- FAVORITE PRODUCTS ---

  fun observeFavoriteProductIds(userId: String): Flow<Set<String>> =
    db.collection("users")
      .document(userId)
      .collection("favorites")
      .snapshots()
      .map { snapshot ->
        snapshot.documents.map { it.id }.toSet()
      }
      .catch { error ->
        val fsError = unwrapFirestoreException(error)
        if (fsError == null && error is CancellationException) throw error
        if (auth.currentUser?.uid == userId) {
          handleFirestoreError(fsError ?: error, OperationType.LIST, "users/$userId/favorites")
        }
        throw error
      }

  suspend fun setProductFavorite(productId: String, isFavorite: Boolean): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users")
      .document(uid)
      .collection("favorites")
      .document(productId)
    try {
      if (isFavorite) {
        val payload = mapOf(
          "productId" to productId,
          "createdAt" to FieldValue.serverTimestamp(),
        )
        docRef.set(payload).await()
      } else {
        docRef.delete().await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, if (isFavorite) OperationType.CREATE else OperationType.DELETE, docRef.path)
      throw e
    }
  }

  // --- PERSISTENT SHOPPING CART (/users/{userId}/cart/{productId}) ---

  fun observeCartItems(userId: String): Flow<List<CartItem>> =
    db.collection("users")
      .document(userId)
      .collection("cart")
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(CartItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
      }
      .catch { error ->
        val fsError = unwrapFirestoreException(error)
        if (fsError == null && error is CancellationException) throw error
        if (auth.currentUser?.uid == userId) {
          handleFirestoreError(fsError ?: error, OperationType.LIST, "users/$userId/cart")
        }
        throw error
      }

  suspend fun addToCart(product: Product, quantityToAdd: Int = 1): Result<Long> = runCatching {
    val uid = requireUserId()
    require(product.id.isNotBlank()) { "Product ID must not be blank" }
    val docRef = db.collection("users")
      .document(uid)
      .collection("cart")
      .document(product.id)
    val maxAllowed = if (product.quantityAvailable > 0L) {
      product.quantityAvailable.coerceAtMost(1000L)
    } else {
      1000L
    }
    try {
      val existingSnap = docRef.get().await()
      if (existingSnap.exists()) {
        val currentQty = existingSnap.getLong("quantity") ?: 0L
        val nextQty = (currentQty + quantityToAdd.coerceAtLeast(1)).coerceIn(1L, maxAllowed)
        val updates = buildMap<String, Any> {
          put("quantity", nextQty)
          put("priceTzs", product.priceTzs)
          put("title", product.title.trim().take(180))
          put("sizeSpec", product.sizeSpec.trim().ifEmpty { "Standard" }.take(80))
          if (!product.photoUri.isNullOrBlank()) put("photoUri", product.photoUri.take(500))
          put("updatedAt", FieldValue.serverTimestamp())
        }
        docRef.update(updates).await()
        nextQty
      } else {
        val nextQty = quantityToAdd.toLong().coerceIn(1L, maxAllowed)
        val payload = buildMap<String, Any> {
          put("productId", product.id)
          put("providerId", product.providerId.ifBlank { uid }.take(128))
          put("providerName", product.providerName.trim().ifEmpty { "Trady Merchant" }.take(120))
          put("category", product.category)
          put("title", product.title.trim().take(180))
          put("priceTzs", product.priceTzs)
          put("quantity", nextQty)
          put("sizeSpec", product.sizeSpec.trim().ifEmpty { "Standard" }.take(80))
          put("weightKg", product.weightKg)
          put("requiresTruck", product.requiresTruck)
          put("pickupLabel", product.pickupLabel.trim().ifEmpty { "Kariakoo Market Block A" }.take(120))
          if (!product.photoUri.isNullOrBlank()) put("photoUri", product.photoUri.take(500))
          put("createdAt", FieldValue.serverTimestamp())
          put("updatedAt", FieldValue.serverTimestamp())
        }
        docRef.set(payload).await()
        nextQty
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      throw e
    }
  }

  suspend fun updateCartItemQuantity(productId: String, newQuantity: Int): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users")
      .document(uid)
      .collection("cart")
      .document(productId)
    try {
      if (newQuantity <= 0) {
        docRef.delete().await()
      } else {
        docRef.update(
          mapOf(
            "quantity" to newQuantity.toLong().coerceIn(1L, 1000L),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(
        e,
        if (newQuantity <= 0) OperationType.DELETE else OperationType.UPDATE,
        docRef.path,
      )
      throw e
    }
  }

  suspend fun removeFromCart(productId: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users")
      .document(uid)
      .collection("cart")
      .document(productId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  suspend fun clearCart(): Result<Unit> = runCatching {
    val uid = requireUserId()
    val cartCollection = db.collection("users").document(uid).collection("cart")
    try {
      val snapshot = cartCollection.get().await()
      if (!snapshot.isEmpty) {
        val batch = db.batch()
        for (doc in snapshot.documents) {
          batch.delete(doc.reference)
        }
        batch.commit().await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, "users/$uid/cart")
      throw e
    }
  }

  suspend fun checkoutCart(
    items: List<CartItem>,
    distanceKm: Double,
    paymentMethod: PaymentMethod,
    driverName: String,
    driverPlate: String,
  ): Result<List<Order>> = runCatching {
    val uid = requireUserId()
    require(items.isNotEmpty()) { "Cart is empty" }
    val createdOrders = mutableListOf<Order>()
    for ((index, item) in items.withIndex()) {
      val product = Product(
        id = item.productId,
        providerId = item.providerId,
        providerName = item.providerName,
        category = item.category,
        title = item.title,
        priceTzs = item.priceTzs,
        quantityAvailable = item.quantity,
        sizeSpec = item.sizeSpec,
        weightKg = item.weightKg,
        requiresTruck = item.requiresTruck,
        pickupLabel = item.pickupLabel,
        photoUri = item.photoUri,
      )
      // Charge base/distance transport fee on first order or split cleanly
      val order = placeOrder(
        product = product,
        quantity = item.quantity.toInt().coerceAtLeast(1),
        selectedSize = item.sizeSpec,
        distanceKm = if (index == 0) distanceKm else 1.0,
        paymentMethod = paymentMethod,
        driverName = driverName,
        driverPlate = driverPlate,
      ).getOrThrow()
      createdOrders.add(order)
    }
    clearCart().getOrThrow()
    createdOrders
  }

  // --- MARKETPLACE PRODUCTS ---

  fun observeActiveProducts(): Flow<List<Product>> =
    db.collection("products")
      .whereEqualTo("isActive", true)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(Product::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
      }
      .catch { error ->
        val fsError = unwrapFirestoreException(error)
        if (fsError == null && error is CancellationException) throw error
        if (auth.currentUser != null) {
          handleFirestoreError(fsError ?: error, OperationType.LIST, "products")
        }
        throw error
      }

  fun observeProviderProducts(providerId: String): Flow<List<Product>> =
    db.collection("products")
      .whereEqualTo("providerId", providerId)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(Product::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
      }
      .catch { error ->
        val fsError = unwrapFirestoreException(error)
        if (fsError == null && error is CancellationException) throw error
        if (auth.currentUser?.uid == providerId) {
          handleFirestoreError(fsError ?: error, OperationType.LIST, "products")
        }
        throw error
      }

  suspend fun createProduct(
    providerName: String,
    category: ProductCategory,
    title: String,
    description: String,
    priceTzs: Double,
    quantityAvailable: Long,
    sizeSpec: String,
    weightKg: Double,
    isDailyDeal: Boolean,
    pickupLabel: String,
    photoUri: String?,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("products").document()
    val requiresTruck = weightKg > MOTORCYCLE_MAX_WEIGHT_KG
    val payload = buildMap<String, Any> {
      put("providerId", uid)
      put("providerName", providerName.trim().ifEmpty { "Urban Seller" }.take(120))
      put("category", category.key)
      put("title", title.trim().take(180))
      if (description.isNotBlank()) put("description", description.trim().take(1000))
      put("priceTzs", priceTzs)
      put("quantityAvailable", quantityAvailable)
      put("sizeSpec", sizeSpec.trim().ifEmpty { "Standard" }.take(80))
      put("weightKg", weightKg)
      put("requiresTruck", requiresTruck)
      put("isDailyDeal", isDailyDeal)
      put("isActive", true)
      put("pickupLabel", pickupLabel.trim().ifEmpty { "Kariakoo Market Block A" }.take(120))
      if (!photoUri.isNullOrBlank()) put("photoUri", photoUri.take(500))
      put("createdAt", FieldValue.serverTimestamp())
      put("updatedAt", FieldValue.serverTimestamp())
    }
    try {
      docRef.set(payload).await()
      docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun seedSampleCatalog(providerName: String): Result<Int> = runCatching {
    val uid = requireUserId()
    val baseSeller = providerName.ifEmpty { "Kariakoo Official Merchant" }.take(90)
    val samples = listOf(
      SampleSeed(ProductCategory.ELECTRONICS, "Pro Wireless Gamepad DualShock", "Low-latency Bluetooth gamepad with textured grip for mobile and console.", 85_000.0, 14L, "16 x 11 cm", 0.5, true, "Kariakoo Market Block A", "$baseSeller · Block A"),
      SampleSeed(ProductCategory.ELECTRONICS, "Pro Wireless Gamepad DualShock", "Official dual-vibration wireless controller + phone clip mount.", 81_000.0, 9L, "16 x 11 cm + Clip", 0.55, false, "Samora Tech Hub", "Samora Digital Hub"),
      SampleSeed(ProductCategory.ELECTRONICS, "Pro Wireless Gamepad DualShock", "Retail showroom pro controller pack with braided USB-C cable.", 89_000.0, 20L, "16 x 11 cm Pro", 0.5, false, "Mlimani City Retail Plaza", "Mlimani Gaming Store"),
      SampleSeed(ProductCategory.ELECTRONICS, "5G Rugged Smartphone 128GB", "Daylight-bright 6.7\" AMOLED display, 5000mAh battery, dual SIM.", 420_000.0, 7L, "162 x 76 mm", 0.4, true, "Samora Tech Hub", "Samora Digital Hub"),
      SampleSeed(ProductCategory.ELECTRONICS, "5G Rugged Smartphone 128GB", "Direct wholesale import with 33W fast charger and case included.", 408_000.0, 12L, "128GB + Fast Charger", 0.45, true, "Kariakoo Market Block A", "$baseSeller · Block A"),
      SampleSeed(ProductCategory.ELECTRONICS, "55-Inch 4K Smart LED TV", "Requires Phase 2 Commercial Light Truck due to box dimensions.", 980_000.0, 3L, "124 x 75 cm (Truck)", 42.0, false, "Nyerere Rd Warehouse", "Nyerere Appliance Depot"),
      SampleSeed(ProductCategory.SPORTS_AND_OUTDOORS, "Dar Premier Match Football", "FIFA-grade stitched synthetic leather match ball + pump.", 45_000.0, 20L, "Size 5 Official", 0.7, true, "Uhuru Sports Stall 4", "Uhuru Sports Hub"),
      SampleSeed(ProductCategory.FOOD, "Kilombero Aromatic Rice (10 kg)", "Fresh harvest Grade-1 aromatic super rice, sealed woven sack.", 32_000.0, 35L, "10 kg Sack", 10.0, true, "Kariakoo Market Block A", "Kariakoo Harvest Granary"),
      SampleSeed(ProductCategory.FOOD, "Kilombero Aromatic Rice (10 kg)", "Mill-direct wholesale 10 kg sack of pure Kilombero aromatic rice.", 29_500.0, 60L, "10 kg Mill Sack", 10.0, true, "Nyerere Rd Warehouse", "Nyerere Millers Co-op"),
      SampleSeed(ProductCategory.BOOKS, "East Africa Business & Trade Law", "Comprehensive 2026 commercial edition with Tanzania statutes.", 38_000.0, 11L, "Paperback 420p", 0.8, false, "Posta Bookshop Stall 2", "Posta Bookshop"),
      SampleSeed(ProductCategory.CLOTHING, "Kitenge Tailored Linen Shirt", "Breathable cotton-linen collar shirt crafted in Dar es Salaam.", 55_000.0, 16L, "M / L / XL", 0.4, true, "Mwenge Crafts Arcade", "Mwenge Fashion Arcade"),
      SampleSeed(ProductCategory.CLOTHING, "Kitenge Tailored Linen Shirt", "Bespoke Kariakoo textile edition in royal blue and gold print.", 49_000.0, 22L, "M / L / XL", 0.4, false, "Kariakoo Market Block A", "Kariakoo Textile House"),
      SampleSeed(ProductCategory.SHOES, "All-Weather Trail Sneakers", "Slip-resistant sole built for city commutes and rain.", 92_000.0, 9L, "EU 41 - 45", 1.1, false, "Mlimani Footwear Hub", "Mlimani Footwear Hub"),
      SampleSeed(ProductCategory.SHOES, "All-Weather Trail Sneakers", "Direct wholesale import with cushioned insole.", 86_000.0, 15L, "EU 41 - 44", 1.1, true, "Kariakoo Market Block A", "Kariakoo Footwear Depot"),
      SampleSeed(ProductCategory.JEWELLERY, "Zanzibar Silver Signet Ring", "Hand-engraved 925 sterling silver coastal motif ring.", 110_000.0, 6L, "Ring Size 8-10", 0.1, true, "Slipway Artisan Guild", "Slipway Artisan Guild"),
      SampleSeed(ProductCategory.BEAUTY_AND_PERSONAL_CARE, "Cold-Pressed Baobab & Shea Kit", "Natural daytime moisture balm and botanical cleanser.", 28_000.0, 24L, "250 ml + 100 g", 0.6, false, "Kinondoni Botanics", "Kinondoni Botanics"),
    )
    val batch = db.batch()
    for (sample in samples) {
      val docRef = db.collection("products").document()
      val payload = mapOf(
        "providerId" to uid,
        "providerName" to sample.sellerLabel.take(120),
        "category" to sample.category.key,
        "title" to sample.title,
        "description" to sample.description,
        "priceTzs" to sample.priceTzs,
        "quantityAvailable" to sample.quantity,
        "sizeSpec" to sample.sizeSpec,
        "weightKg" to sample.weightKg,
        "requiresTruck" to (sample.weightKg > MOTORCYCLE_MAX_WEIGHT_KG),
        "isDailyDeal" to sample.isDailyDeal,
        "isActive" to true,
        "pickupLabel" to sample.pickupLabel,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      batch.set(docRef, payload)
    }
    try {
      batch.commit().await()
      samples.size
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "products/batch")
      throw e
    }
  }

  // --- ORDERS, VERBAL SECRET SAFETY CODES & REFUND ENGINE ---

  fun observeUserOrders(userId: String): Flow<List<Order>> =
    db.collection("orders")
      .whereArrayContains("participantIds", userId)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(Order::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
      }
      .catch { error ->
        val fsError = unwrapFirestoreException(error)
        if (fsError == null && error is CancellationException) throw error
        if (auth.currentUser?.uid == userId) {
          handleFirestoreError(fsError ?: error, OperationType.LIST, "orders")
        }
        throw error
      }

  suspend fun placeOrder(
    product: Product,
    quantity: Int,
    selectedSize: String,
    distanceKm: Double,
    paymentMethod: PaymentMethod,
    driverName: String,
    driverPlate: String,
  ): Result<Order> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("orders").document()
    val subtotal = product.priceTzs * quantity
    val transportFee = computeTransportFeeTzs(product.requiresTruck, distanceKm)
    val totalAmount = subtotal + transportFee
    val safetyCode = generateSecretSafetyCode()
    val receiptNumber = generateReceiptNumber()
    val participants = listOf(uid, product.providerId).distinct()

    val payload = mapOf(
      "buyerId" to uid,
      "providerId" to product.providerId,
      "driverId" to uid,
      "participantIds" to participants,
      "productId" to product.id,
      "productTitle" to product.title,
      "category" to product.category,
      "selectedSize" to selectedSize.ifBlank { product.sizeSpec },
      "quantity" to quantity.toLong(),
      "unitPriceTzs" to product.priceTzs,
      "itemSubtotalTzs" to subtotal,
      "transportFeeTzs" to transportFee,
      "totalAmountTzs" to totalAmount,
      "paymentMethod" to paymentMethod.key,
      "secretSafetyCode" to safetyCode,
      "receiptNumber" to receiptNumber,
      "driverName" to driverName.ifBlank { "Juma Hassan" },
      "driverPlate" to driverPlate.ifBlank { if (product.requiresTruck) "T 482 DTR" else "MC 482 DTR" },
      "requiresTruck" to product.requiresTruck,
      "cargoBoxAttached" to true,
      "status" to OrderStatus.PENDING_DISPATCH.key,
      "refundAmountTzs" to 0.0,
      "disturbanceCompTzs" to 0.0,
      "driverLat" to -6.8180,
      "driverLng" to 39.2750,
      "destLat" to -6.7920,
      "destLng" to 39.2680,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )

    try {
      docRef.set(payload).await()
      Order(
        id = docRef.id,
        buyerId = uid,
        providerId = product.providerId,
        driverId = uid,
        participantIds = participants,
        productId = product.id,
        productTitle = product.title,
        category = product.category,
        selectedSize = selectedSize.ifBlank { product.sizeSpec },
        quantity = quantity.toLong(),
        unitPriceTzs = product.priceTzs,
        itemSubtotalTzs = subtotal,
        transportFeeTzs = transportFee,
        totalAmountTzs = totalAmount,
        paymentMethod = paymentMethod.key,
        secretSafetyCode = safetyCode,
        receiptNumber = receiptNumber,
        driverName = driverName.ifBlank { "Juma Hassan" },
        driverPlate = driverPlate.ifBlank { if (product.requiresTruck) "T 482 DTR" else "MC 482 DTR" },
        requiresTruck = product.requiresTruck,
        cargoBoxAttached = true,
        status = OrderStatus.PENDING_DISPATCH.key,
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun dispatchBoltTransport(
    tier: TransportTier,
    isSellerMode: Boolean,
    requesterName: String,
    pickupHub: String,
    dropoffLabel: String,
    distanceKm: Double,
    weightKg: Double,
    paymentMethod: PaymentMethod,
    driverName: String,
    driverPlate: String,
  ): Result<Order> = runCatching {
    val uid = requireUserId()
    val quote = computeBoltTierQuote(tier, distanceKm, weightKg)
    val dispatchTitle = buildString {
      append("Bolt ")
      append(tier.displayName)
      append(if (isSellerMode) " · Seller Cargo" else " · Buyer Ride")
      append(" (")
      append(pickupHub.take(36))
      append(")")
    }.take(180)
    val sizeLabel = "${quote.distanceKm.toInt()} km → ${dropoffLabel.ifBlank { "Buyer Gate" }}".take(80)

    // Ensure backing product document exists to satisfy Firestore security rules
    val productDocRef = db.collection("products").document()
    val productPayload = mapOf(
      "providerId" to uid,
      "providerName" to requesterName.trim().ifEmpty { "Tradee Dispatch" }.take(120),
      "category" to ProductCategory.SPORTS_AND_OUTDOORS.key,
      "title" to dispatchTitle,
      "description" to "On-demand Tradee Bolt transport from $pickupHub to $dropoffLabel (${weightKg} kg).",
      "priceTzs" to tier.baseFeeTzs,
      "quantityAvailable" to 1L,
      "sizeSpec" to sizeLabel,
      "weightKg" to weightKg.coerceIn(0.1, 10000.0),
      "requiresTruck" to tier.requiresTruck,
      "isDailyDeal" to false,
      "isActive" to false,
      "pickupLabel" to pickupHub.ifBlank { "Kariakoo Market Block A" }.take(120),
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )

    val orderDocRef = db.collection("orders").document()
    val baseFare = tier.baseFeeTzs
    val distanceFare = (quote.fareTzs - baseFare).coerceAtLeast(0.0)
    val totalFare = quote.fareTzs
    val safetyCode = generateSecretSafetyCode()
    val receiptNumber = generateReceiptNumber()
    val participants = listOf(uid)

    val orderPayload = mapOf(
      "buyerId" to uid,
      "providerId" to uid,
      "driverId" to uid,
      "participantIds" to participants,
      "productId" to productDocRef.id,
      "productTitle" to dispatchTitle,
      "category" to ProductCategory.SPORTS_AND_OUTDOORS.key,
      "selectedSize" to sizeLabel,
      "quantity" to 1L,
      "unitPriceTzs" to baseFare,
      "itemSubtotalTzs" to baseFare,
      "transportFeeTzs" to distanceFare,
      "totalAmountTzs" to totalFare,
      "paymentMethod" to paymentMethod.key,
      "secretSafetyCode" to safetyCode,
      "receiptNumber" to receiptNumber,
      "driverName" to driverName.ifBlank { "Juma Hassan" },
      "driverPlate" to driverPlate.ifBlank { if (tier.requiresTruck) "T 884 DTR" else "MC 482 DTR" },
      "requiresTruck" to tier.requiresTruck,
      "cargoBoxAttached" to (tier != TransportTier.TRADEE_BODA),
      "status" to OrderStatus.PENDING_DISPATCH.key,
      "refundAmountTzs" to 0.0,
      "disturbanceCompTzs" to 0.0,
      "driverLat" to -6.8180,
      "driverLng" to 39.2750,
      "destLat" to -6.7920,
      "destLng" to 39.2680,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )

    try {
      productDocRef.set(productPayload).await()
      orderDocRef.set(orderPayload).await()
      Order(
        id = orderDocRef.id,
        buyerId = uid,
        providerId = uid,
        driverId = uid,
        participantIds = participants,
        productId = productDocRef.id,
        productTitle = dispatchTitle,
        category = ProductCategory.SPORTS_AND_OUTDOORS.key,
        selectedSize = sizeLabel,
        quantity = 1L,
        unitPriceTzs = baseFare,
        itemSubtotalTzs = baseFare,
        transportFeeTzs = distanceFare,
        totalAmountTzs = totalFare,
        paymentMethod = paymentMethod.key,
        secretSafetyCode = safetyCode,
        receiptNumber = receiptNumber,
        driverName = driverName.ifBlank { "Juma Hassan" },
        driverPlate = driverPlate.ifBlank { if (tier.requiresTruck) "T 884 DTR" else "MC 482 DTR" },
        requiresTruck = tier.requiresTruck,
        cargoBoxAttached = (tier != TransportTier.TRADEE_BODA),
        status = OrderStatus.PENDING_DISPATCH.key,
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, orderDocRef.path)
      throw e
    }
  }

  suspend fun advanceOrderStatus(
    order: Order,
    nextStatus: OrderStatus,
    cargoBoxAttached: Boolean,
    driverLat: Double,
    driverLng: Double,
  ): Result<Unit> = runCatching {
    requireUserId()
    val docRef = db.collection("orders").document(order.id)
    val updates = mapOf(
      "status" to nextStatus.key,
      "cargoBoxAttached" to cargoBoxAttached,
      "driverLat" to driverLat,
      "driverLng" to driverLng,
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      docRef.update(updates).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun confirmSafeReceipt(order: Order): Result<Unit> = runCatching {
    requireUserId()
    val docRef = db.collection("orders").document(order.id)
    val updates = mapOf(
      "status" to OrderStatus.DELIVERED_COMPLETED.key,
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      docRef.update(updates).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  // Rule A: Damaged or Lost Goods -> 100% refund
  suspend fun claimDamagedOrLostRefund(order: Order): Result<Unit> = runCatching {
    requireUserId()
    val docRef = db.collection("orders").document(order.id)
    val updates = mapOf(
      "status" to OrderStatus.DAMAGED_OR_LOST_REFUNDED.key,
      "disturbanceCompTzs" to 0.0,
      "refundAmountTzs" to order.totalAmountTzs,
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      docRef.update(updates).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  // Rule B: Customer Harmed / Product Stolen / Cash Robbed -> 100% Refund
  suspend fun claimRobberyOrHarmRefund(order: Order): Result<Unit> = runCatching {
    requireUserId()
    val docRef = db.collection("orders").document(order.id)
    val updates = mapOf(
      "status" to OrderStatus.ROBBERY_OR_HARM_REFUNDED.key,
      "disturbanceCompTzs" to 0.0,
      "refundAmountTzs" to order.totalAmountTzs,
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      docRef.update(updates).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  // --- FLEET DRIVER OPERATIONS & 2-YEAR LEASE-TO-OWN LEDGER ---

  fun observeDriverContract(driverId: String): Flow<DriverContract?> =
    db.collection("driver_contracts")
      .document(driverId)
      .snapshots()
      .map { snap ->
        if (snap.exists()) {
          snap.toObject(DriverContract::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } else {
          null
        }
      }
      .catch { error ->
        val fsError = unwrapFirestoreException(error)
        if (fsError == null && error is CancellationException) throw error
        if (auth.currentUser?.uid == driverId) {
          handleFirestoreError(fsError ?: error, OperationType.GET, "driver_contracts/$driverId")
        }
        throw error
      }

  suspend fun ensureDriverContract(
    driverName: String,
    vehiclePlate: String,
    stationName: String,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("driver_contracts").document(uid)
    val snap = docRef.get().await()
    if (snap.exists()) return@runCatching
    val payload = mapOf(
      "driverId" to uid,
      "driverName" to driverName.ifBlank { "Juma Hassan" }.take(120),
      "vehiclePlate" to vehiclePlate.ifBlank { "MC 482 DTR" }.take(25),
      "secretGpsImei" to "IMEI-8642091100482",
      "stationName" to stationName.ifBlank { "Kariakoo Market Block A" }.take(120),
      "vehicleStatus" to VehicleStatus.STATION_CHECKED_IN.key,
      "cargoBoxAttached" to false,
      "rideHailPartner" to RideHailPartner.NONE.key,
      "weeklyFeeTzs" to WEEKLY_LEASE_FEE_TZS,
      "totalWeeksRequired" to TOTAL_LEASE_WEEKS,
      "weeksPaidCount" to 0L,
      "totalPaidTzs" to 0.0,
      "ownershipTransferred" to false,
      "curfewViolated" to false,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      docRef.set(payload).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun updateDriverShift(
    contract: DriverContract,
    vehicleStatus: VehicleStatus,
    cargoBoxAttached: Boolean,
    rideHailPartner: RideHailPartner,
    curfewViolated: Boolean = contract.curfewViolated,
    checkedOutLabel: String? = contract.lastCheckedOutLabel,
    checkedInLabel: String? = contract.lastCheckedInLabel,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("driver_contracts").document(uid)
    val updates = buildMap<String, Any> {
      put("vehicleStatus", vehicleStatus.key)
      put("cargoBoxAttached", cargoBoxAttached)
      put("rideHailPartner", rideHailPartner.key)
      put("curfewViolated", curfewViolated)
      if (!checkedOutLabel.isNullOrBlank()) put("lastCheckedOutLabel", checkedOutLabel.take(80))
      if (!checkedInLabel.isNullOrBlank()) put("lastCheckedInLabel", checkedInLabel.take(80))
      put("updatedAt", FieldValue.serverTimestamp())
    }
    try {
      docRef.update(updates).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun recordWeeklyRemittance(contract: DriverContract, weeksToAdd: Int = 1): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("driver_contracts").document(uid)
    val nextWeeks = (contract.weeksPaidCount + weeksToAdd).coerceAtMost(TOTAL_LEASE_WEEKS)
    val nextTotalPaid = nextWeeks * WEEKLY_LEASE_FEE_TZS.toDouble()
    val transferred = nextWeeks >= TOTAL_LEASE_WEEKS
    val updates = mapOf(
      "weeksPaidCount" to nextWeeks,
      "totalPaidTzs" to nextTotalPaid,
      "ownershipTransferred" to transferred,
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      docRef.update(updates).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  data class CartPriceSummary(
    val totalQuantity: Int,
    val itemsSubtotalTzs: Double,
    val transportFeeTzs: Double,
    val grandTotalTzs: Double,
  )

  companion object {
    const val MOTORCYCLE_MAX_WEIGHT_KG = 35.0
    const val BASE_MOTORCYCLE_FEE_TZS = 2_500.0
    const val BASE_TRUCK_FEE_TZS = 8_000.0
    const val PER_KM_FEE_TZS = 500.0
    const val WEEKLY_LEASE_FEE_TZS = 70_000L
    const val TOTAL_LEASE_WEEKS = 104L

    private val random = SecureRandom()
    private val phoneticWords = listOf("SIMBA", "KILIMA", "ZANZIBAR", "SERENGETI", "DAR", "NYOTA", "AMANI")

    fun computeTransportFeeTzs(requiresTruck: Boolean, distanceKm: Double): Double {
      val base = if (requiresTruck) BASE_TRUCK_FEE_TZS else BASE_MOTORCYCLE_FEE_TZS
      return base + (distanceKm.coerceAtLeast(1.0) * PER_KM_FEE_TZS)
    }

    fun calculateCartTotals(items: List<CartItem>, distanceKm: Double): CartPriceSummary {
      if (items.isEmpty()) {
        return CartPriceSummary(
          totalQuantity = 0,
          itemsSubtotalTzs = 0.0,
          transportFeeTzs = 0.0,
          grandTotalTzs = 0.0,
        )
      }
      val totalQty = items.sumOf { it.quantity }.toInt()
      val itemsSubtotal = items.sumOf { it.priceTzs * it.quantity }
      val transportFee = items.mapIndexed { index, item ->
        computeTransportFeeTzs(
          requiresTruck = item.requiresTruck,
          distanceKm = if (index == 0) distanceKm else 1.0,
        )
      }.sum()
      return CartPriceSummary(
        totalQuantity = totalQty,
        itemsSubtotalTzs = itemsSubtotal,
        transportFeeTzs = transportFee,
        grandTotalTzs = itemsSubtotal + transportFee,
      )
    }

    fun generateSecretSafetyCode(): String {
      val digits = 1000 + random.nextInt(9000)
      val word = phoneticWords[random.nextInt(phoneticWords.size)]
      val checkDigit = 1 + random.nextInt(9)
      return "$digits - $word-$checkDigit"
    }

    fun generateReceiptNumber(): String {
      val fmt = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("Africa/Dar_es_Salaam")
      }
      val suffix = 1000 + random.nextInt(9000)
      return "TL-${fmt.format(Date())}-$suffix"
    }

    fun normalizeComparisonKey(title: String): String =
      title.trim().lowercase(Locale.US).replace(Regex("[^a-z0-9]+"), "_").trim('_')

    fun estimateHubDistanceKm(pickupLabel: String, buyerBaseDistanceKm: Double): Double {
      val base = buyerBaseDistanceKm.coerceAtLeast(1.0)
      val lower = pickupLabel.lowercase(Locale.US)
      val hubDelta = when {
        lower.contains("kariakoo") -> 0.0
        lower.contains("samora") || lower.contains("posta") -> 0.8
        lower.contains("mwenge") || lower.contains("kinondoni") -> 2.2
        lower.contains("mlimani") -> 3.4
        lower.contains("slipway") -> 4.0
        lower.contains("nyerere") || lower.contains("warehouse") -> 5.0
        else -> 0.5
      }
      return (base + hubDelta).coerceAtLeast(1.0)
    }

    fun compareProductOffers(
      offers: List<Product>,
      baseDistanceKm: Double = 4.0,
    ): List<SellerOfferComparison> {
      val lead = offers.firstOrNull() ?: return emptyList()
      return compareProductOffers(
        allProducts = offers,
        targetProduct = lead,
        buyerDistanceKm = baseDistanceKm,
      )
    }

    fun compareProductOffers(
      allProducts: List<Product>,
      targetProduct: Product,
      buyerDistanceKm: Double = 4.0,
    ): List<SellerOfferComparison> {
      if (allProducts.isEmpty()) return emptyList()
      val targetKey = normalizeComparisonKey(targetProduct.title)
      val matching = allProducts.filter {
        it.isActive && normalizeComparisonKey(it.title) == targetKey && targetKey.isNotBlank()
      }
      val pool = if (matching.isNotEmpty()) matching else listOf(targetProduct)

      val rawOffers = pool.map { offer ->
        val effKm = estimateHubDistanceKm(offer.pickupLabel, buyerDistanceKm)
        val transportFee = computeTransportFeeTzs(offer.requiresTruck, effKm)
        val landedTotal = offer.priceTzs + transportFee
        Triple(offer, effKm, transportFee to landedTotal)
      }.sortedWith(
        compareBy<Triple<Product, Double, Pair<Double, Double>>> { it.third.second }
          .thenBy { it.first.priceTzs }
      )

      val minSticker = rawOffers.minOfOrNull { it.first.priceTzs } ?: targetProduct.priceTzs
      val minLanded = rawOffers.minOfOrNull { it.third.second } ?: targetProduct.priceTzs
      val maxLanded = rawOffers.maxOfOrNull { it.third.second } ?: targetProduct.priceTzs

      return rawOffers.mapIndexed { idx, (prod, effKm, fees) ->
        val (transportFee, landedTotal) = fees
        SellerOfferComparison(
          product = prod,
          rank = idx + 1,
          effectiveDistanceKm = effKm,
          unitPriceTzs = prod.priceTzs,
          transportFeeTzs = transportFee,
          landedTotalTzs = landedTotal,
          isBestLandedValue = landedTotal <= minLanded + 0.01,
          isLowestStickerPrice = prod.priceTzs <= minSticker + 0.01,
          savingsVsHighestTzs = (maxLanded - landedTotal).coerceAtLeast(0.0),
        )
      }
    }

    fun buildPriceComparisonGroups(
      allProducts: List<Product>,
      buyerDistanceKm: Double = 4.0,
      baseDistanceKm: Double? = null,
    ): List<ProductComparisonGroup> {
      if (allProducts.isEmpty()) return emptyList()
      val resolvedDistanceKm = baseDistanceKm ?: buyerDistanceKm
      val seenKeys = mutableSetOf<String>()
      val groups = mutableListOf<ProductComparisonGroup>()

      for (product in allProducts) {
        if (!product.isActive) continue
        val key = normalizeComparisonKey(product.title)
        if (key.isBlank() || !seenKeys.add(key)) continue

        val offers = compareProductOffers(allProducts, product, resolvedDistanceKm)
        if (offers.isEmpty()) continue
        val minSticker = offers.minOf { it.unitPriceTzs }
        val maxSticker = offers.maxOf { it.unitPriceTzs }

        groups.add(
          ProductComparisonGroup(
            groupKey = key,
            title = product.title,
            category = product.category,
            sellerCount = offers.size,
            lowestStickerTzs = minSticker,
            highestStickerTzs = maxSticker,
            priceSpreadTzs = (maxSticker - minSticker).coerceAtLeast(0.0),
            bestLandedOffer = offers.first(),
            offers = offers,
          )
        )
      }

      return groups.sortedWith(
        compareByDescending<ProductComparisonGroup> { it.sellerCount }
          .thenByDescending { it.priceSpreadTzs }
      )
    }

    fun computeCategoryPriceBenchmark(
      allProducts: List<Product>,
      categoryKey: String,
    ): CategoryPriceBenchmark {
      val matching = allProducts.filter {
        it.isActive && (categoryKey.isBlank() || it.category == categoryKey) && it.priceTzs > 0.0
      }
      if (matching.isEmpty()) {
        return CategoryPriceBenchmark(count = 0, lowestTzs = 0.0, averageTzs = 0.0, highestTzs = 0.0)
      }
      val minPrice = matching.minOf { it.priceTzs }
      val maxPrice = matching.maxOf { it.priceTzs }
      val avgPrice = matching.map { it.priceTzs }.average()
      return CategoryPriceBenchmark(
        count = matching.size,
        lowestTzs = minPrice,
        averageTzs = avgPrice,
        highestTzs = maxPrice,
      )
    }

    fun computeBoltTierQuote(
      tier: TransportTier,
      distanceKm: Double,
      weightKg: Double,
      isRecommended: Boolean = false,
    ): BoltTransportQuote {
      val clampedKm = distanceKm.coerceAtLeast(1.0)
      val clampedWeight = weightKg.coerceAtLeast(0.1)
      val eligible = clampedWeight <= tier.maxWeightKg
      val fare = tier.baseFeeTzs + (clampedKm * tier.perKmRateTzs)
      val eta = (tier.baseEtaMinutes + (clampedKm * 1.6)).toInt()
      return BoltTransportQuote(
        tier = tier,
        isEligible = eligible,
        isRecommended = isRecommended,
        distanceKm = clampedKm,
        weightKg = clampedWeight,
        fareTzs = fare,
        estimatedMinutes = eta,
        ineligibleReason = if (eligible) null else "Exceeds ${tier.maxWeightKg.toInt()} kg limit",
      )
    }

    fun compareAllBoltTiers(
      distanceKm: Double,
      weightKg: Double = 5.0,
      cargoWeightKg: Double? = null,
    ): List<BoltTransportQuote> {
      val resolvedWeight = (cargoWeightKg ?: weightKg).coerceAtLeast(0.1)
      val initial = TransportTier.entries.map { tier ->
        computeBoltTierQuote(tier, distanceKm, resolvedWeight, isRecommended = false)
      }
      val idealTier = when {
        resolvedWeight <= 3.0 -> TransportTier.TRADEE_BODA
        resolvedWeight <= 35.0 -> TransportTier.TRADEE_MOTO_BOX
        resolvedWeight <= 250.0 -> TransportTier.TRADEE_BAJAJI
        else -> TransportTier.TRADEE_TRUCK
      }
      val eligible = initial.filter { it.isEligible }
      val bestEligibleTier = eligible.firstOrNull { it.tier == idealTier }?.tier
        ?: eligible.minByOrNull { it.fareTzs }?.tier
        ?: TransportTier.TRADEE_TRUCK
      return initial.map { quote ->
        quote.copy(isRecommended = quote.tier == bestEligibleTier && quote.isEligible)
      }
    }
  }

  private data class SampleSeed(
    val category: ProductCategory,
    val title: String,
    val description: String,
    val priceTzs: Double,
    val quantity: Long,
    val sizeSpec: String,
    val weightKg: Double,
    val isDailyDeal: Boolean,
    val pickupLabel: String,
    val sellerLabel: String = "Kariakoo Official Merchant",
  )
}
