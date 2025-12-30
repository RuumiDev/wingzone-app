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
import androidx.core.content.ContextCompat
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

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Splash) }
    val authViewModel = remember { AuthViewModel() }
    val cartViewModel = remember { CartViewModel() }
    val groupOrderViewModel = remember { GroupOrderViewModel() }
    var requiresAuth by remember { mutableStateOf(false) }
    var showOrderTracking by remember { mutableStateOf(false) }
    var trackingOrderId by remember { mutableStateOf<String?>(null) }
    var showOrderHistory by remember { mutableStateOf(false) }
    var returnToAccount by remember { mutableStateOf(false) }
    
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
                    }
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
    }
}
