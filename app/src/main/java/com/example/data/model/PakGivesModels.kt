package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "",
  val email: String = "",
  val coinBalance: Long = 0L,
  val totalDonatedCoins: Long = 0L,
  val totalMoneyReceived: Long = 0L,
  val favoriteRegions: List<String> = emptyList(),
  val acceptedTerms: Boolean = false,
  val role: String = "user",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class FavoriteName(
  @DocumentId val id: String = "",
  val name: String = "",
  val originRegion: String = "",
  val gender: String = "Unisex",
  val meaning: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class RegionalName(
  @DocumentId val id: String = "",
  val name: String = "",
  val script: String = "",
  val pronunciation: String = "",
  val originRegion: String = "",
  val gender: String = "Unisex",
  val meaning: String = "",
  val culturalNote: String = "",
  val userId: String = "",
  val authorName: String = "",
  val status: String = "approved",
  val likesCount: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class Giveaway(
  @DocumentId val id: String = "",
  val title: String = "",
  val description: String = "",
  val prizeTitle: String = "",
  val regionTag: String = "All Pakistan",
  val imageUrl: String = "",
  val presetArt: String = "giveaway_gift",
  val entryCostCoins: Long = 10L,
  val maxEntries: Long = 0L, // 0 = unlimited tickets
  val entriesCount: Long = 0L,
  val status: String = "active",
  val winnerUserId: String = "",
  val winnerName: String = "",
  val createdBy: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class GiveawayEntry(
  @DocumentId val id: String = "",
  val userId: String = "",
  val userName: String = "",
  val giveawayId: String = "",
  val ticketCount: Long = 1L,
  val coinsSpent: Long = 10L,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class DonationCampaign(
  @DocumentId val id: String = "",
  val title: String = "",
  val description: String = "",
  val category: String = "",
  val region: String = "",
  val goalCoins: Long = 1000L,
  val raisedCoins: Long = 0L,
  val status: String = "active",
  val createdBy: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class DonationRecord(
  @DocumentId val id: String = "",
  val userId: String = "",
  val donorName: String = "",
  val campaignId: String = "",
  val campaignTitle: String = "",
  val coinsDonated: Long = 0L,
  val message: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class PayoutProof(
  @DocumentId val id: String = "",
  val title: String = "",
  val proofType: String = "Money Payout", // "Giveaway Proof", "Money Payout", "Helping Donation"
  val recipientName: String = "",
  val recipientUserId: String = "",
  val amountDisplay: String = "",
  val pkrAmount: Long = 0L,
  val region: String = "All Pakistan",
  val description: String = "",
  val imageUrl: String = "",
  val presetArt: String = "money_payout",
  val status: String = "published",
  val createdBy: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class ProblemReport(
  @DocumentId val id: String = "",
  val userId: String = "",
  val reporterName: String = "",
  val category: String = "General",
  val subject: String = "",
  val description: String = "",
  val status: String = "open", // "open", "in_review", "resolved"
  val adminReply: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class CoinOrder(
  @DocumentId val id: String = "",
  val userId: String = "",
  val buyerName: String = "",
  val packageName: String = "",
  val coinsAmount: Long = 0L,
  val priceDisplay: String = "",
  val whatsappRef: String = "",
  val status: String = "pending",
  val adminNote: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class AppConfig(
  @DocumentId val id: String = "config",
  val whatsappGroupUrl: String = "https://chat.whatsapp.com/PakGivesCoinsOfficial",
  val whatsappAdminContact: String = "+92 300 0000000 (PakGives Desk)",
  val termsTitle: String = "PakGives Terms, Giveaways & Privacy Policy",
  val termsBody: String = "",
  val giveawayPolicyBody: String = "",
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

data class CoinPackage(
  val id: String,
  val title: String,
  val coins: Long,
  val priceDisplay: String,
  val badge: String = "",
)

data class CuratedRegionalName(
  val id: String,
  val name: String,
  val script: String,
  val pronunciation: String,
  val originRegion: String,
  val gender: String,
  val meaning: String,
  val culturalNote: String,
)
