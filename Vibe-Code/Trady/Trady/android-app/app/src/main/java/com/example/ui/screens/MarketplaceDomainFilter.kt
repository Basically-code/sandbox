package com.example.ui.screens

import com.example.data.model.Product
import com.example.data.model.ProductCategory

internal enum class MarketplaceSortOption(
  val label: String,
  val optionTestTag: String,
) {
  NEWEST_FIRST("Newest First", "marketplace_sort_option_newest"),
  PRICE_LOW_TO_HIGH("Price: Low to High", "marketplace_sort_option_price_low"),
  PRICE_HIGH_TO_LOW("Price: High to Low", "marketplace_sort_option_price_high"),
}

internal fun sortMarketplaceProducts(
  products: List<Product>,
  sortOption: MarketplaceSortOption,
): List<Product> =
  when (sortOption) {
    MarketplaceSortOption.NEWEST_FIRST -> products.sortedWith(
      compareByDescending<Product> { it.createdAt?.seconds ?: 0L }
        .thenByDescending { it.createdAt?.nanoseconds ?: 0 },
    )
    MarketplaceSortOption.PRICE_LOW_TO_HIGH -> products.sortedWith(
      compareBy<Product> { it.priceTzs }
        .thenByDescending { it.createdAt?.seconds ?: 0L },
    )
    MarketplaceSortOption.PRICE_HIGH_TO_LOW -> products.sortedWith(
      compareByDescending<Product> { it.priceTzs }
        .thenByDescending { it.createdAt?.seconds ?: 0L },
    )
  }

internal fun filterMarketplaceProducts(
  products: List<Product>,
  favoriteProductIds: Set<String>,
  searchQuery: String,
  selectedCategoryKey: String?,
  dealsOnly: Boolean,
  favoritesOnly: Boolean,
  sortOption: MarketplaceSortOption,
): List<Product> {
  val q = searchQuery.trim().lowercase()
  val filtered = products.filter { product ->
    val categoryInfo = ProductCategory.fromKey(product.category)
    val matchesQuery = q.isEmpty() ||
      product.title.lowercase().contains(q) ||
      categoryInfo.displayName.lowercase().contains(q) ||
      categoryInfo.key.lowercase().contains(q) ||
      product.description.lowercase().contains(q) ||
      product.providerName.lowercase().contains(q)
    val matchesCategory = selectedCategoryKey == null || product.category == selectedCategoryKey
    val matchesDeals = !dealsOnly || product.isDailyDeal
    val matchesFavorites = !favoritesOnly || favoriteProductIds.contains(product.id)
    matchesQuery && matchesCategory && matchesDeals && matchesFavorites
  }
  return sortMarketplaceProducts(filtered, sortOption)
}
