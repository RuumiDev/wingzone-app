package wingzone.zenith

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import wingzone.zenith.data.repository.Order
import wingzone.zenith.ui.screens.*
import wingzone.zenith.ui.theme.WZAPPTheme
import wingzone.zenith.utils.rememberNetworkState
import wingzone.zenith.viewmodel.AuthViewModel
import wingzone.zenith.viewmodel.CartViewModel
import wingzone.zenith.viewmodel.GroupOrderViewModel
import wingzone.zenith.viewmodel.MenuViewModel
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Button

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

sealed class Screen {
    object Splash : Screen()
    object Home : Screen()
    object Login : Screen()
    object SignUp : Screen()
    object CreateLobby : Screen()
    object JoinLobby : Screen()
    object QRScanner : Screen()
    data class LobbyDetail(val lobbyId: String) : Screen()
    data class PaymentWebView(val pendingOrderId: String) : Screen()
}

data class PendingOrder(
    val userId: String,
    val userName: String,
    val cartItemsJson: String, // Serialized cart data
    val paymentMethod: String,
    val paymentType: String,
    val phoneNumber: String?,
    val orderType: String?,
    val location: String?,
    val lobbyId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

object DeepLinkHandler {
    private val _lobbyCodeFlow = MutableSharedFlow<String>(replay = 0)
    val lobbyCodeFlow = _lobbyCodeFlow.asSharedFlow()
    
    private val _paymentSuccessFlow = MutableSharedFlow<String>(replay = 0) // Emits pendingOrderId
    val paymentSuccessFlow = _paymentSuccessFlow.asSharedFlow()
    
    private val _paymentFailedFlow = MutableSharedFlow<String>(replay = 0)
    val paymentFailedFlow = _paymentFailedFlow.asSharedFlow()
    
    suspend fun emitLobbyCode(code: String) {
        android.util.Log.d("DeepLinkHandler", "Emitting lobby code: $code")
        _lobbyCodeFlow.emit(code)
    }
    
    suspend fun emitPaymentSuccess(pendingOrderId: String) {
        android.util.Log.d("DeepLinkHandler", "Payment success for: $pendingOrderId")
        _paymentSuccessFlow.emit(pendingOrderId)
    }
    
    suspend fun emitPaymentFailed(pendingOrderId: String) {
        android.util.Log.d("DeepLinkHandler", "Payment failed for: $pendingOrderId")
        _paymentFailedFlow.emit(pendingOrderId)
    }
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
        
        // Handle deep link from cold start
        handleDeepLink(intent)
        
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
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Handle deep link when app is already running
        handleDeepLink(intent)
    }
    
    private fun handleDeepLink(intent: Intent?) {
        intent?.data?.let { uri ->
            android.util.Log.d("MainActivity", "Deep link received: $uri")
            
            // Handle lobby join deep link
            if (uri.scheme == "wz" && uri.host == "join") {
                val code = uri.getQueryParameter("code")
                android.util.Log.d("MainActivity", "Lobby code extracted: $code")
                if (code != null) {
                    // Emit the code to the flow
                    CoroutineScope(Dispatchers.Main).launch {
                        DeepLinkHandler.emitLobbyCode(code)
                    }
                }
            }
            
            // Handle payment callback deep links (wz://payment/success or wz://payment/failed)
            if (uri.scheme == "wz" && uri.host == "payment") {
                val pendingOrderId = uri.getQueryParameter("order_id") ?: uri.getQueryParameter("reference_id") ?: ""
                when (uri.path) {
                    "/success" -> {
                        android.util.Log.d("MainActivity", "Payment success for order: $pendingOrderId")
                        CoroutineScope(Dispatchers.Main).launch {
                            DeepLinkHandler.emitPaymentSuccess(pendingOrderId)
                        }
                    }
                    "/failed" -> {
                        android.util.Log.d("MainActivity", "Payment failed for order: $pendingOrderId")
                        CoroutineScope(Dispatchers.Main).launch {
                            DeepLinkHandler.emitPaymentFailed(pendingOrderId)
                        }
                    }
                }
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
                specialInstructions = null,
                orderType = lobbyData["orderType"] as? String,
                location = (lobbyData["location"] as? Map<String, Any>)?.get("name") as? String,
                paymentMethod = lobbyData["paymentMethod"] as? String
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
    val menuViewModel = remember { MenuViewModel(context.applicationContext as android.app.Application) }
    val lobbyViewModel = remember { 
        wingzone.zenith.viewmodel.LobbyViewModel(
            context.applicationContext as android.app.Application,
            groupOrderViewModel = groupOrderViewModel
        ) 
    }
    var requiresAuth by remember { mutableStateOf(false) }
    var showOrderTracking by remember { mutableStateOf(false) }
    var trackingOrderId by remember { mutableStateOf<String?>(null) }
    var showOrderHistory by remember { mutableStateOf(false) }
    var showMyReviews by remember { mutableStateOf(false) }
    var returnToAccount by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    var currentLobbyId by remember { mutableStateOf<String?>(null) }
    var pendingOrderIdForProcessing by remember { mutableStateOf<String?>(null) }
    var qrScannerReturnToHome by remember { mutableStateOf(false) }
    
    // Active order tracking
    var activeOrder by remember { mutableStateOf<Order?>(null) }
    
    // Listen for active orders
    DisposableEffect(authViewModel.getCurrentUser()?.id) {
        val userId = authViewModel.getCurrentUser()?.id
        val listener = if (userId != null) {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("orders")
                .whereEqualTo("userId", userId)
                .whereIn("status", listOf("pending", "confirmed", "preparing", "ready"))
                .addSnapshotListener { snapshot: com.google.firebase.firestore.QuerySnapshot?, error: com.google.firebase.firestore.FirebaseFirestoreException? ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    
                    // Get the most recent active order
                    val orders: List<Order> = snapshot.documents.mapNotNull { doc: com.google.firebase.firestore.DocumentSnapshot ->
                        try {
                            Order(
                                id = doc.id,
                                userId = doc.getString("userId") ?: "",
                                userName = doc.getString("userName") ?: "",
                                status = doc.getString("status") ?: "pending",
                                total = doc.getDouble("total") ?: 0.0,
                                createdAt = doc.getTimestamp("createdAt")?.toDate() ?: Date()
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    
                    activeOrder = orders.maxByOrNull { order: Order -> order.createdAt }
                }
        } else {
            activeOrder = null
            null
        }
        
        onDispose {
            listener?.remove()
        }
    }
    
    // Listen for deep link events
    LaunchedEffect(Unit) {
        DeepLinkHandler.lobbyCodeFlow.collect { code ->
            android.util.Log.d("AppNavigation", "Received lobby code from flow: $code")
            
            // Wait if we're still on splash
            while (currentScreen == Screen.Splash) {
                android.util.Log.d("AppNavigation", "Waiting for splash to finish...")
                delay(100)
            }
            
            android.util.Log.d("AppNavigation", "Processing lobby code: $code")
            
            // Pre-fill the join code
            lobbyViewModel.updateJoinCode(code)
            
            // Navigate to Join Lobby screen
            currentScreen = Screen.JoinLobby
            
            android.widget.Toast.makeText(
                context,
                "Joining lobby $code...",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            
            // Give UI time to render
            delay(1000)
            
            // Auto-trigger join
            lobbyViewModel.joinLobby(code) { result ->
                CoroutineScope(Dispatchers.Main).launch {
                    if (result.isSuccess) {
                        val lobbyId = result.getOrNull()
                        android.util.Log.d("AppNavigation", "Join success: $lobbyId")
                        if (lobbyId != null) {
                            currentLobbyId = lobbyId
                            currentScreen = Screen.LobbyDetail(lobbyId)
                            android.widget.Toast.makeText(
                                context,
                                "✓ Successfully joined!",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        val error = result.exceptionOrNull()
                        android.util.Log.e("AppNavigation", "Join failed: ${error?.message}")
                        android.widget.Toast.makeText(
                            context,
                            "Failed: ${error?.message ?: "Unknown error"}",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }
    
    // Listen for payment success via deep link (browser redirects app after payment)
    LaunchedEffect(Unit) {
        DeepLinkHandler.paymentSuccessFlow.collect { pendingOrderId ->
            android.util.Log.d("AppNavigation", "Payment succeeded (deep link) for order: $pendingOrderId")

            // The order was already created in Firestore by createToyyibPayBill Cloud Function
            // and updated to paid/confirmed by the paymentCallback webhook.
            // We just need to clean up locally and navigate.
            try {
                wingzone.zenith.utils.PendingOrderManager.deletePendingOrder(context, pendingOrderId)
            } catch (e: Exception) {
                // Non-critical – log and continue
                android.util.Log.w("AppNavigation", "Could not delete local pending order", e)
            }

            cartViewModel.clearCart()

            android.widget.Toast.makeText(
                context,
                "✓ Payment successful! Your order has been placed.",
                android.widget.Toast.LENGTH_LONG
            ).show()

            currentScreen = Screen.Home
        }
    }
    
    // Listen for payment failure callback
    LaunchedEffect(Unit) {
        DeepLinkHandler.paymentFailedFlow.collect { pendingOrderId ->
            android.util.Log.e("AppNavigation", "Payment failed for order: $pendingOrderId")
            
            // Show error message
            android.widget.Toast.makeText(
                context,
                "Payment failed. Please try again.",
                android.widget.Toast.LENGTH_LONG
            ).show()
            
            // Keep pending order for retry - don't delete
            // User can try payment again from cart
            
            // Navigate back to cart
            currentScreen = Screen.Home
            selectedTab = 2 // Cart tab
        }
    }
    
    // Network state monitoring
    val isConnected by rememberNetworkState()
    var previousConnectedState by remember { mutableStateOf(true) }
    var showBackOnlineBanner by remember { mutableStateOf(false) }
    
    // Detect connection restoration and show temporary banner
    LaunchedEffect(isConnected) {
        if (isConnected && !previousConnectedState) {
            // Connection restored
            showBackOnlineBanner = true
            delay(3000) // Show for 3 seconds
            showBackOnlineBanner = false
        }
        previousConnectedState = isConnected
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Main content
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
                            selectedTab = 4 // Navigate to Account tab
                        }
                    },
                    onOrderClick = { orderId ->
                        trackingOrderId = orderId
                        showOrderHistory = false
                        showOrderTracking = true
                    },
                    cartViewModel = cartViewModel
                )
            } else if (showMyReviews) {
                wingzone.zenith.ui.screens.MyReviewsScreen(
                    onBack = { 
                        showMyReviews = false
                        if (returnToAccount) {
                            returnToAccount = false
                            selectedTab = 4 // Navigate to Account tab
                        }
                    },
                    onRateOrder = { order ->
                        // Handle rating - could show rating sheet here
                        // For now, just navigate back to order history
                        showMyReviews = false
                        showOrderHistory = true
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
                                returnToAccount = false
                                selectedTab = 4 // Navigate to Account tab
                            }
                        }
                    )
                } else {
                    OrderTrackingScreen(
                        onBack = { 
                            showOrderTracking = false
                            if (returnToAccount) {
                                returnToAccount = false
                                selectedTab = 4 // Navigate to Account tab
                            }
                        }
                    )
                }
            } else {
                HomeScreen(
                    authViewModel = authViewModel,
                    cartViewModel = cartViewModel,
                    groupOrderViewModel = groupOrderViewModel,
                    menuViewModel = menuViewModel,
                    lobbyViewModel = lobbyViewModel,
                    onAuthRequired = {
                        requiresAuth = true
                        currentScreen = Screen.Login
                    },
                    onNavigateToOrderTracking = {
                        showOrderTracking = true
                        trackingOrderId = null
                        returnToAccount = true
                    },
                    onNavigateToOrderHistory = {
                        showOrderHistory = true
                        returnToAccount = true
                    },
                    onNavigateToMyReviews = {
                        showMyReviews = true
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
                    onNavigateToLobbyDetail = { lobbyId ->
                        currentLobbyId = lobbyId
                        currentScreen = Screen.LobbyDetail(lobbyId)
                    },
                    onNavigateToPayment = { pendingOrderId ->
                        currentScreen = Screen.PaymentWebView(pendingOrderId)
                    },
                    onNavigateToQRScanner = {
                        qrScannerReturnToHome = true
                        currentScreen = Screen.QRScanner
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
                    if (qrScannerReturnToHome) {
                        qrScannerReturnToHome = false
                        currentScreen = Screen.Home
                        selectedTab = 3
                    } else {
                        currentScreen = Screen.JoinLobby
                    }
                },
                onCodeScanned = { code ->
                    // Auto-fill code and attempt to join
                    lobbyViewModel.updateJoinCode(code)
                    lobbyViewModel.joinLobby(code) { result ->
                        if (result.isSuccess) {
                            val lobbyId = result.getOrNull()
                            if (lobbyId != null) {
                                qrScannerReturnToHome = false
                                currentLobbyId = lobbyId
                                currentScreen = Screen.LobbyDetail(lobbyId)
                            }
                        } else {
                            // Show error and go back to manual entry
                            if (qrScannerReturnToHome) {
                                qrScannerReturnToHome = false
                                currentScreen = Screen.Home
                                selectedTab = 3
                            } else {
                                currentScreen = Screen.JoinLobby
                            }
                        }
                    }
                }
            )
        }
        is Screen.LobbyDetail -> {
            wingzone.zenith.ui.screens.LobbyDetailScreen(
                lobbyId = (currentScreen as Screen.LobbyDetail).lobbyId,
                currentUserId = authViewModel.getCurrentUser()?.id ?: "",
                lobbyViewModel = lobbyViewModel,
                onNavigateBack = {
                    // Go back to home tab (tab 0)
                    selectedTab = 0
                    currentScreen = Screen.Home
                },
                onNavigateToMenu = {
                    // Capture lobby ID before changing screen
                    val lobbyId = (currentScreen as Screen.LobbyDetail).lobbyId
                    selectedTab = 1 // Menu tab - set BEFORE screen change
                    currentScreen = Screen.Home
                    // Load lobby as GroupOrder for menu context
                    CoroutineScope(Dispatchers.Main).launch {
                        loadLobbyAsGroupOrder(
                            lobbyId,
                            groupOrderViewModel
                        )
                    }
                },
                onNavigateToCart = {
                    // Capture lobby ID before changing screen
                    val lobbyId = (currentScreen as Screen.LobbyDetail).lobbyId
                    selectedTab = 2 // Cart tab - set BEFORE screen change
                    currentScreen = Screen.Home
                    // Load lobby as GroupOrder for menu context
                    CoroutineScope(Dispatchers.Main).launch {
                        loadLobbyAsGroupOrder(
                            lobbyId,
                            groupOrderViewModel
                        )
                    }
                },
                onLobbyDeleted = {
                    groupOrderViewModel.setCurrentGroupOrder(null)
                    selectedTab = 3
                    currentScreen = Screen.Home
                }
            )
        }
        is Screen.PaymentWebView -> {
            val pendingOrderId = (currentScreen as Screen.PaymentWebView).pendingOrderId
            var paymentUrl by remember { mutableStateOf<String?>(null) }
            var isCreatingPayment by remember { mutableStateOf(false) }
            var paymentError by remember { mutableStateOf<String?>(null) }
            
            // Create payment URL on first load
            LaunchedEffect(pendingOrderId) {
                isCreatingPayment = true
                paymentError = null
                
                val result = wingzone.zenith.utils.PendingOrderManager.getPaymentUrl(
                    context = context,
                    pendingOrderId = pendingOrderId
                )
                
                result.onSuccess { url ->
                    paymentUrl = url
                    isCreatingPayment = false
                }.onFailure { error ->
                    paymentError = error.message ?: "Failed to create payment link"
                    isCreatingPayment = false
                    android.util.Log.e("MainActivity", "Payment URL creation failed", error)
                }
            }
            
            // Show different UI based on state
            when {
                isCreatingPayment -> {
                    // Loading state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Creating payment link...",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
                
                paymentError != null -> {
                    // Error state
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Payment Error",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = paymentError!!,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    // Return to cart
                                    currentScreen = Screen.Home
                                    selectedTab = 2 // Cart tab
                                }
                            ) {
                                Text("Return to Cart")
                            }
                        }
                    }
                }
                
                paymentUrl != null -> {
                    // Success - show WebView
                    wingzone.zenith.ui.screens.PaymentWebViewScreen(
                        paymentUrl = paymentUrl!!,
                        onBack = {
                            // User cancelled payment - go back to cart
                            currentScreen = Screen.Home
                            selectedTab = 2 // Cart tab
                        },
                        onPaymentSuccess = {
                            // Payment confirmed – the order already exists in Firestore
                            // (created by createToyyibPayBill Cloud Function and updated
                            // to paid/confirmed by the paymentCallback webhook).
                            // Just clean up locally, clear the cart and navigate home.
                            wingzone.zenith.utils.PendingOrderManager.deletePendingOrder(context, pendingOrderId)
                            cartViewModel.clearCart()
                            android.widget.Toast.makeText(
                                context,
                                "✓ Payment successful! Your order has been placed.",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                            currentScreen = Screen.Home
                        },
                        onPaymentFailed = {
                            // Show error and return to cart
                            android.util.Log.e("MainActivity", "Payment failed for order: $pendingOrderId")
                            currentScreen = Screen.Home
                            selectedTab = 2 // Cart tab
                            // Could show a Toast here
                        }
                    )
                }
            }
        }
        }  // Close when statement
        
        // Offline banner
        AnimatedVisibility(
            visible = !isConnected,
            enter = slideInVertically(initialOffsetY = { -it }),
            exit = slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            OfflineBanner()
        }
        
        // Back online banner
        AnimatedVisibility(
            visible = showBackOnlineBanner,
            enter = slideInVertically(initialOffsetY = { -it }),
            exit = slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            BackOnlineBanner()
        }
        
        // Floating order tracker
        AnimatedVisibility(
            visible = activeOrder != null && currentScreen == Screen.Home && !showOrderTracking,
            enter = slideInVertically(initialOffsetY = { it }) + androidx.compose.animation.fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + androidx.compose.animation.fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 80.dp) // Above bottom navigation
        ) {
            activeOrder?.let { order ->
                FloatingOrderTracker(
                    order = order,
                    onClick = {
                        trackingOrderId = order.id
                        showOrderTracking = true
                    }
                )
            }
        }
    }
}

@Composable
fun OfflineBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFEF4444))
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "No internet",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "No internet connection",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun BackOnlineBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF10B981))
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Back online",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Back online",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun FloatingOrderTracker(
    order: Order,
    onClick: () -> Unit
) {
    val statusText = when (order.status.lowercase()) {
        "pending" -> "Order Placed"
        "confirmed" -> "Order Confirmed"
        "preparing" -> "Preparing your food..."
        "ready" -> "Ready for pickup!"
        else -> "Processing order..."
    }
    
    val statusIconRes = when (order.status.lowercase()) {
        "pending"   -> wingzone.zenith.R.drawable.confirmed_received
        "confirmed" -> wingzone.zenith.R.drawable.confirmed_received
        "preparing" -> wingzone.zenith.R.drawable.preparing
        "ready"     -> wingzone.zenith.R.drawable.ready
        else        -> wingzone.zenith.R.drawable.confirmed_received
    }
    
    // Calculate estimated time (15 mins for preparing, 10 mins for confirmed)
    val estimatedMinutes = when (order.status.lowercase()) {
        "confirmed" -> 20
        "preparing" -> 15
        "ready" -> 5
        else -> 25
    }
    
    val calendar = java.util.Calendar.getInstance()
    calendar.time = order.createdAt
    calendar.add(java.util.Calendar.MINUTE, estimatedMinutes)
    val etaFormat = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
    val etaTime = etaFormat.format(calendar.time)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Status icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(wingzone.zenith.ui.theme.WingZoneOrange.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = statusIconRes),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        colorFilter = null
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = statusText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = wingzone.zenith.ui.theme.TextPrimary
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "By $etaTime",
                            fontSize = 14.sp,
                            color = wingzone.zenith.ui.theme.TextSecondary
                        )
                        
                        if (order.status.lowercase() in listOf("pending", "confirmed", "preparing")) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(wingzone.zenith.ui.theme.WingZoneOrange),
                                contentAlignment = Alignment.Center
                            ) {
                                // Pulsing dot animation
                                val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(
                                    label = "pulse"
                                )
                                val scale by infiniteTransition.animateFloat(
                                    initialValue = 1f,
                                    targetValue = 1.5f,
                                    animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                                        animation = androidx.compose.animation.core.tween(800),
                                        repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                                    ),
                                    label = "scale"
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            scaleX = scale
                                            scaleY = scale
                                        }
                                )
                            }
                        }
                    }
                }
            }
            
            // Loading indicator or chevron
            if (order.status.lowercase() in listOf("preparing")) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = wingzone.zenith.ui.theme.WingZoneOrange,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "View details",
                    tint = wingzone.zenith.ui.theme.TextSecondary
                )
            }
        }
    }
}
