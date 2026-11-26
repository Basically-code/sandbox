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
const CHARLIE_UID = "charlie_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function nowTs() {
  return new Date(Date.now() - 1000);
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

test("Unauthenticated user: cannot read or write notes, scripts, or panels", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("notes").get());
  await assertFails(unauthDb.collection("panels").where("memberUids", "array-contains", ALICE_UID).get());
  await assertFails(unauthDb.collection("panel_invites").doc("HVL123").get());
});

test("User profile: owner can create and read; cross-user and ghost fields rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = nowTs();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Creator",
      email: "alice@example.com",
      defaultTags: ["#ScriptIdea", "#Startup"],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      defaultTags: ["#ScriptIdea"],
      isAdmin: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Notes: owner creates note, preserves immutable rawContent on update, cross-user blocked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = nowTs();

  const noteRef = aliceDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_1");
  await assertSucceeds(
    noteRef.set({
      userId: ALICE_UID,
      rawContent: "Record hook about zero friction note taking by Friday",
      cleanContent: "Record hook about zero friction note taking by Friday",
      title: "Video Hook",
      tags: ["#ScriptIdea"],
      captureSource: "voice",
      priority: "high",
      deadlineEpochMs: 1794873600000,
      deadlineLabel: "Fri, Nov 13",
      reminderCadence: "escalating_1w_1d_1h",
      aiProcessed: true,
      isArchived: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Owner can list and update cleanContent/tags
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("notes").get());
  await assertSucceeds(
    noteRef.update({
      cleanContent: "Polished hook: Stop unlocking three apps just to save one thought.",
      tags: ["#ScriptIdea", "#Startup"],
      updatedAt: nowTs(),
    })
  );

  // Non-destructive invariant: rawContent cannot be overwritten
  await assertFails(
    noteRef.update({
      rawContent: "Tampered verbatim note",
      updatedAt: nowTs(),
    })
  );

  // Cross-user isolation
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("notes").get());
});

test("Stitched Scripts: owner can create and read; cross-user rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = nowTs();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("stitched_scripts").doc("script_1").set({
      userId: ALICE_UID,
      title: "Zero Friction Capture Launch Script",
      themeTag: "#ScriptIdea",
      insightSummary: "Linked 3 voice notes recorded this week.",
      scriptBody: "Intro: Every creator loses ideas between unlock and folder selection...",
      linkedNoteIds: ["note_1", "note_2", "note_3"],
      linkedNoteCount: 3,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("stitched_scripts").doc("script_1").get()
  );
});

test("Team Panels, Invites, Ideas, and Events: member collaboration, terminal lock, and isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = nowTs();

  // Alice creates panel + invite atomically in batch
  const batch = aliceDb.batch();
  const panelRef = aliceDb.collection("panels").doc("panel_1");
  const inviteRef = aliceDb.collection("panel_invites").doc("HVL842");

  batch.set(panelRef, {
    ownerId: ALICE_UID,
    ownerName: "Alice",
    name: "Q4 Creator Hooks",
    description: "Shared raw thoughts for launch",
    inviteCode: "HVL842",
    memberUids: [ALICE_UID],
    createdAt: ts,
    updatedAt: ts,
  });
  batch.set(inviteRef, {
    inviteCode: "HVL842",
    panelId: "panel_1",
    panelName: "Q4 Creator Hooks",
    ownerId: ALICE_UID,
    createdAt: ts,
  });
  await assertSucceeds(batch.commit());

  // Listing all panel_invites is forbidden (no scraping)
  await assertFails(bobDb.collection("panel_invites").get());

  // Bob looks up exact invite code and joins panel
  await assertSucceeds(bobDb.collection("panel_invites").doc("HVL842").get());
  await assertSucceeds(
    bobDb.collection("panels").doc("panel_1").update({
      memberUids: [ALICE_UID, BOB_UID],
      updatedAt: nowTs(),
    })
  );

  // Unfiltered panel query fails; filtered memberUids query succeeds
  await assertFails(bobDb.collection("panels").get());
  await assertSucceeds(bobDb.collection("panels").where("memberUids", "array-contains", BOB_UID).get());
  await assertFails(charlieDb.collection("panels").doc("panel_1").get());

  // Bob posts a collaborative idea
  await assertSucceeds(
    bobDb.collection("panels").doc("panel_1").collection("ideas").doc("idea_1").set({
      panelId: "panel_1",
      authorId: BOB_UID,
      authorName: "Bob",
      content: "Open with a 2-second lock screen capture demo.",
      tag: "#ScriptIdea",
      sourceNoteId: "",
      memberUids: [ALICE_UID, BOB_UID],
      upvoterUids: [],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Alice upvotes Bob's idea
  await assertSucceeds(
    aliceDb.collection("panels").doc("panel_1").collection("ideas").doc("idea_1").update({
      upvoterUids: [ALICE_UID],
      updatedAt: nowTs(),
    })
  );

  // Charlie cannot read panel ideas (direct get, unfiltered list, or querying Alice's ideas)
  await assertFails(
    charlieDb.collection("panels").doc("panel_1").collection("ideas").doc("idea_1").get()
  );
  await assertFails(
    charlieDb.collection("panels").doc("panel_1").collection("ideas").get()
  );
  await assertFails(
    charlieDb.collection("panels").doc("panel_1").collection("ideas")
      .where("memberUids", "array-contains", ALICE_UID).get()
  );

  // Alice creates a brainstorming event and wraps it (terminal state lock)
  const eventRef = aliceDb.collection("panels").doc("panel_1").collection("events").doc("event_1");
  await assertSucceeds(
    eventRef.set({
      panelId: "panel_1",
      creatorId: ALICE_UID,
      creatorName: "Alice",
      title: "Launch Hook Jam",
      topicPrompt: "30-minute rapid ideation for opening 3 seconds",
      scheduledEpochMs: 1794873600000,
      scheduledLabel: "Tomorrow 10:00 AM",
      status: "live",
      aiSummary: "",
      participantUids: [ALICE_UID],
      memberUids: [ALICE_UID, BOB_UID],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Wrap event with AI summary
  await assertSucceeds(
    eventRef.update({
      status: "wrapped",
      aiSummary: "Team aligned on 2-second lock-screen demo hook.",
      updatedAt: nowTs(),
    })
  );

  // Terminal lock prevents further RSVP or status edits once wrapped
  await assertFails(
    bobDb.collection("panels").doc("panel_1").collection("events").doc("event_1").update({
      participantUids: [ALICE_UID, BOB_UID],
      updatedAt: nowTs(),
    })
  );
});
