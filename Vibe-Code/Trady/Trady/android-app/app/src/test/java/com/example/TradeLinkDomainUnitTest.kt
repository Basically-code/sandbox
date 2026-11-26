package com.example

import com.example.data.model.CartItem
import com.example.data.model.OrderStatus
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.TransportTier
import com.example.data.repository.TradeLinkRepository
import com.example.ui.screens.MarketplaceSortOption
import com.example.ui.screens.filterMarketplaceProducts
import com.example.ui.screens.sortMarketplaceProducts
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TradeLinkDomainUnitTest {

  @Test
  fun categories_coverAllEightBlueprintChannels() {
    assertEquals(8, ProductCategory.entries.size)
    assertEquals(ProductCategory.ELECTRONICS, ProductCategory.entries[1])
  }

  @Test
  fun transportFee_appliesMotorcycleVsTruckRates() {
    val motoFee = TradeLinkRepository.computeTransportFeeTzs(requiresTruck = false, distanceKm = 4.0)
    val truckFee = TradeLinkRepository.computeTransportFeeTzs(requiresTruck = true, distanceKm = 4.0)
    assertEquals(4_500.0, motoFee, 0.01)
    assertEquals(10_000.0, truckFee, 0.01)
  }

  @Test
  fun leaseToOwn_twoYearTotalMatches104WeeksAt70k() {
    val total = TradeLinkRepository.WEEKLY_LEASE_FEE_TZS * TradeLinkRepository.TOTAL_LEASE_WEEKS
    assertEquals(7_280_000L, total)
  }

  @Test
  fun orderStatus_terminalStatesLockedProperly() {
    assertFalse(OrderStatus.IN_TRANSIT.isTerminal)
    assertTrue(OrderStatus.DELIVERED_COMPLETED.isTerminal)
    assertTrue(OrderStatus.DAMAGED_OR_LOST_REFUNDED.isTerminal)
    assertTrue(OrderStatus.ROBBERY_OR_HARM_REFUNDED.isTerminal)
  }

  @Test
  fun marketplaceSort_ordersByPriceAndNewestFirst() {
    val olderExpensive = Product(
      id = "p1",
      title = "Smart TV",
      priceTzs = 980_000.0,
      createdAt = Timestamp(1_700_000_000L, 0),
    )
    val newestMid = Product(
      id = "p2",
      title = "Gamepad",
      priceTzs = 85_000.0,
      createdAt = Timestamp(1_700_000_200L, 0),
    )
    val oldestCheap = Product(
      id = "p3",
      title = "USB Cable",
      priceTzs = 12_000.0,
      createdAt = Timestamp(1_699_999_900L, 0),
    )
    val input = listOf(olderExpensive, newestMid, oldestCheap)

    assertEquals(
      listOf("p2", "p1", "p3"),
      sortMarketplaceProducts(input, MarketplaceSortOption.NEWEST_FIRST).map { it.id },
    )
    assertEquals(
      listOf("p3", "p2", "p1"),
      sortMarketplaceProducts(input, MarketplaceSortOption.PRICE_LOW_TO_HIGH).map { it.id },
    )
    assertEquals(
      listOf("p1", "p2", "p3"),
      sortMarketplaceProducts(input, MarketplaceSortOption.PRICE_HIGH_TO_LOW).map { it.id },
    )
  }

  @Test
  fun cartTotals_calculatesSubtotalTransportAndGrandTotal() {
    val emptySummary = TradeLinkRepository.calculateCartTotals(emptyList(), distanceKm = 4.0)
    assertEquals(0, emptySummary.totalQuantity)
    assertEquals(0.0, emptySummary.grandTotalTzs, 0.01)

    val items = listOf(
      CartItem(
        productId = "p1",
        title = "Gamepad",
        priceTzs = 85_000.0,
        quantity = 2L,
        requiresTruck = false,
      ),
      CartItem(
        productId = "p2",
        title = "Leather Sneakers",
        priceTzs = 92_000.0,
        quantity = 1L,
        requiresTruck = false,
      ),
    )

    val summary = TradeLinkRepository.calculateCartTotals(items, distanceKm = 4.0)
    assertEquals(3, summary.totalQuantity)
    assertEquals(262_000.0, summary.itemsSubtotalTzs, 0.01)
    // First item at 4 km (2500 + 2000 = 4500) + second item at 1 km (2500 + 500 = 3000) = 7500
    assertEquals(7_500.0, summary.transportFeeTzs, 0.01)
    assertEquals(269_500.0, summary.grandTotalTzs, 0.01)
  }

  @Test
  fun marketplaceCategoryChipFilter_filtersQuicklyByCategory() {
    val items = listOf(
      Product(id = "e1", category = ProductCategory.ELECTRONICS.key, title = "Smartphone", priceTzs = 420_000.0),
      Product(id = "c1", category = ProductCategory.CLOTHING.key, title = "Linen Shirt", priceTzs = 55_000.0),
      Product(id = "f1", category = ProductCategory.FOOD.key, title = "Aromatic Rice", priceTzs = 32_000.0),
    )

    val electronicsOnly = filterMarketplaceProducts(
      products = items,
      favoriteProductIds = emptySet(),
      searchQuery = "",
      selectedCategoryKey = ProductCategory.ELECTRONICS.key,
      dealsOnly = false,
      favoritesOnly = false,
      sortOption = MarketplaceSortOption.NEWEST_FIRST,
    )
    assertEquals(listOf("e1"), electronicsOnly.map { it.id })

    val allItems = filterMarketplaceProducts(
      products = items,
      favoriteProductIds = emptySet(),
      searchQuery = "",
      selectedCategoryKey = null,
      dealsOnly = false,
      favoritesOnly = false,
      sortOption = MarketplaceSortOption.PRICE_LOW_TO_HIGH,
    )
    assertEquals(listOf("f1", "c1", "e1"), allItems.map { it.id })
  }

  @Test
  fun priceComparison_ranksOffersByLandedTotalAndComputesSavings() {
    assertTrue(TradeLinkRepository.compareProductOffers(emptyList()).isEmpty())

    val offers = listOf(
      Product(
        id = "s1",
        providerName = "Mlimani Gaming Store",
        category = ProductCategory.ELECTRONICS.key,
        title = "Pro Wireless Gamepad DualShock",
        priceTzs = 89_000.0,
        pickupLabel = "Mlimani City Retail Plaza",
      ),
      Product(
        id = "s2",
        providerName = "Samora Digital Hub",
        category = ProductCategory.ELECTRONICS.key,
        title = "Pro Wireless Gamepad DualShock",
        priceTzs = 81_000.0,
        pickupLabel = "Samora Tech Hub",
      ),
      Product(
        id = "s3",
        providerName = "Kariakoo Official Merchant",
        category = ProductCategory.ELECTRONICS.key,
        title = "Pro Wireless Gamepad DualShock",
        priceTzs = 85_000.0,
        pickupLabel = "Kariakoo Market Block A",
      ),
    )

    val ranked = TradeLinkRepository.compareProductOffers(offers, baseDistanceKm = 4.0)
    assertEquals(3, ranked.size)
    assertEquals("s2", ranked.first().product.id)
    assertTrue(ranked.first().isBestLandedValue)
    assertTrue(ranked.first().isLowestStickerPrice)
    assertTrue(ranked.first().savingsVsHighestTzs > 0.0)

    val groups = TradeLinkRepository.buildPriceComparisonGroups(offers, baseDistanceKm = 4.0)
    assertEquals(1, groups.size)
    assertEquals(3, groups.first().sellerCount)
    assertEquals(8_000.0, groups.first().priceSpreadTzs, 0.01)

    // Verify category price benchmark calculation
    val benchmark = TradeLinkRepository.computeCategoryPriceBenchmark(offers, ProductCategory.ELECTRONICS.key)
    assertEquals(3, benchmark.count)
    assertEquals(81_000.0, benchmark.lowestTzs, 0.01)
    assertEquals(85_000.0, benchmark.averageTzs, 0.01)
    assertEquals(89_000.0, benchmark.highestTzs, 0.01)
  }

  @Test
  fun boltTransportTiers_enforcesWeightLimitsAndClampsDistance() {
    val expressBodaQuotes = TradeLinkRepository.compareAllBoltTiers(distanceKm = 2.0, cargoWeightKg = 1.5)
    assertTrue(expressBodaQuotes.first { it.tier == TransportTier.TRADEE_BODA }.isRecommended)

    val lightQuotes = TradeLinkRepository.compareAllBoltTiers(distanceKm = 0.0, cargoWeightKg = 5.0)
    assertEquals(4, lightQuotes.size)
    // Distance 0.0 clamps to 1.0 km -> Moto Box = 2500 + 500 = 3000
    val motoQuote = lightQuotes.first { it.tier == TransportTier.TRADEE_MOTO_BOX }
    assertTrue(motoQuote.isEligible)
    assertTrue(motoQuote.isRecommended)
    assertEquals(3_000.0, motoQuote.fareTzs, 0.01)

    // Heavy cargo (50 kg) disqualifies Boda (15kg) and Moto Box (35kg), recommends Bajaji (150kg)
    val mediumHeavyQuotes = TradeLinkRepository.compareAllBoltTiers(distanceKm = 4.0, cargoWeightKg = 50.0)
    assertFalse(mediumHeavyQuotes.first { it.tier == TransportTier.TRADEE_BODA }.isEligible)
    assertFalse(mediumHeavyQuotes.first { it.tier == TransportTier.TRADEE_MOTO_BOX }.isEligible)
    assertTrue(mediumHeavyQuotes.first { it.tier == TransportTier.TRADEE_BAJAJI }.isRecommended)

    // Bulk cargo (400 kg) disqualifies Boda, Moto Box, and Bajaji, recommends Commercial Truck
    val bulkTruckQuotes = TradeLinkRepository.compareAllBoltTiers(distanceKm = 4.0, cargoWeightKg = 400.0)
    val truckQuote = bulkTruckQuotes.first { it.tier == TransportTier.TRADEE_TRUCK }
    assertTrue(truckQuote.isEligible)
    assertTrue(truckQuote.isRecommended)
    assertEquals(10_000.0, truckQuote.fareTzs, 0.01)
  }
}
