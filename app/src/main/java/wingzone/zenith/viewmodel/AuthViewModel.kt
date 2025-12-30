package wingzone.zenith.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import wingzone.zenith.data.models.*
import wingzone.zenith.data.repository.IAuthRepository
import wingzone.zenith.data.repository.RepositoryProvider
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepository: IAuthRepository = RepositoryProvider.getAuthRepository()) : ViewModel() {
    
    val authState: StateFlow<AuthState> = authRepository.authState
    val currentUser: StateFlow<User?> = authRepository.currentUser
    
    fun signUp(email: String, password: String, name: String, onResult: (Result<User>) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.signUp(email, password, name)
            onResult(result)
        }
    }
    
    fun signIn(email: String, password: String, onResult: (Result<User>) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.signIn(email, password)
            onResult(result)
        }
    }
    
    fun signOut() {
        authRepository.signOut()
    }
    
    fun isAuthenticated(): Boolean = authRepository.isAuthenticated()
    
    fun getCurrentUser(): User? = authRepository.getCurrentUser()
    
    fun reloadUser() {
        authRepository.reloadCurrentUser()
    }
}
