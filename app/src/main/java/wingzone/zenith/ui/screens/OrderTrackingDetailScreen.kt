package wingzone.zenith.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.firestore.FirebaseFirestore
import wingzone.zenith.data.repository.Order
import wingzone.zenith.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Grab-style Order Tracking Screen
 * Shows real-time order status updates
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderTrackingDetailScreen(
    orderId: String,
    onBack: () -> Unit = {}
) {
    val firestore = FirebaseFirestore.getInstance()
    var order by remember { mutableStateOf<Order?>(null) }
    var loading by remember { mutableStateOf(true) }
    var showProofDialog by remember { mutableStateOf(false) }
    var orderDetails by remember { mutableStateOf<Map<String, Any>>(emptyMap()) }

    LaunchedEffect(orderId) {
        firestore.collection("orders")
            .document(orderId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    loading = false
                    return@addSnapshotListener
                }

                try {
                    orderDetails = snapshot.data ?: emptyMap()
                    order = Order(
                        id = snapshot.id,
                        userId = snapshot.getString("userId") ?: "",
                        userName = snapshot.getString("userName") ?: "",
                        items = emptyList(),
                        subtotal = (snapshot.get("subtotal") as? Number)?.toDouble() ?: 0.0,
                        tax = (snapshot.get("tax") as? Number)?.toDouble() ?: 0.0,
                        total = (snapshot.get("total") as? Number)?.toDouble() ?: 0.0,
                        status = snapshot.getString("status") ?: "pending",
                        paymentStatus = snapshot.getString("paymentStatus") ?: "unpaid",
                        paymentMethod = snapshot.getString("paymentMethod") ?: "cash",
                        createdAt = (snapshot.get("createdAt") as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
                        updatedAt = (snapshot.get("updatedAt") as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
                        deliveryAddress = snapshot.getString("deliveryAddress"),
                        deliveryNotes = snapshot.getString("deliveryNotes"),
                        phoneNumber = snapshot.getString("phoneNumber")
                    )
                    loading = false
                } catch (e: Exception) {
                    loading = false
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Track Order",
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
        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = WingZoneRed)
            }
        } else if (order == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Order not found", color = TextSecondary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // Status Header Card
                StatusHeaderCard(order!!)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Order Progress Timeline
                OrderProgressTimeline(order!!)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Order Details Card
                OrderDetailsCard(order!!)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // "I'm Here" Button - Show when order is ready
                if (order!!.status.equals("ready", ignoreCase = true)) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = WingZoneRed,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Ready to collect?",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Show your order proof to the counter",
                                fontSize = 14.sp,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showProofDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = WingZoneRed),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "I'm Here - Show Proof",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
        
        // Show Digital Proof Dialog
        if (showProofDialog && order != null) {
            OrderPickupProofDialog(
                order = order!!,
                orderDetails = orderDetails,
                onDismiss = { showProofDialog = false }
            )
        }
    }
}

@Composable
fun StatusHeaderCard(order: Order) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status Icon
            val (icon, iconColor) = getStatusIconAndColor(order.status)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(iconColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Status Text
            Text(
                text = getStatusDisplayText(order.status),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = getStatusDescription(order.status),
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Order ID
            Text(
                text = "Order #${order.id.take(8).uppercase()}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = WingZoneRed
            )
        }
    }
}

@Composable
fun OrderProgressTimeline(order: Order) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Order Progress",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            val steps = listOf(
                Triple("Order Placed", "pending", Icons.Default.ShoppingCart),
                Triple("Confirmed", "confirmed", Icons.Default.CheckCircle),
                Triple("Preparing", "preparing", Icons.Default.Build),
                Triple("Ready", "ready", Icons.Default.Done),
                Triple("Delivered", "delivered", Icons.Default.CheckCircle)
            )
            
            steps.forEachIndexed { index, (title, status, icon) ->
                TimelineStep(
                    title = title,
                    icon = icon,
                    isCompleted = isStepCompleted(order.status, status),
                    isActive = order.status.equals(status, ignoreCase = true),
                    isLast = index == steps.size - 1
                )
            }
        }
    }
}

@Composable
fun TimelineStep(
    title: String,
    icon: ImageVector,
    isCompleted: Boolean,
    isActive: Boolean,
    isLast: Boolean
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "timeline_scale"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isCompleted || isActive -> WingZoneRed
            else -> Color(0xFFE0E0E0)
        },
        animationSpec = tween(durationMillis = 600),
        label = "timeline_background"
    )
    
    val lineProgress by animateFloatAsState(
        targetValue = if (isCompleted) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "line_progress"
    )
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Timeline indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .background(
                        color = backgroundColor,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCompleted) Icons.Default.Check else icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(48.dp)
                        .background(color = Color(0xFFE0E0E0))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(lineProgress)
                            .background(color = WingZoneRed)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Step title
        Column(
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                color = if (isCompleted || isActive) TextPrimary else TextSecondary
            )
            
            if (isActive) {
                Text(
                    text = "In Progress",
                    fontSize = 12.sp,
                    color = WingZoneRed,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun OrderDetailsCard(order: Order) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Order Details",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            DetailRow("Total Amount", "RM ${String.format("%.2f", order.total)}")
            Spacer(modifier = Modifier.height(12.dp))
            
            DetailRow("Payment Method", order.paymentMethod.capitalize())
            Spacer(modifier = Modifier.height(12.dp))
            
            DetailRow("Payment Status", order.paymentStatus.capitalize())
            Spacer(modifier = Modifier.height(12.dp))
            
            val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
            DetailRow("Order Time", dateFormat.format(order.createdAt))
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = TextSecondary
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
    }
}

// Helper functions
fun getStatusIconAndColor(status: String): Pair<ImageVector, Color> {
    return when (status.lowercase()) {
        "pending" -> Pair(Icons.Default.Info, Color(0xFFFFA726))
        "confirmed" -> Pair(Icons.Default.CheckCircle, Color(0xFF42A5F5))
        "preparing" -> Pair(Icons.Default.Build, Color(0xFFFF7043))
        "ready" -> Pair(Icons.Default.Done, Color(0xFF66BB6A))
        "delivered" -> Pair(Icons.Default.CheckCircle, Color(0xFF4CAF50))
        "cancelled" -> Pair(Icons.Default.Close, Color(0xFFEF5350))
        else -> Pair(Icons.Default.Info, Color.Gray)
    }
}

fun getStatusDisplayText(status: String): String {
    return when (status.lowercase()) {
        "pending" -> "Order Received"
        "confirmed" -> "Order Confirmed"
        "preparing" -> "Preparing Your Food"
        "ready" -> "Ready for Pickup"
        "delivered" -> "Delivered"
        "cancelled" -> "Order Cancelled"
        else -> status.capitalize()
    }
}

fun getStatusDescription(status: String): String {
    return when (status.lowercase()) {
        "pending" -> "We've received your order and will confirm it shortly"
        "confirmed" -> "Your order has been confirmed and will be prepared soon"
        "preparing" -> "Our chefs are preparing your delicious meal"
        "ready" -> "Your order is ready! Come pick it up"
        "delivered" -> "Hope you enjoyed your meal!"
        "cancelled" -> "This order has been cancelled"
        else -> ""
    }
}

fun isStepCompleted(currentStatus: String, stepStatus: String): Boolean {
    val statusOrder = listOf("pending", "confirmed", "preparing", "ready", "delivered")
    val currentIndex = statusOrder.indexOf(currentStatus.lowercase())
    val stepIndex = statusOrder.indexOf(stepStatus.lowercase())
    return currentIndex > stepIndex
}

/**
 * Modern Pickup Proof Dialog - Shown when user clicks "I'm Here"
 */
@Composable
fun OrderPickupProofDialog(
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
                .background(Color.Black.copy(alpha = 0.9f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Animated Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        WingZoneRed,
                                        Color(0xFFB71C1C)
                                    )
                                )
                            )
                            .padding(32.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Pulsing checkmark
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .background(
                                        Color.White.copy(alpha = 0.2f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Done,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(56.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "READY FOR PICKUP",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Show this screen to the counter staff",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.95f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                    
                    // Large Order Code
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFAFAFA))
                            .padding(vertical = 28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "ORDER NUMBER",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.5.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "#${order.id.take(8).uppercase()}",
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Black,
                                color = WingZoneRed,
                                letterSpacing = 6.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            // Enhanced barcode visual
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                repeat(24) { index ->
                                    Box(
                                        modifier = Modifier
                                            .width(if (index % 4 == 0) 4.dp else if (index % 3 == 0) 3.dp else 2.dp)
                                            .height(if (index % 2 == 0) 40.dp else 28.dp)
                                            .background(
                                                if (index % 5 == 0) WingZoneRed else Color(
                                                    0xFF424242
                                                ),
                                                RoundedCornerShape(1.dp)
                                            )
                                    )
                                }
                            }
                        }
                    }
                    
                    // Verification Details
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp)
                    ) {
                        // Payment Status - Prominent
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (paymentStatus.equals(
                                            "paid",
                                            ignoreCase = true
                                        )
                                    ) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                                    RoundedCornerShape(16.dp)
                                )
                                .border(
                                    width = 2.dp,
                                    color = if (paymentStatus.equals(
                                            "paid",
                                            ignoreCase = true
                                        )
                                    ) Color(0xFF4CAF50) else Color(0xFFFF9800),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "PAYMENT STATUS",
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = paymentStatus.uppercase(),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (paymentStatus.equals(
                                                "paid",
                                                ignoreCase = true
                                            )
                                        ) Color(0xFF2E7D32) else Color(0xFFE65100)
                                    )
                                    Text(
                                        text = "via ${paymentMethod.uppercase()}",
                                        fontSize = 13.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Icon(
                                    imageVector = if (paymentStatus.equals(
                                            "paid",
                                            ignoreCase = true
                                        )
                                    ) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (paymentStatus.equals(
                                            "paid",
                                            ignoreCase = true
                                        )
                                    ) Color(0xFF4CAF50) else Color(0xFFFF9800),
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        HorizontalDivider(color = Color(0xFFE0E0E0))
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Service Type & Location
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Service Type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        Color(0xFFF5F5F5),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "SERVICE",
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = orderType.uppercase(),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                            }
                            
                            // Total
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        Color(0xFFF5F5F5),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "TOTAL",
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "RM ${String.format("%.2f", order.total)}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WingZoneRed
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Location
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Color(0xFFFFF8E1),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFFFF6F00),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "BRANCH",
                                        fontSize = 10.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = location,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Timestamp
                        val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
                        Text(
                            text = "Order placed: ${dateFormat.format(order.createdAt)}",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                    
                    // Close Button - Visible on all screens
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 24.dp, top = 16.dp)
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238)),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
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
