package wingzone.zenith.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import wingzone.zenith.R
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

    // Handle back button
    BackHandler {
        onBack()
    }

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
        containerColor = Color(0xFFFAF8F5) // Cream/off-white background
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
                // Check if order is awaiting counter payment
                if (order!!.status.equals("Pending Payment", ignoreCase = true)) {
                    // Show E-Receipt Card for cash payment orders
                    wingzone.zenith.ui.components.EReceiptCard(
                        order = order!!,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // Standard order tracking flow
                    // Modern Hero Status Section
                    ModernHeroStatusSection(order!!)
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Order Progress Timeline
                    OrderProgressTimeline(order!!)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Order Details Card
                    OrderDetailsCard(order!!)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Additional sections applicable to all orders
                Spacer(modifier = Modifier.height(16.dp))
                
                // "I'm Here" Button - Show when order is ready
                if (order!!.status.equals("ready", ignoreCase = true)) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
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
                                onClick = {
                                    showProofDialog = true
                                    // Mark order as delivered when user arrives
                                    firestore.collection("orders")
                                        .document(orderId)
                                        .update(
                                            mapOf(
                                                "status" to "delivered",
                                                "deliveredAt" to com.google.firebase.Timestamp.now()
                                            )
                                        )
                                },
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
fun ModernHeroStatusSection(order: Order) {
    // Pulsing animation for background circle
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Get status drawable based on order status (matches res/drawable IDs)
    val statusDrawableId = when (order.status.lowercase()) {
        "pending payment", "pending", "placed", "confirmed" ->
            R.drawable.confirmed_received
        "preparing" -> R.drawable.preparing
        "ready"     -> R.drawable.ready
        "delivered" -> R.drawable.delivered
        else        -> R.drawable.confirmed_received  // safe fallback
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Large icon with pulsing background
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(160.dp)
        ) {
            // Pulsing background circle
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                        alpha = 0.3f
                    }
                    .background(
                        color = WingZoneOrange,
                        shape = CircleShape
                    )
            )

            // Main icon — uses res/drawable, fully fills the inner area
            Image(
                painter = painterResource(id = statusDrawableId),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Status title - Bold, 24sp
        Text(
            text = getStatusDisplayText(order.status),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Status subtitle
        Text(
            text = getStatusDescription(order.status),
            fontSize = 15.sp,
            color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Order ID badge
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = WingZoneRed.copy(alpha = 0.1f)
        ) {
            Text(
                text = "Order #${order.id.take(8).uppercase()}",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = WingZoneRed,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
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
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "Order Progress",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            val steps: List<Triple<String, String, Int>> = listOf(
                Triple("Order Placed", "pending", R.drawable.confirmed_received),
                Triple("Confirmed", "confirmed", R.drawable.confirmed_received),
                Triple("Preparing", "preparing", R.drawable.preparing),
                Triple("Ready", "ready", R.drawable.ready),
                Triple("Delivered", "delivered", R.drawable.delivered)
            )
            
            steps.forEachIndexed { index, (title, status, drawableId) ->
                TimelineStep(
                    title = title,
                    drawableId = drawableId,
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
    drawableId: Int,
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
            isCompleted || isActive -> Color.White
            else -> Color(0xFFE0E0E0)
        },
        animationSpec = tween(durationMillis = 600),
        label = "timeline_background"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isCompleted || isActive -> WingZoneOrange
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 600),
        label = "timeline_border"
    )
    
    val lineProgress by animateFloatAsState(
        targetValue = if (isCompleted) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "line_progress"
    )
    
    val lineColor by animateColorAsState(
        targetValue = if (isCompleted) WingZoneRed else Color(0xFFE0E0E0),
        animationSpec = tween(durationMillis = 600),
        label = "line_color"
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
                    )
                    .border(
                        width = 2.dp,
                        color = borderColor,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = isCompleted to isActive,
                    transitionSpec = {
                        (fadeIn() + scaleIn()) togetherWith (fadeOut() + scaleOut())
                    },
                    label = "icon_animation"
                ) { (completed, _) ->
                    if (completed) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = WingZoneOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Image(
                            painter = painterResource(id = drawableId),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(48.dp)
                        .background(color = lineColor)
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
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
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
            
            DetailRow("Payment Method", order.paymentMethod.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() })
            Spacer(modifier = Modifier.height(12.dp))
            
            DetailRow("Payment Status", order.paymentStatus.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() })
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
        "preparing" -> Pair(Icons.Default.Info, Color(0xFFFF7043))
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
        else -> status.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
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
 * Modern Digital Receipt Dialog - Sleek Pickup Proof
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
    val paymentType = (orderDetails["paymentType"] as? String) ?: paymentMethod
    val paymentStatus = order.paymentStatus
    val items = orderDetails["items"] as? List<*> ?: emptyList<Any>()
    val firestore = FirebaseFirestore.getInstance()
    var isDelivering by remember { mutableStateOf(false) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    // Compact Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = WingZoneOrange,
                                modifier = Modifier.size(32.dp)
                            )
                            Column {
                                Text(
                                    text = "Pickup Confirmed",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Order #${order.id.take(8).uppercase()}",
                                    fontSize = 14.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Dashed Divider
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                    ) {
                        drawLine(
                            color = Color(0xFFBDBDBD),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f), 0f),
                            strokeWidth = 2f
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Order Items Section
                    Text(
                        text = "Order Items",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    items.forEach { item ->
                        val itemMap = item as? Map<*, *>
                        // Access nested menuItem structure
                        val menuItem = itemMap?.get("menuItem") as? Map<*, *>
                        val itemName = menuItem?.get("name") as? String ?: ""
                        val quantity = (itemMap?.get("quantity") as? Number)?.toInt() ?: 1
                        val customization = itemMap?.get("customization") as? Map<*, *>
                        
                        if (itemName.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                // Item name and quantity
                                Text(
                                    text = "${quantity}x  $itemName",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                                
                                // Customization details
                                if (customization != null) {
                                    Column(
                                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                                    ) {
                                        // Bone Type
                                        customization["boneType"]?.let { boneType ->
                                            if (boneType.toString().isNotEmpty()) {
                                                Text(
                                                    text = "• $boneType",
                                                    fontSize = 12.sp,
                                                    color = TextSecondary
                                                )
                                            }
                                        }
                                        
                                        // Flavor
                                        customization["flavor"]?.let { flavor ->
                                            if (flavor.toString().isNotEmpty() && flavor.toString() != "None") {
                                                Text(
                                                    text = "• Flavor: $flavor",
                                                    fontSize = 12.sp,
                                                    color = TextSecondary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                        
                                        // Dipping Sauce
                                        customization["dippingSauce"]?.let { sauce ->
                                            if (sauce.toString().isNotEmpty() && sauce.toString() != "None") {
                                                Text(
                                                    text = "• Dipping Sauce: $sauce",
                                                    fontSize = 12.sp,
                                                    color = WingZoneOrange,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                        
                                        // Fries Exchange
                                        (customization["friesExchange"] as? Map<*, *>)?.let { fries ->
                                            val friesName = fries["name"]?.toString() ?: ""
                                            val size = fries["selectedSize"]?.toString() ?: ""
                                            val friesFlavor = fries["selectedFlavor"]?.toString() ?: ""
                                            
                                            if (friesName.isNotEmpty()) {
                                                val sizeText = if (size == "jumbo") " (Jumbo)" else ""
                                                val flavorText = if (friesFlavor.isNotEmpty()) " - $friesFlavor" else ""
                                                Text(
                                                    text = "• Side: $friesName$sizeText$flavorText",
                                                    fontSize = 12.sp,
                                                    color = TextSecondary
                                                )
                                            }
                                        }
                                        
                                        // Salad Type
                                        customization["saladType"]?.let { salad ->
                                            if (salad.toString().isNotEmpty()) {
                                                Text(
                                                    text = "• Salad: $salad",
                                                    fontSize = 12.sp,
                                                    color = TextSecondary
                                                )
                                            }
                                        }
                                        
                                        // Drink
                                        customization["drink"]?.let { drink ->
                                            if (drink.toString().isNotEmpty() && drink.toString() != "None") {
                                                Text(
                                                    text = "• Drink: $drink",
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF1976D2),
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                        
                                        // Special Instructions
                                        itemMap["specialInstructions"]?.let { instructions ->
                                            if (instructions.toString().isNotEmpty()) {
                                                Text(
                                                    text = "• Note: $instructions",
                                                    fontSize = 12.sp,
                                                    color = TextSecondary,
                                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Dashed Divider
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                    ) {
                        drawLine(
                            color = Color(0xFFBDBDBD),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f), 0f),
                            strokeWidth = 2f
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Footer with Compact Barcode
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Smaller Barcode
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            repeat(16) { index ->
                                Box(
                                    modifier = Modifier
                                        .width(if (index % 3 == 0) 3.dp else 2.dp)
                                        .height(if (index % 2 == 0) 28.dp else 20.dp)
                                        .background(
                                            if (index % 4 == 0) WingZoneOrange else Color(0xFF424242),
                                            RoundedCornerShape(1.dp)
                                        )
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Payment Info Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Payment",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    text = when (paymentType.lowercase()) {
                                        "cash" -> "Cash"
                                        "online" -> "Online Banking (FPX)"
                                        else -> paymentType.replaceFirstChar { it.uppercase() }
                                    },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Total",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "RM ${String.format("%.2f", order.total)}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WingZoneRed
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Location & Type
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$orderType @ $location",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = paymentStatus.uppercase(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (paymentStatus.equals("paid", ignoreCase = true)) 
                                    Color(0xFF4CAF50) else Color(0xFFFF9800)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Action Buttons
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Mark as Delivered Button - Only show if order is ready
                        if (order.status == "ready") {
                            Button(
                                onClick = {
                                    isDelivering = true
                                    firestore.collection("orders")
                                        .document(order.id)
                                        .update(
                                            mapOf(
                                                "status" to "delivered",
                                                "deliveredAt" to com.google.firebase.Timestamp.now()
                                            )
                                        )
                                        .addOnSuccessListener {
                                            isDelivering = false
                                            onDismiss()
                                        }
                                        .addOnFailureListener {
                                            isDelivering = false
                                        }
                                },
                                enabled = !isDelivering,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = WingZoneRed),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isDelivering) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Mark as Delivered",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        
                        // Close Button
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
                        ) {
                            Text(
                                text = "Close",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
