package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.UserRole
import com.example.ui.components.TradeLinkLogoBadge
import com.example.ui.theme.BgPrimaryCanvas
import com.example.ui.theme.BgSurfaceCard
import com.example.ui.theme.BgSurfaceSubtle
import com.example.ui.theme.BorderHairline
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandSecondary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun OnboardingTermsScreen(
  defaultName: String,
  isBusy: Boolean,
  onContinue: (
    fullName: String,
    phoneNumber: String,
    role: UserRole,
    stationName: String,
    vehiclePlate: String,
  ) -> Unit,
) {
  var selectedRole by remember { mutableStateOf(UserRole.BUYER) }
  var fullName by remember { mutableStateOf(defaultName) }
  var phoneNumber by remember { mutableStateOf("+255 712 000 482") }
  var stationName by remember { mutableStateOf("Kariakoo Market Block A") }
  var vehiclePlate by remember { mutableStateOf("MC 482 DTR") }
  var termsAccepted by remember { mutableStateOf(false) }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(BgPrimaryCanvas)
      .safeDrawingPadding(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 600.dp)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        TradeLinkLogoBadge(modifier = Modifier.size(52.dp))
        Column {
          Text(
            text = "Welcome to Trady",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
          )
          Text(
            text = "Tanzania TZS Network",
            style = MaterialTheme.typography.labelMedium,
            color = BrandPrimary,
          )
        }
      }

      Text(
        text = "Select Your Role Portal:",
        style = MaterialTheme.typography.titleMedium,
        color = TextPrimary,
      )

      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        UserRole.entries.forEach { role ->
          val selected = role == selectedRole
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { selectedRole = role }
              .testTag("role_option_${role.key}"),
            shape = RoundedCornerShape(14.dp),
            color = if (selected) BrandSecondary else BgSurfaceCard,
            border = BorderStroke(
              width = if (selected) 1.6.dp else 1.dp,
              color = if (selected) BrandPrimary else BorderHairline,
            ),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              RadioButton(
                selected = selected,
                onClick = { selectedRole = role },
                colors = RadioButtonDefaults.colors(selectedColor = BrandPrimary),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Column {
                Text(
                  text = role.title,
                  style = MaterialTheme.typography.titleMedium,
                  color = TextPrimary,
                )
                Text(
                  text = role.subtitle,
                  style = MaterialTheme.typography.bodyMedium,
                  color = TextSecondary,
                )
              }
            }
          }
        }
      }

      OutlinedTextField(
        value = fullName,
        onValueChange = { fullName = it },
        label = { Text("Full Legal / Business Name") },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("onboarding_name_input"),
      )

      OutlinedTextField(
        value = phoneNumber,
        onValueChange = { phoneNumber = it },
        label = { Text("Phone Number (+255)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )

      if (selectedRole != UserRole.BUYER) {
        OutlinedTextField(
          value = stationName,
          onValueChange = { stationName = it },
          label = { Text("Allocated Station / Market Block") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }

      if (selectedRole == UserRole.DRIVER) {
        OutlinedTextField(
          value = vehiclePlate,
          onValueChange = { vehiclePlate = it },
          label = { Text("Assigned Vehicle Plate Number") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }

      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = BgSurfaceSubtle,
        border = BorderStroke(1.dp, BorderHairline),
      ) {
        Column(
          modifier = Modifier
            .padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = "TRADE LINK TERMS & CONDITIONS",
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary,
          )
          Text(
            text = "1. Buyers pay product price + transport fee via Phase 1 Cash on Delivery (with Receipt) or Phase 2 Firewall-Protected Card.\n" +
              "2. Damaged/lost goods guarantee: 100% full refund of product price and transport fee.\n" +
              "3. Drivers must pronounce the Buyer's Verbal Secret Safety Code aloud before gate entry.\n" +
              "4. Fleet curfew: Vehicles must check in at supervised stations between 18:00-21:00 EAT or trigger 1Hz Secret GPS recovery.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
          )
        }
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { termsAccepted = !termsAccepted }
          .testTag("terms_checkbox_row"),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Checkbox(
          checked = termsAccepted,
          onCheckedChange = { termsAccepted = it },
          colors = CheckboxDefaults.colors(checkedColor = BrandPrimary),
          modifier = Modifier.testTag("terms_checkbox"),
        )
        Text(
          text = "I have read and agree to all Trade Link Terms & Conditions",
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
          color = TextPrimary,
        )
      }

      Button(
        onClick = {
          onContinue(fullName, phoneNumber, selectedRole, stationName, vehiclePlate)
        },
        enabled = termsAccepted && !isBusy,
        modifier = Modifier
          .fillMaxWidth()
          .height(54.dp)
          .testTag("continue_to_trady_button"),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = BrandPrimary,
          contentColor = Color.White,
        ),
      ) {
        if (isBusy) {
          CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = Color.White,
            strokeWidth = 2.dp,
          )
        } else {
          Text(
            text = "CONTINUE TO TRADY LINK",
            style = MaterialTheme.typography.labelLarge,
          )
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}
