package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.viewmodel.GiveawayParticipationItem
import com.example.ui.viewmodel.PakGivesViewModel
import com.example.ui.viewmodel.TransactionCategory
import com.example.ui.viewmodel.TransactionDirection
import com.example.ui.viewmodel.WalletTransactionItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(
  viewModel: PakGivesViewModel,
  onOpenTerms: () -> Unit,
  onOpenAdminPanel: () -> Unit,
  onOpenBuyCoins: () -> Unit,
  onOpenGiveaways: () -> Unit,
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val profile by viewModel.userProfile.collectAsStateWithLifecycle()
  val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
  val favoriteIds by viewModel.favoriteNameIds.collectAsStateWithLifecycle()
  val myReports by viewModel.myProblemReports.collectAsStateWithLifecycle()
  val participationHistory by viewModel.giveawayParticipationHistory.collectAsStateWithLifecycle()
  val allTransactions by viewModel.walletTransactions.collectAsStateWithLifecycle()

  var showPasswordDialog by remember { mutableStateOf(false) }
  var showEditProfileDialog by remember { mutableStateOf(false) }
  var showReportDialog by remember { mutableStateOf(false) }
  var selectedTxCategory by remember { mutableStateOf(TransactionCategory.ALL) }
  var showAllTransactions by remember { mutableStateOf(false) }

  val filteredTransactions = remember(allTransactions, selectedTxCategory) {
    if (selectedTxCategory == TransactionCategory.ALL) allTransactions
    else allTransactions.filter { it.category == selectedTxCategory }
  }
  val visibleTransactions = remember(filteredTransactions, showAllTransactions) {
    if (showAllTransactions) filteredTransactions else filteredTransactions.take(5)
  }

  val coinBalance = profile?.coinBalance ?: 0L

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // User Identity Card
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier = Modifier
              .size(56.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Filled.Person,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(30.dp),
            )
          }
          Spacer(modifier = Modifier.width(14.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = profile?.displayName ?: viewModel.currentUserName,
              style = MaterialTheme.typography.titleLarge,
            )
            Text(
              text = profile?.email ?: viewModel.currentUserEmail,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OriginPill(text = "${favoriteIds.size} Saved Names")
              if (isAdmin) {
                OriginPill(text = "Admin Verified", isAccent = true)
              }
            }
          }
          IconButton(
            onClick = { showEditProfileDialog = true },
            modifier = Modifier.testTag("edit_profile_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Edit,
              contentDescription = stringResource(R.string.cd_edit_profile),
              tint = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }
    }

    // Current Coin Balance Hero Card
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("profile_coin_balance_card"),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Filled.MonetizationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = stringResource(R.string.profile_coin_balance_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "$coinBalance Coins",
              style = MaterialTheme.typography.headlineMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
              text = "≈ ${coinBalance / 10L} giveaway tickets (10 Coins/ticket)",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Button(
            onClick = onOpenBuyCoins,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .height(48.dp)
              .testTag("profile_buy_coins_button"),
          ) {
            Text(stringResource(R.string.buy_coins_action))
          }
        }
      }
    }

    // Donations & Money Received Stats
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
          shape = RoundedCornerShape(18.dp),
          modifier = Modifier.weight(1f),
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Icon(Icons.Filled.VolunteerActivism, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            Spacer(modifier = Modifier.height(6.dp))
            Text("${profile?.totalDonatedCoins ?: 0L} Coins", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.coins_donated_label), style = MaterialTheme.typography.labelSmall)
          }
        }
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
          shape = RoundedCornerShape(18.dp),
          modifier = Modifier.weight(1f),
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(6.dp))
            Text("PKR ${profile?.totalMoneyReceived ?: 0L}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.money_received_label), style = MaterialTheme.typography.labelSmall)
          }
        }
      }
    }

    // Giveaway Participation History
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 4.dp)
          .testTag("profile_participation_section"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Filled.CardGiftcard,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "${stringResource(R.string.profile_participation_title)} (${participationHistory.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }

    if (participationHistory.isEmpty()) {
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = RoundedCornerShape(18.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text = stringResource(R.string.profile_participation_empty),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
              onClick = onOpenGiveaways,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.height(48.dp),
            ) {
              Text(stringResource(R.string.browse_giveaways_action))
            }
          }
        }
      }
    } else {
      items(participationHistory, key = { "part_${it.giveaway.id}" }) { item ->
        GiveawayParticipationCard(item = item)
      }
    }

    // Transaction History Header & Filters
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 4.dp)
          .testTag("profile_transactions_section"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Filled.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "${stringResource(R.string.profile_transactions_title)} (${filteredTransactions.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(TransactionCategory.entries) { category ->
            FilterChip(
              selected = selectedTxCategory == category,
              onClick = {
                selectedTxCategory = category
                showAllTransactions = false
              },
              label = { Text(category.label) },
              modifier = Modifier.testTag("tx_filter_${category.name.lowercase()}"),
            )
          }
        }
      }
    }

    if (visibleTransactions.isEmpty()) {
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = RoundedCornerShape(18.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = if (selectedTxCategory == TransactionCategory.ALL) {
              stringResource(R.string.profile_transactions_empty)
            } else {
              "No ${selectedTxCategory.label.lowercase()} transactions recorded yet."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(18.dp),
          )
        }
      }
    } else {
      items(visibleTransactions, key = { it.id }) { tx ->
        WalletTransactionRowCard(tx = tx)
      }
      if (filteredTransactions.size > 5) {
        item {
          TextButton(
            onClick = { showAllTransactions = !showAllTransactions },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("toggle_all_transactions_button"),
          ) {
            Text(
              if (showAllTransactions) "Show fewer transactions"
              else "Show all (${filteredTransactions.size}) transactions"
            )
          }
        }
      }
    }

    // Report a Problem / Support
    item {
      Card(
        onClick = { showReportDialog = true },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("report_problem_button"),
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(Icons.Filled.ReportProblem, contentDescription = null, tint = MaterialTheme.colorScheme.error)
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.report_problem_title), style = MaterialTheme.typography.titleMedium)
            Text(
              stringResource(R.string.report_problem_sub),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }

    if (myReports.isNotEmpty()) {
      item {
        Text(
          text = "My Problem Reports (${myReports.size})",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(top = 4.dp),
        )
      }
      items(myReports, key = { it.id }) { rep ->
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(rep.subject, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
              OriginPill(text = rep.status, isAccent = rep.status == "resolved")
            }
            Text("${rep.category} • ${rep.description}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (rep.adminReply.isNotBlank()) {
              Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Text(
                  text = "Admin Reply: ${rep.adminReply}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.padding(10.dp),
                )
              }
            }
          }
        }
      }
    }

    // Terms & Policies
    item {
      Card(
        onClick = onOpenTerms,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("open_terms_button"),
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(Icons.Filled.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.terms_policies_title), style = MaterialTheme.typography.titleMedium)
            Text(
              if (profile?.acceptedTerms == true) stringResource(R.string.terms_accepted_sub)
              else stringResource(R.string.terms_review_sub),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          if (profile?.acceptedTerms == true) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          }
        }
      }
    }

    // Admin Panel Gate
    item {
      Card(
        onClick = {
          if (isAdmin) onOpenAdminPanel() else showPasswordDialog = true
        },
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("open_admin_panel_button"),
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.admin_panel_title), style = MaterialTheme.typography.titleMedium)
            Text(
              if (isAdmin) stringResource(R.string.admin_unlocked_sub)
              else stringResource(R.string.admin_locked_sub),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
      }
    }

    // Sign Out
    item {
      OutlinedButton(
        onClick = onSignOut,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("sign_out_button"),
      ) {
        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.sign_out_action))
      }
    }
  }

  if (showPasswordDialog) {
    AdminPasswordDialog(
      onDismiss = { showPasswordDialog = false },
      onUnlock = { password ->
        viewModel.unlockAdmin(password) {
          showPasswordDialog = false
          onOpenAdminPanel()
        }
      },
    )
  }

  if (showEditProfileDialog) {
    EditProfileDialog(
      currentName = profile?.displayName ?: viewModel.currentUserName,
      onDismiss = { showEditProfileDialog = false },
      onSave = { newName ->
        viewModel.updateDisplayName(newName)
        showEditProfileDialog = false
      },
    )
  }

  if (showReportDialog) {
    ReportProblemDialog(
      onDismiss = { showReportDialog = false },
      onSubmit = { category, subject, description ->
        viewModel.submitProblemReport(category, subject, description)
        showReportDialog = false
      },
    )
  }
}

@Composable
private fun GiveawayParticipationCard(item: GiveawayParticipationItem) {
  val giveaway = item.giveaway
  val isLive = giveaway.status == "active"
  val ticketWord = if (item.totalTickets == 1L) "ticket" else "tickets"
  val statusText = when {
    item.isCurrentUserWinner -> "You Won!"
    isLive -> "Live Draw"
    giveaway.winnerName.isNotBlank() -> "Winner: ${giveaway.winnerName}"
    else -> "Drawn"
  }

  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    shape = RoundedCornerShape(18.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("participation_card_${giveaway.id}"),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ArtworkMediaBox(
        imageUrl = giveaway.imageUrl,
        presetArt = giveaway.presetArt,
        contentDescription = giveaway.title,
        modifier = Modifier
          .size(64.dp)
          .clip(RoundedCornerShape(14.dp)),
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
          text = giveaway.title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "Prize: ${giveaway.prizeTitle}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = "${item.totalTickets} $ticketWord • ${item.totalCoinsSpent} Coins",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
          )
          Surface(
            color = when {
              item.isCurrentUserWinner -> MaterialTheme.colorScheme.secondaryContainer
              isLive -> MaterialTheme.colorScheme.primaryContainer
              else -> MaterialTheme.colorScheme.surfaceVariant
            },
            shape = RoundedCornerShape(50),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              if (item.isCurrentUserWinner) {
                Icon(
                  imageVector = Icons.Filled.EmojiEvents,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.secondary,
                  modifier = Modifier.size(12.dp),
                )
              }
              Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                color = when {
                  item.isCurrentUserWinner -> MaterialTheme.colorScheme.onSecondaryContainer
                  isLive -> MaterialTheme.colorScheme.onPrimaryContainer
                  else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
        if (item.latestTimestampSeconds > 0L) {
          Text(
            text = "Last entry: ${formatEpochSeconds(item.latestTimestampSeconds)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
private fun WalletTransactionRowCard(tx: WalletTransactionItem) {
  val categoryIcon = when (tx.category) {
    TransactionCategory.COINS -> Icons.Filled.MonetizationOn
    TransactionCategory.GIVEAWAYS -> Icons.Filled.ConfirmationNumber
    TransactionCategory.DONATIONS -> Icons.Filled.VolunteerActivism
    TransactionCategory.PAYOUTS -> Icons.Filled.AccountBalanceWallet
    TransactionCategory.ALL -> Icons.Filled.History
  }
  val iconTint = when (tx.direction) {
    TransactionDirection.CREDIT -> MaterialTheme.colorScheme.primary
    TransactionDirection.DEBIT -> MaterialTheme.colorScheme.error
    TransactionDirection.PENDING -> MaterialTheme.colorScheme.secondary
  }
  val badgeContainer = when (tx.direction) {
    TransactionDirection.CREDIT -> MaterialTheme.colorScheme.primaryContainer
    TransactionDirection.DEBIT -> MaterialTheme.colorScheme.surfaceVariant
    TransactionDirection.PENDING -> MaterialTheme.colorScheme.secondaryContainer
  }
  val badgeContent = when (tx.direction) {
    TransactionDirection.CREDIT -> MaterialTheme.colorScheme.onPrimaryContainer
    TransactionDirection.DEBIT -> MaterialTheme.colorScheme.onSurfaceVariant
    TransactionDirection.PENDING -> MaterialTheme.colorScheme.onSecondaryContainer
  }

  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(16.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("transaction_card_${tx.id}"),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .background(badgeContainer),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = categoryIcon,
          contentDescription = null,
          tint = iconTint,
          modifier = Modifier.size(20.dp),
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = tx.title,
          style = MaterialTheme.typography.titleSmall,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = tx.subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        if (tx.timestampSeconds > 0L) {
          Text(
            text = formatEpochSeconds(tx.timestampSeconds),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      Spacer(modifier = Modifier.width(8.dp))
      Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = tx.amountText,
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
          color = iconTint,
        )
        Surface(
          color = badgeContainer,
          shape = RoundedCornerShape(50),
        ) {
          Text(
            text = tx.statusLabel,
            style = MaterialTheme.typography.labelSmall,
            color = badgeContent,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
          )
        }
      }
    }
  }
}

private fun formatEpochSeconds(seconds: Long): String {
  if (seconds <= 0L) return "Just now"
  val formatter = SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault())
  return formatter.format(Date(seconds * 1000L))
}

@Composable
private fun EditProfileDialog(
  currentName: String,
  onDismiss: () -> Unit,
  onSave: (String) -> Unit,
) {
  var name by remember { mutableStateOf(currentName) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Update Display Name") },
    text = {
      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Display Name") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
    },
    confirmButton = {
      Button(
        onClick = { onSave(name) },
        enabled = name.trim().isNotEmpty(),
      ) {
        Text("Save")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_action)) }
    },
  )
}

@Composable
private fun ReportProblemDialog(
  onDismiss: () -> Unit,
  onSubmit: (String, String, String) -> Unit,
) {
  val categories = listOf("Giveaway", "Coin Order", "Donation", "Account", "Other")
  var selectedCategory by remember { mutableStateOf(categories.first()) }
  var subject by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = { Icon(Icons.Filled.ReportProblem, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
    title = { Text(stringResource(R.string.report_problem_title)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(categories) { cat ->
            FilterChip(
              selected = selectedCategory == cat,
              onClick = { selectedCategory = cat },
              label = { Text(cat) },
            )
          }
        }
        OutlinedTextField(
          value = subject,
          onValueChange = { subject = it },
          label = { Text("Subject *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("report_subject_input"),
        )
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Describe your problem *") },
          minLines = 3,
          modifier = Modifier.fillMaxWidth().testTag("report_description_input"),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSubmit(selectedCategory, subject, description) },
        enabled = subject.trim().length >= 3 && description.trim().length >= 5,
        modifier = Modifier.testTag("submit_report_button"),
      ) {
        Text("Send Report")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_action)) }
    },
  )
}

@Composable
private fun AdminPasswordDialog(
  onDismiss: () -> Unit,
  onUnlock: (String) -> Unit,
) {
  var password by remember { mutableStateOf("") }
  var showPassword by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = { Icon(Icons.Filled.AdminPanelSettings, contentDescription = null) },
    title = { Text(stringResource(R.string.admin_gate_title)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = stringResource(R.string.admin_gate_prompt),
          style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
          value = password,
          onValueChange = { password = it },
          label = { Text(stringResource(R.string.admin_password_label)) },
          singleLine = true,
          visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
          trailingIcon = {
            IconButton(onClick = { showPassword = !showPassword }) {
              Icon(
                imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                contentDescription = stringResource(R.string.cd_toggle_password),
              )
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_password_input"),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onUnlock(password) },
        enabled = password.isNotBlank(),
        modifier = Modifier.testTag("unlock_admin_button"),
      ) {
        Text(stringResource(R.string.unlock_action))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_action)) }
    },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsPoliciesScreen(
  viewModel: PakGivesViewModel,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  val appConfig by viewModel.appConfig.collectAsStateWithLifecycle()
  val profile by viewModel.userProfile.collectAsStateWithLifecycle()

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.terms_policies_title)) },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
          }
        },
      )
    },
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = RoundedCornerShape(18.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
              text = appConfig.termsTitle,
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = appConfig.termsBody,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      }

      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
          shape = RoundedCornerShape(18.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
              text = stringResource(R.string.giveaway_policy_header),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = appConfig.giveawayPolicyBody,
              style = MaterialTheme.typography.bodyMedium,
            )
          }
        }
      }

      item {
        if (profile?.acceptedTerms == true) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 8.dp),
          ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = stringResource(R.string.terms_already_accepted),
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        } else {
          Button(
            onClick = { viewModel.acceptTerms() },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("accept_terms_button"),
          ) {
            Text(stringResource(R.string.accept_terms_action))
          }
        }
      }
    }
  }
}
