package wingzone.zenith.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import wingzone.zenith.data.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import com.google.firebase.Timestamp

class FirebaseAuthRepository : IAuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    init {
        // Enable offline persistence
        try {
            FirebaseFirestore.getInstance().firestoreSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
        } catch (e: Exception) {
            android.util.Log.e("FirebaseAuthRepository", "Firestore persistence already set", e)
        }
        
        // Check if user is already signed in
        auth.currentUser?.let { firebaseUser ->
            android.util.Log.d("FirebaseAuthRepository", "User already signed in: ${firebaseUser.uid}")
            loadUserData(firebaseUser)
        } ?: run {
            android.util.Log.d("FirebaseAuthRepository", "No user signed in")
            _authState.value = AuthState.Unauthenticated
        }
        
        // Listen to auth state changes
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser == null) {
                android.util.Log.d("FirebaseAuthRepository", "Auth state changed: user signed out")
                _authState.value = AuthState.Unauthenticated
                _currentUser.value = null
            } else {
                android.util.Log.d("FirebaseAuthRepository", "Auth state changed: user signed in ${firebaseUser.uid}")
                if (_currentUser.value == null) {
                    loadUserData(firebaseUser)
                }
            }
        }
    }
    
    private fun loadUserData(firebaseUser: FirebaseUser) {
        android.util.Log.d("FirebaseAuthRepository", "Loading user data for: ${firebaseUser.uid}")
        firestore.collection("users")
            .document(firebaseUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    android.util.Log.d("FirebaseAuthRepository", "User document found: ${document.data}")
                    val user = User(
                        id = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        name = document.getString("displayName") ?: document.getString("name") ?: "User",
                        phoneNumber = document.getString("phoneNumber"),
                        profileImageUrl = firebaseUser.photoUrl?.toString(),
                        wzBalance = document.getDouble("wzBalance") ?: 0.0,
                        wzPoints = document.getLong("wzPoints")?.toInt() ?: 100,
                        isPhoneVerified = document.getBoolean("isPhoneVerified") ?: false,
                        isEmailVerified = firebaseUser.isEmailVerified
                    )
                    _currentUser.value = user
                    _authState.value = AuthState.Authenticated(user)
                } else {
                    android.util.Log.w("FirebaseAuthRepository", "User document doesn't exist, creating...")
                    // Create user document if it doesn't exist
                    createUserDocument(firebaseUser)
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("FirebaseAuthRepository", "Failed to load user data", e)
                // Even if Firestore fails, we can still authenticate with basic info from FirebaseAuth
                val user = User(
                    id = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    name = firebaseUser.displayName ?: "User",
                    wzBalance = 0.0,
                    wzPoints = 100
                )
                _currentUser.value = user
                _authState.value = AuthState.Authenticated(user)
            }
    }
    
    private fun createUserDocument(firebaseUser: FirebaseUser) {
        val userData = hashMapOf(
            "email" to firebaseUser.email,
            "displayName" to (firebaseUser.displayName ?: "User"),
            "name" to (firebaseUser.displayName ?: "User"),
            "phoneNumber" to firebaseUser.phoneNumber,
            "wzBalance" to 0.0,
            "wzPoints" to 100, // Welcome bonus
            "role" to "customer",
            "createdAt" to Timestamp.now(),
            "updatedAt" to Timestamp.now()
        )
        
        android.util.Log.d("FirebaseAuthRepository", "Creating user document: $userData")
        
        firestore.collection("users")
            .document(firebaseUser.uid)
            .set(userData)
            .addOnSuccessListener {
                android.util.Log.d("FirebaseAuthRepository", "User document created successfully")
                loadUserData(firebaseUser)
            }
            .addOnFailureListener { e ->
                android.util.Log.e("FirebaseAuthRepository", "Failed to create user document", e)
                _authState.value = AuthState.Error(e.message ?: "Failed to create user profile")
                
                // Still set a basic user object even if Firestore fails
                val user = User(
                    id = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    name = firebaseUser.displayName ?: "User",
                    wzBalance = 0.0,
                    wzPoints = 100
                )
                _currentUser.value = user
                _authState.value = AuthState.Authenticated(user)
            }
    }
    
    override suspend fun signUp(email: String, password: String, name: String): Result<User> {
        return try {
            _authState.value = AuthState.Loading
            
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Failed to create user")
            
            // Update profile with name
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()
            
            // Create user document in Firestore
            val userData = hashMapOf(
                "email" to email,
                "displayName" to name,
                "name" to name,
                "wzBalance" to 0.0,
                "wzPoints" to 100, // Welcome bonus
                "role" to "customer",
                "createdAt" to Timestamp.now()
            )
            
            firestore.collection("users")
                .document(firebaseUser.uid)
                .set(userData)
                .await()
            
            val user = User(
                id = firebaseUser.uid,
                email = email,
                name = name,
                wzBalance = 0.0,
                wzPoints = 100
            )
            
            _currentUser.value = user
            _authState.value = AuthState.Authenticated(user)
            
            Result.success(user)
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "Sign up failed")
            Result.failure(e)
        }
    }
    
    override suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            _authState.value = AuthState.Loading
            
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Failed to sign in")
            
            // Load user data from Firestore
            var document = firestore.collection("users")
                .document(firebaseUser.uid)
                .get()
                .await()
            
            if (!document.exists()) {
                // Create user document if it doesn't exist (for users created before Firestore setup)
                val userData = hashMapOf(
                    "email" to (firebaseUser.email ?: email),
                    "displayName" to (firebaseUser.displayName ?: "User"),
                    "name" to (firebaseUser.displayName ?: "User"),
                    "phoneNumber" to firebaseUser.phoneNumber,
                    "wzBalance" to 0.0,
                    "wzPoints" to 100,
                    "role" to "customer",
                    "createdAt" to Timestamp.now()
                )
                
                firestore.collection("users")
                    .document(firebaseUser.uid)
                    .set(userData)
                    .await()
                
                // Reload document
                document = firestore.collection("users")
                    .document(firebaseUser.uid)
                    .get()
                    .await()
            }
            
            val user = User(
                id = firebaseUser.uid,
                email = firebaseUser.email ?: email,
                name = document.getString("displayName") ?: document.getString("name") ?: "",
                phoneNumber = document.getString("phoneNumber"),
                profileImageUrl = firebaseUser.photoUrl?.toString(),
                wzBalance = document.getDouble("wzBalance") ?: 0.0,
                wzPoints = document.getLong("wzPoints")?.toInt() ?: 0,
                isPhoneVerified = document.getBoolean("isPhoneVerified") ?: false,
                isEmailVerified = firebaseUser.isEmailVerified
            )
            
            _currentUser.value = user
            _authState.value = AuthState.Authenticated(user)
            
            Result.success(user)
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "Sign in failed")
            Result.failure(e)
        }
    }
    
    override fun signOut() {
        auth.signOut()
        _authState.value = AuthState.Unauthenticated
        _currentUser.value = null
    }
    
    override fun getCurrentUser(): User? = _currentUser.value
    
    override fun isAuthenticated(): Boolean = auth.currentUser != null && _currentUser.value != null
    
    override fun reloadCurrentUser() {
        val firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            loadUserData(firebaseUser)
        }
    }
}
