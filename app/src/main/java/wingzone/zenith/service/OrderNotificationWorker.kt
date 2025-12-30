package wingzone.zenith.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Background worker to check for order updates and send notifications
 * This runs periodically even when the app is closed
 */
class OrderNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val notificationService = OrderNotificationService(context)
    
    override suspend fun doWork(): Result {
        return try {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            if (currentUserId != null) {
                checkOrderUpdates(currentUserId)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
    
    private suspend fun checkOrderUpdates(userId: String) {
        val firestore = FirebaseFirestore.getInstance()
        
        // Get active orders
        val snapshot = firestore.collection("orders")
            .whereEqualTo("userId", userId)
            .whereIn("status", listOf("pending", "confirmed", "preparing", "ready"))
            .get()
            .await()
        
        snapshot.documents.forEach { doc ->
            val orderId = doc.id
            val status = doc.getString("status") ?: "pending"
            
            // Check if status changed since last check
            val lastStatus = getLastKnownStatus(orderId)
            if (lastStatus != null && lastStatus != status) {
                // Status changed - send notification
                val title = notificationService.getNotificationTitle(status)
                val message = notificationService.getNotificationMessage(
                    status,
                    getEstimatedTime(status)
                )
                notificationService.showOrderStatusNotification(
                    orderId = orderId,
                    status = status,
                    title = title,
                    message = message
                )
            }
            
            // Save current status
            saveLastKnownStatus(orderId, status)
        }
    }
    
    private fun getLastKnownStatus(orderId: String): String? {
        val prefs = applicationContext.getSharedPreferences("order_status", Context.MODE_PRIVATE)
        return prefs.getString("status_$orderId", null)
    }
    
    private fun saveLastKnownStatus(orderId: String, status: String) {
        val prefs = applicationContext.getSharedPreferences("order_status", Context.MODE_PRIVATE)
        prefs.edit().putString("status_$orderId", status).apply()
    }
    
    private fun getEstimatedTime(status: String): String {
        return when (status.lowercase()) {
            "pending" -> "5 - 10 min"
            "confirmed" -> "10 - 15 min"
            "preparing" -> "15 - 20 min"
            "ready" -> "Ready now"
            "delivered" -> "Completed"
            else -> "Estimating..."
        }
    }
    
    companion object {
        private const val WORK_NAME = "order_notification_worker"
        
        fun schedulePeriodicWork(context: Context) {
            val constraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()
            
            val workRequest = androidx.work.PeriodicWorkRequestBuilder<OrderNotificationWorker>(
                15, java.util.concurrent.TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()
            
            androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
        
        fun cancelWork(context: Context) {
            androidx.work.WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
