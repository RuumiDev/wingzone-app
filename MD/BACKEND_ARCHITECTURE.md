# WingZone Backend & Admin Architecture

## 🏗️ System Architecture Overview

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│  Android App    │────▶│  Firebase Cloud  │◀────│  Admin Web App  │
│  (Customers)    │     │    Firestore     │     │  (Management)   │
└─────────────────┘     └──────────────────┘     └─────────────────┘
                              │      │
                              │      └──────────────┐
                              ▼                     ▼
                    ┌──────────────────┐  ┌─────────────────┐
                    │ Cloud Functions  │  │ Firebase Storage│
                    │  (Business Logic)│  │ (Images/Docs)   │
                    └──────────────────┘  └─────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │ Thermal Printer  │
                    │ (Label/Receipt)  │
                    └──────────────────┘
```

## 📊 Firestore Database Schema

### Collections Structure

#### 1. `menu_items`
```javascript
{
  id: string (auto-generated),
  name: string,
  description: string,
  category: "entrees" | "alacarte" | "beverages" | "addons",
  price: number,
  imageUrl: string,
  isAvailable: boolean,
  requiresCustomization: boolean,
  options: {
    flavors: string[],
    dippingSauces: string[],
    drinks: string[]
  },
  stockCount: number (optional),
  createdAt: timestamp,
  updatedAt: timestamp
}
```

#### 2. `group_orders`
```javascript
{
  id: string,
  code: string (e.g., "WZABCD"),
  hostId: string,
  status: "open" | "full" | "ordering" | "confirmed" | "preparing" | "ready" | "completed" | "cancelled",
  members: [
    {
      userId: string,
      name: string,
      email: string,
      isHost: boolean,
      cartItems: [
        {
          menuItemId: string,
          menuItemName: string,
          quantity: number,
          customization: {
            flavor: string,
            dippingSauce: string,
            drink: string
          },
          price: number,
          subtotal: number
        }
      ],
      memberTotal: number,
      paymentStatus: "pending" | "paid" | "refunded"
    }
  ],
  deliveryAddress: string,
  specialInstructions: string,
  totalAmount: number,
  totalItems: number,
  tax: number,
  createdAt: timestamp,
  expiresAt: timestamp,
  confirmedAt: timestamp (when all orders locked),
  completedAt: timestamp
}
```

#### 3. `individual_orders`
```javascript
{
  id: string,
  userId: string,
  userName: string,
  userEmail: string,
  orderNumber: string (e.g., "WZ20241204-001"),
  items: [/* same as cartItems above */],
  subtotal: number,
  tax: number,
  total: number,
  status: "pending" | "confirmed" | "preparing" | "ready" | "completed" | "cancelled",
  paymentStatus: "pending" | "paid" | "refunded",
  orderType: "individual" | "group",
  groupOrderId: string (if part of group),
  seatNumber: string (for group orders),
  deliveryAddress: string,
  specialInstructions: string,
  createdAt: timestamp,
  updatedAt: timestamp,
  preparedAt: timestamp,
  completedAt: timestamp
}
```

#### 4. `users`
```javascript
{
  id: string (Firebase Auth UID),
  email: string,
  name: string,
  phoneNumber: string,
  role: "customer" | "admin" | "kitchen_staff",
  wzBalance: number,
  wzPoints: number,
  profileImageUrl: string,
  addresses: [
    {
      id: string,
      label: string,
      fullAddress: string,
      isDefault: boolean
    }
  ],
  orderHistory: string[], // order IDs
  createdAt: timestamp,
  lastLoginAt: timestamp
}
```

#### 5. `receipts`
```javascript
{
  id: string,
  orderId: string,
  orderNumber: string,
  type: "order" | "kitchen" | "label",
  htmlContent: string,
  pdfUrl: string (Firebase Storage),
  printedAt: timestamp,
  printedBy: string (userId),
  printerStatus: "pending" | "printed" | "failed"
}
```

#### 6. `kitchen_queue`
```javascript
{
  id: string,
  orderId: string,
  orderNumber: string,
  items: [
    {
      menuItemName: string,
      quantity: number,
      customization: object,
      seatNumber: string (for group orders),
      customerName: string
    }
  ],
  priority: number (1-5, 5 being highest),
  status: "queued" | "preparing" | "ready" | "served",
  assignedTo: string (kitchen staff userId),
  queuedAt: timestamp,
  startedAt: timestamp,
  completedAt: timestamp,
  estimatedTime: number (minutes)
}
```

#### 7. `inventory`
```javascript
{
  id: string,
  menuItemId: string,
  currentStock: number,
  minStock: number,
  maxStock: number,
  unit: string,
  lastRestocked: timestamp,
  restockedBy: string (userId),
  lowStockAlert: boolean
}
```

#### 8. `analytics`
```javascript
{
  id: string (date-based, e.g., "2024-12-04"),
  date: timestamp,
  totalOrders: number,
  totalRevenue: number,
  totalItems: number,
  groupOrderCount: number,
  individualOrderCount: number,
  averageOrderValue: number,
  topSellingItems: [
    {
      menuItemId: string,
      menuItemName: string,
      quantitySold: number,
      revenue: number
    }
  ],
  peakHours: object, // hour: orderCount
  cancelledOrders: number,
  refundAmount: number
}
```

## 🔐 Firebase Security Rules

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Helper functions
    function isSignedIn() {
      return request.auth != null;
    }
    
    function isAdmin() {
      return isSignedIn() && 
             get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin';
    }
    
    function isKitchenStaff() {
      return isSignedIn() && 
             (get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'kitchen_staff' ||
              isAdmin());
    }
    
    // Menu Items - Public read, Admin write
    match /menu_items/{itemId} {
      allow read: if true; // Everyone can view menu
      allow create, update, delete: if isAdmin();
    }
    
    // Group Orders - Authenticated users
    match /group_orders/{orderId} {
      allow read: if isSignedIn();
      allow create: if isSignedIn();
      allow update: if isSignedIn() && 
                      (resource.data.hostId == request.auth.uid || 
                       request.auth.uid in resource.data.members.userId);
      allow delete: if isAdmin() || resource.data.hostId == request.auth.uid;
    }
    
    // Individual Orders
    match /individual_orders/{orderId} {
      allow read: if isSignedIn() && 
                    (resource.data.userId == request.auth.uid || isAdmin() || isKitchenStaff());
      allow create: if isSignedIn();
      allow update: if isAdmin() || isKitchenStaff();
      allow delete: if isAdmin();
    }
    
    // Users
    match /users/{userId} {
      allow read: if isSignedIn() && (request.auth.uid == userId || isAdmin());
      allow create: if isSignedIn() && request.auth.uid == userId;
      allow update: if isSignedIn() && request.auth.uid == userId;
      allow delete: if isAdmin();
    }
    
    // Kitchen Queue - Kitchen staff only
    match /kitchen_queue/{queueId} {
      allow read, write: if isKitchenStaff();
    }
    
    // Inventory - Admin only
    match /inventory/{inventoryId} {
      allow read: if isSignedIn();
      allow write: if isAdmin();
    }
    
    // Receipts - Admin and Kitchen staff
    match /receipts/{receiptId} {
      allow read, write: if isAdmin() || isKitchenStaff();
    }
    
    // Analytics - Admin only
    match /analytics/{analyticsId} {
      allow read, write: if isAdmin();
    }
  }
}
```

## ⚡ Cloud Functions

### Key Functions to Implement

1. **`onGroupOrderConfirm`** - Triggered when group order status changes to "confirmed"
   - Split individual orders
   - Create kitchen queue items
   - Generate receipts
   - Send notifications

2. **`calculateSplitBill`** - Calculate individual member payments
   - Apply tax and fees
   - Handle discounts
   - Update payment statuses

3. **`generateOrderNumber`** - Create unique order numbers
   - Format: `WZ[YYYYMMDD]-[Sequential]`

4. **`updateInventory`** - Deduct stock on order confirmation
   - Check stock availability
   - Trigger low stock alerts

5. **`generateDailyReport`** - Scheduled function (midnight)
   - Aggregate daily analytics
   - Generate sales report
   - Archive old orders

6. **`sendOrderNotifications`** - FCM notifications
   - Order status updates
   - Group order invites
   - Payment reminders

7. **`generateReceipt`** - Create printable receipts
   - HTML to PDF conversion
   - Store in Firebase Storage
   - Return download URL

8. **`generateLabel`** - Create order labels/stickers
   - Include QR code with order ID
   - Seat number and customer name
   - Order items summary

## 🖨️ Receipt/Label Generation

### Receipt Template Structure
```html
<!DOCTYPE html>
<html>
<head>
  <style>
    @media print {
      body { width: 80mm; }
      .receipt { font-family: monospace; }
    }
  </style>
</head>
<body>
  <div class="receipt">
    <h2>WINGZONE</h2>
    <p>Order #: {{orderNumber}}</p>
    <p>Date: {{date}}</p>
    <hr>
    <div class="items">
      {{#each items}}
      <div>
        <span>{{name}} x{{quantity}}</span>
        <span>RM {{total}}</span>
      </div>
      {{/each}}
    </div>
    <hr>
    <p>Subtotal: RM {{subtotal}}</p>
    <p>Tax (6%): RM {{tax}}</p>
    <p><strong>TOTAL: RM {{total}}</strong></p>
  </div>
</body>
</html>
```

### Label Template (40mm x 30mm)
```html
<div class="label" style="width: 40mm; height: 30mm;">
  <div class="seat-number">SEAT {{seatNumber}}</div>
  <div class="customer-name">{{customerName}}</div>
  <div class="qr-code">
    <img src="{{qrCodeUrl}}" />
  </div>
  <div class="order-id">{{orderNumber}}</div>
</div>
```

## 🎨 Admin Dashboard Features

### Pages & Functionality

1. **Dashboard (Home)**
   - Real-time metrics: Today's revenue, orders, customers
   - Live order status board
   - Peak hours chart
   - Recent activities feed

2. **Menu Management**
   - CRUD operations for menu items
   - Category filtering
   - Availability toggle (Available/Unavailable)
   - Bulk actions (delete, update price)
   - Image upload to Firebase Storage
   - Stock level indicators

3. **Orders**
   - View all orders (tabs: All, Active, Completed, Cancelled)
   - Filter by date, status, type (group/individual)
   - Order details modal
   - Status update buttons
   - Print receipt button
   - Cancel/Refund actions

4. **Group Orders**
   - Active lobbies view
   - Member breakdown
   - Individual vs total amounts
   - Force close/cancel lobby
   - Print batch labels

5. **Kitchen Dashboard**
   - Queue view (drag-and-drop priority)
   - Timer for each order
   - Mark as "Preparing" / "Ready" / "Served"
   - Ingredient batching view
   - Print kitchen tickets

6. **Inventory**
   - Stock levels table
   - Low stock alerts (red indicators)
   - Restock form
   - Stock history log
   - Export to CSV

7. **Receipts & Labels**
   - Receipt history
   - Reprint functionality
   - Batch label printing
   - Template customization
   - Printer status monitor

8. **Analytics**
   - Date range selector
   - Revenue chart (line/bar)
   - Top selling items
   - Order type breakdown (pie chart)
   - Customer analytics
   - Export reports (PDF/CSV)

9. **Users Management**
   - Customer list
   - Admin/Staff accounts
   - Role assignment
   - Ban/Suspend users
   - View order history

10. **Settings**
    - Tax rate configuration
    - Delivery fee settings
    - Operating hours
    - Printer configuration
    - Notification settings
    - Firebase config

## 🛠️ Tech Stack for Admin Dashboard

### Frontend
```json
{
  "framework": "React 18 + TypeScript",
  "build": "Vite",
  "styling": "Tailwind CSS",
  "ui": "shadcn/ui components",
  "state": "@tanstack/react-query",
  "forms": "react-hook-form + zod",
  "charts": "recharts",
  "icons": "lucide-react",
  "routing": "react-router-dom",
  "firebase": "firebase@10.x",
  "printing": "react-to-print",
  "qr": "qrcode.react",
  "notifications": "sonner"
}
```

### Project Structure
```
admin-dashboard/
├── src/
│   ├── components/
│   │   ├── ui/          # shadcn components
│   │   ├── layout/      # Sidebar, Header, etc.
│   │   ├── menu/        # Menu management components
│   │   ├── orders/      # Order components
│   │   └── kitchen/     # Kitchen dashboard
│   ├── lib/
│   │   ├── firebase.ts  # Firebase config
│   │   ├── queries.ts   # React Query hooks
│   │   └── utils.ts     # Helpers
│   ├── pages/
│   │   ├── Dashboard.tsx
│   │   ├── Menu.tsx
│   │   ├── Orders.tsx
│   │   ├── Kitchen.tsx
│   │   └── Analytics.tsx
│   ├── types/
│   │   └── index.ts     # TypeScript interfaces
│   ├── App.tsx
│   └── main.tsx
├── package.json
├── tsconfig.json
├── tailwind.config.js
└── vite.config.ts
```

## 🚀 Next Steps

1. Set up Firebase project
2. Initialize Firestore collections
3. Deploy security rules
4. Create Cloud Functions
5. Build Admin Dashboard (React)
6. Integrate printer API
7. Test end-to-end flow
8. Deploy to production

---

**Note**: This architecture supports the full lifecycle from customer ordering to kitchen preparation to physical label printing, with complete admin control over all aspects of the system.
