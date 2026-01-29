package wingzone.zenith.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import wingzone.zenith.data.repository.Order
import wingzone.zenith.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    onBack: () -> Unit = {},
    onOrderClick: (String) -> Unit = {}
) {
    val firestore = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var selectedOrder by remember { mutableStateOf<Order?>(null) }
    var orderDetails by remember { mutableStateOf<Map<String, Any>>(emptyMap()) }

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

                    val ordersList = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            Order(
                                id = doc.id,
                                userId = doc.getString("userId") ?: "",
                                userName = doc.getString("userName") ?: "",
                                items = emptyList(),
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
                                phoneNumber = doc.getString("phoneNumber")
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
                        fontWeight = FontWeight.Bold,
                        color = WingZoneRed,
                        fontSize = 20.sp
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
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
            
            // Items count
            Text(
                text = "${order.items.size} items",
                fontSize = 14.sp,
                color = TextSecondary
            )
            
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
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
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
                    
                    // Close Button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 24.dp)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF212121)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "Close",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
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
