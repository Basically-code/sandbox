package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.TransportTier
import com.example.data.repository.TradeLinkRepository
import com.example.ui.components.TradeLinkLogoBadge
import com.example.ui.components.formatTzs
import com.example.ui.theme.AccentWarmSoft
import com.example.ui.theme.BgPrimaryCanvas
import com.example.ui.theme.BgSurfaceCard
import com.example.ui.theme.BorderHairline
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandSecondary
import com.example.ui.theme.StatusSafeGreenDark
import com.example.ui.theme.StatusSafeGreenSoft
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningSoft
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderPortalScreen(
  sellerProducts: List<Product>,
  orders: List<Order>,
  isBusy: Boolean,
  onOpenDrawer: () -> Unit,
  onCreateProduct: (
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
    onDone: () -> Unit,
  ) -> Unit,
  onAdvanceOrderStep: (Order) -> Unit,
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
  var showAddForm by remember { mutableStateOf(false) }
  var showBoltSheet by remember { mutableStateOf(false) }

  var selectedCategory by remember { mutableStateOf(ProductCategory.ELECTRONICS) }
  var title by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var priceInput by remember { mutableStateOf("65000") }
  var quantityInput by remember { mutableStateOf("10") }
  var sizeSpec by remember { mutableStateOf("M / Standard Box") }
  var weightInput by remember { mutableStateOf("1.5") }
  var pickupLabel by remember { mutableStateOf("Kariakoo Market Block A") }
  var isDailyDeal by remember { mutableStateOf(false) }
  var pickedPhotoUri by remember { mutableStateOf<Uri?>(null) }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    pickedPhotoUri = uri
  }

  val parsedWeight = weightInput.toDoubleOrNull() ?: 1.0
  val parsedPrice = priceInput.toDoubleOrNull() ?: 0.0
  val parsedQty = quantityInput.toLongOrNull() ?: 1L
  val requiresTruck = parsedWeight > TradeLinkRepository.MOTORCYCLE_MAX_WEIGHT_KG
  val lowStockCount = sellerProducts.count { it.quantityAvailable <= 5L }
  val activeOrders = orders.filter { !OrderStatus.fromKey(it.status).isTerminal }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(BgPrimaryCanvas)
      .statusBarsPadding(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Surface(modifier = Modifier.fillMaxWidth(), color = BgSurfaceCard) {
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
            IconButton(onClick = onOpenDrawer, modifier = Modifier.testTag("provider_menu_button")) {
              Icon(Icons.Filled.Menu, contentDescription = "Open Portal Menu", tint = TextPrimary)
            }
            TradeLinkLogoBadge(modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text("Provider Portal", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
              Text("Seller Inventory & Dispatch", style = MaterialTheme.typography.labelMedium, color = BrandPrimary)
            }
          }
          Button(
            onClick = { showAddForm = !showAddForm },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
            modifier = Modifier.testTag("toggle_add_product_button"),
          ) {
            Text(if (showAddForm) "Close Form" else "+ Add Product")
          }
        }
        HorizontalDivider(color = BorderHairline)
      }
    }

    LazyColumn(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 620.dp)
        .weight(1f),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // KPI Row
      item {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          KpiStatCard(
            label = "Listings",
            value = sellerProducts.size.toString(),
            bg = BgSurfaceCard,
            modifier = Modifier.weight(1f),
          )
          KpiStatCard(
            label = "Low Stock",
            value = lowStockCount.toString(),
            bg = if (lowStockCount > 0) StatusWarningSoft else BgSurfaceCard,
            modifier = Modifier.weight(1f),
          )
          KpiStatCard(
            label = "Active Orders",
            value = activeOrders.size.toString(),
            bg = BrandSecondary,
            modifier = Modifier.weight(1f),
          )
        }
      }

      // Seller Bolt Transport Dispatch Action
      item {
        Button(
          onClick = { showBoltSheet = true },
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("provider_order_bolt_transport_button"),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = StatusSafeGreenDark),
        ) {
          Icon(Icons.Filled.TwoWheeler, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("ORDER BOLT TRANSPORT (SELLER CARGO / RESTOCK)")
        }
      }

      if (showAddForm) {
        item {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = BgSurfaceCard,
            border = BorderStroke(1.2.dp, BrandPrimary),
          ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Text("List New Product on Trade Link", style = MaterialTheme.typography.titleLarge, color = TextPrimary)

              // 8 Mandatory Categories Selector
              Text("Category (8 Core Channels)", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                ProductCategory.entries.forEach { cat ->
                  FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat.displayName) },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = BrandPrimary,
                      selectedLabelColor = Color.White,
                    ),
                  )
                }
              }

              OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Product Title *") },
                singleLine = true,
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("add_product_title"),
              )

              OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Product Description") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
              )

              // Zero-permission Photo Picker
              Surface(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    photoPickerLauncher.launch(
                      PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                  },
                shape = RoundedCornerShape(12.dp),
                color = BgPrimaryCanvas,
                border = BorderStroke(1.dp, BorderHairline),
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  if (pickedPhotoUri != null) {
                    AsyncImage(
                      model = pickedPhotoUri,
                      contentDescription = "Selected product photo",
                      contentScale = ContentScale.Crop,
                      modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    )
                    Text("Photo attached via Android Photo Picker", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                  } else {
                    Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = BrandPrimary)
                    Text("Attach Product Photo (Zero-Permission Picker)", style = MaterialTheme.typography.bodyMedium, color = BrandPrimary)
                  }
                }
              }

              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                  value = priceInput,
                  onValueChange = { priceInput = it.filter { ch -> ch.isDigit() } },
                  label = { Text("Price (TSh)") },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                  singleLine = true,
                  modifier = Modifier
                    .weight(1f)
                    .testTag("add_product_price"),
                )
                OutlinedTextField(
                  value = quantityInput,
                  onValueChange = { quantityInput = it.filter { ch -> ch.isDigit() } },
                  label = { Text("Stock Qty") },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                  singleLine = true,
                  modifier = Modifier.weight(1f),
                )
              }

              // Live Market Price Benchmark Card (Revisit #1 Section 3.3)
              val benchmark = remember(sellerProducts, selectedCategory) {
                TradeLinkRepository.computeCategoryPriceBenchmark(sellerProducts, selectedCategory.key)
              }
              if (benchmark.count > 0) {
                val enteredPrice = priceInput.toDoubleOrNull() ?: 0.0
                Surface(
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(12.dp),
                  color = BgPrimaryCanvas,
                  border = BorderStroke(1.dp, BorderHairline),
                ) {
                  Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    Text(
                      text = "📊 Live Market Price Benchmark (${selectedCategory.displayName} · ${benchmark.count} listings)",
                      style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                      color = TextPrimary,
                    )
                    Text(
                      text = "Lowest: ${formatTzs(benchmark.lowestTzs)} · Avg: ${formatTzs(benchmark.averageTzs)} · Highest: ${formatTzs(benchmark.highestTzs)}",
                      style = MaterialTheme.typography.bodySmall,
                      color = TextSecondary,
                    )
                    if (enteredPrice > 0.0) {
                      val statusLabel = when {
                        enteredPrice <= benchmark.lowestTzs -> "★ Beats or matches lowest sticker in category!"
                        enteredPrice <= benchmark.averageTzs -> "✓ Competitive (below category average)"
                        else -> "Above category average — check distance advantage"
                      }
                      Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (enteredPrice <= benchmark.averageTzs) StatusSafeGreenDark else StatusWarning,
                      )
                    }
                  }
                }
              }

              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                  value = sizeSpec,
                  onValueChange = { sizeSpec = it },
                  label = { Text("Size / Dimensions") },
                  singleLine = true,
                  modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                  value = weightInput,
                  onValueChange = { weightInput = it },
                  label = { Text("Weight (kg)") },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                  singleLine = true,
                  modifier = Modifier.weight(1f),
                )
              }

              // Auto-Vehicle Classifier Banner
              Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = if (requiresTruck) AccentWarmSoft else StatusSafeGreenSoft,
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Icon(
                    imageVector = if (requiresTruck) Icons.Filled.LocalShipping else Icons.Filled.TwoWheeler,
                    contentDescription = null,
                    tint = TextPrimary,
                  )
                  Text(
                    text = if (requiresTruck) {
                      "Auto-Classifier (>35 kg): Phase 2 Truck Required"
                    } else {
                      "Auto-Classifier (<=35 kg): Motorcycle Cargo Box"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                  )
                }
              }

              OutlinedTextField(
                value = pickupLabel,
                onValueChange = { pickupLabel = it },
                label = { Text("Pickup Station / Stall") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )

              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text("Feature in \"Deals of today\"", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                Switch(
                  checked = isDailyDeal,
                  onCheckedChange = { isDailyDeal = it },
                  colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BrandPrimary),
                )
              }

              Button(
                onClick = {
                  onCreateProduct(
                    selectedCategory,
                    title,
                    description,
                    parsedPrice,
                    parsedQty.coerceAtLeast(1L),
                    sizeSpec,
                    parsedWeight.coerceAtLeast(0.1),
                    isDailyDeal,
                    pickupLabel,
                    pickedPhotoUri?.toString(),
                  ) {
                    title = ""
                    description = ""
                    pickedPhotoUri = null
                    showAddForm = false
                  }
                },
                enabled = title.isNotBlank() && parsedPrice > 0.0 && !isBusy,
                modifier = Modifier
                  .fillMaxWidth()
                  .height(52.dp)
                  .testTag("publish_product_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
              ) {
                Text("PUBLISH TO TRADE LINK")
              }
            }
          }
        }
      }

      if (activeOrders.isNotEmpty()) {
        item {
          Text("Incoming Orders to Dispatch", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        }
        items(activeOrders, key = { it.id }) { order ->
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = BgSurfaceCard,
            border = BorderStroke(1.dp, BorderHairline),
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(order.productTitle, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(formatTzs(order.totalAmountTzs), style = MaterialTheme.typography.labelLarge, color = BrandPrimary)
              }
              Text("Status: ${OrderStatus.fromKey(order.status).label}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
              if (OrderStatus.fromKey(order.status) != OrderStatus.ARRIVED_AWAITING_SECRET_CODE) {
                OutlinedButton(
                  onClick = { onAdvanceOrderStep(order) },
                  modifier = Modifier.fillMaxWidth().height(44.dp),
                ) {
                  Text("Dispatch / Advance Delivery Stage")
                }
              }
            }
          }
        }
      }

      item {
        Text("Your Marketplace Inventory", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
      }

      if (sellerProducts.isEmpty()) {
        item {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = BgSurfaceCard,
            border = BorderStroke(1.dp, BorderHairline),
          ) {
            Column(
              modifier = Modifier.padding(20.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(Icons.Filled.Inventory, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(36.dp))
              Text("No seller listings yet", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
              Text("Tap \"+ Add Product\" above to publish items in TSh.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
          }
        }
      } else {
        items(sellerProducts, key = { it.id }) { item ->
          Surface(
            modifier = Modifier.fillMaxWidth(),
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
                Text(ProductCategory.fromKey(item.category).displayName, style = MaterialTheme.typography.labelMedium, color = BrandPrimary)
                Text(item.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(
                  text = "Qty: ${item.quantityAvailable} · ${item.sizeSpec} · ${if (item.requiresTruck) "Truck" else "Moto"}",
                  style = MaterialTheme.typography.bodyMedium,
                  color = if (item.quantityAvailable <= 5L) StatusWarning else TextSecondary,
                )
              }
              Text(formatTzs(item.priceTzs), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
            }
          }
        }
      }
    }

    if (showBoltSheet) {
      val boltSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
      ModalBottomSheet(
        onDismissRequest = { showBoltSheet = false },
        sheetState = boltSheetState,
        containerColor = BgSurfaceCard,
      ) {
        BoltTransportOrderSheet(
          defaultRoleIsSeller = true,
          isBusy = isBusy,
          onDispatchTransport = { tier, isSellerMode, pickupHub, dropoff, distKm, weightKg, method ->
            showBoltSheet = false
            onDispatchBoltTransport(tier, isSellerMode, pickupHub, dropoff, distKm, weightKg, method)
          },
        )
      }
    }
  }
}

@Composable
private fun KpiStatCard(
  label: String,
  value: String,
  bg: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(14.dp),
    color = bg,
    border = BorderStroke(1.dp, BorderHairline),
  ) {
    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Text(value, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
      Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
    }
  }
}
