package wingzone.zenith.data.repository

import wingzone.zenith.data.models.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthRepository : IAuthRepository {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    // Mock users database
    private val mockUsers = mutableMapOf<String, Pair<String, User>>() // email -> (password, user)
    
    override suspend fun signUp(email: String, password: String, name: String): Result<User> {
        _authState.value = AuthState.Loading
        
        // Simulate network delay
        delay(1000)
        
        // Check if user already exists
        if (mockUsers.containsKey(email)) {
            _authState.value = AuthState.Error("Email already registered")
            return Result.failure(Exception("Email already registered"))
        }
        
        // Create new user
        val user = User(
            email = email,
            name = name,
            wzBalance = 0.0,
            wzPoints = 100 // Welcome bonus
        )
        
        mockUsers[email] = Pair(password, user)
        _currentUser.value = user
        _authState.value = AuthState.Authenticated(user)
        
        return Result.success(user)
    }
    
    override suspend fun signIn(email: String, password: String): Result<User> {
        _authState.value = AuthState.Loading
        
        // Simulate network delay
        delay(1000)
        
        val userPair = mockUsers[email]
        
        if (userPair == null) {
            _authState.value = AuthState.Error("User not found")
            return Result.failure(Exception("User not found"))
        }
        
        if (userPair.first != password) {
            _authState.value = AuthState.Error("Invalid password")
            return Result.failure(Exception("Invalid password"))
        }
        
        val user = userPair.second
        _currentUser.value = user
        _authState.value = AuthState.Authenticated(user)
        
        return Result.success(user)
    }
    
    override fun signOut() {
        _currentUser.value = null
        _authState.value = AuthState.Unauthenticated
    }
    
    override fun isAuthenticated(): Boolean {
        return _currentUser.value != null
    }
    
    override fun getCurrentUser(): User? {
        return _currentUser.value
    }
    
    override fun reloadCurrentUser() {
        // Mock implementation - no need to reload in-memory data
    }
}
