package wingzone.zenith.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import wingzone.zenith.data.repository.Order
import wingzone.zenith.service.OrderNotificationService
import java.util.*

/**
 * Listens for order status changes and shows bottom sheet notification
 * Similar to GrabFood's real-time order tracking
 */
@Composable
fun OrderStatusListener(
    userId: String,
    onStatusUpdate: (Order) -> Unit = {}
) {
    val context = LocalContext.current
    val notificationService = remember { OrderNotificationService(context) }
    val previousOrderStates = remember { mutableMapOf<String, String>() }
    
    LaunchedEffect(userId) {
        val firestore = FirebaseFirestore.getInstance()
        
        // Listen for active orders only (not delivered)
        firestore.collection("orders")
            .whereEqualTo("userId", userId)
            .whereIn("status", listOf("pending", "confirmed", "preparing", "ready"))
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                
                snapshot.documents.forEach { doc ->
                    try {
                        val order = Order(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            userName = doc.getString("userName") ?: "",
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
                        
                        // Check if order is new or status changed
                        val previousStatus = previousOrderStates[order.id]
                        val isNewOrder = previousStatus == null
                        val statusChanged = previousStatus != null && previousStatus != order.status
                        
                        if (isNewOrder || statusChanged) {
                            // Always update in-app state for bottom sheet
                            onStatusUpdate(order)
                            
                            // Always send Android notification for reliability
                            // In-app notification (bottom sheet) shows when app is active
                            // System notification shows in tray regardless
                            val title = notificationService.getNotificationTitle(order.status)
                            val message = notificationService.getNotificationMessage(
                                order.status,
                                getEstimatedTime(order.status)
                            )
                            notificationService.showOrderStatusNotification(
                                orderId = order.id,
                                status = order.status,
                                title = title,
                                message = message
                            )
                        }
                        
                        previousOrderStates[order.id] = order.status
                        
                    } catch (e: Exception) {
                        // Ignore parsing errors
                    }
                }
            }
    }
}

/**
 * Shows floating notification when order status changes
 */
@Composable
fun OrderNotificationHandler(
    onNavigateToTracking: (String) -> Unit = {}
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    var activeOrders by remember { mutableStateOf<List<Order>>(emptyList()) }
    val dismissedOrders = remember { mutableStateListOf<String>() }
    var previousUserId by remember { mutableStateOf<String?>(null) }
    
    // Clear state when user changes (logout/login)
    LaunchedEffect(currentUserId) {
        if (currentUserId != previousUserId) {
            activeOrders = emptyList()
            dismissedOrders.clear()
            previousUserId = currentUserId
        }
    }
    
    if (currentUserId != null) {
        OrderStatusListener(
            userId = currentUserId,
            onStatusUpdate = { order ->
                // Only add non-delivered orders to active list
                if (order.status != "delivered") {
                    activeOrders = activeOrders.filter { it.id != order.id } + order
                } else {
                    // Remove delivered orders from active list
                    activeOrders = activeOrders.filter { it.id != order.id }
                }
            }
        )
    }
    
    // Show notification for first undismissed active order (non-delivered only)
    val orderToShow = activeOrders
        .filter { it.status != "delivered" }
        .firstOrNull { it.id !in dismissedOrders }
    
    if (orderToShow != null) {
        OrderStatusBottomSheet(
            orderId = orderToShow.id,
            status = orderToShow.status,
            estimatedTime = getEstimatedTime(orderToShow.status),
            storeName = "WingZone",
            onViewDetails = {
                dismissedOrders.add(orderToShow.id)
                onNavigateToTracking(orderToShow.id)
            },
            onDismiss = {
                dismissedOrders.add(orderToShow.id)
            }
        )
    }
}

fun getEstimatedTime(status: String): String {
    return when (status.lowercase()) {
        "pending" -> "5 - 10 min"
        "confirmed" -> "10 - 15 min"
        "preparing" -> "15 - 20 min"
        "ready" -> "Ready now"
        "delivered" -> "Completed"
        else -> "Estimating..."
    }
}
