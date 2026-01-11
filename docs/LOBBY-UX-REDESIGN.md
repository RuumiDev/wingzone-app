# Lobby/Group Order UX Redesign

## Overview
Redesigning the group ordering experience to be more intuitive, informative, and user-friendly with better payment options and easier joining mechanisms.

**Date:** January 11, 2026  
**Design Philosophy:** Following ZUS Coffee style - modern, clean, chip-based selections

---

## 🎨 Design Philosophy

### Visual Style
- **Cards with rounded corners** (16dp)
- **Chip-based selections** instead of dropdowns
- **Navy blue** (`#1E3A8A`) for primary actions
- **Large icons** for visual recognition
- **Bottom sheets** for modals
- **Gray background** (`#F9FAFB`) with white cards

### User Flow Principles
1. Clear expectations upfront (disclaimer)
2. Minimal steps to create/join
3. Multiple join options (code/QR/link)
4. Transparent payment choices
5. Real-time updates

---

## 1. Create Lobby Flow

### 1.1 Initial Disclaimer Screen
**Trigger:** First time creating lobby OR if user unchecked "don't show again"

```
┌─────────────────────────────────────┐
│  🍗 Wing Zone Group Order           │
├─────────────────────────────────────┤
│                                     │
│  ⚠️ Important Notice                │
│                                     │
│  Wing Zone currently does NOT       │
│  offer delivery services.           │
│                                     │
│  All orders must be:                │
│  • Picked up at our location        │
│  • Or dine in at our restaurant     │
│                                     │
│  ┌─────────────────────────────┐   │
│  │ ☐ I understand and agree    │   │
│  │   to these terms            │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │ ☐ Don't show this again     │   │
│  └─────────────────────────────┘   │
│                                     │
│  [ Cancel ]     [ Continue ]        │
│                   (disabled)        │
└─────────────────────────────────────┘
```

**Implementation:**
- Full-screen modal with dim background
- Continue button only enabled when checkbox checked
- Store preference in `SharedPreferences` or `AsyncStorage`
- Key: `lobby_disclaimer_acknowledged`
- Fade-in animation for better UX

---

### 1.2 Create Lobby Screen
**Design:** Full screen with scrollable form on gray background

```
┌─────────────────────────────────────┐
│  ← Create Group Order               │ Top Bar
├─────────────────────────────────────┤
│                                     │ Gray BG
│  ┌─────────────────────────────┐   │
│  │ 🥡 Order Type *             │   │ White Card
│  │                             │   │
│  │ ⚪ Pickup                   │   │ Chip Radio
│  │ ⚪ Dine-In                  │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │ 📍 Location *               │   │ White Card
│  │                             │   │
│  │ ⚪ Wingzone Meru            │   │ Chip Radio
│  │    Lebuh Meru Raya, Ipoh    │   │ Small text
│  │                             │   │
│  │ ⚪ 20 Persiaran Greentown 1, Greentown Business Center, 30450 Ipoh, Perak    │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │ 💰 Payment Method *         │   │ White Card
│  │                             │   │
│  │ ⚪ Host Pays All            │   │ Chip Radio
│  │    You pay for everyone     │   │ Small text
│  │                             │   │
│  │ ⚪ Split Equally            │   │
│  │    Total ÷ members          │   │
│  │                             │   │
│  │ ⚪ Individual Payment       │   │
│  │    Each pays their own      │   │
│  └─────────────────────────────┘   │
│                                     │
│                                     │
│  [     Create Lobby     ]           │ Navy Button
│                                     │
└─────────────────────────────────────┘
```

**Features:**
- Each section in its own white card
- **Order Type & Payment**: Chip-style radio buttons (like ZUS design)
  - Selected chip: Navy blue background, white text
  - Unselected chip: White background, navy border
- **Location**: Dropdown selector (cleaner, shows full address below)
- Icons for visual scanning (🥡 📍 💰)
- Required fields marked with asterisk
- Create button only enabled when all required selected

**Validation:**
- All three selections required
- Show error if trying to create without selections
- Haptic feedback on chip selection

---

### 1.3 Lobby Created Success Screen

```
┌─────────────────────────────────────┐
│  Lobby Created! 🎉                  │
├─────────────────────────────────────┤
│                                     │
│  ┌─────────────────────────────┐   │
│  │  Your lobby code:           │   │
│  │                             │   │
│  │      5A2X9K                 │   │ Large text
│  │                             │   │
│  │  [ 📋 Copy Code ]           │   │ Button
│  └─────────────────────────────┘   │
│                                     │
│  ─── Share with friends ───         │
│                                     │
│  ┌─────────────────────────────┐   │
│  │                             │   │
│  │     [  QR CODE IMAGE  ]     │   │ 200x200
│  │                             │   │
│  │      Scan to join           │   │
│  │                             │   │
│  └─────────────────────────────┘   │
│                                     │
│  [ 🔗 Share Link ] [ 📷 Save QR ]  │
│                                     │
│  ─── Members ───                    │
│                                     │
│  ┌─────────────────────────────┐   │
│  │ 👤 You (Host)               │   │
│  │    Waiting for orders...    │   │
│  └─────────────────────────────┘   │
│                                     │
│  [   Start Ordering   ]             │ Primary Action
│                                     │
└─────────────────────────────────────┘
```

**Share Options When Clicking "Share Link":**
```
┌─────────────────────────────────────┐
│  Share Lobby                        │ Bottom Sheet
├─────────────────────────────────────┤
│                                     │
│  📱 [ WhatsApp    ]                 │
│  💬 [ Telegram    ]                 │
│  📧 [ Email       ]                 │
│  💬 [ SMS         ]                 │
│  📋 [ Copy Link   ]                 │
│  📤 [ More...     ]                 │
│                                     │
└─────────────────────────────────────┘
```

**Share Message Template:**
```
Hey! Join my Wing Zone group order 🍗

Code: 5A2X9K
Link: https://order.wingzone.app/lobby/5A2X9K

📍 Pickup at Wingzone Meru
💰 Payment: Individual
⏱️ Order within 60 minutes
```

---

## 2. Join Lobby Flow

### 2.1 Join Lobby Home Screen

```
┌─────────────────────────────────────┐
│  ← Join Group Order                 │
├─────────────────────────────────────┤
│                                     │
│  ┌─────────────────────────────┐   │
│  │  Enter 6-digit code:        │   │
│  │                             │   │
│  │  ┌───┬───┬───┬───┬───┬───┐ │   │
│  │  │ 5 │ A │ 2 │ X │ 9 │ K │ │   │ Auto-advance
│  │  └───┴───┴───┴───┴───┴───┘ │   │
│  │                             │   │
│  │  [      Join      ]         │   │ Primary
│  └─────────────────────────────┘   │
│                                     │
│  ─────────── or ───────────         │
│                                     │
│  ┌─────────────────────────────┐   │
│  │ 📷  Scan QR Code            │   │ Secondary
│  └─────────────────────────────┘   │
│                                     │
│  💡 Tip: Ask your friend to share   │
│     the lobby link directly!        │
│                                     │
└─────────────────────────────────────┘
```

**Features:**
- 6 separate input boxes (like OTP entry)
- Auto-advance to next box on input
- Auto-submit when 6 characters entered
- Uppercase conversion automatically
- Clear button to reset all boxes
- Large scan button with icon
- Helpful tip about link sharing

---

### 2.2 QR Scanner Screen

```
┌─────────────────────────────────────┐
│  ← Scan QR Code       [🔦 Flash]   │
├─────────────────────────────────────┤
│                                     │
│  ┌─────────────────────────────┐   │
│  │                             │   │
│  │                             │   │
│  │    ╔═══════════════╗        │   │
│  │    ║               ║        │   │ Camera View
│  │    ║  [QR TARGET]  ║        │   │
│  │    ║               ║        │   │
│  │    ╚═══════════════╝        │   │
│  │                             │   │
│  │                             │   │
│  └─────────────────────────────┘   │
│                                     │
│  Position QR code within frame      │
│                                     │
│  [  Enter Code Manually  ]          │ Text Button
│                                     │
└─────────────────────────────────────┘
```

**Features:**
- Full camera view with overlay
- Scanning frame with corner markers
- Flashlight toggle in top bar
- Auto-detect and validate QR code
- Vibration feedback on successful scan
- Fallback to manual entry
- Request camera permission gracefully

---

### 2.3 Deep Link Handling

**URL Formats:**
```
App Deep Link:    wingzone://lobby/join?code=5A2X9K
Web URL:          https://order.wingzone.app/lobby/5A2X9K
```

**Behavior:**
1. **App Installed:**
   - Opens app directly
   - Auto-navigates to join screen
   - Pre-fills code
   - Auto-validates and joins

2. **App Not Installed:**
   - Opens web page
   - Shows lobby info (host, location, payment method)
   - "Download App" button
   - "Enter code manually" option

**Web Fallback Page:**
```html
<!DOCTYPE html>
<html>
<head>
  <title>Join Wing Zone Group Order</title>
</head>
<body>
  <div class="container">
    <h1>🍗 Wing Zone Group Order</h1>
    
    <div class="lobby-info">
      <h2>Lobby Code: 5A2X9K</h2>
      <p>📍 Pickup at Wingzone Meru</p>
      <p>👤 Host: Ahmad</p>
      <p>💰 Payment: Individual</p>
    </div>
    
    <a href="wingzone://lobby/join?code=5A2X9K" 
       class="btn-primary">
      Open in App
    </a>
    
    <a href="#download" class="btn-secondary">
      Download Wing Zone App
    </a>
  </div>
</body>
</html>
```

---

## 3. Active Lobby Experience

### 3.1 Host View

```
┌─────────────────────────────────────┐
│  ← Lobby: 5A2X9K         [ ⚙️ ]    │ Top Bar
├─────────────────────────────────────┤
│                                     │
│  ┌─────────────────────────────┐   │
│  │ 📍 Pickup at Wingzone Meru  │   │ Info Card
│  │ 💰 Individual Payment       │   │
│  │ ⏱️  Expires in 58 minutes    │   │ Countdown
│  └─────────────────────────────┘   │
│                                     │
│  [  📤  Share Lobby  ]              │ Prominent
│                                     │
│  ─── Members (3) ───                │
│                                     │
│  ┌─────────────────────────────┐   │
│  │ 👤 You (Host)    RM 45.00 ✓ │   │ Member Card
│  │    2x Wing Combo            │   │
│  │    1x Garden Salad          │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │ 👤 Ahmad         RM 22.50 ✓ │   │
│  │    1x Wing Combo            │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │ 👤 Sarah         RM 18.00 🕐 │   │ In Progress
│  │    1x Garden Salad          │   │
│  │    Still ordering...        │   │ Animated
│  └─────────────────────────────┘   │
│                                     │
│  ─────────────────────────────      │
│                                     │
│  Total: RM 85.50                    │ Large
│                                     │
│  [ 🔒 Lock Lobby ] [ 💳 Checkout ] │ Actions
│                                     │
└─────────────────────────────────────┘
```

**Host-Specific Features:**
- Settings menu (gear icon):
  - Change payment method
  - Kick member
  - Cancel lobby
- Lock Lobby: Prevents new members
- Real-time order updates with animations
- Member status indicators:
  - ✓ Ready (marked as done)
  - 🕐 Ordering (still browsing)
  - ⚠️ Empty cart (no items yet)

---

### 3.2 Member View

```
┌─────────────────────────────────────┐
│  ← Lobby: 5A2X9K                    │
├─────────────────────────────────────┤
│                                     │
│  ┌─────────────────────────────┐   │
│  │ 📍 Pickup at Wingzone Meru  │   │
│  │ 💰 Individual Payment       │   │
│  │ 👤 Host: Ahmad              │   │
│  └─────────────────────────────┘   │
│                                     │
│  ─── Your Order ───                 │
│                                     │
│  ┌─────────────────────────────┐   │
│  │ 2x Wing Combo       RM 45.00│   │ Your Items
│  │ 1x Garden Salad     RM 18.00│   │
│  │                             │   │
│  │ [   Add More Items   ]      │   │ To Menu
│  └─────────────────────────────┘   │
│                                     │
│  ─── Other Members (2) ───          │
│                                     │
│  👤 Sarah         RM 22.50 ✓        │ Status List
│  👤 Ali           RM 15.00 🕐       │
│                                     │
│  ─────────────────────────────      │
│                                     │
│  Your Total: RM 63.00               │
│                                     │
│  [ ✓ Mark Ready ] [ Leave Lobby ]   │
│                                     │
└─────────────────────────────────────┘
```

**Member-Specific Features:**
- Can only edit own orders
- "Mark Ready" button (confirms done ordering)
- "Leave Lobby" option
- See other members' totals (privacy: don't show exact items)
- Can't checkout (only host can)

---

## 4. Payment Method Flows

### 4.1 Host Pays All

**Host View After Checkout:**
```
┌─────────────────────────────────────┐
│  Payment Confirmation               │
├─────────────────────────────────────┤
│                                     │
│  You're paying for everyone!        │
│                                     │
│  ┌─────────────────────────────┐   │
│  │ Total Amount:               │   │
│  │                             │   │
│  │      RM 85.50               │   │ Large
│  │                             │   │
│  │ For 3 members:              │   │
│  │ • You: RM 45.00             │   │
│  │ • Ahmad: RM 22.50           │   │
│  │ • Sarah: RM 18.00           │   │
│  └─────────────────────────────┘   │
│                                     │
│  [ Select Payment Method ]          │
│                                     │
└─────────────────────────────────────┘
```

**Members See:**
```
┌─────────────────────────────────────┐
│  Order Confirmed! ✅                │
├─────────────────────────────────────┤
│                                     │
│  Host is paying for everyone        │
│                                     │
│  Your order: RM 63.00               │
│  Paid by Ahmad (Host)               │
│                                     │
│  📍 Pickup at Wingzone Meru         │
│  ⏱️  Ready in 20-30 minutes         │
│                                     │
│  Order #: WZ-5A2X9K                 │
│                                     │
│  [  View Receipt  ]                 │
│                                     │
└─────────────────────────────────────┘
```

---

### 4.2 Split Equally

**Checkout Flow:**
```
┌─────────────────────────────────────┐
│  Split Payment                      │
├─────────────────────────────────────┤
│                                     │
│  Total: RM 85.50                    │
│  Members: 3                         │
│                                     │
│  ┌─────────────────────────────┐   │
│  │ Each pays:                  │   │
│  │                             │   │
│  │      RM 28.50               │   │ Large
│  │                             │   │
│  └─────────────────────────────┘   │
│                                     │
│  Payment Status:                    │
│  ✓ You             RM 28.50 PAID    │
│  ⏳ Ahmad          RM 28.50 PENDING │
│  ⏳ Sarah          RM 28.50 PENDING │
│                                     │
│  Waiting for others to pay...       │
│                                     │
└─────────────────────────────────────┘
```

**After All Paid:**
```
┌─────────────────────────────────────┐
│  All Payments Complete! 🎉          │
├─────────────────────────────────────┤
│                                     │
│  ✓ You             RM 28.50 PAID    │
│  ✓ Ahmad           RM 28.50 PAID    │
│  ✓ Sarah           RM 28.50 PAID    │
│                                     │
│  Total Collected: RM 85.50          │
│                                     │
│  📍 Pickup at Wingzone Meru         │
│  ⏱️  Ready in 20-30 minutes         │
│                                     │
│  [  View Receipt  ]                 │
│                                     │
└─────────────────────────────────────┘
```

---

### 4.3 Individual Payment (Current)

Each member pays their own amount separately. Existing flow remains the same.

---

## 5. Data Structures

### Lobby Document (Firestore)
```typescript
interface Lobby {
  id: string;
  code: string; // 6-character code
  qrCodeUrl?: string; // Generated QR image URL
  
  // Host info
  hostUserId: string;
  hostUserName: string;
  
  // New fields
  orderType: 'pickup' | 'dine-in';
  location: {
    id: string;
    name: string; // "Wingzone Meru"
    address: string;
  };
  paymentMethod: 'host-pays-all' | 'split-equally' | 'individual';
  
  // Members
  members: Member[];
  maxMembers?: number; // Default 10
  
  // Payment tracking
  payments?: {
    [userId: string]: {
      amount: number;
      status: 'pending' | 'paid' | 'failed';
      transactionId?: string;
      paidAt?: Timestamp;
    }
  };
  
  // Status
  status: 'active' | 'locked' | 'paid' | 'completed' | 'cancelled';
  
  // Timestamps
  createdAt: Timestamp;
  expiresAt: Timestamp; // 1 hour from creation
  lockedAt?: Timestamp;
  completedAt?: Timestamp;
}

interface Member {
  userId: string;
  userName: string;
  joinedAt: Timestamp;
  cartItems: CartItem[];
  total: number;
  status: 'ordering' | 'ready'; // Member marks ready
  paidAmount?: number; // For split payment
  paymentStatus?: 'pending' | 'paid';
}
```

### Restaurant Location (Firestore Collection)
```typescript
interface RestaurantLocation {
  id: string;
  name: string; // "Wingzone Meru"
  displayName: string; // Shown in UI
  address: string;
  addressLine1: string; // For receipts
  addressLine2: string; // For receipts
  city: string;
  coordinates: {
    lat: number;
    lng: number;
  };
  hours: {
    open: string; // "10:00"
    close: string; // "22:00"
  };
  active: boolean;
  createdAt: Timestamp;
}

// Firestore path: /restaurantLocations/{id}

// Example Documents:
/*
Wingzone Meru:
{
  id: "wingzone-meru",
  name: "Wingzone Meru",
  displayName: "Wingzone Meru",
  address: "Lebuh Meru Raya, Bandar Meru Raya, Ipoh",
  addressLine1: "Lebuh Meru Raya,",
  addressLine2: "Bandar Meru Raya, Ipoh",
  city: "Ipoh",
  coordinates: { lat: 4.5975, lng: 101.0901 },
  hours: { open: "10:00", close: "22:00" },
  active: true
}

Wingzone GreenTown:
{
  id: "wingzone-greentown",
  name: "Wingzone GreenTown",
  displayName: "Wingzone GreenTown",
  address: "No. 2, Lorong Greentown 8, Greentown Business Centre, 30450 Ipoh, Perak",
  addressLine1: "No. 2, Lorong Greentown 8,",
  addressLine2: "Greentown Business Centre, 30450 Ipoh, Perak",
  city: "Ipoh",
  coordinates: { lat: 4.5975, lng: 101.0901 },
  hours: { open: "10:00", close: "22:00" },
  active: true
}
*/

// Example Documents:
/*
{
  id: "wingzone-meru",
  name: "Wingzone Meru",
  displayName: "Wingzone Meru",
  address: "Lebuh Meru Raya, Bandar Meru Raya, Ipoh",
  addressLine1: "Lebuh Meru Raya,",
  addressLine2: "Bandar Meru Raya, Ipoh",
  city: "Ipoh",
  coordinates: { lat: 4.5975, lng: 101.0901 },
  hours: { open: "10:00", close: "22:00" },
  active: true
}

{
  id: "wingzone-greentown",
  name: "Wingzone GreenTown",
  displayName: "Wingzone GreenTown",
  address: "No. 2, Lorong Greentown 8, Greentown Business Centre, 30450 Ipoh, Perak",
  addressLine1: "No. 2, Lorong Greentown 8,",
  addressLine2: "Greentown Business Centre, 30450 Ipoh, Perak",
  city: "Ipoh",
  coordinates: { lat: 4.5975, lng: 101.0901 },
  hours: { open: "10:00", close: "22:00" },
  active: true
}
*/
```

---

## 6. Implementation Checklist

### Backend (Firestore & Cloud Functions)

#### Firestore Setup
- [ ] Create `restaurantLocations` collection
  - [ ] Add Wingzone Meru document
    - Address: "Lebuh Meru Raya, Bandar Meru Raya, Ipoh"
  - [ ] Add Wingzone GreenTown document
    - Address: "No. 2, Lorong Greentown 8, Greentown Business Centre, 30450 Ipoh, Perak"
- [ ] Update `lobbies` collection schema
  - [ ] Add `orderType` field
  - [ ] Add `location` object
  - [ ] Add `paymentMethod` field
  - [ ] Add `payments` map
  - [ ] Add `status` with new values

#### Cloud Functions
- [ ] `generateLobbyCode` - Create unique 6-char code
- [ ] `generateLobbyQR` - Create QR code image
- [ ] `validateLobbyCode` - Check if code exists/valid
- [ ] `handleSplitPayment` - Track split payment status
- [ ] `expireLobby` - Auto-expire after 1 hour
- [ ] `notifyMemberJoin` - Push notification to host
- [ ] `notifyMemberReady` - Notify host when all ready

---

### Frontend (Android Kotlin/Compose)

#### Screens to Create
- [ ] `DisclaimerDialog.kt` - Modal with checkboxes
- [ ] `CreateLobbyScreen.kt` - Form with chip selections
- [ ] `LobbySuccessScreen.kt` - Code/QR display
- [ ] `JoinLobbyScreen.kt` - Code entry + scan button
- [ ] `QRScannerScreen.kt` - Camera scanner
- [ ] `ActiveLobbyScreen.kt` - Live lobby view (host/member)
- [ ] `SplitPaymentScreen.kt` - Payment tracking

#### Components to Create
- [ ] `ChipRadioGroup` - Radio button chips (for payment)
- [ ] `LocationDropdown` - Location selector dropdown with address preview
- [ ] `PaymentMethodChip` - Payment option chip
- [ ] `CodeInputBox` - Single character input
- [ ] `MemberCard` - Member info card
- [ ] `CountdownTimer` - Expiry countdown
- [ ] `ShareSheet` - Share options bottom sheet

#### State Management
- [ ] `LobbyViewModel` 
  - Create lobby
  - Join lobby
  - Leave lobby
  - Update member status
  - Track payments
- [ ] `LocationViewModel`
  - Fetch locations
  - Cache locations

#### Preferences
- [ ] Save disclaimer acknowledgment
- [ ] Last selected location
- [ ] Last selected payment method

---

### Deep Linking

#### Android Configuration
- [ ] Update `AndroidManifest.xml`
  ```xml
  <intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="wingzone" 
          android:host="lobby" 
          android:pathPrefix="/join" />
  </intent-filter>
  ```
- [ ] Add App Link verification
  ```xml
  <intent-filter android:autoVerify="true">
    <data android:scheme="https"
          android:host="order.wingzone.app" />
  </intent-filter>
  ```
- [ ] Handle deep link in MainActivity

#### Web Fallback
- [ ] Create `lobby/[code].html` page
- [ ] Add app download buttons
- [ ] Add manual code entry form
- [ ] Add lobby info preview

---

### QR Code

#### Generation
- [ ] Install QR library (`com.google.zxing:core`)
- [ ] Generate QR on lobby creation
- [ ] Upload to Firebase Storage
- [ ] Store URL in lobby document

#### Scanning
- [ ] Install scanner library (`com.google.mlkit:barcode-scanning`)
- [ ] Request camera permission
- [ ] Implement scanner UI
- [ ] Handle QR detection
- [ ] Validate scanned code
- [ ] Navigate to lobby

---

### Payment Integration

#### Host Pays All
- [ ] Calculate total from all members
- [ ] Show confirmation screen
- [ ] Process single payment
- [ ] Update all members as "paid"
- [ ] Send receipts to all members

#### Split Equally
- [ ] Calculate per-person amount
- [ ] Create payment intent for each member
- [ ] Track payment status per member
- [ ] Wait for all payments
- [ ] Handle partial payments (timeout/refund)
- [ ] Notify when all paid

#### Individual (Existing)
- [ ] Keep current flow
- [ ] Each pays their own total

---

## 7. User Flows

### Create Lobby
```
App Open
  ↓
[First time?] → YES → Show Disclaimer → Accept
  ↓ NO
  ↓
Navigate to Create Lobby
  ↓
Select Order Type (Pickup/Dine-In)
  ↓
Select Location (Meru/GreenTown)
  ↓
Select Payment Method
  ↓
Tap Create Lobby
  ↓
Generate Code + QR + Link
  ↓
Show Success Screen
  ↓
Tap Share → Share via WhatsApp/etc
  ↓
Tap Start Ordering → Go to Menu
```

### Join Lobby
```
App Open
  ↓
Navigate to Join Lobby
  ↓
Choose Method:
  ├→ Manual Entry → Type 6-digit code → Validate → Join
  ├→ QR Scan → Open camera → Scan QR → Join
  └→ Deep Link → App opens → Auto-join
        ↓
    [Valid Lobby?]
      ↓ YES
    Navigate to Lobby Screen
      ↓
    Browse Menu → Add Items → Mark Ready
      ↓
    Wait for Host Checkout
```

---

## 8. Error Handling

### Create Lobby Errors
- **No disclaimer acceptance**: "Please accept the terms to continue"
- **Missing selections**: "Please select all required options"
- **Location unavailable**: "This location is currently closed"
- **Network error**: "Failed to create lobby. Please try again"

### Join Lobby Errors
- **Invalid code**: "Lobby not found. Please check the code and try again."
- **Expired lobby**: "This lobby has expired. Ask the host to create a new one."
- **Locked lobby**: "This lobby is locked. No new members can join."
- **Full lobby**: "This lobby is full (max 10 members)."
- **Already joined**: "You're already in this lobby!"
- **Camera permission**: "Camera access needed to scan QR codes. Enable in settings?"

### Payment Errors
- **Payment failed**: "Payment failed. Please try again or use another method."
- **Timeout (split)**: "Payment timeout. Your payment will be refunded within 3-5 days."
- **Network error**: "Connection lost. Please check your internet."

---

## 9. Testing Scenarios

### Create Lobby
- [ ] First-time user sees disclaimer
- [ ] "Don't show again" persists
- [ ] Can't proceed without acceptance
- [ ] All order types work
- [ ] All locations work
- [ ] All payment methods work
- [ ] QR generates successfully
- [ ] Share opens system sheet
- [ ] Code is unique
- [ ] Lobby expires after 1 hour

### Join Lobby
- [ ] Manual code works
- [ ] QR scan detects codes
- [ ] Invalid code shows error
- [ ] Expired lobby blocked
- [ ] Locked lobby blocked
- [ ] Full lobby blocked
- [ ] Deep link opens app
- [ ] Web fallback works

### Active Lobby
- [ ] Real-time member updates
- [ ] Member join notification
- [ ] Member ready indicator
- [ ] Host can lock lobby
- [ ] Host can kick members
- [ ] Members can leave
- [ ] Countdown timer accurate

### Payments
- [ ] Host pays all: Single charge
- [ ] Split: Correct per-person amount
- [ ] Individual: Separate charges
- [ ] Payment status updates
- [ ] Receipts generated
- [ ] Failed payment handling

---

## 10. Analytics to Track

### Lobby Creation
- `lobby_created` - Track creation count
- `payment_method_selected` - Which method chosen
- `location_selected` - Which location popular
- `order_type_selected` - Pickup vs Dine-in ratio

### Lobby Joining
- `lobby_joined` - Join count
- `join_method_used` - Code vs QR vs Link
- `join_failed` - Track error reasons

### Lobby Activity
- `member_added` - Member join events
- `member_left` - Member leave events
- `member_marked_ready` - Ready indicator usage
- `lobby_locked` - Lock action
- `lobby_expired` - Natural expiry

### Payment
- `checkout_initiated` - Checkout start
- `payment_completed` - Success
- `payment_failed` - Failure reasons
- `split_payment_timeout` - Timeout rate

---

## 11. Future Enhancements

### Phase 2
- [ ] Save lobby templates
- [ ] Recurring lobbies
- [ ] Lobby chat
- [ ] Invite from contacts
- [ ] Push notifications for lobby events
- [ ] Table reservation (dine-in)

### Phase 3
- [ ] Loyalty rewards for hosts
- [ ] Group discounts
- [ ] Pre-order scheduling
- [ ] Multiple pickup locations (split order)
- [ ] Order tracking map

---

## 12. Design Assets Needed

### Icons
- 🥡 Pickup icon
- 🍽️ Dine-in icon
- 📍 Location pin
- 💰 Payment/money icon
- 👤 User/member icon
- 📷 Camera/scan icon
- 🔗 Link/share icon
- ⏱️ Timer/clock icon
- ✓ Checkmark
- 🕐 In-progress indicator

### Images
- Restaurant location photos
- Payment method illustrations
- Empty state illustrations
- Success state animations

---

This comprehensive redesign transforms the lobby experience into a modern, intuitive flow that matches the ZUS Coffee design philosophy while adding powerful features like QR joining, multiple payment methods, and better member management!

**Ready for implementation!** 🚀
