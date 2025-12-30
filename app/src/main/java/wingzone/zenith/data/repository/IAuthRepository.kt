package wingzone.zenith.data.repository

import wingzone.zenith.data.models.AuthState
import wingzone.zenith.data.models.User
import kotlinx.coroutines.flow.StateFlow

interface IAuthRepository {
    val authState: StateFlow<AuthState>
    val currentUser: StateFlow<User?>
    
    suspend fun signUp(email: String, password: String, name: String): Result<User>
    suspend fun signIn(email: String, password: String): Result<User>
    fun signOut()
    fun isAuthenticated(): Boolean
    fun getCurrentUser(): User?
    fun reloadCurrentUser()
}
