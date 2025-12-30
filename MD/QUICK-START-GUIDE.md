# Quick Start Guide - Admin Dashboard

## 🚀 How to Import Menu (Step-by-Step)

### Current State:
```
Your screenshot shows: "Combo Meals (0)"
This means: Database is EMPTY ❌
```

### Solution:

#### 1. Start the Dashboard
```bash
cd admin-dashboard
npm run dev
```

#### 2. Navigate in Browser
```
URL: http://localhost:5173
```

#### 3. Look at Sidebar - Click "Import Menu"
```
Dashboard    ← Home
├─ Menu Management
├─ Availability
├─ Import Menu      ← CLICK THIS! 📤
├─ Orders
└─ Users
```

#### 4. You'll See This Page:
```
┌─────────────────────────────────────────────┐
│ Seed Menu Database                          │
│                                             │
│ Import Menu from WingZoneMenu.md            │
│                                             │
│ This will populate the database with:       │
│ • 12 Combo Meals                           │
│ • 10 Wings                                 │
│ • 5 Tenders                                │
│ • 7 Burgers & Sandwiches                   │
│ • 6 Local Favorites                        │
│ • 6 Salads                                 │
│ • 16 Sides                                 │
│ • 5 Beverages                              │
│                                             │
│ Total: 67 menu items                       │
│                                             │
│ ⚠️ Warning: This will delete existing items!│
│                                             │
│ [📤 Import Menu Items] ← CLICK THIS BUTTON  │
└─────────────────────────────────────────────┘
```

#### 5. Confirm the Action
```
Browser Alert: "This will delete all existing 
menu items and add new ones. Continue?"

Click: [OK] ✅
```

#### 6. Success!
```
✅ Successfully added 67 menu items from WingZoneMenu.md!
```

#### 7. Go Back to Menu Management
```
Now you'll see:
┌──────────────────────────────────────────────┐
│ Menu Management                               │
│                                              │
│ [All (67)] [Combo Meals (12)] [Wings (10)]  │
│ [Tenders (5)] [Burgers & Sandwiches (7)]    │
│ [Local Favorites (6)] [Salads (6)]          │
│ [Sides (16)] [Beverages (5)]                │
│                                              │
│ ┌────────────────────────────────────────┐  │
│ │ Entree 1          RM 25.90     [Edit]  │  │
│ │ 6 pcs Boneless Wings + Fries...        │  │
│ ├────────────────────────────────────────┤  │
│ │ Entree 2          RM 29.90     [Edit]  │  │
│ │ 7 pcs Original Wings + Fries...        │  │
│ └────────────────────────────────────────┘  │
└──────────────────────────────────────────────┘
```

---

## 🎯 Testing New Features

### Bell Icon (Notifications)
```
Click 🔔 in top-right → Opens Notifications Page

Shows:
├─ New Order Received (5 minutes ago)
├─ Order Completed (15 minutes ago)
└─ Menu Updated (1 hour ago)

Features:
• Mark individual as read
• Mark all as read
• Unread count badge
```

### Settings Icon
```
Click ⚙️ in top-right → Opens Settings Page

Configure:
├─ Restaurant Information
│  ├─ Name: WingZone
│  ├─ Email: admin@wingzone.com
│  ├─ Phone: +60 12-345 6789
│  └─ Address: Kuala Lumpur
├─ Pricing Settings
│  ├─ Currency: RM
│  ├─ Tax Rate: 6%
│  └─ Service Charge: 10%
└─ Notification Preferences
   ├─ ☑ Order notifications
   ├─ ☐ Email notifications
   └─ ☑ Sound alerts

[Save Settings] button
```

---

## 📱 Android App

The app will automatically sync with Firebase once you import the menu!

### Menu will show:
```
Home Screen
└─ Menu Tab
   ├─ ⭐ Combo Meals (12)
   ├─ 🛒 Wings (10)
   ├─ 🛒 Tenders (5)
   ├─ 🛒 Burgers & Sandwiches (7)
   ├─ 🏠 Local Favorites (6)
   ├─ 🛒 Salads (6)
   ├─ 🛒 Sides (16)
   └─ 🛒 Beverages (5)
```

### Customization will work:
```
User selects: Entree 1
↓
Customization Screen:
├─ Choose Flavor: 
│  [Buffalo Wing] [Soul of Seoul] [Garlic Parm]
│  [Mambo Sauce] ... (all 15 flavors)
├─ Choose Beverage:
│  [Coca-Cola] [Coke Zero] [Sprite]
│  [Iced Lemon Tea] [Orange Juice]
├─ Choose Dipping Sauce:
│  [Ranch] [Bleu Cheese]
└─ Exchange Fries? (Optional)
   [Premium Wedge Fries] FREE
   [Kettle Chips] FREE
   [Flavor Rub Fries] +RM5
   [Sweet Potato Fries] +RM5
   [Mozzarella Stix] +RM11
   [Caesar Salad] +RM14

[Add to Cart]
```

---

## 🐛 Troubleshooting

### "Nothing happens when I click Import"
- Check browser console for errors
- Verify Firebase connection in `.env`
- Make sure you're logged in

### "Still showing (0) items"
- Refresh the page (F5)
- Check Network tab - look for Firestore requests
- Verify Firebase Firestore rules allow read/write

### "Flavors don't match Wing Zone menu"
- ✅ This is FIXED now!
- Old placeholder flavors replaced with authentic 15 flavors
- Matches WingZoneMenu.md exactly

---

## ✅ Verification Checklist

After import, verify:
- [ ] Combo Meals shows (12)
- [ ] Wings shows (10)
- [ ] Tenders shows (5)
- [ ] All categories have items
- [ ] Bell icon opens Notifications
- [ ] Settings icon opens Settings
- [ ] Edit menu item shows correct 15 flavors
- [ ] Android app syncs and displays menu

---

## 🎉 You're Done!

Your admin dashboard now has:
1. ✅ Complete 67-item menu from WingZoneMenu.md
2. ✅ Correct 15 Wing Zone flavors
3. ✅ Functional notification system
4. ✅ Working settings page
5. ✅ Real-time Firebase sync

**The menu was always there in the code - you just needed to import it!**
