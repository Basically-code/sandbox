package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
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
import com.example.ui.theme.DisplayCodeStyle
import com.example.ui.theme.MonoTagStyle
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusDangerSoft
import com.example.ui.theme.StatusSafeGreen
import com.example.ui.theme.StatusSafeGreenSoft
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.atan2

@Composable
fun LiveMapAndOrdersScreen(
  orders: List<Order>,
  startOnMap: Boolean,
  isBusy: Boolean,
  onBack: (() -> Unit)? = null,
  onAdvanceOrderStep: (Order) -> Unit,
  onConfirmSafeReceipt: (Order) -> Unit,
  onClaimDamagedOrLost: (Order) -> Unit,
  onClaimRobberyOrHarm: (Order) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showMapTab by remember(startOnMap) { mutableStateOf(startOnMap) }
  var showSecretHardwareGps by remember { mutableStateOf(false) }

  val activeOrder = orders.firstOrNull { !OrderStatus.fromKey(it.status).isTerminal } ?: orders.firstOrNull()

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
            .padding(horizontal = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
              IconButton(onClick = onBack, modifier = Modifier.testTag("map_orders_back_button")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
              }
            }
            Text(
              text = if (showMapTab) "Live Telemetry" else "Orders & Receipts",
              style = MaterialTheme.typography.titleLarge,
              color = TextPrimary,
            )
          }

          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(
              shape = RoundedCornerShape(999.dp),
              color = if (showMapTab) BrandPrimary else BgSurfaceSubtle,
              onClick = { showMapTab = true },
            ) {
              Text(
                text = "Live Map",
                style = MaterialTheme.typography.labelMedium,
                color = if (showMapTab) Color.White else TextPrimary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
              )
            }
            Surface(
              shape = RoundedCornerShape(999.dp),
              color = if (!showMapTab) BrandPrimary else BgSurfaceSubtle,
              onClick = { showMapTab = false },
            ) {
              Text(
                text = "Orders (${orders.size})",
                style = MaterialTheme.typography.labelMedium,
                color = if (!showMapTab) Color.White else TextPrimary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
              )
            }
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
      if (showMapTab) {
        item {
          LiveTelemetryMapCard(
            activeOrder = activeOrder,
            showSecretHardwareGps = showSecretHardwareGps,
            onToggleSecretGps = { showSecretHardwareGps = it },
          )
        }

        if (activeOrder != null) {
          item {
            GateVerificationAndDriverCard(
              order = activeOrder,
              isBusy = isBusy,
              onAdvanceStep = { onAdvanceOrderStep(activeOrder) },
              onConfirmSafeReceipt = { onConfirmSafeReceipt(activeOrder) },
              onClaimDamagedOrLost = { onClaimDamagedOrLost(activeOrder) },
              onClaimRobberyOrHarm = { onClaimRobberyOrHarm(activeOrder) },
            )
          }
        } else {
          item {
            EmptyOrdersNotice()
          }
        }
      } else {
        if (orders.isEmpty()) {
          item { EmptyOrdersNotice() }
        } else {
          items(orders, key = { it.id }) { order ->
            OrderReceiptSummaryCard(
              order = order,
              isBusy = isBusy,
              onTrackOnMap = { showMapTab = true },
              onAdvanceStep = { onAdvanceOrderStep(order) },
              onConfirmSafeReceipt = { onConfirmSafeReceipt(order) },
              onClaimDamagedOrLost = { onClaimDamagedOrLost(order) },
              onClaimRobberyOrHarm = { onClaimRobberyOrHarm(order) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun LiveTelemetryMapCard(
  activeOrder: Order?,
  showSecretHardwareGps: Boolean,
  onToggleSecretGps: (Boolean) -> Unit,
) {
  val status = OrderStatus.fromKey(activeOrder?.status)
  val targetProgress = when (status) {
    OrderStatus.PENDING_DISPATCH -> 0.08f
    OrderStatus.DRIVER_ASSIGNED -> 0.25f
    OrderStatus.BOX_ATTACHED_PICKED_UP -> 0.52f
    OrderStatus.IN_TRANSIT -> 0.78f
    OrderStatus.ARRIVED_AWAITING_SECRET_CODE,
    OrderStatus.DELIVERED_COMPLETED,
    -> 1.0f
    else -> 0.45f
  }
  val animatedProgress by animateFloatAsState(
    targetValue = targetProgress,
    animationSpec = tween(durationMillis = 1100, easing = LinearEasing),
    label = "kalman_spline_progress",
  )

  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    color = BgSurfaceCard,
    border = BorderStroke(1.2.dp, BorderHairline),
    shadowElevation = 3.dp,
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text("Dar es Salaam Live Courier Grid", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
          Text(
            text = if (activeOrder != null) status.label else "Kariakoo Block A · Fleet Standby",
            style = MaterialTheme.typography.labelMedium,
            color = BrandPrimary,
          )
        }
        Surface(shape = RoundedCornerShape(999.dp), color = StatusSafeGreenSoft) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(Icons.Filled.GpsFixed, contentDescription = null, tint = StatusSafeGreen, modifier = Modifier.size(14.dp))
            Text("60 FPS Kalman", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
          }
        }
      }

      // Vector Map Canvas with Hermite-style road polyline and bearing-rotated sprite
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(220.dp)
          .background(Color(0xFFF1F5F9), RoundedCornerShape(14.dp)),
      ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val w = size.width
          val h = size.height

          // Subtle daylight road grid
          val gridColor = Color(0xFFE2E8F0)
          for (i in 1..6) {
            drawLine(gridColor, Offset(0f, h * i / 7f), Offset(w, h * i / 7f), strokeWidth = 2f)
            drawLine(gridColor, Offset(w * i / 7f, 0f), Offset(w * i / 7f, h), strokeWidth = 2f)
          }

          // Main Dar arterial road (Uhuru / Samora / Ali Hassan Mwinyi curve)
          val p0 = Offset(w * 0.14f, h * 0.80f)
          val p1 = Offset(w * 0.38f, h * 0.58f)
          val p2 = Offset(w * 0.64f, h * 0.42f)
          val p3 = Offset(w * 0.85f, h * 0.22f)

          val routePath = Path().apply {
            moveTo(p0.x, p0.y)
            lineTo(p1.x, p1.y)
            lineTo(p2.x, p2.y)
            lineTo(p3.x, p3.y)
          }

          drawPath(
            path = routePath,
            color = BrandSecondary,
            style = Stroke(width = 16f, cap = StrokeCap.Round),
          )
          drawPath(
            path = routePath,
            color = BrandPrimary,
            style = Stroke(
              width = 5f,
              cap = StrokeCap.Round,
              pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)),
            ),
          )

          // Pickup Station Pin (Kariakoo Market Block A)
          drawCircle(color = AccentWarm, radius = 14f, center = p0)
          drawCircle(color = Color.White, radius = 6f, center = p0)

          // Buyer Gate Destination Pin
          drawCircle(color = StatusSafeGreen, radius = 16f, center = p3)
          drawCircle(color = Color.White, radius = 7f, center = p3)

          // Interpolated vehicle position & shortest-arc bearing
          val points = listOf(p0, p1, p2, p3)
          val scaled = animatedProgress * (points.size - 1)
          val seg = scaled.toInt().coerceIn(0, points.size - 2)
          val localT = (scaled - seg).coerceIn(0f, 1f)
          val startPt = points[seg]
          val endPt = points[seg + 1]
          val vehiclePos = Offset(
            x = startPt.x + (endPt.x - startPt.x) * localT,
            y = startPt.y + (endPt.y - startPt.y) * localT,
          )
          val bearingDeg = Math.toDegrees(
            atan2((endPt.y - startPt.y).toDouble(), (endPt.x - startPt.x).toDouble())
          ).toFloat()

          // Covert Hardware GPS dot (offset slightly to visualize dual-tracker parity)
          if (showSecretHardwareGps) {
            drawCircle(
              color = StatusDanger.copy(alpha = 0.25f),
              radius = 28f,
              center = vehiclePos,
            )
            drawCircle(
              color = StatusDanger,
              radius = 8f,
              center = Offset(vehiclePos.x - 10f, vehiclePos.y + 8f),
            )
          }

          // Bearing-rotated motorcycle/truck badge
          drawCircle(color = Color(0xFF0284C7).copy(alpha = 0.22f), radius = 26f, center = vehiclePos)
          rotate(degrees = bearingDeg, pivot = vehiclePos) {
            drawCircle(color = TextPrimary, radius = 16f, center = vehiclePos)
            drawLine(
              color = AccentWarm,
              start = vehiclePos,
              end = Offset(vehiclePos.x + 16f, vehiclePos.y),
              strokeWidth = 5f,
              cap = StrokeCap.Round,
            )
          }
        }

        // Map Legend Overlay
        Row(
          modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(10.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Surface(shape = RoundedCornerShape(8.dp), color = BgSurfaceCard.copy(alpha = 0.92f)) {
            Text(
              text = "Station: Kariakoo A  →  Buyer Gate",
              style = MaterialTheme.typography.labelMedium,
              color = TextPrimary,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Covert Hardware GPS Overlay", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
          Text("Anti-theft tracker IMEI-8642091100482", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        }
        Switch(
          checked = showSecretHardwareGps,
          onCheckedChange = onToggleSecretGps,
          colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = StatusDanger),
        )
      }
    }
  }
}

@Composable
private fun GateVerificationAndDriverCard(
  order: Order,
  isBusy: Boolean,
  onAdvanceStep: () -> Unit,
  onConfirmSafeReceipt: () -> Unit,
  onClaimDamagedOrLost: () -> Unit,
  onClaimRobberyOrHarm: () -> Unit,
) {
  val status = OrderStatus.fromKey(order.status)

  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    // Verbal Secret Safety Code Card
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(20.dp),
      color = BgSurfaceCard,
      border = BorderStroke(1.5.dp, BrandPrimary),
    ) {
      Column(
        modifier = Modifier.padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Icon(Icons.Filled.Security, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(18.dp))
          Text("BUYER VERBAL SAFETY CODE", style = MaterialTheme.typography.labelLarge, color = BrandPrimary)
        }
        Text(
          text = order.secretSafetyCode,
          style = DisplayCodeStyle,
          color = TextPrimary,
          modifier = Modifier.testTag("secret_safety_code_text"),
        )
        Text(
          text = "Keep gate closed until the driver pronounces this exact code aloud and matches their face & plate below.",
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary,
        )
      }
    }

    // Verified Driver Identity & Plate Card
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(18.dp),
      color = BgSurfaceCard,
      border = BorderStroke(1.dp, BorderHairline),
    ) {
      Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = StatusSafeGreenSoft) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = if (order.requiresTruck) Icons.Filled.LocalShipping else Icons.Filled.TwoWheeler,
                contentDescription = null,
                tint = StatusSafeGreen,
              )
            }
          }
          Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(order.driverName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
              Icon(Icons.Filled.VerifiedUser, contentDescription = "Verified Driver", tint = StatusSafeGreen, modifier = Modifier.size(16.dp))
            }
            Text(
              text = "Plate: ${order.driverPlate} · ${if (order.cargoBoxAttached) "Cargo Box Mounted" else "Box Detached"}",
              style = MonoTagStyle,
              color = TextSecondary,
            )
          }
        }
      }
    }

    // Delivery Simulation Stepper + Handover & Refund Controls
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(18.dp),
      color = BgSurfaceCard,
      border = BorderStroke(1.dp, BorderHairline),
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Order ${order.receiptNumber}", style = MonoTagStyle, color = TextSecondary)
          Text(formatTzs(order.totalAmountTzs), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
        }
        Text(
          text = "${order.productTitle} (×${order.quantity}) · ${PaymentMethod.fromKey(order.paymentMethod).label}",
          style = MaterialTheme.typography.bodyMedium,
          color = TextPrimary,
        )

        if (!status.isTerminal) {
          if (status != OrderStatus.ARRIVED_AWAITING_SECRET_CODE) {
            OutlinedButton(
              onClick = onAdvanceStep,
              enabled = !isBusy,
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("advance_delivery_step_button"),
              shape = RoundedCornerShape(12.dp),
            ) {
              Text("Simulate Courier Progress → Next Stage")
            }
          }

          Button(
            onClick = onConfirmSafeReceipt,
            enabled = !isBusy,
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("confirm_safe_receipt_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = StatusSafeGreen),
          ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Confirm Secret Code & Safe Receipt")
          }

          HorizontalDivider(color = BorderHairline)
          Text("Buyer Protection Guarantee (Instant Claims)", style = MaterialTheme.typography.labelLarge, color = TextPrimary)

          OutlinedButton(
            onClick = onClaimDamagedOrLost,
            enabled = !isBusy,
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("claim_damaged_lost_button"),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, StatusWarning),
          ) {
            Icon(Icons.Filled.ReportProblem, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Damaged / Lost: 100% Full Refund", color = StatusWarning)
          }

          OutlinedButton(
            onClick = onClaimRobberyOrHarm,
            enabled = !isBusy,
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("claim_robbery_harm_button"),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, StatusDanger),
          ) {
            Icon(Icons.Filled.Shield, contentDescription = null, tint = StatusDanger, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Robbery / Harm: 100% Refund + Lock Route", color = StatusDanger)
          }
        } else {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = if (status == OrderStatus.DELIVERED_COMPLETED) StatusSafeGreenSoft else StatusDangerSoft,
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(status.label, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
              if (order.refundAmountTzs > 0.0) {
                Text(
                  text = "Total Refund Issued: ${formatTzs(order.refundAmountTzs)} (100% full refund)",
                  style = MaterialTheme.typography.bodyMedium,
                  color = TextPrimary,
                )
              } else {
                Text(
                  text = "Digital Receipt ${order.receiptNumber} sealed on Trade Link ledger.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = TextSecondary,
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
private fun OrderReceiptSummaryCard(
  order: Order,
  isBusy: Boolean,
  onTrackOnMap: () -> Unit,
  onAdvanceStep: () -> Unit,
  onConfirmSafeReceipt: () -> Unit,
  onClaimDamagedOrLost: () -> Unit,
  onClaimRobberyOrHarm: () -> Unit,
) {
  val status = OrderStatus.fromKey(order.status)
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(18.dp),
    color = BgSurfaceCard,
    border = BorderStroke(1.dp, BorderHairline),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(18.dp))
          Text(order.receiptNumber, style = MonoTagStyle, color = TextPrimary)
        }
        Surface(
          shape = RoundedCornerShape(999.dp),
          color = if (status == OrderStatus.DELIVERED_COMPLETED) StatusSafeGreenSoft else BrandSecondary,
        ) {
          Text(
            text = status.label,
            style = MaterialTheme.typography.labelMedium,
            color = BrandPrimaryDark,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      }

      Text("${order.productTitle} (×${order.quantity})", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
      Text("Safety Code: ${order.secretSafetyCode} · Total: ${formatTzs(order.totalAmountTzs)}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)

      if (!status.isTerminal) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(onClick = onTrackOnMap, modifier = Modifier.weight(1f).height(44.dp)) {
            Text("Track Map")
          }
          Button(
            onClick = onConfirmSafeReceipt,
            enabled = !isBusy,
            modifier = Modifier.weight(1f).height(44.dp),
            colors = ButtonDefaults.buttonColors(containerColor = StatusSafeGreen),
          ) {
            Text("Confirm Safe")
          }
        }
      }
    }
  }
}

@Composable
private fun EmptyOrdersNotice() {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(18.dp),
    color = BgSurfaceCard,
    border = BorderStroke(1.dp, BorderHairline),
  ) {
    Column(
      modifier = Modifier.padding(22.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(Icons.Filled.TwoWheeler, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(40.dp))
      Text("No Active Deliveries", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
      Text(
        text = "Browse any of the 8 categories on the Home screen to place an order and receive your Verbal Secret Safety Code + Live Courier GPS.",
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary,
      )
    }
  }
}
