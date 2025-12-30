# Admin Dashboard Integration Guide

## Notification System for New Orders

To enable real-time notifications for incoming orders in the admin dashboard, add the `AdminOrderNotificationListener` component to your admin dashboard root.

### Usage in Admin Dashboard

```kotlin
import wingzone.zenith.ui.components.AdminOrderNotificationListener

@Composable
fun AdminDashboard() {
    // Add this at the root level of your admin screen
    AdminOrderNotificationListener(
        onNewOrder = { order ->
            // Handle new order in UI (e.g., update order list, show badge)
            println("New order received: ${order.id}")
        }
    )
    
    // Your admin dashboard UI
    Column {
        // ... admin content
    }
}
```

### What it Does

1. **Real-time Listening**: Listens to all new orders in Firebase in real-time
2. **Android Notifications**: Sends system notifications with order details
3. **Prevents Duplicates**: Tracks processed orders to avoid duplicate notifications
4. **Order Details**: Provides full order object with:
   - Order ID
   - Customer name
   - Total amount
   - Status
   - Payment method
   - Items (if available)

### Notification Content

Admin notifications include:
- **Title**: "🔔 New Order Received!"
- **Message**: Order ID (last 6 chars), customer name, total amount
- **Action**: Tap to open order details in admin dashboard
- **Vibration**: Double vibration pattern for attention

### Integration with Web Admin (slash-admin-template)

Since the admin dashboard is a separate React/TypeScript project, you'll need to:

1. Create a Firebase Cloud Function to send notifications to admin devices
2. Or integrate Firebase Cloud Messaging (FCM) in the web dashboard
3. Or poll the `/orders` collection for new orders

For Android admin app, simply add the component to your admin screen composable.
