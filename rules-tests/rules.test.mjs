// Security rules tests for BGLITDesk. Run from this folder:  npm install && npm test
// (needs the Firebase CLI and Java for the Firestore emulator).
import { test, before, after, beforeEach } from "node:test";
import { readFileSync } from "node:fs";
import { initializeTestEnvironment, assertFails, assertSucceeds } from "@firebase/rules-unit-testing";
import {
  doc, getDoc, setDoc, updateDoc, collection, getDocs, query, where, serverTimestamp, arrayUnion,
} from "firebase/firestore";

let env;

const users = {
  emp: { name: "Rahim", email: "rahim@gmail.com", role: "USER", isApproved: true, isActive: true },
  other: { name: "Sumi", email: "sumi@yahoo.com", role: "USER", isApproved: true, isActive: true },
  pending: { name: "New", email: "new@outlook.com", role: "USER", isApproved: false, isActive: true },
  admin: { name: "Nadia", email: "nadia@bitopibd.com", role: "ADMIN", isApproved: true, isActive: true },
  admin2: { name: "Karim", email: "karim@bitopibd.com", role: "ADMIN", isApproved: true, isActive: true },
};

const ticket = (overrides = {}) => ({
  ticketNumber: "", title: "Printer jam", description: "Tray 2", category: "Printer", priority: "MEDIUM",
  status: "NEW", requesterId: "emp", requesterName: "Rahim", assigneeId: null, assigneeName: null,
  attachments: [], createdAt: serverTimestamp(), ...overrides,
});

const as = (uid) => env.authenticatedContext(uid, { email: users[uid]?.email ?? `${uid}@example.com` }).firestore();

const notification = (overrides = {}) => ({
  audience: "ADMINS", title: "t", body: "b", ticketId: null, kind: "TICKET", createdBy: "emp",
  createdAt: serverTimestamp(), readBy: [], ...overrides,
});

before(async () => {
  env = await initializeTestEnvironment({
    projectId: "demo-bglitdesk",
    firestore: { rules: readFileSync("../firestore.rules", "utf8") },
  });
});
after(async () => env.cleanup());

beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    for (const [uid, u] of Object.entries(users)) await setDoc(doc(db, "users", uid), u);
    await setDoc(doc(db, "tickets", "t1"), { ...ticket(), createdAt: new Date() });
    await setDoc(doc(db, "tickets", "t1", "comments", "c1"), { authorId: "admin", text: "public", isInternal: false });
    await setDoc(doc(db, "tickets", "t1", "comments", "c2"), { authorId: "admin", text: "secret", isInternal: true });
    await setDoc(doc(db, "notifications", "n1"), { ...notification({ audience: "emp", createdBy: "admin" }), createdAt: new Date() });
  });
});

test("anyone can sign up with any email, but only as an unapproved USER", async () => {
  const gmail = env.authenticatedContext("g", { email: "someone@gmail.com" }).firestore();
  await assertFails(setDoc(doc(gmail, "users", "g"), { name: "G", email: "someone@gmail.com", role: "ADMIN", isApproved: false, isActive: true }));
  await assertFails(setDoc(doc(gmail, "users", "g"), { name: "G", email: "someone@gmail.com", role: "USER", isApproved: true, isActive: true }));
  await assertFails(setDoc(doc(gmail, "users", "g"), { name: "G", email: "fake@bitopibd.com", role: "USER", isApproved: false, isActive: true }));
  await assertSucceeds(setDoc(doc(gmail, "users", "g"), { name: "G", email: "someone@gmail.com", role: "USER", isApproved: false, isActive: true }));
});

test("a new user can tell admins about their sign-up, nothing else", async () => {
  await assertSucceeds(setDoc(doc(as("pending"), "notifications", "s1"), notification({ kind: "SIGNUP", createdBy: "pending" })));
  await assertFails(setDoc(doc(as("pending"), "notifications", "s2"), notification({ kind: "TICKET", createdBy: "pending" })));
});

test("only admins approve people or change roles", async () => {
  await assertFails(updateDoc(doc(as("pending"), "users", "pending"), { isApproved: true }));
  await assertFails(updateDoc(doc(as("emp"), "users", "emp"), { role: "ADMIN" }));
  await assertSucceeds(updateDoc(doc(as("emp"), "users", "emp"), { phone: "01700000000" }));
  await assertSucceeds(updateDoc(doc(as("admin"), "users", "pending"), { isApproved: true }));
  await assertSucceeds(updateDoc(doc(as("admin"), "users", "emp"), { role: "ADMIN" }));
  await assertFails(updateDoc(doc(as("admin"), "users", "admin"), { role: "USER" }));
});

test("users cannot list other users; admins can", async () => {
  await assertFails(getDocs(collection(as("emp"), "users")));
  await assertSucceeds(getDocs(collection(as("admin"), "users")));
});

test("pending users cannot create tickets; approved users can, with a number from the counter", async () => {
  await assertFails(setDoc(doc(as("pending"), "tickets", "p1"), ticket({ requesterId: "pending" })));
  await assertSucceeds(setDoc(doc(as("emp"), "tickets", "t2"), ticket()));
  await assertSucceeds(setDoc(doc(as("emp"), "tickets", "t3"), ticket({ ticketNumber: "BGL-261022-0001" })));
  await assertSucceeds(setDoc(doc(as("emp"), "tickets", "t3old"), ticket({ ticketNumber: "BGL-2026-0001" })));
  await assertFails(setDoc(doc(as("emp"), "tickets", "t3bad"), ticket({ ticketNumber: "BGL-26102-0001" })));
  await assertFails(setDoc(doc(as("emp"), "tickets", "t4"), ticket({ ticketNumber: "HACK" })));
  await assertFails(setDoc(doc(as("emp"), "tickets", "t5"), ticket({ assigneeId: "admin" })));
});

test("counter can only go up by one", async () => {
  await assertSucceeds(setDoc(doc(as("emp"), "counters", "tickets-261022"), { value: 1 }));
  await assertFails(setDoc(doc(as("emp"), "counters", "tickets-261022"), { value: 5 }));
  await assertSucceeds(setDoc(doc(as("emp"), "counters", "tickets-261022"), { value: 2 }));
  await assertFails(setDoc(doc(as("emp"), "counters", "tickets-abc"), { value: 1 }));
});

test("users see only their own tickets", async () => {
  await assertSucceeds(getDoc(doc(as("emp"), "tickets", "t1")));
  await assertFails(getDoc(doc(as("other"), "tickets", "t1")));
  await assertSucceeds(getDoc(doc(as("admin"), "tickets", "t1")));
});

test("requester may cancel a new ticket but not resolve it; admins can", async () => {
  await assertFails(updateDoc(doc(as("emp"), "tickets", "t1"), { status: "RESOLVED" }));
  await assertSucceeds(updateDoc(doc(as("admin"), "tickets", "t1"), { status: "ASSIGNED", assigneeId: "admin2" }));
  await assertSucceeds(updateDoc(doc(as("emp"), "tickets", "t1"), { status: "CANCELLED" }));
});

test("internal notes are hidden from the requester", async () => {
  const db = as("emp");
  await assertSucceeds(getDocs(query(collection(db, "tickets", "t1", "comments"), where("isInternal", "==", false))));
  await assertFails(getDocs(collection(db, "tickets", "t1", "comments")));
  await assertFails(setDoc(doc(db, "tickets", "t1", "comments", "x"), { authorId: "emp", text: "hi", isInternal: true }));
});

test("photos: uploader and requester can read, strangers cannot", async () => {
  await assertSucceeds(setDoc(doc(as("admin"), "tickets", "t1", "images", "i1"), { data: "abc", createdBy: "admin" }));
  await assertSucceeds(getDoc(doc(as("emp"), "tickets", "t1", "images", "i1")));
  await assertFails(getDoc(doc(as("other"), "tickets", "t1", "images", "i1")));
});

test("notifications: own feed only, and you can only mark yourself as read", async () => {
  await assertSucceeds(getDocs(query(collection(as("emp"), "notifications"), where("audience", "in", ["emp"]))));
  await assertFails(getDocs(query(collection(as("other"), "notifications"), where("audience", "in", ["emp"]))));
  await assertSucceeds(getDocs(query(collection(as("admin"), "notifications"), where("audience", "in", ["admin", "ADMINS"]))));
  await assertSucceeds(updateDoc(doc(as("emp"), "notifications", "n1"), { readBy: arrayUnion("emp") }));
  await assertFails(updateDoc(doc(as("emp"), "notifications", "n1"), { readBy: arrayUnion("someoneElse") }));
});
