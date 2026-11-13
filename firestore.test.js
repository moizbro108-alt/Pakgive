const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

const safeTs = () => new Date(Date.now() - 1000);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function validUserPayload(uid, email = "user@test.com") {
  return {
    userId: uid,
    displayName: "Test User",
    email,
    coinBalance: 0,
    totalDonatedCoins: 0,
    totalMoneyReceived: 0,
    favoriteRegions: ["Punjabi"],
    acceptedTerms: true,
    role: "user",
    createdAt: safeTs(),
    updatedAt: safeTs(),
  };
}

// --- 1. UNAUTHENTICATED ACCESS DENIAL ---
test("Unauthenticated user cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("giveaways").where("status", "in", ["active", "completed"]).get());
  await assertFails(unauthDb.collection("donation_campaigns").where("status", "in", ["active", "completed"]).get());
  await assertFails(unauthDb.collection("payout_proofs").where("status", "==", "published").get());
  await assertFails(unauthDb.collection("problem_reports").where("userId", "==", ALICE_UID).get());
});

// --- 2. USER PROFILE, PII ISOLATION & ANTI-CHEAT ---
test("User profile protects coins, money received, and role from self-escalation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@test.com", email_verified: true }).firestore();

  const freeCoinPayload = validUserPayload(BOB_UID, "bob@test.com");
  freeCoinPayload.coinBalance = 50;
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@test.com", email_verified: true }).firestore();
  await assertFails(bobDb.collection("users").doc(BOB_UID).set(freeCoinPayload));

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID, "alice@test.com")));

  await assertFails(aliceDb.collection("users").doc(ALICE_UID).update({ coinBalance: 9999, updatedAt: safeTs() }));
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).update({ totalMoneyReceived: 5000, updatedAt: safeTs() }));
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).update({ role: "admin", updatedAt: safeTs() }));
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).update({ isVerified: true, updatedAt: safeTs() }));
});

// --- 3. ONLY ADMIN CAN POST GIVEAWAYS & DONATIONS ---
test("Non-admin cannot create giveaways or donation campaigns", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertFails(
    aliceDb.collection("giveaways").doc("gw_unauth").set({
      title: "Fake Giveaway",
      description: "Should fail",
      prizeTitle: "Nothing",
      regionTag: "All Pakistan",
      imageUrl: "",
      presetArt: "giveaway_gift",
      entryCostCoins: 10,
      maxEntries: 50,
      entriesCount: 0,
      status: "active",
      winnerUserId: "",
      winnerName: "",
      createdBy: ALICE_UID,
      createdAt: safeTs(),
      updatedAt: safeTs(),
    })
  );

  await assertFails(
    aliceDb.collection("donation_campaigns").doc("camp_unauth").set({
      title: "Fake Cause",
      description: "Should fail",
      category: "Food",
      region: "Punjab",
      goalCoins: 1000,
      raisedCoins: 0,
      status: "active",
      createdBy: ALICE_UID,
      createdAt: safeTs(),
      updatedAt: safeTs(),
    })
  );
});

// --- 4. MULTI-TICKET GIVEAWAY PURCHASE (10 COINS/TICKET, 1 TO INFINITY, TICKET LIMITS) ---
test("User can buy multiple tickets (10 coins each) in one batch and respects admin ticket limit", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "moizbro108@gmail.com",
    email_verified: true,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID)));
  await assertSucceeds(adminDb.collection("users").doc(ALICE_UID).update({ coinBalance: 50, updatedAt: safeTs() }));

  // Admin creates giveaway with limit of 4 tickets
  await assertSucceeds(
    adminDb.collection("giveaways").doc("gw_limited").set({
      title: "Limited Brass Set",
      description: "Only 4 tickets total!",
      prizeTitle: "Brass Tea Set",
      regionTag: "Punjabi",
      imageUrl: "https://example.com/prize.jpg",
      presetArt: "giveaway_gift",
      entryCostCoins: 10,
      maxEntries: 4,
      entriesCount: 0,
      status: "active",
      winnerUserId: "",
      winnerName: "",
      createdBy: ADMIN_UID,
      createdAt: safeTs(),
      updatedAt: safeTs(),
    })
  );

  // Buying 5 tickets exceeds limit of 4 -> MUST FAIL
  const overBatch = aliceDb.batch();
  overBatch.update(aliceDb.collection("users").doc(ALICE_UID), { coinBalance: 0, updatedAt: safeTs() });
  overBatch.update(aliceDb.collection("giveaways").doc("gw_limited"), { entriesCount: 5, updatedAt: safeTs() });
  overBatch.set(aliceDb.collection("giveaways").doc("gw_limited").collection("entries").doc("tkt_1"), {
    userId: ALICE_UID,
    userName: "Alice",
    giveawayId: "gw_limited",
    ticketCount: 5,
    coinsSpent: 50,
    createdAt: safeTs(),
  });
  await assertFails(overBatch.commit());

  // Buying 3 tickets (30 coins) within limit of 4 -> SUCCEEDS
  const validBatch = aliceDb.batch();
  validBatch.update(aliceDb.collection("users").doc(ALICE_UID), { coinBalance: 20, updatedAt: safeTs() });
  validBatch.update(aliceDb.collection("giveaways").doc("gw_limited"), { entriesCount: 3, updatedAt: safeTs() });
  validBatch.set(aliceDb.collection("giveaways").doc("gw_limited").collection("entries").doc("tkt_2"), {
    userId: ALICE_UID,
    userName: "Alice",
    giveawayId: "gw_limited",
    ticketCount: 3,
    coinsSpent: 30,
    createdAt: safeTs(),
  });
  await assertSucceeds(validBatch.commit());
});

// --- 5. UNLIMITED TICKET GIVEAWAY (maxEntries == 0) ---
test("Admin can create unlimited ticket giveaway (maxEntries == 0)", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "moizbro108@gmail.com",
    email_verified: true,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID)));
  await assertSucceeds(adminDb.collection("users").doc(ALICE_UID).update({ coinBalance: 50, updatedAt: safeTs() }));

  await assertSucceeds(
    adminDb.collection("giveaways").doc("gw_inf").set({
      title: "Infinity Draw",
      description: "Unlimited tickets available",
      prizeTitle: "Mega Bundle",
      regionTag: "All Pakistan",
      imageUrl: "",
      presetArt: "giveaway_gift",
      entryCostCoins: 10,
      maxEntries: 0,
      entriesCount: 0,
      status: "active",
      winnerUserId: "",
      winnerName: "",
      createdBy: ADMIN_UID,
      createdAt: safeTs(),
      updatedAt: safeTs(),
    })
  );

  const batch = aliceDb.batch();
  batch.update(aliceDb.collection("users").doc(ALICE_UID), { coinBalance: 10, updatedAt: safeTs() });
  batch.update(aliceDb.collection("giveaways").doc("gw_inf"), { entriesCount: 4, updatedAt: safeTs() });
  batch.set(aliceDb.collection("giveaways").doc("gw_inf").collection("entries").doc("tkt_inf1"), {
    userId: ALICE_UID,
    userName: "Alice",
    giveawayId: "gw_inf",
    ticketCount: 4,
    coinsSpent: 40,
    createdAt: safeTs(),
  });
  await assertSucceeds(batch.commit());
});

// --- 6. PAYOUT PROOFS & HELPING DONATIONS ---
test("Only admin can post payout proofs; all signed-in users can read published proofs", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "moizbro108@gmail.com",
    email_verified: true,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertFails(
    aliceDb.collection("payout_proofs").doc("prf_1").set({
      title: "Fake Proof",
      proofType: "Money Payout",
      recipientName: "Alice",
      recipientUserId: ALICE_UID,
      amountDisplay: "PKR 5000",
      pkrAmount: 5000,
      region: "Punjab",
      description: "Should fail",
      imageUrl: "",
      presetArt: "money_payout",
      status: "published",
      createdBy: ALICE_UID,
      createdAt: safeTs(),
    })
  );

  await assertSucceeds(
    adminDb.collection("payout_proofs").doc("prf_1").set({
      title: "Easypaisa Winner Payout",
      proofType: "Money Payout",
      recipientName: "Alice",
      recipientUserId: ALICE_UID,
      amountDisplay: "PKR 5,000",
      pkrAmount: 5000,
      region: "Punjab",
      description: "Sent prize payout to Alice.",
      imageUrl: "",
      presetArt: "money_payout",
      status: "published",
      createdBy: ADMIN_UID,
      createdAt: safeTs(),
    })
  );

  await assertSucceeds(aliceDb.collection("payout_proofs").where("status", "==", "published").get());
});

// --- 7. PROBLEM REPORTS ---
test("User can submit problem report; cannot self-resolve; admin can reply & resolve", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "moizbro108@gmail.com",
    email_verified: true,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("problem_reports").doc("rep_1").set({
      userId: ALICE_UID,
      reporterName: "Alice",
      category: "Giveaway",
      subject: "Missing ticket confirmation",
      description: "My network dropped right after entering.",
      status: "open",
      adminReply: "",
      createdAt: safeTs(),
      updatedAt: safeTs(),
    })
  );

  // Bob cannot read Alice's problem report
  await assertFails(bobDb.collection("problem_reports").doc("rep_1").get());

  // Alice cannot self-resolve
  await assertFails(
    aliceDb.collection("problem_reports").doc("rep_1").update({
      status: "resolved",
      adminReply: "Fixed myself",
      updatedAt: safeTs(),
    })
  );

  // Admin replies & resolves
  await assertSucceeds(
    adminDb.collection("problem_reports").doc("rep_1").update({
      status: "resolved",
      adminReply: "Checked ledger, your 3 tickets are active!",
      updatedAt: safeTs(),
    })
  );
});
