package wingzone.zenith.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import wingzone.zenith.data.repository.IAuthRepository
import wingzone.zenith.data.repository.RepositoryProvider
import wingzone.zenith.ui.screens.Location
import wingzone.zenith.data.models.GroupOrderStatus
import java.util.*
import kotlin.random.Random

class LobbyViewModel(
    application: Application,
    private val authRepository: IAuthRepository = RepositoryProvider.getAuthRepository(),
    private val cartRepository: wingzone.zenith.data.repository.CartRepository = RepositoryProvider.getCartRepository(),
    private val groupOrderViewModel: GroupOrderViewModel? = null
) : AndroidViewModel(application) {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val prefs = application.getSharedPreferences("lobby_prefs", Context.MODE_PRIVATE)
    
    private val _locations = MutableStateFlow<List<Location>>(emptyList())
    val locations: StateFlow<List<Location>> = _locations.asStateFlow()
    
    private val _joinCode = MutableStateFlow("")
    val joinCode: StateFlow<String> = _joinCode.asStateFlow()
    
    private val _shouldShowDisclaimer = MutableStateFlow(true)
    val shouldShowDisclaimer: StateFlow<Boolean> = _shouldShowDisclaimer.asStateFlow()
    
    private val _currentLobbyId = MutableStateFlow<String?>(null)
    val currentLobbyId: StateFlow<String?> = _currentLobbyId.asStateFlow()
    
    init {
        checkDisclaimerStatus()
        startCartSyncListener()
    }
    
    // Continuously sync cart to lobby when changes occur
    private fun startCartSyncListener() {
        viewModelScope.launch {
            cartRepository.cart.collect { cart ->
                val lobbyId = _currentLobbyId.value
                if (lobbyId != null && cart.items.isNotEmpty()) {
                    syncCartToLobby(lobbyId)
                }
            }
        }
    }
    
    private fun checkDisclaimerStatus() {
        val acknowledged = prefs.getBoolean("lobby_disclaimer_acknowledged", false)
        _shouldShowDisclaimer.value = !acknowledged
    }
    
    fun setDisclaimerAcknowledged() {
        prefs.edit().putBoolean("lobby_disclaimer_acknowledged", true).apply()
        _shouldShowDisclaimer.value = false
    }
    
    fun loadLocations() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("restaurantLocations")
                    .whereEqualTo("active", true)
                    .get()
                    .await()
                
                val locationsList = snapshot.documents.mapNotNull { doc ->
                    Location(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        address = doc.getString("address") ?: "",
                        addressLine1 = doc.getString("addressLine1") ?: "",
                        addressLine2 = doc.getString("addressLine2") ?: "",
                        city = doc.getString("city") ?: ""
                    )
                }
                
                _locations.value = locationsList
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to hardcoded locations if Firestore fails
                _locations.value = getDefaultLocations()
            }
        }
    }
    
    private fun getDefaultLocations(): List<Location> {
        return listOf(
            Location(
                id = "wingzone-meru",
                name = "Wingzone Meru",
                address = "Lebuh Meru Raya, Bandar Meru Raya, Ipoh",
                addressLine1 = "Lebuh Meru Raya,",
                addressLine2 = "Bandar Meru Raya, Ipoh",
                city = "Ipoh"
            ),
            Location(
                id = "wingzone-greentown",
                name = "Wingzone GreenTown",
                address = "No. 2, Lorong Greentown 8, Greentown Business Centre, 30450 Ipoh, Perak",
                addressLine1 = "No. 2, Lorong Greentown 8,",
                addressLine2 = "Greentown Business Centre, 30450 Ipoh, Perak",
                city = "Ipoh"
            )
        )
    }
    
    fun createLobby(
        orderType: String,
        location: Location,
        paymentMethod: String,
        paymentType: String,
        onResult: (Result<String>) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val currentUser = authRepository.getCurrentUser()
                    ?: throw Exception("User not authenticated")
                
                // Generate unique 6-character code
                val code = generateLobbyCode()
                
                // Create lobby document
                val lobbyData = hashMapOf(
                    "code" to code,
                    "hostUserId" to currentUser.id,
                    "hostUserName" to currentUser.name,
                    "orderType" to orderType,
                    "location" to hashMapOf(
                        "id" to location.id,
                        "name" to location.name,
                        "address" to location.address,
                        "addressLine1" to location.addressLine1,
                        "addressLine2" to location.addressLine2,
                        "city" to location.city
                    ),
                    "paymentMethod" to paymentMethod,
                    "paymentType" to paymentType,
                    "members" to arrayListOf(
                        hashMapOf(
                            "userId" to currentUser.id,
                            "userName" to currentUser.name,
                            "joinedAt" to Date(),
                            "cartItems" to arrayListOf<Any>(),
                            "total" to 0.0,
                            "status" to "ordering",
                            "hasPaid" to false,
                            "isHost" to true
                        )
                    ),
                    "status" to "active",
                    "createdAt" to Date(),
                    "expiresAt" to Date(System.currentTimeMillis() + 3600000), // 1 hour
                    "maxMembers" to 10
                )
                
                val docRef = firestore.collection("lobbies")
                    .add(lobbyData)
                    .await()
                
                // Set current lobby and update GroupOrderViewModel
                _currentLobbyId.value = docRef.id
                loadLobbyAsGroupOrder(docRef.id)
                
                onResult(Result.success(docRef.id))
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(Result.failure(e))
            }
        }
    }
    
    fun joinLobby(code: String, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            try {
                val currentUser = authRepository.getCurrentUser()
                    ?: throw Exception("User not authenticated")
                
                // Find lobby by code
                val snapshot = firestore.collection("lobbies")
                    .whereEqualTo("code", code.uppercase())
                    .whereEqualTo("status", "active")
                    .get()
                    .await()
                
                if (snapshot.isEmpty) {
                    throw Exception("Lobby not found. Please check the code and try again.")
                }
                
                val lobbyDoc = snapshot.documents.first()
                val lobbyId = lobbyDoc.id
                
                // Check if lobby is expired
                val expiresAt = lobbyDoc.getDate("expiresAt")
                if (expiresAt != null && expiresAt.before(Date())) {
                    throw Exception("This lobby has expired. Ask the host to create a new one.")
                }
                
                // Check if already a member
                val members = lobbyDoc.get("members") as? List<Map<String, Any>> ?: emptyList()
                val alreadyMember = members.any { 
                    (it["userId"] as? String) == currentUser.id 
                }
                
                if (alreadyMember) {
                    // Already in lobby, sync cart to lobby again
                    syncCartToLobby(lobbyId)
                    onResult(Result.success(lobbyId))
                    return@launch
                }
                
                // Check if lobby is full
                val maxMembers = (lobbyDoc.getLong("maxMembers") ?: 10L).toInt()
                if (members.size >= maxMembers) {
                    throw Exception("This lobby is full (max $maxMembers members).")
                }
                
                // Sync current cart to lobby
                val currentCart = cartRepository.cart.value
                val serializedCartItems = currentCart.items.map { cartItem ->
                    hashMapOf<String, Any?>(
                        "id" to cartItem.id,
                        "menuItem" to hashMapOf(
                            "id" to cartItem.menuItem.id,
                            "name" to cartItem.menuItem.name,
                            "description" to cartItem.menuItem.description,
                            "price" to cartItem.menuItem.price,
                            "category" to cartItem.menuItem.category,
                            "imageUrl" to (cartItem.menuItem.imageUrl ?: ""),
                            "isAvailable" to cartItem.menuItem.isAvailable,
                            "requiresCustomization" to cartItem.menuItem.requiresCustomization,
                            "customizationOptions" to cartItem.menuItem.customizationOptions?.let { options ->
                                hashMapOf<String, Any?>(
                                    "requiresFlavor" to (options.requiresFlavor ?: false),
                                    "requiresBeverage" to (options.requiresBeverage ?: false),
                                    "requiresDippingSauce" to (options.requiresDippingSauce ?: false),
                                    "allowFriesExchange" to (options.allowFriesExchange ?: false),
                                    "requiresSaladChoice" to (options.requiresSaladChoice ?: false),
                                    "requiresBoneType" to (options.requiresBoneType ?: false)
                                )
                            }
                        ),
                        "quantity" to cartItem.quantity,
                        "customization" to if (cartItem.customization != null) {
                            hashMapOf<String, Any?>(
                                "flavor" to cartItem.customization.flavor.name,
                                "dippingSauce" to cartItem.customization.dippingSauce.name,
                                "drink" to cartItem.customization.drink.name,
                                "boneType" to cartItem.customization.boneType?.name,
                                "friesExchange" to cartItem.customization.friesExchange?.let { exchange ->
                                    hashMapOf(
                                        "name" to exchange.name,
                                        "regularPrice" to exchange.regularPrice,
                                        "jumboPrice" to exchange.jumboPrice,
                                        "selectedSize" to exchange.selectedSize,
                                        "selectedFlavor" to exchange.selectedFlavor
                                    )
                                },
                                "saladType" to cartItem.customization.saladType
                            )
                        } else null,
                        "specialInstructions" to (cartItem.specialInstructions ?: ""),
                        "subtotal" to cartItem.subtotal
                    )
                }
                
                val newMember = hashMapOf(
                    "userId" to currentUser.id,
                    "userName" to currentUser.name,
                    "joinedAt" to Date(),
                    "cartItems" to serializedCartItems,
                    "total" to currentCart.total,
                    "status" to "ordering",
                    "hasPaid" to false,
                    "isHost" to false
                )
                
                firestore.collection("lobbies").document(lobbyId)
                    .update("members", com.google.firebase.firestore.FieldValue.arrayUnion(newMember))
                    .await()
                
                // Set current lobby and update GroupOrderViewModel
                _currentLobbyId.value = lobbyId
                android.util.Log.d("LobbyViewModel", "joinLobby: Setting currentLobbyId to $lobbyId, calling loadLobbyAsGroupOrder")
                loadLobbyAsGroupOrder(lobbyId)
                
                onResult(Result.success(lobbyId))
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(Result.failure(e))
            }
        }
    }
    
    private fun generateLobbyCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6)
            .map { chars.random() }
            .joinToString("")
    }
    
    fun updateJoinCode(code: String) {
        _joinCode.value = code.take(6).uppercase()
    }
    
    // Fetch all lobbies where the current user is a member
    suspend fun getUserLobbies(): List<Map<String, Any>> {
        return try {
            val currentUser = authRepository.getCurrentUser() ?: return emptyList()
            
            val snapshot = firestore.collection("lobbies")
                .whereEqualTo("status", "active")
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                val members = doc.get("members") as? List<Map<String, Any>> ?: return@mapNotNull null
                val isMember = members.any { member ->
                    member["userId"] == currentUser.id
                }
                
                if (isMember) {
                    val data = doc.data?.toMutableMap() ?: mutableMapOf()
                    data["id"] = doc.id
                    data
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    // Helper function to load lobby and set as current group order
    private fun loadLobbyAsGroupOrder(lobbyId: String) {
        android.util.Log.d("LobbyViewModel", "loadLobbyAsGroupOrder called for lobbyId: $lobbyId, groupOrderViewModel is null: ${groupOrderViewModel == null}")
        viewModelScope.launch {
            try {
                val lobbyDoc = firestore.collection("lobbies").document(lobbyId).get().await()
                if (lobbyDoc.exists()) {
                    val code = lobbyDoc.getString("code") ?: ""
                    val hostUserId = lobbyDoc.getString("hostUserId") ?: ""
                    val paymentMethod = lobbyDoc.getString("paymentMethod") ?: "individual"
                    val expiresAtTimestamp = lobbyDoc.getTimestamp("expiresAt")
                    val expiresAt = expiresAtTimestamp?.toDate() ?: Date(System.currentTimeMillis() + 3600000) // 1 hour default
                    
                    android.util.Log.d("LobbyViewModel", "Creating GroupOrder: id=$lobbyId, code=$code, paymentMethod=$paymentMethod")
                    
                    // Create a GroupOrder from lobby data
                    val groupOrder = wingzone.zenith.data.models.GroupOrder(
                        id = lobbyId,
                        code = code,
                        hostId = hostUserId,
                        status = GroupOrderStatus.OPEN,
                        members = emptyList(), // Will be populated by repository
                        createdAt = Date(),
                        expiresAt = expiresAt,
                        paymentMethod = paymentMethod
                    )
                    
                    android.util.Log.d("LobbyViewModel", "Calling setCurrentGroupOrder with groupOrder.id=${groupOrder.id}")
                    groupOrderViewModel?.setCurrentGroupOrder(groupOrder)
                    android.util.Log.d("LobbyViewModel", "setCurrentGroupOrder completed")
                } else {
                    android.util.Log.w("LobbyViewModel", "Lobby document does not exist for id: $lobbyId")
                }
            } catch (e: Exception) {
                android.util.Log.e("LobbyViewModel", "Failed to load lobby as group order", e)
            }
        }
    }
    
    // Helper function to sync current cart to lobby
    private suspend fun syncCartToLobby(lobbyId: String) {
        try {
            val currentUser = authRepository.getCurrentUser() ?: return
            val currentCart = cartRepository.cart.value
            
            val serializedCartItems = currentCart.items.map { cartItem ->
                hashMapOf<String, Any?>(
                    "id" to cartItem.id,
                    "menuItem" to hashMapOf(
                        "id" to cartItem.menuItem.id,
                        "name" to cartItem.menuItem.name,
                        "description" to cartItem.menuItem.description,
                        "price" to cartItem.menuItem.price,
                        "category" to cartItem.menuItem.category,
                        "imageUrl" to (cartItem.menuItem.imageUrl ?: ""),
                        "isAvailable" to cartItem.menuItem.isAvailable,
                        "requiresCustomization" to cartItem.menuItem.requiresCustomization,
                        "customizationOptions" to cartItem.menuItem.customizationOptions?.let { options ->
                            hashMapOf<String, Any?>(
                                "requiresFlavor" to (options.requiresFlavor ?: false),
                                "requiresBeverage" to (options.requiresBeverage ?: false),
                                "requiresDippingSauce" to (options.requiresDippingSauce ?: false),
                                "allowFriesExchange" to (options.allowFriesExchange ?: false),
                                "requiresSaladChoice" to (options.requiresSaladChoice ?: false),
                                "requiresBoneType" to (options.requiresBoneType ?: false)
                            )
                        }
                    ),
                    "quantity" to cartItem.quantity,
                    "customization" to if (cartItem.customization != null) {
                        hashMapOf(
                            "flavor" to cartItem.customization!!.flavor.displayName,
                            "dippingSauce" to cartItem.customization!!.dippingSauce.displayName,
                            "drink" to cartItem.customization!!.drink.displayName,
                            "boneType" to cartItem.customization!!.boneType?.name,
                            "friesExchange" to if (cartItem.customization!!.friesExchange != null) {
                                hashMapOf(
                                    "name" to cartItem.customization!!.friesExchange!!.name,
                                    "selectedSize" to cartItem.customization!!.friesExchange!!.selectedSize,
                                    "selectedFlavor" to (cartItem.customization!!.friesExchange!!.selectedFlavor ?: "")
                                )
                            } else null
                        )
                    } else null,
                    "subtotal" to cartItem.subtotal
                )
            }
            
            // Update member's cart in lobby
            val lobbyDoc = firestore.collection("lobbies").document(lobbyId).get().await()
            val members = lobbyDoc.get("members") as? MutableList<MutableMap<String, Any>> ?: mutableListOf()
            val memberIndex = members.indexOfFirst { it["userId"] == currentUser.id }
            
            if (memberIndex >= 0) {
                members[memberIndex]["cartItems"] = serializedCartItems
                members[memberIndex]["total"] = currentCart.total
                
                firestore.collection("lobbies").document(lobbyId)
                    .update("members", members)
                    .await()
            }
        } catch (e: Exception) {
            android.util.Log.e("LobbyViewModel", "Failed to sync cart to lobby", e)
        }
    }
    
    // Mark user as paid in lobby
    fun markAsPaid(lobbyId: String, userId: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            try {
                val lobbyDoc = firestore.collection("lobbies").document(lobbyId).get().await()
                if (!lobbyDoc.exists()) throw Exception("Lobby not found")
                
                @Suppress("UNCHECKED_CAST")
                val members = lobbyDoc.get("members") as? List<Map<String, Any>> ?: emptyList()
                val updatedMembers = members.map { member ->
                    if (member["userId"] == userId) {
                        member.toMutableMap().apply {
                            put("hasPaid", true)
                            put("status", "paid")
                        }
                    } else {
                        member
                    }
                }
                
                firestore.collection("lobbies").document(lobbyId)
                    .update("members", updatedMembers)
                    .await()
                
                onResult(Result.success(Unit))
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(Result.failure(e))
            }
        }
    }
    
    // Submit order (host only, when all paid or host-pays-all)
    fun submitOrder(lobbyId: String, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            try {
                val lobbyDoc = firestore.collection("lobbies").document(lobbyId).get().await()
                if (!lobbyDoc.exists()) throw Exception("Lobby not found")
                
                val lobbyData = lobbyDoc.data ?: throw Exception("Invalid lobby data")
                val currentUser = authRepository.getCurrentUser() ?: throw Exception("Not authenticated")
                
                // Verify user is host
                if (lobbyData["hostUserId"] != currentUser.id) {
                    throw Exception("Only the host can submit the order")
                }
                
                val paymentMethod = lobbyData["paymentMethod"] as? String ?: "individual"
                val paymentType = lobbyData["paymentType"] as? String ?: "cash"
                @Suppress("UNCHECKED_CAST")
                val members = lobbyData["members"] as? List<Map<String, Any>> ?: emptyList()
                
                // Check if all members have paid (except for host-pays-all)
                if (paymentMethod != "host-pays-all") {
                    val allPaid = members.all { (it["hasPaid"] as? Boolean) ?: false }
                    if (!allPaid) {
                        throw Exception("All members must pay before submitting the order")
                    }
                }
                
                // Calculate group total
                val groupTotal = members.sumOf { (it["total"] as? Number)?.toDouble() ?: 0.0 }
                val totalItems = members.sumOf { member ->
                    val cartItems = member["cartItems"] as? List<*>
                    cartItems?.sumOf { item ->
                        ((item as? Map<String, Any>)?.get("quantity") as? Number)?.toInt() ?: 0
                    } ?: 0
                }
                
                // Create main group order in orders collection
                val groupOrderData = hashMapOf(
                    "isGroupOrder" to true,
                    "lobbyId" to lobbyId,
                    "code" to lobbyData["code"],
                    "orderType" to (lobbyData["orderType"] ?: "pickup"),
                    "location" to lobbyData["location"],
                    "paymentMethod" to paymentMethod,
                    "paymentType" to paymentType,
                    "hostUserId" to lobbyData["hostUserId"],
                    "hostUserName" to lobbyData["hostUserName"],
                    "members" to members,
                    "memberCount" to members.size,
                    "groupTotal" to groupTotal,
                    "subtotal" to groupTotal * 0.94, // Approximate subtotal (6% tax)
                    "tax" to groupTotal * 0.06,
                    "total" to groupTotal,
                    "totalItems" to totalItems,
                    "status" to "pending",
                    "paymentStatus" to "paid",
                    "createdAt" to Date(),
                    "estimatedTime" to 30, // 30 minutes default
                    "userId" to currentUser.id,
                    "userName" to currentUser.name
                )
                
                val groupOrderRef = firestore.collection("orders").add(groupOrderData).await()
                
                // Clear carts for all members BEFORE deleting lobby
                members.forEach { member ->
                    val userId = member["userId"] as? String
                    if (userId != null) {
                        try {
                            firestore.collection("carts").document(userId)
                                .set(hashMapOf(
                                    "items" to emptyList<Any>(),
                                    "lastUpdated" to Date()
                                ))
                                .await()
                        } catch (e: Exception) {
                            android.util.Log.e("LobbyViewModel", "Failed to clear cart for user $userId", e)
                        }
                    }
                }
                
                // Delete the lobby after successful order submission
                // This removes it for all members (host and non-host)
                firestore.collection("lobbies").document(lobbyId)
                    .delete()
                    .await()
                
                // Clear local state
                _currentLobbyId.value = null
                groupOrderViewModel?.clearCurrentGroupOrder()
                
                // Delete the lobby after a short delay (to allow final reads)
                viewModelScope.launch {
                    kotlinx.coroutines.delay(2000) // 2 second delay
                    try {
                        firestore.collection("lobbies").document(lobbyId).delete().await()
                        android.util.Log.d("LobbyViewModel", "Lobby $lobbyId deleted successfully")
                    } catch (e: Exception) {
                        android.util.Log.e("LobbyViewModel", "Failed to delete lobby $lobbyId", e)
                    }
                }
                
                onResult(Result.success(groupOrderRef.id))
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(Result.failure(e))
            }
        }
    }
    
    // Clear current lobby (when lobby doesn't exist or user leaves)
    fun clearCurrentLobby() {
        android.util.Log.d("LobbyViewModel", "Clearing current lobby state")
        _currentLobbyId.value = null
        groupOrderViewModel?.clearCurrentGroupOrder()
    }
}
