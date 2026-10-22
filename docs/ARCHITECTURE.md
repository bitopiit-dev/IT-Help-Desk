# Architecture

> **v2 (BGLITDesk) changes:** roles are now USER and ADMIN only; sign-up accepts any email; the app runs on the
> free Spark plan (ticket numbers via a counter transaction, SLA dates set on create, photos in Firestore,
> notifications in a `notifications` collection plus a WorkManager check); user and admin get separate
> navigation graphs and themes (`ui/user`, `ui/admin`), chosen in `ui/AppRoot.kt`. Sections below that mention
> technicians, Cloud Storage or Cloud Functions describe v1.

The app is one Android module organised in three layers. The domain layer holds every business rule as plain Kotlin with no Android or Firebase imports, which is why the status workflow, SLA and dashboard maths are unit-tested on the JVM and mirrored in the Cloud Functions.

## Layers

```
app/src/main/java/com/company/ithelpdesk/
├── domain/                 pure Kotlin
│   ├── model/              Ticket, UserProfile, Comment, StatusChange, SlaSettings, Category, Department
│   ├── logic/              StatusWorkflow, TicketTimes, SlaCalculator, DashboardCalculator
│   └── repository/         interfaces the UI depends on
├── data/                   Firebase implementations
│   ├── *RepositoryImpl.kt  Firestore + Storage access, explicit field mapping (Mappers.kt)
│   ├── SessionManager.kt   signed-in user, profile and role as one StateFlow; registers FCM tokens
│   └── messaging/          FirebaseMessagingService + notification channel
├── di/                     Hilt modules
├── ui/                     Compose screens, one ViewModel per screen
│   ├── auth/  employee/  technician/  ticket/  admin/  profile/  home/
│   ├── components/         chips, cards, filters, charts
│   ├── navigation/         routes and NavHost
│   └── theme/              light/dark colour schemes, status colours
└── util/                   formatting, CSV export, image compression
```

ViewModels expose `StateFlow` built from Firestore snapshot listeners, so every screen is live. Writes use batched writes and return `SYNCED` or `QUEUED`, so the UI can tell users when something was saved offline.

## Screens and navigation

```
Signed out:  Login ⇄ Register
Signed in, no profile:   Complete profile
Signed in, unapproved:   Waiting for approval
Signed in, deactivated:  Account deactivated

Home (bottom bar depends on role)
├── Employee:   My tickets · Profile
├── Technician: Queue · My requests · Profile
└── Admin:      Dashboard · Queue · My requests · Settings · Profile

From Home:
  My tickets ──(FAB)──▶ New ticket ──▶ Ticket detail
  Any list ──────────────────────────▶ Ticket detail (actions depend on role)
  Dashboard workload row ────────────▶ Technician's tickets ──▶ Ticket detail
  Settings ──▶ Users & roles | Categories & auto-assign | Departments | SLA targets
  Push notification ─────────────────▶ Ticket detail
```

## Data model (Firestore)

| Collection | Key fields | Written by |
|---|---|---|
| `users/{uid}` | name, email, employeeId, department, phone, location, role, isApproved, isActive, fcmTokens[] | user (contact details), admin (role, approval) |
| `tickets/{id}` | ticketNumber, title, description, category, subCategory, priority, status, requester*, assignee*, attachments[], deviceInfo, assetTag, contactMethod, createdAt, firstResponseAt, resolvedAt, closedAt, onHoldSince, onHoldTotalMs, slaResponseDueAt, slaResolveDueAt, slaState, rating, resolution{}, reopenCount, lastChangedBy | app + functions |
| `tickets/{id}/comments` | authorId, authorName, authorRole, text, images[], isInternal, createdAt | app |
| `tickets/{id}/statusHistory` | from, to, changedBy, changedByName, changedAt, note | app + functions |
| `categories` | name, subCategories[], autoAssign, defaultAssigneeId, roundRobinIndex | admin, functions |
| `departments` | name | admin |
| `settings/sla` | per priority: respondMinutes, resolveMinutes | admin |
| `statsDaily/{yyyy-MM-dd}` | created, resolved, repairMsTotal, activeRepairMsTotal, slaMet, slaBreached, ratings, ratingTotal, reopened | functions only |
| `counters/tickets-{YYMMDD}` | value (daily sequence for BGL-YYMMDD-XXXX) | app, in the ticket-create transaction |

## Decisions that differ from the original brief

- **Daily stats live in `statsDaily/{date}`** instead of `stats/daily/{date}`. Firestore paths alternate collection/document, so the original path would make each day a collection rather than a document.
- **Charts are drawn with Compose** (`ui/components/Charts.kt`) instead of the Vico library. The dashboard needs a donut, bar lists and a two-series line; a small in-house implementation avoids tracking Vico's frequent API changes and exposes every chart's numbers to TalkBack.
- **The dashboard reads live tickets** (three listeners: created in range, resolved in range, all open) and computes metrics on the device. This keeps any filter combination exact and real time. `statsDaily` is maintained alongside it for long-range reporting; if ticket volume grows past a few thousand per month, switch the dashboard's long ranges to read `statsDaily`.
- **The SLA resolve clock pauses while on hold.** The brief did not specify this; without it, waiting for the user or for parts would count against IT.
- **Device clock vs server clock.** Event times (created, first response, resolved) use server timestamps. Only the on-hold duration is computed from the device clock, because Firestore cannot subtract timestamps in a write.

## Security model

Roles live on `users/{uid}.role` and every rule reads them from there, so changing a role in the app takes effect immediately and cannot be spoofed by a modified client. Highlights:

- Self-registration is limited to the company domain and always creates an unapproved employee.
- Employees read only tickets they raised and only non-internal comments.
- Requesters can cancel (before work), confirm, reopen and rate, nothing else.
- Technicians can assign tickets only to themselves; admins assign anyone.
- Nobody can edit SLA due dates or ticket numbers from the app; Cloud Functions set them.
- Stats and counters are written only by Cloud Functions.
