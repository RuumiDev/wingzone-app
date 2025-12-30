# Firebase Cloud Functions Setup Guide

## Initialize Functions

```bash
cd wz-app
firebase init functions
# Select TypeScript
# Install dependencies
```

## Project Structure

```
functions/
├── src/
│   ├── index.ts              # Main entry point
│   ├── types.ts              # Import shared types
│   ├── triggers/
│   │   ├── onGroupOrderConfirm.ts
│   │   ├── onOrderCreate.ts
│   │   └── scheduledFunctions.ts
│   ├── api/
│   │   ├── orders.ts
│   │   ├── menu.ts
│   │   └── receipts.ts
│   └── utils/
│       ├── notifications.ts
│       ├── receiptGenerator.ts
│       └── analytics.ts
├── package.json
└── tsconfig.json
```

## Sample Implementation

### `functions/src/index.ts`

```typescript
import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import { onGroupOrderConfirm } from './triggers/onGroupOrderConfirm';
import { generateOrderNumber } from './api/orders';
import { generateReceipt } from './api/receipts';
import { updateDailyAnalytics } from './triggers/scheduledFunctions';

admin.initializeApp();

// Export all functions
export {
  onGroupOrderConfirm,
  generateOrderNumber,
  generateReceipt,
  updateDailyAnalytics,
};

// HTTPS Callable Functions
export const createOrder = functions.https.onCall(async (data, context) => {
  // Authentication check
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'User must be logged in');
  }

  const { items, deliveryAddress, orderType } = data;
  
  // Generate order number
  const orderNumber = await generateOrderNumber();
  
  // Create order in Firestore
  const orderRef = await admin.firestore().collection('individual_orders').add({
    userId: context.auth.uid,
    userName: context.auth.token.name || '',
    userEmail: context.auth.token.email || '',
    orderNumber,
    items,
    deliveryAddress,
    orderType,
    status: 'pending',
    paymentStatus: 'pending',
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  });

  return { success: true, orderId: orderRef.id, orderNumber };
});

// More functions...
```

### `functions/src/triggers/onGroupOrderConfirm.ts`

```typescript
import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import { GroupOrder, IndividualOrder } from '../types';

export const onGroupOrderConfirm = functions.firestore
  .document('group_orders/{orderId}')
  .onUpdate(async (change, context) => {
    const before = change.before.data() as GroupOrder;
    const after = change.after.data() as GroupOrder;

    // Check if status changed to 'confirmed'
    if (before.status !== 'confirmed' && after.status === 'confirmed') {
      console.log(`Group order ${context.params.orderId} confirmed. Creating individual orders...`);

      // Split into individual orders
      const batch = admin.firestore().batch();
      const orderNumber = await generateGroupOrderNumber();

      after.members.forEach((member, index) => {
        if (member.cartItems.length > 0) {
          const individualOrder: Partial<IndividualOrder> = {
            userId: member.userId,
            userName: member.name,
            userEmail: member.email,
            orderNumber: `${orderNumber}-${index + 1}`,
            items: member.cartItems,
            subtotal: member.memberTotal,
            tax: member.memberTotal * 0.06,
            total: member.memberTotal * 1.06,
            status: 'confirmed',
            paymentStatus: member.paymentStatus,
            orderType: 'group',
            groupOrderId: context.params.orderId,
            seatNumber: `${index + 1}`,
            deliveryAddress: after.deliveryAddress,
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
          };

          const orderRef = admin.firestore().collection('individual_orders').doc();
          batch.set(orderRef, individualOrder);

          // Add to kitchen queue
          const queueItem = {
            orderId: orderRef.id,
            orderNumber: individualOrder.orderNumber,
            items: member.cartItems.map(item => ({
              menuItemName: item.menuItemName,
              quantity: item.quantity,
              customization: item.customization,
              seatNumber: `${index + 1}`,
              customerName: member.name,
            })),
            priority: 3,
            status: 'queued',
            queuedAt: admin.firestore.FieldValue.serverTimestamp(),
            estimatedTime: 20, // 20 minutes default
          };

          const queueRef = admin.firestore().collection('kitchen_queue').doc();
          batch.set(queueRef, queueItem);
        }
      });

      await batch.commit();
      console.log('Individual orders and kitchen queue items created');

      // Send notifications
      await sendGroupOrderNotifications(after);
    }
  });

async function generateGroupOrderNumber(): Promise<string> {
  const date = new Date();
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  
  // Get today's order count
  const snapshot = await admin.firestore()
    .collection('individual_orders')
    .where('orderNumber', '>=', `WZ${year}${month}${day}`)
    .where('orderNumber', '<', `WZ${year}${month}${day}Z`)
    .get();

  const sequence = snapshot.size + 1;
  return `WZ${year}${month}${day}-${String(sequence).padStart(3, '0')}`;
}

async function sendGroupOrderNotifications(order: GroupOrder) {
  // Implementation for FCM notifications
  const tokens = await getDeviceTokens(order.members.map(m => m.userId));
  
  const message = {
    notification: {
      title: 'Group Order Confirmed!',
      body: `Your order ${order.code} has been confirmed and sent to the kitchen.`,
    },
    data: {
      groupOrderId: order.id,
      type: 'order_confirmed',
    },
    tokens,
  };

  await admin.messaging().sendMulticast(message);
}

async function getDeviceTokens(userIds: string[]): Promise<string[]> {
  // Fetch FCM tokens from users collection
  const tokens: string[] = [];
  for (const userId of userIds) {
    const userDoc = await admin.firestore().collection('users').doc(userId).get();
    const fcmToken = userDoc.data()?.fcmToken;
    if (fcmToken) tokens.push(fcmToken);
  }
  return tokens;
}
```

### `functions/src/api/receipts.ts`

```typescript
import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import { IndividualOrder, Receipt } from '../types';

export const generateReceipt = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'Must be logged in');
  }

  const { orderId, type } = data;

  // Fetch order
  const orderDoc = await admin.firestore().collection('individual_orders').doc(orderId).get();
  if (!orderDoc.exists) {
    throw new functions.https.HttpsError('not-found', 'Order not found');
  }

  const order = orderDoc.data() as IndividualOrder;

  // Generate HTML content
  const htmlContent = generateReceiptHTML(order, type);

  // Save receipt
  const receipt: Partial<Receipt> = {
    orderId,
    orderNumber: order.orderNumber,
    type,
    htmlContent,
    printedAt: admin.firestore.FieldValue.serverTimestamp(),
    printedBy: context.auth.uid,
    printerStatus: 'pending',
  };

  const receiptRef = await admin.firestore().collection('receipts').add(receipt);

  return { success: true, receiptId: receiptRef.id, htmlContent };
});

function generateReceiptHTML(order: IndividualOrder, type: string): string {
  if (type === 'order') {
    return `
      <!DOCTYPE html>
      <html>
      <head>
        <style>
          @media print {
            body { width: 80mm; font-family: monospace; }
          }
          .receipt { padding: 10px; }
          .header { text-align: center; font-size: 18px; font-weight: bold; }
          .item { display: flex; justify-content: space-between; margin: 5px 0; }
          .total { border-top: 1px dashed #000; margin-top: 10px; padding-top: 10px; }
        </style>
      </head>
      <body>
        <div class="receipt">
          <div class="header">WINGZONE</div>
          <p>Order: ${order.orderNumber}</p>
          <p>Date: ${new Date().toLocaleDateString()}</p>
          <p>Customer: ${order.userName}</p>
          <hr>
          ${order.items.map(item => `
            <div class="item">
              <span>${item.menuItemName} x${item.quantity}</span>
              <span>RM ${item.subtotal.toFixed(2)}</span>
            </div>
          `).join('')}
          <div class="total">
            <div class="item">
              <span>Subtotal</span>
              <span>RM ${order.subtotal.toFixed(2)}</span>
            </div>
            <div class="item">
              <span>Tax (6%)</span>
              <span>RM ${order.tax.toFixed(2)}</span>
            </div>
            <div class="item" style="font-weight: bold;">
              <span>TOTAL</span>
              <span>RM ${order.total.toFixed(2)}</span>
            </div>
          </div>
          <p style="text-align: center; margin-top: 20px;">Thank you!</p>
        </div>
      </body>
      </html>
    `;
  } else if (type === 'label') {
    return `
      <div style="width: 40mm; height: 30mm; padding: 5mm; border: 1px solid #000;">
        <div style="font-size: 16px; font-weight: bold;">SEAT ${order.seatNumber}</div>
        <div style="font-size: 12px;">${order.userName}</div>
        <div style="font-size: 10px; margin-top: 5mm;">${order.orderNumber}</div>
      </div>
    `;
  }

  return '';
}
```

### `functions/src/triggers/scheduledFunctions.ts`

```typescript
import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import { DailyAnalytics } from '../types';

// Run every day at midnight
export const updateDailyAnalytics = functions.pubsub
  .schedule('0 0 * * *')
  .timeZone('Asia/Kuala_Lumpur')
  .onRun(async (context) => {
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    yesterday.setHours(0, 0, 0, 0);

    const today = new Date(yesterday);
    today.setDate(today.getDate() + 1);

    // Get all orders from yesterday
    const ordersSnapshot = await admin.firestore()
      .collection('individual_orders')
      .where('createdAt', '>=', yesterday)
      .where('createdAt', '<', today)
      .get();

    let totalRevenue = 0;
    let totalItems = 0;
    const itemsSold: Record<string, { name: string; quantity: number; revenue: number }> = {};

    ordersSnapshot.forEach(doc => {
      const order = doc.data();
      totalRevenue += order.total;

      order.items.forEach((item: any) => {
        totalItems += item.quantity;
        if (!itemsSold[item.menuItemId]) {
          itemsSold[item.menuItemId] = {
            name: item.menuItemName,
            quantity: 0,
            revenue: 0,
          };
        }
        itemsSold[item.menuItemId].quantity += item.quantity;
        itemsSold[item.menuItemId].revenue += item.subtotal;
      });
    });

    // Create analytics document
    const analytics: Partial<DailyAnalytics> = {
      date: yesterday,
      totalOrders: ordersSnapshot.size,
      totalRevenue,
      totalItems,
      averageOrderValue: ordersSnapshot.size > 0 ? totalRevenue / ordersSnapshot.size : 0,
      topSellingItems: Object.entries(itemsSold)
        .map(([id, data]) => ({
          menuItemId: id,
          menuItemName: data.name,
          quantitySold: data.quantity,
          revenue: data.revenue,
        }))
        .sort((a, b) => b.quantitySold - a.quantitySold)
        .slice(0, 10),
    };

    const dateId = yesterday.toISOString().split('T')[0];
    await admin.firestore().collection('analytics').doc(dateId).set(analytics);

    console.log('Daily analytics updated for', dateId);
  });
```

## Deploy Functions

```bash
cd functions
npm run build
cd ..
firebase deploy --only functions
```

## Environment Variables

```bash
firebase functions:config:set app.name="WingZone"
firebase functions:config:set printer.enabled="true"
```

## Testing

```bash
# Local emulator
firebase emulators:start

# Run specific function
firebase functions:shell
```

## Monitoring

- View logs: `firebase functions:log`
- Firebase Console: https://console.firebase.google.com

---

This setup provides the complete backend logic for order processing, notifications, receipts, and analytics! 🚀
