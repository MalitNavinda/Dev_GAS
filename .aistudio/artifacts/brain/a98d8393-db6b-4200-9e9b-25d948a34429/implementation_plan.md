# LankaGas Live — Real-Time Sri Lanka LPG Stock & Queue Tracker

**LankaGas Live** eliminates wasted hours in cooking gas queues across Sri Lanka by uniting real-time Litro (Blue) and LAUGFS (Yellow) cylinder stock tracking (`12.5kg`, `5kg`, `2.3kg`), community-verified queue telemetry, official dealer dispatch updates, and administrative moderation inside one sunlight-readable, trilingual mobile application.

### User Review & Critical Decisions

> [!IMPORTANT]
> Please review the confirmed product scope and architectural blueprint below before we provision the live cloud database and build the application.

- **Confirmed Decision — Unified Three-Role Experience**: Consumer discovery & queue reporting, Distributor/Seller stock dashboard, and Admin verification & moderation are unified in one seamless mobile app with role-aware navigation.
- **Confirmed Decision — Spatial Map Canvas + GPS Distance List**: Consumers explore outlets via an interactive Sri Lanka spatial radar/map canvas (zoomable/pannable with color-coded stock pins and live GPS beacon) paired with a distance-sorted list view—requiring zero external paid map API keys.
- **Confirmed Decision — Trilingual Interface**: Instant in-app language switching across **English**, **Sinhala (සිංහල)**, and **Tamil (தமிழ்)** for inclusive nationwide accessibility.
- **Confirmed Decision — High-Contrast Civic Utility Theme**: Designed as **LankaGas Live** with high-contrast outdoor readability, unmistakable Litro Cobalt Blue and LAUGFS Amber Yellow brand badges, and traffic-light availability indicators.
- **Cloud Provisioning Gate (Next Step Upon Approval)**: Real-time multi-user stock counters, community queue reports, and role-based moderation use **Firebase Firestore** and **Google Sign-In** (via Android Credential Manager). Upon plan approval, you will be prompted once to accept Firebase setup so live cloud sync and zero-trust security rules are provisioned automatically.

---

### 1. Overview & Core Concept

- **What It Does**: Provides households and small businesses across all 25 Sri Lankan districts with live cylinder counts (`12.5kg`, `5kg`, `2.3kg`), delivery ETAs, queue lengths, and nearby restock alerts—while giving LPG dealers a 10-second stock update dashboard and administrators a verification and anti-spam console.
- **Target Audience / Personas**:
  - **Consumers (Households, Eateries, Bakeries)**: Citizens checking whether nearby Litro or LAUGFS dealers actually have cylinders before carrying an empty tank to join a queue.
  - **Distributors & Retail Dealers**: Local hardware stores, fuel stations, and authorized LPG agents broadcasting incoming lorry ETAs, opening stock counts, and instant "Sold Out" notices to prevent crowd friction outside their shops.
  - **Platform Administrators**: Trust & safety moderators verifying legitimate dealer registrations, resolving disputed crowd reports, and monitoring island-wide supply metrics.
- **Key Value**: Replaces rumor-driven WhatsApp messages and physical queue waiting with verified, timestamped stock telemetry and weighted community consensus.

---

### 2. User Experience & Visual Design

#### Key User Flows & Screen Wireframes

1. **Authentication & Trilingual Onboarding**: Users sign in via one-tap **Sign in with Google** (Credential Manager) and can switch between **EN / සිං / தமிழ்** at any time from the top utility bar. First-launch seeds realistic verified Sri Lankan Litro & LAUGFS outlets across Colombo, Gampaha, Kandy, Galle, Jaffna, and Kurunegala if the live database is empty, so the map and list are immediately interactive.
2. **Consumer Flow (Map & List -> Filter -> Outlet Detail -> Confirm / Watch)**:
   - Toggle between the **Interactive Spatial Map Canvas** and **GPS Distance List**, filter by Brand (`All`, `Litro`, `LAUGFS`), Cylinder Size (`12.5kg`, `5kg`, `2.3kg`), District/Search query, or `Available Only`.
   - Tap any station pin or card to inspect cylinder-by-cylinder counts, delivery lorry status, live queue length & estimated wait time, and community trust score.
   - Tap **"Confirm Stock / Report Queue"** to submit a proximity-checked crowd report, or tap **"Watch Station"** to receive an instant Android system notification when stock transitions to `In Transit` or `Available`.
3. **Distributor / Seller Flow (Register Outlet -> Dispatch Lifecycle -> Quick Decrement / Sold Out)**:
   - Switch to the **Seller** tab to register a new dealership (or manage owned outlets), set incoming truck ETA (`Scheduled` -> `In Transit`), tap **"Unload & Open Sales"** (`Available`), adjust live cylinder counts with rapid `-5 / -1 / +10` steppers, set official queue severity, or trigger the prominent **"Mark Sold Out"** action.
4. **Admin Panel Flow (Verify Dealers -> Moderate Flagged Reports -> Island Metrics)**:
   - Accessible to verified administrators (bootstrapped automatically for your account), providing one-tap approval/revocation of official dealer badges, resolution of flagged community reports, and live island-wide cylinder availability KPIs.

```
┌──────────────────────────────────────┐   ┌──────────────────────────────────────┐
│ LANKAGAS LIVE       [EN|සිං|தமிழ்] 🔔 │   │ ←  Perera Gas Center · Dehiwala   ⭐ │
├──────────────────────────────────────┤   ├──────────────────────────────────────┤
│ 🔍 Search town or dealer...  [📍GPS] │   │ [LITRO BLUE]  ✔ Verified Dealer      │
│ [All] [🔵 Litro] [🟡 LAUGFS] [12.5kg]│   │ Status: ● AVAILABLE (Updated 4m ago) │
├──────────────────────────────────────┤   ├──────────────────────────────────────┤
│ ┌────────── [Map] │ List ──────────┐ │   │ LIVE CYLINDER STOCK                  │
│ │      · Kandy (🟡 40)             │ │   │ ┌──────────┬──────────┬────────────┐ │
│ │   🔵 85     📍 You (Colombo)     │ │   │ │ 12.5 kg  │  5.0 kg  │   2.3 kg   │ │
│ │      🔴 0 (Sold Out)   🚚 ETA 2h │ │   │ │   48     │   19     │     12     │ │
│ │            · Galle (🔵 22)       │ │   │ │ In Stock │ In Stock │ Low Stock  │ │
│ └──────────────────────────────────┘ │   │ └──────────┴──────────┴────────────┘ │
│ NEARBY STATIONS (Sorted by 1.2 km)   │   │ QUEUE TELEMETRY (Trust Score: 92%)   │
│ ┌──────────────────────────────────┐ │   │ 👥 ~25 people · Est. wait ~35 mins   │
│ │ 🔵 Perera Enterprises  ● AVAIL   │ │   │ [👍 Confirm Accurate (18)] [🚩 Flag] │
│ │ 1.2 km · Queue: ~25 ppl (~35m)   │ │   │ [➕ Submit Live Queue / Stock Report]│
│ │ [12.5kg: 48] [5kg: 19] [2.3kg:12]│ │   ├──────────────────────────────────────┤
│ └──────────────────────────────────┘ │   │ [🔔 Watch Restock]  [🧭 Directions]  │
├──────────────────────────────────────┤   └──────────────────────────────────────┘
│  [🗺️ Explore] [🏪 Seller] [🛡️ Admin] │
└──────────────────────────────────────┘

┌──────────────────────────────────────┐
│ 🏪 SELLER DASHBOARD · My Dealership  │
├──────────────────────────────────────┤
│ Current State: [Scheduled]→[●Avail]  │
│ Truck Arrival / Note: [Unloaded 9AM] │
├──────────────────────────────────────┤
│ QUICK STOCK COUNTER                  │
│ 12.5 kg :  [-5] [-1] [  48  ] [+10]  │
│  5.0 kg :  [-5] [-1] [  19  ] [+10]  │
│  2.3 kg :  [-5] [-1] [  12  ] [+10]  │
├──────────────────────────────────────┤
│ Official Queue: [Short][●Mod][Long]  │
│ [💾 Publish Live Update]             │
│ [🛑 MARK ALL CYLINDERS SOLD OUT]     │
└──────────────────────────────────────┘
```

#### Visual Identity & Theme

- **Aesthetic Direction**: **Civic Utility & Sunlight-Ready High Contrast**—inspired by emergency logistics boards and railway dispatch signage. Prioritizes zero-ambiguity status scanning under harsh tropical midday sunlight while standing outdoors.
- **Color Palette & Mood** (Material 3 Semantic Tokens + Domain Badges):
  - `background` / `surface`: Crisp Warm Alabaster (`#F8F9FA`) in light mode and Deep Graphite (`#0F1419`) in dark mode, with elevated `surfaceContainerHigh` cards for tactile separation.
  - `primary`: Civic Navy (`#0B3C5D`) for authoritative navigation and primary actions; `secondary`: Sri Lankan Teal (`#0E7C7B`) for location and GPS telemetry.
  - **Brand Identity Accents**: Litro Gas Cobalt Blue (`#0057B8` fill / `#FFFFFF` label) and LAUGFS Gas Signal Yellow (`#F5B800` fill / `#141414` high-contrast dark label).
  - **Semantic Stock Status Tokens**:
    - `Available`: Signal Emerald (`#15803D`)
    - `In Transit` / `Low Stock (<10)`: Dispatch Amber (`#D97706`)
    - `Scheduled`: Slate Steel (`#475569`)
    - `Sold Out`: Alert Crimson (`#B91C1C`)
- **Typography & Hierarchy**:
  - Locally bundled Google Fonts: **Space Grotesk** (bold, tabular-friendly display headers and large cylinder stock counters so numbers never jitter when updating live) paired with **Plus Jakarta Sans** for clean UI legibility, with graceful system fallback rendering for **Sinhala** and **Tamil** Unicode scripts.
- **Component Styling & Adaptive Layout**:
  - Touch targets strictly `>= 48dp` with large pill filter chips and giant `-5 / -1 / +10` seller stepper buttons operable one-handed on a busy loading bay.
  - **Adaptive Responsive Support**: Compact portrait handhelds use a bottom `NavigationBar` and modal detail sheet; Medium/Expanded tablets and foldables switch to a side `NavigationRail` with a canonical **List/Map + Detail split pane**.
- **Interactive Feedback & Motion**:
  - Pulsing radar ring around the user's live GPS coordinate and `In Transit` delivery pins; smooth `<250ms` spring transitions when stock badges update via live Firestore snapshots; immediate haptic/snackbar confirmation on crowd reports and restock alerts.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1 — Native Android (Kotlin + Jetpack Compose) Implementation with Cross-Platform Blueprint Alignment**:
  - *Chosen Approach*: Build a production native Android application using **Kotlin, Jetpack Compose, Coroutines/Flow, and Material 3**, structured around clean MVVM + Repository boundaries that mirror reactive component architectures.
  - *Why*: In this Android build environment, native Jetpack Compose compiles directly to a fast, compact APK with zero JS-bridge overhead, smooth 60fps Canvas map rendering, native Android `CredentialManager` authentication, and direct `FusedLocationProviderClient` + `NotificationManager` hardware access—critical for budget Android smartphones widely used across Sri Lanka. *(Note: In cross-platform teams targeting iOS simultaneously, Flutter would edge out React Native due to Skia/Impeller's consistent custom map/badge rendering on low-RAM devices, using the exact same Firestore data contract below).*
- **Decision 2 — Backend Architecture: Firebase Firestore + Client Geohash/Bounding-Box Lookup**:
  - *Chosen Approach*: **Firebase Firestore (Enterprise)** with real-time `.snapshots()` Kotlin Flows, offline persistence cache, zero-trust security rules, and latitude/longitude + district + Geohash prefix indexing.
  - *Why*: During gas shortages, traffic spikes violently whenever a shipment arrives (95% read-heavy fan-out to thousands of anxious citizens watching the same 10 stations). Firestore natively absorbs massive concurrent real-time listener fan-out and caches last-known station stock offline during Sri Lankan power cuts or mobile data drops, without managing custom Node.js WebSocket clusters or Redis pub/sub servers.
  - *Alternatives Considered*: Custom Node.js + PostgreSQL/PostGIS offers richer polygon SQL queries (`ST_DWithin`), but requires operating WebSocket horizontal scaling and connection pooling under flash-crowd load. Geohash prefix filtering + Haversine client distance sorting achieves sub-100ms nearby lookups across Sri Lanka's ~1,500–3,000 outlets effortlessly.
- **Decision 3 — Anti-Spam Community Confirmation & Weighted Trust Engine**:
  - *Chosen Approach*: Low-friction 2-tap crowd reporting protected by four layered safeguards: (1) Google Sign-In UID accountability, (2) **GPS Proximity Verification** (`<= 1.5 km` from station earns a `2.0x` weight multiplier and "At Station" badge), (3) **Per-User Cooldown** (15-minute cooldown per station enforced in logic and rules-validated timestamps), and (4) **Official Seller Precedence** (crowd reports update the live Queue Estimate and Community Confidence score, and surface a "Crowd Reports Sold Out" warning banner if `>= 3` weighted reports conflict with stale seller stock, without destructively erasing an official verified dealer's inventory ledger).
  - *Why*: Keeps reporting effortless for citizens standing in line while preventing rival shops or internet trolls from remotely spoofing fake "Sold Out" or "Available" panics.
- **Decision 4 — Real-Time Watchlist & Local System Notifications**:
  - *Chosen Approach*: Users subscribe to specific stations ("Watch Restock"). Active Firestore snapshot listeners detect transitions into `IN_TRANSIT` or `AVAILABLE` (with stock > 0) and dispatch high-priority Android system notifications via `NotificationChannel` (`POST_NOTIFICATIONS` runtime permission), alongside an in-app Notification Center log.
  - *Why*: Works immediately out-of-the-box in the Android runtime without external server keys, while structuring watchlist subscriptions cleanly in Firestore so Cloud Functions + FCM background push can be attached in the Firebase Console for killed-app wakeups.

---

### 4. Technical Architecture & Data Strategy *(Technical Reference)*

#### System Architecture & Component Hierarchy

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        LANKAGAS LIVE ANDROID CLIENT                         │
│                                                                             │
│  ┌──────────────────────┐  ┌──────────────────────┐  ┌───────────────────┐  │
│  │   Consumer Screens   │  │   Seller Dashboard   │  │   Admin Console   │  │
│  │ • Spatial Map Canvas │  │ • Outlet Onboarding  │  │ • Dealer Verify   │  │
│  │ • GPS Station List   │  │ • 12.5/5/2.3Stepper  │  │ • Report Mod      │  │
│  │ • Outlet Detail Sheet│  │ • ETA & Dispatch     │  │ • Supply Metrics  │  │
│  │ • Watchlist & Alerts │  │ • 1-Tap Sold Out     │  │                   │  │
│  └──────────┬───────────┘  └──────────┬───────────┘  └─────────┬─────────┘  │
│             └─────────────────────────┼────────────────────────┘            │
│                                       ▼                                     │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                MVVM State Layer (StateFlow + UiState)                 │  │
│  │ • GasTrackerViewModel (Filters, Haversine Sort, Trust Score, Locale) │  │
│  │ • AuthViewModel (Credential Manager Google Sign-In & Role Resolution) │  │
│  └──────────────────┬─────────────────────────────────┬──────────────────┘  │
│                     ▼                                 ▼                     │
│  ┌────────────────────────────────────┐  ┌───────────────────────────────┐  │
│  │    Android Platform Services       │  │   Firestore Repository Layer  │  │
│  │ • FusedLocationProvider (Live GPS) │  │ • Named DB Instance           │  │
│  │ • NotificationManager (Restock)    │  │ • Real-time .snapshots() Flow │  │
│  │ • Trilingual String Catalog        │  │ • Structured Error Handler    │  │
│  └────────────────────────────────────┘  └───────────────┬───────────────┘  │
└──────────────────────────────────────────────────────────┼──────────────────┘
                                                           │
                                    Zero-Trust Security Rules + TLS Sync
                                                           ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                    FIREBASE CLOUD BACKEND (PROVISIONED)                     │
│  • Firebase Auth (Google Identity Provider)                                 │
│  • Firestore Collections:                                                   │
│    /users/{uid}              (Role, trustWeight, preferredLang, createdAt)  │
│    /distributors/{stationId} (Brand, geo/geohash, stock, status, queue)     │
│    /stockLogs/{logId}        (Immutable audit trail of stock transitions)   │
│    /queueReports/{reportId}  (Crowd queue/stock reports, proximity, flags)  │
│    /users/{uid}/watchlist/{stationId} (User-scoped restock subscriptions)   │
│    /admins/{uid}             (Verified administrator registry)              │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### Data Model & Firestore Schema

1. **`users/{userId}`** *(Owner/Admin isolated so PII is never exposed publicly)*:
   - `userId` (string, matches Auth UID), `displayName` (string), `role` (`"CONSUMER"` | `"SELLER"` | `"ADMIN"`), `reputationScore` (int, default `100`), `preferredLanguage` (`"EN"` | `"SI"` | `"TA"`), `createdAt` / `updatedAt` (server timestamps).
2. **`distributors/{stationId}`** *(Readable by authenticated users; creatable by authenticated sellers; updatable by station owner, admin, or strictly bounded crowd-summary action)*:
   - Identity & Geo: `ownerId` (UID), `name` (string), `brand` (`"LITRO"` | `"LAUGFS"`), `district` (string), `town` (string), `address` (string), `phone` (string), `latitude` (number), `longitude` (number), `geohash` (string, 6-char precision for fast spatial prefix lookup), `isVerified` (boolean — toggled by Admin).
   - Stock & Dispatch State: `status` (`"SCHEDULED"` | `"IN_TRANSIT"` | `"AVAILABLE"` | `"SOLD_OUT"`), `stock12_5kg` (int `>= 0`), `stock5kg` (int `>= 0`), `stock2_3kg` (int `>= 0`), `nextArrivalNote` (string, e.g. `"Today 14:30"`), `lastSellerUpdateAt` (timestamp).
   - Queue & Community Telemetry: `queueLevel` (`"NONE"` | `"SHORT"` | `"MODERATE"` | `"LONG"`), `estimatedQueueCount` (int), `confirmationsCount` (int), `discrepancyCount` (int), `createdAt` / `updatedAt` (timestamps).
3. **`stockLogs/{logId}`** *(Immutable append-only audit log of dealer stock changes)*:
   - `stationId`, `actorId` (UID), `previousStatus`, `newStatus`, `stock12_5kg`, `stock5kg`, `stock2_3kg`, `note`, `createdAt` (timestamp).
4. **`queueReports/{reportId}`** *(Crowdsourced live reports from consumers in queue)*:
   - `stationId`, `userId` (UID), `reporterName` (first name/alias), `reportedStatus` (`"AVAILABLE"` | `"LOW_STOCK"` | `"SOLD_OUT"`), `queueLengthEstimate` (int `0..500`), `queueLevel` (`"NONE"` | `"SHORT"` | `"MODERATE"` | `"LONG"`), `isProximityVerified` (boolean — GPS distance `<= 1.5km`), `weight` (int `1..3`), `moderationState` (`"ACTIVE"` | `"FLAGGED"` | `"DISMISSED"`), `comment` (string `<= 160` chars), `createdAt` / `updatedAt` (timestamps).
5. **`users/{userId}/watchlist/{stationId}`** *(Private per-user restock notification subscriptions)*:
   - `userId`, `stationId`, `stationName`, `brand`, `desiredSize` (`"ANY"` | `"12_5KG"` | `"5KG"` | `"2_3KG"`), `lastNotifiedStatus`, `createdAt` (timestamp).

#### Real-Time Stock & Queue State Machine

```
   [Dealer schedules truck]
              │
              ▼
      ┌───────────────┐
      │   SCHEDULED   │  (Badge: Slate · Shows expected date/time ETA)
      └───────┬───────┘
              │  Dealer marks lorry departed depot
              ▼
      ┌───────────────┐
      │  IN_TRANSIT   │  (Badge: Pulsing Amber · Triggers "Truck En Route" Watchlist Alert)
      └───────┬───────┘
              │  Dealer unloads & enters >0 cylinders (12.5kg / 5kg / 2.3kg)
              ▼
      ┌───────────────┐
      │   AVAILABLE   │◀─┐ (Badge: Emerald · Triggers "Gas Available Now!" Watchlist Alert)
      └───────┬───────┘  │
              │          │ Restock > 0 cylinders
   All 3 sizes == 0      │
   OR 1-Tap "Sold Out"   │
              ▼          │
      ┌───────────────┐  │
      │   SOLD_OUT    │──┘ (Badge: Crimson · Resets active queue wait-time warning)
      └───────────────┘
```

#### Interactive Component & State Mapping

- **Consumer Map & List Explorer**:
  - *Inputs*: Search text, Brand chip (`All` / `Litro` / `LAUGFS`), Size chip (`All` / `12.5kg` / `5kg` / `2.3kg`), District selector, `Available Only` switch, GPS permission grant.
  - *State Transitions*: Combines live Firestore `distributors` stream with `FusedLocationProviderClient` coordinates -> computes Haversine distance in km -> renders interactive zoom/pan Canvas pins (with tap hit-testing to select a station) and synchronized lazy list cards.
- **Crowd Confirmation & Queue Report Modal**:
  - *Inputs*: Observed stock radio (`Available` / `Running Low` / `Sold Out`), Queue slider (`0` to `200+` people, auto-computing estimated wait at `~1.4 min/person`), optional short tip, `Confirm` / `Flag` taps.
  - *Validation & State*: Checks 15-minute client cooldown per station, computes GPS distance to station (`<= 1.5 km` sets `isProximityVerified = true`), writes `queueReports` document + updates aggregated queue/confirmation counters on `distributors`, and displays a confirmation snackbar.
- **Seller Stock Dashboard**:
  - *Inputs*: Outlet registration form (with "Use Current Shop GPS" button or district coordinate picker), status selector, cylinder steppers (`12.5kg`, `5kg`, `2.3kg`), ETA text, `Publish Update` button, and `Mark Sold Out` emergency button.
  - *Validation & State*: Enforces non-negative integers (`0..5000`); if total cylinders == 0 when status is `AVAILABLE`, automatically coerces status to `SOLD_OUT`; writes updated `distributors` state and appends a `stockLogs` audit record atomically.
- **Admin Verification & Moderation Console**:
  - *Inputs*: Filter tabs (`Unverified Dealers`, `Flagged Reports`, `Island Overview`), `Verify / Revoke` dealer action, `Dismiss / Uphold` report moderation action.
  - *State Transitions*: Updates `isVerified` on `distributors` or `moderationState` on `queueReports` under admin security rules, immediately reflecting verified checkmarks across all connected consumer devices.
