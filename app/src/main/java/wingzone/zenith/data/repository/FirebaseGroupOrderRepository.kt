package wingzone.zenith.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.Timestamp
import wingzone.zenith.data.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.*

class FirebaseGroupOrderRepository(private val authRepository: FirebaseAuthRepository) {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    private val _groupOrders = MutableStateFlow<List<GroupOrder>>(emptyList())
    val groupOrders: StateFlow<List<GroupOrder>> = _groupOrders.asStateFlow()
    
    private val _currentGroupOrder = MutableStateFlow<GroupOrder?>(null)
    val currentGroupOrder: StateFlow<GroupOrder?> = _currentGroupOrder.asStateFlow()
    
    private var ordersListener: ListenerRegistration? = null
    private var currentOrderListener: ListenerRegistration? = null
    
    init {
        startOrdersListener()
    }
    
    private fun startOrdersListener() {
        val userId = auth.currentUser?.uid ?: return
        
        ordersListener?.remove()
        ordersListener = firestore.collection("groupOrders")
            .whereArrayContains("memberIds", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                
                val orders = snapshot?.documents?.mapNotNull { doc ->
                    parseGroupOrder(doc.id, doc.data)
                } ?: emptyList()
                
                _groupOrders.value = orders
            }
    }
    
    private fun parseGroupOrder(id: String, data: Map<String, Any>?): GroupOrder? {
        if (data == null) return null
        
        return try {
            val membersData = data["members"] as? List<Map<String, Any>> ?: emptyList()
            val members = membersData.mapNotNull { memberData ->
                parseGroupMember(memberData)
            }
            
            val createdAtTimestamp = data["createdAt"] as? Timestamp
            val expiresAtTimestamp = data["expiresAt"] as? Timestamp
            
            GroupOrder(
                id = id,
                code = data["code"] as? String ?: "",
                hostId = data["hostUserId"] as? String ?: data["hostId"] as? String ?: "",
                members = members,
                maxMembers = (data["maxMembers"] as? Long)?.toInt() ?: 8,
                status = parseOrderStatus(data["status"] as? String),
                createdAt = createdAtTimestamp?.toDate() ?: Date(),
                expiresAt = expiresAtTimestamp?.toDate() ?: Date(),
                deliveryAddress = data["deliveryAddress"] as? String,
                specialInstructions = data["deliveryInstructions"] as? String ?: data["specialInstructions"] as? String
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parseGroupMember(data: Map<String, Any>): GroupMember? {
        return try {
            val cartItemsData = data["cartItems"] as? List<Map<String, Any>> ?: emptyList()
            val cartItems = cartItemsData.mapNotNull { itemData ->
                parseCartItem(itemData)
            }
            
            GroupMember(
                userId = data["userId"] as? String ?: "",
                name = data["name"] as? String ?: "",
                profileImageUrl = data["profileImageUrl"] as? String,
                isHost = data["isHost"] as? Boolean ?: false,
                cartItems = cartItems
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parseCartItem(data: Map<String, Any>): CartItem? {
        return try {
            val menuItemData = data["menuItem"] as? Map<String, Any> ?: return null
            val customizationData = data["customization"] as? Map<String, Any>
            
            val menuItem = MenuItem(
                id = menuItemData["id"] as? String ?: menuItemData["menuItemId"] as? String ?: "",
                name = menuItemData["name"] as? String ?: menuItemData["menuItemName"] as? String ?: "",
                description = menuItemData["description"] as? String ?: "",
                price = (menuItemData["price"] as? Number ?: menuItemData["basePrice"] as? Number)?.toDouble() ?: 0.0,
                category = menuItemData["category"] as? String ?: "",
                imageUrl = menuItemData["imageUrl"] as? String,
                requiresCustomization = menuItemData["requiresCustomization"] as? Boolean ?: false
            )
            
            val customization = if (customizationData != null) {
                EntreeCustomization(
                    flavor = Flavor.valueOf(customizationData["flavor"] as? String ?: "PLAIN"),
                    dippingSauce = DippingSauce.valueOf(customizationData["dippingSauce"] as? String ?: "NONE"),
                    drink = Drink.valueOf(customizationData["drink"] as? String ?: "NONE")
                )
            } else null
            
            CartItem(
                id = data["id"] as? String ?: UUID.randomUUID().toString(),
                menuItem = menuItem,
                quantity = (data["quantity"] as? Number)?.toInt() ?: 1,
                customization = customization,
                specialInstructions = data["specialInstructions"] as? String
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parseOrderStatus(status: String?): GroupOrderStatus {
        return when (status?.uppercase()) {
            "OPEN" -> GroupOrderStatus.OPEN
            "FULL" -> GroupOrderStatus.FULL
            "ORDERING" -> GroupOrderStatus.ORDERING
            "CONFIRMED" -> GroupOrderStatus.CONFIRMED
            "COMPLETED" -> GroupOrderStatus.COMPLETED
            "CANCELLED" -> GroupOrderStatus.CANCELLED
            "ACTIVE" -> GroupOrderStatus.OPEN
            "PREPARING" -> GroupOrderStatus.CONFIRMED
            "READY" -> GroupOrderStatus.CONFIRMED
            "DELIVERED" -> GroupOrderStatus.COMPLETED
            else -> GroupOrderStatus.OPEN
        }
    }
    
    fun generateOrderCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return "WZ${(1..4).map { chars.random() }.joinToString("")}"
    }
    
    suspend fun createGroupOrder(
        deliveryAddress: String? = null,
        specialInstructions: String? = null
    ): Result<GroupOrder> {
        return try {
            val currentUser = authRepository.getCurrentUser()
                ?: throw Exception("User not authenticated")
            
            val code = generateOrderCode()
            val expiresAt = Calendar.getInstance().apply {
                add(Calendar.HOUR, 2) // Expires in 2 hours
            }.time
            
            val host = GroupMember(
                userId = currentUser.id,
                name = currentUser.name,
                profileImageUrl = currentUser.profileImageUrl,
                isHost = true,
                cartItems = emptyList()
            )
            
            val orderData = hashMapOf(
                "code" to code,
                "hostUserId" to currentUser.id,
                "hostUserName" to currentUser.name,
                "memberIds" to listOf(currentUser.id),
                "members" to listOf(
                    hashMapOf(
                        "userId" to host.userId,
                        "name" to host.name,
                        "email" to currentUser.email,
                        "profileImageUrl" to host.profileImageUrl,
                        "isHost" to true,
                        "cartItems" to emptyList<Any>(),
                        "memberTotal" to 0.0,
                        "paymentStatus" to "pending"
                    )
                ),
                "maxMembers" to 8,
                "status" to "active",
                "createdAt" to Timestamp.now(),
                "expiresAt" to Timestamp(expiresAt),
                "deliveryAddress" to deliveryAddress,
                "deliveryInstructions" to specialInstructions,
                "totalAmount" to 0.0
            )
            
            val docRef = firestore.collection("groupOrders")
                .add(orderData)
                .await()
            
            val groupOrder = GroupOrder(
                id = docRef.id,
                code = code,
                hostId = currentUser.id,
                members = listOf(host),
                maxMembers = 8,
                status = GroupOrderStatus.OPEN,
                createdAt = Date(),
                expiresAt = expiresAt,
                deliveryAddress = deliveryAddress,
                specialInstructions = specialInstructions
            )
            
            _currentGroupOrder.value = groupOrder
            listenToCurrentOrder(docRef.id)
            
            Result.success(groupOrder)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun joinGroupOrder(code: String): Result<GroupOrder> {
        return try {
            val currentUser = authRepository.getCurrentUser()
                ?: throw Exception("User not authenticated")
            
            // Find order by code
            val querySnapshot = firestore.collection("groupOrders")
                .whereEqualTo("code", code.uppercase())
                .whereEqualTo("status", "active")
                .get()
                .await()
            
            if (querySnapshot.isEmpty) {
                throw Exception("Group order not found or is no longer active")
            }
            
            val doc = querySnapshot.documents.first()
            val orderData = doc.data ?: throw Exception("Order data not found")
            
            // Check if already a member
            val memberIds = orderData["memberIds"] as? List<String> ?: emptyList()
            if (memberIds.contains(currentUser.id)) {
                throw Exception("You are already a member of this group order")
            }
            
            // Check if full
            val maxMembers = (orderData["maxMembers"] as? Long)?.toInt() ?: 8
            if (memberIds.size >= maxMembers) {
                throw Exception("Group order is full")
            }
            
            // Add member
            val newMember: Map<String, Any> = hashMapOf(
                "userId" to currentUser.id,
                "name" to currentUser.name,
                "email" to currentUser.email,
                "profileImageUrl" to (currentUser.profileImageUrl ?: ""),
                "isHost" to false,
                "cartItems" to emptyList<Any>(),
                "memberTotal" to 0.0,
                "paymentStatus" to "pending"
            )
            
            val members = (orderData["members"] as? List<Map<String, Any>>)?.toMutableList() ?: mutableListOf()
            members.add(newMember)
            
            val updatedMemberIds = memberIds.toMutableList()
            updatedMemberIds.add(currentUser.id)
            
            firestore.collection("groupOrders")
                .document(doc.id)
                .update(
                    mapOf(
                        "members" to members,
                        "memberIds" to updatedMemberIds
                    )
                )
                .await()
            
            // Get updated order
            val updatedDoc = firestore.collection("groupOrders")
                .document(doc.id)
                .get()
                .await()
            
            val groupOrder = parseGroupOrder(updatedDoc.id, updatedDoc.data)
                ?: throw Exception("Failed to parse group order")
            
            _currentGroupOrder.value = groupOrder
            listenToCurrentOrder(doc.id)
            
            Result.success(groupOrder)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun addItemsToGroupOrder(orderId: String, cartItems: List<CartItem>): Result<Unit> {
        return try {
            val currentUser = authRepository.getCurrentUser()
                ?: throw Exception("User not authenticated")
            
            val doc = firestore.collection("groupOrders")
                .document(orderId)
                .get()
                .await()
            
            if (!doc.exists()) {
                throw Exception("Group order not found")
            }
            
            val orderData = doc.data ?: throw Exception("Order data not found")
            val members = (orderData["members"] as? List<Map<String, Any>>)?.toMutableList() ?: mutableListOf()
            
            // Find current user's member entry
            val memberIndex = members.indexOfFirst { it["userId"] == currentUser.id }
            if (memberIndex == -1) {
                throw Exception("You are not a member of this group order")
            }
            
            // Convert cart items to Firestore format
            val cartItemsData = cartItems.map { item ->
                hashMapOf(
                    "menuItemId" to item.menuItem.id,
                    "menuItemName" to item.menuItem.name,
                    "category" to item.menuItem.category,
                    "quantity" to item.quantity,
                    "basePrice" to item.menuItem.price,
                    "customization" to item.customization?.let {
                        hashMapOf(
                            "flavor" to it.flavor.name,
                            "dippingSauce" to it.dippingSauce.name,
                            "drink" to it.drink.name
                        )
                    },
                    "subtotal" to item.subtotal
                )
            }
            
            val memberTotal = cartItems.sumOf { it.subtotal }
            
            // Update member's cart items
            val updatedMember = members[memberIndex].toMutableMap()
            updatedMember["cartItems"] = cartItemsData
            updatedMember["memberTotal"] = memberTotal
            members[memberIndex] = updatedMember
            
            // Calculate new total
            val totalAmount = members.sumOf { 
                (it["memberTotal"] as? Number)?.toDouble() ?: 0.0
            }
            
            firestore.collection("groupOrders")
                .document(orderId)
                .update(
                    mapOf(
                        "members" to members,
                        "totalAmount" to totalAmount
                    )
                )
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun leaveGroupOrder(orderId: String): Result<Unit> {
        return try {
            val currentUser = authRepository.getCurrentUser()
                ?: throw Exception("User not authenticated")
            
            val doc = firestore.collection("groupOrders")
                .document(orderId)
                .get()
                .await()
            
            if (!doc.exists()) {
                throw Exception("Group order not found")
            }
            
            val orderData = doc.data ?: throw Exception("Order data not found")
            
            // Check if user is host
            if (orderData["hostUserId"] == currentUser.id) {
                throw Exception("Host cannot leave. Please cancel the order instead.")
            }
            
            val members = (orderData["members"] as? List<Map<String, Any>>)?.toMutableList() ?: mutableListOf()
            val memberIds = (orderData["memberIds"] as? List<String>)?.toMutableList() ?: mutableListOf()
            
            members.removeAll { it["userId"] == currentUser.id }
            memberIds.remove(currentUser.id)
            
            // Recalculate total
            val totalAmount = members.sumOf { 
                (it["memberTotal"] as? Number)?.toDouble() ?: 0.0
            }
            
            firestore.collection("groupOrders")
                .document(orderId)
                .update(
                    mapOf(
                        "members" to members,
                        "memberIds" to memberIds,
                        "totalAmount" to totalAmount
                    )
                )
                .await()
            
            _currentGroupOrder.value = null
            stopCurrentOrderListener()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun listenToCurrentOrder(orderId: String) {
        currentOrderListener?.remove()
        currentOrderListener = firestore.collection("groupOrders")
            .document(orderId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                
                val groupOrder = parseGroupOrder(snapshot?.id ?: "", snapshot?.data)
                _currentGroupOrder.value = groupOrder
            }
    }
    
    private fun stopCurrentOrderListener() {
        currentOrderListener?.remove()
        currentOrderListener = null
    }
    
    fun stopListening() {
        ordersListener?.remove()
        currentOrderListener?.remove()
        ordersListener = null
        currentOrderListener = null
    }
}
