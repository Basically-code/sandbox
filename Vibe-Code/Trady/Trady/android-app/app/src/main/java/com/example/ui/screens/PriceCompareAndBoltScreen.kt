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
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.ProductComparisonGroup
import com.example.data.model.SellerOfferComparison
import com.example.data.model.TransportTier
import com.example.data.repository.TradeLinkRepository
import com.example.ui.components.formatTzs
import com.example.ui.theme.AccentWarmSoft
import com.example.ui.theme.BgPrimaryCanvas
import com.example.ui.theme.BgSurfaceCard
import com.example.ui.theme.BgSurfaceSubtle
import com.example.ui.theme.BoltTransportGradient
import com.example.ui.theme.BorderHairline
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandPrimaryDark
import com.example.ui.theme.BrandSecondary
import com.example.ui.theme.HeroOceanGradient
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSafeGreen
import com.example.ui.theme.StatusSafeGreenDark
import com.example.ui.theme.StatusSafeGreenSoft
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun PriceComparisonSheet(
  allProducts: List<Product>,
  focusedProduct: Product?,
  isBusy: Boolean,
  onSelectOfferToBuy: (Product, Double) -> Unit,
  onAddOfferToCart: (Product) -> Unit,
  modifier: Modifier = Modifier,
) {
  var buyerDistanceKm by remember { mutableDoubleStateOf(4.0) }

  val groups: List<ProductComparisonGroup> = remember(allProducts, focusedProduct, buyerDistanceKm) {
    val built = TradeLinkRepository.buildPriceComparisonGroups(allProducts, buyerDistanceKm)
    if (focusedProduct != null) {
      val targetKey = TradeLinkRepository.normalizeComparisonKey(focusedProduct.title)
      built.sortedByDescending { it.groupKey == targetKey }
    } else {
      built
    }
  }

  LazyColumn(
    modifier = modifier
      .fillMaxWidth()
      .heightIn(max = 680.dp)
      .testTag("price_comparison_sheet"),
    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 28.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item(key = "compare_hero_banner") {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(18.dp))
          .background(HeroOceanGradient)
          .padding(16.dp),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.CompareArrows,
              contentDescription = null,
              tint = Color.White,
            )
            Text(
              text = "Tradee Multi-Seller Price Comparison",
              style = MaterialTheme.typography.titleLarge,
              color = Color.White,
            )
          }
          Text(
            text = "Compare Sticker Prices + Bolt Transport Fees across Dar es Salaam market hubs to find the true lowest landed price.",
            style = MaterialTheme.typography.bodyMedium,
            color = BrandSecondary,
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Your Delivery Distance: ${buyerDistanceKm.roundToInt()} km",
              style = MaterialTheme.typography.labelLarge,
              color = Color.White,
            )
            Text(
              text = "${groups.size} Product Families",
              style = MaterialTheme.typography.labelMedium,
              color = AccentWarmSoft,
            )
          }
          Slider(
            value = buyerDistanceKm.toFloat(),
            onValueChange = { buyerDistanceKm = it.toDouble() },
            valueRange = 1f..20f,
            steps = 18,
            colors = SliderDefaults.colors(
              thumbColor = Color.White,
              activeTrackColor = BrandSecondary,
            ),
            modifier = Modifier.testTag("price_compare_distance_slider"),
          )
        }
      }
    }

    if (groups.isEmpty()) {
      item(key = "compare_empty") {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          color = BgSurfaceSubtle,
          border = BorderStroke(1.dp, BorderHairline),
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = "No Active Listings to Compare Yet",
              style = MaterialTheme.typography.titleMedium,
              color = TextPrimary,
            )
            Text(
              text = "Seed the sample TSh catalog or add products from the Seller Portal to compare competing offers side by side.",
              style = MaterialTheme.typography.bodyMedium,
              color = TextSecondary,
            )
          }
        }
      }
    } else {
      items(groups, key = { it.groupKey }) { group ->
        ComparisonGroupCard(
          group = group,
          isBusy = isBusy,
          onBuyOffer = { offer -> onSelectOfferToBuy(offer.product, offer.effectiveDistanceKm) },
          onAddCartOffer = { offer -> onAddOfferToCart(offer.product) },
        )
      }
    }
  }
}

@Composable
private fun ComparisonGroupCard(
  group: ProductComparisonGroup,
  isBusy: Boolean,
  onBuyOffer: (SellerOfferComparison) -> Unit,
  onAddCartOffer: (SellerOfferComparison) -> Unit,
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("compare_group_${group.groupKey}"),
    shape = RoundedCornerShape(18.dp),
    color = BgSurfaceCard,
    border = BorderStroke(1.2.dp, BorderHairline),
    shadowElevation = 2.dp,
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = ProductCategory.fromKey(group.category).displayName.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = BrandPrimary,
          )
          Text(
            text = group.title,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
          )
        }
        Surface(
          shape = RoundedCornerShape(999.dp),
          color = if (group.sellerCount > 1) AccentWarmSoft else BrandSecondary,
        ) {
          Text(
            text = "${group.sellerCount} Seller(s)",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          )
        }
      }

      if (group.priceSpreadTzs > 0.0) {
        Text(
          text = "Sticker Range: ${formatTzs(group.lowestStickerTzs)} – ${formatTzs(group.highestStickerTzs)} (Spread: ${formatTzs(group.priceSpreadTzs)})",
          style = MaterialTheme.typography.labelMedium,
          color = StatusSafeGreenDark,
        )
      }

      HorizontalDivider(color = BorderHairline)

      group.offers.forEach { offer ->
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          color = if (offer.isBestLandedValue) StatusSafeGreenSoft.copy(alpha = 0.5f) else BgPrimaryCanvas,
          border = BorderStroke(
            width = if (offer.isBestLandedValue) 1.5.dp else 1.dp,
            color = if (offer.isBestLandedValue) StatusSafeGreen else BorderHairline,
          ),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f),
              ) {
                Icon(
                  imageVector = Icons.Filled.Verified,
                  contentDescription = null,
                  tint = if (offer.isBestLandedValue) StatusSafeGreenDark else BrandPrimary,
                  modifier = Modifier.size(16.dp),
                )
                Text(
                  text = "#${offer.rank} ${offer.product.providerName.ifBlank { "Verified Seller" }}",
                  style = MaterialTheme.typography.titleMedium,
                  color = TextPrimary,
                )
              }

              if (offer.isBestLandedValue) {
                Surface(
                  shape = RoundedCornerShape(999.dp),
                  color = StatusSafeGreen,
                ) {
                  Text(
                    text = "★ BEST TOTAL VALUE",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                  )
                }
              } else if (offer.isLowestStickerPrice) {
                Surface(
                  shape = RoundedCornerShape(999.dp),
                  color = AccentWarmSoft,
                ) {
                  Text(
                    text = "Lowest Sticker",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                  )
                }
              }
            }

            Text(
              text = "Hub: ${offer.product.pickupLabel} (${offer.effectiveDistanceKm.roundToInt()} km) · ${offer.product.sizeSpec}",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary,
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column {
                Text(
                  text = "Item: ${formatTzs(offer.unitPriceTzs)} + Bolt: ${formatTzs(offer.transportFeeTzs)}",
                  style = MaterialTheme.typography.labelMedium,
                  color = TextSecondary,
                )
                Text(
                  text = "Landed Total: ${formatTzs(offer.landedTotalTzs)}",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                  color = BrandPrimaryDark,
                )
                if (offer.savingsVsHighestTzs > 0.0) {
                  Text(
                    text = "Save ${formatTzs(offer.savingsVsHighestTzs)} vs highest landed",
                    style = MaterialTheme.typography.labelSmall,
                    color = StatusSafeGreenDark,
                  )
                }
              }

              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                  onClick = { onAddCartOffer(offer) },
                  enabled = !isBusy,
                  shape = RoundedCornerShape(10.dp),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                ) {
                  Text("+ Cart")
                }
                Button(
                  onClick = { onBuyOffer(offer) },
                  enabled = !isBusy,
                  shape = RoundedCornerShape(10.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                  Text("Buy Deal")
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun BoltTransportOrderSheet(
  defaultRoleIsSeller: Boolean = false,
  isBusy: Boolean,
  onDispatchTransport: (
    tier: TransportTier,
    isSellerMode: Boolean,
    pickupHub: String,
    dropoffLabel: String,
    distanceKm: Double,
    weightKg: Double,
    paymentMethod: PaymentMethod,
  ) -> Unit,
  modifier: Modifier = Modifier,
) {
  val hubs = remember {
    listOf(
      "Kariakoo Market Block A",
      "Samora Tech Hub",
      "Mlimani City Retail Plaza",
      "Nyerere Rd Warehouse",
      "Mwenge Crafts Arcade",
      "Slipway Artisan Guild",
    )
  }

  var isSellerMode by remember { mutableStateOf(defaultRoleIsSeller) }
  var selectedPickupHub by remember { mutableStateOf(hubs.first()) }
  var dropoffLabel by remember { mutableStateOf("Mikocheni B Gate #14, Dar es Salaam") }
  var distanceKm by remember { mutableDoubleStateOf(4.5) }
  var weightInput by remember { mutableStateOf(if (defaultRoleIsSeller) "25.0" else "5.0") }
  var selectedTier by remember { mutableStateOf(TransportTier.TRADEE_MOTO_BOX) }
  var paymentMethod by remember { mutableStateOf(PaymentMethod.CASH_BY_HAND_WITH_RECEIPT) }

  val parsedWeightKg = (weightInput.toDoubleOrNull() ?: 5.0).coerceAtLeast(0.5)
  val tierQuotes = remember(distanceKm, parsedWeightKg) {
    TradeLinkRepository.compareAllBoltTiers(distanceKm, parsedWeightKg)
  }
  val activeQuote = tierQuotes.firstOrNull { it.tier == selectedTier && it.isEligible }
    ?: tierQuotes.firstOrNull { it.isRecommended }
    ?: tierQuotes.last()

  LazyColumn(
    modifier = modifier
      .fillMaxWidth()
      .heightIn(max = 680.dp)
      .testTag("bolt_transport_sheet"),
    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 28.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item(key = "bolt_hero_header") {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(18.dp))
          .background(BoltTransportGradient)
          .padding(16.dp),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.TwoWheeler,
              contentDescription = null,
              tint = Color.White,
            )
            Text(
              text = "Tradee Bolt Transport & Cargo Dispatch",
              style = MaterialTheme.typography.titleLarge,
              color = Color.White,
            )
          }
          Text(
            text = "Order instant on-demand rides, motorcycle cargo boxes, Bajaji 3-wheelers, or commercial trucks — built for both Buyers and Sellers.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.92f),
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
              selected = !isSellerMode,
              onClick = { isSellerMode = false },
              label = { Text("Buyer Ride / Parcel Pickup") },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color.White,
                selectedLabelColor = BrandPrimaryDark,
                labelColor = Color.White,
              ),
              modifier = Modifier.testTag("bolt_mode_buyer_chip"),
            )
            FilterChip(
              selected = isSellerMode,
              onClick = { isSellerMode = true },
              label = { Text("Seller Inventory / Restock") },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color.White,
                selectedLabelColor = BrandPrimaryDark,
                labelColor = Color.White,
              ),
              modifier = Modifier.testTag("bolt_mode_seller_chip"),
            )
          }
        }
      }
    }

    item(key = "bolt_route_inputs") {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = BgSurfaceCard,
        border = BorderStroke(1.dp, BorderHairline),
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = if (isSellerMode) "1. Seller Pickup Hub & Destination" else "1. Buyer Pickup & Gate Pin",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            hubs.take(3).forEach { hub ->
              FilterChip(
                selected = selectedPickupHub == hub,
                onClick = { selectedPickupHub = hub },
                label = { Text(hub.substringBefore(" ")) },
                modifier = Modifier.weight(1f),
              )
            }
          }

          OutlinedTextField(
            value = dropoffLabel,
            onValueChange = { dropoffLabel = it },
            label = { Text("Dropoff Destination / Buyer Gate") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null, tint = BrandPrimary) },
            modifier = Modifier.fillMaxWidth(),
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Trip Distance: ${distanceKm.roundToInt()} km",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
              color = TextPrimary,
            )
            Text(
              text = "Load Weight: ${parsedWeightKg.roundToInt()} kg",
              style = MaterialTheme.typography.labelLarge,
              color = BrandPrimary,
            )
          }

          Slider(
            value = distanceKm.toFloat(),
            onValueChange = { distanceKm = it.toDouble() },
            valueRange = 1f..25f,
            steps = 23,
            colors = SliderDefaults.colors(thumbColor = BrandPrimary, activeTrackColor = BrandPrimary),
            modifier = Modifier.testTag("bolt_distance_slider"),
          )

          OutlinedTextField(
            value = weightInput,
            onValueChange = { weightInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
            label = { Text("Estimated Load / Cargo Weight (kg)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    }

    item(key = "bolt_tiers_header") {
      Text(
        text = "2. Compare Tradee Vehicle Tiers (Live TSh Fares)",
        style = MaterialTheme.typography.titleMedium,
        color = TextPrimary,
      )
    }

    items(tierQuotes, key = { it.tier.key }) { quote ->
      val isSelected = activeQuote.tier == quote.tier
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .clickable(enabled = quote.isEligible) { selectedTier = quote.tier }
          .testTag("bolt_tier_${quote.tier.key}"),
        shape = RoundedCornerShape(16.dp),
        color = when {
          !quote.isEligible -> BgSurfaceSubtle.copy(alpha = 0.55f)
          isSelected -> StatusSafeGreenSoft.copy(alpha = 0.55f)
          else -> BgSurfaceCard
        },
        border = BorderStroke(
          width = if (isSelected) 1.8.dp else 1.dp,
          color = if (isSelected) StatusSafeGreen else BorderHairline,
        ),
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
          ) {
            Icon(
              imageVector = if (quote.tier.requiresTruck) Icons.Filled.LocalShipping else Icons.Filled.TwoWheeler,
              contentDescription = null,
              tint = if (quote.isEligible) BrandPrimary else TextSecondary,
              modifier = Modifier.size(28.dp),
            )
            Column {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Text(
                  text = quote.tier.displayName,
                  style = MaterialTheme.typography.titleMedium,
                  color = TextPrimary,
                )
                if (quote.isRecommended) {
                  Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = StatusSafeGreen,
                  ) {
                    Text(
                      text = "★ Best Fit",
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                      color = Color.White,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                  }
                }
              }
              Text(
                text = quote.tier.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
              )
              if (quote.ineligibleReason != null) {
                Text(
                  text = quote.ineligibleReason,
                  style = MaterialTheme.typography.labelSmall,
                  color = StatusDanger,
                )
              } else {
                Text(
                  text = "ETA ~${quote.estimatedMinutes} mins · Max ${quote.tier.maxWeightKg.toInt()} kg",
                  style = MaterialTheme.typography.labelSmall,
                  color = BrandPrimaryDark,
                )
              }
            }
          }

          Column(horizontalAlignment = Alignment.End) {
            Text(
              text = formatTzs(quote.fareTzs),
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
              color = TextPrimary,
            )
            Text(
              text = "${quote.distanceKm.roundToInt()} km",
              style = MaterialTheme.typography.labelSmall,
              color = TextSecondary,
            )
          }
        }
      }
    }

    item(key = "bolt_dispatch_cta") {
      Button(
        onClick = {
          onDispatchTransport(
            activeQuote.tier,
            isSellerMode,
            selectedPickupHub,
            dropoffLabel,
            distanceKm,
            parsedWeightKg,
            paymentMethod,
          )
        },
        enabled = !isBusy,
        modifier = Modifier
          .fillMaxWidth()
          .height(54.dp)
          .testTag("bolt_dispatch_confirm_button"),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = StatusSafeGreenDark),
      ) {
        Icon(Icons.Filled.TwoWheeler, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "DISPATCH ${activeQuote.tier.displayName.uppercase()} · ${formatTzs(activeQuote.fareTzs)}",
          style = MaterialTheme.typography.labelLarge,
        )
      }
    }
  }
}
