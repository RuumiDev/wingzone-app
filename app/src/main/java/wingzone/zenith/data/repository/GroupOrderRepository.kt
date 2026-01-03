package wingzone.zenith.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import wingzone.zenith.data.models.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import kotlin.random.Random

class GroupOrderRepository(private val authRepository: IAuthRepository) {
    private val firestore = FirebaseFirestore.getInstance()
    private val groupOrdersCollection = firestore.collection("groupOrders")
    
    private val _groupOrders = MutableStateFlow<List<GroupOrder>>(emptyList())
    val groupOrders: StateFlow<List<GroupOrder>> = _groupOrders.asStateFlow()
    
    // Helper function to serialize kitchen ingredients
    private fun serializeKitchenIngredients(kitchen: KitchenIngredients?): Any? {
        return kitchen?.let {
            hashMapOf(
                "ingredients" to it.ingredients.map { ingredient ->
                    hashMapOf(
                        "type" to ingredient.type,
                        "quantity" to ingredient.quantity,
                        "requiresSelection" to ingredient.requiresSelection
                    )
                }
            )
        }
    }
    
    private val _currentGroupOrder = MutableStateFlow<GroupOrder?>(null)
    val currentGroupOrder: StateFlow<GroupOrder?> = _currentGroupOrder.asStateFlow()
    
    // Local cache
    private val orders = mutableMapOf<String, GroupOrder>() // code -> order
    
    fun generateOrderCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return "WZ${(1..4).map { chars.random() }.joinToString("")}"
    }
    
    suspend fun createGroupOrder(
        deliveryAddress: String? = null,
        specialInstructions: String? = null
    ): Result<GroupOrder> {
        delay(500) // Simulate network delay
        
        val currentUser = authRepository.getCurrentUser()
            ?: return Result.failure(Exception("User not authenticated"))
        
        val code = generateOrderCode()
        val expiresAt = Calendar.getInstance().apply {
            add(Calendar.HOUR, 2) // Expires in 2 hours
        }.time
        
        val host = GroupMember(
            userId = currentUser.id,
            name = currentUser.name,
            profileImageUrl = currentUser.profileImageUrl,
            isHost = true
        )
        
        val groupOrder = GroupOrder(
            code = code,
            hostId = currentUser.id,
            members = listOf(host),
            expiresAt = expiresAt,
            deliveryAddress = deliveryAddress,
            specialInstructions = specialInstructions
        )
        
        try {
            // Save to Firebase
            val orderData = hashMapOf(
                "code" to groupOrder.code,
                "hostId" to groupOrder.hostId,
                "members" to groupOrder.members.map { member ->
                    hashMapOf(
                        "userId" to member.userId,
                        "name" to member.name,
                        "profileImageUrl" to (member.profileImageUrl ?: ""),
                        "isHost" to member.isHost,
                        "hasPaid" to member.hasPaid,
                        "cartItems" to member.cartItems.map { item ->
                            hashMapOf(
                                "menuItemId" to item.menuItem.id,
                                "menuItemName" to item.menuItem.name,
                                "quantity" to item.quantity,
                                "price" to item.menuItem.price,
                                "kitchenIngredients" to serializeKitchenIngredients(item.menuItem.kitchenIngredients),
                                "customization" to item.customization
                            )
                        }
                    )
                },
                "status" to groupOrder.status.name,
                "maxMembers" to groupOrder.maxMembers,
                "createdAt" to Timestamp(groupOrder.createdAt),
                "expiresAt" to Timestamp(groupOrder.expiresAt),
                "deliveryAddress" to (groupOrder.deliveryAddress ?: ""),
                "specialInstructions" to (groupOrder.specialInstructions ?: "")
            )
            
            groupOrdersCollection.document(code).set(orderData).await()
            
            orders[code] = groupOrder
            _currentGroupOrder.value = groupOrder
            updateGroupOrdersList()
            
            return Result.success(groupOrder)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
    
    suspend fun joinGroupOrder(code: String): Result<GroupOrder> {
        try {
            val currentUser = authRepository.getCurrentUser()
                ?: return Result.failure(Exception("User not authenticated"))
            
            // Fetch from Firebase
            val docSnapshot = groupOrdersCollection.document(code.uppercase()).get().await()
            
            if (!docSnapshot.exists()) {
                return Result.failure(Exception("Group order not found"))
            }
            
            val order = parseGroupOrderFromFirebase(docSnapshot.data ?: emptyMap())
                ?: return Result.failure(Exception("Invalid group order data"))
            
            if (order.isFull) {
                return Result.failure(Exception("Group order is full"))
            }
            
            if (order.isExpired) {
                return Result.failure(Exception("Group order has expired"))
            }
            
            if (order.members.any { it.userId == currentUser.id }) {
                return Result.failure(Exception("Already in this group order"))
            }
            
            val newMember = GroupMember(
                userId = currentUser.id,
                name = currentUser.name,
                profileImageUrl = currentUser.profileImageUrl,
                isHost = false
            )
            
            val updatedMembers = order.members + newMember
            val newStatus = if (updatedMembers.size >= order.maxMembers) GroupOrderStatus.FULL else order.status
            
            // Update Firebase
            groupOrdersCollection.document(code.uppercase()).update(
                mapOf(
                    "members" to updatedMembers.map { member ->
                        hashMapOf(
                            "userId" to member.userId,
                            "name" to member.name,
                            "profileImageUrl" to (member.profileImageUrl ?: ""),
                            "isHost" to member.isHost,
                            "hasPaid" to member.hasPaid,
                            "cartItems" to member.cartItems.map { item ->
                                hashMapOf(
                                    "menuItemId" to item.menuItem.id,
                                    "menuItemName" to item.menuItem.name,
                                    "quantity" to item.quantity,
                                    "price" to item.menuItem.price,
                                    "kitchenIngredients" to serializeKitchenIngredients(item.menuItem.kitchenIngredients),
                                    "customization" to item.customization
                                )
                            }
                        )
                    },
                    "status" to newStatus.name
                )
            ).await()
            
            val updatedOrder = order.copy(members = updatedMembers, status = newStatus)
            orders[code.uppercase()] = updatedOrder
            _currentGroupOrder.value = updatedOrder
            updateGroupOrdersList()
            
            return Result.success(updatedOrder)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
    
    private fun parseGroupOrderFromFirebase(data: Map<String, Any>): GroupOrder? {
        return try {
            val membersList = (data["members"] as? List<*>)?.mapNotNull memberLoop@{ memberData ->
                val member = memberData as? Map<*, *> ?: return@memberLoop null
                
                // Parse cart items
                val cartItems = (member["cartItems"] as? List<*>)?.mapNotNull itemLoop@{ itemData ->
                    val item = itemData as? Map<*, *> ?: return@itemLoop null
                    CartItem(
                        menuItem = MenuItem(
                            id = item["menuItemId"] as? String ?: return@itemLoop null,
                            name = item["menuItemName"] as? String ?: return@itemLoop null,
                            price = (item["price"] as? Number)?.toDouble() ?: 0.0,
                            description = "",
                            category = "entree",
                            imageUrl = "",
                            isAvailable = true,
                            requiresCustomization = false
                        ),
                        quantity = (item["quantity"] as? Long)?.toInt() ?: 1,
                        customization = null
                    )
                } ?: emptyList()
                
                GroupMember(
                    userId = member["userId"] as? String ?: return@memberLoop null,
                    name = member["name"] as? String ?: return@memberLoop null,
                    profileImageUrl = member["profileImageUrl"] as? String,
                    isHost = member["isHost"] as? Boolean ?: false,
                    hasPaid = member["hasPaid"] as? Boolean ?: false,
                    cartItems = cartItems
                )
            } ?: emptyList()
            
            GroupOrder(
                id = data["code"] as? String ?: return null,
                code = data["code"] as? String ?: return null,
                hostId = data["hostId"] as? String ?: return null,
                members = membersList,
                status = GroupOrderStatus.valueOf(data["status"] as? String ?: "OPEN"),
                maxMembers = (data["maxMembers"] as? Long)?.toInt() ?: 5,
                createdAt = (data["createdAt"] as? Timestamp)?.toDate() ?: Date(),
                expiresAt = (data["expiresAt"] as? Timestamp)?.toDate() ?: Date(),
                deliveryAddress = data["deliveryAddress"] as? String,
                specialInstructions = data["specialInstructions"] as? String
            )
        } catch (e: Exception) {
            null
        }
    }
    
    fun startListeningToGroupOrder(code: String, onUpdate: (GroupOrder?) -> Unit) {
        groupOrdersCollection.document(code.uppercase())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onUpdate(null)
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    val order = parseGroupOrderFromFirebase(snapshot.data ?: emptyMap())
                    if (order != null) {
                        orders[code.uppercase()] = order
                        _currentGroupOrder.value = order
                    }
                    onUpdate(order)
                } else {
                    onUpdate(null)
                }
            }
    }
    
    suspend fun leaveGroupOrder(orderId: String): Result<Unit> {
        try {
            val currentUser = authRepository.getCurrentUser()
                ?: return Result.failure(Exception("User not authenticated"))
            
            val order = orders.values.find { it.id == orderId }
                ?: return Result.failure(Exception("Group order not found"))
            
            if (order.hostId == currentUser.id) {
                // Host is leaving, delete the order from Firebase
                groupOrdersCollection.document(order.code).delete().await()
                orders.remove(order.code)
                if (_currentGroupOrder.value?.id == orderId) {
                    _currentGroupOrder.value = null
                }
            } else {
                // Regular member leaving - update Firebase
                val updatedMembers = order.members.filter { it.userId != currentUser.id }
                val newStatus = if (order.status == GroupOrderStatus.FULL && updatedMembers.size < order.maxMembers) {
                    GroupOrderStatus.OPEN
                } else order.status
                
                groupOrdersCollection.document(order.code).update(
                    mapOf(
                        "members" to updatedMembers.map { member ->
                            hashMapOf(
                                "userId" to member.userId,
                                "name" to member.name,
                                "profileImageUrl" to (member.profileImageUrl ?: ""),
                                "isHost" to member.isHost,
                                "hasPaid" to member.hasPaid,
                                "cartItems" to member.cartItems.map { item ->
                                    hashMapOf(
                                        "menuItemId" to item.menuItem.id,
                                        "menuItemName" to item.menuItem.name,
                                        "quantity" to item.quantity,
                                        "price" to item.menuItem.price,
                                "kitchenIngredients" to serializeKitchenIngredients(item.menuItem.kitchenIngredients),
                                        "customization" to item.customization
                                    )
                                }
                            )
                        },
                        "status" to newStatus.name
                    )
                ).await()
                
                val updatedOrder = order.copy(members = updatedMembers, status = newStatus)
                orders[order.code] = updatedOrder
                
                if (_currentGroupOrder.value?.id == orderId) {
                    _currentGroupOrder.value = null
                }
            }
            
            updateGroupOrdersList()
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
    
    suspend fun addItemToGroupOrder(orderId: String, cartItem: CartItem): Result<Unit> {
        try {
            val currentUser = authRepository.getCurrentUser()
                ?: return Result.failure(Exception("User not authenticated"))
            
            val order = orders.values.find { it.id == orderId }
                ?: return Result.failure(Exception("Group order not found"))
            
            val updatedMembers = order.members.map { member ->
                if (member.userId == currentUser.id) {
                    member.copy(cartItems = member.cartItems + cartItem)
                } else {
                    member
                }
            }
            
            // Update Firebase
            groupOrdersCollection.document(order.code).update(
                mapOf(
                    "members" to updatedMembers.map { member ->
                        hashMapOf(
                            "userId" to member.userId,
                            "name" to member.name,
                            "profileImageUrl" to (member.profileImageUrl ?: ""),
                            "isHost" to member.isHost,
                            "hasPaid" to member.hasPaid,
                            "cartItems" to member.cartItems.map { item ->
                                hashMapOf(
                                    "menuItemId" to item.menuItem.id,
                                    "menuItemName" to item.menuItem.name,
                                    "quantity" to item.quantity,
                                    "price" to item.menuItem.price,
                                    "kitchenIngredients" to serializeKitchenIngredients(item.menuItem.kitchenIngredients),
                                    "customization" to item.customization
                                )
                            }
                        )
                    }
                )
            ).await()
            
            val updatedOrder = order.copy(members = updatedMembers)
            orders[order.code] = updatedOrder
            
            if (_currentGroupOrder.value?.id == orderId) {
                _currentGroupOrder.value = updatedOrder
            }
            
            updateGroupOrdersList()
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
    
    suspend fun markMemberAsPaid(orderId: String, userId: String): Result<Unit> {
        delay(300)
        
        val order = orders.values.find { it.id == orderId }
            ?: return Result.failure(Exception("Group order not found"))
        
        val updatedMembers = order.members.map { member ->
            if (member.userId == userId) {
                member.copy(hasPaid = true)
            } else {
                member
            }
        }
        
        val updatedOrder = order.copy(members = updatedMembers)
        orders[order.code] = updatedOrder
        
        if (_currentGroupOrder.value?.id == orderId) {
            _currentGroupOrder.value = updatedOrder
        }
        
        updateGroupOrdersList()
        return Result.success(Unit)
    }
    
    suspend fun startOrdering(orderId: String): Result<Unit> {
        delay(300)
        
        val currentUser = authRepository.getCurrentUser()
            ?: return Result.failure(Exception("User not authenticated"))
        
        val order = orders.values.find { it.id == orderId }
            ?: return Result.failure(Exception("Group order not found"))
        
        if (order.hostId != currentUser.id) {
            return Result.failure(Exception("Only the host can start ordering"))
        }
        
        val updatedOrder = order.copy(status = GroupOrderStatus.ORDERING)
        orders[order.code] = updatedOrder
        
        if (_currentGroupOrder.value?.id == orderId) {
            _currentGroupOrder.value = updatedOrder
        }
        
        updateGroupOrdersList()
        return Result.success(Unit)
    }
    
    suspend fun finalizeGroupOrder(orderId: String): Result<Unit> {
        delay(500)
        
        val currentUser = authRepository.getCurrentUser()
            ?: return Result.failure(Exception("User not authenticated"))
        
        val order = orders.values.find { it.id == orderId }
            ?: return Result.failure(Exception("Group order not found"))
        
        if (order.hostId != currentUser.id) {
            return Result.failure(Exception("Only the host can finalize the order"))
        }
        
        if (!order.allMembersPaid) {
            return Result.failure(Exception("All members must pay before finalizing"))
        }
        
        val updatedOrder = order.copy(status = GroupOrderStatus.CONFIRMED)
        orders[order.code] = updatedOrder
        
        if (_currentGroupOrder.value?.id == orderId) {
            _currentGroupOrder.value = updatedOrder
        }
        
        updateGroupOrdersList()
        return Result.success(Unit)
    }
    
    suspend fun payForMember(orderId: String, memberId: String): Result<Unit> {
        delay(300)
        
        val currentUser = authRepository.getCurrentUser()
            ?: return Result.failure(Exception("User not authenticated"))
        
        val order = orders.values.find { it.id == orderId }
            ?: return Result.failure(Exception("Group order not found"))
        
        if (order.hostId != currentUser.id) {
            return Result.failure(Exception("Only the host can pay for other members"))
        }
        
        val updatedMembers = order.members.map { member ->
            if (member.userId == memberId) {
                member.copy(hasPaid = true)
            } else {
                member
            }
        }
        
        val updatedOrder = order.copy(members = updatedMembers)
        orders[order.code] = updatedOrder
        
        if (_currentGroupOrder.value?.id == orderId) {
            _currentGroupOrder.value = updatedOrder
        }
        
        updateGroupOrdersList()
        return Result.success(Unit)
    }
    
    suspend fun kickMember(orderId: String, memberId: String): Result<Unit> {
        delay(300)
        
        val currentUser = authRepository.getCurrentUser()
            ?: return Result.failure(Exception("User not authenticated"))
        
        val order = orders.values.find { it.id == orderId }
            ?: return Result.failure(Exception("Group order not found"))
        
        if (order.hostId != currentUser.id) {
            return Result.failure(Exception("Only the host can kick members"))
        }
        
        if (memberId == order.hostId) {
            return Result.failure(Exception("Host cannot kick themselves"))
        }
        
        val updatedMembers = order.members.filter { it.userId != memberId }
        
        if (updatedMembers.isEmpty()) {
            return Result.failure(Exception("Cannot remove the last member"))
        }
        
        val updatedOrder = order.copy(
            members = updatedMembers,
            status = if (order.status == GroupOrderStatus.FULL && updatedMembers.size < order.maxMembers) {
                GroupOrderStatus.ORDERING
            } else order.status
        )
        orders[order.code] = updatedOrder
        
        if (_currentGroupOrder.value?.id == orderId) {
            _currentGroupOrder.value = updatedOrder
        }
        
        updateGroupOrdersList()
        return Result.success(Unit)
    }
    
    suspend fun removeItemFromGroupOrder(orderId: String, userId: String, itemIndex: Int): Result<Unit> {
        try {
            val order = orders.values.find { it.id == orderId }
                ?: return Result.failure(Exception("Group order not found"))
            
            val updatedMembers = order.members.map { member ->
                if (member.userId == userId) {
                    val updatedItems = member.cartItems.filterIndexed { index, _ -> index != itemIndex }
                    member.copy(cartItems = updatedItems, hasPaid = false)
                } else {
                    member
                }
            }
            
            // Update Firebase
            groupOrdersCollection.document(order.code).update(
                mapOf(
                    "members" to updatedMembers.map { member ->
                        hashMapOf(
                            "userId" to member.userId,
                            "name" to member.name,
                            "profileImageUrl" to (member.profileImageUrl ?: ""),
                            "isHost" to member.isHost,
                            "hasPaid" to member.hasPaid,
                            "cartItems" to member.cartItems.map { item ->
                                hashMapOf(
                                    "menuItemId" to item.menuItem.id,
                                    "menuItemName" to item.menuItem.name,
                                    "quantity" to item.quantity,
                                    "price" to item.menuItem.price,
                                    "kitchenIngredients" to serializeKitchenIngredients(item.menuItem.kitchenIngredients),
                                    "customization" to item.customization
                                )
                            }
                        )
                    }
                )
            ).await()
            
            val updatedOrder = order.copy(members = updatedMembers)
            orders[order.code] = updatedOrder
            
            if (_currentGroupOrder.value?.id == orderId) {
                _currentGroupOrder.value = updatedOrder
            }
            
            updateGroupOrdersList()
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
    
    fun getUserGroupOrders(): List<GroupOrder> {
        val currentUser = authRepository.getCurrentUser() ?: return emptyList()
        return orders.values.filter { order ->
            order.members.any { it.userId == currentUser.id }
        }.sortedByDescending { it.createdAt }
    }
    
    private fun updateGroupOrdersList() {
        _groupOrders.value = orders.values.toList().sortedByDescending { it.createdAt }
    }
    
    fun getGroupOrder(code: String): GroupOrder? {
        return orders[code.uppercase()]
    }
    
    fun setCurrentGroupOrder(order: GroupOrder?) {
        _currentGroupOrder.value = order
    }
}
