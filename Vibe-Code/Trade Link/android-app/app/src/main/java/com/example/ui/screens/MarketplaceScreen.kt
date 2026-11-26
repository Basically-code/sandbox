package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.CartItem
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.TransportTier
import com.example.ui.components.FloatingLiveMapButton
import com.example.ui.components.SkewedDealsBanner
import com.example.ui.components.TradeLinkLogoBadge
import com.example.ui.components.formatTzs
import com.example.ui.components.rememberShimmerBrush
import com.example.ui.theme.AccentWarm
import com.example.ui.theme.AccentWarmSoft
import com.example.ui.theme.BgPrimaryCanvas
import com.example.ui.theme.BgSurfaceCard
import com.example.ui.theme.BgSurfaceSubtle
import com.example.ui.theme.BorderHairline
import com.example.ui.theme.BorderInput
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandPrimaryDark
import com.example.ui.theme.BrandSecondary
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusDangerSoft
import com.example.ui.theme.StatusSafeGreenDark
import com.example.ui.theme.StatusSafeGreenSoft
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.UiState

private fun categoryChipIcon(category: ProductCategory) = when (category) {
  ProductCategory.SPORTS_AND_OUTDOORS -> Icons.Filled.FitnessCenter
  ProductCategory.ELECTRONICS -> Icons.Filled.Devices
  ProductCategory.FOOD -> Icons.Filled.Restaurant
  ProductCategory.BOOKS -> Icons.AutoMirrored.Filled.MenuBook
  ProductCategory.CLOTHING -> Icons.Filled.Checkroom
  ProductCategory.SHOES -> Icons.Filled.ShoppingBag
  ProductCategory.JEWELLERY -> Icons.Filled.Diamond
  ProductCategory.BEAUTY_AND_PERSONAL_CARE -> Icons.Filled.Spa
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(
  userName: String,
  searchQuery: String,
  onSearchQueryChange: (String) -> Unit,
  productsState: UiState<List<Product>>,
  favoriteProductIds: Set<String>,
  cartItems: List<CartItem>,
  orders: List<Order>,
  isBusy: Boolean,
  onOpenDrawer: () -> Unit,
  onOpenProfile: () -> Unit,
  onOpenOrders: () -> Unit,
  onOpenCategoryShowcase: () -> Unit,
  onOpenLiveMap: () -> Unit,
  onSeedSampleCatalog: () -> Unit,
  onSwitchToProvider: () -> Unit,
  onProductClicked: (Product) -> Unit,
  onToggleFavorite: (Product) -> Unit,
  onAddToCart: (Product) -> Unit,
  onUpdateCartQuantity: (CartItem, Int) -> Unit,
  onRemoveCartItem: (CartItem) -> Unit,
  onClearCart: () -> Unit,
  onOpenCheckout: () -> Unit,
  onPlaceOrder: (
    product: Product,
    quantity: Int,
    selectedSize: String,
    distanceKm: Double,
    paymentMethod: PaymentMethod,
  ) -> Unit,
  onDispatchBoltTransport: (
    tier: TransportTier,
    isSellerMode: Boolean,
    pickupHub: String,
    dropoffLabel: String,
    distanceKm: Double,
    weightKg: Double,
    paymentMethod: PaymentMethod,
  ) -> Unit = { _, _, _, _, _, _, _ -> },
  modifier: Modifier = Modifier,
) {
  var selectedCategoryKey by rememberSaveable { mutableStateOf<String?>(null) }
  var dealsOnly by rememberSaveable { mutableStateOf(false) }
  var favoritesOnly by rememberSaveable { mutableStateOf(false) }
  var selectedSortOption by rememberSaveable { mutableStateOf(MarketplaceSortOption.NEWEST_FIRST) }
  var sortMenuExpanded by remember { mutableStateOf(false) }
  var showCartSheet by remember { mutableStateOf(false) }
  var checkoutProduct by remember { mutableStateOf<Product?>(null) }
  var showPriceComparisonSheet by remember { mutableStateOf(false) }
  var focusedCompareProduct by remember { mutableStateOf<Product?>(null) }
  var showBoltTransportSheet by remember { mutableStateOf(false) }
  val focusManager = LocalFocusManager.current

  val cartItemCount = cartItems.sumOf { it.quantity }.toInt()
  val activeOrderCount = orders.count { !OrderStatus.fromKey(it.status).isTerminal }
  val isAnyInTransit = orders.any {
    val st = OrderStatus.fromKey(it.status)
    st == OrderStatus.IN_TRANSIT ||
      st == OrderStatus.BOX_ATTACHED_PICKED_UP ||
      st == OrderStatus.DRIVER_ASSIGNED ||
      st == OrderStatus.ARRIVED_AWAITING_SECRET_CODE
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(BgPrimaryCanvas),
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding(),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Top Header
      Surface(
        modifier = Modifier.fillMaxWidth(),
        color = BgSurfaceCard,
      ) {
        Column {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .height(60.dp)
              .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = onOpenDrawer,
                modifier = Modifier.testTag("marketplace_drawer_button"),
              ) {
                Icon(
                  imageVector = Icons.Filled.Menu,
                  contentDescription = "Open Portal Menu",
                  tint = TextPrimary,
                )
              }
              TradeLinkLogoBadge(modifier = Modifier.size(34.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  text = "Marketplace",
                  style = MaterialTheme.typography.titleLarge,
                  color = TextPrimary,
                )
                Text(
                  text = "Live Firestore Catalog",
                  style = MaterialTheme.typography.labelMedium,
                  color = BrandPrimary,
                )
              }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              Surface(
                modifier = Modifier
                  .clip(RoundedCornerShape(999.dp))
                  .clickable(onClick = onOpenProfile)
                  .testTag("marketplace_account_button"),
                shape = RoundedCornerShape(999.dp),
                color = BgSurfaceSubtle,
                border = BorderStroke(1.dp, BorderHairline),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = "Account & Role",
                    tint = BrandPrimary,
                    modifier = Modifier.size(18.dp),
                  )
                  Text(
                    text = userName.substringBefore(" ").ifBlank { "Account" },
                    style = MaterialTheme.typography.labelMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }

              IconButton(
                onClick = { showCartSheet = true },
                modifier = Modifier.testTag("marketplace_cart_button"),
              ) {
                BadgedBox(
                  badge = {
                    if (cartItemCount > 0) {
                      Badge(containerColor = BrandPrimary, contentColor = Color.White) {
                        Text(text = cartItemCount.toString())
                      }
                    }
                  },
                ) {
                  Icon(
                    imageVector = Icons.Filled.ShoppingCart,
                    contentDescription = "Open Shopping Cart",
                    tint = TextPrimary,
                  )
                }
              }

              IconButton(
                onClick = onOpenOrders,
                modifier = Modifier.testTag("marketplace_orders_button"),
              ) {
                BadgedBox(
                  badge = {
                    if (activeOrderCount > 0) {
                      Badge(containerColor = BrandPrimary, contentColor = Color.White) {
                        Text(text = activeOrderCount.toString())
                      }
                    }
                  },
                ) {
                  Icon(
                    imageVector = Icons.Filled.MailOutline,
                    contentDescription = "Orders & Telemetry",
                    tint = TextPrimary,
                  )
                }
              }
            }
          }
          // Pinned Top Search Bar (filters by product name or category)
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            contentAlignment = Alignment.Center,
          ) {
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 620.dp)
                .height(48.dp)
                .testTag("marketplace_search_bar"),
              shape = RoundedCornerShape(999.dp),
              color = BgSurfaceSubtle,
              border = BorderStroke(
                width = 1.dp,
                color = if (searchQuery.isNotEmpty()) BrandPrimary else BorderInput,
              ),
            ) {
              Row(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(start = 14.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = Icons.Filled.Search,
                  contentDescription = "Search products",
                  tint = BrandPrimary,
                  modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                  if (searchQuery.isEmpty()) {
                    Text(
                      text = "Search by product name or category",
                      style = MaterialTheme.typography.bodyLarge,
                      color = TextMuted,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                  BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                    cursorBrush = SolidColor(BrandPrimary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("marketplace_search_input"),
                  )
                }
                if (searchQuery.isNotEmpty()) {
                  IconButton(
                    onClick = { onSearchQueryChange("") },
                    modifier = Modifier.testTag("marketplace_search_clear_button"),
                  ) {
                    Icon(
                      imageVector = Icons.Filled.Close,
                      contentDescription = "Clear Search",
                      tint = TextSecondary,
                      modifier = Modifier.size(18.dp),
                    )
                  }
                }
              }
            }
          }
          HorizontalDivider(color = BorderHairline, thickness = 1.dp)
        }
      }

      // Main Content
      val allProducts = (productsState as? UiState.Success)?.data ?: emptyList()
      val filteredProducts = remember(
        allProducts,
        favoriteProductIds,
        searchQuery,
        selectedCategoryKey,
        dealsOnly,
        favoritesOnly,
        selectedSortOption,
      ) {
        filterMarketplaceProducts(
          products = allProducts,
          favoriteProductIds = favoriteProductIds,
          searchQuery = searchQuery,
          selectedCategoryKey = selectedCategoryKey,
          dealsOnly = dealsOnly,
          favoritesOnly = favoritesOnly,
          sortOption = selectedSortOption,
        )
      }

      // Pinned Horizontal Category Filter Chips Row directly above the Product List
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("marketplace_category_chips_bar"),
        color = BgSurfaceCard,
      ) {
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          LazyRow(
            modifier = Modifier
              .fillMaxWidth()
              .widthIn(max = 620.dp)
              .testTag("marketplace_category_chips_row"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            item(key = "chip_all") {
              val isAllSelected = selectedCategoryKey == null && !dealsOnly && !favoritesOnly
              FilterChip(
                selected = isAllSelected,
                onClick = {
                  selectedCategoryKey = null
                  dealsOnly = false
                  favoritesOnly = false
                },
                label = { Text("All (${allProducts.size})") },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = BrandSecondary,
                  selectedLabelColor = BrandPrimaryDark,
                ),
                border = FilterChipDefaults.filterChipBorder(
                  enabled = true,
                  selected = isAllSelected,
                  selectedBorderColor = BrandPrimary,
                ),
                modifier = Modifier.testTag("marketplace_filter_all"),
              )
            }
            items(ProductCategory.entries, key = { it.key }) { cat ->
              val isCatSelected = selectedCategoryKey == cat.key
              FilterChip(
                selected = isCatSelected,
                onClick = {
                  selectedCategoryKey = if (isCatSelected) null else cat.key
                },
                leadingIcon = {
                  Icon(
                    imageVector = categoryChipIcon(cat),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                  )
                },
                label = { Text(cat.displayName) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = BrandSecondary,
                  selectedLabelColor = BrandPrimaryDark,
                  selectedLeadingIconColor = BrandPrimaryDark,
                ),
                border = FilterChipDefaults.filterChipBorder(
                  enabled = true,
                  selected = isCatSelected,
                  selectedBorderColor = BrandPrimary,
                ),
                modifier = Modifier.testTag("marketplace_filter_${cat.key}"),
              )
            }
            item(key = "chip_deals") {
              FilterChip(
                selected = dealsOnly,
                onClick = { dealsOnly = !dealsOnly },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Filled.LocalOffer,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                  )
                },
                label = { Text("Daily Deals") },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = AccentWarmSoft,
                  selectedLabelColor = TextPrimary,
                ),
                modifier = Modifier.testTag("marketplace_filter_deals"),
              )
            }
            item(key = "chip_favorites") {
              FilterChip(
                selected = favoritesOnly,
                onClick = { favoritesOnly = !favoritesOnly },
                leadingIcon = {
                  Icon(
                    imageVector = if (favoritesOnly) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = StatusDanger,
                    modifier = Modifier.size(16.dp),
                  )
                },
                label = { Text("Favorites (${favoriteProductIds.size})") },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = StatusDangerSoft,
                  selectedLabelColor = TextPrimary,
                ),
                modifier = Modifier.testTag("marketplace_filter_favorites"),
              )
            }
          }
          HorizontalDivider(color = BorderHairline, thickness = 1.dp)
        }
      }

      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 620.dp)
          .weight(1f)
          .testTag("marketplace_products_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {

        // Daily Deals Banner
        item(key = "deals_banner") {
          SkewedDealsBanner(
            onClick = {
              dealsOnly = !dealsOnly
              if (dealsOnly) {
                selectedCategoryKey = null
                favoritesOnly = false
              }
            },
          )
        }

        // Tradee Price Comparison + Bolt Transport Quick Action Bar
        item(key = "tradee_compare_and_bolt_actions") {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            OutlinedButton(
              onClick = {
                focusedCompareProduct = null
                showPriceComparisonSheet = true
              },
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.2.dp, BrandPrimary),
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("marketplace_compare_prices_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.CompareArrows,
                contentDescription = null,
                tint = BrandPrimary,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Compare Prices",
                style = MaterialTheme.typography.labelLarge,
                color = BrandPrimaryDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }

            Button(
              onClick = { showBoltTransportSheet = true },
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(containerColor = StatusSafeGreenDark),
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("marketplace_order_bolt_transport_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.TwoWheeler,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Bolt Transport",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }

        // Section Header, Sort Dropdown & Category Showcase Shortcut
        item(key = "section_header") {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              val activeLabel = when {
                favoritesOnly -> "Saved Favorites (${filteredProducts.size})"
                dealsOnly && selectedCategoryKey != null ->
                  "${ProductCategory.fromKey(selectedCategoryKey!!).displayName} Deals (${filteredProducts.size})"
                dealsOnly -> "Daily Deals (${filteredProducts.size})"
                selectedCategoryKey != null ->
                  "${ProductCategory.fromKey(selectedCategoryKey!!).displayName} (${filteredProducts.size})"
                else -> "All Products (${filteredProducts.size})"
              }
              Text(
                text = activeLabel,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
              )
              TextButton(
                onClick = onOpenCategoryShowcase,
                modifier = Modifier.testTag("marketplace_open_showcase_button"),
              ) {
                Icon(
                  imageVector = Icons.Filled.ViewCarousel,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Category Deck")
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.Sort,
                  contentDescription = null,
                  tint = BrandPrimary,
                  modifier = Modifier.size(18.dp),
                )
                Text(
                  text = "Sort products",
                  style = MaterialTheme.typography.bodyMedium,
                  color = TextSecondary,
                )
              }

              Box {
                Surface(
                  modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { sortMenuExpanded = true }
                    .testTag("marketplace_sort_button"),
                  shape = RoundedCornerShape(12.dp),
                  color = BgSurfaceCard,
                  border = BorderStroke(
                    width = 1.dp,
                    color = if (sortMenuExpanded) BrandPrimary else BorderHairline,
                  ),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    Text(
                      text = selectedSortOption.label,
                      style = MaterialTheme.typography.labelLarge,
                      color = TextPrimary,
                    )
                    Icon(
                      imageVector = Icons.Filled.ArrowDropDown,
                      contentDescription = "Choose sort order",
                      tint = TextPrimary,
                      modifier = Modifier.size(20.dp),
                    )
                  }
                }

                DropdownMenu(
                  expanded = sortMenuExpanded,
                  onDismissRequest = { sortMenuExpanded = false },
                  modifier = Modifier
                    .background(BgSurfaceCard)
                    .testTag("marketplace_sort_menu"),
                ) {
                  MarketplaceSortOption.entries.forEach { option ->
                    val isSelected = option == selectedSortOption
                    DropdownMenuItem(
                      text = {
                        Text(
                          text = option.label,
                          style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                          ),
                          color = if (isSelected) BrandPrimary else TextPrimary,
                        )
                      },
                      trailingIcon = {
                        if (isSelected) {
                          Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Selected",
                            tint = BrandPrimary,
                            modifier = Modifier.size(18.dp),
                          )
                        }
                      },
                      onClick = {
                        selectedSortOption = option
                        sortMenuExpanded = false
                      },
                      modifier = Modifier.testTag(option.optionTestTag),
                    )
                  }
                }
              }
            }
          }
        }

        // Firestore Product Fetch States
        when (productsState) {
          UiState.Loading -> {
            items(4, key = { index -> "skeleton_product_$index" }) { index ->
              MarketplaceProductSkeletonCard(
                showImagePlaceholder = index % 2 == 0,
                modifier = Modifier.testTag("marketplace_loading_skeleton_$index"),
              )
            }
          }

          is UiState.Error -> {
            item(key = "error_card") {
              Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = StatusDangerSoft),
                border = BorderStroke(1.dp, StatusDanger.copy(alpha = 0.3f)),
              ) {
                Row(
                  modifier = Modifier.padding(16.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  Icon(
                    imageVector = Icons.Filled.CloudOff,
                    contentDescription = null,
                    tint = StatusDanger,
                  )
                  Column {
                    Text(
                      text = "Unable to load marketplace items",
                      style = MaterialTheme.typography.titleMedium,
                      color = TextPrimary,
                    )
                    Text(
                      text = productsState.message,
                      style = MaterialTheme.typography.bodyMedium,
                      color = TextSecondary,
                    )
                  }
                }
              }
            }
          }

          is UiState.Success -> {
            if (filteredProducts.isEmpty()) {
              item(key = "empty_card") {
                Card(
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(18.dp),
                  colors = CardDefaults.cardColors(containerColor = BgSurfaceCard),
                  border = BorderStroke(1.dp, BorderHairline),
                ) {
                  Column(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Filled.Inventory2,
                      contentDescription = null,
                      tint = BrandPrimary,
                      modifier = Modifier.size(44.dp),
                    )
                    Text(
                      text = if (allProducts.isEmpty()) {
                        "No Firestore Products Listed Yet"
                      } else {
                        "No Matching Products Found"
                      },
                      style = MaterialTheme.typography.titleLarge,
                      color = TextPrimary,
                    )
                    Text(
                      text = if (allProducts.isEmpty()) {
                        "Seed sample Tanzania TZS listings into Firestore or publish your own item from the Seller portal."
                      } else {
                        "Try clearing your search query or switching category filters."
                      },
                      style = MaterialTheme.typography.bodyMedium,
                      color = TextSecondary,
                    )
                    if (allProducts.isEmpty()) {
                      Button(
                        onClick = onSeedSampleCatalog,
                        enabled = !isBusy,
                        modifier = Modifier
                          .fillMaxWidth()
                          .height(48.dp)
                          .testTag("marketplace_seed_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                      ) {
                        if (isBusy) {
                          CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                          )
                        } else {
                          Text("Populate Sample TSh Catalog (8 Categories)")
                        }
                      }
                      OutlinedButton(
                        onClick = onSwitchToProvider,
                        modifier = Modifier
                          .fillMaxWidth()
                          .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                      ) {
                        Icon(Icons.Filled.Storefront, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Provider / Seller Portal")
                      }
                    } else {
                      OutlinedButton(
                        onClick = {
                          onSearchQueryChange("")
                          selectedCategoryKey = null
                          dealsOnly = false
                          favoritesOnly = false
                        },
                        modifier = Modifier
                          .fillMaxWidth()
                          .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                      ) {
                        Text("Show All Firestore Products")
                      }
                    }
                  }
                }
              }
            } else {
              items(filteredProducts, key = { it.id }) { product ->
                val inCartQty = cartItems.firstOrNull { it.productId == product.id }?.quantity ?: 0L
                MarketplaceProductCard(
                  product = product,
                  isFavorite = favoriteProductIds.contains(product.id),
                  inCartQuantity = inCartQty,
                  onCardClicked = { onProductClicked(product) },
                  onToggleFavorite = { onToggleFavorite(product) },
                  onCompareClicked = {
                    focusedCompareProduct = product
                    showPriceComparisonSheet = true
                  },
                  onAddToCart = { onAddToCart(product) },
                  onBuyClicked = { checkoutProduct = product },
                )
              }
            }
          }
        }
      }
    }

    FloatingLiveMapButton(
      isOrderInTransit = isAnyInTransit,
      onClick = onOpenLiveMap,
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(end = 20.dp, bottom = 16.dp),
    )
  }

  if (showPriceComparisonSheet) {
    val compareSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val allLoadedProducts = (productsState as? UiState.Success)?.data ?: emptyList()
    ModalBottomSheet(
      onDismissRequest = {
        showPriceComparisonSheet = false
        focusedCompareProduct = null
      },
      sheetState = compareSheetState,
      containerColor = BgSurfaceCard,
    ) {
      PriceComparisonSheet(
        allProducts = allLoadedProducts,
        focusedProduct = focusedCompareProduct,
        isBusy = isBusy,
        onSelectOfferToBuy = { chosenProduct, _ ->
          showPriceComparisonSheet = false
          focusedCompareProduct = null
          checkoutProduct = chosenProduct
        },
        onAddOfferToCart = { chosenProduct ->
          onAddToCart(chosenProduct)
        },
      )
    }
  }

  if (showBoltTransportSheet) {
    val boltSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
      onDismissRequest = { showBoltTransportSheet = false },
      sheetState = boltSheetState,
      containerColor = BgSurfaceCard,
    ) {
      BoltTransportOrderSheet(
        defaultRoleIsSeller = false,
        isBusy = isBusy,
        onDispatchTransport = { tier, isSellerMode, pickupHub, dropoff, distKm, weightKg, method ->
          showBoltTransportSheet = false
          onDispatchBoltTransport(tier, isSellerMode, pickupHub, dropoff, distKm, weightKg, method)
        },
      )
    }
  }

  if (showCartSheet) {
    val cartSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
      onDismissRequest = { showCartSheet = false },
      sheetState = cartSheetState,
      containerColor = BgSurfaceCard,
    ) {
      ShoppingCartSheet(
        cartItems = cartItems,
        isBusy = isBusy,
        onIncrementQuantity = { item -> onUpdateCartQuantity(item, (item.quantity + 1).toInt()) },
        onDecrementQuantity = { item -> onUpdateCartQuantity(item, (item.quantity - 1).toInt()) },
        onRemoveItem = onRemoveCartItem,
        onClearCart = onClearCart,
        onProceedToCheckout = {
          showCartSheet = false
          onOpenCheckout()
        },
      )
    }
  }

  val selected = checkoutProduct
  if (selected != null) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
      onDismissRequest = { checkoutProduct = null },
      sheetState = sheetState,
      containerColor = BgSurfaceCard,
    ) {
      ProductCheckoutSheet(
        product = selected,
        isBusy = isBusy,
        onConfirmOrder = { qty, size, distKm, method ->
          onPlaceOrder(selected, qty, size, distKm, method)
          checkoutProduct = null
        },
      )
    }
  }
}

@Composable
private fun MarketplaceProductCard(
  product: Product,
  isFavorite: Boolean,
  inCartQuantity: Long,
  onCardClicked: () -> Unit,
  onToggleFavorite: () -> Unit,
  onCompareClicked: () -> Unit = {},
  onAddToCart: () -> Unit,
  onBuyClicked: () -> Unit,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onCardClicked)
      .testTag("marketplace_product_card_${product.id}"),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = BgSurfaceCard),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border = BorderStroke(1.dp, BorderHairline),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      if (!product.photoUri.isNullOrBlank()) {
        AsyncImage(
          model = product.photoUri,
          contentDescription = product.title,
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(12.dp)),
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          shape = RoundedCornerShape(999.dp),
          color = BrandSecondary,
        ) {
          Text(
            text = ProductCategory.fromKey(product.category).displayName,
            style = MaterialTheme.typography.labelMedium,
            color = BrandPrimaryDark,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          if (product.isDailyDeal) {
            Surface(shape = RoundedCornerShape(999.dp), color = AccentWarmSoft) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Icon(Icons.Filled.LocalOffer, contentDescription = null, tint = AccentWarm, modifier = Modifier.size(13.dp))
                Text("Deal", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
              }
            }
          }
          Surface(
            shape = RoundedCornerShape(999.dp),
            color = if (product.requiresTruck) AccentWarmSoft else StatusSafeGreenSoft,
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = if (product.requiresTruck) Icons.Filled.LocalShipping else Icons.Filled.TwoWheeler,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(14.dp),
              )
              Text(
                text = if (product.requiresTruck) "Truck (${product.weightKg}kg)" else "Moto Box",
                style = MaterialTheme.typography.labelMedium,
                color = TextPrimary,
              )
            }
          }
          IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier
              .size(48.dp)
              .testTag("marketplace_favorite_button_${product.id}"),
          ) {
            Icon(
              imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
              contentDescription = if (isFavorite) "Remove ${product.title} from favorites" else "Save ${product.title} to favorites",
              tint = if (isFavorite) StatusDanger else TextSecondary,
            )
          }
        }
      }

      Text(
        text = product.title,
        style = MaterialTheme.typography.titleLarge,
        color = TextPrimary,
      )

      if (product.description.isNotBlank()) {
        Text(
          text = product.description,
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "Size: ${product.sizeSpec}",
          style = MaterialTheme.typography.labelMedium,
          color = TextSecondary,
        )
        Text(
          text = "${product.quantityAvailable} in stock · ${product.pickupLabel}",
          style = MaterialTheme.typography.labelMedium,
          color = TextSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      HorizontalDivider(color = BorderHairline)

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = formatTzs(product.priceTzs),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = TextPrimary,
          )
          Text(
            text = "Seller: ${product.providerName}",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Surface(
            modifier = Modifier
              .padding(top = 4.dp)
              .clip(RoundedCornerShape(999.dp))
              .clickable(onClick = onCompareClicked)
              .testTag("marketplace_compare_card_${product.id}"),
            shape = RoundedCornerShape(999.dp),
            color = BrandSecondary,
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.CompareArrows,
                contentDescription = null,
                tint = BrandPrimaryDark,
                modifier = Modifier.size(13.dp),
              )
              Text(
                text = "Compare Sellers",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = BrandPrimaryDark,
              )
            }
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedButton(
            onClick = onAddToCart,
            enabled = product.quantityAvailable > 0,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(
              width = 1.dp,
              color = if (inCartQuantity > 0) BrandPrimary else BorderInput,
            ),
            contentPadding = PaddingValues(horizontal = 12.dp),
            modifier = Modifier
              .height(48.dp)
              .testTag("marketplace_add_to_cart_${product.id}"),
          ) {
            Icon(
              imageVector = Icons.Filled.AddShoppingCart,
              contentDescription = "Add ${product.title} to cart",
              tint = BrandPrimary,
              modifier = Modifier.size(18.dp),
            )
            if (inCartQuantity > 0) {
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = inCartQuantity.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = BrandPrimary,
              )
            }
          }

          Button(
            onClick = onBuyClicked,
            enabled = product.quantityAvailable > 0,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
            modifier = Modifier
              .height(48.dp)
              .testTag("marketplace_buy_button_${product.id}"),
          ) {
            Text(if (product.quantityAvailable > 0) "Buy Now" else "Sold Out")
          }
        }
      }
    }
  }
}

@Composable
private fun MarketplaceProductSkeletonCard(
  showImagePlaceholder: Boolean,
  modifier: Modifier = Modifier,
) {
  val shimmerBrush = rememberShimmerBrush()
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = BgSurfaceCard),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border = BorderStroke(1.dp, BorderHairline),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      if (showImagePlaceholder) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(shimmerBrush),
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier = Modifier
            .width(96.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(shimmerBrush),
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Box(
            modifier = Modifier
              .width(86.dp)
              .height(24.dp)
              .clip(RoundedCornerShape(999.dp))
              .background(shimmerBrush),
          )
          Box(
            modifier = Modifier
              .size(32.dp)
              .clip(CircleShape)
              .background(shimmerBrush),
          )
        }
      }

      Box(
        modifier = Modifier
          .fillMaxWidth(0.72f)
          .height(22.dp)
          .clip(RoundedCornerShape(6.dp))
          .background(shimmerBrush),
      )

      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
          modifier = Modifier
            .fillMaxWidth(0.94f)
            .height(14.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(shimmerBrush),
        )
        Box(
          modifier = Modifier
            .fillMaxWidth(0.62f)
            .height(14.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(shimmerBrush),
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Box(
          modifier = Modifier
            .width(92.dp)
            .height(14.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(shimmerBrush),
        )
        Box(
          modifier = Modifier
            .width(126.dp)
            .height(14.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(shimmerBrush),
        )
      }

      HorizontalDivider(color = BorderHairline)

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Box(
            modifier = Modifier
              .width(114.dp)
              .height(22.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(shimmerBrush),
          )
          Box(
            modifier = Modifier
              .width(88.dp)
              .height(14.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(shimmerBrush),
          )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Box(
            modifier = Modifier
              .size(48.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(shimmerBrush),
          )
          Box(
            modifier = Modifier
              .width(96.dp)
              .height(48.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(shimmerBrush),
          )
        }
      }
    }
  }
}
