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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.ui.components.TradeLinkLogoBadge
import com.example.ui.theme.BgPrimaryCanvas
import com.example.ui.theme.BgSurfaceCard
import com.example.ui.theme.BgSurfaceSubtle
import com.example.ui.theme.BorderHairline
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandSecondary
import com.example.ui.theme.MonoTagStyle
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSafeGreen
import com.example.ui.theme.StatusSafeGreenSoft
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ProfileScreen(
  profile: UserProfile,
  email: String?,
  activeOrdersCount: Int,
  sellerListingsCount: Int,
  onSwitchRole: (UserRole) -> Unit,
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val activeRole = UserRole.fromKey(profile.activeRole)

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
            .padding(horizontal = 16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          TradeLinkLogoBadge(modifier = Modifier.size(34.dp))
          Column {
            Text("Profile & Portal", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            Text("Account, Station & Firewall Security", style = MaterialTheme.typography.labelMedium, color = BrandPrimary)
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
      // Account Identity Card
      item {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(20.dp),
          color = BgSurfaceCard,
          border = BorderStroke(1.dp, BorderHairline),
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            Surface(modifier = Modifier.size(56.dp), shape = CircleShape, color = BrandSecondary) {
              Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(30.dp))
              }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(profile.fullName, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
              if (!email.isNullOrBlank()) {
                Text(email, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
              }
              if (profile.phoneNumber.isNotBlank()) {
                Text(profile.phoneNumber, style = MonoTagStyle, color = BrandPrimary)
              }
            }
            Surface(shape = RoundedCornerShape(999.dp), color = StatusSafeGreenSoft) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = StatusSafeGreen, modifier = Modifier.size(14.dp))
                Text("Terms OK", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
              }
            }
          }
        }
      }

      // Quick Summary Metrics
      item {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          ProfileMetricCard(
            label = "Active Orders",
            value = activeOrdersCount.toString(),
            modifier = Modifier.weight(1f),
          )
          ProfileMetricCard(
            label = "Seller Listings",
            value = sellerListingsCount.toString(),
            modifier = Modifier.weight(1f),
          )
          ProfileMetricCard(
            label = "Plate",
            value = profile.vehiclePlate.ifBlank { "MC 482" },
            modifier = Modifier.weight(1f),
          )
        }
      }

      // Portal Role Switcher
      item {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(20.dp),
          color = BgSurfaceCard,
          border = BorderStroke(1.dp, BorderHairline),
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Marketplace Mode & Portal Role", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(
              text = "Choose whether the Marketplace tab opens in Buyer Shopping mode or Provider / Seller Inventory mode.",
              style = MaterialTheme.typography.bodyMedium,
              color = TextSecondary,
            )

            RoleSelectTile(
              role = UserRole.BUYER,
              selected = activeRole == UserRole.BUYER,
              icon = Icons.Filled.ShoppingBag,
              onClick = { onSwitchRole(UserRole.BUYER) },
            )
            RoleSelectTile(
              role = UserRole.PROVIDER,
              selected = activeRole == UserRole.PROVIDER,
              icon = Icons.Filled.Storefront,
              onClick = { onSwitchRole(UserRole.PROVIDER) },
            )
            RoleSelectTile(
              role = UserRole.DRIVER,
              selected = activeRole == UserRole.DRIVER,
              icon = Icons.Filled.TwoWheeler,
              onClick = { onSwitchRole(UserRole.DRIVER) },
            )
          }
        }
      }

      // Station & Privacy Firewall Info
      item {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          color = BgSurfaceSubtle,
          border = BorderStroke(1.dp, BorderHairline),
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Filled.Lock, contentDescription = null, tint = StatusSafeGreen, modifier = Modifier.size(18.dp))
              Text("Zero-Trust Privacy & Station Binding", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }
            Text("Allocated Station: ${profile.stationName}", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Text(
              text = "Owner-isolated Firestore security rules, 100% Buyer Protection refund guarantee, and masked participant telemetry.",
              style = MaterialTheme.typography.bodyMedium,
              color = TextSecondary,
            )
          }
        }
      }

      // Sign Out
      item {
        Button(
          onClick = onSignOut,
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("profile_sign_out_button"),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = StatusDanger, contentColor = Color.White),
        ) {
          Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Sign Out", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
        }
      }
    }
  }
}

@Composable
private fun ProfileMetricCard(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(14.dp),
    color = BgSurfaceCard,
    border = BorderStroke(1.dp, BorderHairline),
  ) {
    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Text(value, style = MaterialTheme.typography.titleLarge, color = BrandPrimary, maxLines = 1)
      Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary, maxLines = 1)
    }
  }
}

@Composable
private fun RoleSelectTile(
  role: UserRole,
  selected: Boolean,
  icon: ImageVector,
  onClick: () -> Unit,
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .testTag("profile_role_${role.key}"),
    shape = RoundedCornerShape(12.dp),
    color = if (selected) BrandSecondary else BgPrimaryCanvas,
    border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) BrandPrimary else BorderHairline),
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Icon(icon, contentDescription = null, tint = if (selected) BrandPrimary else TextSecondary)
      Column(modifier = Modifier.weight(1f)) {
        Text(role.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Text(role.subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
      }
      if (selected) {
        Icon(Icons.Filled.CheckCircle, contentDescription = "Active Role", tint = BrandPrimary)
      }
    }
  }
}
