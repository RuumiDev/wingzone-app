package wingzone.zenith.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import wingzone.zenith.data.models.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseCartRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    private val _cart = MutableStateFlow(Cart())
    val cart: StateFlow<Cart> = _cart.asStateFlow()
    
    private var cartListener: ListenerRegistration? = null
    private var settingsListener: ListenerRegistration? = null
    private var currentTaxRate = 0.0 // No tax
    
    init {
        // Listen to cart changes and settings from Firestore
        loadAppSettings()
        startCartListener()
    }
    
    private fun loadAppSettings() {
        settingsListener?.remove()
        settingsListener = firestore.collection("appSettings")
            .document("general")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseCartRepository", "Error loading app settings: ${error.message}")
                    return@addSnapshotListener
                }
                
                val taxRate = (snapshot?.get("taxRate") as? Number)?.toDouble() ?: 0.0
                android.util.Log.d("FirebaseCartRepository", "Loaded tax rate from Firebase: $taxRate (no tax)")
                currentTaxRate = taxRate
                
                // Update existing cart with new tax rate
                val currentCart = _cart.value
                _cart.value = currentCart.copy(taxRate = taxRate)
                android.util.Log.d("FirebaseCartRepository", "Updated cart with new tax rate. Cart tax: ${currentCart.copy(taxRate = taxRate).tax}")
            }
    }
    
    private fun startCartListener() {
        val userId = auth.currentUser?.uid ?: return
        
        cartListener?.remove()
        cartListener = firestore.collection("cartItems")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                
                val items = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val menuItemData = doc.get("menuItem") as? Map<*, *>
                        val customizationData = doc.get("customization") as? Map<*, *>
                        
                        val menuItem = MenuItem(
                            id = menuItemData?.get("id") as? String ?: "",
                            name = menuItemData?.get("name") as? String ?: "",
                            description = menuItemData?.get("description") as? String ?: "",
                            price = (menuItemData?.get("price") as? Number)?.toDouble() ?: 0.0,
                            category = menuItemData?.get("category") as? String ?: "",
                            imageUrl = menuItemData?.get("imageUrl") as? String,
                            requiresCustomization = menuItemData?.get("requiresCustomization") as? Boolean ?: false
                        )
                        // The result
                        val customization = if (customizationData != null) {
                            EntreeCustomization(
                                flavor = Flavor.valueOf(customizationData["flavor"] as? String ?: "PLAIN"),
                                dippingSauce = DippingSauce.valueOf(customizationData["dippingSauce"] as? String ?: "NONE"),
                                drink = Drink.valueOf(customizationData["drink"] as? String ?: "NONE")
                            )
                        } else null
                        
                        CartItem(
                            id = doc.id,
                            menuItem = menuItem,
                            quantity = (doc.getLong("quantity") ?: 1).toInt(),
                            customization = customization,
                            specialInstructions = doc.getString("specialInstructions")
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                
                _cart.value = Cart(items = items, taxRate = currentTaxRate)
            }
    }
    
    suspend fun addItem(cartItem: CartItem): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
            
            val cartItemData = hashMapOf(
                "userId" to userId,
                "menuItem" to hashMapOf(
                    "id" to cartItem.menuItem.id,
                    "name" to cartItem.menuItem.name,
                    "description" to cartItem.menuItem.description,
                    "price" to cartItem.menuItem.price,
                    "category" to cartItem.menuItem.category,
                    "imageUrl" to cartItem.menuItem.imageUrl,
                    "requiresCustomization" to cartItem.menuItem.requiresCustomization
                ),
                "quantity" to cartItem.quantity,
                "customization" to cartItem.customization?.let {
                    hashMapOf(
                        "flavor" to it.flavor.name,
                        "dippingSauce" to it.dippingSauce.name,
                        "drink" to it.drink.name
                    )
                },
                "specialInstructions" to cartItem.specialInstructions
            )
            
            firestore.collection("cartItems")
                .add(cartItemData)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun removeItem(itemId: String): Result<Unit> {
        return try {
            firestore.collection("cartItems")
                .document(itemId)
                .delete()
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateItemQuantity(itemId: String, newQuantity: Int): Result<Unit> {
        return try {
            if (newQuantity <= 0) {
                return removeItem(itemId)
            }
            
            firestore.collection("cartItems")
                .document(itemId)
                .update("quantity", newQuantity)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun clearCart(): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
            
            val cartItems = firestore.collection("cartItems")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            val batch = firestore.batch()
            cartItems.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun stopListening() {
        cartListener?.remove()
        cartListener = null
    }
}
