package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.ProductCategory
import com.example.data.model.RideHailPartner
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.model.VehicleStatus
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TradeLinkRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun userProfile_saveAndObserve_succeedsForOwner() = runBlocking {
    val uid = signInTestUser(BUYER_EMAIL)
    val repo = TradeLinkRepository(firestore, auth)

    val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.saveUserProfile(
        UserProfile(
          userId = uid,
          fullName = "Amina Juma",
          phoneNumber = "+255712000111",
          activeRole = UserRole.BUYER.key,
          termsAccepted = true,
        )
      )
    }
    assertTrue(saveResult.isSuccess)

    val loaded = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeUserProfile(uid).first { it != null }
    }
    assertNotNull(loaded)
    assertEquals("Amina Juma", loaded?.fullName)
    assertTrue(loaded?.termsAccepted == true)
  }

  @Test
  fun productAndOrderLifecycle_buyerAndSeller_enforcesRefundAndTerminalRules() = runBlocking {
    signInTestUser(SELLER_EMAIL)
    val sellerRepo = TradeLinkRepository(firestore, auth)

    val productId = withTimeout(DEFAULT_TIMEOUT_MS) {
      sellerRepo.createProduct(
        providerName = "Kariakoo Tech Hub",
        category = ProductCategory.ELECTRONICS,
        title = "Wireless Controller",
        description = "Bluetooth dual-shock controller",
        priceTzs = 85_000.0,
        quantityAvailable = 8L,
        sizeSpec = "Standard Box",
        weightKg = 0.6,
        isDailyDeal = true,
        pickupLabel = "Kariakoo Market Block A",
        photoUri = null,
      ).getOrThrow()
    }

    val buyerUid = signInTestUser(BUYER_EMAIL)
    val buyerRepo = TradeLinkRepository(firestore, auth)

    val activeProducts = withTimeout(FLOW_TIMEOUT_MS) {
      buyerRepo.observeActiveProducts().first { list -> list.any { it.id == productId } }
    }
    val product = activeProducts.first { it.id == productId }
    assertFalse(product.requiresTruck)

    val order = withTimeout(DEFAULT_TIMEOUT_MS) {
      buyerRepo.placeOrder(
        product = product,
        quantity = 1,
        selectedSize = "Standard Box",
        distanceKm = 4.0,
        paymentMethod = PaymentMethod.CASH_BY_HAND_WITH_RECEIPT,
        driverName = "Juma Hassan",
        driverPlate = "MC 482 DTR",
      ).getOrThrow()
    }
    assertTrue(order.secretSafetyCode.isNotBlank())

    // Advance order to IN_TRANSIT
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        buyerRepo.advanceOrderStatus(order, OrderStatus.IN_TRANSIT, true, -6.805, 39.271).getOrThrow()
      }
    } catch (t: Throwable) {
      throw AssertionError("advanceOrderStatus failed: ${t.message}", t)
    }

    // Claim Rule A Refund (100% refund)
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        buyerRepo.claimDamagedOrLostRefund(order).getOrThrow()
      }
    } catch (t: Throwable) {
      throw AssertionError("claimDamagedOrLostRefund failed (total=${order.totalAmountTzs}): ${t.message}", t)
    }

    val updatedOrder = withTimeout(FLOW_TIMEOUT_MS) {
      buyerRepo.observeUserOrders(buyerUid).first { list ->
        list.any { it.id == order.id && it.status == OrderStatus.DAMAGED_OR_LOST_REFUNDED.key }
      }.first { it.id == order.id }
    }
    assertEquals(order.totalAmountTzs, updatedOrder.refundAmountTzs, 0.01)
    assertEquals(0.0, updatedOrder.disturbanceCompTzs, 0.01)

    // Terminal state lock prevents reverting back to DELIVERED_COMPLETED
    val terminalAttempt = withTimeout(DEFAULT_TIMEOUT_MS) {
      buyerRepo.confirmSafeReceipt(updatedOrder)
    }
    assertTrue(terminalAttempt.isFailure)
    val fsErr = terminalAttempt.exceptionOrNull()?.unwrapFirestoreException()
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsErr?.code)
  }

  @Test
  fun driverContract_leaseToOwnAndOwnershipTransfer_succeeds() = runBlocking {
    val driverUid = signInTestUser(DRIVER_EMAIL)
    val repo = TradeLinkRepository(firestore, auth)

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.ensureDriverContract("Juma Hassan", "MC 482 DTR", "Kariakoo Market Block A").getOrThrow()
    }

    val contract = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeDriverContract(driverUid).first { it != null }!!
    }

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.updateDriverShift(
        contract = contract,
        vehicleStatus = VehicleStatus.ACTIVE_PASSENGER_IDLE_MODE,
        cargoBoxAttached = false,
        rideHailPartner = RideHailPartner.BOLT,
        checkedOutLabel = "06:15 EAT",
      ).getOrThrow()
    }

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.recordWeeklyRemittance(contract, weeksToAdd = 104).getOrThrow()
    }

    val transferred = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeDriverContract(driverUid).first { it?.ownershipTransferred == true }!!
    }
    assertEquals(104L, transferred.weeksPaidCount)
    assertEquals(7_280_000.0, transferred.totalPaidTzs, 0.01)
  }

  @Test
  fun observeProducts_unauthenticated_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = TradeLinkRepository(firestore, auth)
    var caught: Throwable? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeActiveProducts().first()
      }
    } catch (t: Throwable) {
      caught = t
    }
    assertNotNull(caught)
    val fsError = caught?.unwrapFirestoreException()
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsError?.code)
  }

  @Test
  fun favorites_saveObserveAndRemove_succeedsForOwner() = runBlocking {
    val buyerUid = signInTestUser(BUYER_EMAIL)
    val repo = TradeLinkRepository(firestore, auth)

    val addRes = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.setProductFavorite("prod_fav_01", isFavorite = true)
    }
    assertTrue(addRes.isSuccess)

    val savedIds = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeFavoriteProductIds(buyerUid).first { it.contains("prod_fav_01") }
    }
    assertTrue(savedIds.contains("prod_fav_01"))

    val removeRes = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.setProductFavorite("prod_fav_01", isFavorite = false)
    }
    assertTrue(removeRes.isSuccess)

    val clearedIds = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeFavoriteProductIds(buyerUid).first { !it.contains("prod_fav_01") }
    }
    assertFalse(clearedIds.contains("prod_fav_01"))
  }

  @Test
  fun cart_addUpdateObserveAndRemove_succeedsForOwnerAndIsolatesOtherUsers() = runBlocking {
    signInTestUser(SELLER_EMAIL)
    val sellerRepo = TradeLinkRepository(firestore, auth)
    val productId = withTimeout(DEFAULT_TIMEOUT_MS) {
      sellerRepo.createProduct(
        providerName = "Kariakoo Tech Hub",
        category = ProductCategory.ELECTRONICS,
        title = "Cart Test Gamepad",
        description = "Persistent cart verification item",
        priceTzs = 85_000.0,
        quantityAvailable = 10L,
        sizeSpec = "Standard Box",
        weightKg = 0.6,
        isDailyDeal = true,
        pickupLabel = "Kariakoo Market Block A",
        photoUri = null,
      ).getOrThrow()
    }

    val buyerUid = signInTestUser(BUYER_EMAIL)
    val buyerRepo = TradeLinkRepository(firestore, auth)
    val product = withTimeout(FLOW_TIMEOUT_MS) {
      buyerRepo.observeActiveProducts().first { list -> list.any { it.id == productId } }
    }.first { it.id == productId }

    // Add once -> qty 1
    val firstQty = withTimeout(DEFAULT_TIMEOUT_MS) {
      buyerRepo.addToCart(product, quantityToAdd = 1).getOrThrow()
    }
    assertEquals(1L, firstQty)

    // Add again -> increments to qty 3
    val nextQty = withTimeout(DEFAULT_TIMEOUT_MS) {
      buyerRepo.addToCart(product, quantityToAdd = 2).getOrThrow()
    }
    assertEquals(3L, nextQty)

    val cartItems = withTimeout(FLOW_TIMEOUT_MS) {
      buyerRepo.observeCartItems(buyerUid).first { list -> list.any { it.productId == productId && it.quantity == 3L } }
    }
    assertEquals(1, cartItems.count { it.productId == productId })
    assertEquals(85_000.0, cartItems.first { it.productId == productId }.priceTzs, 0.01)

    // Update quantity explicitly
    withTimeout(DEFAULT_TIMEOUT_MS) {
      buyerRepo.updateCartItemQuantity(productId, 5).getOrThrow()
    }
    val updatedItems = withTimeout(FLOW_TIMEOUT_MS) {
      buyerRepo.observeCartItems(buyerUid).first { list -> list.any { it.productId == productId && it.quantity == 5L } }
    }
    assertEquals(5L, updatedItems.first { it.productId == productId }.quantity)

    // Cross-user isolation: another user observing their own cart sees no buyer items, and cannot overwrite buyer's cart
    val intruderUid = signInTestUser(DRIVER_EMAIL)
    val intruderRepo = TradeLinkRepository(firestore, auth)
    val intruderCart = withTimeout(FLOW_TIMEOUT_MS) {
      intruderRepo.observeCartItems(intruderUid).first()
    }
    assertFalse(intruderCart.any { it.productId == productId })

    var crossUserError: Throwable? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        intruderRepo.observeCartItems(buyerUid).first()
      }
    } catch (t: Throwable) {
      crossUserError = t
    }
    assertNotNull(crossUserError)
    assertEquals(
      FirebaseFirestoreException.Code.PERMISSION_DENIED,
      crossUserError?.unwrapFirestoreException()?.code,
    )

    // Owner removes item from cart
    signInTestUser(BUYER_EMAIL)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      buyerRepo.removeFromCart(productId).getOrThrow()
    }
    val emptyCart = withTimeout(FLOW_TIMEOUT_MS) {
      buyerRepo.observeCartItems(buyerUid).first { list -> list.none { it.productId == productId } }
    }
    assertTrue(emptyCart.none { it.productId == productId })
  }

  @Test
  fun checkoutCart_movesCartItemsToOrdersAndClearsCart() = runBlocking {
    signInTestUser(SELLER_EMAIL)
    val sellerRepo = TradeLinkRepository(firestore, auth)
    val productId = withTimeout(DEFAULT_TIMEOUT_MS) {
      sellerRepo.createProduct(
        providerName = "Kariakoo Tech Hub",
        category = ProductCategory.ELECTRONICS,
        title = "Checkout Screen Bundle",
        description = "Portable power station for checkout verification",
        priceTzs = 120_000.0,
        quantityAvailable = 6L,
        sizeSpec = "Compact Box",
        weightKg = 1.8,
        isDailyDeal = false,
        pickupLabel = "Kariakoo Market Block A",
        photoUri = null,
      ).getOrThrow()
    }

    val buyerUid = signInTestUser(BUYER_EMAIL)
    val buyerRepo = TradeLinkRepository(firestore, auth)
    val product = withTimeout(FLOW_TIMEOUT_MS) {
      buyerRepo.observeActiveProducts().first { list -> list.any { it.id == productId } }
    }.first { it.id == productId }

    withTimeout(DEFAULT_TIMEOUT_MS) {
      buyerRepo.addToCart(product, quantityToAdd = 2).getOrThrow()
    }
    val cartSnapshot = withTimeout(FLOW_TIMEOUT_MS) {
      buyerRepo.observeCartItems(buyerUid).first { list -> list.any { it.productId == productId && it.quantity == 2L } }
    }

    val createdOrders = withTimeout(DEFAULT_TIMEOUT_MS) {
      buyerRepo.checkoutCart(
        items = cartSnapshot,
        distanceKm = 4.0,
        paymentMethod = PaymentMethod.CASH_BY_HAND_WITH_RECEIPT,
        driverName = "Juma Hassan",
        driverPlate = "MC 482 DTR",
      ).getOrThrow()
    }
    assertEquals(1, createdOrders.size)
    assertEquals(productId, createdOrders.first().productId)
    assertEquals(2L, createdOrders.first().quantity)
    assertEquals(240_000.0, createdOrders.first().itemSubtotalTzs, 0.01)

    val clearedCart = withTimeout(FLOW_TIMEOUT_MS) {
      buyerRepo.observeCartItems(buyerUid).first { it.isEmpty() }
    }
    assertTrue(clearedCart.isEmpty())

    val syncedOrders = withTimeout(FLOW_TIMEOUT_MS) {
      buyerRepo.observeUserOrders(buyerUid).first { orders -> orders.any { it.id == createdOrders.first().id } }
    }
    assertTrue(syncedOrders.any { it.productId == productId && it.quantity == 2L })
  }

  private companion object {
    const val BUYER_EMAIL = "buyer@tradelink.tz"
    const val SELLER_EMAIL = "seller@tradelink.tz"
    const val DRIVER_EMAIL = "driver@tradelink.tz"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3500L
  }
}
