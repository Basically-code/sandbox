package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DriverContract
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.RideHailPartner
import com.example.data.model.VehicleStatus
import com.example.ui.components.TradeLinkLogoBadge
import com.example.ui.components.formatTzs
import com.example.ui.theme.AccentWarm
import com.example.ui.theme.AccentWarmSoft
import com.example.ui.theme.BgPrimaryCanvas
import com.example.ui.theme.BgSurfaceCard
import com.example.ui.theme.BgSurfaceSubtle
import com.example.ui.theme.BorderHairline
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandSecondary
import com.example.ui.theme.MonoTagStyle
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusDangerSoft
import com.example.ui.theme.StatusSafeGreen
import com.example.ui.theme.StatusSafeGreenSoft
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningSoft
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Calendar
import java.util.TimeZone

@Composable
fun DriverPortalScreen(
  contract: DriverContract?,
  orders: List<Order>,
  onOpenDrawer: () -> Unit,
  onEnsureInitialized: () -> Unit,
  onMorningCheckOut: (DriverContract) -> Unit,
  onEveningCheckIn: (DriverContract) -> Unit,
  onToggleCargoBox: (DriverContract) -> Unit,
  onSelectRideHailPartner: (DriverContract, RideHailPartner) -> Unit,
  onTriggerCurfewTest: (DriverContract) -> Unit,
  onRecordLeasePayment: (DriverContract, Int) -> Unit,
  onAdvanceOrderStep: (Order) -> Unit,
  modifier: Modifier = Modifier,
) {
  LaunchedEffect(contract == null) {
    if (contract == null) {
      onEnsureInitialized()
    }
  }

  val eatHour = Calendar.getInstance(TimeZone.getTimeZone("Africa/Dar_es_Salaam")).get(Calendar.HOUR_OF_DAY)
  val isCurfewApproaching = eatHour in 18..20
  val activeDelivery = orders.firstOrNull { !OrderStatus.fromKey(it.status).isTerminal }

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
        ) {
          IconButton(onClick = onOpenDrawer, modifier = Modifier.testTag("driver_menu_button")) {
            Icon(Icons.Filled.Menu, contentDescription = "Open Portal Menu", tint = TextPrimary)
          }
          TradeLinkLogoBadge(modifier = Modifier.size(32.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text("Fleet Driver Portal", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            Text("Dual-Mode Logistics & 2-Yr Lease", style = MaterialTheme.typography.labelMedium, color = BrandPrimary)
          }
        }
        HorizontalDivider(color = BorderHairline)
      }
    }

    if (contract == null) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        Text("Initializing Fleet Contract & Hidden GPS Binding...", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
      }
      return@Column
    }

    val vehicleStatus = VehicleStatus.fromKey(contract.vehicleStatus)
    val partner = RideHailPartner.fromKey(contract.rideHailPartner)
    val isBreach = contract.curfewViolated || vehicleStatus == VehicleStatus.CURFEW_BREACH_STOLEN_ALERT

    LazyColumn(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 620.dp)
        .weight(1f),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // 1. Curfew & Vehicle Identity Banner
      item {
        val bannerBg = when {
          isBreach -> StatusDangerSoft
          isCurfewApproaching && vehicleStatus != VehicleStatus.STATION_CHECKED_IN -> StatusWarningSoft
          vehicleStatus == VehicleStatus.STATION_CHECKED_IN -> StatusSafeGreenSoft
          else -> BrandSecondary
        }
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          color = bannerBg,
          border = BorderStroke(1.2.dp, if (isBreach) StatusDanger else BorderHairline),
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                  imageVector = if (isBreach) Icons.Filled.Warning else Icons.Filled.TwoWheeler,
                  contentDescription = null,
                  tint = if (isBreach) StatusDanger else TextPrimary,
                )
                Text(contract.driverName, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
              }
              Surface(shape = RoundedCornerShape(8.dp), color = BgSurfaceCard) {
                Text(
                  text = contract.vehiclePlate,
                  style = MonoTagStyle.copy(fontWeight = FontWeight.Bold),
                  color = TextPrimary,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
              }
            }
            Text("Status: ${vehicleStatus.label}", style = MaterialTheme.typography.titleMedium, color = if (isBreach) StatusDanger else TextPrimary)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Filled.GpsFixed, contentDescription = null, tint = if (isBreach) StatusDanger else StatusSafeGreen, modifier = Modifier.size(16.dp))
              Text(
                text = if (isBreach) {
                  "1Hz RECOVERY MODE ACTIVE · ${contract.secretGpsImei}"
                } else {
                  "Allocated Station: ${contract.stationName} · ${contract.secretGpsImei}"
                },
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
              )
            }
          }
        }
      }

      // 2. Daily Shift State Machine (05:00-07:00 Out / 18:00-21:00 In)
      item {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          color = BgSurfaceCard,
          border = BorderStroke(1.dp, BorderHairline),
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Filled.Schedule, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(18.dp))
              Text("Daily Station Shift & Curfew Gate", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }
            Text(
              text = "Morning Check-Out: 05:00-07:00 EAT · Evening Check-In: 18:00-21:00 EAT (Auto-Theft Lock at 21:00:01)",
              style = MaterialTheme.typography.bodyMedium,
              color = TextSecondary,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              Button(
                onClick = { onMorningCheckOut(contract) },
                modifier = Modifier
                  .weight(1f)
                  .height(48.dp)
                  .testTag("driver_checkout_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
              ) {
                Icon(Icons.Filled.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Check-Out")
              }
              Button(
                onClick = { onEveningCheckIn(contract) },
                modifier = Modifier
                  .weight(1f)
                  .height(48.dp)
                  .testTag("driver_checkin_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StatusSafeGreen),
              ) {
                Text("Station Check-In")
              }
            }

            if (!isBreach && vehicleStatus != VehicleStatus.STATION_CHECKED_IN) {
              OutlinedButton(
                onClick = { onTriggerCurfewTest(contract) },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                border = BorderStroke(1.dp, StatusDanger),
              ) {
                Text("Test 21:00 Curfew Breach Protocol", color = StatusDanger)
              }
            }
          }
        }
      }

      // 3. Dual-Mode Switcher (Cargo Box vs Idle Passenger Mode)
      item {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          color = BgSurfaceCard,
          border = BorderStroke(1.dp, BorderHairline),
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Dual-Mode Utilization Switcher", style = MaterialTheme.typography.titleMedium, color = TextPrimary)

            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Inventory2, contentDescription = null, tint = BrandPrimary)
                Column {
                  Text("Detachable Rear Cargo Box", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                  Text(
                    text = if (contract.cargoBoxAttached) "Mounted (Mode A: Package Delivery)" else "Detached (Mode B: Passenger Ready)",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                  )
                }
              }
              Switch(
                checked = contract.cargoBoxAttached,
                onCheckedChange = { onToggleCargoBox(contract) },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BrandPrimary),
              )
            }

            HorizontalDivider(color = BorderHairline)
            Text("Idle-Time Passenger Mode (When Queue Empty)", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              listOf(
                RideHailPartner.STREET_HAIL,
                RideHailPartner.BOLT,
                RideHailPartner.UBER,
                RideHailPartner.LINKER,
              ).forEach { option ->
                val active = partner == option
                Surface(
                  modifier = Modifier
                    .weight(1f)
                    .clickable { onSelectRideHailPartner(contract, option) },
                  shape = RoundedCornerShape(10.dp),
                  color = if (active) BrandPrimary else BgSurfaceSubtle,
                  border = BorderStroke(1.dp, if (active) BrandPrimary else BorderHairline),
                ) {
                  Text(
                    text = option.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (active) Color.White else TextPrimary,
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                  )
                }
              }
            }

            if (activeDelivery != null) {
              HorizontalDivider(color = BorderHairline)
              Text("Active Package Queue: ${activeDelivery.productTitle}", style = MaterialTheme.typography.titleMedium, color = BrandPrimary)
              Text("Speak Buyer Safety Code at Gate: ${activeDelivery.secretSafetyCode}", style = MonoTagStyle, color = TextPrimary)
              if (OrderStatus.fromKey(activeDelivery.status) != OrderStatus.ARRIVED_AWAITING_SECRET_CODE) {
                Button(
                  onClick = { onAdvanceOrderStep(activeDelivery) },
                  modifier = Modifier.fillMaxWidth().height(46.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                ) {
                  Text("Advance Package Delivery")
                }
              }
            }
          }
        }
      }

      // 4. 2-Year (104-Week) Lease-to-Own Ownership Ledger (TSh 70,000/week)
      item {
        val progress = (contract.weeksPaidCount.toFloat() / contract.totalWeeksRequired.toFloat()).coerceIn(0f, 1f)
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(20.dp),
          color = if (contract.ownershipTransferred) StatusSafeGreenSoft else BgSurfaceCard,
          border = BorderStroke(1.2.dp, if (contract.ownershipTransferred) StatusSafeGreen else BorderHairline),
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = if (contract.ownershipTransferred) StatusSafeGreen else AccentWarm,
              )
              Text("2-Year Lease-to-Own Ledger (TSh 70,000/wk)", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("${contract.weeksPaidCount} / ${contract.totalWeeksRequired} Weeks", style = MaterialTheme.typography.titleLarge, color = BrandPrimary)
              Text("${formatTzs(contract.totalPaidTzs)} / TSh 7,280,000", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
            }

            LinearProgressIndicator(
              progress = { progress },
              modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(999.dp)),
              color = if (contract.ownershipTransferred) StatusSafeGreen else BrandPrimary,
              trackColor = BgSurfaceSubtle,
            )

            if (contract.ownershipTransferred) {
              Surface(shape = RoundedCornerShape(12.dp), color = BgSurfaceCard) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Text(
                    text = "CERTIFICATE OF PERMANENT OWNERSHIP TRANSFER",
                    style = MaterialTheme.typography.labelLarge,
                    color = StatusSafeGreen,
                  )
                  Text(
                    text = "Trady Fleet hereby transfers permanent title of ${contract.vehiclePlate} to ${contract.driverName}. Welcome as an Independent Owner-Partner!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                  )
                }
              }
            } else {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                  onClick = { onRecordLeasePayment(contract, 1) },
                  modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("record_weekly_lease_button"),
                  shape = RoundedCornerShape(12.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                ) {
                  Text("Pay Week (+70k)")
                }
                OutlinedButton(
                  onClick = { onRecordLeasePayment(contract, 26) },
                  modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                  shape = RoundedCornerShape(12.dp),
                ) {
                  Text("+26 Weeks (6m)")
                }
              }
            }
          }
        }
      }
    }
  }
}
