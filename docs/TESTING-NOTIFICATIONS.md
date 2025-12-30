# Testing the Notification System

## Adding Test Notifications to Firebase

Since the notification system is now real (not mockup), you need to add notifications through Firebase to see them appear.

### Method 1: Using Firebase Console (Manual)

1. Open Firebase Console: https://console.firebase.google.com
2. Select your project
3. Go to Firestore Database
4. Click "Start collection"
5. Collection ID: `notifications`
6. Add document with these fields:

```javascript
{
  type: "order",              // string: "order", "system", or "info"
  title: "New Order Received", // string
  message: "Order #1234 - Table 5", // string
  createdAt: Timestamp.now(), // timestamp
  read: false                 // boolean
}
```

### Method 2: Using Browser Console (Quick)

1. Open admin dashboard in browser
2. Open DevTools Console (F12)
3. Paste this code:

```javascript
// Import Firebase
import { collection, addDoc, Timestamp } from 'firebase/firestore';
import { db } from './src/lib/firebase';

// Add order notification
await addDoc(collection(db, 'notifications'), {
  type: 'order',
  title: 'New Order Received',
  message: 'Order #1234 - Table 5',
  createdAt: Timestamp.now(),
  read: false
});

// Add system notification
await addDoc(collection(db, 'notifications'), {
  type: 'system',
  title: 'Menu Updated',
  message: 'Successfully imported 67 menu items',
  createdAt: Timestamp.now(),
  read: false
});

// Add multiple test notifications
for (let i = 1; i <= 5; i++) {
  await addDoc(collection(db, 'notifications'), {
    type: i % 2 === 0 ? 'order' : 'system',
    title: i % 2 === 0 ? `Order #${1000 + i}` : `System Event ${i}`,
    message: i % 2 === 0 ? `Table ${i} placed an order` : `System update completed`,
    createdAt: Timestamp.now(),
    read: Math.random() > 0.5
  });
}
```

### Method 3: Using the Notification Service (Recommended)

Add this code to your admin dashboard pages:

```typescript
import { notificationService } from '../services/notifications';

// In your component or service

// Order notification
await notificationService.addOrderNotification('1234', '5');

// System notification
await notificationService.addSystemNotification(
  'Menu Updated',
  'Successfully imported menu items'
);

// Custom notification
await notificationService.addNotification({
  type: 'info',
  title: 'Information',
  message: 'This is a test notification'
});
```

### Method 4: Automatic Notifications (Integration)

#### When Menu is Imported
Edit `SeedMenuPage.tsx`:

```typescript
const handleSeed = async () => {
  setLoading(true);
  try {
    const result = await seedMenuItems();
    setSuccess(`Successfully added ${result.count} menu items!`);
    
    // Add notification
    await notificationService.addSystemNotification(
      'Menu Updated',
      `Successfully imported ${result.count} menu items`
    );
  } catch (err) {
    setError(err.message);
  } finally {
    setLoading(false);
  }
};
```

#### When Order is Created
In your order creation function:

```typescript
const createOrder = async (orderData) => {
  const order = await addDoc(collection(db, 'orders'), orderData);
  
  // Add notification
  await notificationService.addOrderNotification(
    order.id,
    orderData.tableNumber
  );
  
  return order;
};
```

## Testing the Features

### Test Bell Animation
1. Add unread notifications (read: false)
2. Bell should wiggle continuously
3. Red badge shows count
4. Mark all as read → bell stops wiggling

### Test Bubble Pop Animation
1. Close and reopen dropdown
2. Watch badge scale from 0 → 1.2 → 1
3. Smooth cubic-bezier animation

### Test Slide In
1. Add multiple notifications
2. Each should slide in from left
3. Staggered timing creates wave effect

### Test Mark as Read
1. Click blue icon circle on unread notification
2. Should turn white instantly
3. Blue left border disappears
4. Blue dot disappears
5. Firebase updates in real-time

### Test Delete Animation
1. Hover over notification
2. Red X button appears on right
3. Click X button
4. Notification slides right and fades
5. Removed from Firebase after 300ms

### Test Time Formatting
- Just now: < 1 minute
- 5m ago: 5 minutes
- 1h ago: 1 hour
- 2d ago: 2 days

## Quick Test Script

Run this in browser console after opening admin dashboard:

```javascript
// Clear all notifications
const notificationsRef = collection(db, 'notifications');
const snapshot = await getDocs(notificationsRef);
snapshot.docs.forEach(doc => deleteDoc(doc.ref));

// Add test notifications
const testNotifications = [
  {
    type: 'order',
    title: 'New Order #1234',
    message: 'Table 5 ordered 20 pcs Wings',
    read: false,
    createdAt: Timestamp.now()
  },
  {
    type: 'order',
    title: 'Order Completed #1230',
    message: 'Table 3 order is ready',
    read: false,
    createdAt: new Timestamp(Date.now() / 1000 - 900, 0) // 15m ago
  },
  {
    type: 'system',
    title: 'Menu Updated',
    message: 'Successfully imported 67 items',
    read: true,
    createdAt: new Timestamp(Date.now() / 1000 - 3600, 0) // 1h ago
  }
];

for (const notif of testNotifications) {
  await addDoc(notificationsRef, notif);
}

console.log('✅ Test notifications added!');
```

## Expected Behavior Checklist

- [ ] Bell wiggles when unread notifications exist
- [ ] Badge shows correct count (9+ for 10+)
- [ ] Badge has bubble pop animation
- [ ] Dropdown slides down smoothly
- [ ] Notifications slide in from left
- [ ] Unread have blue background + border
- [ ] Clicking icon marks as read
- [ ] "Mark all read" button works
- [ ] Hover shows delete button
- [ ] Delete slides out notification
- [ ] Time displays correctly (5m ago, 1h ago)
- [ ] Dropdown positioned 8px from edge
- [ ] Empty state shows when no notifications
- [ ] Real-time updates work without refresh

---

Last Updated: December 5, 2025
