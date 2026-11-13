package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.catalog.RegionalNameCatalog
import com.example.data.model.AppConfig
import com.example.data.model.CoinOrder
import com.example.data.model.CoinPackage
import com.example.data.model.CuratedRegionalName
import com.example.data.model.DonationCampaign
import com.example.data.model.DonationRecord
import com.example.data.model.Giveaway
import com.example.data.model.GiveawayEntry
import com.example.data.model.PayoutProof
import com.example.data.model.ProblemReport
import com.example.data.model.RegionalName
import com.example.data.model.UserProfile
import com.example.data.repository.PakGivesRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

data class DisplayNameItem(
  val id: String,
  val name: String,
  val script: String,
  val pronunciation: String,
  val originRegion: String,
  val gender: String,
  val meaning: String,
  val culturalNote: String,
  val isCommunity: Boolean,
  val isFeatured: Boolean,
  val authorName: String,
  val authorUserId: String,
  val likesCount: Long,
)

enum class TransactionDirection { CREDIT, DEBIT, PENDING }

enum class TransactionCategory(val label: String) {
  ALL("All"),
  COINS("Coins"),
  GIVEAWAYS("Giveaways"),
  DONATIONS("Donations"),
  PAYOUTS("Payouts"),
}

data class WalletTransactionItem(
  val id: String,
  val category: TransactionCategory,
  val direction: TransactionDirection,
  val title: String,
  val subtitle: String,
  val amountText: String,
  val statusLabel: String,
  val timestampSeconds: Long,
)

data class GiveawayParticipationItem(
  val giveaway: Giveaway,
  val totalTickets: Long,
  val totalCoinsSpent: Long,
  val purchaseCount: Int,
  val latestTimestampSeconds: Long,
  val isCurrentUserWinner: Boolean,
)

class PakGivesViewModel(
  private val repository: PakGivesRepository,
  val currentUserId: String,
  val currentUserName: String,
  val currentUserEmail: String,
) : ViewModel() {

  private val _statusMessage = MutableStateFlow<String?>(null)
  val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

  private val _isAdmin = MutableStateFlow(false)
  val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

  private val _selectedRegion = MutableStateFlow("All Regions")
  val selectedRegion: StateFlow<String> = _selectedRegion.asStateFlow()

  private val _selectedGender = MutableStateFlow("All")
  val selectedGender: StateFlow<String> = _selectedGender.asStateFlow()

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _favoritesOnly = MutableStateFlow(false)
  val favoritesOnly: StateFlow<Boolean> = _favoritesOnly.asStateFlow()

  private val _spotlightName = MutableStateFlow<DisplayNameItem?>(null)
  val spotlightName: StateFlow<DisplayNameItem?> = _spotlightName.asStateFlow()

  private val _isBusy = MutableStateFlow(false)
  val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

  init {
    viewModelScope.launch {
      repository.ensureUserProfile(currentUserId, currentUserName, currentUserEmail)
        .onFailure { err ->
          Log.w(TAG, "Profile init warning: ${err.message}")
        }
      val adminStatus = repository.checkIsAdmin(currentUserId, currentUserEmail)
      _isAdmin.value = adminStatus
      if (adminStatus) {
        repository.seedStarterData()
      }
    }
    suggestRandomName()
  }

  private fun <T> Flow<T>.toUiState(errorLabel: String): StateFlow<UiState<T>> =
    this.map<T, UiState<T>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, errorLabel, rootError)
        emit(UiState.Error(rootError.message ?: errorLabel))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
    .catch { error ->
      val firestoreError = generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
      if (firestoreError == null && error is CancellationException) throw error
      Log.w(TAG, "Profile stream error", firestoreError ?: error)
      emit(null)
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

  val favoriteNameIds: StateFlow<Set<String>> = repository.observeFavoriteNames(currentUserId)
    .map { list -> list.map { it.id }.toSet() }
    .catch { emit(emptySet()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptySet())

  private val communityNames: StateFlow<List<RegionalName>> = repository.observeCommunityNames()
    .catch { error ->
      val firestoreError = generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
      if (firestoreError == null && error is CancellationException) throw error
      Log.w(TAG, "Community names stream warning", firestoreError ?: error)
      emit(emptyList())
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

  val filteredNames: StateFlow<List<DisplayNameItem>> = combine(
    communityNames,
    _selectedRegion,
    _selectedGender,
    _searchQuery,
    _favoritesOnly,
    favoriteNameIds,
  ) { values ->
    @Suppress("UNCHECKED_CAST")
    val remote = values[0] as List<RegionalName>
    val region = values[1] as String
    val gender = values[2] as String
    val query = (values[3] as String).trim().lowercase()
    val favOnly = values[4] as Boolean
    @Suppress("UNCHECKED_CAST")
    val favIds = values[5] as Set<String>

    val combined = buildAllNames(remote)
    combined.filter { item ->
      val matchesRegion = region == "All Regions" || item.originRegion.equals(region, ignoreCase = true)
      val matchesGender = gender == "All" || item.gender.equals(gender, ignoreCase = true)
      val matchesFav = !favOnly || favIds.contains(item.id)
      val matchesQuery = query.isBlank() ||
        item.name.lowercase().contains(query) ||
        item.meaning.lowercase().contains(query) ||
        item.originRegion.lowercase().contains(query) ||
        item.culturalNote.lowercase().contains(query)
      matchesRegion && matchesGender && matchesFav && matchesQuery
    }
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
    initialValue = buildAllNames(emptyList()),
  )

  val giveawaysState: StateFlow<UiState<List<Giveaway>>> =
    repository.observeGiveaways().toUiState("Failed to load giveaways")

  val campaignsState: StateFlow<UiState<List<DonationCampaign>>> =
    repository.observeDonationCampaigns().toUiState("Failed to load donation campaigns")

  val publishedProofs: StateFlow<List<PayoutProof>> = repository.observePublishedProofs()
    .catch { emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

  val myDonations: StateFlow<List<DonationRecord>> = repository.observeMyDonations(currentUserId)
    .catch { emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

  val myCoinOrders: StateFlow<List<CoinOrder>> = repository.observeMyCoinOrders(currentUserId)
    .catch { emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

  val myProblemReports: StateFlow<List<ProblemReport>> = repository.observeMyProblemReports(currentUserId)
    .catch { emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

  val appConfig: StateFlow<AppConfig> = repository.observeAppConfig()
    .catch { emit(RegionalNameCatalog.defaultAppConfig) }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = RegionalNameCatalog.defaultAppConfig,
    )

  @OptIn(ExperimentalCoroutinesApi::class)
  val adminCoinOrders: StateFlow<List<CoinOrder>> = _isAdmin
    .flatMapLatest { admin ->
      if (admin) repository.observeAllCoinOrders().catch { emit(emptyList()) }
      else flowOf(emptyList())
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

  @OptIn(ExperimentalCoroutinesApi::class)
  val adminUsers: StateFlow<List<UserProfile>> = _isAdmin
    .flatMapLatest { admin ->
      if (admin) repository.observeAllUsers().catch { emit(emptyList()) }
      else flowOf(emptyList())
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

  @OptIn(ExperimentalCoroutinesApi::class)
  val adminProblemReports: StateFlow<List<ProblemReport>> = _isAdmin
    .flatMapLatest { admin ->
      if (admin) repository.observeAllProblemReports().catch { emit(emptyList()) }
      else flowOf(emptyList())
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

  @OptIn(ExperimentalCoroutinesApi::class)
  private val userGiveawayEntries: StateFlow<List<Pair<Giveaway, GiveawayEntry>>> = giveawaysState
    .flatMapLatest { state ->
      when (state) {
        is UiState.Success -> {
          val giveaways = state.data
          if (giveaways.isEmpty()) {
            flowOf(emptyList())
          } else {
            val perGiveawayFlows = giveaways.map { gw ->
              repository.observeUserEntriesForGiveaway(gw.id, currentUserId)
                .catch { emit(emptyList()) }
                .map { entries -> entries.map { gw to it } }
            }
            combine(perGiveawayFlows) { arrays ->
              arrays.flatMap { it }
            }
          }
        }
        else -> flowOf(emptyList())
      }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

  val giveawayParticipationHistory: StateFlow<List<GiveawayParticipationItem>> = userGiveawayEntries
    .map { pairs ->
      pairs
        .groupBy { (giveaway, _) -> giveaway.id }
        .values
        .mapNotNull { group ->
          val giveaway = group.firstOrNull()?.first ?: return@mapNotNull null
          val entries = group.map { it.second }
          val totalTickets = entries.sumOf { it.ticketCount.coerceAtLeast(1L) }
          val totalCoinsSpent = entries.sumOf {
            if (it.coinsSpent > 0L) it.coinsSpent else it.ticketCount.coerceAtLeast(1L) * giveaway.entryCostCoins
          }
          val latestTs = entries.maxOfOrNull { it.createdAt?.seconds ?: 0L } ?: (giveaway.createdAt?.seconds ?: 0L)
          GiveawayParticipationItem(
            giveaway = giveaway,
            totalTickets = totalTickets,
            totalCoinsSpent = totalCoinsSpent,
            purchaseCount = entries.size,
            latestTimestampSeconds = latestTs,
            isCurrentUserWinner = giveaway.status == "completed" && giveaway.winnerUserId == currentUserId,
          )
        }
        .sortedByDescending { it.latestTimestampSeconds }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

  val walletTransactions: StateFlow<List<WalletTransactionItem>> = combine(
    myCoinOrders,
    userGiveawayEntries,
    myDonations,
    publishedProofs,
  ) { orders, entries, donations, proofs ->
    val items = mutableListOf<WalletTransactionItem>()

    orders.forEach { order ->
      val direction = when (order.status) {
        "approved" -> TransactionDirection.CREDIT
        "pending" -> TransactionDirection.PENDING
        else -> TransactionDirection.DEBIT
      }
      val prefix = when (order.status) {
        "approved" -> "+"
        else -> ""
      }
      val statusText = when (order.status) {
        "approved" -> "Approved"
        "pending" -> "Pending"
        "rejected" -> "Rejected"
        else -> order.status.replaceFirstChar { it.uppercase() }
      }
      val noteSuffix = if (order.adminNote.isNotBlank()) " • ${order.adminNote}" else ""
      items.add(
        WalletTransactionItem(
          id = "ord_${order.id}",
          category = TransactionCategory.COINS,
          direction = direction,
          title = "Coin Purchase • ${order.packageName}",
          subtitle = "Ref: ${order.whatsappRef} (${order.priceDisplay})$noteSuffix",
          amountText = "$prefix${order.coinsAmount} Coins",
          statusLabel = statusText,
          timestampSeconds = order.updatedAt?.seconds ?: order.createdAt?.seconds ?: 0L,
        )
      )
    }

    entries.forEach { (giveaway, entry) ->
      val cost = if (entry.coinsSpent > 0L) entry.coinsSpent else entry.ticketCount * giveaway.entryCostCoins
      val ticketWord = if (entry.ticketCount == 1L) "ticket" else "tickets"
      items.add(
        WalletTransactionItem(
          id = "ent_${entry.id}",
          category = TransactionCategory.GIVEAWAYS,
          direction = TransactionDirection.DEBIT,
          title = "Giveaway Tickets • ${giveaway.title}",
          subtitle = "${entry.ticketCount} $ticketWord @ ${giveaway.entryCostCoins} Coins/ticket",
          amountText = "-$cost Coins",
          statusLabel = if (giveaway.status == "active") "Entered" else "Drawn",
          timestampSeconds = entry.createdAt?.seconds ?: 0L,
        )
      )
    }

    donations.forEach { don ->
      val notePart = if (don.message.isNotBlank()) " • \"${don.message}\"" else ""
      items.add(
        WalletTransactionItem(
          id = "don_${don.id}",
          category = TransactionCategory.DONATIONS,
          direction = TransactionDirection.DEBIT,
          title = "Donation • ${don.campaignTitle}",
          subtitle = "Pure charity (no tickets)$notePart",
          amountText = "-${don.coinsDonated} Coins",
          statusLabel = "Donated",
          timestampSeconds = don.createdAt?.seconds ?: 0L,
        )
      )
    }

    proofs.filter { it.recipientUserId == currentUserId }.forEach { proof ->
      items.add(
        WalletTransactionItem(
          id = "prf_${proof.id}",
          category = TransactionCategory.PAYOUTS,
          direction = TransactionDirection.CREDIT,
          title = proof.title.ifBlank { proof.proofType },
          subtitle = proof.description.ifBlank { "Verified admin payout" },
          amountText = "+${proof.amountDisplay}",
          statusLabel = "Credited",
          timestampSeconds = proof.createdAt?.seconds ?: 0L,
        )
      )
    }

    items.sortedByDescending { it.timestampSeconds }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

  fun observeUserTicketCount(giveawayId: String): Flow<Long> =
    repository.observeUserTicketCountForGiveaway(giveawayId, currentUserId).catch { emit(0L) }

  private fun buildAllNames(remote: List<RegionalName>): List<DisplayNameItem> {
    val communityItems = remote.map {
      DisplayNameItem(
        id = it.id,
        name = it.name,
        script = it.script,
        pronunciation = it.pronunciation,
        originRegion = it.originRegion,
        gender = it.gender,
        meaning = it.meaning,
        culturalNote = it.culturalNote,
        isCommunity = true,
        isFeatured = it.status == "featured",
        authorName = it.authorName,
        authorUserId = it.userId,
        likesCount = it.likesCount,
      )
    }
    val curatedItems = RegionalNameCatalog.curatedNames.map { c: CuratedRegionalName ->
      DisplayNameItem(
        id = c.id,
        name = c.name,
        script = c.script,
        pronunciation = c.pronunciation,
        originRegion = c.originRegion,
        gender = c.gender,
        meaning = c.meaning,
        culturalNote = c.culturalNote,
        isCommunity = false,
        isFeatured = false,
        authorName = "PakGives Heritage",
        authorUserId = "",
        likesCount = 0L,
      )
    }
    return communityItems + curatedItems
  }

  fun selectRegion(region: String) {
    _selectedRegion.value = region
    suggestRandomName()
  }

  fun selectGender(gender: String) {
    _selectedGender.value = gender
    suggestRandomName()
  }

  fun updateSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun toggleFavoritesOnly() {
    _favoritesOnly.value = !_favoritesOnly.value
  }

  fun suggestRandomName() {
    val region = _selectedRegion.value
    val gender = _selectedGender.value
    val pool = buildAllNames(communityNames.value).filter { item ->
      val rOk = region == "All Regions" || item.originRegion.equals(region, ignoreCase = true)
      val gOk = gender == "All" || item.gender.equals(gender, ignoreCase = true)
      rOk && gOk
    }
    val candidates = pool.ifEmpty { buildAllNames(communityNames.value) }
    val currentId = _spotlightName.value?.id
    val next = if (candidates.size > 1) {
      candidates.filter { it.id != currentId }.random()
    } else {
      candidates.firstOrNull()
    }
    _spotlightName.value = next
  }

  fun toggleFavorite(item: DisplayNameItem) {
    val currentlyFav = favoriteNameIds.value.contains(item.id)
    viewModelScope.launch {
      repository.toggleFavoriteName(
        userId = currentUserId,
        nameId = item.id,
        name = item.name,
        originRegion = item.originRegion,
        gender = item.gender,
        meaning = item.meaning,
        currentlyFavorite = currentlyFav,
      ).onSuccess {
        _statusMessage.value = if (currentlyFav) {
          "Removed ${item.name} from favorites"
        } else {
          "Saved ${item.name} to favorites"
        }
      }.onFailure { e ->
        _statusMessage.value = e.localizedMessage ?: "Failed to update favorite"
      }
    }
  }

  fun submitRegionalName(
    name: String,
    script: String,
    pronunciation: String,
    originRegion: String,
    gender: String,
    meaning: String,
    culturalNote: String,
  ) {
    if (name.trim().isEmpty() || meaning.trim().length < 2) {
      _statusMessage.value = "Please enter both a name and its meaning."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      repository.submitRegionalName(
        name = name,
        script = script,
        pronunciation = pronunciation,
        originRegion = originRegion,
        gender = gender,
        meaning = meaning,
        culturalNote = culturalNote,
        authorName = userProfile.value?.displayName ?: currentUserName,
      ).onSuccess {
        _statusMessage.value = "Shared '$name' with the PakGives community!"
      }.onFailure { e ->
        _statusMessage.value = e.localizedMessage ?: "Could not submit name"
      }
      _isBusy.value = false
    }
  }

  fun likeCommunityName(nameId: String) {
    viewModelScope.launch {
      repository.likeRegionalName(nameId)
        .onFailure { e -> _statusMessage.value = e.localizedMessage ?: "Could not like name" }
    }
  }

  fun deleteCommunityName(nameId: String) {
    viewModelScope.launch {
      repository.deleteRegionalName(nameId)
        .onSuccess { _statusMessage.value = "Name removed." }
        .onFailure { e -> _statusMessage.value = e.localizedMessage ?: "Could not delete name" }
    }
  }

  fun buyGiveawayTickets(giveaway: Giveaway, ticketCount: Long) {
    if (ticketCount < 1L) {
      _statusMessage.value = "Select at least 1 ticket."
      return
    }
    if (giveaway.maxEntries > 0L) {
      val remaining = giveaway.maxEntries - giveaway.entriesCount
      if (ticketCount > remaining) {
        _statusMessage.value = "Only $remaining tickets left in this giveaway."
        return
      }
    }
    val totalCost = ticketCount * giveaway.entryCostCoins
    val balance = userProfile.value?.coinBalance ?: 0L
    if (balance < totalCost) {
      _statusMessage.value = "Need $totalCost Giveaway Coins for $ticketCount ticket(s). Buy coins via WhatsApp!"
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      val name = userProfile.value?.displayName ?: currentUserName
      repository.enterGiveaway(giveaway, ticketCount, currentUserId, name)
        .onSuccess {
          _statusMessage.value = "Bought $ticketCount ticket(s) for '${giveaway.title}'! Good luck!"
        }
        .onFailure { e ->
          _statusMessage.value = e.localizedMessage ?: "Could not buy tickets"
        }
      _isBusy.value = false
    }
  }

  fun donateToCampaign(campaign: DonationCampaign, coins: Long, note: String) {
    val balance = userProfile.value?.coinBalance ?: 0L
    if (coins <= 0L) {
      _statusMessage.value = "Enter a valid coin amount."
      return
    }
    if (balance < coins) {
      _statusMessage.value = "Insufficient Giveaway Coins. Top up via WhatsApp group!"
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      val donorName = userProfile.value?.displayName ?: currentUserName
      repository.donateToCampaign(campaign, coins, note, currentUserId, donorName)
        .onSuccess {
          _statusMessage.value = "Donated $coins coins to ${campaign.title} (pure charity, no tickets). Thank you!"
        }
        .onFailure { e ->
          _statusMessage.value = e.localizedMessage ?: "Donation failed"
        }
      _isBusy.value = false
    }
  }

  fun submitCoinOrder(pkg: CoinPackage, whatsappRef: String) {
    if (whatsappRef.trim().isEmpty()) {
      _statusMessage.value = "Please enter your WhatsApp name or payment reference."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      val buyerName = userProfile.value?.displayName ?: currentUserName
      repository.submitCoinOrder(currentUserId, buyerName, pkg, whatsappRef)
        .onSuccess {
          _statusMessage.value = "Order submitted! An admin will verify and credit ${pkg.coins} coins soon."
        }
        .onFailure { e ->
          _statusMessage.value = e.localizedMessage ?: "Failed to submit coin order"
        }
      _isBusy.value = false
    }
  }

  fun updateDisplayName(newName: String, favoriteRegions: List<String>? = null) {
    if (newName.trim().isEmpty()) {
      _statusMessage.value = "Display name cannot be empty."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      repository.updateUserProfileDetails(currentUserId, newName, favoriteRegions)
        .onSuccess { _statusMessage.value = "Profile updated!" }
        .onFailure { e -> _statusMessage.value = e.localizedMessage ?: "Profile update failed" }
      _isBusy.value = false
    }
  }

  fun submitProblemReport(category: String, subject: String, description: String) {
    viewModelScope.launch {
      _isBusy.value = true
      val reporterName = userProfile.value?.displayName ?: currentUserName
      repository.submitProblemReport(currentUserId, reporterName, category, subject, description)
        .onSuccess { _statusMessage.value = "Problem report sent to PakGives Admin!" }
        .onFailure { e -> _statusMessage.value = e.localizedMessage ?: "Could not submit report" }
      _isBusy.value = false
    }
  }

  fun acceptTerms() {
    viewModelScope.launch {
      repository.acceptTerms(currentUserId)
        .onSuccess { _statusMessage.value = "Terms & Policies accepted." }
        .onFailure { e -> _statusMessage.value = e.localizedMessage ?: "Could not save acceptance" }
    }
  }

  fun unlockAdmin(passcode: String, onSuccess: () -> Unit) {
    viewModelScope.launch {
      _isBusy.value = true
      repository.unlockAdminWithPasscode(currentUserId, passcode)
        .onSuccess {
          _isAdmin.value = true
          repository.seedStarterData()
          _statusMessage.value = "Admin Panel unlocked!"
          onSuccess()
        }
        .onFailure { e ->
          _statusMessage.value = e.localizedMessage ?: "Invalid admin password"
        }
      _isBusy.value = false
    }
  }

  // --- ADMIN ACTIONS ---
  fun approveCoinOrder(order: CoinOrder) {
    viewModelScope.launch {
      _isBusy.value = true
      repository.approveCoinOrder(order)
        .onSuccess { _statusMessage.value = "Credited ${order.coinsAmount} coins to ${order.buyerName}" }
        .onFailure { e -> _statusMessage.value = e.localizedMessage ?: "Approval failed" }
      _isBusy.value = false
    }
  }

  fun rejectCoinOrder(order: CoinOrder) {
    viewModelScope.launch {
      _isBusy.value = true
      repository.rejectCoinOrder(order)
        .onSuccess { _statusMessage.value = "Order rejected" }
        .onFailure { e -> _statusMessage.value = e.localizedMessage ?: "Reject failed" }
      _isBusy.value = false
    }
  }

  fun createGiveaway(
    title: String,
    description: String,
    prizeTitle: String,
    regionTag: String,
    imageUrl: String,
    presetArt: String,
    maxEntries: Long,
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      repository.createGiveaway(title, description, prizeTitle, regionTag, imageUrl, presetArt, maxEntries)
        .onSuccess { _statusMessage.value = "Giveaway '$title' published (10 Coins/ticket)!" }
        .onFailure { e -> _statusMessage.value = e.localizedMessage ?: "Failed to create giveaway" }
      _isBusy.value = false
    }
  }

  fun updateGiveawayLimit(giveaway: Giveaway, newMaxEntries: Long) {
    viewModelScope.launch {
      _isBusy.value = true
      repository.updateGiveawayTicketLimit(giveaway, newMaxEntries)
        .onSuccess {
          val label = if (newMaxEntries == 0L) "Unlimited (∞)" else "$newMaxEntries tickets"
          _statusMessage.value = "Updated '${giveaway.title}' limit to $label"
        }
        .onFailure { e -> _statusMessage.value = e.localizedMessage ?: "Update failed" }
      _isBusy.value = false
    }
  }

  fun drawGiveawayWinner(giveaway: Giveaway) {
    viewModelScope.launch {
      _isBusy.value = true
      repository.drawGiveawayWinner(giveaway)
        .onSuccess { winner -> _statusMessage.value = "Winner drawn: $winner!" }
        .onFailure { e -> _statusMessage.value = e.localizedMessage ?: "Could not draw winner" }
      _isBusy.value = false
    }
  }

  fun deleteGiveaway(giveawayId: String) {
    viewModelScope.launch {
      repository.deleteGiveaway(giveawayId)
        .onSuccess { _statusMessage.value = "Giveaway removed" }
        .onFailure { e -> _statusMessage.value = e.localizedMessage ?: "Delete failed" }
    }
  }

  fun createDonationCampaign(
    title: String,
    description: String,
    category: String,
    region: String,
    goalCoins: Long,
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      repository.createDonationCampaign(title, description, category, region, goalCoins)
        .onSuccess { _statusMessage.value = "Donation campaign '$title' launched!" }
        .onFailure { e -> _statusMessage.value = e.localizedMessage ?: "Failed to create campaign" }
      _isBusy.value = false
    }
  }

  fun giveMoneyToUser(
    targetUser: UserProfile,
    pkrAmount: Long,
    bonusCoins: Long,
    paymentMethod: String,
    description: String,
    imageUrl: String,
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      repository.giveMoneyToUser(
        targetUserId = targetUser.userId,
        targetUserName = targetUser.displayName,
        pkrAmount = pkrAmount,
        bonusCoins = bonusCoins,
        paymentMethod = paymentMethod,
        description = description,
        imageUrl = imageUrl,
      )
        .onSuccess { _statusMessage.value = "Payout & proof recorded for ${targetUser.displayName}!" }
        .onFailure { e -> _statusMessage.value = e.localizedMessage ?: "Payout failed" }
      _isBusy.value = false
    }
  }

  fun createPayoutProof(
    title: String,
    proofType: String,
    recipientName: String,
    amountDisplay: String,
    pkrAmount: Long,
    region: String,
    description: String,
    imageUrl: String,
    presetArt: String,
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      repository.createPayoutProof(
        title = title,
        proofType = proofType,
        recipientName = recipientName,
        amountDisplay = amountDisplay,
        pkrAmount = pkrAmount,
        region = region,
        description = description,
        imageUrl = imageUrl,
        presetArt = presetArt,
      )
        .onSuccess { _statusMessage.value = "Published '$title' to Proofs & Helping Donations!" }
        .onFailure { e -> _statusMessage.value = e.localizedMessage ?: "Failed to publish proof" }
      _isBusy.value = false
    }
  }

  fun deletePayoutProof(proofId: String) {
    viewModelScope.launch {
      repository.deletePayoutProof(proofId)
        .onSuccess { _statusMessage.value = "Proof removed" }
        .onFailure { e -> _statusMessage.value = e.localizedMessage ?: "Delete failed" }
    }
  }

  fun replyToProblemReport(reportId: String, status: String, reply: String) {
    viewModelScope.launch {
      _isBusy.value = true
      repository.replyToProblemReport(reportId, status, reply)
        .onSuccess { _statusMessage.value = "Report updated ($status)" }
        .onFailure { e -> _statusMessage.value = e.localizedMessage ?: "Update failed" }
      _isBusy.value = false
    }
  }

  fun updateAppConfig(config: AppConfig) {
    viewModelScope.launch {
      _isBusy.value = true
      repository.updateAppConfig(config)
        .onSuccess { _statusMessage.value = "WhatsApp & Policy settings updated!" }
        .onFailure { e -> _statusMessage.value = e.localizedMessage ?: "Failed to save settings" }
      _isBusy.value = false
    }
  }

  fun creditUserCoins(userId: String, amount: Long) {
    viewModelScope.launch {
      _isBusy.value = true
      repository.creditUserCoins(userId, amount)
        .onSuccess { _statusMessage.value = "Added $amount coins!" }
        .onFailure { e -> _statusMessage.value = e.localizedMessage ?: "Credit failed" }
      _isBusy.value = false
    }
  }

  fun seedStarterData() {
    viewModelScope.launch {
      _isBusy.value = true
      repository.seedStarterData()
        .onSuccess { _statusMessage.value = "Starter giveaways, campaigns, proofs & config synced!" }
        .onFailure { e -> _statusMessage.value = e.localizedMessage ?: "Seed failed" }
      _isBusy.value = false
    }
  }

  fun clearStatusMessage() {
    _statusMessage.value = null
  }

  private companion object {
    const val TAG = "PakGivesVM"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
