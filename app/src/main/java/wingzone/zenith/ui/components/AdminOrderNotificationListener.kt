package wingzone.zenith.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.firestore.FirebaseFirestore
import wingzone.zenith.data.repository.Order
import wingzone.zenith.service.OrderNotificationService
import java.util.*

/**
 * Real-time listener for admin dashboard to get notified of new orders
 * This component should be placed in the admin dashboard root
 */
@Composable
fun AdminOrderNotificationListener(
    onNewOrder: (Order) -> Unit = {}
) {
    val context = LocalContext.current
    val notificationService = remember { OrderNotificationService(context) }
    val processedOrderIds = remember { mutableSetOf<String>() }
    
    LaunchedEffect(Unit) {
        val firestore = FirebaseFirestore.getInstance()
        
        // Listen for ALL orders (for admin)
        firestore.collection("orders")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                
                snapshot.documentChanges.forEach { change ->
                    // Only process new orders (not modifications)
                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        val doc = change.document
                        val orderId = doc.id
                        
                        // Check if we've already processed this order
                        if (orderId !in processedOrderIds) {
                            processedOrderIds.add(orderId)
                            
                            try {
                                val order = Order(
                                    id = orderId,
                                    userId = doc.getString("userId") ?: "",
                                    userName = doc.getString("userName") ?: "Guest",
                                    items = emptyList(),
                                    subtotal = (doc.get("subtotal") as? Number)?.toDouble() ?: 0.0,
                                    tax = (doc.get("tax") as? Number)?.toDouble() ?: 0.0,
                                    total = (doc.get("total") as? Number)?.toDouble() ?: 0.0,
                                    status = doc.getString("status") ?: "pending",
                                    paymentStatus = doc.getString("paymentStatus") ?: "unpaid",
                                    paymentMethod = doc.getString("paymentMethod") ?: "cash",
                                    createdAt = (doc.get("createdAt") as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
                                    updatedAt = (doc.get("updatedAt") as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
                                    deliveryAddress = doc.getString("deliveryAddress"),
                                    deliveryNotes = doc.getString("deliveryNotes"),
                                    phoneNumber = doc.getString("phoneNumber")
                                )
                                
                                // Notify callback
                                onNewOrder(order)
                                
                                // Send notification to admin
                                notificationService.showAdminOrderNotification(
                                    orderId = order.id,
                                    userName = order.userName,
                                    total = order.total,
                                    itemCount = order.items.size
                                )
                                
                            } catch (e: Exception) {
                                // Ignore parsing errors
                            }
                        }
                    }
                }
            }
    }
}
