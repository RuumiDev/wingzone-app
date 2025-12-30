package wingzone.zenith.data.repository

/**
 * Simple singleton to provide repository instances.
 * Toggle USE_FIREBASE to switch between mock and Firebase implementations.
 */
object RepositoryProvider {
    // Set this to true to use Firebase, false to use mock repositories
    private const val USE_FIREBASE = true  // Using Firebase for persistent data storage
    
    private var _authRepository: AuthRepository? = null
    private var _firebaseAuthRepository: FirebaseAuthRepository? = null
    private var _cartRepository: CartRepository? = null
    private var _firebaseCartRepository: FirebaseCartRepository? = null
    private var _groupOrderRepository: GroupOrderRepository? = null
    private var _firebaseGroupOrderRepository: FirebaseGroupOrderRepository? = null
    private var _menuRepository: FirebaseMenuRepository? = null
    
    fun getAuthRepository(): IAuthRepository {
        if (!USE_FIREBASE) {
            if (_authRepository == null) {
                _authRepository = AuthRepository()
            }
            return _authRepository!!
        } else {
            // Use Firebase for persistent storage
            if (_firebaseAuthRepository == null) {
                _firebaseAuthRepository = FirebaseAuthRepository()
            }
            return _firebaseAuthRepository!!
        }
    }
    
    fun getFirebaseAuthRepository(): FirebaseAuthRepository {
        if (_firebaseAuthRepository == null) {
            _firebaseAuthRepository = FirebaseAuthRepository()
        }
        return _firebaseAuthRepository!!
    }
    
    fun getCartRepository(): CartRepository {
        if (!USE_FIREBASE) {
            if (_cartRepository == null) {
                _cartRepository = CartRepository()
            }
            return _cartRepository!!
        } else {
            if (_cartRepository == null) {
                _cartRepository = CartRepository()
            }
            return _cartRepository!!
        }
    }
    
    fun getFirebaseCartRepository(): FirebaseCartRepository {
        if (_firebaseCartRepository == null) {
            _firebaseCartRepository = FirebaseCartRepository()
        }
        return _firebaseCartRepository!!
    }
    
    fun getGroupOrderRepository(): GroupOrderRepository {
        if (!USE_FIREBASE) {
            if (_groupOrderRepository == null) {
                _groupOrderRepository = GroupOrderRepository(getAuthRepository())
            }
            return _groupOrderRepository!!
        } else {
            if (_groupOrderRepository == null) {
                _groupOrderRepository = GroupOrderRepository(getAuthRepository())
            }
            return _groupOrderRepository!!
        }
    }
    
    fun getFirebaseGroupOrderRepository(): FirebaseGroupOrderRepository {
        if (_firebaseGroupOrderRepository == null) {
            _firebaseGroupOrderRepository = FirebaseGroupOrderRepository(getFirebaseAuthRepository())
        }
        return _firebaseGroupOrderRepository!!
    }
    
    val menuRepository: FirebaseMenuRepository
        get() {
            if (_menuRepository == null) {
                _menuRepository = FirebaseMenuRepository()
            }
            return _menuRepository!!
        }
    
    fun reset() {
        _authRepository = null
        _firebaseAuthRepository = null
        _cartRepository = null
        _firebaseCartRepository = null
        _groupOrderRepository = null
        _firebaseGroupOrderRepository = null
        _menuRepository = null
    }
}
