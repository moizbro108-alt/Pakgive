package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.catalog.RegionalNameCatalog
import com.example.data.model.CoinOrder
import com.example.data.model.CoinPackage
import com.example.ui.viewmodel.PakGivesViewModel

@Composable
fun BuyCoinsScreen(
  viewModel: PakGivesViewModel,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val clipboard = LocalClipboardManager.current
  val appConfig by viewModel.appConfig.collectAsStateWithLifecycle()
  val myOrders by viewModel.myCoinOrders.collectAsStateWithLifecycle()
  val profile by viewModel.userProfile.collectAsStateWithLifecycle()
  val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

  var selectedPackage by remember { mutableStateOf(RegionalNameCatalog.coinPackages.first()) }
  var whatsappRef by remember { mutableStateOf("") }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // Official WhatsApp Group Card
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Chat,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = stringResource(R.string.whatsapp_group_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
              Text(
                text = "Current Balance: ${profile?.coinBalance ?: 0L} Coins",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = stringResource(R.string.whatsapp_group_instructions),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
          )
          Spacer(modifier = Modifier.height(10.dp))
          Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = appConfig.whatsappAdminContact,
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = appConfig.whatsappGroupUrl,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              IconButton(
                onClick = {
                  clipboard.setText(AnnotatedString(appConfig.whatsappGroupUrl))
                  Toast.makeText(context, "WhatsApp group link copied", Toast.LENGTH_SHORT).show()
                }
              ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = stringResource(R.string.cd_copy_link))
              }
            }
          }
          Spacer(modifier = Modifier.height(12.dp))
          Button(
            onClick = {
              val url = appConfig.whatsappGroupUrl.ifBlank { "https://chat.whatsapp.com/PakGivesCoinsOfficial" }
              try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
              } catch (_: Exception) {
                clipboard.setText(AnnotatedString(url))
                Toast.makeText(context, "Link copied! Open WhatsApp to join.", Toast.LENGTH_LONG).show()
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("open_whatsapp_group_button"),
          ) {
            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.join_whatsapp_group_action))
          }
        }
      }
    }

    // Coin Packages
    item {
      Text(
        text = stringResource(R.string.select_coin_package),
        style = MaterialTheme.typography.titleMedium,
      )
    }

    items(RegionalNameCatalog.coinPackages, key = { it.id }) { pkg ->
      val isSelected = selectedPackage.id == pkg.id
      CoinPackageCard(
        pkg = pkg,
        isSelected = isSelected,
        onClick = { selectedPackage = pkg },
      )
    }

    // Submit Order Reference Card
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = "Request ${selectedPackage.coins} Coins (${selectedPackage.title})",
            style = MaterialTheme.typography.titleMedium,
          )
          Text(
            text = stringResource(R.string.submit_order_helper),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          OutlinedTextField(
            value = whatsappRef,
            onValueChange = { whatsappRef = it },
            label = { Text(stringResource(R.string.whatsapp_ref_label)) },
            placeholder = { Text(stringResource(R.string.whatsapp_ref_hint)) },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("whatsapp_ref_input"),
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            OutlinedButton(
              onClick = {
                val msg = Uri.encode(
                  "Assalam-o-Alaikum PakGives! I want to buy ${selectedPackage.title} (${selectedPackage.coins} Giveaway Coins - ${selectedPackage.priceDisplay}). My PakGives ID: ${viewModel.currentUserId}"
                )
                val waUrl = "https://wa.me/?text=$msg"
                try {
                  context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(waUrl)))
                } catch (_: Exception) {
                  clipboard.setText(AnnotatedString(viewModel.currentUserId))
                  Toast.makeText(context, "User ID copied for WhatsApp chat!", Toast.LENGTH_SHORT).show()
                }
              },
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .weight(1f)
                .height(48.dp),
            ) {
              Text(stringResource(R.string.send_wa_message_action))
            }
            Button(
              onClick = {
                viewModel.submitCoinOrder(selectedPackage, whatsappRef)
                whatsappRef = ""
              },
              enabled = !isBusy && whatsappRef.isNotBlank(),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("submit_coin_order_button"),
            ) {
              Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(stringResource(R.string.submit_order_action))
            }
          }
        }
      }
    }

    // Order History
    if (myOrders.isNotEmpty()) {
      item {
        Text(
          text = stringResource(R.string.my_coin_orders_title),
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.padding(top = 6.dp),
        )
      }
      items(myOrders, key = { it.id }) { order ->
        CoinOrderRow(order = order)
      }
    }

    item { Spacer(modifier = Modifier.height(24.dp)) }
  }
}

@Composable
private fun CoinPackageCard(
  pkg: CoinPackage,
  isSelected: Boolean,
  onClick: () -> Unit,
) {
  Card(
    colors = CardDefaults.cardColors(
      containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
    ),
    border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.secondary) else null,
    shape = RoundedCornerShape(18.dp),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .testTag("coin_package_${pkg.id}"),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Filled.MonetizationOn,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.secondary,
          modifier = Modifier.size(32.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(pkg.title, style = MaterialTheme.typography.titleMedium)
            if (pkg.badge.isNotBlank()) {
              Spacer(modifier = Modifier.width(8.dp))
              OriginPill(text = pkg.badge, isAccent = true)
            }
          }
          Text(
            text = "${pkg.coins} Giveaway Coins",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
      Text(
        text = pkg.priceDisplay,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Composable
private fun CoinOrderRow(order: CoinOrder) {
  val statusColor = when (order.status) {
    "approved" -> MaterialTheme.colorScheme.primary
    "rejected" -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.secondary
  }
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(14.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text("${order.packageName} • ${order.coinsAmount} Coins", style = MaterialTheme.typography.titleSmall)
        Text("Ref: ${order.whatsappRef}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (order.adminNote.isNotBlank()) {
          Text("Note: ${order.adminNote}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
      }
      Surface(
        color = statusColor.copy(alpha = 0.14f),
        shape = RoundedCornerShape(50),
      ) {
        Text(
          text = order.status.replaceFirstChar { it.uppercase() },
          style = MaterialTheme.typography.labelSmall,
          color = statusColor,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
      }
    }
  }
}
