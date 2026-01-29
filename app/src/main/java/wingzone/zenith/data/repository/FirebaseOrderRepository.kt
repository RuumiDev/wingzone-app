package wingzone.zenith.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import wingzone.zenith.data.models.Cart
import wingzone.zenith.data.models.CartItem
import java.util.Date

data class Order(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val items: List<CartItem> = emptyList(),
    val subtotal: Double = 0.0,
    val tax: Double = 0.0,
    val total: Double = 0.0,
    val status: String = "pending", // pending, confirmed, preparing, ready, delivered, cancelled
    val paymentStatus: String = "unpaid", // unpaid, paid
    val paymentMethod: String = "", // cash, card, online
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val deliveryAddress: String? = null,
    val deliveryNotes: String? = null,
    val phoneNumber: String? = null
)

class FirebaseOrderRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val ordersCollection = firestore.collection("orders")
    private var orderListener: ListenerRegistration? = null
    
    /**
     * Create a new order from cart
     */
    suspend fun createOrder(
        userId: String,
        userName: String,
        cart: Cart,
        paymentMethod: String,
        deliveryAddress: String? = null,
        deliveryNotes: String? = null,
        phoneNumber: String? = null,
        orderType: String? = null,
        location: String? = null,
        lobbyPaymentMethod: String? = null
    ): Result<String> {
        return createOrderInternal(
            userId = userId,
            userName = userName,
            cart = cart,
            paymentMethod = paymentMethod,
            deliveryAddress = deliveryAddress,
            deliveryNotes = deliveryNotes,
            phoneNumber = phoneNumber,
            isGroupOrder = false,
            groupOrderCode = null,
            orderType = orderType,
            location = location,
            lobbyPaymentMethod = lobbyPaymentMethod
        )
    }
    
    /**
     * Create a consolidated group order (one order for all members)
     */
    suspend fun createGroupOrder(
        hostUserId: String,
        hostUserName: String,
        cart: Cart,
        groupOrderCode: String,
        memberCount: Int,
        memberDetails: String,
        paymentMethod: String,
        deliveryAddress: String? = null,
        deliveryNotes: String? = null,
        phoneNumber: String? = null
    ): Result<String> {
        return try {
            val orderData = hashMapOf(
                "userId" to hostUserId,
                "userName" to hostUserName,
                "isGroupOrder" to true,
                "groupOrderCode" to groupOrderCode,
                "memberCount" to memberCount,
                "memberDetails" to memberDetails,
                "items" to cart.items.map { item ->
                    hashMapOf(
                        "menuItem" to hashMapOf(
                            "id" to item.menuItem.id,
                            "name" to item.menuItem.name,
                            "description" to item.menuItem.description,
                            "price" to item.menuItem.price,
                            "category" to item.menuItem.category
                        ),
                        "quantity" to item.quantity,
                        "customization" to item.customization,
                        "specialInstructions" to (item.specialInstructions ?: "")
                    )
                },
                "subtotal" to cart.subtotal,
                "tax" to cart.tax,
                "total" to cart.total,
                "totalItems" to cart.totalItems,
                "status" to "pending",
                "paymentStatus" to "paid",
                "paymentMethod" to paymentMethod,
                "createdAt" to Date(),
                "updatedAt" to Date(),
                "deliveryAddress" to (deliveryAddress ?: ""),
                "deliveryNotes" to (deliveryNotes ?: ""),
                "phoneNumber" to (phoneNumber ?: "")
            )
            
            val docRef = ordersCollection.add(orderData).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun createOrderInternal(
        userId: String,
        userName: String,
        cart: Cart,
        paymentMethod: String,
        deliveryAddress: String? = null,
        deliveryNotes: String? = null,
        phoneNumber: String? = null,
        isGroupOrder: Boolean,
        groupOrderCode: String?,
        orderType: String? = null,
        location: String? = null,
        lobbyPaymentMethod: String? = null
    ): Result<String> {
        return try {
            val orderData = hashMapOf(
                "userId" to userId,
                "userName" to userName,
                "isGroupOrder" to isGroupOrder,
                "groupOrderCode" to (groupOrderCode ?: ""),
                "orderType" to (orderType ?: "pickup"),
                "location" to (location ?: ""),
                "lobbyPaymentMethod" to (lobbyPaymentMethod ?: ""),
                "items" to cart.items.map { item ->
                    hashMapOf(
                        "menuItem" to hashMapOf(
                            "id" to item.menuItem.id,
                            "name" to item.menuItem.name,
                            "description" to item.menuItem.description,
                            "price" to item.menuItem.price,
                            "category" to item.menuItem.category,
                            "kitchenIngredients" to item.menuItem.kitchenIngredients?.let { kitchen ->
                                hashMapOf(
                                    "ingredients" to kitchen.ingredients.map { ingredient ->
                                        hashMapOf(
                                            "type" to ingredient.type,
                                            "quantity" to ingredient.quantity,
                                            "requiresSelection" to ingredient.requiresSelection
                                        )
                                    }
                                )
                            }
                        ),
                        "quantity" to item.quantity,
                        "customization" to item.customization,
                        "specialInstructions" to (item.specialInstructions ?: "")
                    )
                },
                "subtotal" to cart.subtotal,
                "tax" to cart.tax,
                "total" to cart.total,
                "totalItems" to cart.totalItems,
                "status" to "pending",
                "paymentStatus" to "paid",
                "paymentMethod" to paymentMethod,
                "createdAt" to Date(),
                "updatedAt" to Date(),
                "deliveryAddress" to (deliveryAddress ?: ""),
                "deliveryNotes" to (deliveryNotes ?: ""),
                "phoneNumber" to (phoneNumber ?: "")
            )
            
            val docRef = ordersCollection.add(orderData).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get user's order history
     */
    suspend fun getUserOrders(userId: String): List<Order> {
        return try {
            val snapshot = ordersCollection
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    Order(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        userName = doc.getString("userName") ?: "",
                        items = emptyList(), // Parse items if needed
                        subtotal = doc.getDouble("subtotal") ?: 0.0,
                        tax = doc.getDouble("tax") ?: 0.0,
                        total = doc.getDouble("total") ?: 0.0,
                        status = doc.getString("status") ?: "pending",
                        paymentStatus = doc.getString("paymentStatus") ?: "unpaid",
                        paymentMethod = doc.getString("paymentMethod") ?: "",
                        createdAt = doc.getDate("createdAt") ?: Date(),
                        updatedAt = doc.getDate("updatedAt") ?: Date(),
                        deliveryAddress = doc.getString("deliveryAddress"),
                        deliveryNotes = doc.getString("deliveryNotes"),
                        phoneNumber = doc.getString("phoneNumber")
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Listen to order status updates
     */
    fun listenToOrder(orderId: String): Flow<Order?> = callbackFlow {
        orderListener = ordersCollection.document(orderId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val order = snapshot?.let { doc ->
                    try {
                        Order(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            userName = doc.getString("userName") ?: "",
                            items = emptyList(),
                            subtotal = doc.getDouble("subtotal") ?: 0.0,
                            tax = doc.getDouble("tax") ?: 0.0,
                            total = doc.getDouble("total") ?: 0.0,
                            status = doc.getString("status") ?: "pending",
                            paymentStatus = doc.getString("paymentStatus") ?: "unpaid",
                            paymentMethod = doc.getString("paymentMethod") ?: "",
                            createdAt = doc.getDate("createdAt") ?: Date(),
                            updatedAt = doc.getDate("updatedAt") ?: Date(),
                            deliveryAddress = doc.getString("deliveryAddress"),
                            deliveryNotes = doc.getString("deliveryNotes"),
                            phoneNumber = doc.getString("phoneNumber")
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                
                trySend(order)
            }
        
        awaitClose { orderListener?.remove() }
    }
    
    /**
     * Cancel an order
     */
    suspend fun cancelOrder(orderId: String): Result<Unit> {
        return try {
            ordersCollection.document(orderId)
                .update(
                    mapOf(
                        "status" to "cancelled",
                        "updatedAt" to Date()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun stopListening() {
        orderListener?.remove()
        orderListener = null
    }
}
