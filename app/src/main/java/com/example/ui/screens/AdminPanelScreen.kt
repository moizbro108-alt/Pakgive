package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.AppConfig
import com.example.data.model.Giveaway
import com.example.data.model.ProblemReport
import com.example.data.model.UserProfile
import com.example.ui.viewmodel.PakGivesViewModel
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
  viewModel: PakGivesViewModel,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  val allOrders by viewModel.adminCoinOrders.collectAsStateWithLifecycle()
  val allUsers by viewModel.adminUsers.collectAsStateWithLifecycle()
  val allReports by viewModel.adminProblemReports.collectAsStateWithLifecycle()
  val giveawaysState by viewModel.giveawaysState.collectAsStateWithLifecycle()
  val publishedProofs by viewModel.publishedProofs.collectAsStateWithLifecycle()
  val appConfig by viewModel.appConfig.collectAsStateWithLifecycle()
  val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

  var selectedTab by remember { mutableIntStateOf(0) }
  var showCreateGiveawayDialog by remember { mutableStateOf(false) }
  var showCreateCampaignDialog by remember { mutableStateOf(false) }
  var showCreateProofDialog by remember { mutableStateOf(false) }
  var giveawayToEditLimit by remember { mutableStateOf<Giveaway?>(null) }
  var userToGiveMoney by remember { mutableStateOf<UserProfile?>(null) }
  var reportToReply by remember { mutableStateOf<ProblemReport?>(null) }

  val tabs = listOf("Coin Orders", "Giveaways", "Users & Money", "Proofs & Help", "Reports", "Settings")

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.admin_panel_title)) },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
          }
        },
        actions = {
          TextButton(onClick = { viewModel.seedStarterData() }, enabled = !isBusy) {
            Icon(Icons.Filled.Star, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Seed")
          }
        },
      )
    },
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
    ) {
      SecondaryScrollableTabRow(selectedTabIndex = selectedTab) {
        tabs.forEachIndexed { index, title ->
          Tab(
            selected = selectedTab == index,
            onClick = { selectedTab = index },
            text = { Text(title) },
          )
        }
      }

      when (selectedTab) {
        0 -> {
          // Coin Orders Approval Tab
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            if (allOrders.isEmpty()) {
              item {
                Text(
                  text = stringResource(R.string.admin_no_orders),
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            } else {
              items(allOrders, key = { it.id }) { order ->
                Card(
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                  shape = RoundedCornerShape(16.dp),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                      Text("${order.buyerName} • ${order.coinsAmount} Coins", style = MaterialTheme.typography.titleMedium)
                      OriginPill(text = order.status, isAccent = order.status == "pending")
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${order.packageName} (${order.priceDisplay})", style = MaterialTheme.typography.bodySmall)
                    Text("WhatsApp Ref: ${order.whatsappRef}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    if (order.status == "pending") {
                      Spacer(modifier = Modifier.height(10.dp))
                      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                          onClick = { viewModel.approveCoinOrder(order) },
                          enabled = !isBusy,
                          modifier = Modifier.weight(1f).testTag("approve_order_${order.id}"),
                        ) {
                          Icon(Icons.Filled.Check, contentDescription = null)
                          Spacer(modifier = Modifier.width(4.dp))
                          Text("Approve")
                        }
                        OutlinedButton(
                          onClick = { viewModel.rejectCoinOrder(order) },
                          enabled = !isBusy,
                          colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                          modifier = Modifier.weight(1f),
                        ) {
                          Icon(Icons.Filled.Close, contentDescription = null)
                          Spacer(modifier = Modifier.width(4.dp))
                          Text("Reject")
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }

        1 -> {
          // Giveaways Management Tab (10 coins/ticket, ticket limits, pics, draw winner)
          val giveaways = (giveawaysState as? UiState.Success)?.data ?: emptyList()
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            item {
              Button(
                onClick = { showCreateGiveawayDialog = true },
                modifier = Modifier.fillMaxWidth().testTag("admin_create_giveaway_button"),
              ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.create_giveaway_action))
              }
            }
            items(giveaways, key = { it.id }) { gw ->
              val limitLabel = if (gw.maxEntries == 0L) "∞ Unlimited" else "${gw.maxEntries} max"
              Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(modifier = Modifier.padding(16.dp)) {
                  Text(gw.title, style = MaterialTheme.typography.titleMedium)
                  Text(
                    "Prize: ${gw.prizeTitle} • Sold: ${gw.entriesCount} ($limitLabel) • 10 Coins/tkt • ${gw.status}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  if (gw.winnerName.isNotBlank()) {
                    Text("Winner: ${gw.winnerName}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                  }
                  Spacer(modifier = Modifier.height(10.dp))
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (gw.status == "active") {
                      OutlinedButton(
                        onClick = { giveawayToEditLimit = gw },
                        enabled = !isBusy,
                      ) {
                        Icon(Icons.Filled.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Limit")
                      }
                      Button(
                        onClick = { viewModel.drawGiveawayWinner(gw) },
                        enabled = !isBusy && gw.entriesCount > 0L,
                        modifier = Modifier.weight(1f),
                      ) {
                        Icon(Icons.Filled.EmojiEvents, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Draw")
                      }
                    }
                    OutlinedButton(
                      onClick = { viewModel.deleteGiveaway(gw.id) },
                      enabled = !isBusy,
                      colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                      Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.cd_delete_giveaway))
                    }
                  }
                }
              }
            }
          }
        }

        2 -> {
          // Users, Money Grants & Donation Campaigns Tab
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            item {
              Button(
                onClick = { showCreateCampaignDialog = true },
                modifier = Modifier.fillMaxWidth().testTag("admin_create_campaign_button"),
              ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.create_campaign_action))
              }
            }
            item {
              Text(
                text = "Registered Users (${allUsers.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
              )
            }
            items(allUsers, key = { it.userId }) { user ->
              Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  Column {
                    Text(user.displayName, style = MaterialTheme.typography.titleSmall)
                    Text(
                      "${user.coinBalance} Coins • Donated: ${user.totalDonatedCoins} • Received: PKR ${user.totalMoneyReceived}",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                      onClick = { viewModel.creditUserCoins(user.userId, 50L) },
                      enabled = !isBusy,
                      modifier = Modifier.weight(1f),
                    ) {
                      Text("+50 Coins")
                    }
                    Button(
                      onClick = { userToGiveMoney = user },
                      enabled = !isBusy,
                      modifier = Modifier.weight(1f).testTag("give_money_button_${user.userId}"),
                    ) {
                      Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null)
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Give Money")
                    }
                  }
                }
              }
            }
          }
        }

        3 -> {
          // Proofs & Helping Donations Tab
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            item {
              Button(
                onClick = { showCreateProofDialog = true },
                modifier = Modifier.fillMaxWidth().testTag("admin_add_proof_button"),
              ) {
                Icon(Icons.Filled.Verified, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Proof / Helping Donation")
              }
            }
            items(publishedProofs, key = { it.id }) { proof ->
              Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
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
                    OriginPill(text = proof.proofType, isAccent = true)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(proof.title, style = MaterialTheme.typography.titleSmall)
                    Text(
                      "${proof.amountDisplay} • To: ${proof.recipientName}",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                      proof.description,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  IconButton(onClick = { viewModel.deletePayoutProof(proof.id) }, enabled = !isBusy) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete proof", tint = MaterialTheme.colorScheme.error)
                  }
                }
              }
            }
          }
        }

        4 -> {
          // User Problem Reports Tab
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            if (allReports.isEmpty()) {
              item {
                Text(
                  text = "No user problem reports submitted yet.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            } else {
              items(allReports, key = { it.id }) { report ->
                Card(
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                  shape = RoundedCornerShape(16.dp),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Text(report.subject, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                      OriginPill(text = report.status, isAccent = report.status == "resolved")
                    }
                    Text(
                      "${report.reporterName} • ${report.category}",
                      style = MaterialTheme.typography.labelMedium,
                      color = MaterialTheme.colorScheme.primary,
                    )
                    Text(report.description, style = MaterialTheme.typography.bodySmall)
                    if (report.adminReply.isNotBlank()) {
                      Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                      ) {
                        Text(
                          text = "Reply: ${report.adminReply}",
                          style = MaterialTheme.typography.bodySmall,
                          modifier = Modifier.padding(8.dp),
                        )
                      }
                    }
                    Button(
                      onClick = { reportToReply = report },
                      enabled = !isBusy,
                      modifier = Modifier.fillMaxWidth(),
                    ) {
                      Text("Reply / Update Status")
                    }
                  }
                }
              }
            }
          }
        }

        5 -> {
          // App Settings (WhatsApp Group & Terms/Policies Editor)
          var waUrl by remember(appConfig) { mutableStateOf(appConfig.whatsappGroupUrl) }
          var waContact by remember(appConfig) { mutableStateOf(appConfig.whatsappAdminContact) }
          var termsTitle by remember(appConfig) { mutableStateOf(appConfig.termsTitle) }
          var termsBody by remember(appConfig) { mutableStateOf(appConfig.termsBody) }
          var giveawayPolicy by remember(appConfig) { mutableStateOf(appConfig.giveawayPolicyBody) }

          LaunchedEffect(appConfig) {
            waUrl = appConfig.whatsappGroupUrl
            waContact = appConfig.whatsappAdminContact
            termsTitle = appConfig.termsTitle
            termsBody = appConfig.termsBody
            giveawayPolicy = appConfig.giveawayPolicyBody
          }

          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            item {
              Text(
                text = "WhatsApp Coin Group & Policies",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
            }
            item {
              OutlinedTextField(
                value = waUrl,
                onValueChange = { waUrl = it },
                label = { Text("Official WhatsApp Group URL") },
                modifier = Modifier.fillMaxWidth(),
              )
            }
            item {
              OutlinedTextField(
                value = waContact,
                onValueChange = { waContact = it },
                label = { Text("WhatsApp Admin Contact Label") },
                modifier = Modifier.fillMaxWidth(),
              )
            }
            item {
              OutlinedTextField(
                value = termsTitle,
                onValueChange = { termsTitle = it },
                label = { Text("Terms & Policies Title") },
                modifier = Modifier.fillMaxWidth(),
              )
            }
            item {
              OutlinedTextField(
                value = termsBody,
                onValueChange = { termsBody = it },
                label = { Text("Terms & Policies Body") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
              )
            }
            item {
              OutlinedTextField(
                value = giveawayPolicy,
                onValueChange = { giveawayPolicy = it },
                label = { Text("Giveaway Policy Body") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
              )
            }
            item {
              Button(
                onClick = {
                  viewModel.updateAppConfig(
                    AppConfig(
                      whatsappGroupUrl = waUrl,
                      whatsappAdminContact = waContact,
                      termsTitle = termsTitle,
                      termsBody = termsBody,
                      giveawayPolicyBody = giveawayPolicy,
                    )
                  )
                },
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth().height(50.dp),
              ) {
                Icon(Icons.Filled.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Settings")
              }
            }
          }
        }
      }
    }
  }

  if (showCreateGiveawayDialog) {
    CreateGiveawayDialog(
      onDismiss = { showCreateGiveawayDialog = false },
      onCreate = { title, desc, prize, region, imageUrl, presetArt, maxTickets ->
        viewModel.createGiveaway(title, desc, prize, region, imageUrl, presetArt, maxTickets)
        showCreateGiveawayDialog = false
      },
    )
  }

  giveawayToEditLimit?.let { gw ->
    EditTicketLimitDialog(
      giveaway = gw,
      onDismiss = { giveawayToEditLimit = null },
      onSave = { newMax ->
        viewModel.updateGiveawayLimit(gw, newMax)
        giveawayToEditLimit = null
      },
    )
  }

  if (showCreateCampaignDialog) {
    CreateCampaignDialog(
      onDismiss = { showCreateCampaignDialog = false },
      onCreate = { title, desc, category, region, goal ->
        viewModel.createDonationCampaign(title, desc, category, region, goal)
        showCreateCampaignDialog = false
      },
    )
  }

  userToGiveMoney?.let { user ->
    GiveMoneyDialog(
      user = user,
      onDismiss = { userToGiveMoney = null },
      onSend = { pkrAmount, bonusCoins, method, desc, imageUrl ->
        viewModel.giveMoneyToUser(user, pkrAmount, bonusCoins, method, desc, imageUrl)
        userToGiveMoney = null
      },
    )
  }

  if (showCreateProofDialog) {
    CreateProofDialog(
      onDismiss = { showCreateProofDialog = false },
      onPublish = { title, proofType, recipient, amountDisplay, pkrAmount, region, desc, imageUrl, presetArt ->
        viewModel.createPayoutProof(title, proofType, recipient, amountDisplay, pkrAmount, region, desc, imageUrl, presetArt)
        showCreateProofDialog = false
      },
    )
  }

  reportToReply?.let { report ->
    ReplyReportDialog(
      report = report,
      onDismiss = { reportToReply = null },
      onSubmit = { status, reply ->
        viewModel.replyToProblemReport(report.id, status, reply)
        reportToReply = null
      },
    )
  }
}

@Composable
private fun CreateGiveawayDialog(
  onDismiss: () -> Unit,
  onCreate: (String, String, String, String, String, String, Long) -> Unit,
) {
  var title by remember { mutableStateOf("") }
  var prize by remember { mutableStateOf("") }
  var desc by remember { mutableStateOf("") }
  var region by remember { mutableStateOf("All Pakistan") }
  var imageUrl by remember { mutableStateOf("") }
  var presetArt by remember { mutableStateOf("giveaway_gift") }
  var maxText by remember { mutableStateOf("100") }

  val presets = listOf(
    "giveaway_gift" to "Gift Banner",
    "money_payout" to "Charity & Cash",
    "heritage_name" to "Heritage Art",
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.create_giveaway_action)) },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = "Ticket price is fixed at 10 Giveaway Coins per ticket. Set Ticket Limit to 0 for Unlimited (∞).",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.primary,
        )
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title *") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = prize, onValueChange = { prize = it }, label = { Text("Prize *") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description *") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = region, onValueChange = { region = it }, label = { Text("Region Tag") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
          value = maxText,
          onValueChange = { maxText = it.filter { c -> c.isDigit() } },
          label = { Text("Ticket Limit (0 = Unlimited ∞)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = imageUrl,
          onValueChange = { imageUrl = it },
          label = { Text("Giveaway Picture URL (optional)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(presets) { (key, label) ->
            FilterChip(
              selected = presetArt == key,
              onClick = { presetArt = key },
              label = { Text(label) },
            )
          }
        }
      }
    },
    confirmButton = {
      val max = maxText.toLongOrNull() ?: 0L
      Button(
        onClick = { onCreate(title, desc, prize, region, imageUrl, presetArt, max) },
        enabled = title.trim().length >= 3 && prize.trim().length >= 2 && desc.trim().length >= 3 && max >= 0L,
      ) {
        Text("Publish")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
private fun EditTicketLimitDialog(
  giveaway: Giveaway,
  onDismiss: () -> Unit,
  onSave: (Long) -> Unit,
) {
  var maxText by remember { mutableStateOf(giveaway.maxEntries.toString()) }
  val parsed = maxText.toLongOrNull()

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Set Ticket Limit") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "${giveaway.title} • Already sold: ${giveaway.entriesCount} tickets. Enter 0 for Unlimited (∞) or at least ${giveaway.entriesCount}.",
          style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
          value = maxText,
          onValueChange = { maxText = it.filter { c -> c.isDigit() } },
          label = { Text("New Ticket Limit (0 = ∞)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { if (parsed != null) onSave(parsed) },
        enabled = parsed != null && (parsed == 0L || parsed >= giveaway.entriesCount),
      ) {
        Text("Save Limit")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
private fun CreateCampaignDialog(
  onDismiss: () -> Unit,
  onCreate: (String, String, String, String, Long) -> Unit,
) {
  var title by remember { mutableStateOf("") }
  var desc by remember { mutableStateOf("") }
  var category by remember { mutableStateOf("Food Relief") }
  var region by remember { mutableStateOf("Punjab") }
  var goalText by remember { mutableStateOf("2000") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.create_campaign_action)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Campaign Title *") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description *") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = region, onValueChange = { region = it }, label = { Text("Region") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
          value = goalText,
          onValueChange = { goalText = it.filter { c -> c.isDigit() } },
          label = { Text("Goal (Coins)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      val goal = goalText.toLongOrNull() ?: 1000L
      Button(
        onClick = { onCreate(title, desc, category, region, goal) },
        enabled = title.trim().length >= 3 && desc.trim().length >= 3 && goal >= 10,
      ) {
        Text("Launch")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
private fun GiveMoneyDialog(
  user: UserProfile,
  onDismiss: () -> Unit,
  onSend: (Long, Long, String, String, String) -> Unit,
) {
  var pkrText by remember { mutableStateOf("1000") }
  var coinsText by remember { mutableStateOf("0") }
  var method by remember { mutableStateOf("Easypaisa / JazzCash") }
  var desc by remember { mutableStateOf("Winner & community support payout verified by PakGives Admin.") }
  var imageUrl by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Give Money to ${user.displayName}") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = "This updates ${user.displayName}'s Money Received balance and publishes a verified Payout Proof in the Donate → Proofs & Help tab.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.primary,
        )
        OutlinedTextField(
          value = pkrText,
          onValueChange = { pkrText = it.filter { c -> c.isDigit() } },
          label = { Text("Money Amount (PKR)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = coinsText,
          onValueChange = { coinsText = it.filter { c -> c.isDigit() } },
          label = { Text("Optional Bonus Coins") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(value = method, onValueChange = { method = it }, label = { Text("Payment Method") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Proof Note / Transaction Info") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = imageUrl, onValueChange = { imageUrl = it }, label = { Text("Receipt Picture URL (optional)") }, modifier = Modifier.fillMaxWidth())
      }
    },
    confirmButton = {
      val pkr = pkrText.toLongOrNull() ?: 0L
      val bonus = coinsText.toLongOrNull() ?: 0L
      Button(
        onClick = { onSend(pkr, bonus, method, desc, imageUrl) },
        enabled = pkr > 0L || bonus > 0L,
      ) {
        Text("Send & Post Proof")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
private fun CreateProofDialog(
  onDismiss: () -> Unit,
  onPublish: (String, String, String, String, Long, String, String, String, String) -> Unit,
) {
  val proofTypes = listOf("Helping Donation", "Giveaway Proof", "Money Payout")
  var selectedType by remember { mutableStateOf(proofTypes.first()) }
  var title by remember { mutableStateOf("") }
  var recipient by remember { mutableStateOf("") }
  var pkrText by remember { mutableStateOf("5000") }
  var amountDisplay by remember { mutableStateOf("") }
  var region by remember { mutableStateOf("All Pakistan") }
  var desc by remember { mutableStateOf("") }
  var imageUrl by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add Proof / Helping Donation") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(proofTypes) { type ->
            FilterChip(
              selected = selectedType == type,
              onClick = { selectedType = type },
              label = { Text(type) },
            )
          }
        }
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title *") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = recipient, onValueChange = { recipient = it }, label = { Text("Recipient / Families Helped *") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
          value = pkrText,
          onValueChange = { pkrText = it.filter { c -> c.isDigit() } },
          label = { Text("PKR Value") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = amountDisplay,
          onValueChange = { amountDisplay = it },
          label = { Text("Display Label (e.g. PKR 5,000 Aid)") },
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(value = region, onValueChange = { region = it }, label = { Text("Region") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Proof Details *") }, minLines = 2, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = imageUrl, onValueChange = { imageUrl = it }, label = { Text("Proof Photo URL (optional)") }, modifier = Modifier.fillMaxWidth())
      }
    },
    confirmButton = {
      val pkr = pkrText.toLongOrNull() ?: 0L
      val presetArt = if (selectedType == "Giveaway Proof") "giveaway_gift" else "helping_hand"
      val label = amountDisplay.ifBlank { "PKR $pkr" }
      Button(
        onClick = { onPublish(title, selectedType, recipient, label, pkr, region, desc, imageUrl, presetArt) },
        enabled = title.trim().length >= 3 && recipient.trim().isNotEmpty() && desc.trim().length >= 3,
      ) {
        Text("Publish")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
private fun ReplyReportDialog(
  report: ProblemReport,
  onDismiss: () -> Unit,
  onSubmit: (String, String) -> Unit,
) {
  val statuses = listOf("open", "in_review", "resolved")
  var selectedStatus by remember { mutableStateOf("resolved") }
  var reply by remember { mutableStateOf(report.adminReply) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Reply to ${report.reporterName}") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(report.subject, style = MaterialTheme.typography.titleSmall)
        Text(report.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          statuses.forEach { st ->
            FilterChip(
              selected = selectedStatus == st,
              onClick = { selectedStatus = st },
              label = { Text(st) },
            )
          }
        }
        OutlinedTextField(
          value = reply,
          onValueChange = { reply = it },
          label = { Text("Admin Reply") },
          minLines = 2,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(onClick = { onSubmit(selectedStatus, reply) }) {
        Text("Save Reply")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}
