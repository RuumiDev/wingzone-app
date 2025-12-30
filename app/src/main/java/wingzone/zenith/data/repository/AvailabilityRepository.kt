package wingzone.zenith.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AvailabilitySettings(
    val flavors: List<String> = emptyList(),
    val beverages: List<String> = emptyList(),
    val sides: List<String> = emptyList(),
    val dippingSauces: List<String> = emptyList()
)

class AvailabilityRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private var availabilityListener: ListenerRegistration? = null
    
    private val _availability = MutableStateFlow(AvailabilitySettings())
    val availability: StateFlow<AvailabilitySettings> = _availability.asStateFlow()
    
    init {
        startListening()
    }
    
    private fun startListening() {
        availabilityListener?.remove()
        availabilityListener = firestore.collection("settings")
            .document("availability")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("AvailabilityRepository", "Error listening to availability", error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    try {
                        val flavors = (snapshot.get("flavors") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                        val beverages = (snapshot.get("beverages") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                        val sides = (snapshot.get("sides") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                        val dippingSauces = (snapshot.get("dippingSauces") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                        
                        _availability.value = AvailabilitySettings(
                            flavors = flavors,
                            beverages = beverages,
                            sides = sides,
                            dippingSauces = dippingSauces
                        )
                        
                        android.util.Log.d("AvailabilityRepository", "Updated availability: ${_availability.value}")
                    } catch (e: Exception) {
                        android.util.Log.e("AvailabilityRepository", "Error parsing availability", e)
                    }
                } else {
                    android.util.Log.w("AvailabilityRepository", "Availability document does not exist")
                }
            }
    }
    
    fun isFlavorsAvailable(flavor: String): Boolean {
        return _availability.value.flavors.contains(flavor)
    }
    
    fun isBeverageAvailable(beverage: String): Boolean {
        return _availability.value.beverages.contains(beverage)
    }
    
    fun isSideAvailable(side: String): Boolean {
        return _availability.value.sides.contains(side)
    }
    
    fun isDippingSauceAvailable(sauce: String): Boolean {
        return _availability.value.dippingSauces.contains(sauce)
    }
    
    fun cleanup() {
        availabilityListener?.remove()
    }
}
