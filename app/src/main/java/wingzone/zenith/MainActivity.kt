package wingzone.zenith

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import wingzone.zenith.ui.screens.*
import wingzone.zenith.ui.theme.WZAPPTheme
import wingzone.zenith.viewmodel.AuthViewModel
import wingzone.zenith.viewmodel.CartViewModel
import wingzone.zenith.viewmodel.GroupOrderViewModel

sealed class Screen {
    object Splash : Screen()
    object Home : Screen()
    object Login : Screen()
    object SignUp : Screen()
    object CreateLobby : Screen()
    object JoinLobby : Screen()
    object QRScanner : Screen()
    data class LobbyDetail(val lobbyId: String) : Screen()
}

class MainActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Notification permission granted
        } else {
            // Permission denied - notifications won't work
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        // Schedule background worker for notifications
        wingzone.zenith.service.OrderNotificationWorker.schedulePeriodicWork(this)
        
        setContent {
            WZAPPTheme {
                AppNavigation()
            }
        }
    }
}

// Helper function to load lobby as GroupOrder
suspend fun loadLobbyAsGroupOrder(
    lobbyId: String,
    groupOrderViewModel: GroupOrderViewModel
) {
    try {
        val firestore = FirebaseFirestore.getInstance()
        val lobbyDoc = firestore.collection("lobbies").document(lobbyId).get().await()
        
        if (lobbyDoc.exists()) {
            val lobbyData = lobbyDoc.data ?: return
            
            // Convert lobby members to GroupMembers
            @Suppress("UNCHECKED_CAST")
            val members = (lobbyData["members"] as? List<Map<String, Any>>)?.map { member ->
                val userId = member["userId"] as? String ?: ""
                val userName = member["userName"] as? String ?: ""
                val isHost = member["isHost"] as? Boolean ?: false
                
                @Suppress("UNCHECKED_CAST")
                val cartItems = (member["cartItems"] as? List<Map<String, Any>>)?.map { item ->
                    parseCartItemFromFirestore(item)
                } ?: emptyList()
                
                wingzone.zenith.data.models.GroupMember(
                    userId = userId,
                    name = userName,
                    profileImageUrl = null,
                    isHost = isHost,
                    cartItems = cartItems,
                    hasPaid = (member["status"] as? String) == "paid"
                )
            } ?: emptyList()
            
            // Create GroupOrder from lobby data
            val groupOrder = wingzone.zenith.data.models.GroupOrder(
                id = lobbyId,
                code = lobbyData["code"] as? String ?: "",
                hostId = lobbyData["hostUserId"] as? String ?: "",
                members = members,
                maxMembers = ((lobbyData["maxMembers"] as? Number)?.toInt() ?: 10),
                status = wingzone.zenith.data.models.GroupOrderStatus.ORDERING,
                createdAt = (lobbyData["createdAt"] as? com.google.firebase.Timestamp)?.toDate() ?: java.util.Date(),
                expiresAt = (lobbyData["expiresAt"] as? com.google.firebase.Timestamp)?.toDate() ?: java.util.Date(),
                deliveryAddress = null,
                specialInstructions = null
            )
            
            // Add to repository cache so addItemToGroupOrder can find it
            wingzone.zenith.data.repository.RepositoryProvider.getGroupOrderRepository()
                .addOrderToCache(groupOrder)
            
            groupOrderViewModel.setCurrentGroupOrder(groupOrder)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// Helper function to parse cart items from Firestore
@Suppress("UNCHECKED_CAST")
fun parseCartItemFromFirestore(data: Map<String, Any>): wingzone.zenith.data.models.CartItem {
    // Parse menu item
    val menuItemData = data["menuItem"] as? Map<String, Any>
    val menuItem = if (menuItemData != null) {
        wingzone.zenith.data.models.MenuItem(
            id = menuItemData["id"] as? String ?: "",
            name = menuItemData["name"] as? String ?: "",
            description = menuItemData["description"] as? String ?: "",
            price = (menuItemData["price"] as? Number)?.toDouble() ?: 0.0,
            category = menuItemData["category"] as? String ?: "",
            imageUrl = menuItemData["imageUrl"] as? String,
            isAvailable = menuItemData["isAvailable"] as? Boolean ?: true,
            requiresCustomization = menuItemData["requiresCustomization"] as? Boolean ?: false,
            customizationOptions = null
        )
    } else {
        wingzone.zenith.data.models.MenuItem(
            id = "",
            name = "Unknown Item",
            description = "",
            price = 0.0,
            category = ""
        )
    }
    
    // Parse customization if exists - for now return null, can enhance later
    val customization: wingzone.zenith.data.models.EntreeCustomization? = null
    
    return wingzone.zenith.data.models.CartItem(
        menuItem = menuItem,
        quantity = (data["quantity"] as? Number)?.toInt() ?: 1,
        customization = customization
    )
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Splash) }
    val authViewModel = remember { AuthViewModel() }
    val cartViewModel = remember { CartViewModel() }
    val groupOrderViewModel = remember { GroupOrderViewModel() }
    val lobbyViewModel = remember { wingzone.zenith.viewmodel.LobbyViewModel(context.applicationContext as android.app.Application) }
    var requiresAuth by remember { mutableStateOf(false) }
    var showOrderTracking by remember { mutableStateOf(false) }
    var trackingOrderId by remember { mutableStateOf<String?>(null) }
    var showOrderHistory by remember { mutableStateOf(false) }
    var returnToAccount by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    var currentLobbyId by remember { mutableStateOf<String?>(null) }
    
    // Global order notification handler
    if (currentScreen == Screen.Home) {
        wingzone.zenith.ui.components.OrderNotificationHandler(
            onNavigateToTracking = { orderId ->
                trackingOrderId = orderId
                showOrderTracking = true
            }
        )
    }
    
    when (currentScreen) {
        Screen.Splash -> {
            SplashScreen(
                onSplashFinished = {
                    currentScreen = Screen.Home
                }
            )
        }
        Screen.Home -> {
            if (showOrderHistory) {
                wingzone.zenith.ui.screens.OrderHistoryScreen(
                    onBack = { 
                        showOrderHistory = false
                        if (returnToAccount) {
                            returnToAccount = false
                            // Will navigate back to account screen in HomeScreen
                        }
                    },
                    onOrderClick = { orderId ->
                        trackingOrderId = orderId
                        showOrderHistory = false
                        showOrderTracking = true
                    }
                )
            } else if (showOrderTracking) {
                if (trackingOrderId != null) {
                    wingzone.zenith.ui.screens.OrderTrackingDetailScreen(
                        orderId = trackingOrderId!!,
                        onBack = { 
                            showOrderTracking = false
                            trackingOrderId = null
                            if (returnToAccount) {
                                showOrderHistory = true
                            }
                        }
                    )
                } else {
                    OrderTrackingScreen(
                        onBack = { 
                            showOrderTracking = false
                        }
                    )
                }
            } else {
                HomeScreen(
                    authViewModel = authViewModel,
                    cartViewModel = cartViewModel,
                    groupOrderViewModel = groupOrderViewModel,
                    lobbyViewModel = lobbyViewModel,
                    onAuthRequired = {
                        requiresAuth = true
                        currentScreen = Screen.Login
                    },
                    onNavigateToOrderTracking = {
                        showOrderTracking = true
                        trackingOrderId = null
                    },
                    onNavigateToOrderHistory = {
                        showOrderHistory = true
                        returnToAccount = true
                    },
                    onNavigateToOrderDetails = { orderId ->
                        trackingOrderId = orderId
                        showOrderTracking = true
                    },
                    onNavigateToCreateLobby = {
                        currentScreen = Screen.CreateLobby
                    },
                    onNavigateToJoinLobby = {
                        currentScreen = Screen.JoinLobby
                    },
                    onNavigateToGroupOrder = {},
                    onNavigateToLobbyDetail = { lobbyId ->
                        currentLobbyId = lobbyId
                        currentScreen = Screen.LobbyDetail(lobbyId)
                    },
                    initialTab = selectedTab
                )
            }
        }
        Screen.Login -> {
            LoginScreen(
                authViewModel = authViewModel,
                onNavigateToSignUp = {
                    currentScreen = Screen.SignUp
                },
                onLoginSuccess = {
                    currentScreen = Screen.Home
                    requiresAuth = false
                },
                onBack = {
                    currentScreen = Screen.Home
                }
            )
        }
        Screen.SignUp -> {
            SignUpScreen(
                authViewModel = authViewModel,
                onNavigateToLogin = {
                    currentScreen = Screen.Login
                },
                onSignUpSuccess = {
                    currentScreen = Screen.Home
                    requiresAuth = false
                },
                onBack = {
                    currentScreen = Screen.Login
                }
            )
        }
        Screen.CreateLobby -> {
            wingzone.zenith.ui.screens.CreateLobbyScreen(
                viewModel = lobbyViewModel,
                onNavigateBack = {
                    currentScreen = Screen.Home
                },
                onLobbyCreated = { lobbyId ->
                    currentLobbyId = lobbyId
                    currentScreen = Screen.LobbyDetail(lobbyId)
                }
            )
        }
        Screen.JoinLobby -> {
            wingzone.zenith.ui.screens.JoinLobbyScreen(
                viewModel = lobbyViewModel,
                onNavigateBack = {
                    currentScreen = Screen.Home
                },
                onNavigateToScanner = {
                    currentScreen = Screen.QRScanner
                },
                onLobbyJoined = { lobbyId ->
                    currentLobbyId = lobbyId
                    currentScreen = Screen.LobbyDetail(lobbyId)
                }
            )
        }
        Screen.QRScanner -> {
            wingzone.zenith.ui.screens.QRScannerScreen(
                onBack = {
                    currentScreen = Screen.JoinLobby
                },
                onCodeScanned = { code ->
                    // Auto-fill code and attempt to join
                    lobbyViewModel.updateJoinCode(code)
                    lobbyViewModel.joinLobby(code) { result ->
                        if (result.isSuccess) {
                            val lobbyId = result.getOrNull()
                            if (lobbyId != null) {
                                currentLobbyId = lobbyId
                                currentScreen = Screen.LobbyDetail(lobbyId)
                            }
                        } else {
                            // Show error and go back to manual entry
                            currentScreen = Screen.JoinLobby
                        }
                    }
                }
            )
        }
        is Screen.LobbyDetail -> {
            wingzone.zenith.ui.screens.LobbyDetailScreen(
                lobbyId = (currentScreen as Screen.LobbyDetail).lobbyId,
                currentUserId = authViewModel.getCurrentUser()?.id ?: "",
                onNavigateBack = {
                    groupOrderViewModel.setCurrentGroupOrder(null)
                    selectedTab = 3
                    currentScreen = Screen.Home
                },
                onNavigateToMenu = {
                    // Capture lobby ID before changing screen
                    val lobbyId = (currentScreen as Screen.LobbyDetail).lobbyId
                    // Load lobby as GroupOrder for menu context
                    CoroutineScope(Dispatchers.Main).launch {
                        loadLobbyAsGroupOrder(
                            lobbyId,
                            groupOrderViewModel
                        )
                    }
                    selectedTab = 1 // Menu tab
                    currentScreen = Screen.Home
                },
                onLobbyDeleted = {
                    groupOrderViewModel.setCurrentGroupOrder(null)
                    selectedTab = 3
                    currentScreen = Screen.Home
                }
            )
        }
    }
}
