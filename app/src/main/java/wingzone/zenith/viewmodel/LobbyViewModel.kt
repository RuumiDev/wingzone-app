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
import java.util.*
import kotlin.random.Random

class LobbyViewModel(
    application: Application,
    private val authRepository: IAuthRepository = RepositoryProvider.getAuthRepository()
) : AndroidViewModel(application) {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val prefs = application.getSharedPreferences("lobby_prefs", Context.MODE_PRIVATE)
    
    private val _locations = MutableStateFlow<List<Location>>(emptyList())
    val locations: StateFlow<List<Location>> = _locations.asStateFlow()
    
    private val _joinCode = MutableStateFlow("")
    val joinCode: StateFlow<String> = _joinCode.asStateFlow()
    
    private val _shouldShowDisclaimer = MutableStateFlow(true)
    val shouldShowDisclaimer: StateFlow<Boolean> = _shouldShowDisclaimer.asStateFlow()
    
    init {
        checkDisclaimerStatus()
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
                    "members" to arrayListOf(
                        hashMapOf(
                            "userId" to currentUser.id,
                            "userName" to currentUser.name,
                            "joinedAt" to Date(),
                            "cartItems" to arrayListOf<Any>(),
                            "total" to 0.0,
                            "status" to "ordering",
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
                    // Already in lobby, just navigate there
                    onResult(Result.success(lobbyId))
                    return@launch
                }
                
                // Check if lobby is full
                val maxMembers = (lobbyDoc.getLong("maxMembers") ?: 10L).toInt()
                if (members.size >= maxMembers) {
                    throw Exception("This lobby is full (max $maxMembers members).")
                }
                
                // Add user to members
                val newMember = hashMapOf(
                    "userId" to currentUser.id,
                    "userName" to currentUser.name,
                    "joinedAt" to Date(),
                    "cartItems" to arrayListOf<Any>(),
                    "total" to 0.0,
                    "status" to "ordering",
                    "isHost" to false
                )
                
                firestore.collection("lobbies").document(lobbyId)
                    .update("members", com.google.firebase.firestore.FieldValue.arrayUnion(newMember))
                    .await()
                
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
}
