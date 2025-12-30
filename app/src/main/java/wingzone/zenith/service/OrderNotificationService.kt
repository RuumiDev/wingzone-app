package wingzone.zenith.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import wingzone.zenith.MainActivity
import wingzone.zenith.R

class OrderNotificationService(private val context: Context) {
    
    companion object {
        private const val CHANNEL_ID = "order_updates"
        private const val CHANNEL_NAME = "Order Updates"
        private const val CHANNEL_DESCRIPTION = "Notifications for order status updates"
    }
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
                // Make it more alert-like with stronger settings
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    fun showOrderStatusNotification(
        orderId: String,
        status: String,
        title: String,
        message: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("ORDER_ID", orderId)
            putExtra("NAVIGATE_TO_TRACKING", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            orderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // You can replace with your app icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Alert sound, vibration, and lights
            .setVibrate(longArrayOf(0, 400, 200, 400)) // Alert-style vibration pattern
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
        
        notificationManager.notify(orderId.hashCode(), notification)
    }
    
    fun getNotificationTitle(status: String): String {
        return when (status.lowercase()) {
            "pending" -> "Order Received! 🎉"
            "confirmed" -> "Order Confirmed ✅"
            "preparing" -> "Your Food is Being Prepared 👨‍🍳"
            "ready" -> "Order Ready for Pickup! 🍗"
            "delivered" -> "Order Completed 🎊"
            else -> "Order Update"
        }
    }
    
    fun getNotificationMessage(status: String, estimatedTime: String): String {
        return when (status.lowercase()) {
            "pending" -> "We've received your order. Estimated time: $estimatedTime"
            "confirmed" -> "Your order has been confirmed and will be prepared soon"
            "preparing" -> "Our chefs are preparing your delicious meal. Ready in $estimatedTime"
            "ready" -> "Your order is ready! Come pick it up at WingZone"
            "delivered" -> "Thank you for ordering from WingZone! Enjoy your meal 😊"
            else -> "Your order status has been updated"
        }
    }
    
    /**
     * Admin notification for new incoming orders
     */
    fun showAdminOrderNotification(
        orderId: String,
        userName: String,
        total: Double,
        itemCount: Int
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("ORDER_ID", orderId)
            putExtra("NAVIGATE_TO_ADMIN", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            orderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🔔 New Order Received!")
            .setContentText("Order #${orderId.takeLast(6)} - ${userName} - RM ${String.format("%.2f", total)}")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("New order from $userName\nTotal: RM ${String.format("%.2f", total)}\nTap to view details"))
            .setVibrate(longArrayOf(0, 500, 250, 500))
            .build()
        
        notificationManager.notify(("admin_$orderId").hashCode(), notification)
    }
}
