package wingzone.zenith.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import coil.compose.AsyncImage
import wingzone.zenith.R
import wingzone.zenith.data.models.CartItem
import wingzone.zenith.data.repository.FirebaseOrderRepository
import wingzone.zenith.ui.theme.*
import wingzone.zenith.viewmodel.AuthViewModel
import wingzone.zenith.viewmodel.CartViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    cartViewModel: CartViewModel = CartViewModel(),
    authViewModel: AuthViewModel = AuthViewModel(),
    groupOrderViewModel: wingzone.zenith.viewmodel.GroupOrderViewModel? = null,
    lobbyViewModel: wingzone.zenith.viewmodel.LobbyViewModel? = null,
    currentLobbyId: String? = null,
    onAuthRequired: () -> Unit = {},
    onCheckoutClick: () -> Unit = {},
    onOrderPlaced: (String) -> Unit = {},
    onPaymentComplete: () -> Unit = {}
) {
    val cart by cartViewModel.cart.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val isAuthenticated = authViewModel.isAuthenticated()
    val coroutineScope = rememberCoroutineScope()
    val orderRepository = remember { FirebaseOrderRepository() }
    
    // Check if user has already paid in current lobby
    var userHasPaid by remember { mutableStateOf(false) }
    var lobbyPaymentMethod by remember { mutableStateOf<String?>(null) }
    
    // State for remove confirmation dialog
    var showRemoveDialog by remember { mutableStateOf(false) }
    var itemToRemove by remember { mutableStateOf<CartItem?>(null) }
    
    LaunchedEffect(currentLobbyId, currentUser?.id) {
        if (currentLobbyId != null && currentUser?.id != null) {
            // Check payment status from Firestore
            try {
                val lobbyDoc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("lobbies")
                    .document(currentLobbyId)
                    .get()
                    .await()
                
                if (lobbyDoc.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    val members = lobbyDoc.get("members") as? List<Map<String, Any>> ?: emptyList()
                    val currentMember = members.find { it["userId"] == currentUser?.id }
                    userHasPaid = currentMember?.get("hasPaid") as? Boolean ?: false
                    lobbyPaymentMethod = lobbyDoc.getString("paymentMethod")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            userHasPaid = false
            lobbyPaymentMethod = null
        }
    }
    
    var selectedOrderType by remember { mutableStateOf<String?>(null) }
    var selectedBranch by remember { mutableStateOf<String?>(null) }
    var selectedPaymentMethod by remember { mutableStateOf("cash") }
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var showVerificationDialog by remember { mutableStateOf(false) }
    var isProcessingOrder by remember { mutableStateOf(false) }
    var orderSuccess by remember { mutableStateOf(false) }
    var orderError by remember { mutableStateOf<String?>(null) }
    var placedOrderId by remember { mutableStateOf<String?>(null) }
    var showOrderNotification by remember { mutableStateOf(false) }
    
    val branches = listOf("Wingzone Meru", "Wingzone GreenTown")
    
    // Redirect to auth if trying to access cart without login
    LaunchedEffect(cart.totalItems) {
        if (cart.totalItems > 0 && !isAuthenticated) {
            onAuthRequired()
        }
    }
    
    Scaffold(
        topBar = {
            Surface(
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cart",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    // Show cart items count
                    if (cart.totalItems > 0) {
                        Text(
                            text = "${cart.totalItems} items",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        },
        containerColor = BackgroundGray
    ) { paddingValues ->
        if (cart.items.isEmpty()) {
            // Empty Cart State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = Color.Gray.copy(alpha = 0.3f)
                    )
                    Text(
                        text = "Your cart is empty",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Add items from the menu to get started",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Cart Items List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Show payment status banner if user has paid in lobby
                if (userHasPaid && currentLobbyId != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Paid",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "Payment Complete - Cart Locked",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
                
                // Host pays all banner if applicable
                if (currentLobbyId != null && lobbyPaymentMethod == "host-pays-all") {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFFFFA000),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Host will cover the entire group order",
                                    fontSize = 14.sp,
                                    color = Color(0xFF856404),
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                
                // Cart Items
                items(cart.items) { cartItem ->
                    CartItemCardRedesigned(
                        cartItem = cartItem,
                        enabled = !userHasPaid,
                        onUpdateQuantity = { newQty ->
                            if (newQty > 0) {
                                cartViewModel.updateQuantity(cartItem.id, newQty)
                            } else {
                                // Show confirmation when quantity would become 0
                                itemToRemove = cartItem
                                showRemoveDialog = true
                            }
                        },
                        onRemove = {
                            // Show confirmation when delete button is clicked
                            itemToRemove = cartItem
                            showRemoveDialog = true
                        }
                    )
                }
                
                // Spacer
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Totals Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Order Summary",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Subtotal",
                                    fontSize = 14.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "RM ${String.format("%.2f", cart.subtotal)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                            }
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Total",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "RM ${String.format("%.2f", cart.total)}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WingZoneRed
                                )
                            }
                        }
                    }
                }
                
                // Add Another Order Button (for group orders)
                if (currentLobbyId != null && !userHasPaid) {
                    item {
                        TextButton(
                            onClick = { /* onBackClick or navigate to menu */ },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = WingZoneRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Add another order",
                                color = WingZoneRed,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                // Payment Method Section (only for non-lobby or individual payment lobbies)
                if (currentLobbyId == null || (lobbyPaymentMethod != "host-pays-all" && !userHasPaid)) {
                    item {
                        Text(
                            text = "Payment Method",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { /* Open payment method selector */ },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF1976D2)
                                    )
                                    Column {
                                        Text(
                                            text = "Online Banking",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "FPX / Payment Gateway",
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50)
                                )
                            }
                        }
                    }
                }
                
                // Order Type Selection (only for non-lobby orders)
                if (currentLobbyId == null) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Pickup Option
                            OrderTypeCard(
                                icon = Icons.Default.Info,
                                title = "For Pickup",
                                isSelected = selectedOrderType == "pickup",
                                onClick = { 
                                    selectedOrderType = if (selectedOrderType == "pickup") null else "pickup"
                                    if (selectedOrderType != "pickup") selectedBranch = null
                                },
                                content = {
                                    if (selectedOrderType == "pickup") {
                                        BranchDropdown(
                                            selectedBranch = selectedBranch,
                                            branches = branches,
                                            onBranchSelected = { selectedBranch = it }
                                        )
                                    }
                                }
                            )
                            
                            // Dine In Option
                            OrderTypeCard(
                                icon = Icons.Default.Home,
                                title = "For Dine In",
                                isSelected = selectedOrderType == "dinein",
                                onClick = { 
                                    selectedOrderType = if (selectedOrderType == "dinein") null else "dinein"
                                    if (selectedOrderType != "dinein") selectedBranch = null
                                },
                                content = {
                                    if (selectedOrderType == "dinein") {
                                        BranchDropdown(
                                            selectedBranch = selectedBranch,
                                            branches = branches,
                                            onBranchSelected = { selectedBranch = it }
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
                
                // Error Message
                if (orderError != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = orderError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(16.dp),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                
                // Place Order / Pay Button
                item {
                    val isInLobby = currentLobbyId != null
                    val isHostPaysAll = lobbyPaymentMethod == "host-pays-all"
                    
                    // Only show button if not host-pays-all and user hasn't paid
                    if (!isHostPaysAll && !userHasPaid) {
                        Button(
                            onClick = {
                                if (!isAuthenticated) {
                                    onAuthRequired()
                                } else if (currentUser?.isPhoneVerified == false) {
                                    showVerificationDialog = true
                                } else if (isInLobby) {
                                    // In lobby - mark as paid
                                    showCheckoutDialog = true
                                } else {
                                    // Not in lobby - validate and create order
                                    if (selectedOrderType == null) {
                                        orderError = "Please select Pickup or Dine In"
                                    } else if (selectedBranch == null) {
                                        orderError = "Please select a branch"
                                    } else {
                                        showCheckoutDialog = true
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = !isProcessingOrder,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WingZoneRed,
                                disabledContainerColor = Color.Gray
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            if (isProcessingOrder) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isInLobby) "Proceed to Payment" else "Proceed to Payment",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Bottom Spacer
                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
    
    // Checkout Dialog
    if (showCheckoutDialog) {
        val isInLobby = currentLobbyId != null
        
        AlertDialog(
            onDismissRequest = { if (!isProcessingOrder) showCheckoutDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = null,
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isProcessingOrder) {
                        // Processing State
                        Spacer(modifier = Modifier.height(16.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = WingZoneRed,
                            strokeWidth = 4.dp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = if (isInLobby) "Processing Payment..." else "Processing Order...",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Please wait while we confirm your payment",
                            fontSize = 14.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    } else if (orderSuccess) {
                        // Success State
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isInLobby) "Payment Confirmed!" else "Order Placed!",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isInLobby) {
                                "Your payment has been confirmed and recorded in the lobby."
                            } else {
                                "We've received your order and will prepare it right away!"
                            },
                            fontSize = 14.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    } else if (orderError != null) {
                        // Error State
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Payment Failed",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = orderError ?: "An error occurred",
                            fontSize = 14.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        // Confirmation State
                        Icon(
                            imageVector = if (isInLobby) Icons.Default.Check else Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = WingZoneRed,
                            modifier = Modifier
                                .size(64.dp)
                                .padding(8.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isInLobby) "Confirm Payment" else "Confirm Order",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Order Details Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Items",
                                        fontSize = 14.sp,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "${cart.totalItems} item${if (cart.totalItems > 1) "s" else ""}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    )
                                }
                                HorizontalDivider(color = Color(0xFFE0E0E0))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Total Amount",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "RM ${String.format("%.2f", cart.total)}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WingZoneRed
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Payment Method Info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF1976D2),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Payment via Online Banking (FPX)",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = if (isInLobby) {
                                "You'll be redirected to complete your payment"
                            } else {
                                "Complete payment to confirm your order"
                            },
                            fontSize= 12.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                if (!isProcessingOrder && !orderSuccess) {
                    Button(
                        onClick = {
                            isProcessingOrder = true
                            coroutineScope.launch {
                                if (isInLobby && currentLobbyId != null) {
                                    // Mark as paid in lobby
                                    lobbyViewModel?.markAsPaid(currentLobbyId, currentUser?.id ?: "") { result ->
                                        result.fold(
                                            onSuccess = {
                                                orderSuccess = true
                                                isProcessingOrder = false
                                                coroutineScope.launch {
                                                    kotlinx.coroutines.delay(1500)
                                                    showCheckoutDialog = false
                                                    orderSuccess = false
                                                    onPaymentComplete()
                                                }
                                            },
                                            onFailure = { error ->
                                                orderError = error.message
                                                isProcessingOrder = false
                                            }
                                        )
                                    }
                                } else {
                                    // Individual order (not in lobby)
                                    val result = orderRepository.createOrder(
                                        userId = currentUser?.id ?: "",
                                        userName = currentUser?.name ?: "Guest",
                                        cart = cart,
                                        paymentMethod = selectedPaymentMethod,
                                        phoneNumber = currentUser?.email,
                                        orderType = selectedOrderType,
                                        location = selectedBranch,
                                        lobbyPaymentMethod = null
                                    )
                                
                                    result.onSuccess { orderId ->
                                        orderSuccess = true
                                        placedOrderId = orderId
                                        // Clear cart and show notification
                                        kotlinx.coroutines.delay(500)
                                        cartViewModel.clearCart()
                                        showCheckoutDialog = false
                                        orderSuccess = false
                                        isProcessingOrder = false
                                        onOrderPlaced(orderId)
                                    }.onFailure { error ->
                                        orderError = error.message
                                        isProcessingOrder = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WingZoneRed)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isInLobby) "Proceed to Payment" else "Proceed to Pay",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else if (orderSuccess) {
                    Button(
                        onClick = {
                            showCheckoutDialog = false
                            orderSuccess = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("Done", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            dismissButton = {
                if (!isProcessingOrder && !orderSuccess) {
                    OutlinedButton(
                        onClick = { 
                            showCheckoutDialog = false
                            orderError = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                    ) {
                        Text("Cancel", fontSize = 16.sp)
                    }
                }
            }
        )
    }
    
    // Verification Required Dialog
    if (showVerificationDialog) {
        AlertDialog(
            onDismissRequest = { showVerificationDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = WingZoneRed,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "Verification Required",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Text(
                    text = "Please verify your phone number to place orders. This helps us ensure smooth delivery and contact you about your order.",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showVerificationDialog = false
                        // Navigate to account screen for verification
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WingZoneRed)
                ) {
                    Text("Verify Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVerificationDialog = false }) {
                    Text("Later", color = TextSecondary)
                }
            }
        )
    }
    
    // Remove Confirmation Dialog
    if (showRemoveDialog && itemToRemove != null) {
        AlertDialog(
            onDismissRequest = {
                showRemoveDialog = false
                itemToRemove = null
            },
            title = {
                Text(
                    "Remove Item?",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    "Are you sure you want to remove ${itemToRemove?.menuItem?.name} from your cart?",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        itemToRemove?.let { cartViewModel.removeItem(it.id) }
                        showRemoveDialog = false
                        itemToRemove = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = WingZoneRed
                    )
                ) {
                    Text("Remove", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRemoveDialog = false
                        itemToRemove = null
                    }
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun CartItemCardRedesigned(
    cartItem: CartItem,
    enabled: Boolean = true,
    onUpdateQuantity: (Int) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Item Image (Left)
            AsyncImage(
                model = cartItem.menuItem.imageUrl ?: "",
                contentDescription = cartItem.menuItem.name,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF5F5F5)),
                contentScale = ContentScale.Crop
            )
            
            // Quantity Controls + Item Details (Middle)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Item Name
                Text(
                    text = cartItem.menuItem.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) TextPrimary else Color.Gray
                )
                
                // Customization Details
                if (cartItem.customization != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        if (cartItem.customization.boneType != null) {
                            Text(
                                text = cartItem.customization.boneType.toString(),
                                fontSize = 12.sp,
                                color = if (enabled) TextSecondary else Color.LightGray
                            )
                        }
                        Text(
                            text = "Flavor: ${cartItem.customization.flavor.displayName}",
                            fontSize = 12.sp,
                            color = if (enabled) TextSecondary else Color.LightGray
                        )
                        Text(
                            text = "Sauce: ${cartItem.customization.dippingSauce.displayName}",
                            fontSize = 12.sp,
                            color = if (enabled) TextSecondary else Color.LightGray
                        )
                        if (cartItem.customization.drink.displayName != "None") {
                            Text(
                                text = "Drink: ${cartItem.customization.drink.displayName}",
                                fontSize = 12.sp,
                                color = if (enabled) TextSecondary else Color.LightGray
                            )
                        }
                    }
                }
                
                // Quantity Controls Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Decrease Button
                    IconButton(
                        onClick = { onUpdateQuantity(cartItem.quantity - 1) },
                        modifier = Modifier
                            .size(28.dp)
                            .border(1.dp, if (enabled) Color(0xFFE0E0E0) else Color(0xFFF0F0F0), CircleShape),
                        enabled = enabled
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Decrease",
                            tint = if (enabled) TextPrimary else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    Text(
                        text = cartItem.quantity.toString(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (enabled) TextPrimary else Color.Gray,
                        modifier = Modifier.widthIn(min = 20.dp),
                        textAlign = TextAlign.Center
                    )
                    
                    // Increase Button
                    IconButton(
                        onClick = { onUpdateQuantity(cartItem.quantity + 1) },
                        modifier = Modifier
                            .size(28.dp)
                            .border(1.dp, if (enabled) Color(0xFFE0E0E0) else Color(0xFFF0F0F0), CircleShape),
                        enabled = enabled
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase",
                            tint = if (enabled) TextPrimary else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            // Price and Remove Button (Right)
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onRemove,
                    enabled = enabled,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove item",
                        tint = if (enabled) Color.Gray else Color.LightGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "RM ${String.format("%.2f", cartItem.subtotal)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) TextPrimary else Color.Gray
                )
            }
        }
    }
}

@Composable
fun OrderTypeCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) WingZoneRed.copy(alpha = 0.1f) else Color.White
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, WingZoneRed)
        } else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Radio Button
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .border(
                                width = 2.dp,
                                color = if (isSelected) WingZoneRed else Color(0xFFE0E0E0),
                                shape = CircleShape
                            )
                            .background(if (isSelected) WingZoneRed else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                    }
                    
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isSelected) WingZoneRed else TextPrimary
                    )
                }
            }
            
            if (isSelected) {
                Spacer(modifier = Modifier.height(12.dp))
                content()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchDropdown(
    selectedBranch: String?,
    branches: List<String>,
    onBranchSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedBranch ?: "",
            onValueChange = {},
            readOnly = true,
            placeholder = { Text("Select Branch", fontSize = 14.sp) },
            trailingIcon = {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF5F5F5),
                unfocusedContainerColor = Color(0xFFF5F5F5),
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = WingZoneRed
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            branches.forEach { branch ->
                DropdownMenuItem(
                    text = { Text(branch) },
                    onClick = {
                        onBranchSelected(branch)
                        expanded = false
                    }
                )
            }
        }
    }
}

// Old components kept for backwards compatibility
@Composable
fun CartItemCard(
    cartItem: CartItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit,
    enabled: Boolean = true
) {
    CartItemCardRedesigned(
        cartItem = cartItem,
        enabled = enabled,
        onUpdateQuantity = { newQty ->
            if (newQty > cartItem.quantity) {
                onIncrement()
            } else if (newQty < cartItem.quantity) {
                onDecrement()
            }
        },
        onRemove = onRemove
    )
}

@Composable
fun CartSummaryCard(cart: wingzone.zenith.data.models.Cart) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Order Summary",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Subtotal", color = Color.Gray)
                Text(
                    "RM ${String.format("%.2f", cart.subtotal)}",
                    fontWeight = FontWeight.Medium
                )
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    "RM ${String.format("%.2f", cart.total)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = WingZoneRed
                )
            }
        }
    }
}
