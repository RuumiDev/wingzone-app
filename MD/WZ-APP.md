# Project: "WingZone" Group Ordering Ecosystem (Multimedia Project)
**Core Concept:** A synchronous F&B group ordering system with split-bill capabilities, real-time kitchen management, and physical labeling automation.

## 📱 1. Customer App (Android)
**Goal:** High-performance, native "Lobby" experience for group ordering.

* **Language:** Kotlin (Strictly typed, modern).
* **UI Framework:** Jetpack Compose (Declarative UI, ideal for reactive states).
* **Concurrency:** Coroutines & Kotlin Flow (For real-time Firestore updates).
* **Multimedia:** Coil (Image loading), Lottie (Animations).
* **Navigation:** Navigation Compose.

## 💻 2. Admin & Kitchen Dashboard (Web)
**Goal:** Data visualization, inventory control, and kitchen dispatching.

* **Framework:** React.js + TypeScript (Vite).
* **Styling:** Tailwind CSS.
* **State Management:** TanStack Query (React Query).
* **Charts/Analytics:** Tremor / Recharts.
* **Icons:** Lucide React.

## ☁️ 3. Backend & Infrastructure (Serverless)
**Goal:** Real-time synchronization and "Source of Truth".

* **Core DB:** Firebase Cloud Firestore (NoSQL).
* **Authentication:** Firebase Auth.
* **Media Storage:** Firebase Storage (Menu images, Proof of Delivery photos).
* **Server Logic:** Cloud Functions for Firebase (Node.js/TS) for split-bill calculation and daily closing.

## 🖨️ 4. Hardware & Physical Output (Multimedia Requirement)
**Goal:** Bridging the digital-to-physical gap for order identification.

* **Device:** Direct Thermal Label Printer (e.g., Xprinter XP-365B or XP-370B).
* **Media:** Thermal Sticker Labels (Recommended size: 40mm x 30mm).
* **Integration:** `react-to-print` library (Interfacing React with Browser Print API).
* **Output:** "Smart Stickers" featuring Seat ID, Name, and unique QR Codes.

## 🧩 Key Architecture Features
1.  **The "Lobby" System:** Atomic transaction logic where 1 Master Order contains multiple Sub-Orders.
2.  **Split Bill Engine:** State-machine logic that locks the order until `paid_users == total_users`.
3.  **Kitchen Batching:** Algorithm to aggregate multiple individual orders into a single cooking list.


# COPILOT INSTRUCTIONS (STRICT)

**Project Context:**
We are building a native Android F&B application (Clone of ZUS Coffee).

**Tech Stack Constraints:**
1.  **UI Framework:** Jetpack Compose ONLY. Do NOT suggest XML layouts or Fragments.
2.  **Architecture:** Single Activity (MainActivity.kt). Use Composables for screens.
3.  **Navigation:** Use `androidx.navigation.compose` (NavHost). Do NOT use FragmentManager.
4.  **Styling:** Use Material Design 3 (`androidx.compose.material3`). Use `Scaffold` for screen structures.
5.  **Image Loading:** Use `io.coil-kt:coil-compose` (AsyncImage).
6.  **Language:** Kotlin.

**Visual Style:**
* Structure: BottomNavigationBar with Scaffold.
* Cards: Rounded corners (12-16.dp).

**Behavior:**
* When asked to generate a UI, provide the full Composable function code.
* Assume `@OptIn(ExperimentalMaterial3Api::class)` is allowed.

