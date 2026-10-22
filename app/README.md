# BGL-IT Help (Android)

BGL-IT Help is the BGL IT helpdesk app. Anyone signs up with **any email address**, an **admin approves** the request, and the person lands in one of **two completely separate apps** inside the same APK:

- **User app** – bright violet/pink/orange design. Raise a ticket in under a minute (category tiles, urgency, photos, barcode for asset tag), follow friendly progress steps, chat with IT, confirm the fix, reopen or rate.
- **Admin app** – dark neon "command centre". Approve or decline sign-ups, manage people and roles, work the full ticket queue (search, filters, SLA), assign, change status with internal notes, see the live dashboard and export CSV, edit categories, departments and SLA targets.

Kotlin + Jetpack Compose (Material 3), Hilt, Navigation Compose, Firebase Auth + Firestore.

## Runs on the free Firebase plan, from any network

The project is on the **Spark (free)** plan, so nothing depends on Cloud Functions or Cloud Storage:

| Feature | How it works without a paid plan |
|---|---|
| Ticket numbers (`BGL-261022-0042`, i.e. BGL-YYMMDD-XXXX, restarts at 0001 each day, Bangladesh time) | Firestore transaction on `counters/tickets-{YYMMDD}` |
| SLA due times | Set by the app when the ticket is created (from `settings/sla`) |
| Photos | Compressed to ≤ 600 KB JPEG and stored in Firestore (`tickets/{id}/images`) |
| Notifications | App writes `notifications/{id}`; shown live while the app runs and by a background check every ~15 min |
| Instant push when closed | *Optional:* deploy `functions/` on the Blaze plan |

The app talks to the real Firebase project over HTTPS, so it works on office Wi-Fi, home broadband and mobile data. **Do not** point it at the Firebase Emulator or a LAN address (`192.168.x.x`) – that only works inside one network.

## Setup (about 15 minutes)

1. **Firebase console → Authentication → Sign-in method →** enable **Email/Password**.
2. **Project settings → Your apps → Android app** with package name `com.company.ithelpdesk` (keep it – it must match your existing `google-services.json`). Put `google-services.json` in `app/`.
3. Deploy the security rules and indexes (needs Node.js and `npm i -g firebase-tools`):
   ```bash
   firebase login
   firebase use bglithelpdesk
   firebase deploy --only firestore:rules,firestore:indexes
   ```
   Index builds take a few minutes; until then a list may show "index required".
4. Open the folder in **Android Studio** (Koala or newer), let Gradle sync, and run on a phone or an emulator with Google Play.
5. **First admin:** already done for `bgl.it@bitopibd.com` (role `ADMIN`, `isApproved: true`). For anyone else, set those two fields once in Firestore → `users/{uid}`; after that, promote people from **People → ⋮ → Make admin**.
6. In the admin app: **Setup → Categories → Add default categories**, then add **Departments** (they appear on the sign-up form).

### Release APK
Build → Generate Signed App Bundle / APK → APK → `release`. Add the keystore's SHA-1/SHA-256 to the Android app in Firebase project settings and download the updated `google-services.json`.

### Optional: instant push (Blaze plan only)
```bash
cd functions && npm install && cd ..
firebase deploy --only functions
```
The function sends a push for every new notification document. The app de-duplicates it with the in-app notification.

## Sign-up and approval flow

```
Sign up (any email) → users/{uid} {role: USER, isApproved: false} + alert to all admins
      ↓ admin taps Approve in People › Pending
User app opens automatically (no need to sign in again)
Decline / Block → "Access not available" screen; can be restored from People › Blocked
```

## Tests

```bash
./gradlew testDebugUnitTest                 # workflow, SLA, dashboard, roles, notifications (JVM)
./gradlew connectedAndroidTest              # Compose UI tests for the ticket dialogs (device needed)
cd rules-tests && npm install && npm test   # security rules on the Firestore emulator (needs Java)
```

## Build status of this delivery

The domain rules (status workflow, SLA, times, dashboard, roles, notifications, ticket numbers) were compiled with Kotlin 2.0.20 and their 28 unit tests pass. The optional Cloud Function type-checks under `tsc --strict`. The full Android build and the emulator rules tests could not be run where this was produced (no access to Google's Maven repository or the emulator download). Run `./gradlew assembleDebug` in Android Studio; if anything fails, the error names the file and line.

## Copyright

© 2026 Khondaker Arifur Rahman. All Rights Reserved. See [COPYRIGHT.md](COPYRIGHT.md) for the full notice and the third-party components the app uses.
