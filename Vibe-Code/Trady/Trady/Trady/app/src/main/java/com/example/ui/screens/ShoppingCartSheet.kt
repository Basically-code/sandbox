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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.CartItem
import com.example.data.model.ProductCategory
import com.example.data.repository.TradeLinkRepository
import com.example.ui.components.formatTzs
import com.example.ui.theme.BgSurfaceCard
import com.example.ui.theme.BgSurfaceSubtle
import com.example.ui.theme.BorderHairline
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandPrimaryDark
import com.example.ui.theme.BrandSecondary
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ShoppingCartSheet(
  cartItems: List<CartItem>,
  isBusy: Boolean,
  onIncrementQuantity: (CartItem) -> Unit,
  onDecrementQuantity: (CartItem) -> Unit,
  onRemoveItem: (CartItem) -> Unit,
  onClearCart: () -> Unit,
  onProceedToCheckout: () -> Unit,
) {
  val summary = remember(cartItems) {
    TradeLinkRepository.calculateCartTotals(cartItems, distanceKm = 4.0)
  }
  val totalItemCount = summary.totalQuantity
  val itemsSubtotal = summary.itemsSubtotalTzs

  LazyColumn(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(max = 640.dp)
      .testTag("shopping_cart_sheet"),
    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item(key = "cart_header") {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.ShoppingCart,
            contentDescription = null,
            tint = BrandPrimary,
          )
          Column {
            Text(
              text = "Shopping Cart",
              style = MaterialTheme.typography.titleLarge,
              color = TextPrimary,
            )
            Text(
              text = if (totalItemCount == 1) {
                "1 item saved to your account"
              } else {
                "$totalItemCount items saved to your account"
              },
              style = MaterialTheme.typography.labelMedium,
              color = TextSecondary,
            )
          }
        }
        if (cartItems.isNotEmpty()) {
          TextButton(
            onClick = onClearCart,
            enabled = !isBusy,
            modifier = Modifier.testTag("cart_clear_button"),
          ) {
            Text("Clear", color = StatusDanger)
          }
        }
      }
      HorizontalDivider(color = BorderHairline, modifier = Modifier.padding(top = 8.dp))
    }

    if (cartItems.isEmpty()) {
      item(key = "cart_empty") {
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .testTag("cart_empty_state"),
          shape = RoundedCornerShape(16.dp),
          color = BgSurfaceSubtle,
          border = BorderStroke(1.dp, BorderHairline),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = Icons.Outlined.ShoppingCart,
              contentDescription = null,
              tint = BrandPrimary,
              modifier = Modifier.size(44.dp),
            )
            Text(
              text = "Your cart is empty",
              style = MaterialTheme.typography.titleMedium,
              color = TextPrimary,
            )
            Text(
              text = "Add products from the Marketplace to save them to your persistent Firestore cart.",
              style = MaterialTheme.typography.bodyMedium,
              color = TextSecondary,
            )
          }
        }
      }
    } else {
      items(cartItems, key = { it.productId }) { item ->
        CartItemCard(
          item = item,
          onIncrement = { onIncrementQuantity(item) },
          onDecrement = { onDecrementQuantity(item) },
          onRemove = { onRemoveItem(item) },
        )
      }

      item(key = "cart_checkout_controls") {
        Column(
          modifier = Modifier.padding(top = 4.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          HorizontalDivider(color = BorderHairline)

          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = BgSurfaceSubtle,
            border = BorderStroke(1.dp, BorderHairline),
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              CartSummaryRow("Items Subtotal ($totalItemCount items)", formatTzs(itemsSubtotal))
              Text(
                text = "Delivery fee & payment method are calculated on the Checkout screen.",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
              )
            }
          }

          Button(
            onClick = onProceedToCheckout,
            enabled = !isBusy,
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("cart_proceed_to_checkout_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
          ) {
            Text(
              text = "Proceed to Checkout · ${formatTzs(itemsSubtotal)}",
              style = MaterialTheme.typography.labelLarge,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun CartItemCard(
  item: CartItem,
  onIncrement: () -> Unit,
  onDecrement: () -> Unit,
  onRemove: () -> Unit,
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("cart_item_card_${item.productId}"),
    shape = RoundedCornerShape(16.dp),
    color = BgSurfaceCard,
    border = BorderStroke(1.dp, BorderHairline),
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
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

        Column(modifier = Modifier.weight(1f)) {
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
              text = "${formatTzs(item.priceTzs)} each",
              style = MaterialTheme.typography.labelLarge,
              color = BrandPrimary,
            )
          }
        }

        IconButton(
          onClick = onRemove,
          modifier = Modifier
            .size(48.dp)
            .testTag("cart_remove_${item.productId}"),
        ) {
          Icon(
            imageVector = Icons.Filled.DeleteOutline,
            contentDescription = "Remove ${item.title} from cart",
            tint = StatusDanger,
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "Subtotal: ${formatTzs(item.priceTzs * item.quantity)}",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = TextPrimary,
        )

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = BgSurfaceSubtle,
            border = BorderStroke(1.dp, BorderHairline),
            modifier = Modifier
              .size(44.dp)
              .clip(RoundedCornerShape(10.dp))
              .clickable(onClick = onDecrement)
              .testTag("cart_dec_${item.productId}"),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(Icons.Filled.Remove, contentDescription = "Decrease quantity", tint = TextPrimary)
            }
          }
          Text(
            text = item.quantity.toString(),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 8.dp),
          )
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = BrandSecondary,
            border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.35f)),
            modifier = Modifier
              .size(44.dp)
              .clip(RoundedCornerShape(10.dp))
              .clickable(onClick = onIncrement)
              .testTag("cart_inc_${item.productId}"),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(Icons.Filled.Add, contentDescription = "Increase quantity", tint = BrandPrimaryDark)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CartSummaryRow(label: String, value: String) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    Text(text = value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
  }
}
