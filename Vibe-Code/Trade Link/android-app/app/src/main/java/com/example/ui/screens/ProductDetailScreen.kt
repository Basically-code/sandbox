package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.repository.TradeLinkRepository
import com.example.ui.components.formatTzs
import com.example.ui.theme.AccentWarm
import com.example.ui.theme.AccentWarmSoft
import com.example.ui.theme.BgPrimaryCanvas
import com.example.ui.theme.BgSurfaceCard
import com.example.ui.theme.BgSurfaceSubtle
import com.example.ui.theme.BorderHairline
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandPrimaryDark
import com.example.ui.theme.BrandSecondary
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusDangerSoft
import com.example.ui.theme.StatusSafeGreen
import com.example.ui.theme.StatusSafeGreenSoft
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProductDetailScreen(
  product: Product?,
  isFavorite: Boolean,
  cartQuantity: Long,
  isBusy: Boolean,
  onBack: () -> Unit,
  onToggleFavorite: () -> Unit,
  onAddToCart: () -> Unit,
  onPlaceOrder: (
    quantity: Int,
    selectedSize: String,
    distanceKm: Double,
    paymentMethod: PaymentMethod,
  ) -> Unit,
  allProducts: List<Product> = emptyList(),
  onAddOfferToCart: (Product) -> Unit = { onAddToCart() },
  onPlaceOfferOrder: (
    chosenProduct: Product,
    quantity: Int,
    selectedSize: String,
    distanceKm: Double,
    paymentMethod: PaymentMethod,
  ) -> Unit = { _, qty, size, distKm, method -> onPlaceOrder(qty, size, distKm, method) },
  modifier: Modifier = Modifier,
) {
  var showCheckoutSheet by remember { mutableStateOf(false) }
  var showPriceCompareSheet by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(BgPrimaryCanvas)
      .statusBarsPadding(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // Sticky Top App Bar
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
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            IconButton(
              onClick = onBack,
              modifier = Modifier
                .size(48.dp)
                .testTag("product_detail_back_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to Marketplace",
                tint = TextPrimary,
              )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column {
              Text(
                text = "Product Details",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = if (product != null) {
                  ProductCategory.fromKey(product.category).displayName
                } else {
                  "Trady Marketplace"
                },
                style = MaterialTheme.typography.labelMedium,
                color = BrandPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }

          if (product != null) {
            IconButton(
              onClick = onToggleFavorite,
              modifier = Modifier
                .size(48.dp)
                .testTag("product_detail_favorite_button"),
            ) {
              Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (isFavorite) {
                  "Remove ${product.title} from favorites"
                } else {
                  "Save ${product.title} to favorites"
                },
                tint = if (isFavorite) StatusDanger else TextSecondary,
              )
            }
          }
        }
        HorizontalDivider(color = BorderHairline)
      }
    }

    if (product == null) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .padding(24.dp),
        contentAlignment = Alignment.Center,
      ) {
        Surface(
          modifier = Modifier.widthIn(max = 460.dp),
          shape = RoundedCornerShape(20.dp),
          color = BgSurfaceCard,
          border = BorderStroke(1.dp, BorderHairline),
        ) {
          Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.Inventory2,
              contentDescription = null,
              tint = BrandPrimary,
              modifier = Modifier.size(48.dp),
            )
            Text(
              text = "Product Unavailable",
              style = MaterialTheme.typography.titleLarge,
              color = TextPrimary,
            )
            Text(
              text = "This listing may have been removed or is still syncing from Firestore.",
              style = MaterialTheme.typography.bodyMedium,
              color = TextSecondary,
            )
            Button(
              onClick = onBack,
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
            ) {
              Text("Back to Marketplace")
            }
          }
        }
      }
    } else {
      val categoryInfo = ProductCategory.fromKey(product.category)
      val minDeliveryFee = TradeLinkRepository.computeTransportFeeTzs(
        requiresTruck = product.requiresTruck,
        distanceKm = 1.0,
      )

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 620.dp)
          .weight(1f)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 16.dp)
          .testTag("product_detail_scroll_content"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        // Hero Image & Visual Showcase Card
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("product_detail_image_card"),
          shape = RoundedCornerShape(20.dp),
          color = BgSurfaceCard,
          border = BorderStroke(1.dp, BorderHairline),
          shadowElevation = 2.dp,
        ) {
          Column {
            if (!product.photoUri.isNullOrBlank()) {
              Box(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                  model = product.photoUri,
                  contentDescription = "${product.title} product image",
                  contentScale = ContentScale.Crop,
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .testTag("product_detail_hero_image"),
                )
                Surface(
                  modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                  shape = RoundedCornerShape(999.dp),
                  color = Color.Black.copy(alpha = 0.65f),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Filled.Image,
                      contentDescription = null,
                      tint = Color.White,
                      modifier = Modifier.size(14.dp),
                    )
                    Text(
                      text = "Verified Listing Photo",
                      style = MaterialTheme.typography.labelMedium,
                      color = Color.White,
                    )
                  }
                }
              }
            } else {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(200.dp)
                  .background(
                    Brush.linearGradient(
                      colors = listOf(
                        BrandSecondary,
                        AccentWarmSoft.copy(alpha = 0.7f),
                        BgSurfaceSubtle,
                      ),
                    ),
                  )
                  .testTag("product_detail_visual_preview"),
                contentAlignment = Alignment.Center,
              ) {
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Surface(
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    color = BgSurfaceCard,
                    border = BorderStroke(1.5.dp, BrandPrimary.copy(alpha = 0.25f)),
                    shadowElevation = 4.dp,
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        imageVector = categoryIcon(categoryInfo),
                        contentDescription = "${categoryInfo.displayName} product visual",
                        tint = BrandPrimary,
                        modifier = Modifier.size(34.dp),
                      )
                    }
                  }
                  Text(
                    text = categoryInfo.tagline,
                    style = MaterialTheme.typography.labelLarge,
                    color = BrandPrimaryDark,
                  )
                }
              }
            }

            // Image details footer bar
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(BgSurfaceSubtle)
                .padding(horizontal = 14.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f),
              ) {
                Icon(
                  imageVector = Icons.Filled.Image,
                  contentDescription = null,
                  tint = BrandPrimary,
                  modifier = Modifier.size(16.dp),
                )
                Text(
                  text = if (!product.photoUri.isNullOrBlank()) {
                    "Image source: ${product.photoUri.substringAfterLast('/').ifBlank { "Seller Upload" }}"
                  } else {
                    "Catalog Visual · ${categoryInfo.displayName}"
                  },
                  style = MaterialTheme.typography.labelMedium,
                  color = TextSecondary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = product.sizeSpec.ifBlank { "Standard Size" },
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary,
              )
            }
          }
        }

        // Title, Badges & Price Card
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          color = BgSurfaceCard,
          border = BorderStroke(1.dp, BorderHairline),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Surface(shape = RoundedCornerShape(999.dp), color = BrandSecondary) {
                Text(
                  text = categoryInfo.displayName,
                  style = MaterialTheme.typography.labelMedium,
                  color = BrandPrimaryDark,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
              }
              if (product.isDailyDeal) {
                Surface(shape = RoundedCornerShape(999.dp), color = AccentWarmSoft) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    Icon(Icons.Filled.LocalOffer, contentDescription = null, tint = AccentWarm, modifier = Modifier.size(13.dp))
                    Text("Daily Deal", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
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
                    text = if (product.requiresTruck) "Commercial Truck" else "Moto Cargo Box",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextPrimary,
                  )
                }
              }
              Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (product.quantityAvailable > 0) StatusSafeGreenSoft else StatusDangerSoft,
              ) {
                Text(
                  text = if (product.quantityAvailable > 0) "${product.quantityAvailable} in stock" else "Out of stock",
                  style = MaterialTheme.typography.labelMedium,
                  color = if (product.quantityAvailable > 0) StatusSafeGreen else StatusDanger,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
              }
            }

            Text(
              text = product.title,
              style = MaterialTheme.typography.headlineMedium,
              color = TextPrimary,
              modifier = Modifier.testTag("product_detail_title"),
            )

            Text(
              text = formatTzs(product.priceTzs),
              style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
              color = BrandPrimary,
              modifier = Modifier.testTag("product_detail_price"),
            )

            Text(
              text = "Delivery starts from ${formatTzs(minDeliveryFee)} (1 km) · 100% Buyer Protection Guarantee",
              style = MaterialTheme.typography.bodyMedium,
              color = TextSecondary,
            )

            OutlinedButton(
              onClick = { showPriceCompareSheet = true },
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.2.dp, BrandPrimary),
              modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("product_detail_compare_prices_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.CompareArrows,
                contentDescription = null,
                tint = BrandPrimary,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Compare Prices & Landed Transport Across Sellers",
                style = MaterialTheme.typography.labelLarge,
                color = BrandPrimaryDark,
              )
            }
          }
        }

        // Full Description Card
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          color = BgSurfaceCard,
          border = BorderStroke(1.dp, BorderHairline),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = "Product Description",
              style = MaterialTheme.typography.titleMedium,
              color = TextPrimary,
            )
            Text(
              text = product.description.ifBlank {
                "Verified ${categoryInfo.displayName} listing sold by ${product.providerName} from ${product.pickupLabel}. Protected end-to-end with verbal secret safety code gate release and 100% full refund coverage."
              },
              style = MaterialTheme.typography.bodyLarge,
              color = TextSecondary,
              modifier = Modifier.testTag("product_detail_description"),
            )
          }
        }

        // Specifications, Image & Fulfillment Details Card
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          color = BgSurfaceCard,
          border = BorderStroke(1.dp, BorderHairline),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text = "Specifications & Image Details",
              style = MaterialTheme.typography.titleMedium,
              color = TextPrimary,
            )
            DetailSpecRow(
              icon = Icons.Filled.Storefront,
              label = "Seller",
              value = product.providerName.ifBlank { "Verified Trady Merchant" },
            )
            HorizontalDivider(color = BorderHairline)
            DetailSpecRow(
              icon = Icons.Filled.LocationOn,
              label = "Pickup Hub",
              value = product.pickupLabel.ifBlank { "Kariakoo Market Block A" },
            )
            HorizontalDivider(color = BorderHairline)
            DetailSpecRow(
              icon = Icons.Filled.Inventory2,
              label = "Size / Variant",
              value = product.sizeSpec.ifBlank { "Standard" },
            )
            HorizontalDivider(color = BorderHairline)
            DetailSpecRow(
              icon = if (product.requiresTruck) Icons.Filled.LocalShipping else Icons.Filled.TwoWheeler,
              label = "Dispatch Weight",
              value = "${product.weightKg} kg (${if (product.requiresTruck) "Truck Required >= 50 kg" else "Motorcycle Cargo Box"})",
            )
            HorizontalDivider(color = BorderHairline)
            DetailSpecRow(
              icon = Icons.Filled.Image,
              label = "Image Details",
              value = if (!product.photoUri.isNullOrBlank()) {
                "Seller Photo Attached (${product.photoUri.take(40)})"
              } else {
                "Catalog Preview (${categoryInfo.displayName} · No custom photo URI)"
              },
            )
            HorizontalDivider(color = BorderHairline)
            DetailSpecRow(
              icon = Icons.Filled.Shield,
              label = "Buyer Protection",
              value = "100% Refund Guarantee + Verbal Secret Code Gate",
            )
          }
        }
      }

      // Sticky Bottom Checkout Bar
      Surface(
        modifier = Modifier.fillMaxWidth(),
        color = BgSurfaceCard,
        shadowElevation = 8.dp,
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          HorizontalDivider(color = BorderHairline)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .widthIn(max = 620.dp)
              .padding(horizontal = 16.dp, vertical = 12.dp),
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
                text = if (product.quantityAvailable > 0) {
                  "${product.quantityAvailable} available"
                } else {
                  "Currently out of stock"
                },
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedButton(
                onClick = onAddToCart,
                enabled = product.quantityAvailable > 0 && !isBusy,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, BrandPrimary),
                modifier = Modifier
                  .height(52.dp)
                  .testTag("product_detail_add_to_cart_button"),
              ) {
                Icon(
                  imageVector = Icons.Filled.AddShoppingCart,
                  contentDescription = "Add to Cart",
                  tint = BrandPrimary,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = if (cartQuantity > 0) "Cart ($cartQuantity)" else "Add",
                  style = MaterialTheme.typography.labelLarge,
                  color = BrandPrimary,
                )
              }
              Button(
                onClick = { showCheckoutSheet = true },
                enabled = product.quantityAvailable > 0 && !isBusy,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                modifier = Modifier
                  .height(52.dp)
                  .testTag("product_detail_buy_button"),
              ) {
                Text(
                  text = if (product.quantityAvailable > 0) "Buy Now" else "Sold Out",
                  style = MaterialTheme.typography.labelLarge,
                )
              }
            }
          }
        }
      }

      if (showPriceCompareSheet) {
        val compareSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
          onDismissRequest = { showPriceCompareSheet = false },
          sheetState = compareSheetState,
          containerColor = BgSurfaceCard,
        ) {
          PriceComparisonSheet(
            allProducts = if (allProducts.isNotEmpty()) allProducts else listOf(product),
            focusedProduct = product,
            isBusy = isBusy,
            onSelectOfferToBuy = { chosenProduct, distKm ->
              showPriceCompareSheet = false
              onPlaceOfferOrder(
                chosenProduct,
                1,
                chosenProduct.sizeSpec,
                distKm,
                PaymentMethod.CASH_BY_HAND_WITH_RECEIPT,
              )
            },
            onAddOfferToCart = { chosenProduct ->
              onAddOfferToCart(chosenProduct)
            },
          )
        }
      }

      if (showCheckoutSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
          onDismissRequest = { showCheckoutSheet = false },
          sheetState = sheetState,
          containerColor = BgSurfaceCard,
        ) {
          ProductCheckoutSheet(
            product = product,
            isBusy = isBusy,
            onConfirmOrder = { qty, size, distKm, method ->
              onPlaceOrder(qty, size, distKm, method)
              showCheckoutSheet = false
            },
          )
        }
      }
    }
  }
}

@Composable
private fun DetailSpecRow(
  icon: ImageVector,
  label: String,
  value: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.Top,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = BrandPrimary,
      modifier = Modifier
        .padding(top = 2.dp)
        .size(18.dp),
    )
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = TextSecondary,
      modifier = Modifier.width(104.dp),
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
      color = TextPrimary,
      modifier = Modifier.weight(1f),
    )
  }
}

private fun categoryIcon(category: ProductCategory): ImageVector =
  when (category) {
    ProductCategory.SPORTS_AND_OUTDOORS -> Icons.Filled.FitnessCenter
    ProductCategory.ELECTRONICS -> Icons.Filled.Devices
    ProductCategory.FOOD -> Icons.Filled.Restaurant
    ProductCategory.BOOKS -> Icons.AutoMirrored.Filled.MenuBook
    ProductCategory.CLOTHING -> Icons.Filled.Checkroom
    ProductCategory.SHOES -> Icons.Filled.TwoWheeler
    ProductCategory.JEWELLERY -> Icons.Filled.Diamond
    ProductCategory.BEAUTY_AND_PERSONAL_CARE -> Icons.Filled.Spa
  }
