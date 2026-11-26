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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.ui.components.FloatingLiveMapButton
import com.example.ui.components.LayeredCategoryCarousel
import com.example.ui.components.SkewedDealsBanner
import com.example.ui.components.TradeLinkLogoBadge
import com.example.ui.components.formatTzs
import com.example.ui.theme.AccentWarmSoft
import com.example.ui.theme.BgPrimaryCanvas
import com.example.ui.theme.BgSurfaceCard
import com.example.ui.theme.BgSurfaceSubtle
import com.example.ui.theme.BorderHairline
import com.example.ui.theme.BorderInput
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandSecondary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun BuyerHomeScreen(
  userName: String,
  searchQuery: String,
  onSearchQueryChange: (String) -> Unit,
  selectedCarouselIndex: Int,
  onCarouselIndexChange: (Int) -> Unit,
  products: List<Product>,
  orders: List<Order>,
  onOpenDrawer: () -> Unit,
  onOpenAccountSheet: () -> Unit,
  onOpenOrders: () -> Unit,
  onOpenDailyDeals: () -> Unit,
  onCategorySelected: (ProductCategory) -> Unit,
  onProductClicked: (Product) -> Unit,
  onOpenLiveMap: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val activeOrderCount = orders.count { !OrderStatus.fromKey(it.status).isTerminal }
  val isAnyInTransit = orders.any {
    val st = OrderStatus.fromKey(it.status)
    st == OrderStatus.IN_TRANSIT ||
      st == OrderStatus.BOX_ATTACHED_PICKED_UP ||
      st == OrderStatus.DRIVER_ASSIGNED ||
      st == OrderStatus.ARRIVED_AWAITING_SECRET_CODE
  }

  val filteredSearchProducts = if (searchQuery.isBlank()) {
    emptyList()
  } else {
    val q = searchQuery.trim().lowercase()
    products.filter {
      it.title.lowercase().contains(q) ||
        it.description.lowercase().contains(q) ||
        ProductCategory.fromKey(it.category).displayName.lowercase().contains(q)
    }
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
      // 1. Top Navigation Header (Exact Sketch Layout)
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
                modifier = Modifier.testTag("hamburger_menu_button"),
              ) {
                Icon(
                  imageVector = Icons.Filled.Menu,
                  contentDescription = "Open Portal Menu",
                  tint = TextPrimary,
                )
              }
              TradeLinkLogoBadge(modifier = Modifier.size(34.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Trady Link",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
              )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              Surface(
                modifier = Modifier
                  .clip(RoundedCornerShape(999.dp))
                  .clickable(onClick = onOpenAccountSheet)
                  .testTag("header_account_button"),
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
                onClick = onOpenOrders,
                modifier = Modifier.testTag("header_orders_inbox_button"),
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
                    contentDescription = "Orders & Digital Receipts",
                    tint = TextPrimary,
                  )
                }
              }
            }
          }
          HorizontalDivider(color = BorderHairline, thickness = 1.dp)
        }
      }

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 600.dp)
          .weight(1f)
          .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Spacer(modifier = Modifier.height(12.dp))

        // 2. Global Search Bar
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
          shape = RoundedCornerShape(999.dp),
          color = BgSurfaceSubtle,
          border = BorderStroke(1.dp, BorderInput),
        ) {
          Row(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(modifier = Modifier.weight(1f)) {
              if (searchQuery.isEmpty()) {
                Text(
                  text = "Search Trade Link",
                  style = MaterialTheme.typography.bodyLarge,
                  color = TextMuted,
                )
              }
              BasicTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                cursorBrush = SolidColor(BrandPrimary),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("global_search_input"),
              )
            }
            if (searchQuery.isNotEmpty()) {
              IconButton(
                onClick = { onSearchQueryChange("") },
                modifier = Modifier.size(32.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.Close,
                  contentDescription = "Clear Search",
                  tint = TextSecondary,
                  modifier = Modifier.size(18.dp),
                )
              }
            }
            VerticalDivider(
              modifier = Modifier
                .height(22.dp)
                .padding(horizontal = 10.dp),
              color = BorderInput,
            )
            Icon(
              imageVector = Icons.Filled.Search,
              contentDescription = "Search",
              tint = BrandPrimary,
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Promotional Daily Deals Skewed Parallelogram Banner
        SkewedDealsBanner(onClick = onOpenDailyDeals)

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Central 3D Layered Category Carousel OR Live Search Results
        if (searchQuery.isNotBlank()) {
          if (filteredSearchProducts.isEmpty()) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "No listings match \"$searchQuery\". Try another keyword or swipe categories.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
              )
            }
          } else {
            LazyColumn(
              modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
              contentPadding = PaddingValues(bottom = 84.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              items(filteredSearchProducts, key = { it.id }) { product ->
                SearchProductResultCard(
                  product = product,
                  onClick = { onProductClicked(product) },
                )
              }
            }
          }
        } else {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
            contentAlignment = Alignment.Center,
          ) {
            LayeredCategoryCarousel(
              selectedIndex = selectedCarouselIndex,
              onIndexChanged = onCarouselIndexChange,
              onCategoryTap = onCategorySelected,
            )
          }
          Spacer(modifier = Modifier.height(48.dp))
        }
      }
    }

    // 5. Bottom-Right Floating Live Map Button
    FloatingLiveMapButton(
      isOrderInTransit = isAnyInTransit,
      onClick = onOpenLiveMap,
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(end = 20.dp, bottom = 16.dp),
    )
  }
}

@Composable
private fun SearchProductResultCard(
  product: Product,
  onClick: () -> Unit,
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(16.dp),
    color = BgSurfaceCard,
    border = BorderStroke(1.dp, BorderHairline),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = ProductCategory.fromKey(product.category).displayName.uppercase(),
          style = MaterialTheme.typography.labelMedium,
          color = BrandPrimary,
        )
        Text(
          text = product.title,
          style = MaterialTheme.typography.titleMedium,
          color = TextPrimary,
        )
        Text(
          text = "${product.sizeSpec} · ${product.quantityAvailable} in stock",
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary,
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = formatTzs(product.priceTzs),
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
          color = TextPrimary,
        )
        Surface(
          shape = RoundedCornerShape(999.dp),
          color = if (product.requiresTruck) AccentWarmSoft else BrandSecondary,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
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
              text = if (product.requiresTruck) "Truck" else "Moto",
              style = MaterialTheme.typography.labelMedium,
              color = TextPrimary,
            )
          }
        }
      }
    }
  }
}
