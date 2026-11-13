package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.catalog.RegionalNameCatalog
import com.example.data.model.AppConfig
import com.example.data.model.CoinOrder
import com.example.data.model.CoinPackage
import com.example.data.model.DonationCampaign
import com.example.data.model.DonationRecord
import com.example.data.model.FavoriteName
import com.example.data.model.Giveaway
import com.example.data.model.GiveawayEntry
import com.example.data.model.PayoutProof
import com.example.data.model.ProblemReport
import com.example.data.model.RegionalName
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.random.Random

class PakGivesRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    db = FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  private fun <T> Flow<T>.logFirestoreErrors(
    operationType: OperationType,
    path: String,
    expectedUid: String? = null,
  ): Flow<T> = catch { error ->
    val firestoreError = generateSequence(error) { it.cause }
      .filterIsInstance<FirebaseFirestoreException>()
      .firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    val exceptionToLog = firestoreError ?: (error as? Exception)
    if (exceptionToLog != null && (expectedUid == null || auth.currentUser?.uid == expectedUid)) {
      handleFirestoreError(exceptionToLog, operationType, path)
    }
    throw error
  }

  // --- USER PROFILE & SIGN-UP ---
  suspend fun ensureUserProfile(
    userId: String,
    displayName: String,
    email: String,
  ): Result<UserProfile> = runCatching {
    val uid = requireUserId()
    require(uid == userId) { "Authenticated user mismatch" }
    val docRef = db.collection("users").document(uid)
    try {
      val snapshot = docRef.get().await()
      if (snapshot.exists()) {
        snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          ?: UserProfile(id = uid, userId = uid, displayName = displayName, email = email)
      } else {
        val cleanName = displayName.trim().ifBlank { "PakGives Member" }.take(60)
        val cleanEmail = email.trim().take(120)
        val payload = mapOf(
          "userId" to uid,
          "displayName" to cleanName,
          "email" to cleanEmail,
          "coinBalance" to 0L,
          "totalDonatedCoins" to 0L,
          "totalMoneyReceived" to 0L,
          "favoriteRegions" to emptyList<String>(),
          "acceptedTerms" to false,
          "role" to "user",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        docRef.set(payload).await()
        UserProfile(
          id = uid,
          userId = uid,
          displayName = cleanName,
          email = cleanEmail,
          coinBalance = 0L,
          totalDonatedCoins = 0L,
          totalMoneyReceived = 0L,
          favoriteRegions = emptyList(),
          acceptedTerms = false,
          role = "user",
        )
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      throw e
    }
  }

  fun observeUserProfile(userId: String): Flow<UserProfile?> {
    val path = "users/$userId"
    return db.collection("users").document(userId)
      .snapshots()
      .map { snap ->
        if (snap.exists()) {
          snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } else {
          null
        }
      }
      .logFirestoreErrors(OperationType.GET, path, expectedUid = userId)
  }

  suspend fun updateUserProfileDetails(
    userId: String,
    displayName: String,
    favoriteRegions: List<String>? = null,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    require(uid == userId) { "User mismatch" }
    val cleanName = displayName.trim().take(60)
    require(cleanName.isNotEmpty()) { "Display name cannot be empty" }
    val docRef = db.collection("users").document(uid)
    val updates = mutableMapOf<String, Any>(
      "displayName" to cleanName,
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    if (favoriteRegions != null) {
      updates["favoriteRegions"] = favoriteRegions.take(10)
    }
    try {
      docRef.update(updates).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun acceptTerms(userId: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    require(uid == userId) { "User mismatch" }
    val docRef = db.collection("users").document(uid)
    try {
      docRef.update(
        mapOf(
          "acceptedTerms" to true,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  // --- FAVORITE REGIONAL NAMES ---
  fun observeFavoriteNames(userId: String): Flow<List<FavoriteName>> {
    val path = "users/$userId/favorite_names"
    return db.collection("users").document(userId).collection("favorite_names")
      .snapshots()
      .map { snap ->
        snap.toObjects(FavoriteName::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
      .logFirestoreErrors(OperationType.LIST, path, expectedUid = userId)
  }

  suspend fun toggleFavoriteName(
    userId: String,
    nameId: String,
    name: String,
    originRegion: String,
    gender: String,
    meaning: String,
    currentlyFavorite: Boolean,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    require(uid == userId) { "User mismatch" }
    val safeId = nameId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
    val docRef = db.collection("users").document(uid).collection("favorite_names").document(safeId)
    try {
      if (currentlyFavorite) {
        docRef.delete().await()
      } else {
        docRef.set(
          mapOf(
            "name" to name.trim().take(60),
            "originRegion" to originRegion.trim().take(40),
            "gender" to gender,
            "meaning" to meaning.trim().take(240),
            "createdAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, if (currentlyFavorite) OperationType.DELETE else OperationType.CREATE, docRef.path)
      throw e
    }
  }

  // --- COMMUNITY REGIONAL NAMES ---
  fun observeCommunityNames(): Flow<List<RegionalName>> {
    val path = "regional_names"
    return db.collection(path)
      .whereIn("status", listOf("approved", "featured"))
      .snapshots()
      .map { snap ->
        snap.toObjects(RegionalName::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.likesCount }
      }
      .logFirestoreErrors(OperationType.LIST, path)
  }

  suspend fun submitRegionalName(
    name: String,
    script: String,
    pronunciation: String,
    originRegion: String,
    gender: String,
    meaning: String,
    culturalNote: String,
    authorName: String,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val docId = "rn_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val docRef = db.collection("regional_names").document(docId)
    val payload = mapOf(
      "name" to name.trim().take(60),
      "script" to script.trim().take(60),
      "pronunciation" to pronunciation.trim().take(80),
      "originRegion" to originRegion.trim().take(40),
      "gender" to gender,
      "meaning" to meaning.trim().take(250),
      "culturalNote" to culturalNote.trim().take(300),
      "userId" to uid,
      "authorName" to authorName.trim().ifBlank { "Community Member" }.take(60),
      "status" to "approved",
      "likesCount" to 0L,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      docRef.set(payload).await()
      docId
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun likeRegionalName(nameId: String): Result<Unit> = runCatching {
    requireUserId()
    val docRef = db.collection("regional_names").document(nameId)
    try {
      docRef.update(
        mapOf(
          "likesCount" to FieldValue.increment(1L),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun deleteRegionalName(nameId: String): Result<Unit> = runCatching {
    requireUserId()
    val docRef = db.collection("regional_names").document(nameId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  // --- GIVEAWAYS (1 TICKET = 10 COINS, 1 TO INFINITY PURCHASE, ADMIN TICKET LIMITS) ---
  fun observeGiveaways(): Flow<List<Giveaway>> {
    val path = "giveaways"
    return db.collection(path)
      .whereIn("status", listOf("active", "completed"))
      .snapshots()
      .map { snap ->
        snap.toObjects(Giveaway::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedWith(compareBy<Giveaway> { it.status != "active" }.thenByDescending { it.createdAt?.seconds ?: 0L })
      }
      .logFirestoreErrors(OperationType.LIST, path)
  }

  fun observeUserEntriesForGiveaway(giveawayId: String, userId: String): Flow<List<GiveawayEntry>> {
    val path = "giveaways/$giveawayId/entries"
    return db.collection("giveaways").document(giveawayId)
      .collection("entries")
      .whereEqualTo("userId", userId)
      .snapshots()
      .map { snap ->
        snap.toObjects(GiveawayEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .logFirestoreErrors(OperationType.LIST, path, expectedUid = userId)
  }

  fun observeUserTicketCountForGiveaway(giveawayId: String, userId: String): Flow<Long> =
    observeUserEntriesForGiveaway(giveawayId, userId).map { entries ->
      entries.sumOf { it.ticketCount }
    }

  suspend fun enterGiveaway(
    giveaway: Giveaway,
    ticketCount: Long,
    userId: String,
    userName: String,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    require(uid == userId) { "User mismatch" }
    require(giveaway.id.isNotBlank()) { "Giveaway ID required" }
    require(ticketCount >= 1L) { "Must buy at least 1 ticket." }
    if (giveaway.maxEntries > 0L) {
      val remaining = giveaway.maxEntries - giveaway.entriesCount
      require(ticketCount <= remaining) { "Only $remaining tickets left in this giveaway." }
    }

    val totalCost = ticketCount * giveaway.entryCostCoins
    val entryId = "tkt_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val userRef = db.collection("users").document(uid)
    val giveawayRef = db.collection("giveaways").document(giveaway.id)
    val entryRef = giveawayRef.collection("entries").document(entryId)

    try {
      val batch = db.batch()
      batch.update(
        userRef,
        mapOf(
          "coinBalance" to FieldValue.increment(-totalCost),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      )
      batch.update(
        giveawayRef,
        mapOf(
          "entriesCount" to FieldValue.increment(ticketCount),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      )
      batch.set(
        entryRef,
        mapOf(
          "userId" to uid,
          "userName" to userName.trim().ifBlank { "Member" }.take(60),
          "giveawayId" to giveaway.id,
          "ticketCount" to ticketCount,
          "coinsSpent" to totalCost,
          "createdAt" to FieldValue.serverTimestamp(),
        )
      )
      batch.commit().await()
      entryId
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, entryRef.path)
      throw e
    }
  }

  // --- DONATIONS (PURE CHARITY, NO TICKETS ISSUED, UNLIMITED COINS) ---
  fun observeDonationCampaigns(): Flow<List<DonationCampaign>> {
    val path = "donation_campaigns"
    return db.collection(path)
      .whereIn("status", listOf("active", "completed"))
      .snapshots()
      .map { snap ->
        snap.toObjects(DonationCampaign::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedWith(compareBy<DonationCampaign> { it.status != "active" }.thenByDescending { it.raisedCoins })
      }
      .logFirestoreErrors(OperationType.LIST, path)
  }

  fun observeMyDonations(userId: String): Flow<List<DonationRecord>> {
    val path = "donations"
    return db.collection(path)
      .whereEqualTo("userId", userId)
      .snapshots()
      .map { snap ->
        snap.toObjects(DonationRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .logFirestoreErrors(OperationType.LIST, path, expectedUid = userId)
  }

  suspend fun donateToCampaign(
    campaign: DonationCampaign,
    coinsToDonate: Long,
    message: String,
    userId: String,
    donorName: String,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    require(uid == userId) { "User mismatch" }
    require(coinsToDonate > 0L) { "Donation amount must be positive" }
    val donationId = "don_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val userRef = db.collection("users").document(uid)
    val campaignRef = db.collection("donation_campaigns").document(campaign.id)
    val donationRef = db.collection("donations").document(donationId)

    try {
      val batch = db.batch()
      batch.update(
        userRef,
        mapOf(
          "coinBalance" to FieldValue.increment(-coinsToDonate),
          "totalDonatedCoins" to FieldValue.increment(coinsToDonate),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      )
      batch.update(
        campaignRef,
        mapOf(
          "raisedCoins" to FieldValue.increment(coinsToDonate),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      )
      batch.set(
        donationRef,
        mapOf(
          "userId" to uid,
          "donorName" to donorName.trim().ifBlank { "Supporter" }.take(60),
          "campaignId" to campaign.id,
          "campaignTitle" to campaign.title.take(100),
          "coinsDonated" to coinsToDonate,
          "message" to message.trim().take(200),
          "createdAt" to FieldValue.serverTimestamp(),
        )
      )
      batch.commit().await()
      donationId
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, donationRef.path)
      throw e
    }
  }

  // --- PAYOUT PROOFS & HELPING DONATIONS ---
  fun observePublishedProofs(): Flow<List<PayoutProof>> {
    val path = "payout_proofs"
    return db.collection(path)
      .whereEqualTo("status", "published")
      .snapshots()
      .map { snap ->
        snap.toObjects(PayoutProof::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .logFirestoreErrors(OperationType.LIST, path)
  }

  // --- PROBLEM REPORTS ---
  fun observeMyProblemReports(userId: String): Flow<List<ProblemReport>> {
    val path = "problem_reports"
    return db.collection(path)
      .whereEqualTo("userId", userId)
      .snapshots()
      .map { snap ->
        snap.toObjects(ProblemReport::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .logFirestoreErrors(OperationType.LIST, path, expectedUid = userId)
  }

  suspend fun submitProblemReport(
    userId: String,
    reporterName: String,
    category: String,
    subject: String,
    description: String,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    require(uid == userId) { "User mismatch" }
    require(subject.trim().length >= 3) { "Subject must be at least 3 characters." }
    require(description.trim().length >= 5) { "Please describe the problem in at least 5 characters." }
    val reportId = "rep_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val docRef = db.collection("problem_reports").document(reportId)
    val payload = mapOf(
      "userId" to uid,
      "reporterName" to reporterName.trim().ifBlank { "Member" }.take(60),
      "category" to category.trim().ifBlank { "General" }.take(40),
      "subject" to subject.trim().take(120),
      "description" to description.trim().take(1000),
      "status" to "open",
      "adminReply" to "",
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      docRef.set(payload).await()
      reportId
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  // --- WHATSAPP COIN ORDERS ---
  fun observeMyCoinOrders(userId: String): Flow<List<CoinOrder>> {
    val path = "coin_orders"
    return db.collection(path)
      .whereEqualTo("userId", userId)
      .snapshots()
      .map { snap ->
        snap.toObjects(CoinOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .logFirestoreErrors(OperationType.LIST, path, expectedUid = userId)
  }

  suspend fun submitCoinOrder(
    userId: String,
    buyerName: String,
    coinPackage: CoinPackage,
    whatsappRef: String,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    require(uid == userId) { "User mismatch" }
    val orderId = "ord_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val docRef = db.collection("coin_orders").document(orderId)
    val payload = mapOf(
      "userId" to uid,
      "buyerName" to buyerName.trim().ifBlank { "Member" }.take(60),
      "packageName" to coinPackage.title.take(60),
      "coinsAmount" to coinPackage.coins,
      "priceDisplay" to coinPackage.priceDisplay.take(40),
      "whatsappRef" to whatsappRef.trim().take(120),
      "status" to "pending",
      "adminNote" to "",
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      docRef.set(payload).await()
      orderId
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  // --- APP CONFIG (WHATSAPP GROUP & TERMS) ---
  fun observeAppConfig(): Flow<AppConfig> {
    val path = "app_settings/config"
    return db.collection("app_settings").document("config")
      .snapshots()
      .map { snap ->
        if (snap.exists()) {
          snap.toObject(AppConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?: RegionalNameCatalog.defaultAppConfig
        } else {
          RegionalNameCatalog.defaultAppConfig
        }
      }
      .logFirestoreErrors(OperationType.GET, path)
  }

  // --- ADMIN AUTHENTICATION & PANEL OPERATIONS ---
  suspend fun checkIsAdmin(userId: String, email: String?): Boolean {
    if (email.equals("moizbro108@gmail.com", ignoreCase = true) && auth.currentUser?.isEmailVerified == true) {
      return true
    }
    val docRef = db.collection("admins").document(userId)
    return try {
      docRef.get().await().exists()
    } catch (e: Exception) {
      false
    }
  }

  suspend fun unlockAdminWithPasscode(userId: String, passcode: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    require(uid == userId) { "User mismatch" }
    val trimmed = passcode.trim()
    require(trimmed == "2+2=5" || trimmed == "2+2=5:-)") {
      "Incorrect admin password."
    }
    val docRef = db.collection("admins").document(uid)
    try {
      if (!docRef.get().await().exists()) {
        docRef.set(
          mapOf(
            "userId" to uid,
            "passcode" to trimmed,
            "grantedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  fun observeAllCoinOrders(): Flow<List<CoinOrder>> {
    val path = "coin_orders"
    return db.collection(path)
      .snapshots()
      .map { snap ->
        snap.toObjects(CoinOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedWith(compareBy<CoinOrder> { it.status != "pending" }.thenByDescending { it.createdAt?.seconds ?: 0L })
      }
      .logFirestoreErrors(OperationType.LIST, path)
  }

  fun observeAllUsers(): Flow<List<UserProfile>> {
    val path = "users"
    return db.collection(path)
      .snapshots()
      .map { snap ->
        snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.coinBalance }
      }
      .logFirestoreErrors(OperationType.LIST, path)
  }

  fun observeAllProblemReports(): Flow<List<ProblemReport>> {
    val path = "problem_reports"
    return db.collection(path)
      .snapshots()
      .map { snap ->
        snap.toObjects(ProblemReport::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedWith(compareBy<ProblemReport> { it.status == "resolved" }.thenByDescending { it.createdAt?.seconds ?: 0L })
      }
      .logFirestoreErrors(OperationType.LIST, path)
  }

  suspend fun approveCoinOrder(order: CoinOrder, adminNote: String = "Verified in WhatsApp"): Result<Unit> = runCatching {
    requireUserId()
    val orderRef = db.collection("coin_orders").document(order.id)
    val buyerRef = db.collection("users").document(order.userId)
    try {
      val batch = db.batch()
      batch.update(
        orderRef,
        mapOf(
          "status" to "approved",
          "adminNote" to adminNote.trim().take(200),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      )
      batch.update(
        buyerRef,
        mapOf(
          "coinBalance" to FieldValue.increment(order.coinsAmount),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      )
      batch.commit().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, orderRef.path)
      throw e
    }
  }

  suspend fun rejectCoinOrder(order: CoinOrder, adminNote: String = "Reference not found"): Result<Unit> = runCatching {
    requireUserId()
    val orderRef = db.collection("coin_orders").document(order.id)
    try {
      orderRef.update(
        mapOf(
          "status" to "rejected",
          "adminNote" to adminNote.trim().take(200),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, orderRef.path)
      throw e
    }
  }

  suspend fun createGiveaway(
    title: String,
    description: String,
    prizeTitle: String,
    regionTag: String,
    imageUrl: String = "",
    presetArt: String = "giveaway_gift",
    maxEntries: Long = 0L, // 0 = unlimited tickets; fixed 10 coins per ticket per rules
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val id = "gw_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val docRef = db.collection("giveaways").document(id)
    val payload = mapOf(
      "title" to title.trim().take(100),
      "description" to description.trim().take(500),
      "prizeTitle" to prizeTitle.trim().take(100),
      "regionTag" to regionTag.trim().take(40),
      "imageUrl" to imageUrl.trim().take(500),
      "presetArt" to presetArt.trim().ifBlank { "giveaway_gift" }.take(40),
      "entryCostCoins" to 10L,
      "maxEntries" to maxEntries.coerceAtLeast(0L),
      "entriesCount" to 0L,
      "status" to "active",
      "winnerUserId" to "",
      "winnerName" to "",
      "createdBy" to uid,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      docRef.set(payload).await()
      id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun updateGiveawayTicketLimit(
    giveaway: Giveaway,
    newMaxEntries: Long,
  ): Result<Unit> = runCatching {
    requireUserId()
    require(newMaxEntries == 0L || newMaxEntries >= giveaway.entriesCount) {
      "Ticket limit must be 0 (Unlimited) or at least ${giveaway.entriesCount} already sold."
    }
    val docRef = db.collection("giveaways").document(giveaway.id)
    try {
      docRef.update(
        mapOf(
          "maxEntries" to newMaxEntries.coerceAtLeast(0L),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun drawGiveawayWinner(giveaway: Giveaway): Result<String> = runCatching {
    requireUserId()
    val giveawayRef = db.collection("giveaways").document(giveaway.id)
    try {
      val entriesSnap = giveawayRef.collection("entries").get().await()
      val entries = entriesSnap.toObjects(GiveawayEntry::class.java)
      require(entries.isNotEmpty()) { "No participants have entered this giveaway yet." }

      // Weighted draw by ticketCount so every ticket has an equal chance
      val totalTickets = entries.sumOf { it.ticketCount.coerceAtLeast(1L) }
      val roll = Random.nextLong(totalTickets)
      var cumulative = 0L
      var winner = entries.first()
      for (entry in entries) {
        cumulative += entry.ticketCount.coerceAtLeast(1L)
        if (roll < cumulative) {
          winner = entry
          break
        }
      }

      giveawayRef.update(
        mapOf(
          "status" to "completed",
          "winnerUserId" to winner.userId,
          "winnerName" to winner.userName,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      winner.userName
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, giveawayRef.path)
      throw e
    }
  }

  suspend fun deleteGiveaway(giveawayId: String): Result<Unit> = runCatching {
    requireUserId()
    val docRef = db.collection("giveaways").document(giveawayId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  suspend fun createDonationCampaign(
    title: String,
    description: String,
    category: String,
    region: String,
    goalCoins: Long,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val id = "camp_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val docRef = db.collection("donation_campaigns").document(id)
    val payload = mapOf(
      "title" to title.trim().take(100),
      "description" to description.trim().take(500),
      "category" to category.trim().take(40),
      "region" to region.trim().take(40),
      "goalCoins" to goalCoins,
      "raisedCoins" to 0L,
      "status" to "active",
      "createdBy" to uid,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      docRef.set(payload).await()
      id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun giveMoneyToUser(
    targetUserId: String,
    targetUserName: String,
    pkrAmount: Long,
    bonusCoins: Long,
    paymentMethod: String,
    description: String,
    imageUrl: String = "",
  ): Result<String> = runCatching {
    val adminUid = requireUserId()
    require(pkrAmount > 0L || bonusCoins > 0L) { "Enter a PKR payout or coin bonus amount." }
    val proofId = "prf_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val userRef = db.collection("users").document(targetUserId)
    val proofRef = db.collection("payout_proofs").document(proofId)

    val amountLabel = buildString {
      if (pkrAmount > 0L) append("PKR $pkrAmount")
      if (pkrAmount > 0L && bonusCoins > 0L) append(" + ")
      if (bonusCoins > 0L) append("$bonusCoins Coins")
    }.ifBlank { "Verified Grant" }

    val userUpdates = mutableMapOf<String, Any>(
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    if (pkrAmount > 0L) {
      userUpdates["totalMoneyReceived"] = FieldValue.increment(pkrAmount)
    }
    if (bonusCoins > 0L) {
      userUpdates["coinBalance"] = FieldValue.increment(bonusCoins)
    }

    val cleanDesc = description.trim().ifBlank {
      "Sent via ${paymentMethod.trim().ifBlank { "Easypaisa / JazzCash" }} by PakGives Admin."
    }.take(500)

    try {
      val batch = db.batch()
      batch.update(userRef, userUpdates)
      batch.set(
        proofRef,
        mapOf(
          "title" to "${paymentMethod.trim().ifBlank { "Direct Payout" }} to ${targetUserName.trim().ifBlank { "Member" }}".take(120),
          "proofType" to "Money Payout",
          "recipientName" to targetUserName.trim().ifBlank { "PakGives Member" }.take(80),
          "recipientUserId" to targetUserId,
          "amountDisplay" to amountLabel.take(60),
          "pkrAmount" to pkrAmount.coerceAtLeast(0L),
          "region" to "All Pakistan",
          "description" to cleanDesc,
          "imageUrl" to imageUrl.trim().take(500),
          "presetArt" to "money_payout",
          "status" to "published",
          "createdBy" to adminUid,
          "createdAt" to FieldValue.serverTimestamp(),
        )
      )
      batch.commit().await()
      proofId
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, proofRef.path)
      throw e
    }
  }

  suspend fun createPayoutProof(
    title: String,
    proofType: String,
    recipientName: String,
    amountDisplay: String,
    pkrAmount: Long,
    region: String,
    description: String,
    imageUrl: String = "",
    presetArt: String = "helping_hand",
  ): Result<String> = runCatching {
    val adminUid = requireUserId()
    val proofId = "prf_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val proofRef = db.collection("payout_proofs").document(proofId)
    val payload = mapOf(
      "title" to title.trim().take(120),
      "proofType" to proofType,
      "recipientName" to recipientName.trim().ifBlank { "Community Beneficiary" }.take(80),
      "recipientUserId" to "",
      "amountDisplay" to amountDisplay.trim().ifBlank { "PKR $pkrAmount" }.take(60),
      "pkrAmount" to pkrAmount.coerceAtLeast(0L),
      "region" to region.trim().ifBlank { "All Pakistan" }.take(40),
      "description" to description.trim().take(500),
      "imageUrl" to imageUrl.trim().take(500),
      "presetArt" to presetArt.trim().ifBlank { "helping_hand" }.take(40),
      "status" to "published",
      "createdBy" to adminUid,
      "createdAt" to FieldValue.serverTimestamp(),
    )
    try {
      proofRef.set(payload).await()
      proofId
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, proofRef.path)
      throw e
    }
  }

  suspend fun deletePayoutProof(proofId: String): Result<Unit> = runCatching {
    requireUserId()
    val docRef = db.collection("payout_proofs").document(proofId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  suspend fun replyToProblemReport(
    reportId: String,
    status: String,
    adminReply: String,
  ): Result<Unit> = runCatching {
    requireUserId()
    val docRef = db.collection("problem_reports").document(reportId)
    try {
      docRef.update(
        mapOf(
          "status" to status,
          "adminReply" to adminReply.trim().take(500),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun updateAppConfig(config: AppConfig): Result<Unit> = runCatching {
    requireUserId()
    val docRef = db.collection("app_settings").document("config")
    val payload = mapOf(
      "whatsappGroupUrl" to config.whatsappGroupUrl.trim().take(300),
      "whatsappAdminContact" to config.whatsappAdminContact.trim().take(80),
      "termsTitle" to config.termsTitle.trim().take(120),
      "termsBody" to config.termsBody.trim().take(8000),
      "giveawayPolicyBody" to config.giveawayPolicyBody.trim().take(8000),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      docRef.set(payload).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      throw e
    }
  }

  suspend fun creditUserCoins(targetUserId: String, coinsToAdd: Long): Result<Unit> = runCatching {
    requireUserId()
    require(coinsToAdd > 0L) { "Amount must be positive" }
    val docRef = db.collection("users").document(targetUserId)
    try {
      docRef.update(
        mapOf(
          "coinBalance" to FieldValue.increment(coinsToAdd),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun seedStarterData(): Result<Unit> = runCatching {
    requireUserId()
    val configRef = db.collection("app_settings").document("config")
    if (!configRef.get().await().exists()) {
      updateAppConfig(RegionalNameCatalog.defaultAppConfig).getOrThrow()
    }
    val existingGiveaways = db.collection("giveaways").whereIn("status", listOf("active", "completed")).limit(1).get().await()
    if (existingGiveaways.isEmpty) {
      createGiveaway(
        title = "PakGives Heritage Hamper Draw",
        description = "10 Giveaway Coins per ticket. Buy 1 or as many tickets as you want! Includes hand-crafted Sindhi Ajrak and engraved brass tea set.",
        prizeTitle = "Artisan Ajrak & Brass Tea Set",
        regionTag = "Sindhi & Punjabi",
        imageUrl = "",
        presetArt = "giveaway_gift",
        maxEntries = 100L,
      ).getOrThrow()
      createGiveaway(
        title = "Crescent Infinity Cash & Coin Drop",
        description = "Unlimited tickets open! Every ticket is 10 Giveaway Coins and multiplies your winning odds in the grand draw.",
        prizeTitle = "PKR 5,000 + 500 Giveaway Coins",
        regionTag = "All Pakistan",
        imageUrl = "",
        presetArt = "money_payout",
        maxEntries = 0L,
      ).getOrThrow()
    }
    val existingCampaigns = db.collection("donation_campaigns").whereIn("status", listOf("active", "completed")).limit(1).get().await()
    if (existingCampaigns.isEmpty) {
      createDonationCampaign(
        title = "Tharparkar Clean Water Wells",
        description = "Pure charity: solar water filtration units bringing clean drinking water to desert villages. Donate any coin amount (no tickets).",
        category = "Clean Water",
        region = "Sindhi",
        goalCoins = 5000L,
      ).getOrThrow()
      createDonationCampaign(
        title = "Swat & Chitral School Kits",
        description = "Backpacks, storybooks, and warm winter uniforms for mountain classrooms. Donate 1 to unlimited coins.",
        category = "Education",
        region = "Pashtun & GB",
        goalCoins = 3500L,
      ).getOrThrow()
      createDonationCampaign(
        title = "Lahore & Karachi Ration Drive",
        description = "Monthly staple food baskets supporting daily-wage families across urban communities.",
        category = "Food Relief",
        region = "Punjabi & Urdu",
        goalCoins = 4000L,
      ).getOrThrow()
    }
    val existingProofs = db.collection("payout_proofs").whereEqualTo("status", "published").limit(1).get().await()
    if (existingProofs.isEmpty) {
      createPayoutProof(
        title = "Winter Ration & Medical Aid Dispatch",
        proofType = "Helping Donation",
        recipientName = "12 Families • Swat & Tharparkar",
        amountDisplay = "PKR 24,000 Aid",
        pkrAmount = 24000L,
        region = "Pashtun & Sindhi",
        description = "Verified PakGives community relief delivery funded by coin donations and admin grants.",
        imageUrl = "",
        presetArt = "helping_hand",
      ).getOrThrow()
    }
  }
}
