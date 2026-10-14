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

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

function getSamplePack(userId) {
  return {
    id: "pack_001",
    userId: userId,
    roleTitle: "Senior Backend Engineer",
    department: "Engineering",
    experienceLevel: "Senior (5+ yrs)",
    rawNotes: "Looking for Golang expert with distributed systems experience.",
    jdHeadline: "🚀 We're hiring a Senior Backend Engineer!",
    jdAbout: "Join our high-impact team building payments infra.",
    jdResponsibilities: ["Design APIs", "Scale databases"],
    jdHardSkills: ["Golang", "PostgreSQL", "AWS"],
    jdSoftSkills: ["Clear communication", "Team mentoring"],
    jdPerks: ["Competitive equity", "Health insurance"],
    jdHashtags: ["#Hiring", "#Engineering"],
    jdRawFormattedText: "Formatted JD content here",
    questions: [
      { id: 1, competency: "Scalability", question: "Tell me about..." }
    ],
    createdAt: new Date(),
    updatedAt: new Date()
  };
}

test("Unauthenticated user: cannot read recruitment packs", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("recruitment_packs").get());
});

test("Unauthenticated user: cannot write recruitment packs", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("recruitment_packs").doc("pack_001").set(getSamplePack(ALICE_UID))
  );
});

test("Authenticated user: cannot read another user's recruitment pack", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).collection("recruitment_packs").doc("pack_bob").set(getSamplePack(BOB_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("recruitment_packs").doc("pack_bob").get());
});

test("Authenticated user: can create and read their own recruitment pack", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("recruitment_packs").doc("pack_001").set(getSamplePack(ALICE_UID))
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("recruitment_packs").doc("pack_001").get()
  );
});

test("Authenticated user: cannot tamper with userId on update", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("recruitment_packs").doc("pack_001").set(getSamplePack(ALICE_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("recruitment_packs").doc("pack_001").update({
      userId: BOB_UID,
      updatedAt: new Date()
    })
  );
});

test("Authenticated user: can delete their own recruitment pack", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("recruitment_packs").doc("pack_001").set(getSamplePack(ALICE_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("recruitment_packs").doc("pack_001").delete()
  );
});
