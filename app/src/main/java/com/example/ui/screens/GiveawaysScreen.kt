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
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Giveaway
import com.example.ui.viewmodel.PakGivesViewModel
import com.example.ui.viewmodel.UiState

@Composable
fun GiveawaysScreen(
  viewModel: PakGivesViewModel,
  onNavigateToBuyCoins: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val giveawaysState by viewModel.giveawaysState.collectAsStateWithLifecycle()
  val profile by viewModel.userProfile.collectAsStateWithLifecycle()
  val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
  val balance = profile?.coinBalance ?: 0L

  var selectedGiveawayForTickets by remember { mutableStateOf<Giveaway?>(null) }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // Hero Banner
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(144.dp)
          .clip(RoundedCornerShape(22.dp)),
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_giveaway_banner),
          contentDescription = stringResource(R.string.cd_giveaway_banner),
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop,
        )
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.horizontalGradient(
                listOf(Color(0xD9052B1B), Color(0x55052B1B))
              )
            )
        )
        Column(
          modifier = Modifier
            .align(Alignment.CenterStart)
            .padding(18.dp),
        ) {
          Text(
            text = stringResource(R.string.giveaways_hero_title),
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = stringResource(R.string.giveaways_hero_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFFFF3DC),
          )
        }
      }
    }

    // Coin Balance Quick Bar
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Filled.MonetizationOn,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = stringResource(R.string.your_giveaway_coins),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
              )
              Text(
                text = "$balance Coins (${balance / 10L} tickets)",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
              )
            }
          }
          OutlinedButton(
            onClick = onNavigateToBuyCoins,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("giveaways_buy_coins_button"),
          ) {
            Text(stringResource(R.string.buy_coins_action))
          }
        }
      }
    }

    when (val state = giveawaysState) {
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
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = state.message,
              color = MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.padding(16.dp),
            )
          }
        }
      }
      is UiState.Success -> {
        if (state.data.isEmpty()) {
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
                Icon(
                  imageVector = Icons.Filled.CardGiftcard,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(44.dp),
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                  text = stringResource(R.string.empty_giveaways_title),
                  style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = stringResource(R.string.empty_giveaways_subtitle),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        } else {
          items(state.data, key = { it.id }) { giveaway ->
            val ticketFlow = remember(giveaway.id) { viewModel.observeUserTicketCount(giveaway.id) }
            val myTickets by ticketFlow.collectAsStateWithLifecycle(initialValue = 0L)

            GiveawayCard(
              giveaway = giveaway,
              myTickets = myTickets,
              isBusy = isBusy,
              balance = balance,
              onOpenBuyDialog = { selectedGiveawayForTickets = giveaway },
              onBuyCoins = onNavigateToBuyCoins,
            )
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(24.dp)) }
  }

  selectedGiveawayForTickets?.let { giveaway ->
    val ticketFlow = remember(giveaway.id) { viewModel.observeUserTicketCount(giveaway.id) }
    val myTickets by ticketFlow.collectAsStateWithLifecycle(initialValue = 0L)
    BuyGiveawayTicketsDialog(
      giveaway = giveaway,
      myCurrentTickets = myTickets,
      coinBalance = balance,
      onDismiss = { selectedGiveawayForTickets = null },
      onBuyTickets = { count ->
        viewModel.buyGiveawayTickets(giveaway, count)
        selectedGiveawayForTickets = null
      },
      onBuyCoins = {
        selectedGiveawayForTickets = null
        onNavigateToBuyCoins()
      },
    )
  }
}

@Composable
internal fun ArtworkMediaBox(
  imageUrl: String,
  presetArt: String,
  contentDescription: String,
  modifier: Modifier = Modifier,
) {
  val fallbackRes = when (presetArt) {
    "money_payout", "donation_help", "helping_hand" -> R.drawable.img_donation_banner
    "heritage_name", "names" -> R.drawable.img_hero_names
    else -> R.drawable.img_giveaway_banner
  }
  if (imageUrl.isNotBlank()) {
    AsyncImage(
      model = imageUrl,
      contentDescription = contentDescription,
      placeholder = painterResource(id = fallbackRes),
      error = painterResource(id = fallbackRes),
      contentScale = ContentScale.Crop,
      modifier = modifier,
    )
  } else {
    Image(
      painter = painterResource(id = fallbackRes),
      contentDescription = contentDescription,
      contentScale = ContentScale.Crop,
      modifier = modifier,
    )
  }
}

@Composable
private fun GiveawayCard(
  giveaway: Giveaway,
  myTickets: Long,
  isBusy: Boolean,
  balance: Long,
  onOpenBuyDialog: () -> Unit,
  onBuyCoins: () -> Unit,
) {
  val isActive = giveaway.status == "active"
  val isUnlimited = giveaway.maxEntries == 0L
  val ticketsLeft = if (isUnlimited) Long.MAX_VALUE else (giveaway.maxEntries - giveaway.entriesCount).coerceAtLeast(0L)
  val isSoldOut = !isUnlimited && ticketsLeft <= 0L
  val progress = if (!isUnlimited && giveaway.maxEntries > 0L) {
    (giveaway.entriesCount.toFloat() / giveaway.maxEntries.toFloat()).coerceIn(0f, 1f)
  } else 0f

  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    shape = RoundedCornerShape(20.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column {
      // Giveaway Picture + Title Overlay
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(172.dp)
          .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
      ) {
        ArtworkMediaBox(
          imageUrl = giveaway.imageUrl,
          presetArt = giveaway.presetArt,
          contentDescription = "Picture for ${giveaway.title}",
          modifier = Modifier.fillMaxSize(),
        )
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                listOf(Color(0x33000000), Color(0xCC06281A))
              )
            )
        )
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopStart)
            .padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OriginPill(text = giveaway.regionTag, isAccent = true)
          Surface(
            color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(50),
          ) {
            Text(
              text = if (isActive) {
                if (isSoldOut) "Sold Out" else stringResource(R.string.status_live)
              } else {
                stringResource(R.string.status_completed)
              },
              style = MaterialTheme.typography.labelSmall,
              color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
        }
        Column(
          modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(14.dp),
        ) {
          Text(
            text = giveaway.title,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "Prize: ${giveaway.prizeTitle}",
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFFFFE5B4),
          )
        }
      }

      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = giveaway.description,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Ticket details: Sold, Left, Price per ticket, User's current tickets
        if (!isUnlimited) {
          LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
              .fillMaxWidth()
              .height(8.dp)
              .clip(RoundedCornerShape(50)),
          )
          Spacer(modifier = Modifier.height(8.dp))
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = if (isUnlimited) Icons.Filled.AllInclusive else Icons.Filled.ConfirmationNumber,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (isUnlimited) {
                "${giveaway.entriesCount} sold • ∞ Unlimited left"
              } else {
                "${giveaway.entriesCount}/${giveaway.maxEntries} sold • $ticketsLeft left"
              },
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurface,
              fontWeight = FontWeight.SemiBold,
            )
          }
          Text(
            text = "${giveaway.entryCostCoins} Coins / ticket",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold,
          )
        }

        if (myTickets > 0L) {
          Spacer(modifier = Modifier.height(8.dp))
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = "You hold $myTickets ticket(s) in this draw",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (!isActive && giveaway.winnerName.isNotBlank()) {
          Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "Winner: ${giveaway.winnerName}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
              )
            }
          }
        } else if (isActive) {
          if (balance >= giveaway.entryCostCoins && !isSoldOut) {
            Button(
              onClick = onOpenBuyDialog,
              enabled = !isBusy,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("enter_giveaway_button_${giveaway.id}"),
            ) {
              Icon(Icons.Filled.ConfirmationNumber, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(stringResource(R.string.buy_tickets_action))
            }
          } else if (isSoldOut) {
            OutlinedButton(
              onClick = onOpenBuyDialog,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
              Text("Sold Out — View Giveaway Details")
            }
          } else {
            OutlinedButton(
              onClick = onBuyCoins,
              colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            ) {
              Text("Need ${giveaway.entryCostCoins} Coins/ticket — Buy on WhatsApp")
            }
          }
        }
      }
    }
  }
}

@Composable
private fun BuyGiveawayTicketsDialog(
  giveaway: Giveaway,
  myCurrentTickets: Long,
  coinBalance: Long,
  onDismiss: () -> Unit,
  onBuyTickets: (Long) -> Unit,
  onBuyCoins: () -> Unit,
) {
  val isUnlimited = giveaway.maxEntries == 0L
  val ticketsLeft = if (isUnlimited) Long.MAX_VALUE else (giveaway.maxEntries - giveaway.entriesCount).coerceAtLeast(0L)
  val maxAffordable = coinBalance / giveaway.entryCostCoins.coerceAtLeast(1L)
  val maxPurchasable = minOf(maxAffordable, ticketsLeft).coerceAtLeast(0L)

  var ticketCountText by remember { mutableStateOf("1") }
  val requestedTickets = ticketCountText.toLongOrNull() ?: 0L
  val totalCost = requestedTickets * giveaway.entryCostCoins
  val quickCounts = listOf(1L, 5L, 10L, 50L)

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(giveaway.title) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(14.dp)),
        ) {
          ArtworkMediaBox(
            imageUrl = giveaway.imageUrl,
            presetArt = giveaway.presetArt,
            contentDescription = giveaway.title,
            modifier = Modifier.fillMaxSize(),
          )
        }
        Text("Prize: ${giveaway.prizeTitle}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Text(giveaway.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
          text = if (isUnlimited) {
            "Tickets Sold: ${giveaway.entriesCount} • Remaining: ∞ Unlimited"
          } else {
            "Tickets Sold: ${giveaway.entriesCount} / ${giveaway.maxEntries} • Remaining: $ticketsLeft"
          },
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          text = "Your tickets: $myCurrentTickets • Wallet: $coinBalance Coins (1 ticket = ${giveaway.entryCostCoins} Coins)",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (ticketsLeft > 0L) {
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            quickCounts.forEach { preset ->
              FilterChip(
                selected = ticketCountText == preset.toString(),
                onClick = { ticketCountText = preset.toString() },
                label = { Text("+$preset") },
              )
            }
            if (maxPurchasable > 0L) {
              FilterChip(
                selected = ticketCountText == maxPurchasable.toString(),
                onClick = { ticketCountText = maxPurchasable.toString() },
                label = { Text("Max") },
              )
            }
          }

          OutlinedTextField(
            value = ticketCountText,
            onValueChange = { ticketCountText = it.filter { ch -> ch.isDigit() } },
            label = { Text("Tickets to buy (1 to ∞)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("ticket_count_input"),
          )

          Text(
            text = "Total Cost: $totalCost Giveaway Coins",
            style = MaterialTheme.typography.titleSmall,
            color = if (totalCost <= coinBalance && requestedTickets <= ticketsLeft) {
              MaterialTheme.colorScheme.primary
            } else {
              MaterialTheme.colorScheme.error
            },
            fontWeight = FontWeight.Bold,
          )
        }
      }
    },
    confirmButton = {
      if (ticketsLeft > 0L && maxAffordable >= 1L) {
        Button(
          onClick = { onBuyTickets(requestedTickets) },
          enabled = requestedTickets >= 1L && requestedTickets <= ticketsLeft && totalCost <= coinBalance,
          modifier = Modifier.testTag("confirm_buy_tickets_button"),
        ) {
          Text("Buy $requestedTickets Ticket(s)")
        }
      } else if (ticketsLeft > 0L) {
        Button(onClick = onBuyCoins) {
          Text("Buy Coins")
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Close") }
    },
  )
}
