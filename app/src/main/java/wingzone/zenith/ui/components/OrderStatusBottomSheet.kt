package wingzone.zenith.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import wingzone.zenith.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * GrabFood-style floating order status notification
 * Shows at the bottom of screen when order status updates
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderStatusBottomSheet(
    orderId: String,
    status: String,
    estimatedTime: String,
    storeName: String = "WingZone",
    onViewDetails: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 80.dp, start = 16.dp, end = 16.dp)
                    .clickable(onClick = onViewDetails),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Time and illustration
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = estimatedTime,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = getStatusMessage(status),
                                fontSize = 16.sp,
                                color = TextPrimary,
                                lineHeight = 22.sp
                            )
                        }
                        
                        // Cooking illustration
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(Color(0xFFE3F2FD), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getStatusIllustrationIcon(status),
                                contentDescription = null,
                                tint = WingZoneRed,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Progress bar with icons
                    OrderProgressBar(status = status)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Status message
                    if (isDelayed(status)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFFF3E0), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFF8F00),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Delayed: Kitchen needs more time to prepare your order.",
                                fontSize = 13.sp,
                                color = Color(0xFFE65100),
                                lineHeight = 18.sp
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Store info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(WingZoneRed.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "W",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = WingZoneRed
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = storeName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Order #${orderId.take(8).uppercase()}",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                        
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "View details",
                            tint = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OrderProgressBar(status: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Order placed icon
        ProgressIcon(
            icon = Icons.Default.CheckCircle,
            label = "",
            isCompleted = true,
            isActive = status == "pending"
        )
        
        // Progress line 1
        ProgressLine(isCompleted = isStepCompleted(status, "confirmed"))
        
        // Preparing icon
        ProgressIcon(
            icon = Icons.Default.Build,
            label = "",
            isCompleted = isStepCompleted(status, "preparing"),
            isActive = status == "confirmed" || status == "preparing"
        )
        
        // Progress line 2
        ProgressLine(isCompleted = isStepCompleted(status, "ready"))
        
        // Ready icon
        ProgressIcon(
            icon = Icons.Default.Home,
            label = "",
            isCompleted = isStepCompleted(status, "ready"),
            isActive = status == "ready" || status == "delivered"
        )
    }
}

@Composable
fun RowScope.ProgressIcon(
    icon: ImageVector,
    label: String,
    isCompleted: Boolean,
    isActive: Boolean
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "icon_scale"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isCompleted || isActive -> WingZoneRed
            else -> Color(0xFFE0E0E0)
        },
        animationSpec = tween(durationMillis = 500),
        label = "background_color"
    )
    
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
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun RowScope.ProgressLine(isCompleted: Boolean) {
    val progress by animateFloatAsState(
        targetValue = if (isCompleted) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "progress_line"
    )
    
    Box(
        modifier = Modifier
            .weight(1f)
            .height(4.dp)
            .background(
                color = Color(0xFFE0E0E0),
                shape = RoundedCornerShape(2.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .fillMaxHeight()
                .background(
                    color = WingZoneRed,
                    shape = RoundedCornerShape(2.dp)
                )
        )
    }
}

// Helper functions
fun getStatusMessage(status: String): String {
    return when (status.lowercase()) {
        "pending" -> "We've got your order"
        "confirmed" -> "Your order's confirmed"
        "preparing" -> "Your order's in the kitchen."
        "ready" -> "Your order is ready for pickup!"
        "delivered" -> "Order completed"
        else -> "Processing your order"
    }
}

fun getStatusIllustrationIcon(status: String): ImageVector {
    return when (status.lowercase()) {
        "pending" -> Icons.Default.ShoppingCart
        "confirmed" -> Icons.Default.CheckCircle
        "preparing" -> Icons.Default.Build
        "ready" -> Icons.Default.Done
        "delivered" -> Icons.Default.CheckCircle
        else -> Icons.Default.Info
    }
}

fun isDelayed(status: String): Boolean {
    // You can add logic here to check if order is delayed
    // For now, return false
    return false
}

fun isStepCompleted(currentStatus: String, stepStatus: String): Boolean {
    val statusOrder = listOf("pending", "confirmed", "preparing", "ready", "delivered")
    val currentIndex = statusOrder.indexOf(currentStatus.lowercase())
    val stepIndex = statusOrder.indexOf(stepStatus.lowercase())
    return currentIndex > stepIndex
}
