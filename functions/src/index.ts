/**
 * OPTIONAL — only for projects on the Blaze (pay-as-you-go) plan.
 *
 * BGLITDesk works fully on the free Spark plan without this file: the app writes notification
 * documents itself and shows them (live while open, and a background check about every 15 minutes).
 * Deploying this function adds *instant* push notifications when the app is closed.
 *
 *   cd functions && npm install && cd .. && firebase deploy --only functions
 */
import { initializeApp } from "firebase-admin/app";
import { getFirestore, FieldValue } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";
import { onDocumentCreated } from "firebase-functions/v2/firestore";

initializeApp();

const REGION = "asia-south1"; // same as the Firestore database

export const pushNotification = onDocumentCreated(
  { document: "notifications/{id}", region: REGION },
  async (event) => {
    const n = event.data?.data();
    if (!n) return;
    const db = getFirestore();

    const recipients =
      n.audience === "ADMINS"
        ? (await db.collection("users").where("role", "==", "ADMIN").get()).docs
        : [await db.collection("users").doc(String(n.audience)).get()];

    const tokenOwners = new Map<string, string>();
    for (const doc of recipients) {
      const u = doc.data();
      if (!u || u.isActive === false || doc.id === n.createdBy) continue;
      if (n.audience === "ADMINS" && u.isApproved !== true) continue;
      for (const t of (u.fcmTokens ?? []) as string[]) tokenOwners.set(t, doc.id);
    }
    const tokens = [...tokenOwners.keys()];
    if (tokens.length === 0) return;

    // Data-only message: the app shows it and de-duplicates it with the in-app notification.
    const res = await getMessaging().sendEachForMulticast({
      tokens,
      data: {
        notificationId: event.params.id,
        title: String(n.title ?? "BGLITDesk"),
        body: String(n.body ?? ""),
        ticketId: n.ticketId ? String(n.ticketId) : "",
      },
      android: { priority: "high" },
    });

    // Remove tokens for uninstalled apps.
    const stale: Promise<unknown>[] = [];
    res.responses.forEach((r, i) => {
      const code = r.error?.code ?? "";
      if (code.includes("registration-token-not-registered") || code.includes("invalid-argument")) {
        const uid = tokenOwners.get(tokens[i])!;
        stale.push(db.collection("users").doc(uid).update({ fcmTokens: FieldValue.arrayRemove(tokens[i]) }));
      }
    });
    await Promise.all(stale);
  },
);
