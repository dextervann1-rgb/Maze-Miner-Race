const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_miner_101";
const BOB_UID = "bob_miner_202";
const CHARLIE_UID = "charlie_miner_303";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function nowPast(offsetMs = 2000) {
  return new Date(Date.now() - offsetMs);
}

function validProfilePayload(uid, ts = nowPast()) {
  return {
    userId: uid,
    displayName: "DexterVann",
    gems: 120,
    streak: 4,
    bestStreak: 7,
    totalWins: 14,
    totalBlocksBroken: 380,
    totalGemsCollected: 420,
    pickLevel: 3,
    lanternLevel: 2,
    staminaLevel: 2,
    sonarLevel: 1,
    harvesterLevel: 2,
    shieldLevel: 1,
    founderPackOwned: true,
    pvpWins: 5,
    pvpMatches: 8,
    campaignSummary: "1:3:4200,2:3:5100",
    createdAt: ts,
    updatedAt: ts,
  };
}

function validPvpRoomPayload(hostUid, ts = nowPast()) {
  return {
    roomCode: "MZ-8421",
    seed: "pvp-apex-2026",
    biome: "VOLCANO",
    boardSize: 21,
    difficulty: "NORMAL",
    status: "WAITING",
    hostUid,
    hostName: "Alice",
    hostX: 1,
    hostY: 1,
    hostScore: 0,
    hostBeacons: 0,
    hostFinished: false,
    guestUid: "",
    guestName: "",
    guestX: 1,
    guestY: 1,
    guestScore: 0,
    guestBeacons: 0,
    guestFinished: false,
    winnerUid: "",
    createdAt: ts,
    updatedAt: ts,
  };
}

function validCommunitySeedPayload(authorUid, ts = nowPast()) {
  return {
    authorUid,
    authorName: "Alice",
    title: "Volcanic Labyrinth Gauntlet",
    seed: "volcano-gauntlet-99",
    biome: "VOLCANO",
    boardSize: 25,
    difficulty: "HARD",
    strategicMaze: true,
    visibility: "PUBLIC",
    bestScore: 5400,
    playsCount: 0,
    upvotes: 0,
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

// =========================================================================
// 1. CLOUD PROFILE SAVE (/users/{userId})
// =========================================================================

test("Users: unauthenticated cannot read or write cloud saves", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID)));
});

test("Users: owner can create, update, and read own cloud save; cross-user denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = nowPast(5000);

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, created)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  // Partial update preserving immutable userId & createdAt
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      gems: 250,
      pvpWins: 6,
      pvpMatches: 9,
      updatedAt: nowPast(1000),
    })
  );

  // Cross-user read & write strictly blocked
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ gems: 999, updatedAt: nowPast(500) }));

  // Ghost field & future timestamp tampering rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: nowPast(500),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      gems: 300,
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Immutable createdAt mutation rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: nowPast(500),
      updatedAt: nowPast(500),
    })
  );
});

// =========================================================================
// 2. LIVE PVP MATCHMAKING ROOMS (/pvp_rooms/{roomId})
// =========================================================================

test("PvP Rooms: host creates room, guest joins, live telemetry updates, winner locks terminally", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const created = nowPast(10000);

  // Host creates WAITING room
  await assertSucceeds(aliceDb.collection("pvp_rooms").doc("room_1").set(validPvpRoomPayload(ALICE_UID, created)));

  // Filtered WAITING lobby query succeeds; unfiltered query fails
  await assertSucceeds(bobDb.collection("pvp_rooms").where("status", "==", "WAITING").get());
  await assertFails(bobDb.collection("pvp_rooms").get());

  // Host cannot join own room
  await assertFails(
    aliceDb.collection("pvp_rooms").doc("room_1").update({
      guestUid: ALICE_UID,
      guestName: "Self",
      status: "ACTIVE",
      updatedAt: nowPast(8000),
    })
  );

  // Bob joins as guest -> ACTIVE
  await assertSucceeds(
    bobDb.collection("pvp_rooms").doc("room_1").update({
      guestUid: BOB_UID,
      guestName: "Bob",
      status: "ACTIVE",
      updatedAt: nowPast(7000),
    })
  );

  // Third-party Charlie cannot read or modify private ACTIVE match
  await assertFails(charlieDb.collection("pvp_rooms").doc("room_1").get());
  await assertFails(
    charlieDb.collection("pvp_rooms").doc("room_1").update({
      guestScore: 9999,
      updatedAt: nowPast(6000),
    })
  );

  // Host updates own coordinates/score; cannot spoof guestScore
  await assertSucceeds(
    aliceDb.collection("pvp_rooms").doc("room_1").update({
      hostX: 5,
      hostY: 7,
      hostScore: 840,
      hostBeacons: 1,
      updatedAt: nowPast(5000),
    })
  );
  await assertFails(
    aliceDb.collection("pvp_rooms").doc("room_1").update({
      guestScore: 999,
      updatedAt: nowPast(4500),
    })
  );

  // Guest updates own telemetry
  await assertSucceeds(
    bobDb.collection("pvp_rooms").doc("room_1").update({
      guestX: 6,
      guestY: 8,
      guestScore: 920,
      guestBeacons: 2,
      updatedAt: nowPast(4000),
    })
  );

  // Guest finishes and locks match FINISHED
  await assertSucceeds(
    bobDb.collection("pvp_rooms").doc("room_1").update({
      guestX: 19,
      guestY: 19,
      guestScore: 3400,
      guestBeacons: 3,
      guestFinished: true,
      status: "FINISHED",
      winnerUid: BOB_UID,
      updatedAt: nowPast(2000),
    })
  );

  // Terminal state locking: nobody can mutate a FINISHED room
  await assertFails(
    aliceDb.collection("pvp_rooms").doc("room_1").update({
      hostFinished: true,
      winnerUid: ALICE_UID,
      status: "FINISHED",
      updatedAt: nowPast(1000),
    })
  );
});

// =========================================================================
// 3. SHAREABLE COMMUNITY MAZE SEEDS (/community_seeds/{seedId})
// =========================================================================

test("Community Seeds: publish, public query alignment, upvote/play increment, author delete only", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = nowPast(8000);

  // Unauthenticated denied
  await assertFails(unauthDb.collection("community_seeds").where("visibility", "==", "PUBLIC").get());

  // Author publishes valid seed
  await assertSucceeds(
    aliceDb.collection("community_seeds").doc("seed_volcano_1").set(validCommunitySeedPayload(ALICE_UID, created))
  );

  // Filtered PUBLIC query succeeds; unfiltered query fails
  await assertSucceeds(bobDb.collection("community_seeds").where("visibility", "==", "PUBLIC").get());
  await assertFails(bobDb.collection("community_seeds").get());

  // Bob can increment playsCount (+1) and upvotes (+1)
  await assertSucceeds(
    bobDb.collection("community_seeds").doc("seed_volcano_1").update({
      playsCount: 1,
      upvotes: 1,
      bestScore: 5800,
      updatedAt: nowPast(3000),
    })
  );

  // Bob cannot jump upvotes by +100 or delete Alice's seed
  await assertFails(
    bobDb.collection("community_seeds").doc("seed_volcano_1").update({
      upvotes: 101,
      updatedAt: nowPast(2000),
    })
  );
  await assertFails(bobDb.collection("community_seeds").doc("seed_volcano_1").delete());

  // Author can edit title and delete own seed
  await assertSucceeds(
    aliceDb.collection("community_seeds").doc("seed_volcano_1").update({
      title: "Volcanic Gauntlet EX",
      updatedAt: nowPast(1500),
    })
  );
  await assertSucceeds(aliceDb.collection("community_seeds").doc("seed_volcano_1").delete());
});

// =========================================================================
// 4. DAILY RANKED GHOST LEADERBOARD (/daily_ranked/{dateKey}/entries/{userId})
// =========================================================================

const VALID_SHA256 = "a1b2c3d4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f60718293a4b5c6d7e8f90";

test("Daily Ranked Ghosts: valid submit, monotonic score improvement, speedhack/hash/size/delete denials", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const dateKey = "2026-11-24";

  const entryRef = (db, uid) => db.collection("daily_ranked").doc(dateKey).collection("entries").doc(uid);

  const validRun = {
    uid: ALICE_UID,
    displayName: "Alice",
    score: 4800,
    clearTimeMs: 32500,
    sha256Fingerprint: VALID_SHA256,
    compressedInputs: "0,1,1,0;2,2,1,0;4,3,1,0",
    submittedAt: nowPast(5000),
  };

  // Unauthenticated denied
  await assertFails(entryRef(unauthDb, ALICE_UID).get());
  await assertFails(entryRef(unauthDb, ALICE_UID).set(validRun));

  // Owner submits valid verified run
  await assertSucceeds(entryRef(aliceDb, ALICE_UID).set(validRun));

  // Signed-in Bob can fetch top ghost runs (filtered score > 0)
  await assertSucceeds(
    bobDb.collection("daily_ranked").doc(dateKey).collection("entries").where("score", ">", 0).get()
  );

  // Lower or equal score rejected (monotonic PB only)
  await assertFails(entryRef(aliceDb, ALICE_UID).set({ ...validRun, score: 4700, submittedAt: nowPast(3000) }));

  // Higher score accepted
  await assertSucceeds(entryRef(aliceDb, ALICE_UID).set({ ...validRun, score: 5600, submittedAt: nowPast(2500) }));

  // Speedhack (<8000ms) rejected
  await assertFails(entryRef(bobDb, BOB_UID).set({ ...validRun, uid: BOB_UID, clearTimeMs: 4200, submittedAt: nowPast(2000) }));

  // Invalid SHA-256 rejected
  await assertFails(entryRef(bobDb, BOB_UID).set({ ...validRun, uid: BOB_UID, sha256Fingerprint: "not-a-sha256", submittedAt: nowPast(2000) }));

  // Oversized replay (>32KB) rejected
  await assertFails(entryRef(bobDb, BOB_UID).set({ ...validRun, uid: BOB_UID, compressedInputs: "x".repeat(33000), submittedAt: nowPast(2000) }));

  // Cross-user write & delete strictly blocked
  await assertFails(entryRef(bobDb, ALICE_UID).set({ ...validRun, score: 9999, submittedAt: nowPast(1000) }));
  await assertFails(entryRef(aliceDb, ALICE_UID).delete());
});

// =========================================================================
// 5. ABYSSAL DEPTH ENDLESS LEADERBOARD (/abyssal_leaderboard/{userId})
// =========================================================================

test("Abyssal Leaderboard: monotonic floor climbing, SHA-256 validation, bounds and cross-user denial", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const floor1 = {
    uid: ALICE_UID,
    displayName: "Alice",
    highestFloor: 4,
    totalScore: 18500,
    runRootSeed: 20261124,
    runFingerprint: VALID_SHA256,
    updatedAt: nowPast(4000),
  };

  await assertSucceeds(aliceDb.collection("abyssal_leaderboard").doc(ALICE_UID).set(floor1));
  await assertSucceeds(bobDb.collection("abyssal_leaderboard").where("highestFloor", ">=", 1).get());

  // Higher floor succeeds
  await assertSucceeds(
    aliceDb.collection("abyssal_leaderboard").doc(ALICE_UID).set({
      ...floor1,
      highestFloor: 7,
      totalScore: 34200,
      updatedAt: nowPast(2000),
    })
  );

  // Lower floor rejected; out-of-range (>999) rejected; cross-user denied; delete denied
  await assertFails(aliceDb.collection("abyssal_leaderboard").doc(ALICE_UID).set({ ...floor1, highestFloor: 2, updatedAt: nowPast(1500) }));
  await assertFails(bobDb.collection("abyssal_leaderboard").doc(BOB_UID).set({ ...floor1, uid: BOB_UID, highestFloor: 1500, updatedAt: nowPast(1500) }));
  await assertFails(bobDb.collection("abyssal_leaderboard").doc(ALICE_UID).set({ ...floor1, highestFloor: 99, updatedAt: nowPast(1000) }));
  await assertFails(aliceDb.collection("abyssal_leaderboard").doc(ALICE_UID).delete());
});

// =========================================================================
// 6. LIVE-OPS DYNAMIC BALANCE CONFIG (/live_config/{docId})
// =========================================================================

test("Live Config: signed-in read allowed, all client writes and unauth reads denied", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertFails(unauthDb.collection("live_config").doc("balance").get());
  await assertSucceeds(aliceDb.collection("live_config").doc("balance").get());
  await assertFails(aliceDb.collection("live_config").doc("balance").set({ goldieSpeedMultiplier: 2.0 }));
});

