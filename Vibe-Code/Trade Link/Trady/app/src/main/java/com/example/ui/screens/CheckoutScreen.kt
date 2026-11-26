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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.CartItem
import com.example.data.model.PaymentMethod
import com.example.data.model.ProductCategory
import com.example.data.repository.TradeLinkRepository
import com.example.ui.components.formatTzs
import com.example.ui.theme.BgPrimaryCanvas
import com.example.ui.theme.BgSurfaceCard
import com.example.ui.theme.BgSurfaceSubtle
import com.example.ui.theme.BorderHairline
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandPrimaryDark
import com.example.ui.theme.BrandSecondary
import com.example.ui.theme.StatusSafeGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun CheckoutScreen(
  cartItems: List<CartItem>,
  isBusy: Boolean,
  onBack: () -> Unit,
  onConfirmPurchase: (distanceKm: Double, paymentMethod: PaymentMethod) -> Unit,
  modifier: Modifier = Modifier,
) {
  var distanceKm by rememberSaveable { mutableDoubleStateOf(4.0) }
  var paymentMethodKey by rememberSaveable { mutableStateOf(PaymentMethod.CASH_BY_HAND_WITH_RECEIPT.key) }
  val selectedPaymentMethod = PaymentMethod.fromKey(paymentMethodKey)

  val priceSummary = remember(cartItems, distanceKm) {
    TradeLinkRepository.calculateCartTotals(cartItems, distanceKm)
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(BgPrimaryCanvas)
      .statusBarsPadding()
      .testTag("checkout_screen"),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // Top App Bar
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
        ) {
          IconButton(
            onClick = onBack,
            modifier = Modifier
              .size(48.dp)
              .testTag("checkout_back_button"),
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
              text = "Checkout",
              style = MaterialTheme.typography.titleLarge,
              color = TextPrimary,
            )
            Text(
              text = if (priceSummary.totalQuantity == 1) {
                "1 item ready for purchase"
              } else {
                "${priceSummary.totalQuantity} items ready for purchase"
              },
              style = MaterialTheme.typography.labelMedium,
              color = BrandPrimary,
            )
          }
        }
        HorizontalDivider(color = BorderHairline)
      }
    }

    if (cartItems.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .padding(24.dp),
        contentAlignment = Alignment.Center,
      ) {
        Surface(
          modifier = Modifier
            .widthIn(max = 460.dp)
            .testTag("checkout_empty_state"),
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
              imageVector = Icons.Outlined.ShoppingCart,
              contentDescription = null,
              tint = BrandPrimary,
              modifier = Modifier.size(48.dp),
            )
            Text(
              text = "Your cart is empty",
              style = MaterialTheme.typography.titleLarge,
              color = TextPrimary,
            )
            Text(
              text = "Add products to your cart in the Marketplace before confirming a purchase.",
              style = MaterialTheme.typography.bodyMedium,
              color = TextSecondary,
            )
            Button(
              onClick = onBack,
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("checkout_return_marketplace_button"),
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
            ) {
              Text("Back to Marketplace")
            }
          }
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 620.dp)
          .weight(1f),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        item(key = "checkout_items_header") {
          Text(
            text = "Order Items (${cartItems.size})",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
          )
        }

        items(cartItems, key = { it.productId }) { item ->
          CheckoutItemRowCard(item = item)
        }

        item(key = "checkout_delivery_and_payment") {
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = BgSurfaceCard),
            border = BorderStroke(1.dp, BorderHairline),
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "Delivery Distance",
                  style = MaterialTheme.typography.titleMedium,
                  color = TextPrimary,
                )
                Text(
                  text = "${distanceKm.roundToInt()} km",
                  style = MaterialTheme.typography.labelLarge,
                  color = BrandPrimary,
                )
              }

              Slider(
                value = distanceKm.toFloat(),
                onValueChange = { distanceKm = it.toDouble() },
                valueRange = 1f..20f,
                steps = 18,
                colors = SliderDefaults.colors(
                  thumbColor = BrandPrimary,
                  activeTrackColor = BrandPrimary,
                ),
                modifier = Modifier.testTag("checkout_distance_slider"),
              )

              HorizontalDivider(color = BorderHairline)

              Text(
                text = "Payment Method",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
              )

              PaymentMethod.entries.forEach { method ->
                val selected = method == selectedPaymentMethod
                Surface(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable { paymentMethodKey = method.key }
                    .testTag("checkout_payment_${method.key}"),
                  shape = RoundedCornerShape(12.dp),
                  color = if (selected) BrandSecondary else BgSurfaceSubtle,
                  border = BorderStroke(1.dp, if (selected) BrandPrimary else BorderHairline),
                ) {
                  Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    RadioButton(
                      selected = selected,
                      onClick = { paymentMethodKey = method.key },
                      colors = RadioButtonDefaults.colors(selectedColor = BrandPrimary),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = method.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                      )
                      Text(
                        text = method.badge,
                        style = MaterialTheme.typography.labelMedium,
                        color = BrandPrimaryDark,
                      )
                    }
                    if (method == PaymentMethod.FIREWALL_CARD_PAYMENT) {
                      Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = StatusSafeGreen,
                        modifier = Modifier.size(18.dp),
                      )
                    }
                  }
                }
              }
            }
          }
        }

        item(key = "checkout_price_summary") {
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .testTag("checkout_price_summary_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = BgSurfaceCard),
            border = BorderStroke(1.dp, BorderHairline),
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Text(
                text = "Price Breakdown",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
              )
              CheckoutSummaryLine(
                label = "Cart Items Total (${priceSummary.totalQuantity} items)",
                value = formatTzs(priceSummary.itemsSubtotalTzs),
              )
              CheckoutSummaryLine(
                label = "Transport & Dispatch (${distanceKm.roundToInt()} km)",
                value = formatTzs(priceSummary.transportFeeTzs),
              )
              HorizontalDivider(color = BorderHairline)
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "Total Price",
                  style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                  color = TextPrimary,
                )
                Text(
                  text = formatTzs(priceSummary.grandTotalTzs),
                  style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                  color = BrandPrimary,
                  modifier = Modifier.testTag("checkout_total_price_text"),
                )
              }
            }
          }
        }
      }

      // Sticky Bottom Confirmation Bar
      Surface(
        modifier = Modifier.fillMaxWidth(),
        color = BgSurfaceCard,
        shadowElevation = 8.dp,
      ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
          HorizontalDivider(color = BorderHairline)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .widthIn(max = 620.dp)
              .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column {
              Text(
                text = "Total Payable",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
              )
              Text(
                text = formatTzs(priceSummary.grandTotalTzs),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = BrandPrimary,
              )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Button(
              onClick = { onConfirmPurchase(distanceKm, selectedPaymentMethod) },
              enabled = !isBusy && cartItems.isNotEmpty(),
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
              modifier = Modifier
                .height(50.dp)
                .weight(1f)
                .testTag("checkout_confirm_purchase_button"),
            ) {
              if (isBusy) {
                CircularProgressIndicator(
                  modifier = Modifier.size(20.dp),
                  color = Color.White,
                  strokeWidth = 2.dp,
                )
              } else {
                Icon(
                  imageVector = Icons.Filled.CheckCircle,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Confirm Purchase",
                  style = MaterialTheme.typography.labelLarge,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CheckoutItemRowCard(item: CartItem) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("checkout_item_card_${item.productId}"),
    shape = RoundedCornerShape(16.dp),
    color = BgSurfaceCard,
    border = BorderStroke(1.dp, BorderHairline),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (!item.photoUri.isNullOrBlank()) {
        AsyncImage(
          model = item.photoUri,
          contentDescription = item.title,
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(12.dp)),
        )
      } else {
        Surface(
          modifier = Modifier.size(56.dp),
          shape = RoundedCornerShape(12.dp),
          color = BrandSecondary,
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Filled.Inventory2,
              contentDescription = null,
              tint = BrandPrimary,
              modifier = Modifier.size(26.dp),
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = item.title,
          style = MaterialTheme.typography.titleMedium,
          color = TextPrimary,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "${ProductCategory.fromKey(item.category).displayName} · ${item.sizeSpec}",
          style = MaterialTheme.typography.labelMedium,
          color = TextSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Icon(
            imageVector = if (item.requiresTruck) Icons.Filled.LocalShipping else Icons.Filled.TwoWheeler,
            contentDescription = null,
            tint = BrandPrimary,
            modifier = Modifier.size(13.dp),
          )
          Text(
            text = "${item.quantity} × ${formatTzs(item.priceTzs)}",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
          )
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      Text(
        text = formatTzs(item.priceTzs * item.quantity),
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = TextPrimary,
      )
    }
  }
}

@Composable
private fun CheckoutSummaryLine(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = TextSecondary,
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
      color = TextPrimary,
    )
  }
}
