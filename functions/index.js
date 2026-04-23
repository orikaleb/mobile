/**
 * NexiRide Cloud Functions.
 *
 * Today we only ship one function: `pushBroadcastNotification`. It listens
 * to writes under the `notifications` collection (created either by
 * `FirestoreAdminRepository.broadcastNotification` or as a side-effect of a
 * booking confirmation) and fans each document out to every device token
 * registered for the targeted user. The resulting push hits the Android
 * system tray via Firebase Cloud Messaging.
 *
 * Token layout (kept in sync by `FcmTokenManager.kt`):
 *   users/{uid}.fcmTokens   : string[]   (passenger devices)
 *   drivers/{uid}.fcmTokens : string[]   (driver devices)
 *
 * We merge both lists so an admin broadcast reaches a user regardless of
 * which portal they opened last. Tokens that the FCM backend reports as
 * invalid or unregistered are pruned from Firestore automatically so the
 * lists don't grow unbounded.
 */

const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { setGlobalOptions } = require("firebase-functions/v2");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");

admin.initializeApp();

// Keep regional settings close to the user base. Change this if you point
// `.firebaserc` at a different project/region.
setGlobalOptions({ region: "us-central1", maxInstances: 10 });

const db = admin.firestore();
const messaging = admin.messaging();

/**
 * Reads the target user's FCM tokens from both collections and returns a
 * deduplicated array. We look under `drivers` too so admins can broadcast to
 * bus drivers with the same write path.
 */
async function collectTokens(userId) {
  const [userSnap, driverSnap] = await Promise.all([
    db.collection("users").doc(userId).get(),
    db.collection("drivers").doc(userId).get(),
  ]);
  const tokens = new Set();
  [userSnap, driverSnap].forEach((snap) => {
    if (!snap.exists) return;
    const arr = snap.get("fcmTokens");
    if (Array.isArray(arr)) arr.forEach((t) => t && tokens.add(t));
  });
  return Array.from(tokens);
}

/**
 * Removes tokens the FCM backend rejected (unregistered / invalid-argument)
 * so they don't rack up failed deliveries on every future broadcast.
 */
async function pruneInvalidTokens(userId, tokens, responses) {
  const toRemove = [];
  responses.forEach((res, idx) => {
    if (res.success) return;
    const code = res.error && res.error.code;
    if (
      code === "messaging/registration-token-not-registered" ||
      code === "messaging/invalid-registration-token" ||
      code === "messaging/invalid-argument"
    ) {
      toRemove.push(tokens[idx]);
    }
  });
  if (toRemove.length === 0) return;
  const payload = { fcmTokens: admin.firestore.FieldValue.arrayRemove(...toRemove) };
  // Swallow failures — document might not exist for a driver-only or
  // passenger-only account, which is fine.
  await Promise.allSettled([
    db.collection("users").doc(userId).set(payload, { merge: true }),
    db.collection("drivers").doc(userId).set(payload, { merge: true }),
  ]);
  logger.info(`Pruned ${toRemove.length} stale tokens for ${userId}`);
}

exports.pushBroadcastNotification = onDocumentCreated(
  "notifications/{notifId}",
  async (event) => {
    const snap = event.data;
    if (!snap) return;
    const data = snap.data() || {};
    const userId = data.userId;
    const title = data.title || "NexiRide";
    const body = data.message || "";
    if (!userId || !body) {
      logger.warn("Skipping notification without userId/message", { id: snap.id });
      return;
    }

    const tokens = await collectTokens(userId);
    if (tokens.length === 0) {
      logger.info(`No FCM tokens for user ${userId}; skipping push.`);
      return;
    }

    // A "data" payload is included alongside the "notification" payload so
    // the client can deep-link later (e.g. jump straight to a booking).
    const message = {
      tokens,
      notification: { title, body },
      data: {
        notificationId: snap.id,
        type: String(data.type || "GENERAL"),
        bookingId: data.bookingId ? String(data.bookingId) : "",
      },
      android: {
        priority: "high",
        notification: {
          channelId: "nexiride_alerts",
          defaultSound: true,
        },
      },
    };

    const response = await messaging.sendEachForMulticast(message);
    logger.info(
      `Push fan-out for ${userId}: ${response.successCount} ok, ${response.failureCount} failed.`
    );
    if (response.failureCount > 0) {
      await pruneInvalidTokens(userId, tokens, response.responses);
    }
  }
);
