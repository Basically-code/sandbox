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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
import com.example.ui.theme.StatusSafeGreen
import com.example.ui.theme.StatusSafeGreenSoft
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryCatalogScreen(
  title: String,
  subtitle: String,
  products: List<Product>,
  isBusy: Boolean,
  onBack: () -> Unit,
  onSeedSampleCatalog: () -> Unit,
  onSwitchToProvider: () -> Unit,
  onProductClicked: (Product) -> Unit = {},
  onPlaceOrder: (
    product: Product,
    quantity: Int,
    selectedSize: String,
    distanceKm: Double,
    paymentMethod: PaymentMethod,
  ) -> Unit,
  modifier: Modifier = Modifier,
) {
  var checkoutProduct by remember { mutableStateOf<Product?>(null) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(BgPrimaryCanvas)
      .statusBarsPadding(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
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
          IconButton(onClick = onBack, modifier = Modifier.testTag("catalog_back_button")) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = TextPrimary,
            )
          }
          Column {
            Text(text = title, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            Text(text = subtitle, style = MaterialTheme.typography.labelMedium, color = BrandPrimary)
          }
        }
        HorizontalDivider(color = BorderHairline)
      }
    }

    if (products.isEmpty()) {
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
            modifier = Modifier.padding(22.dp),
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
              text = "No Active Listings Here Yet",
              style = MaterialTheme.typography.titleLarge,
              color = TextPrimary,
            )
            Text(
              text = "Sellers can publish products across all 8 categories, or you can populate our Tanzanian TZS sample marketplace to test ordering immediately.",
              style = MaterialTheme.typography.bodyMedium,
              color = TextSecondary,
            )
            Button(
              onClick = onSeedSampleCatalog,
              enabled = !isBusy,
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("seed_sample_catalog_button"),
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
            ) {
              if (isBusy) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
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
          }
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 600.dp)
          .weight(1f),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        items(products, key = { it.id }) { product ->
          CatalogProductCard(
            product = product,
            onCardClicked = { onProductClicked(product) },
            onBuyClicked = { checkoutProduct = product },
          )
        }
      }
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
private fun CatalogProductCard(
  product: Product,
  onCardClicked: () -> Unit,
  onBuyClicked: () -> Unit,
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onCardClicked)
      .testTag("product_card_${product.id}"),
    shape = RoundedCornerShape(18.dp),
    color = BgSurfaceCard,
    border = BorderStroke(1.dp, BorderHairline),
    shadowElevation = 2.dp,
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
            .height(150.dp)
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

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                text = if (product.requiresTruck) "Truck (${product.weightKg}kg)" else "Moto Box",
                style = MaterialTheme.typography.labelMedium,
                color = TextPrimary,
              )
            }
          }
        }
      }

      Text(text = product.title, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
      if (product.description.isNotBlank()) {
        Text(text = product.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
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
          text = "Stock: ${product.quantityAvailable} · ${product.pickupLabel}",
          style = MaterialTheme.typography.labelMedium,
          color = TextSecondary,
        )
      }

      HorizontalDivider(color = BorderHairline)

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column {
          Text(
            text = formatTzs(product.priceTzs),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = TextPrimary,
          )
          Text(
            text = "Seller: ${product.providerName}",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
          )
        }

        Button(
          onClick = onBuyClicked,
          enabled = product.quantityAvailable > 0,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
          modifier = Modifier
            .height(48.dp)
            .testTag("buy_product_button_${product.id}"),
        ) {
          Text(if (product.quantityAvailable > 0) "Buy Now" else "Sold Out")
        }
      }
    }
  }
}

@Composable
fun ProductCheckoutSheet(
  product: Product,
  isBusy: Boolean,
  onConfirmOrder: (Int, String, Double, PaymentMethod) -> Unit,
) {
  var quantity by remember { mutableIntStateOf(1) }
  var distanceKm by remember { mutableDoubleStateOf(4.0) }
  var paymentMethod by remember { mutableStateOf(PaymentMethod.CASH_BY_HAND_WITH_RECEIPT) }

  val maxQty = product.quantityAvailable.coerceIn(1L, 50L).toInt()
  val subtotal = product.priceTzs * quantity
  val transportFee = TradeLinkRepository.computeTransportFeeTzs(product.requiresTruck, distanceKm)
  val total = subtotal + transportFee

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp)
      .padding(bottom = 28.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Text(
      text = "Checkout · ${product.title}",
      style = MaterialTheme.typography.titleLarge,
      color = TextPrimary,
    )

    // Quantity Stepper
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column {
        Text("Quantity", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Text("Size / Spec: ${product.sizeSpec}", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
      }
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = BgSurfaceSubtle,
          modifier = Modifier
            .size(44.dp)
            .clickable { if (quantity > 1) quantity-- },
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Remove, contentDescription = "Decrease quantity")
          }
        }
        Text(
          text = quantity.toString(),
          style = MaterialTheme.typography.titleLarge,
          modifier = Modifier.padding(horizontal = 6.dp),
        )
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = BgSurfaceSubtle,
          modifier = Modifier
            .size(44.dp)
            .clickable { if (quantity < maxQty) quantity++ },
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Add, contentDescription = "Increase quantity")
          }
        }
      }
    }

    // Distance Slider for Transparent Transport Fee
    Column {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Delivery Distance", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Text("${distanceKm.roundToInt()} km", style = MaterialTheme.typography.labelLarge, color = BrandPrimary)
      }
      Slider(
        value = distanceKm.toFloat(),
        onValueChange = { distanceKm = it.toDouble() },
        valueRange = 1f..20f,
        steps = 18,
        colors = SliderDefaults.colors(thumbColor = BrandPrimary, activeTrackColor = BrandPrimary),
      )
    }

    // Payment Method Selection
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text("Payment Method", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
      PaymentMethod.entries.forEach { method ->
        val selected = method == paymentMethod
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { paymentMethod = method },
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
              onClick = { paymentMethod = method },
              colors = RadioButtonDefaults.colors(selectedColor = BrandPrimary),
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(method.label, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
              Text(method.badge, style = MaterialTheme.typography.labelMedium, color = BrandPrimaryDark)
            }
            if (method == PaymentMethod.FIREWALL_CARD_PAYMENT) {
              Icon(Icons.Filled.Lock, contentDescription = null, tint = StatusSafeGreen, modifier = Modifier.size(18.dp))
            }
          }
        }
      }
    }

    // Transparent TZS Fee Breakdown
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp),
      color = BgSurfaceSubtle,
      border = BorderStroke(1.dp, BorderHairline),
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FeeRow("Item Subtotal ($quantity × ${formatTzs(product.priceTzs)})", formatTzs(subtotal))
        FeeRow(
          if (product.requiresTruck) "Truck Base + ${distanceKm.roundToInt()} km Fee" else "Motorcycle Box + ${distanceKm.roundToInt()} km Fee",
          formatTzs(transportFee),
        )
        HorizontalDivider(color = BorderHairline)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Total Payable", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = TextPrimary)
          Text(formatTzs(total), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = BrandPrimary)
        }
      }
    }

    Button(
      onClick = { onConfirmOrder(quantity, product.sizeSpec, distanceKm, paymentMethod) },
      enabled = !isBusy,
      modifier = Modifier
        .fillMaxWidth()
        .height(54.dp)
        .testTag("confirm_checkout_button"),
      shape = RoundedCornerShape(14.dp),
      colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
    ) {
      Text("PLACE ORDER & GENERATE SAFETY CODE")
    }
  }
}

@Composable
private fun FeeRow(label: String, value: String) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    Text(text = value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
  }
}
