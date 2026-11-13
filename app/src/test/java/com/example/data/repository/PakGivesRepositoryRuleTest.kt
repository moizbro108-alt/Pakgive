package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.catalog.RegionalNameCatalog
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class PakGivesRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun ensureUserProfile_authenticatedUser_createsProfileWithZeroCoinsAndZeroMoney() = runBlocking {
    val uid = signInTestUser("alice_${UUID.randomUUID()}@test.com")
    val repo = PakGivesRepository(firestore, auth)

    val profile = withTimeout(TIMEOUT_MS) {
      repo.ensureUserProfile(uid, "Alice Khan", "alice@test.com").getOrThrow()
    }
    assertEquals(uid, profile.userId)
    assertEquals(0L, profile.coinBalance)
    assertEquals(0L, profile.totalMoneyReceived)
  }

  @Test
  fun submitRegionalName_authenticatedUser_succeedsAndEmitsInCommunityFeed() = runBlocking {
    signInTestUser("creator_${UUID.randomUUID()}@test.com")
    val repo = PakGivesRepository(firestore, auth)
    val uniqueName = "Mehran_${UUID.randomUUID().toString().take(5)}"

    val nameId = withTimeout(TIMEOUT_MS) {
      repo.submitRegionalName(
        name = uniqueName,
        script = "مہران",
        pronunciation = "Meh-raan",
        originRegion = "Sindhi",
        gender = "Unisex",
        meaning = "Ancient river Indus heritage",
        culturalNote = "Sindhi classic",
        authorName = "Creator",
      ).getOrThrow()
    }

    val list = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeCommunityNames().first { items -> items.any { it.id == nameId } }
    }
    assertTrue(list.any { it.id == nameId && it.name == uniqueName })
  }

  @Test
  fun unlockAdminCreateGiveawayAndBuyMultipleTickets_enforces10CoinsAndLimits() = runBlocking {
    val adminEmail = "admin_${UUID.randomUUID()}@test.com"
    val adminUid = signInTestUser(adminEmail)
    val adminRepo = PakGivesRepository(firestore, auth)
    withTimeout(TIMEOUT_MS) {
      adminRepo.ensureUserProfile(adminUid, "Admin", "admin@test.com").getOrThrow()
      val wrongResult = adminRepo.unlockAdminWithPasscode(adminUid, "2+2=4")
      assertTrue(wrongResult.isFailure)
      adminRepo.unlockAdminWithPasscode(adminUid, "2+2=5").getOrThrow()
    }

    val gwId = withTimeout(TIMEOUT_MS) {
      adminRepo.createGiveaway(
        title = "Limited Brass Set ${UUID.randomUUID().toString().take(4)}",
        description = "Win a regional brass tea set",
        prizeTitle = "Gift Box",
        regionTag = "Punjabi",
        imageUrl = "https://example.com/tea.jpg",
        presetArt = "giveaway_gift",
        maxEntries = 4L,
      ).getOrThrow()
    }

    // Non-admin cannot create giveaway (starts with 0 coins)
    val userEmail = "user_${UUID.randomUUID()}@test.com"
    val userUid = signInTestUser(userEmail)
    val userRepo = PakGivesRepository(firestore, auth)
    withTimeout(TIMEOUT_MS) {
      userRepo.ensureUserProfile(userUid, "Participant", "user@test.com").getOrThrow()
    }
    val nonAdminCreate = withTimeout(TIMEOUT_MS) {
      userRepo.createGiveaway("Bad", "Should fail", "Prize", "Punjab", "", "giveaway_gift", 10L)
    }
    assertTrue(nonAdminCreate.isFailure)

    // Admin credits 50 coins to participant, then participant signs back in
    signInTestUser(adminEmail)
    withTimeout(TIMEOUT_MS) {
      adminRepo.creditUserCoins(userUid, 50L).getOrThrow()
    }
    signInTestUser(userEmail)

    val giveaways = withTimeout(FLOW_TIMEOUT_MS) {
      userRepo.observeGiveaways().first { list -> list.any { it.id == gwId } }
    }
    val gw = giveaways.first { it.id == gwId }
    assertEquals(10L, gw.entryCostCoins)

    // Buying 3 tickets (30 coins) within limit of 4 succeeds
    withTimeout(TIMEOUT_MS) {
      userRepo.enterGiveaway(gw, ticketCount = 3L, userId = userUid, userName = "Participant").getOrThrow()
    }

    val updatedProfile = withTimeout(FLOW_TIMEOUT_MS) {
      userRepo.observeUserProfile(userUid).first { it != null && it.coinBalance == 20L }
    }
    assertEquals(20L, updatedProfile!!.coinBalance)

    val myEntries = withTimeout(FLOW_TIMEOUT_MS) {
      userRepo.observeUserEntriesForGiveaway(gwId, userUid).first { it.isNotEmpty() }
    }
    assertEquals(1, myEntries.size)
    assertEquals(3L, myEntries.first().ticketCount)
    assertEquals(30L, myEntries.first().coinsSpent)

    val myTicketCount = withTimeout(FLOW_TIMEOUT_MS) {
      userRepo.observeUserTicketCountForGiveaway(gwId, userUid).first { it == 3L }
    }
    assertEquals(3L, myTicketCount)

    // Admin can set unlimited limit (0L) and draw winner
    signInTestUser("admin2_${UUID.randomUUID()}@test.com").let { admin2Uid ->
      val admin2Repo = PakGivesRepository(firestore, auth)
      withTimeout(TIMEOUT_MS) {
        admin2Repo.unlockAdminWithPasscode(admin2Uid, "2+2=5:-)").getOrThrow()
        val updatedGw = admin2Repo.observeGiveaways().first { list -> list.any { it.id == gwId && it.entriesCount == 3L } }
          .first { it.id == gwId }
        admin2Repo.updateGiveawayTicketLimit(updatedGw, 0L).getOrThrow()
        val winner = admin2Repo.drawGiveawayWinner(updatedGw).getOrThrow()
        assertEquals("Participant", winner)
      }
    }
  }

  @Test
  fun donateCoins_unlimitedPureCharity_deductsBalanceWithoutTickets() = runBlocking {
    val donorEmail = "donor_${UUID.randomUUID()}@test.com"
    val donorUid = signInTestUser(donorEmail)
    val donorRepo = PakGivesRepository(firestore, auth)
    withTimeout(TIMEOUT_MS) {
      donorRepo.ensureUserProfile(donorUid, "Generous Donor", "donor@test.com").getOrThrow()
    }

    val adminUid = signInTestUser("donadmin_${UUID.randomUUID()}@test.com")
    val adminRepo = PakGivesRepository(firestore, auth)
    withTimeout(TIMEOUT_MS) {
      adminRepo.unlockAdminWithPasscode(adminUid, "2+2=5").getOrThrow()
      adminRepo.creditUserCoins(donorUid, 50L).getOrThrow()
    }
    val campId = withTimeout(TIMEOUT_MS) {
      adminRepo.createDonationCampaign(
        title = "Water Aid ${UUID.randomUUID().toString().take(4)}",
        description = "Clean water charity drive",
        category = "Clean Water",
        region = "Sindhi",
        goalCoins = 1000L,
      ).getOrThrow()
    }

    signInTestUser(donorEmail)
    val camp = withTimeout(FLOW_TIMEOUT_MS) {
      donorRepo.observeDonationCampaigns().first { list -> list.any { it.id == campId } }
    }.first { it.id == campId }

    withTimeout(TIMEOUT_MS) {
      donorRepo.donateToCampaign(camp, 35L, "Peace", donorUid, "Generous Donor").getOrThrow()
    }

    val donorProfile = withTimeout(FLOW_TIMEOUT_MS) {
      donorRepo.observeUserProfile(donorUid).first { it != null && it.coinBalance == 15L }
    }
    assertEquals(15L, donorProfile!!.coinBalance)
    assertEquals(35L, donorProfile.totalDonatedCoins)
  }

  @Test
  fun adminGiveMoneyAndHelpingProof_andUserProblemReport_endToEnd() = runBlocking {
    val memberUid = signInTestUser("member_${UUID.randomUUID()}@test.com")
    val memberRepo = PakGivesRepository(firestore, auth)
    withTimeout(TIMEOUT_MS) {
      memberRepo.ensureUserProfile(memberUid, "Zoya Tariq", "zoya@test.com").getOrThrow()
    }

    val reportId = withTimeout(TIMEOUT_MS) {
      memberRepo.submitProblemReport(
        userId = memberUid,
        reporterName = "Zoya Tariq",
        category = "Giveaway",
        subject = "Question on payout",
        description = "How do I receive Easypaisa cash?",
      ).getOrThrow()
    }

    val adminUid = signInTestUser("supadmin_${UUID.randomUUID()}@test.com")
    val adminRepo = PakGivesRepository(firestore, auth)
    withTimeout(TIMEOUT_MS) {
      adminRepo.unlockAdminWithPasscode(adminUid, "2+2=5").getOrThrow()
      adminRepo.giveMoneyToUser(
        targetUserId = memberUid,
        targetUserName = "Zoya Tariq",
        pkrAmount = 2500L,
        bonusCoins = 20L,
        paymentMethod = "Easypaisa",
        description = "Prize cash payout",
      ).getOrThrow()
      adminRepo.createPayoutProof(
        title = "Ration Helping Donation",
        proofType = "Helping Donation",
        recipientName = "Lahore Families",
        amountDisplay = "PKR 10,000",
        pkrAmount = 10000L,
        region = "Punjabi",
        description = "Food boxes delivered",
      ).getOrThrow()
      adminRepo.replyToProblemReport(reportId, "resolved", "Sent PKR 2,500 to your wallet!").getOrThrow()
    }

    val proofs = withTimeout(FLOW_TIMEOUT_MS) {
      adminRepo.observePublishedProofs().first { list ->
        list.any { it.recipientUserId == memberUid && it.pkrAmount == 2500L } &&
          list.any { it.proofType == "Helping Donation" }
      }
    }
    assertTrue(proofs.isNotEmpty())

    val users = withTimeout(FLOW_TIMEOUT_MS) {
      adminRepo.observeAllUsers().first { list -> list.any { it.userId == memberUid && it.totalMoneyReceived == 2500L } }
    }
    val updatedMember = users.first { it.userId == memberUid }
    assertEquals(2500L, updatedMember.totalMoneyReceived)
    assertEquals(20L, updatedMember.coinBalance)
  }

  @Test
  fun submitCoinOrder_andAdminApprove_creditsCoinsAtomically() = runBlocking {
    val buyerUid = signInTestUser("buyer_${UUID.randomUUID()}@test.com")
    val buyerRepo = PakGivesRepository(firestore, auth)
    withTimeout(TIMEOUT_MS) {
      buyerRepo.ensureUserProfile(buyerUid, "Buyer", "buyer@test.com").getOrThrow()
      buyerRepo.submitCoinOrder(
        userId = buyerUid,
        buyerName = "Buyer",
        coinPackage = RegionalNameCatalog.coinPackages.first(),
        whatsappRef = "WA-REF-777",
      ).getOrThrow()
    }

    val order = withTimeout(FLOW_TIMEOUT_MS) {
      buyerRepo.observeMyCoinOrders(buyerUid).first { it.isNotEmpty() }.first()
    }

    val adminUid = signInTestUser("approver_${UUID.randomUUID()}@test.com")
    val adminRepo = PakGivesRepository(firestore, auth)
    withTimeout(TIMEOUT_MS) {
      adminRepo.unlockAdminWithPasscode(adminUid, "2+2=5:-)").getOrThrow()
      adminRepo.approveCoinOrder(order, "Confirmed on WhatsApp").getOrThrow()
    }

    val users = withTimeout(FLOW_TIMEOUT_MS) {
      adminRepo.observeAllUsers().first { list -> list.any { it.userId == buyerUid && it.coinBalance == 50L } }
    }
    assertTrue(users.any { it.userId == buyerUid && it.coinBalance == 50L })
  }

  @Test
  fun observeCommunityNames_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = PakGivesRepository(firestore, auth)
    try {
      withTimeout(FLOW_TIMEOUT_MS) { repo.observeCommunityNames().first() }
      fail("Expected PERMISSION_DENIED for unauthenticated caller")
    } catch (e: Exception) {
      val firestoreError = e.unwrapFirestoreException()
      assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreError)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreError!!.code)
    }
  }

  private companion object {
    const val TIMEOUT_MS = 8000L
    const val FLOW_TIMEOUT_MS = 5000L
  }
}
