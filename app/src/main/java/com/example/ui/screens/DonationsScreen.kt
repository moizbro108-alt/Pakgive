package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.DonationCampaign
import com.example.data.model.PayoutProof
import com.example.ui.viewmodel.PakGivesViewModel
import com.example.ui.viewmodel.UiState

@Composable
fun DonationsScreen(
  viewModel: PakGivesViewModel,
  modifier: Modifier = Modifier,
) {
  val campaignsState by viewModel.campaignsState.collectAsStateWithLifecycle()
  val publishedProofs by viewModel.publishedProofs.collectAsStateWithLifecycle()
  val myDonations by viewModel.myDonations.collectAsStateWithLifecycle()
  val profile by viewModel.userProfile.collectAsStateWithLifecycle()
  val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

  var selectedSubTab by remember { mutableIntStateOf(0) } // 0 = Causes, 1 = Proofs & Help
  var selectedCampaignForDonate by remember { mutableStateOf<DonationCampaign?>(null) }
  val balance = profile?.coinBalance ?: 0L
  val totalDonated = profile?.totalDonatedCoins ?: 0L

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // Banner
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(144.dp)
          .clip(RoundedCornerShape(22.dp)),
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_donation_banner),
          contentDescription = stringResource(R.string.cd_donation_banner),
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop,
        )
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.horizontalGradient(
                listOf(Color(0xD9052B2E), Color(0x55052B2E))
              )
            )
        )
        Column(
          modifier = Modifier
            .align(Alignment.CenterStart)
            .padding(18.dp),
        ) {
          Text(
            text = stringResource(R.string.donations_hero_title),
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = stringResource(R.string.donations_hero_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFDDF6F5),
          )
        }
      }
    }

    // Impact Card
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
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
              imageVector = Icons.Filled.VolunteerActivism,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.tertiary,
              modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = stringResource(R.string.your_total_impact),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
              )
              Text(
                text = "$totalDonated Coins Donated",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
              )
            }
          }
          Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(50),
          ) {
            Text(
              text = "Wallet: $balance",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
          }
        }
      }
    }

    // Sub-tab switch: Causes | Proofs & Help
    item {
      SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
          selected = selectedSubTab == 0,
          onClick = { selectedSubTab = 0 },
          shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
          modifier = Modifier.testTag("donations_tab_causes"),
        ) {
          Text(stringResource(R.string.donations_tab_causes))
        }
        SegmentedButton(
          selected = selectedSubTab == 1,
          onClick = { selectedSubTab = 1 },
          shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
          modifier = Modifier.testTag("donations_tab_proofs"),
        ) {
          Text("${stringResource(R.string.donations_tab_proofs)} (${publishedProofs.size})")
        }
      }
    }

    if (selectedSubTab == 0) {
      when (val state = campaignsState) {
        is UiState.Loading -> {
          item {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
              contentAlignment = Alignment.Center,
            ) {
              CircularProgressIndicator()
            }
          }
        }
        is UiState.Error -> {
          item {
            Text(
              text = state.message,
              color = MaterialTheme.colorScheme.error,
              modifier = Modifier.padding(16.dp),
            )
          }
        }
        is UiState.Success -> {
          if (state.data.isEmpty()) {
            item {
              Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                  Icon(Icons.Filled.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                  Spacer(modifier = Modifier.height(8.dp))
                  Text(stringResource(R.string.empty_donations_title), style = MaterialTheme.typography.titleMedium)
                  Text(
                    stringResource(R.string.empty_donations_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          } else {
            items(state.data, key = { it.id }) { campaign ->
              CampaignCard(
                campaign = campaign,
                isBusy = isBusy,
                onDonateClick = { selectedCampaignForDonate = campaign },
              )
            }
          }
        }
      }

      if (myDonations.isNotEmpty()) {
        item {
          Text(
            text = stringResource(R.string.my_donation_receipts),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp),
          )
        }
        items(myDonations, key = { it.id }) { rec ->
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
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
                Text(rec.campaignTitle, style = MaterialTheme.typography.titleSmall)
                if (rec.message.isNotBlank()) {
                  Text("\"${rec.message}\"", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                  text = "Charity Receipt • No tickets",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Text(
                text = "+${rec.coinsDonated} Coins",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }
    } else {
      if (publishedProofs.isEmpty()) {
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Icon(Icons.Filled.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
              Spacer(modifier = Modifier.height(8.dp))
              Text("No verified proofs posted yet", style = MaterialTheme.typography.titleMedium)
              Text(
                "Admin payouts, giveaway delivery proofs, and helping donations appear here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      } else {
        items(publishedProofs, key = { it.id }) { proof ->
          PayoutProofCard(proof = proof)
        }
      }
    }

    item { Spacer(modifier = Modifier.height(24.dp)) }
  }

  selectedCampaignForDonate?.let { campaign ->
    DonateCoinsDialog(
      campaign = campaign,
      maxCoins = balance,
      onDismiss = { selectedCampaignForDonate = null },
      onConfirm = { amount, note ->
        viewModel.donateToCampaign(campaign, amount, note)
        selectedCampaignForDonate = null
      },
    )
  }
}

@Composable
private fun CampaignCard(
  campaign: DonationCampaign,
  isBusy: Boolean,
  onDonateClick: () -> Unit,
) {
  val progress = if (campaign.goalCoins > 0L) {
    (campaign.raisedCoins.toFloat() / campaign.goalCoins.toFloat()).coerceIn(0f, 1f)
  } else 0f

  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    shape = RoundedCornerShape(20.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OriginPill(text = campaign.category, isAccent = true)
        OriginPill(text = campaign.region)
        OriginPill(text = "No Tickets")
      }
      Spacer(modifier = Modifier.height(10.dp))
      Text(campaign.title, style = MaterialTheme.typography.titleLarge)
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = campaign.description,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(14.dp))
      LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier
          .fillMaxWidth()
          .height(8.dp)
          .clip(RoundedCornerShape(50)),
      )
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "${campaign.raisedCoins} raised",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "Goal: ${campaign.goalCoins} Coins",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Spacer(modifier = Modifier.height(12.dp))
      Button(
        onClick = onDonateClick,
        enabled = !isBusy && campaign.status == "active",
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("donate_campaign_button_${campaign.id}"),
      ) {
        Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.donate_coins_action))
      }
    }
  }
}

@Composable
private fun PayoutProofCard(proof: PayoutProof) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    shape = RoundedCornerShape(20.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(150.dp)
          .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
      ) {
        ArtworkMediaBox(
          imageUrl = proof.imageUrl,
          presetArt = proof.presetArt,
          contentDescription = proof.title,
          modifier = Modifier.fillMaxSize(),
        )
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(listOf(Color(0x22000000), Color(0xBB052B2E)))
            )
        )
        Row(
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(12.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OriginPill(text = proof.proofType, isAccent = true)
          OriginPill(text = proof.region)
        }
        Column(
          modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(14.dp),
        ) {
          Text(
            text = proof.title,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "${proof.amountDisplay} • To: ${proof.recipientName}",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFFFFE5B4),
          )
        }
      }
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = proof.description,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun DonateCoinsDialog(
  campaign: DonationCampaign,
  maxCoins: Long,
  onDismiss: () -> Unit,
  onConfirm: (Long, String) -> Unit,
) {
  var amountText by remember { mutableStateOf("10") }
  var message by remember { mutableStateOf("") }
  val presets = listOf(10L, 50L, 100L, 500L)

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Donate to ${campaign.title}") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
          color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = "Pure charity: Donations never issue giveaway tickets. Donate 1 to ∞ coins from your wallet.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(10.dp),
          )
        }
        Text(
          text = "Available balance: $maxCoins Coins",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          presets.forEach { preset ->
            FilterChip(
              selected = amountText == preset.toString(),
              onClick = { amountText = preset.toString() },
              label = { Text("$preset") },
            )
          }
          if (maxCoins > 0L) {
            FilterChip(
              selected = amountText == maxCoins.toString(),
              onClick = { amountText = maxCoins.toString() },
              label = { Text("All") },
            )
          }
        }
        OutlinedTextField(
          value = amountText,
          onValueChange = { amountText = it.filter { c -> c.isDigit() } },
          label = { Text(stringResource(R.string.coins_amount_label)) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("donate_amount_input"),
        )
        OutlinedTextField(
          value = message,
          onValueChange = { message = it },
          label = { Text(stringResource(R.string.dedication_note_label)) },
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      val coins = amountText.toLongOrNull() ?: 0L
      Button(
        onClick = { onConfirm(coins, message) },
        enabled = coins in 1..maxCoins,
        modifier = Modifier.testTag("confirm_donate_button"),
      ) {
        Text(stringResource(R.string.confirm_donation_action))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_action)) }
    },
  )
}
