package wingzone.zenith.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    LaunchedEffect(orderId) {
        firestore.collection("orders")
            .document(orderId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    loading = false
                    return@addSnapshotListener
                }

                try {
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
                
                Spacer(modifier = Modifier.height(80.dp))
            }
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
