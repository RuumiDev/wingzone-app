# Background Notifications Setup

## Current Issue
External notifications only work when the app is open because the Firebase listener (Composable function) only runs when the app is active.

## Solution Options

### Option 1: WorkManager (Implemented)
We've created `OrderNotificationWorker` that runs periodically in the background to check for order updates.

**To Enable:**

1. Add WorkManager dependency to `app/build.gradle.kts`:
```kotlin
dependencies {
    implementation("androidx.work:work-runtime-ktx:2.9.0")
}
```

2. Schedule the worker in `MainActivity.onCreate()`:
```kotlin
import androidx.work.*
import java.util.concurrent.TimeUnit

// In onCreate, after setting content:
val workRequest = PeriodicWorkRequestBuilder<OrderNotificationWorker>(
    15, TimeUnit.MINUTES // Check every 15 minutes
).setConstraints(
    Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
).build()

WorkManager.getInstance(this).enqueueUniquePeriodicWork(
    "order_notifications",
    ExistingPeriodicWorkPolicy.KEEP,
    workRequest
)
```

**Limitations:**
- Minimum interval is 15 minutes (Android restriction)
- Not real-time, there will be delays
- Battery-optimized by Android

### Option 2: Firebase Cloud Messaging (FCM) - Recommended for Production

FCM sends push notifications directly from Firebase server, working even when app is completely closed.

**Setup Steps:**

1. Enable Firebase Cloud Messaging in Firebase Console
2. Add FCM dependency to `app/build.gradle.kts`:
```kotlin
implementation("com.google.firebase:firebase-messaging:23.4.0")
```

3. Create FCM Service:
```kotlin
class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val orderId = message.data["orderId"]
        val status = message.data["status"]
        // Show notification
    }
}
```

4. Add to `AndroidManifest.xml`:
```xml
<service
    android:name=".service.MyFirebaseMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

5. Create Cloud Function to send FCM when order status changes:
```javascript
// Firebase Cloud Function
exports.sendOrderNotification = functions.firestore
    .document('orders/{orderId}')
    .onUpdate(async (change, context) => {
        const newStatus = change.after.data().status;
        const userId = change.after.data().userId;
        
        // Get user's FCM token from Firestore
        const userDoc = await admin.firestore()
            .collection('users')
            .doc(userId)
            .get();
        
        const fcmToken = userDoc.data().fcmToken;
        
        // Send notification
        await admin.messaging().send({
            token: fcmToken,
            notification: {
                title: `Order ${newStatus}!`,
                body: `Your order is now ${newStatus}`
            },
            data: {
                orderId: context.params.orderId,
                status: newStatus
            }
        });
    });
```

**Advantages:**
- Real-time notifications (instant)
- Works when app is completely closed
- Low battery usage
- Reliable delivery

### Option 3: Foreground Service (Not Recommended)
Keep app running in background with ongoing notification. High battery usage, user can disable.

## Current Implementation

The app currently uses **in-process Firebase listeners** which only work when app is active. This is why notifications don't work when the app is closed.

## Recommended Action

For production: **Implement FCM (Option 2)** for reliable, real-time notifications.
For testing/MVP: **Use WorkManager (Option 1)** for background checks.

## Group Orders vs Individual Orders

We've now marked orders with:
- `orderType`: "individual" or "group"
- `groupOrderCode`: The group order code (e.g., "WZABCD")

**Admin Dashboard Filtering:**
Update your admin dashboard queries to filter by `orderType`:
```javascript
// Individual orders only
db.collection('orders')
  .where('orderType', '==', 'individual')
  .get()

// Group orders only
db.collection('orders')
  .where('orderType', '==', 'group')
  .get()
```

The tabs in your admin dashboard should filter based on this field!
