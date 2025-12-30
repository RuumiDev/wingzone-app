package wingzone.zenith.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import wingzone.zenith.data.models.CartItem
import wingzone.zenith.data.repository.FirebaseOrderRepository
import wingzone.zenith.ui.theme.*
import wingzone.zenith.viewmodel.AuthViewModel
import wingzone.zenith.viewmodel.CartViewModel
import kotlinx.coroutines.launch

// Helper Composables defined before main screen
@Composable
fun CartBottomBar(
    cart: wingzone.zenith.data.models.Cart,
    onCheckoutClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${cart.totalItems} items",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "RM ${String.format("%.2f", cart.total)}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = WingZoneRed
                )
            }
            
            Button(
                onClick = onCheckoutClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = WingZoneRed
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(56.dp)
            ) {
                Text(
                    "Checkout",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    cartViewModel: CartViewModel = CartViewModel(),
    authViewModel: AuthViewModel = AuthViewModel(),
    onAuthRequired: () -> Unit = {},
    onCheckoutClick: () -> Unit = {},
    onOrderPlaced: (String) -> Unit = {}
) {
    val cart by cartViewModel.cart.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val isAuthenticated = authViewModel.isAuthenticated()
    val coroutineScope = rememberCoroutineScope()
    val orderRepository = remember { FirebaseOrderRepository() }
    
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var showVerificationDialog by remember { mutableStateOf(false) }
    var isProcessingOrder by remember { mutableStateOf(false) }
    var orderSuccess by remember { mutableStateOf(false) }
    var orderError by remember { mutableStateOf<String?>(null) }
    var placedOrderId by remember { mutableStateOf<String?>(null) }
    var showOrderNotification by remember { mutableStateOf(false) }
    
    // Redirect to auth if trying to access cart without login
    LaunchedEffect(cart.totalItems) {
        if (cart.totalItems > 0 && !isAuthenticated) {
            onAuthRequired()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "My Cart",
                        fontWeight = FontWeight.Bold,
                        color = WingZoneRed,
                        fontSize = 24.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        bottomBar = {
            if (cart.totalItems > 0) {
                CartBottomBar(
                    cart = cart,
                    onCheckoutClick = {
                        if (!isAuthenticated) {
                            onAuthRequired()
                        } else if (currentUser?.isPhoneVerified == false) {
                            showVerificationDialog = true
                        } else {
                            showCheckoutDialog = true
                        }
                    }
                )
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
                items(cart.items) { cartItem ->
                    CartItemCard(
                        cartItem = cartItem,
                        onIncrement = {
                            cartViewModel.updateQuantity(cartItem.id, cartItem.quantity + 1)
                        },
                        onDecrement = {
                            if (cartItem.quantity > 1) {
                                cartViewModel.updateQuantity(cartItem.id, cartItem.quantity - 1)
                            } else {
                                cartViewModel.removeItem(cartItem.id)
                            }
                        },
                        onRemove = {
                            cartViewModel.removeItem(cartItem.id)
                        }
                    )
                }
                
                // Summary Card
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    CartSummaryCard(cart = cart)
                }
                
                // Proceed to Checkout Button
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (!isAuthenticated) {
                                onAuthRequired()
                            } else {
                                showCheckoutDialog = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WingZoneRed
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Proceed to Checkout",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
    
    // Checkout Dialog
    if (showCheckoutDialog) {
        AlertDialog(
            onDismissRequest = { if (!isProcessingOrder) showCheckoutDialog = false },
            title = { Text("Confirm Order") },
            text = {
                Column {
                    if (isProcessingOrder) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = WingZoneRed
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Processing order...")
                        }
                    } else if (orderSuccess) {
                        Text(
                            "Order placed successfully! We'll prepare it right away.",
                            color = Color(0xFF4CAF50)
                        )
                    } else if (orderError != null) {
                        Text(
                            "Error: $orderError",
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text("Place order for ${cart.totalItems} items?\n\nTotal: RM ${String.format("%.2f", cart.total)}")
                    }
                }
            },
            confirmButton = {
                if (!isProcessingOrder && !orderSuccess) {
                    Button(
                        onClick = {
                            isProcessingOrder = true
                            coroutineScope.launch {
                                val result = orderRepository.createOrder(
                                    userId = currentUser?.id ?: "",
                                    userName = currentUser?.name ?: "Guest",
                                    cart = cart,
                                    paymentMethod = "cash",
                                    phoneNumber = currentUser?.email
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
                                    showOrderNotification = true
                                }.onFailure { error ->
                                    orderError = error.message
                                    isProcessingOrder = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WingZoneRed)
                    ) {
                        Text("Confirm Order")
                    }
                } else if (orderSuccess) {
                    Button(
                        onClick = {
                            showCheckoutDialog = false
                            orderSuccess = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WingZoneRed)
                    ) {
                        Text("Done")
                    }
                }
            },
            dismissButton = {
                if (!isProcessingOrder && !orderSuccess) {
                    TextButton(onClick = { showCheckoutDialog = false }) {
                        Text("Cancel")
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
}

@Composable
fun CartItemCard(
    cartItem: CartItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = cartItem.menuItem.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                // Customization Details
                if (cartItem.customization != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Flavor: ${cartItem.customization.flavor.displayName}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "Sauce: ${cartItem.customization.dippingSauce.displayName}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        if (cartItem.customization.drink.displayName != "None") {
                            Text(
                                text = "Drink: ${cartItem.customization.drink.displayName}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
                
                // Quantity Controls
                Row(
                    modifier = Modifier
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onDecrement,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Decrease quantity",
                            tint = WingZoneRed
                        )
                    }
                    
                    Text(
                        text = cartItem.quantity.toString(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.widthIn(min = 30.dp),
                        textAlign = TextAlign.Center
                    )
                    
                    IconButton(
                        onClick = onIncrement,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowUp,
                            contentDescription = "Increase quantity",
                            tint = WingZoneRed
                        )
                    }
                }
            }
            
            // Price and Remove Button
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove item",
                        tint = Color.Gray
                    )
                }
                
                Text(
                    text = "RM ${String.format("%.2f", cartItem.subtotal)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = WingZoneRed
                )
            }
        }
    }
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
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
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
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
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
