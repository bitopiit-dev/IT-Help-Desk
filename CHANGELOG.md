# Changelog

## 2.1.1

### Ticket numbers: BGL-YYMMDD-XXXX
- New tickets are numbered like `BGL-261022-0007` (7th ticket of 22 Oct 2026). The last part restarts at 0001 every day.
- The date is Bangladesh time (Asia/Dhaka), whatever time zone the phone is set to.
- Counter documents are now one per day: `counters/tickets-YYMMDD`. Old `tickets-YYYY` counters are left alone.
- Existing tickets keep their old numbers. Firestore rules accept both formats on create for now, so phones still on
  2.0 can raise tickets until they update. **Deploy the rules:** `firebase deploy --only firestore:rules`.

### New category: Item Request 📦
- Added to the built-in categories, so it shows in Quick start and on the new-ticket form.
- "Narrow it down" lists: Mouse, Keyboard, Monitor, PC, Laptop, RAM, SSD, HDD, Data Cable, USB Hub, Type-C Cable,
  LAN Cable, Android Charger, Tab, LAN Phone, Scanner, Printer Head, Speaker, PA System, Camera, Conferencing Camera,
  Laptop Bag, Wi-Fi Router, Headphone / Headset, Pen Drive, Toner / Cartridge, UPS, Multi-plug, Other.
- Projects that already have categories in Firestore: Admin → Setup → Categories shows an "Add Item Request" card.
  It adds just this one category and changes nothing else.
- For item requests the form asks "What do you need?" with matching examples, and no quick fixes are shown.

### Tests
- Ticket number tests updated, plus Bangladesh-time and Item Request tests. Domain tests: 41 pass.
- Rules tests updated for the new counter id and number format.


## 2.1.0 — modernisation pass 1

### Platform
- Toolchain and libraries updated: AGP 8.7.3, Kotlin 2.1.0 (KSP 2.1.0-1.0.29), Hilt 2.54, Compose BOM 2025.01.00,
  Activity 1.10.0, Core 1.15.0, Lifecycle 2.8.7, Navigation 2.8.5, Firebase BOM 33.7.0, Coroutines 1.9.0, WorkManager 2.10.0.
  Gradle wrapper stays on 8.9 (works with AGP 8.7). Android Studio may offer newer versions; take them one group at a time.
- New: `core-splashscreen`, `biometric`, `fragment-ktx`.
- `MainActivity` is now a `FragmentActivity` (needed for the system biometric prompt).
- Predictive back gesture enabled (`android:enableOnBackInvokedCallback`).

### Look and feel
- System splash screen (Android 12 style, back-ported to 8.0+). It stays up until the signed-in session is known,
  at most 2.5 s, so there is no flash of the loading logo on start.
- Skeleton loading: ticket lists, notifications and people show shimmering placeholders instead of a spinner.
  The shimmer stops when the phone has animations turned off.
- List rows animate when they are added, removed or re-ordered (live Firestore updates no longer "jump").
- Faster screen transitions: short fade with a small push, replacing the default 700 ms crossfade. Works with predictive back.
- Admin app on tablets, foldables and landscape (≥ 600 dp wide): side navigation rail instead of the bottom bar.

### Features
- **Quick fixes on the new-ticket form (user app).** As someone picks a category or types, up to two matching
  self-help fixes appear ("Try this first"). "That fixed it, close the form" leaves without sending a ticket.
  12 built-in fixes (Wi-Fi, slow internet, VPN, printer, paper jam, Outlook, mailbox full, password, app crash,
  monitor, keyboard/mouse, slow PC). Edit them in `domain/logic/SelfHelp.kt`. Runs offline, no Firebase cost.
- **Quick replies (admin app).** One-tap reply chips above the message box while it is empty, with the requester's
  first name filled in. Hidden while "Internal note" is on. Edit them in `domain/logic/QuickReplies.kt`.
- **App lock (admin app).** Profile → App lock. When on, the admin app asks for fingerprint, face or the phone's
  screen lock on start and after a minute in the background. The screen underneath keeps its place.
  The setting is per device. If the phone's screen lock is removed later, the app unlocks instead of locking the admin out.

### Tests
- New JVM tests: `SelfHelpTest`, `QuickRepliesTest` (10 tests). Domain tests: 38 pass.

### Files
New: `ui/components/Skeleton.kt`, `ui/components/NavMotion.kt`, `ui/user/QuickFixCard.kt`, `ui/admin/AppLock.kt`,
`data/AppLockStore.kt`, `domain/logic/SelfHelp.kt`, `domain/logic/QuickReplies.kt`, `test/.../SelfHelpTest.kt`.
Changed: `libs.versions.toml`, `app/build.gradle.kts`, `AndroidManifest.xml`, `values*/themes.xml`, `MainActivity.kt`,
`AppRoot.kt`, `AdminShell.kt`, `UserShell.kt`, `AdminTicketScreen.kt`, `AdminTicketsScreen.kt`, `PeopleScreen.kt`,
`NotificationsScreen.kt`, `UserHomeScreen.kt`, `NewTicketScreen.kt`, `ChatInput.kt`.
