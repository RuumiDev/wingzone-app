package wingzone.zenith.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import com.google.firebase.firestore.Query
import wingzone.zenith.data.models.CartItem
import wingzone.zenith.data.models.MenuItem
import wingzone.zenith.data.repository.Order
import wingzone.zenith.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    onBack: () -> Unit = {},
    onOrderClick: (String) -> Unit = {},
    cartViewModel: wingzone.zenith.viewmodel.CartViewModel? = null
) {
    // Handle back button
    BackHandler {
        onBack()
    }
    
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var selectedOrder by remember { mutableStateOf<Order?>(null) }
    var orderDetails by remember { mutableStateOf<Map<String, Any>>(emptyMap()) }
    var showRatingSheet by remember { mutableStateOf(false) }
    var orderToRate by remember { mutableStateOf<Order?>(null) }

    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            firestore.collection("orders")
                .whereEqualTo("userId", currentUserId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        loading = false
                        return@addSnapshotListener
                    }

                    val ordersList = snapshot?.documents?.mapNotNull orderMapper@{ doc ->
                        try {
                            Order(
                                id = doc.id,
                                userId = doc.getString("userId") ?: "",
                                userName = doc.getString("userName") ?: "",
                                items = (doc.get("items") as? List<*>)?.mapNotNull itemMapper@{ itemData ->
                                    try {
                                        val itemMap = itemData as? Map<*, *> ?: return@itemMapper null
                                        val menuItemMap = itemMap["menuItem"] as? Map<*, *> ?: return@itemMapper null
                                        
                                        // Parse customization if present
                                        val customization = (itemMap["customization"] as? Map<*, *>)?.let { custMap ->
                                            try {
                                                Log.d("OrderHistory", "Parsing customization: $custMap")
                                                
                                                // Parse flavor
                                                val flavorStr = custMap["flavor"] as? String
                                                val flavor = flavorStr?.let { 
                                                    wingzone.zenith.data.models.Flavor.valueOf(it)
                                                } ?: wingzone.zenith.data.models.Flavor.BUFFALO_WING
                                                
                                                // Parse dipping sauce
                                                val sauceStr = custMap["dippingSauce"] as? String
                                                val dippingSauce = sauceStr?.let {
                                                    wingzone.zenith.data.models.DippingSauce.valueOf(it)
                                                } ?: wingzone.zenith.data.models.DippingSauce.RANCH
                                                
                                                // Parse drink
                                                val drinkStr = custMap["drink"] as? String
                                                val drink = drinkStr?.let {
                                                    wingzone.zenith.data.models.Drink.valueOf(it)
                                                } ?: wingzone.zenith.data.models.Drink.COCA_COLA
                                                
                                                // Parse bone type (optional)
                                                val boneTypeStr = custMap["boneType"] as? String
                                                val boneType = boneTypeStr?.let {
                                                    wingzone.zenith.data.models.BoneType.valueOf(it)
                                                }
                                                
                                                // Parse fries exchange (optional)
                                                val friesExchangeMap = custMap["friesExchange"] as? Map<*, *>
                                                val friesExchange = friesExchangeMap?.let { fryMap ->
                                                    wingzone.zenith.data.models.FriesExchangeOption(
                                                        name = fryMap["name"] as? String ?: "",
                                                        regularPrice = (fryMap["regularPrice"] as? Number)?.toDouble() ?: 0.0,
                                                        jumboPrice = (fryMap["jumboPrice"] as? Number)?.toDouble(),
                                                        selectedSize = fryMap["selectedSize"] as? String ?: "regular",
                                                        selectedFlavor = fryMap["selectedFlavor"] as? String
                                                    )
                                                }
                                                
                                                // Parse salad type (optional)
                                                val saladType = custMap["saladType"] as? String
                                                
                                                val result = wingzone.zenith.data.models.EntreeCustomization(
                                                    flavor = flavor,
                                                    dippingSauce = dippingSauce,
                                                    drink = drink,
                                                    boneType = boneType,
                                                    friesExchange = friesExchange,
                                                    saladType = saladType
                                                )
                                                Log.d("OrderHistory", "Parsed customization successfully: flavor=${flavor.displayName}, drink=${drink.displayName}")
                                                result
                                            } catch (e: Exception) {
                                                Log.e("OrderHistory", "Error parsing customization: ${e.message}", e)
                                                null
                                            }
                                        }
                                        
                                        Log.d("OrderHistory", "Parsing MenuItem: name=${menuItemMap["name"]}, imageUrl=${menuItemMap["imageUrl"]}, customization=$customization")
                                        
                                        // Parse customizationOptions
                                        val customizationOptions = (menuItemMap["customizationOptions"] as? Map<*, *>)?.let { optsMap ->
                                            wingzone.zenith.data.models.CustomizationOptions(
                                                requiresFlavor = optsMap["requiresFlavor"] as? Boolean ?: false,
                                                requiresBeverage = optsMap["requiresBeverage"] as? Boolean ?: false,
                                                requiresDippingSauce = optsMap["requiresDippingSauce"] as? Boolean ?: false,
                                                requiresBoneType = optsMap["requiresBoneType"] as? Boolean ?: false,
                                                allowFriesExchange = optsMap["allowFriesExchange"] as? Boolean ?: false,
                                                requiresSaladChoice = optsMap["requiresSaladChoice"] as? Boolean ?: false
                                            )
                                        }
                                        
                                        CartItem(
                                            menuItem = MenuItem(
                                                id = menuItemMap["id"] as? String ?: "",
                                                name = menuItemMap["name"] as? String ?: "",
                                                description = menuItemMap["description"] as? String ?: "",
                                                price = (menuItemMap["price"] as? Number)?.toDouble() ?: 0.0,
                                                category = menuItemMap["category"] as? String ?: "",
                                                imageUrl = menuItemMap["imageUrl"] as? String,
                                                customizationOptions = customizationOptions
                                            ),
                                            quantity = (itemMap["quantity"] as? Number)?.toInt() ?: 1,
                                            customization = customization,
                                            specialInstructions = itemMap["specialInstructions"] as? String
                                        )
                                    } catch (e: Exception) {
                                        null
                                    }
                                } ?: emptyList(),
                                subtotal = (doc.get("subtotal") as? Number)?.toDouble() ?: 0.0,
                                tax = (doc.get("tax") as? Number)?.toDouble() ?: 0.0,
                                total = (doc.get("total") as? Number)?.toDouble() ?: 0.0,
                                status = doc.getString("status") ?: "pending",
                                paymentStatus = doc.getString("paymentStatus") ?: "unpaid",
                                paymentMethod = doc.getString("paymentMethod") ?: "cash",
                                createdAt = (doc.get("createdAt") as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
                                updatedAt = (doc.get("updatedAt") as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
                                deliveryAddress = doc.getString("deliveryAddress"),
                                deliveryNotes = doc.getString("deliveryNotes"),
                                phoneNumber = doc.getString("phoneNumber"),
                                ratedAt = (doc.get("ratedAt") as? com.google.firebase.Timestamp)?.toDate(),
                                rating = (doc.get("rating") as? Number)?.toInt()
                            )
                        } catch (e: Exception) {
                            null
                        }
                    } ?: emptyList()

                    orders = ordersList
                    loading = false
                }
        } else {
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Order History",
                        style = MaterialTheme.typography.titleLarge.copy(color = WingZoneRed)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = WingZoneRed
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        containerColor = BackgroundGray
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = WingZoneRed
                    )
                }
                orders.isEmpty() -> {
                    EmptyOrderHistoryState()
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(orders) { order ->
                            OrderHistoryCard(
                                order = order,
                                onClick = {
                                    // Fetch full order details including orderType and location
                                    firestore.collection("orders")
                                        .document(order.id)
                                        .get()
                                        .addOnSuccessListener { doc ->
                                            if (doc.exists()) {
                                                orderDetails = doc.data ?: emptyMap()
                                                selectedOrder = order
                                            }
                                        }
                                },
                                onRateClick = {
                                    orderToRate = order
                                    showRatingSheet = true
                                },
                                onReorder = { items ->
                                    Log.d("OrderHistory", "Re-order clicked. Items count: ${items.size}")
                                    Log.d("OrderHistory", "CartViewModel is null? ${cartViewModel == null}")
                                    
                                    if (cartViewModel == null) {
                                        Log.e("OrderHistory", "CartViewModel is null!")
                                        Toast.makeText(context, "Unable to add items to cart", Toast.LENGTH_SHORT).show()
                                        return@OrderHistoryCard
                                    }
                                    
                                    if (items.isEmpty()) {
                                        Log.w("OrderHistory", "No items to re-order")
                                        Toast.makeText(context, "No items to re-order", Toast.LENGTH_SHORT).show()
                                        return@OrderHistoryCard
                                    }
                                    
                                    items.forEach { item ->
                                        Log.d("OrderHistory", "Adding item: ${item.menuItem.name} x${item.quantity}")
                                        cartViewModel.addItem(item.menuItem, item.quantity, item.customization)
                                    }
                                    
                                    Toast.makeText(
                                        context, 
                                        "${items.size} item(s) added to cart", 
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    
                                    Log.d("OrderHistory", "Re-order completed successfully")
                                }
                            )
                        }
                    }
                }
            }
        }
        
        // Digital Proof Dialog
        selectedOrder?.let { order ->
            OrderProofDialog(
                order = order,
                orderDetails = orderDetails,
                onDismiss = { selectedOrder = null }
            )
        }
        
        // Rating Bottom Sheet
        if (showRatingSheet && orderToRate != null) {
            RatingBottomSheet(
                order = orderToRate!!,
                onDismiss = { 
                    showRatingSheet = false
                    orderToRate = null
                },
                onSubmit = { rating, comment ->
                    showRatingSheet = false
                    orderToRate = null
                }
            )
        }
    }
}

@Composable
fun EmptyOrderHistoryState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ShoppingCart,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color.Gray.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No orders yet",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your order history will appear here",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun OrderHistoryCard(
    order: Order,
    onClick: () -> Unit,
    onRateClick: () -> Unit = {},
    onReorder: (List<wingzone.zenith.data.models.CartItem>) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Order ID and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Order #${order.id.take(8).uppercase()}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                OrderStatusBadge(status = order.status)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Items count - clickable to expand/collapse
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${order.items.size} items",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            // Expandable items list
            androidx.compose.animation.AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    order.items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${item.quantity}x ${item.menuItem.name}",
                                fontSize = 13.sp,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "RM ${String.format("%.2f", item.menuItem.price * item.quantity)}",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        if (item.specialInstructions != null && item.specialInstructions.isNotBlank()) {
                            Text(
                                text = "Note: ${item.specialInstructions}",
                                fontSize = 11.sp,
                                color = TextSecondary.copy(alpha = 0.7f),
                                modifier = Modifier.padding(start = 16.dp, top = 2.dp)
                            )
                        }
                    }
                    
                    // Re-order button
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onReorder(order.items) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = WingZoneRed
                        ),
                        border = BorderStroke(1.dp, WingZoneRed)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Re-order",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Re-order",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Total and Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Total",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "RM ${String.format("%.2f", order.total)}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = WingZoneRed
                    )
                }
                
                val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
                Text(
                    text = dateFormat.format(order.createdAt),
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            
            // Rate Order Button (show only for delivered orders that haven't been rated)
            if (order.status.lowercase() == "delivered" && order.ratedAt == null) {
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedButton(
                    onClick = { onRateClick() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = WingZoneRed
                    ),
                    border = BorderStroke(1.dp, WingZoneRed)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = "Rate",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Rate Order",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Show rating if already rated
            if (order.ratedAt != null && order.rating != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(5) { index ->
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Rounded.Star,
                            contentDescription = null,
                            tint = if (index < order.rating) Color(0xFFFFB300) else Color(0xFFE0E0E0),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "You rated this order",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun OrderStatusBadge(status: String) {
    val (backgroundColor, textColor, displayText) = when (status.lowercase()) {
        "pending" -> Triple(Color(0xFFFFF3E0), Color(0xFFFF8F00), "PENDING")
        "confirmed" -> Triple(Color(0xFFE3F2FD), Color(0xFF1976D2), "CONFIRMED")
        "preparing" -> Triple(Color(0xFFFCE4EC), Color(0xFFE91E63), "PREPARING")
        "ready" -> Triple(Color(0xFFF3E5F5), Color(0xFF9C27B0), "READY")
        "delivered" -> Triple(Color(0xFFE8F5E9), Color(0xFF4CAF50), "DELIVERED")
        "cancelled" -> Triple(Color(0xFFFFEBEE), Color(0xFFF44336), "CANCELLED")
        else -> Triple(Color(0xFFF5F5F5), Color(0xFF757575), status.uppercase())
    }
    
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
    ) {
        Text(
            text = displayText,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

/**
 * Modern Digital Proof Dialog - Like a digital ticket/boarding pass
 */
@Composable
fun OrderProofDialog(
    order: Order,
    orderDetails: Map<String, Any>,
    onDismiss: () -> Unit
) {
    val orderType = (orderDetails["orderType"] as? String) ?: "Pickup"
    val location = (orderDetails["location"] as? String) ?: "Wingzone"
    val paymentMethod = order.paymentMethod
    val paymentStatus = order.paymentStatus

    var showConfirmation by remember { mutableStateOf(false) }

    // Intercept hardware back button — require explicit confirmation
    BackHandler { showConfirmation = true }

    Dialog(
        onDismissRequest = { /* non-dismissible — user must tap 'Mark as Collected' */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Header with gradient
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        WingZoneRed,
                                        Color(0xFFD32F2F)
                                    )
                                )
                            )
                            .padding(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "ORDER VERIFIED",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Show this to the counter",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                    
                    // Order Code - Large and prominent
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF5F5F5))
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "ORDER CODE",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "#${order.id.take(8).uppercase()}",
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Bold,
                                color = WingZoneRed,
                                letterSpacing = 4.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            // Visual barcode-like element
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                repeat(20) { index ->
                                    Box(
                                        modifier = Modifier
                                            .width(if (index % 3 == 0) 3.dp else 2.dp)
                                            .height(if (index % 2 == 0) 32.dp else 24.dp)
                                            .background(
                                                if (index % 4 == 0) WingZoneRed else Color(
                                                    0xFF666666
                                                )
                                            )
                                    )
                                }
                            }
                        }
                    }
                    
                    // Order Details Grid
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        // Status Row
                        ProofDetailRow(
                            label = "STATUS",
                            value = order.status.uppercase(),
                            valueColor = when (order.status.lowercase()) {
                                "ready", "delivered" -> Color(0xFF4CAF50)
                                "preparing" -> Color(0xFFFF9800)
                                else -> TextPrimary
                            },
                            prominent = true
                        )
                        
                        Divider(modifier = Modifier.padding(vertical = 16.dp))
                        
                        // Payment Info
                        ProofDetailRow(
                            label = "PAYMENT",
                            value = paymentStatus.uppercase(),
                            valueColor = if (paymentStatus.equals(
                                    "paid",
                                    ignoreCase = true
                                )
                            ) Color(0xFF4CAF50) else Color(0xFFFF9800)
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        ProofDetailRow(
                            label = "METHOD",
                            value = paymentMethod.uppercase()
                        )
                        
                        Divider(modifier = Modifier.padding(vertical = 16.dp))
                        
                        // Service Type
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "SERVICE",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = orderType.uppercase(),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            
                            Icon(
                                imageVector = if (orderType.contains(
                                        "dine",
                                        ignoreCase = true
                                    )
                                ) Icons.Default.Person else Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = WingZoneRed,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Location
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Color(0xFFFFF3E0),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = WingZoneRed,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "LOCATION",
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = location,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Items Section
                        Text(
                            text = "ORDER ITEMS",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Display all items with details
                        @Suppress("UNCHECKED_CAST")
                        val items = orderDetails["items"] as? List<Map<String, Any>> ?: emptyList()
                        
                        items.forEach { item ->
                            val menuItem = item["menuItem"] as? Map<String, Any>
                            val quantity = (item["quantity"] as? Number)?.toInt() ?: 1
                            val customization = item["customization"] as? Map<String, Any>
                            val specialInstructions = item["specialInstructions"] as? String
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFF9F9F9)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    // Item name and quantity
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${quantity}x ${menuItem?.get("name") as? String ?: "Item"}",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "RM ${String.format("%.2f", ((menuItem?.get("price") as? Number)?.toDouble() ?: 0.0) * quantity)}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = WingZoneRed
                                        )
                                    }
                                    
                                    // Customizations
                                    if (customization != null) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        
                                        val details = buildList {
                                            (customization["flavor"] as? String)?.let { flavor ->
                                                if (flavor != "None") add("🔥 $flavor")
                                            }
                                            (customization["drink"] as? String)?.let { drink ->
                                                if (drink != "None") add("🥤 $drink")
                                            }
                                            (customization["dippingSauce"] as? String)?.let { sauce ->
                                                if (sauce != "None") add("🍯 $sauce")
                                            }
                                            (customization["friesExchange"] as? Map<String, Any>)?.let { fries ->
                                                val friesName = fries["name"] as? String
                                                val friesSize = fries["selectedSize"] as? String
                                                if (friesName != "None" && friesName != null) {
                                                    add("🍟 $friesName${if (friesSize != null) " ($friesSize)" else ""}")
                                                }
                                            }
                                            (customization["boneType"] as? String)?.let { bone ->
                                                if (bone != "None") add("🦴 $bone")
                                            }
                                            (customization["saladType"] as? String)?.let { salad ->
                                                if (salad != "None") add("🥗 $salad")
                                            }
                                        }
                                        
                                        details.forEach { detail ->
                                            Text(
                                                text = "  • $detail",
                                                fontSize = 13.sp,
                                                color = Color(0xFF666666),
                                                modifier = Modifier.padding(vertical = 2.dp)
                                            )
                                        }
                                    }
                                    
                                    // Special instructions
                                    if (!specialInstructions.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "📝 $specialInstructions",
                                            fontSize = 12.sp,
                                            color = Color(0xFF999999),
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Total Amount
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 2.dp,
                                    color = WingZoneRed,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "TOTAL AMOUNT",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "RM ${String.format("%.2f", order.total)}",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WingZoneRed
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Order Time
                        val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
                        Text(
                            text = dateFormat.format(order.createdAt),
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                    
                    // Mark as Collected Button — requires confirmation before closing
                    Button(
                        onClick = { showConfirmation = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 24.dp)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Mark as Collected",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Confirmation dialog — shown when user taps 'Mark as Collected'
                    if (showConfirmation) {
                        AlertDialog(
                            onDismissRequest = { showConfirmation = false },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(32.dp)
                                )
                            },
                            title = {
                                Text(
                                    text = "Confirm Collection",
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            },
                            text = {
                                Text(
                                    text = "Have you shown this receipt to the staff?",
                                    textAlign = TextAlign.Center
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showConfirmation = false
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                ) {
                                    Text("Yes, Done", fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                OutlinedButton(onClick = { showConfirmation = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProofDetailRow(
    label: String,
    value: String,
    valueColor: Color = TextPrimary,
    prominent: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = if (prominent) 12.sp else 11.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = if (prominent) 20.sp else 16.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

/**
 * Rating Bottom Sheet - Similar to Grab's rating modal
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatingBottomSheet(
    order: Order,
    onDismiss: () -> Unit,
    onSubmit: (rating: Int, comment: String) -> Unit
) {
    var selectedRating by remember { mutableIntStateOf(0) }
    var comment by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }
    val reviewRepository = remember { wingzone.zenith.data.repository.FirebaseReviewRepository() }
    val coroutineScope = rememberCoroutineScope()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFE0E0E0))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Title
            Text(
                text = "How were your wings?",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Star Rating
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(5) { index ->
                    val starIndex = index + 1
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Rounded.Star,
                        contentDescription = "Star $starIndex",
                        tint = if (selectedRating >= starIndex) Color(0xFFFFB300) else Color(0xFFE0E0E0),
                        modifier = Modifier
                            .size(48.dp)
                            .clickable { selectedRating = starIndex }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Comment TextField
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                placeholder = {
                    Text(
                        text = "Tell us what you liked...",
                        color = Color(0xFF9E9E9E)
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WingZoneRed,
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    cursorColor = WingZoneRed
                ),
                shape = RoundedCornerShape(12.dp),
                maxLines = 4
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            // Error message
            if (submitError != null) {
                Text(
                    text = submitError!!,
                    color = Color(0xFFDC2626),
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )
            }

            // Submit Button
            Button(
                onClick = {
                    if (selectedRating > 0 && !isSubmitting) {
                        isSubmitting = true
                        submitError = null
                        coroutineScope.launch {
                            try {
                                val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                                val reviewUserId = firebaseUser?.uid ?: order.userId
                                val reviewUserName = (firebaseUser?.displayName
                                    ?.takeIf { it.isNotBlank() && it != "User" }
                                    ?: order.userName.takeIf { it.isNotBlank() && it != "Guest" }
                                    ?: firebaseUser?.email?.substringBefore('@')
                                    ?: "Customer")
                                val result = reviewRepository.submitReview(
                                    orderId = order.id,
                                    userId = reviewUserId,
                                    userName = reviewUserName,
                                    rating = selectedRating,
                                    comment = comment,
                                    menuItemIds = order.items.map { it.menuItem.id }
                                )
                                
                                if (result.isSuccess) {
                                    onSubmit(selectedRating, comment)
                                } else {
                                    android.util.Log.e("ReviewSubmit", "Failed: ${result.exceptionOrNull()?.message}")
                                    submitError = "Could not submit review. Please try again."
                                    isSubmitting = false
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("ReviewSubmit", "Exception: ${e.message}", e)
                                submitError = "Could not submit review. Please try again."
                                isSubmitting = false
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = selectedRating > 0 && !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = WingZoneRed,
                    disabledContainerColor = Color(0xFFE0E0E0)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Submit Review",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
