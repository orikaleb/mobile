# NexiRide2

Intercity bus discovery, booking, and ticket management for Android (Jetpack Compose).

This document maps the project to typical **mobile computing coursework deliverables** (overview, architecture, data, sensors, UI).

---

## 1. Project overview

**Problem statement**  
Many travelers still rely on informal channels for intercity buses: schedules and seat availability are unclear, tracking a trip is hard, and tickets are not always available when the network drops. NexiRide2 is a prototype that centralizes search and booking, shows a live map for an active trip, and keeps PDF tickets on device after download.

**Target audience**  
Passengers using **Android phones** (minSdk 24), including **mid-range devices** and users with **intermittent mobile data**. The app avoids assuming a always-on connection by caching route search results and storing downloaded ticket PDFs locally.

---

## 2. System architecture

**Design pattern: MVVM**  
Compose **screens** observe **ViewModels** (`SearchViewModel`, `LiveTrackingViewModel`, etc.). ViewModels call **use cases** and **repository interfaces** in the **domain** layer. **Data** implements repositories: **Supabase Postgres** (via **PostgREST** and Retrofit) for routes, bookings, seats, profiles, payment methods/transactions, and in-app notifications; **Supabase Auth** (supabase-kt) for sign-in; **Room** for on-device route cache and downloaded ticket PDFs. **Dependency injection** is Hilt.

**Architecture diagram (data flow)**

```mermaid
flowchart LR
    UI[Compose UI] --> VM[ViewModel]
    VM --> UC[Use cases]
    UC --> BR[BusRepository]
    UC --> BK[BookingRepository]
    BR --> CACHE[Room route_cache]
    BR --> REMOTE[Supabase PostgREST routes]
    BK --> SB[Supabase bookings]
    VM --> UI
```

**External APIs and services**

| Service | Role |
|--------|------|
| **Google Maps SDK / Maps Compose** | Map and markers on live tracking; **Google Play Services Location (Fused)** for GPS updates. |
| **Retrofit + OkHttp** | **`SupabaseModule`** exposes `SupabasePostgrestApi` against `…/rest/v1/` with `apikey` + `Authorization: Bearer <user JWT or anon>` (JWT from **`SupabaseAuthRepository`** session when signed in). |
| **Supabase (Postgres + PostgREST)** | Tables **`routes`**, **`bookings`**, **`cities`**, **`route_seats`**, **`profiles`**, **`payment_methods`**, **`payment_transactions`**, **`notifications`** (see `supabase/schema.sql`). Debug **`SupabaseSeed`** fills empty **routes**, **cities**, and **route_seats** from bundled `MockData`. **Bookings** are created only after **real sign-up** (they reference `auth.users`). |
| **Supabase Auth** | Wired via **`SupabaseAuthRepository`** (`auth-kt`: email/password sign-in and sign-up, password reset email, session-backed Retrofit calls). For local dev, disable **email confirmations** under **Authentication → Providers → Email** so `signUp` returns a session immediately. |

---

## 3. Data management

**Local persistence (Room)**

| Table | Purpose |
|-------|---------|
| `downloaded_tickets` | `bookingId`, `referenceCode`, `createdAtEpochMs`, `pdfPath` — offline PDF ticket files. |
| `route_cache` | `cacheKey`, `routesJson`, `updatedAtEpochMs` — cached **route lists** (search + popular routes) as JSON. |

**Sync / offline strategy: cache-then-network (with offline read)**  

- **Online:** `CachingBusRepository` calls **`SupabaseBusRepository`** (PostgREST), then **writes** the JSON payload to `route_cache` under a stable key (e.g. `search:origin|destination|date|passengers` or `cache:popular_routes`).  
- **Offline:** If `ConnectivityManager` reports no usable network, search loads **only** from `route_cache` for that key; if nothing was cached yet, the user sees a clear error asking them to search once while online.  
- **PDF tickets:** `DownloadedTicketRepositoryImpl` writes files and metadata so tickets stay usable without refetching the booking.

*(Room schema version was bumped; debug builds use destructive migration—fine for class demos; production would ship proper migrations.)*

---

## 4. Hardware and sensor integration

At least **three** sensors are used, with processing as follows.

| Sensor | Where | Raw signal → logic → effect |
|--------|--------|-----------------------------|
| **GPS (Fused Location Provider)** | Live tracking | `LocationCallback` receives lat/long → `LatLng` in `LiveTrackingViewModel` → **user marker** on the map and **haversine** straight-line **distance (km)** to the simulated bus position. |
| **Accelerometer** | Search | `SensorManager` `TYPE_ACCELEROMETER` → `ShakeDetector` compares successive acceleration magnitudes; when **jerk** exceeds a threshold and **cooldown** elapsed → triggers **`SearchViewModel.search()`** (shake to refresh). |
| **Proximity** | Ticket detail | `TYPE_PROXIMITY` distance vs `maximumRange` (binary on some devices) → **“near”** state → **QR and share actions hidden** and a privacy overlay shown (e.g. phone near face / in pocket). |

**Permissions:** `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION` for fused location on the tracking screen.

---

## 5. User interface design

**Wireframes / screenshots**  
Attach **your own** screenshots from the emulator or device: Splash, Home, Search, Results, Seat selection, Ticket (with QR), Live tracking map, Profile.

**Responsiveness**  
Layouts use **Compose `dp`** and flexible modifiers (`fillMaxWidth`, `weight`, scroll) so density scales correctly. **No activity-level orientation lock**—the UI reflows in landscape where scrollable content allows. For the coursework write-up, note that **Material 3** spacing and type scales with system font/size settings where applicable.

---

## Supabase setup

1. Create a project at [Supabase](https://supabase.com/) and open **SQL** → run `supabase/schema.sql` (creates tables, RLS policies, `profiles` trigger on `auth.users`, and links `bookings.user_id` to `auth.users`). Use a **new** project or drop conflicting `public` tables first.
2. Under **Project Settings → API**, copy the **Project URL** and **anon public** key.
3. Add to your machine’s **`local.properties`** (not committed):

   ```properties
   supabase.url=https://YOUR_REF.supabase.co
   supabase.anon.key=<anon JWT or sb_publishable_… from Project Settings → API>
   ```

   See `local.properties.example`. **Never commit keys** or paste them into `MainActivity.kt`; the Supabase “Connect” snippet is mapped in code to **`SupabaseModule.provideSupabaseClient()`** (`createSupabaseClient { install(Auth); install(Postgrest) }`) using **`BuildConfig`** values from `local.properties`.

4. **Dashboard “Kotlin” snippet vs this app:** The docs show a top-level `val supabase = createSupabaseClient(...)` in `MainActivity`. Here you inject **`SupabaseClient`** (Hilt) or use **`SupabasePostgrestApi`** (Retrofit). For `decodeList<MyDto>()`, use **`@Serializable`** DTOs (see `SupabaseDtos.kt`); the Kotlin serialization plugin is enabled in `:app`.

5. **RLS:** `schema.sql` enables row-level security with **dev-oriented** policies (e.g. anon can insert routes/cities/seats for the in-app seed). **Tighten** route/seat insert policies before production; prefer **service-role** seeding or SQL-only seeds for prod.
6. **Debug auto-seed:** With a non-blank anon key, first **debug** launch runs **`SupabaseSeed`** for empty **routes**, **cities**, and **route_seats** only. **Bookings** are not seeded (they require a real `auth` user).
7. **Payments:** The app records **`payment_transactions`** and stores saved methods in **`payment_methods`** (ledger in your database). Integrating **Paystack / Stripe** still requires your backend or Edge Functions for client secrets and webhooks.
8. **Push (FCM):** In-app notifications sync from **`notifications`** via PostgREST. **Firebase Cloud Messaging** is not bundled (no `google-services.json` in repo); add FCM if you need push when the app is closed.

---

## Build

Requires Android SDK and a **full JDK 17+** (with `jlink`), not a minimal JRE. From project root:

```bash
./gradlew assembleDebug
```

**If the build fails with `jlink ... does not exist` (often under a `.cursor` or VS Code Java extension path):**

1. **Android Studio:** **Settings → Build, Execution, Deployment → Gradle → Gradle JDK** → choose **Project SDK** or **Embedded JDK** (not a “JRE” from another editor).
2. This repo pins **`.idea/gradle.xml` → Gradle JDK** to **`/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home`** (full JDK with `jlink`). On **Windows/Linux**, edit that path to your JDK, or install JDK 21 at the same macOS path if you use a Mac.
3. **Command line:** ensure `JAVA_HOME` points to a full JDK, or rely on `org.gradle.java.home` in **`gradle.properties`** (macOS path there is an example—adjust or remove if you use Windows/Linux).

Set a valid **Google Maps API key** in `app/src/main/res/values/strings.xml` (`google_maps_key`).

---

## Demo tips (sensors & offline)

1. **Shake:** On Search, pick origin/destination, then **shake** the device/emulator to run search again.  
2. **Offline cache:** While online, run a search once; enable **airplane mode**; search the **same** route again — cached results appear with a **cache hint** banner.  
3. **GPS:** Open **Live tracking**, grant location — your position appears as **“Your location (GPS)”** and distance updates vs the bus.  
4. **Proximity:** On **Ticket details**, cover the top of the phone (proximity sensor) — QR is **masked** until you move the phone away.
