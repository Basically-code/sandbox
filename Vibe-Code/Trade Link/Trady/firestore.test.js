const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const pastTs = () => new Date(Date.now() - 2000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_buyer_01";
const BOB_UID = "bob_seller_02";
const CHARLIE_UID = "charlie_intruder_03";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validUserProfile(uid, role = "BUYER") {
  return {
    userId: uid,
    fullName: "Amina Juma",
    phoneNumber: "+255712345678",
    activeRole: role,
    termsAccepted: true,
    stationName: "Kariakoo Market Block A",
    vehiclePlate: "MC 482 DTR",
    createdAt: pastTs(),
    updatedAt: pastTs(),
  };
}

function validProduct(providerId) {
  return {
    providerId,
    providerName: "Kariakoo Tech Hub",
    category: "ELECTRONICS",
    title: "Wireless Pro Gamepad",
    description: "Dual vibration wireless controller",
    priceTzs: 85000,
    quantityAvailable: 12,
    sizeSpec: "Standard Box",
    weightKg: 0.6,
    requiresTruck: false,
    isDailyDeal: true,
    isActive: true,
    pickupLabel: "Kariakoo Market Block A",
    createdAt: pastTs(),
    updatedAt: pastTs(),
  };
}

function validOrder(buyerId, providerId, productId) {
  return {
    buyerId,
    providerId,
    driverId: providerId,
    participantIds: [buyerId, providerId],
    productId,
    productTitle: "Wireless Pro Gamepad",
    category: "ELECTRONICS",
    selectedSize: "Standard Box",
    quantity: 1,
    unitPriceTzs: 85000,
    itemSubtotalTzs: 85000,
    transportFeeTzs: 3500,
    totalAmountTzs: 88500,
    paymentMethod: "CASH_BY_HAND_WITH_RECEIPT",
    secretSafetyCode: "4829 - SIMBA-7",
    receiptNumber: "TL-20261113-4829",
    driverName: "Juma Hassan",
    driverPlate: "MC 482 DTR",
    requiresTruck: false,
    cargoBoxAttached: true,
    status: "PENDING_DISPATCH",
    refundAmountTzs: 0,
    disturbanceCompTzs: 0,
    driverLat: -6.818,
    driverLng: 39.275,
    destLat: -6.792,
    destLng: 39.268,
    createdAt: pastTs(),
    updatedAt: pastTs(),
  };
}

function validDriverContract(driverId) {
  return {
    driverId,
    driverName: "Juma Hassan",
    vehiclePlate: "MC 482 DTR",
    secretGpsImei: "IMEI-8642091100482",
    stationName: "Kariakoo Market Block A",
    vehicleStatus: "STATION_CHECKED_IN",
    cargoBoxAttached: false,
    rideHailPartner: "NONE",
    weeklyFeeTzs: 70000,
    totalWeeksRequired: 104,
    weeksPaidCount: 14,
    totalPaidTzs: 980000,
    ownershipTransferred: false,
    curfewViolated: false,
    createdAt: pastTs(),
    updatedAt: pastTs(),
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

// 1. Unauthenticated access rejected across all collections
test("Unauthenticated user: denied read/write across users, products, orders, driver_contracts", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("products").where("isActive", "==", true).get());
  await assertFails(unauthDb.collection("orders").where("participantIds", "array-contains", ALICE_UID).get());
  await assertFails(unauthDb.collection("driver_contracts").doc(ALICE_UID).get());
});

// 2. User Profile isolation & Shadow Update test
test("Users: owner can create/update own profile; cross-user read and shadow field fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validUserProfile(ALICE_UID)));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());

  // Shadow Update (ghost field) rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: pastTs(),
    })
  );
});

// 3. Products: Seller creates, Buyer lists active products, non-owner cannot mutate/delete
test("Products: provider creates product, buyer lists active products, non-owner cannot update", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertSucceeds(bobDb.collection("products").doc("prod_01").set(validProduct(BOB_UID)));
  await assertSucceeds(aliceDb.collection("products").where("isActive", "==", true).get());
  await assertFails(aliceDb.collection("products").get()); // unfiltered blanket query rejected
  await assertFails(
    aliceDb.collection("products").doc("prod_01").update({
      priceTzs: 1,
      updatedAt: pastTs(),
    })
  );
});

// 4. Orders: Creation requires valid product, participant list query, refund rules & terminal state locking
test("Orders: buyer creates, advances, claims Rule A (100% refund), terminal lock blocks further updates", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  await assertSucceeds(bobDb.collection("products").doc("prod_01").set(validProduct(BOB_UID)));
  await assertSucceeds(aliceDb.collection("orders").doc("ord_01").set(validOrder(ALICE_UID, BOB_UID, "prod_01")));

  // Participant query succeeds; non-participant cannot read
  await assertSucceeds(
    aliceDb.collection("orders").where("participantIds", "array-contains", ALICE_UID).get()
  );
  await assertFails(charlieDb.collection("orders").doc("ord_01").get());

  // Provider advances delivery to IN_TRANSIT
  await assertSucceeds(
    bobDb.collection("orders").doc("ord_01").update({
      status: "IN_TRANSIT",
      driverLat: -6.805,
      driverLng: 39.271,
      updatedAt: pastTs(),
    })
  );

  // Rule A Damaged/Lost Refund: exact 100% refund succeeds; extra compensation rejected
  await assertFails(
    aliceDb.collection("orders").doc("ord_01").update({
      status: "DAMAGED_OR_LOST_REFUNDED",
      refundAmountTzs: 89500,
      disturbanceCompTzs: 1000,
      updatedAt: pastTs(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("orders").doc("ord_01").update({
      status: "DAMAGED_OR_LOST_REFUNDED",
      refundAmountTzs: 88500,
      disturbanceCompTzs: 0,
      updatedAt: pastTs(),
    })
  );

  // Terminal state lock: no updates allowed after DAMAGED_OR_LOST_REFUNDED
  await assertFails(
    aliceDb.collection("orders").doc("ord_01").update({
      status: "DELIVERED_COMPLETED",
      refundAmountTzs: 0,
      disturbanceCompTzs: 0,
      updatedAt: pastTs(),
    })
  );
});

// 5. Driver Contracts: Owner-isolated lease-to-own ledger & curfew flag
test("DriverContracts: driver owns ledger, cross-user access rejected, weeklyFeeTzs immutable", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertSucceeds(
    bobDb.collection("driver_contracts").doc(BOB_UID).set(validDriverContract(BOB_UID))
  );
  await assertFails(aliceDb.collection("driver_contracts").doc(BOB_UID).get());
  await assertFails(
    bobDb.collection("driver_contracts").doc(BOB_UID).update({
      weeklyFeeTzs: 100,
      updatedAt: pastTs(),
    })
  );
  await assertSucceeds(
    bobDb.collection("driver_contracts").doc(BOB_UID).update({
      weeksPaidCount: 15,
      totalPaidTzs: 1050000,
      updatedAt: pastTs(),
    })
  );
});

// 6. User Favorites: owner-isolated save, list, delete, and schema enforcement
test("Favorites: owner can save/list/remove favorite products; cross-user and mismatched productId fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const aliceFavRef = aliceDb.collection("users").doc(ALICE_UID).collection("favorites").doc("prod_01");
  await assertSucceeds(aliceFavRef.set({ productId: "prod_01", createdAt: pastTs() }));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("favorites").get());

  // Cross-user read/write rejected
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("favorites").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("favorites").doc("prod_01").set({
      productId: "prod_01",
      createdAt: pastTs(),
    })
  );

  // Mismatched productId rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("favorites").doc("prod_02").set({
      productId: "prod_99",
      createdAt: pastTs(),
    })
  );

  // Owner can unfavorite
  await assertSucceeds(aliceFavRef.delete());
});

function validCartItem(providerId, productId, quantity = 1) {
  return {
    productId,
    providerId,
    providerName: "Kariakoo Tech Hub",
    category: "ELECTRONICS",
    title: "Wireless Pro Gamepad",
    priceTzs: 85000,
    quantity,
    sizeSpec: "Standard Box",
    weightKg: 0.6,
    requiresTruck: false,
    pickupLabel: "Kariakoo Market Block A",
    createdAt: pastTs(),
    updatedAt: pastTs(),
  };
}

// 7. User Cart: owner-isolated add/list/update/remove, referenced product required, cross-user and invalid quantity denied
test("Cart: owner can add, update quantity, list, and remove; cross-user, ghost field, and missing product fail", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Unauthenticated access denied
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("cart").get());

  // Creating a cart item for a non-existent product is rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("cart").doc("missing_prod")
      .set(validCartItem(BOB_UID, "missing_prod"))
  );

  // Create referenced product
  await assertSucceeds(bobDb.collection("products").doc("prod_01").set(validProduct(BOB_UID)));

  const aliceCartRef = aliceDb.collection("users").doc(ALICE_UID).collection("cart").doc("prod_01");
  await assertSucceeds(aliceCartRef.set(validCartItem(BOB_UID, "prod_01", 2)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("cart").get());

  // Owner can update quantity
  await assertSucceeds(aliceCartRef.update({ quantity: 4, updatedAt: pastTs() }));

  // Shadow/ghost field or quantity 0 rejected
  await assertFails(aliceCartRef.update({ discountOverride: 99, updatedAt: pastTs() }));
  await assertFails(aliceCartRef.update({ quantity: 0, updatedAt: pastTs() }));

  // Cross-user read/write/delete rejected
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("cart").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("cart").doc("prod_01")
      .set(validCartItem(BOB_UID, "prod_01", 1))
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("cart").doc("prod_01").delete());

  // Owner can remove from cart
  await assertSucceeds(aliceCartRef.delete());
});
