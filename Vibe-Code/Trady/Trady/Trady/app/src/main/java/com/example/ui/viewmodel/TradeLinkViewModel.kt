package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CartItem
import com.example.data.model.DriverContract
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.RideHailPartner
import com.example.data.model.TransportTier
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.model.VehicleStatus
import com.example.data.repository.TradeLinkRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

class TradeLinkViewModel(
  private val repository: TradeLinkRepository,
  val currentUserId: String,
  private val defaultDisplayName: String,
) : ViewModel() {

  private fun <T> kotlinx.coroutines.flow.Flow<T>.toUiStateFlow(tag: String): StateFlow<UiState<T>> =
    map<T, UiState<T>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val root = firestoreError ?: error
        Log.w(TAG, "Stream error [$tag]", root)
        emit(UiState.Error(root.message ?: "Unable to sync $tag"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UiState.Loading,
      )

  val userProfileState: StateFlow<UiState<UserProfile?>> =
    repository.observeUserProfile(currentUserId).toUiStateFlow("user_profile")

  val activeProductsState: StateFlow<UiState<List<Product>>> =
    repository.observeActiveProducts().toUiStateFlow("products")

  val providerProductsState: StateFlow<UiState<List<Product>>> =
    repository.observeProviderProducts(currentUserId).toUiStateFlow("provider_products")

  val ordersState: StateFlow<UiState<List<Order>>> =
    repository.observeUserOrders(currentUserId).toUiStateFlow("orders")

  val driverContractState: StateFlow<UiState<DriverContract?>> =
    repository.observeDriverContract(currentUserId).toUiStateFlow("driver_contract")

  private val firestoreFavoriteIds: StateFlow<Set<String>> =
    repository.observeFavoriteProductIds(currentUserId)
      .catch { error ->
        if (error is CancellationException) throw error
        Log.w(TAG, "Stream error [favorites]", error)
        emit(emptySet())
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptySet(),
      )

  private val _favoriteOverrides = MutableStateFlow<Map<String, Boolean>>(emptyMap())

  val favoriteProductIdsState: StateFlow<Set<String>> =
    combine(firestoreFavoriteIds, _favoriteOverrides) { synced, overrides ->
      val combined = synced.toMutableSet()
      overrides.forEach { (productId, isFav) ->
        if (isFav) combined.add(productId) else combined.remove(productId)
      }
      combined.toSet()
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = emptySet(),
    )

  val cartItemsState: StateFlow<List<CartItem>> =
    repository.observeCartItems(currentUserId)
      .catch { error ->
        if (error is CancellationException) throw error
        Log.w(TAG, "Stream error [cart]", error)
        emit(emptyList())
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList(),
      )

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _carouselIndex = MutableStateFlow(1) // Default focused card: ELECTRONICS
  val carouselIndex: StateFlow<Int> = _carouselIndex.asStateFlow()

  private val _actionBanner = MutableStateFlow<String?>(null)
  val actionBanner: StateFlow<String?> = _actionBanner.asStateFlow()

  private val _isBusy = MutableStateFlow(false)
  val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

  fun updateSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun setCarouselIndex(index: Int) {
    val size = ProductCategory.entries.size
    _carouselIndex.value = ((index % size) + size) % size
  }

  fun clearBanner() {
    _actionBanner.value = null
  }

  fun acceptTermsAndSaveProfile(
    fullName: String,
    phoneNumber: String,
    role: UserRole,
    stationName: String,
    vehiclePlate: String,
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      val resolvedName = fullName.trim().ifEmpty { defaultDisplayName.ifEmpty { "Trade Link Member" } }
      val result = repository.saveUserProfile(
        UserProfile(
          userId = currentUserId,
          fullName = resolvedName,
          phoneNumber = phoneNumber,
          activeRole = role.key,
          termsAccepted = true,
          stationName = stationName.ifBlank { "Kariakoo Market Block A" },
          vehiclePlate = vehiclePlate.ifBlank { "MC 482 DTR" },
        )
      )
      if (result.isSuccess && role == UserRole.DRIVER) {
        repository.ensureDriverContract(resolvedName, vehiclePlate, stationName)
      }
      _isBusy.value = false
      if (result.isFailure) {
        _actionBanner.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to save profile."
      }
    }
  }

  fun switchPortalRole(role: UserRole) {
    val current = (userProfileState.value as? UiState.Success)?.data ?: return
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.saveUserProfile(current.copy(activeRole = role.key))
      if (res.isSuccess && role == UserRole.DRIVER) {
        repository.ensureDriverContract(current.fullName, current.vehiclePlate, current.stationName)
      }
      _isBusy.value = false
      if (res.isSuccess) {
        _actionBanner.value = "Switched to ${role.title} Portal"
      } else {
        _actionBanner.value = res.exceptionOrNull()?.localizedMessage ?: "Role switch failed"
      }
    }
  }

  fun createProduct(
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
    onCreated: () -> Unit,
  ) {
    val profile = (userProfileState.value as? UiState.Success)?.data
    val sellerName = profile?.fullName?.ifBlank { defaultDisplayName } ?: defaultDisplayName.ifBlank { "Urban Seller" }
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.createProduct(
        providerName = sellerName,
        category = category,
        title = title,
        description = description,
        priceTzs = priceTzs,
        quantityAvailable = quantityAvailable,
        sizeSpec = sizeSpec,
        weightKg = weightKg,
        isDailyDeal = isDailyDeal,
        pickupLabel = pickupLabel,
        photoUri = photoUri,
      )
      _isBusy.value = false
      if (res.isSuccess) {
        val vehicleMsg = if (weightKg > TradeLinkRepository.MOTORCYCLE_MAX_WEIGHT_KG) {
          "Published (Auto-flagged: Phase 2 Truck Required)"
        } else {
          "Published for Motorcycle Box Delivery"
        }
        _actionBanner.value = vehicleMsg
        onCreated()
      } else {
        _actionBanner.value = res.exceptionOrNull()?.localizedMessage ?: "Could not publish product."
      }
    }
  }

  fun seedSampleCatalog() {
    val profile = (userProfileState.value as? UiState.Success)?.data
    val sellerName = profile?.fullName?.ifBlank { defaultDisplayName } ?: "Kariakoo Official Merchant"
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.seedSampleCatalog(sellerName)
      _isBusy.value = false
      _actionBanner.value = if (res.isSuccess) {
        "Loaded ${res.getOrDefault(0)} Tanzanian marketplace items across 8 categories."
      } else {
        res.exceptionOrNull()?.localizedMessage ?: "Failed to load sample catalog."
      }
    }
  }

  fun toggleFavorite(product: Product) {
    val targetFavorite = !favoriteProductIdsState.value.contains(product.id)
    _favoriteOverrides.value = _favoriteOverrides.value + (product.id to targetFavorite)
    viewModelScope.launch {
      val res = repository.setProductFavorite(product.id, targetFavorite)
      _favoriteOverrides.value = _favoriteOverrides.value - product.id
      if (res.isSuccess) {
        _actionBanner.value = if (targetFavorite) {
          "Saved \"${product.title}\" to favorites"
        } else {
          "Removed \"${product.title}\" from favorites"
        }
      } else {
        _actionBanner.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to update favorites."
      }
    }
  }

  fun addToCart(product: Product, quantityToAdd: Int = 1) {
    viewModelScope.launch {
      val res = repository.addToCart(product, quantityToAdd)
      if (res.isSuccess) {
        val qty = res.getOrDefault(1L)
        _actionBanner.value = "Added \"${product.title}\" to cart ($qty in cart)"
      } else {
        _actionBanner.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to add to cart."
      }
    }
  }

  fun updateCartQuantity(productId: String, newQuantity: Int) {
    viewModelScope.launch {
      val res = repository.updateCartItemQuantity(productId, newQuantity)
      if (res.isFailure) {
        _actionBanner.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to update cart quantity."
      }
    }
  }

  fun removeFromCart(productId: String) {
    viewModelScope.launch {
      val res = repository.removeFromCart(productId)
      if (res.isSuccess) {
        _actionBanner.value = "Removed item from cart"
      } else {
        _actionBanner.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to remove item."
      }
    }
  }

  fun clearCart() {
    viewModelScope.launch {
      val res = repository.clearCart()
      if (res.isSuccess) {
        _actionBanner.value = "Shopping cart cleared"
      } else {
        _actionBanner.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to clear cart."
      }
    }
  }

  fun checkoutCart(
    distanceKm: Double,
    paymentMethod: PaymentMethod,
    onSuccess: (List<Order>) -> Unit,
  ) {
    val items = cartItemsState.value
    if (items.isEmpty()) return
    val profile = (userProfileState.value as? UiState.Success)?.data
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.checkoutCart(
        items = items,
        distanceKm = distanceKm,
        paymentMethod = paymentMethod,
        driverName = "Juma Hassan",
        driverPlate = profile?.vehiclePlate?.ifBlank { "MC 482 DTR" } ?: "MC 482 DTR",
      )
      _isBusy.value = false
      res.onSuccess { orders ->
        val firstCode = orders.firstOrNull()?.secretSafetyCode.orEmpty()
        _actionBanner.value = if (orders.size == 1) {
          "Order placed from cart! Safety Code: $firstCode"
        } else {
          "Placed ${orders.size} orders from cart! Lead Safety Code: $firstCode"
        }
        onSuccess(orders)
      }.onFailure { err ->
        _actionBanner.value = err.localizedMessage ?: "Cart checkout failed."
      }
    }
  }

  fun placeOrder(
    product: Product,
    quantity: Int,
    selectedSize: String,
    distanceKm: Double,
    paymentMethod: PaymentMethod,
    onSuccess: (Order) -> Unit,
  ) {
    val profile = (userProfileState.value as? UiState.Success)?.data
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.placeOrder(
        product = product,
        quantity = quantity,
        selectedSize = selectedSize,
        distanceKm = distanceKm,
        paymentMethod = paymentMethod,
        driverName = "Juma Hassan",
        driverPlate = profile?.vehiclePlate?.ifBlank { "MC 482 DTR" } ?: "MC 482 DTR",
      )
      _isBusy.value = false
      res.onSuccess { order ->
        _actionBanner.value = "Order placed! Safety Code: ${order.secretSafetyCode}"
        onSuccess(order)
      }.onFailure { err ->
        _actionBanner.value = err.localizedMessage ?: "Checkout failed."
      }
    }
  }

  fun dispatchBoltTransport(
    tier: TransportTier,
    isSellerMode: Boolean,
    pickupHub: String,
    dropoffLabel: String,
    distanceKm: Double,
    weightKg: Double,
    paymentMethod: PaymentMethod,
    onSuccess: (Order) -> Unit = {},
  ) {
    val profile = (userProfileState.value as? UiState.Success)?.data
    val requesterName = profile?.fullName?.ifBlank { defaultDisplayName } ?: defaultDisplayName.ifBlank { "Tradee Member" }
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.dispatchBoltTransport(
        tier = tier,
        isSellerMode = isSellerMode,
        requesterName = requesterName,
        pickupHub = pickupHub,
        dropoffLabel = dropoffLabel,
        distanceKm = distanceKm,
        weightKg = weightKg,
        paymentMethod = paymentMethod,
        driverName = "Juma Hassan",
        driverPlate = profile?.vehiclePlate?.ifBlank { if (tier.requiresTruck) "T 884 DTR" else "MC 482 DTR" }
          ?: if (tier.requiresTruck) "T 884 DTR" else "MC 482 DTR",
      )
      _isBusy.value = false
      res.onSuccess { order ->
        _actionBanner.value = "${tier.displayName} Dispatched! Safety Code: ${order.secretSafetyCode}"
        onSuccess(order)
      }.onFailure { err ->
        _actionBanner.value = err.localizedMessage ?: "Bolt transport dispatch failed."
      }
    }
  }

  fun advanceOrderStep(order: Order) {
    val current = OrderStatus.fromKey(order.status)
    val (nextStatus, nextLat, nextLng) = when (current) {
      OrderStatus.PENDING_DISPATCH -> Triple(OrderStatus.DRIVER_ASSIGNED, -6.8180, 39.2750)
      OrderStatus.DRIVER_ASSIGNED -> Triple(OrderStatus.BOX_ATTACHED_PICKED_UP, -6.8110, 39.2735)
      OrderStatus.BOX_ATTACHED_PICKED_UP -> Triple(OrderStatus.IN_TRANSIT, -6.8030, 39.2710)
      OrderStatus.IN_TRANSIT -> Triple(OrderStatus.ARRIVED_AWAITING_SECRET_CODE, order.destLat, order.destLng)
      else -> return
    }
    viewModelScope.launch {
      val res = repository.advanceOrderStatus(
        order = order,
        nextStatus = nextStatus,
        cargoBoxAttached = true,
        driverLat = nextLat,
        driverLng = nextLng,
      )
      if (res.isSuccess) {
        _actionBanner.value = "Delivery updated: ${nextStatus.label}"
      } else {
        _actionBanner.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to update order."
      }
    }
  }

  fun confirmSafeReceipt(order: Order) {
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.confirmSafeReceipt(order)
      _isBusy.value = false
      _actionBanner.value = if (res.isSuccess) {
        "Receipt ${order.receiptNumber} confirmed! Delivery complete."
      } else {
        res.exceptionOrNull()?.localizedMessage ?: "Failed to confirm receipt."
      }
    }
  }

  fun claimDamagedOrLostRefund(order: Order) {
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.claimDamagedOrLostRefund(order)
      _isBusy.value = false
      _actionBanner.value = if (res.isSuccess) {
        "100% full refund approved for damaged/lost item!"
      } else {
        res.exceptionOrNull()?.localizedMessage ?: "Refund claim failed."
      }
    }
  }

  fun claimRobberyOrHarmRefund(order: Order) {
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.claimRobberyOrHarmRefund(order)
      _isBusy.value = false
      _actionBanner.value = if (res.isSuccess) {
        "Incident logged, driver route frozen, 100% refund issued."
      } else {
        res.exceptionOrNull()?.localizedMessage ?: "Incident report failed."
      }
    }
  }

  // --- FLEET DRIVER ACTIONS ---

  fun ensureDriverInitialized() {
    val profile = (userProfileState.value as? UiState.Success)?.data
    viewModelScope.launch {
      repository.ensureDriverContract(
        driverName = profile?.fullName?.ifBlank { defaultDisplayName } ?: "Juma Hassan",
        vehiclePlate = profile?.vehiclePlate ?: "MC 482 DTR",
        stationName = profile?.stationName ?: "Kariakoo Market Block A",
      )
    }
  }

  fun driverCheckOutMorning(contract: DriverContract) {
    viewModelScope.launch {
      val timeLabel = currentEatTimeLabel()
      val res = repository.updateDriverShift(
        contract = contract,
        vehicleStatus = VehicleStatus.ACTIVE_PACKAGE_DELIVERY,
        cargoBoxAttached = true,
        rideHailPartner = RideHailPartner.NONE,
        curfewViolated = false,
        checkedOutLabel = timeLabel,
      )
      _actionBanner.value = if (res.isSuccess) {
        "Checked out at $timeLabel · Mode A: Package Delivery"
      } else {
        res.exceptionOrNull()?.localizedMessage ?: "Check-out failed."
      }
    }
  }

  fun driverCheckInEvening(contract: DriverContract) {
    viewModelScope.launch {
      val timeLabel = currentEatTimeLabel()
      val res = repository.updateDriverShift(
        contract = contract,
        vehicleStatus = VehicleStatus.STATION_CHECKED_IN,
        cargoBoxAttached = false,
        rideHailPartner = RideHailPartner.NONE,
        curfewViolated = false,
        checkedInLabel = timeLabel,
      )
      _actionBanner.value = if (res.isSuccess) {
        "Vehicle checked in at ${contract.stationName} ($timeLabel). Curfew cleared."
      } else {
        res.exceptionOrNull()?.localizedMessage ?: "Check-in failed."
      }
    }
  }

  fun toggleCargoBox(contract: DriverContract) {
    viewModelScope.launch {
      val nextAttached = !contract.cargoBoxAttached
      val nextStatus = if (nextAttached) {
        VehicleStatus.ACTIVE_PACKAGE_DELIVERY
      } else {
        VehicleStatus.ACTIVE_PASSENGER_IDLE_MODE
      }
      val res = repository.updateDriverShift(
        contract = contract,
        vehicleStatus = nextStatus,
        cargoBoxAttached = nextAttached,
        rideHailPartner = if (nextAttached) RideHailPartner.NONE else RideHailPartner.STREET_HAIL,
      )
      if (res.isSuccess) {
        _actionBanner.value = if (nextAttached) {
          "Rear Cargo Box mounted for Trade Link deliveries."
        } else {
          "Cargo Box detached · Ready for Idle Passenger Mode."
        }
      }
    }
  }

  fun selectRideHailPartner(contract: DriverContract, partner: RideHailPartner) {
    viewModelScope.launch {
      val res = repository.updateDriverShift(
        contract = contract,
        vehicleStatus = if (partner == RideHailPartner.NONE) VehicleStatus.ACTIVE_PACKAGE_DELIVERY else VehicleStatus.ACTIVE_PASSENGER_IDLE_MODE,
        cargoBoxAttached = partner == RideHailPartner.NONE,
        rideHailPartner = partner,
      )
      if (res.isSuccess) {
        _actionBanner.value = if (partner == RideHailPartner.NONE) {
          "Returned to Mode A: Trade Link Package Delivery"
        } else {
          "Mode B Active (${partner.label}) · Background Trade Link GPS ON"
        }
      }
    }
  }

  fun triggerCurfewBreachProtocol(contract: DriverContract) {
    viewModelScope.launch {
      val res = repository.updateDriverShift(
        contract = contract,
        vehicleStatus = VehicleStatus.CURFEW_BREACH_STOLEN_ALERT,
        cargoBoxAttached = contract.cargoBoxAttached,
        rideHailPartner = RideHailPartner.NONE,
        curfewViolated = true,
      )
      if (res.isSuccess) {
        _actionBanner.value = "21:00 CURFEW PROTOCOL: Secret GPS (${contract.secretGpsImei}) polling at 1Hz!"
      }
    }
  }

  fun recordWeeklyLeasePayment(contract: DriverContract, weeks: Int = 1) {
    viewModelScope.launch {
      val res = repository.recordWeeklyRemittance(contract, weeks)
      if (res.isSuccess) {
        val nextCount = (contract.weeksPaidCount + weeks).coerceAtMost(104L)
        _actionBanner.value = if (nextCount >= 104L) {
          "104/104 Weeks Complete! Permanent Motorcycle Ownership Transferred!"
        } else {
          "Recorded +$weeks week(s) · Total: $nextCount / 104 weeks"
        }
      }
    }
  }

  private fun currentEatTimeLabel(): String {
    val fmt = SimpleDateFormat("HH:mm 'EAT'", Locale.US).apply {
      timeZone = TimeZone.getTimeZone("Africa/Dar_es_Salaam")
    }
    return fmt.format(Date())
  }

  private companion object {
    const val TAG = "TradeLinkVM"
  }
}
