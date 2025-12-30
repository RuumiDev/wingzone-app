package wingzone.zenith.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import wingzone.zenith.data.models.MenuItem
import wingzone.zenith.data.models.CustomizationOptions
import wingzone.zenith.data.models.FriesExchangeOption

class FirebaseMenuRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val menuCollection = firestore.collection("menuItems")
    private var menuListener: ListenerRegistration? = null
    
    private fun parseCustomizationOptions(data: Any?, flavors: Any?): CustomizationOptions? {
        if (data !is Map<*, *>) return null
        
        try {
            val friesExchanges = (data["friesExchanges"] as? List<*>)?.mapNotNull { item ->
                if (item is Map<*, *>) {
                    FriesExchangeOption(
                        name = item["name"] as? String ?: return@mapNotNull null,
                        regularPrice = (item["regularPrice"] as? Number)?.toDouble() ?: 0.0,
                        jumboPrice = (item["jumboPrice"] as? Number)?.toDouble()
                    )
                } else null
            } ?: emptyList()
            
            // Parse available flavors from document root
            val availableFlavors = (flavors as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            
            // Parse beverages and dipping sauces from customizationOptions
            val availableBeverages = (data["beverages"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            val availableDippingSauces = (data["dippingSauces"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            
            return CustomizationOptions(
                requiresFlavor = data["requiresFlavor"] as? Boolean ?: false,
                requiresBeverage = data["requiresBeverage"] as? Boolean ?: false,
                requiresDippingSauce = data["requiresDippingSauce"] as? Boolean ?: false,
                allowFriesExchange = data["allowFriesExchange"] as? Boolean ?: false,
                availableFlavors = availableFlavors,
                availableBeverages = availableBeverages,
                availableDippingSauces = availableDippingSauces,
                friesExchanges = friesExchanges
            )
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * Fetch all menu items from Firestore
     */
    suspend fun getMenuItems(): List<MenuItem> {
        return try {
            val snapshot = menuCollection
                .whereEqualTo("isAvailable", true)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    MenuItem(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        description = doc.getString("description") ?: "",
                        price = doc.getDouble("price") ?: 0.0,
                        category = doc.getString("category") ?: "",
                        imageUrl = doc.getString("imageUrl"),
                        requiresCustomization = doc.getBoolean("requiresCustomization") ?: false,
                        customizationOptions = parseCustomizationOptions(doc.get("customizationOptions"), doc.get("flavors"))
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
     * Real-time listener for menu items
     */
    fun listenToMenuItems(): Flow<List<MenuItem>> = callbackFlow {
        menuListener = menuCollection
            .whereEqualTo("isAvailable", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val items = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        MenuItem(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            description = doc.getString("description") ?: "",
                            price = doc.getDouble("price") ?: 0.0,
                            category = doc.getString("category") ?: "",
                            imageUrl = doc.getString("imageUrl"),
                            requiresCustomization = doc.getBoolean("requiresCustomization") ?: false,
                            customizationOptions = parseCustomizationOptions(doc.get("customizationOptions"), doc.get("flavors"))
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                
                trySend(items)
            }
        
        awaitClose { menuListener?.remove() }
    }
    
    /**
     * Get menu items by category
     */
    suspend fun getMenuItemsByCategory(category: String): List<MenuItem> {
        return try {
            val snapshot = menuCollection
                .whereEqualTo("category", category)
                .whereEqualTo("isAvailable", true)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    MenuItem(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        description = doc.getString("description") ?: "",
                        price = doc.getDouble("price") ?: 0.0,
                        category = doc.getString("category") ?: "",
                        imageUrl = doc.getString("imageUrl"),
                        requiresCustomization = doc.getBoolean("requiresCustomization") ?: false,
                        customizationOptions = parseCustomizationOptions(doc.get("customizationOptions"), doc.get("flavors"))
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
     * Stop listening to menu updates 
     */
    fun stopListening() {
        menuListener?.remove()
        menuListener = null
    }
}
