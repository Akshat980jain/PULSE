const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_001";
const BOB_UID = "bob_uid_002";
const EVE_UID = "eve_uid_003";
const THREAD_AB = `${ALICE_UID}_${BOB_UID}`;

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTime(offsetMs = 2000) {
  return new Date(Date.now() - offsetMs);
}

function futureTime(offsetMs = 60000) {
  return new Date(Date.now() + offsetMs);
}

function buildValidThread(overrides = {}) {
  const ts = pastTime(5000);
  return {
    memberUids: [ALICE_UID, BOB_UID],
    participantAId: ALICE_UID,
    participantAName: "Alice Lin",
    participantAUsername: "alicelin",
    participantAAvatarUrl: "https://example.com/alice.jpg",
    participantBId: BOB_UID,
    participantBName: "Bob Park",
    participantBUsername: "bobpark",
    participantBAvatarUrl: "https://example.com/bob.jpg",
    lastMessageText: "Hey Bob!",
    lastSenderUid: ALICE_UID,
    unreadCountA: 0,
    unreadCountB: 1,
    lastMessageAt: ts,
    createdAt: ts,
    updatedAt: ts,
    ...overrides,
  };
}

function buildValidMessage(overrides = {}) {
  return {
    threadId: THREAD_AB,
    memberUids: [ALICE_UID, BOB_UID],
    senderUid: ALICE_UID,
    senderUsername: "alicelin",
    senderDisplayName: "Alice Lin",
    senderAvatarUrl: "https://example.com/alice.jpg",
    text: "Hello from Firebase!",
    likedByUids: [],
    readByUids: [ALICE_UID],
    createdAt: pastTime(2000),
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

test("Unauthenticated users cannot read or write messaging_users, dm_threads, or messages", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("messaging_users").get());
  await assertFails(unauthDb.collection("dm_threads").get());
  await assertFails(
    unauthDb.collection("messaging_users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      displayName: "Alice",
      username: "alice",
      email: "alice@example.com",
      avatarUrl: "",
      supabaseUserId: "",
      updatedAt: pastTime(),
    })
  );
  await assertFails(
    unauthDb.collection("dm_threads").doc(THREAD_AB).set(buildValidThread())
  );
});

test("messaging_users: owner can upsert own directory profile, others can read but not overwrite", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("messaging_users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      displayName: "Alice Lin",
      username: "alicelin",
      email: "alice@example.com",
      avatarUrl: "",
      supabaseUserId: "supa_alice",
      updatedAt: pastTime(),
    })
  );

  // Authenticated users can discover directory profiles for New Chat picker
  await assertSucceeds(bobDb.collection("messaging_users").get());
  await assertSucceeds(bobDb.collection("messaging_users").doc(ALICE_UID).get());

  // Cross-user write rejected
  await assertFails(
    bobDb.collection("messaging_users").doc(ALICE_UID).update({
      displayName: "Hijacked",
      updatedAt: pastTime(500),
    })
  );

  // Future timestamp rejected
  await assertFails(
    aliceDb.collection("messaging_users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      displayName: "Alice Future",
      username: "alicelin",
      email: "alice@example.com",
      avatarUrl: "",
      supabaseUserId: "supa_alice",
      updatedAt: futureTime(),
    })
  );

  // Extra @DocumentId 'id' field rejected
  await assertFails(
    aliceDb.collection("messaging_users").doc(ALICE_UID).set({
      id: ALICE_UID,
      uid: ALICE_UID,
      displayName: "Alice",
      username: "alicelin",
      email: "alice@example.com",
      avatarUrl: "",
      supabaseUserId: "",
      updatedAt: pastTime(),
    })
  );
});

test("dm_threads: participants can create, query with array-contains, and update unread/lastMessage; outsiders blocked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const eveDb = testEnv.authenticatedContext(EVE_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("dm_threads").doc(THREAD_AB).set(buildValidThread())
  );

  // Filtered Kotlin repository query succeeds for participants
  await assertSucceeds(
    aliceDb.collection("dm_threads").where("memberUids", "array-contains", ALICE_UID).get()
  );
  await assertSucceeds(
    bobDb.collection("dm_threads").where("memberUids", "array-contains", BOB_UID).get()
  );

  // Unfiltered list query fails
  await assertFails(aliceDb.collection("dm_threads").get());

  // Outsider cannot read, query Alice's threads, or modify thread
  await assertFails(eveDb.collection("dm_threads").doc(THREAD_AB).get());
  await assertFails(
    eveDb.collection("dm_threads").where("memberUids", "array-contains", ALICE_UID).get()
  );
  await assertFails(
    eveDb.collection("dm_threads").doc(THREAD_AB).update({
      unreadCountB: 0,
      updatedAt: pastTime(500),
    })
  );

  // Bob marks thread as read (partial update)
  await assertSucceeds(
    bobDb.collection("dm_threads").doc(THREAD_AB).update({
      unreadCountB: 0,
      updatedAt: pastTime(500),
    })
  );

  // Mirror Kotlin sendMessage() partial thread update payload
  await assertSucceeds(
    aliceDb.collection("dm_threads").doc(THREAD_AB).update({
      lastMessageText: "Updated message preview",
      lastSenderUid: ALICE_UID,
      lastMessageAt: pastTime(300),
      updatedAt: pastTime(300),
      participantAName: "Alice Lin",
      participantAUsername: "alicelin",
      participantAAvatarUrl: "https://example.com/alice.jpg",
      unreadCountB: 1,
      unreadCountA: 0,
    })
  );

  // Immutable createdAt cannot be modified
  await assertFails(
    aliceDb.collection("dm_threads").doc(THREAD_AB).update({
      createdAt: pastTime(100),
      updatedAt: pastTime(100),
    })
  );

  // Immutable memberUids cannot be mutated
  await assertFails(
    aliceDb.collection("dm_threads").doc(THREAD_AB).update({
      memberUids: [ALICE_UID, EVE_UID],
      updatedAt: pastTime(100),
    })
  );
});

test("dm_threads messages: sender creates message, recipient marks read and toggles heart like, text immutable, outsider blocked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const eveDb = testEnv.authenticatedContext(EVE_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("dm_threads").doc(THREAD_AB).set(buildValidThread())
  );

  const msgPath = aliceDb
    .collection("dm_threads")
    .doc(THREAD_AB)
    .collection("messages")
    .doc("msg_1");

  await assertSucceeds(msgPath.set(buildValidMessage()));

  // Message with optional sharedPost fields also succeeds
  await assertSucceeds(
    aliceDb
      .collection("dm_threads")
      .doc(THREAD_AB)
      .collection("messages")
      .doc("msg_2")
      .set(
        buildValidMessage({
          text: "Check out this Loop!",
          sharedPostId: "loop_42",
          sharedPostCaption: "Night neon walk",
          sharedPostImageUri: "https://example.com/thumb.jpg",
        })
      )
  );

  // Kotlin-aligned subcollection query succeeds for participants
  await assertSucceeds(
    bobDb
      .collection("dm_threads")
      .doc(THREAD_AB)
      .collection("messages")
      .where("memberUids", "array-contains", BOB_UID)
      .get()
  );

  // Bob marks msg_1 as read and likes it
  await assertSucceeds(
    bobDb
      .collection("dm_threads")
      .doc(THREAD_AB)
      .collection("messages")
      .doc("msg_1")
      .update({
        readByUids: [ALICE_UID, BOB_UID],
        likedByUids: [BOB_UID],
      })
  );

  // Sender Alice can edit her own message text; Bob cannot edit Alice's message text
  await assertSucceeds(
    aliceDb
      .collection("dm_threads")
      .doc(THREAD_AB)
      .collection("messages")
      .doc("msg_1")
      .update({
        text: "Edited by Alice",
      })
  );
  await assertFails(
    bobDb
      .collection("dm_threads")
      .doc(THREAD_AB)
      .collection("messages")
      .doc("msg_1")
      .update({
        text: "Hijacked by Bob",
      })
  );

  // Neither Alice nor Bob can spoof senderUid
  await assertFails(
    aliceDb
      .collection("dm_threads")
      .doc(THREAD_AB)
      .collection("messages")
      .doc("msg_1")
      .update({
        senderUid: BOB_UID,
      })
  );

  // Sender spoofing rejected on create
  await assertFails(
    bobDb
      .collection("dm_threads")
      .doc(THREAD_AB)
      .collection("messages")
      .doc("msg_spoof")
      .set(buildValidMessage({ senderUid: ALICE_UID }))
  );

  // Outsider Eve cannot read or write messages
  await assertFails(
    eveDb
      .collection("dm_threads")
      .doc(THREAD_AB)
      .collection("messages")
      .doc("msg_1")
      .get()
  );
  await assertFails(
    eveDb
      .collection("dm_threads")
      .doc(THREAD_AB)
      .collection("messages")
      .doc("msg_1")
      .update({ likedByUids: [EVE_UID] })
  );

  // Bob cannot delete Alice's message, but Alice can delete her own message
  await assertFails(
    bobDb
      .collection("dm_threads")
      .doc(THREAD_AB)
      .collection("messages")
      .doc("msg_1")
      .delete()
  );
  await assertSucceeds(
    aliceDb
      .collection("dm_threads")
      .doc(THREAD_AB)
      .collection("messages")
      .doc("msg_1")
      .delete()
  );
});

test("posts comments: full CRUD with owner edit/delete, authenticated like toggle, and security isolation", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const postId = "loop_video_42";

  function buildValidComment(overrides = {}) {
    const ts = pastTime(3000);
    return {
      postId,
      authorUid: ALICE_UID,
      authorUsername: "alicelin",
      authorDisplayName: "Alice Lin",
      authorAvatarUrl: "https://example.com/alice.jpg",
      text: "Incredible pacing and lighting!",
      likedByUids: [],
      createdAt: ts,
      updatedAt: ts,
      ...overrides,
    };
  }

  // Unauthenticated cannot read or create comments
  await assertFails(
    unauthDb.collection("posts").doc(postId).collection("comments").get()
  );
  await assertFails(
    unauthDb
      .collection("posts")
      .doc(postId)
      .collection("comments")
      .doc("c_unauth")
      .set(buildValidComment())
  );

  // Alice creates a comment and threaded reply
  const commentRef = aliceDb
    .collection("posts")
    .doc(postId)
    .collection("comments")
    .doc("c_1");
  await assertSucceeds(commentRef.set(buildValidComment()));

  await assertSucceeds(
    bobDb
      .collection("posts")
      .doc(postId)
      .collection("comments")
      .doc("c_reply_1")
      .set(
        buildValidComment({
          authorUid: BOB_UID,
          authorUsername: "bobpark",
          authorDisplayName: "Bob Park",
          text: "@alicelin Totally agree!",
          parentCommentId: "c_1",
          replyToUsername: "alicelin",
        })
      )
  );

  // Extra @DocumentId 'id' or future timestamp rejected
  await assertFails(
    aliceDb
      .collection("posts")
      .doc(postId)
      .collection("comments")
      .doc("c_bad_id")
      .set(buildValidComment({ id: "c_bad_id" }))
  );
  await assertFails(
    aliceDb
      .collection("posts")
      .doc(postId)
      .collection("comments")
      .doc("c_future")
      .set(buildValidComment({ createdAt: futureTime() }))
  );

  // Authenticated user can read/list comments
  await assertSucceeds(
    bobDb.collection("posts").doc(postId).collection("comments").get()
  );
  await assertSucceeds(
    bobDb.collection("posts").doc(postId).collection("comments").doc("c_1").get()
  );

  // Alice can edit her own comment text; Bob cannot edit Alice's comment text
  await assertSucceeds(
    commentRef.update({
      text: "Incredible pacing and 35mm lighting!",
      updatedAt: pastTime(500),
    })
  );
  await assertFails(
    bobDb
      .collection("posts")
      .doc(postId)
      .collection("comments")
      .doc("c_1")
      .update({
        text: "Edited by Bob",
        updatedAt: pastTime(400),
      })
  );

  // Bob can toggle his own like on Alice's comment (add + remove), but cannot forge Alice's like
  await assertSucceeds(
    bobDb
      .collection("posts")
      .doc(postId)
      .collection("comments")
      .doc("c_1")
      .update({
        likedByUids: [BOB_UID],
        updatedAt: pastTime(300),
      })
  );
  await assertFails(
    bobDb
      .collection("posts")
      .doc(postId)
      .collection("comments")
      .doc("c_1")
      .update({
        likedByUids: [BOB_UID, ALICE_UID],
        updatedAt: pastTime(200),
      })
  );
  await assertSucceeds(
    bobDb
      .collection("posts")
      .doc(postId)
      .collection("comments")
      .doc("c_1")
      .update({
        likedByUids: [],
        updatedAt: pastTime(100),
      })
  );

  // Bob cannot delete Alice's comment; Alice can delete her own comment
  await assertFails(
    bobDb
      .collection("posts")
      .doc(postId)
      .collection("comments")
      .doc("c_1")
      .delete()
  );
  await assertSucceeds(commentRef.delete());
});
