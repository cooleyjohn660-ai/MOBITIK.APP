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

const serverTimestamp = () => new Date(Date.now() - 1000);

function validUserPayload(uid, role = "USER") {
  const ts = serverTimestamp();
  return {
    userId: uid,
    name: "Alice Pioneer",
    username: "alice_pioneer",
    email: "alice@example.com",
    walletAddress: "MBTK-ALICE1234567890",
    externalWalletAddress: "",
    externalWalletNetwork: "",
    referralCode: "MBKALICE",
    referredByCode: "",
    referredByUserId: "",
    totalBalance: 0,
    availableBalance: 0,
    usdtBalance: 0,
    pendingUsdtWithdrawal: 0,
    pendingRewards: 0,
    lockedBalance: 0,
    totalMiningRewards: 0,
    completedSessions: 0,
    totalReferrals: 0,
    activeReferrals: 0,
    referralRewards: 0,
    autoStartMining: false,
    twoFactorEnabled: false,
    pinHash: "",
    accountStatus: "ACTIVE",
    role: role,
    notificationsEnabled: true,
    themeMode: "DARK",
    language: "en",
    createdAt: ts,
    updatedAt: ts,
  };
}

function validMiningSessionPayload(uid, status = "ACTIVE", claimed = false) {
  const nowMs = Date.now();
  const ts = serverTimestamp();
  return {
    userId: uid,
    sessionNumber: 1,
    startTimeMs: nowMs,
    endTimeMs: nowMs + 12 * 3600 * 1000,
    durationHours: 12,
    rewardAmount: 0.22,
    status: status,
    claimed: claimed,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validWithdrawalPayload(uid, status = "PENDING") {
  const ts = serverTimestamp();
  return {
    userId: uid,
    username: "alice_pioneer",
    destinationAddress: "0x71C7656EC7ab88b098defB751B7401B5f6d8976F",
    network: "BNB Smart Chain (BEP-20)",
    amountUsdt: 15.0,
    networkFeeUsdt: 0.5,
    platformFeeUsdt: 0.25,
    finalAmountUsdt: 14.25,
    status: status,
    txHash: "",
    adminNote: "",
    idempotencyKey: "wd_alice_001",
    createdAt: ts,
    updatedAt: ts,
  };
}

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

// 1. Unauthenticated Rejections
test("Unauthenticated user cannot read user profiles, mining sessions, or withdrawals", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("mining_sessions").doc("session_1").get());
  await assertFails(unauthDb.collection("withdrawals").doc("wd_1").get());
});

// 2. PII Isolation & Cross-User Protection
test("Authenticated user can create and read their own profile, but cannot read another user's private profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

// 3. Privilege Escalation Guard
test("Non-admin user cannot self-assign ADMIN role on profile creation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID, "ADMIN")));
});

// 4. Shadow Update Test
test("Shadow Update: Adding an undeclared ghost field is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID)));

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      name: "Alice Updated",
      isVerifiedGhost: true,
      updatedAt: serverTimestamp(),
    })
  );
});

// 5. Email Spoofing Test
test("Email Spoofing: Unverified admin email cannot read audit logs", async () => {
  const spoofAdminDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "cooleyjohn660@gmail.com",
    email_verified: false,
  }).firestore();
  await assertFails(spoofAdminDb.collection("audit_logs").get());

  const realAdminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "cooleyjohn660@gmail.com",
    email_verified: true,
  }).firestore();
  await assertSucceeds(realAdminDb.collection("audit_logs").get());
});

// 6. Terminal State Locking on Mining Sessions
test("Mining Session: Once CLAIMED, user cannot update or re-claim session", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("mining_sessions").doc("session_1").set(validMiningSessionPayload(ALICE_UID, "ACTIVE", false))
  );

  await assertSucceeds(
    aliceDb.collection("mining_sessions").doc("session_1").update({
      status: "CLAIMED",
      claimed: true,
      updatedAt: serverTimestamp(),
    })
  );

  await assertFails(
    aliceDb.collection("mining_sessions").doc("session_1").update({
      status: "ACTIVE",
      claimed: false,
      updatedAt: serverTimestamp(),
    })
  );
});

// 7. Self-Referral Prevention
test("Referral: User cannot refer themselves", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("referrals").doc("ref_self").set({
      referrerUserId: ALICE_UID,
      referredUserId: ALICE_UID,
      referredUsername: "alice_pioneer",
      referralCode: "MBKALICE",
      status: "REGISTERED",
      rewardAmount: 0.1,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
});

// 8. USDT Withdrawal Security Bounds
test("USDT Withdrawal: User can submit PENDING withdrawal, cannot self-approve to CONFIRMED, and admin can approve", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("withdrawals").doc("wd_1").set(validWithdrawalPayload(ALICE_UID, "PENDING"))
  );

  // User cannot self-create a CONFIRMED withdrawal
  await assertFails(
    aliceDb.collection("withdrawals").doc("wd_2").set(validWithdrawalPayload(ALICE_UID, "CONFIRMED"))
  );

  // User cannot self-approve their PENDING withdrawal to CONFIRMED
  await assertFails(
    aliceDb.collection("withdrawals").doc("wd_1").update({
      status: "CONFIRMED",
      txHash: "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
      updatedAt: serverTimestamp(),
    })
  );

  // Verified Admin can approve and attach on-chain txHash
  const realAdminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "cooleyjohn660@gmail.com",
    email_verified: true,
  }).firestore();
  await assertSucceeds(
    realAdminDb.collection("withdrawals").doc("wd_1").update({
      status: "CONFIRMED",
      txHash: "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
      adminNote: "Verified on BNB Smart Chain",
      updatedAt: serverTimestamp(),
    })
  );
});
