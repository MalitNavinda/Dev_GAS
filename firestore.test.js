const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const SELLER_UID = "seller_kamal_01";
const CONSUMER_UID = "consumer_nimal_02";
const ADMIN_UID = "admin_malith_01";
const ADMIN_CLAIMS = {
  email: "malith.mudunkotuwa@gmail.com",
  email_verified: true,
};
const SPOOF_ADMIN_CLAIMS = {
  email: "malith.mudunkotuwa@gmail.com",
  email_verified: false,
};

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validDistributorPayload(ownerId, overrides = {}) {
  const ts = new Date(Date.now() - 1000);
  return {
    ownerId,
    ownerName: "Kamal Perera",
    name: "Perera Gas Center - Dehiwala",
    brand: "LITRO",
    district: "Colombo",
    town: "Dehiwala",
    address: "142 Galle Road, Dehiwala",
    phone: "+94112712345",
    latitude: 6.8511,
    longitude: 79.8654,
    geohash: "tc0z8u",
    visibility: "PUBLIC",
    isVerified: false,
    status: "AVAILABLE",
    stock12_5kg: 45,
    stock5kg: 20,
    stock2_3kg: 12,
    nextArrivalNote: "Unloaded 08:30 AM",
    queueLevel: "MODERATE",
    estimatedQueueCount: 25,
    confirmationsCount: 0,
    discrepancyCount: 0,
    createdAt: ts,
    updatedAt: ts,
    ...overrides,
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

test("1. Unauthenticated caller is blocked from reading or listing distributors", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("distributors").where("visibility", "==", "PUBLIC").get()
  );
});

test("2. Query Alignment: Authenticated consumer succeeds with visibility==PUBLIC filter and fails unfiltered list", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("distributors")
      .doc("station_pub")
      .set(validDistributorPayload(SELLER_UID));
  });

  const consumerDb = testEnv.authenticatedContext(CONSUMER_UID).firestore();
  await assertSucceeds(
    consumerDb.collection("distributors").where("visibility", "==", "PUBLIC").get()
  );
  await assertFails(consumerDb.collection("distributors").get());
});

test("3. PII Isolation: User profile cannot be read or listed by another user", async () => {
  const ts = new Date(Date.now() - 1000);
  const sellerDb = testEnv.authenticatedContext(SELLER_UID).firestore();
  await assertSucceeds(
    sellerDb.collection("users").doc(SELLER_UID).set({
      userId: SELLER_UID,
      displayName: "Kamal",
      preferredLanguage: "SI",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  const consumerDb = testEnv.authenticatedContext(CONSUMER_UID).firestore();
  await assertFails(consumerDb.collection("users").doc(SELLER_UID).get());
  await assertFails(consumerDb.collection("users").get());
});

test("4. Privilege Escalation & Shadow Update Guard: Regular seller cannot self-verify or inject ghost fields", async () => {
  const sellerDb = testEnv.authenticatedContext(SELLER_UID).firestore();

  // Attempt to create pre-verified station as non-admin fails
  await assertFails(
    sellerDb
      .collection("distributors")
      .doc("st_self_verify")
      .set(validDistributorPayload(SELLER_UID, { isVerified: true }))
  );

  // Valid creation succeeds
  await assertSucceeds(
    sellerDb
      .collection("distributors")
      .doc("st_valid")
      .set(validDistributorPayload(SELLER_UID))
  );

  // Shadow update with ghost field fails
  await assertFails(
    sellerDb.collection("distributors").doc("st_valid").update({
      stock12_5kg: 30,
      ghostAdminFlag: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("5. Email Spoofing Guard: Unverified admin email cannot verify stations, verified admin succeeds", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("distributors")
      .doc("st_1")
      .set(validDistributorPayload(SELLER_UID));
  });

  const spoofDb = testEnv
    .authenticatedContext("spoof_uid", SPOOF_ADMIN_CLAIMS)
    .firestore();
  await assertFails(
    spoofDb.collection("distributors").doc("st_1").update({
      isVerified: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  const realAdminDb = testEnv
    .authenticatedContext(ADMIN_UID, ADMIN_CLAIMS)
    .firestore();
  await assertSucceeds(
    realAdminDb.collection("distributors").doc("st_1").update({
      isVerified: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("6. Community Crowd Report & Bounded Telemetry Update work while preventing +9999 inflation", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("distributors")
      .doc("st_crowd")
      .set(validDistributorPayload(SELLER_UID));
  });

  const consumerDb = testEnv.authenticatedContext(CONSUMER_UID).firestore();
  const ts = new Date(Date.now() - 500);

  await assertSucceeds(
    consumerDb.collection("queueReports").doc("rep_01").set({
      stationId: "st_crowd",
      stationName: "Perera Gas Center - Dehiwala",
      userId: CONSUMER_UID,
      reporterAlias: "Nimal",
      reportedStatus: "AVAILABLE",
      queueLevel: "SHORT",
      queueLengthEstimate: 12,
      isProximityVerified: true,
      weight: 2,
      moderationState: "ACTIVE",
      comment: "Moving fast, ~12 people in line",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Bounded +2 increment succeeds
  await assertSucceeds(
    consumerDb.collection("distributors").doc("st_crowd").update({
      queueLevel: "SHORT",
      estimatedQueueCount: 12,
      confirmationsCount: 2,
      updatedAt: ts,
    })
  );

  // Malicious +500 inflation fails
  await assertFails(
    consumerDb.collection("distributors").doc("st_crowd").update({
      confirmationsCount: 500,
      updatedAt: ts,
    })
  );

  // Consumer cannot directly overwrite seller stock12_5kg
  await assertFails(
    consumerDb.collection("distributors").doc("st_crowd").update({
      stock12_5kg: 0,
      updatedAt: ts,
    })
  );
});

test("7. StockLog Global Consistency & Immutability: Only owner/admin can append log, nobody can mutate/delete", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("distributors")
      .doc("st_log")
      .set(validDistributorPayload(SELLER_UID));
  });

  const ts = new Date(Date.now() - 500);
  const consumerDb = testEnv.authenticatedContext(CONSUMER_UID).firestore();
  const sellerDb = testEnv.authenticatedContext(SELLER_UID).firestore();

  // Consumer cannot forge a StockLog for Seller's station
  await assertFails(
    consumerDb.collection("stockLogs").doc("log_bad").set({
      stationId: "st_log",
      actorId: CONSUMER_UID,
      stationName: "Perera Gas Center - Dehiwala",
      brand: "LITRO",
      previousStatus: "IN_TRANSIT",
      newStatus: "AVAILABLE",
      stock12_5kg: 45,
      stock5kg: 20,
      stock2_3kg: 12,
      note: "Forged",
      visibility: "PUBLIC",
      createdAt: ts,
    })
  );

  // Seller succeeds
  await assertSucceeds(
    sellerDb.collection("stockLogs").doc("log_ok").set({
      stationId: "st_log",
      actorId: SELLER_UID,
      stationName: "Perera Gas Center - Dehiwala",
      brand: "LITRO",
      previousStatus: "IN_TRANSIT",
      newStatus: "AVAILABLE",
      stock12_5kg: 45,
      stock5kg: 20,
      stock2_3kg: 12,
      note: "Morning lorry unloaded",
      visibility: "PUBLIC",
      createdAt: ts,
    })
  );

  // Immutable: even owner cannot delete or update StockLog
  await assertFails(sellerDb.collection("stockLogs").doc("log_ok").delete());
});

test("8. Watchlist Subcollection Isolation: Only owner can subscribe and query their own watchlist", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("distributors")
      .doc("st_watch")
      .set(validDistributorPayload(SELLER_UID));
  });

  const ts = new Date(Date.now() - 500);
  const consumerDb = testEnv.authenticatedContext(CONSUMER_UID).firestore();
  const sellerDb = testEnv.authenticatedContext(SELLER_UID).firestore();

  await assertSucceeds(
    consumerDb
      .collection("users")
      .doc(CONSUMER_UID)
      .collection("watchlist")
      .doc("st_watch")
      .set({
        userId: CONSUMER_UID,
        stationId: "st_watch",
        stationName: "Perera Gas Center - Dehiwala",
        brand: "LITRO",
        desiredSize: "12_5KG",
        lastKnownStatus: "SOLD_OUT",
        createdAt: ts,
        updatedAt: ts,
      })
  );

  await assertSucceeds(
    consumerDb
      .collection("users")
      .doc(CONSUMER_UID)
      .collection("watchlist")
      .where("userId", "==", CONSUMER_UID)
      .get()
  );

  await assertFails(
    sellerDb
      .collection("users")
      .doc(CONSUMER_UID)
      .collection("watchlist")
      .doc("st_watch")
      .get()
  );
});
