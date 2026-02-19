package wingzone.zenith.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import wingzone.zenith.ui.theme.*
import wingzone.zenith.ui.components.SvgIcon

// Helper Composables
@Composable
fun LobbyInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = WingZoneOrange,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary
                )
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = DarkGray
                )
            )
        }
    }
}

@Composable
fun MemberCard(
    member: Map<String, Any>,
    isHost: Boolean,
    canKick: Boolean,
    onKick: () -> Unit,
    isCurrentUserHost: Boolean = false,
    onPayForMember: () -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    // Extract member data at top level for accessibility
    val cartItems = member["cartItems"] as? List<*>
    val itemCount = cartItems?.size ?: 0
    val hasPaid = member["hasPaid"] as? Boolean ?: false
    val total = (member["total"] as? Number)?.toDouble() ?: 0.0
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (isHost) androidx.compose.foundation.BorderStroke(2.dp, WingZoneRed.copy(alpha = 0.3f)) else null
    ) {
        Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Avatar with initial and gradient background
                Box(
                    modifier = Modifier.size(56.dp)
                ) {
                    Surface(
                        color = if (isHost) WingZoneRed else WingZoneOrange,
                        shape = CircleShape,
                        modifier = Modifier.fillMaxSize(),
                        shadowElevation = 4.dp
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = (member["userName"] as? String)?.firstOrNull()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 24.sp
                                )
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = member["userName"] as? String ?: "Unknown",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2C2C2C),
                                fontSize = 16.sp
                            )
                        )
                        if (isHost) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = WingZoneRed.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Host",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = WingZoneRed
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        
                        // Status pill
                        Spacer(modifier = Modifier.width(8.dp))
                        if (hasPaid) {
                            Surface(
                                color = Color(0xFF10B981).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Ready",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981)
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        } else {
                            Surface(
                                color = Color(0xFF9CA3AF).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Ordering...",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF6B7280)
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (itemCount == 0) "No items added yet." else "$itemCount item${if (itemCount == 1) "" else "s"} · RM ${String.format("%.2f", total)}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF999999),
                                fontSize = 14.sp
                            )
                        )
                        
                        // Show expand/collapse indicator if there are items
                        if (itemCount > 0) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = Color(0xFF999999),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            
            // Clickable icon to expand/collapse if there are items
            if (itemCount > 0) {
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = Color(0xFF666666)
                    )
                }
            }
        }
        
        // Show items if expanded
        if (itemCount > 0 && cartItems != null && isExpanded) {
            Divider(color = Color(0xFFEEEEEE), modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp))
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp)
            ) {
                cartItems.forEach { item ->
                    val itemMap = item as? Map<String, Any>
                    if (itemMap != null) {
                        val menuItem = itemMap["menuItem"] as? Map<String, Any>
                        val itemName = menuItem?.get("name") as? String ?: itemMap["name"] as? String ?: "Item"
                        val quantity = (itemMap["quantity"] as? Number)?.toInt() ?: 1
                        val customization = itemMap["customization"] as? Map<String, Any>
                        
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    // Build display name with customization
                                    val displayName = buildString {
                                        append(itemName)
                                        
                                        // Add salad type for items that require it
                                        if (customization != null) {
                                            val customizationOptions = menuItem?.get("customizationOptions") as? Map<String, Any>
                                            val requiresSaladChoice = customizationOptions?.get("requiresSaladChoice") as? Boolean ?: false
                                            val saladType = customization["saladType"] as? String
                                            
                                            if (requiresSaladChoice && saladType != null) {
                                                append(" ($saladType)")
                                            }
                                            
                                            // For Ranch or Bleu Cheese items, show selection
                                            if (itemName.contains("Ranch or Bleu Cheese", ignoreCase = true)) {
                                                val dippingSauce = customization["dippingSauce"] as? String
                                                if (dippingSauce != null && dippingSauce != "None") {
                                                    append(" - $dippingSauce")
                                                }
                                            }
                                        }
                                    }
                                    
                                    Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2C2C2C),
                                            fontSize = 16.sp
                                        )
                                    )
                                    
                                    // Show customizations with better formatting
                                    if (customization != null) {
                                        // Get customization options from menuItem to determine what to show
                                        val customizationOptions = menuItem?.get("customizationOptions") as? Map<String, Any>
                                        
                                        // If customizationOptions is missing, show all available customization data
                                        val showAll = customizationOptions == null
                                        
                                        val requiresBoneType = showAll || (customizationOptions?.get("requiresBoneType") as? Boolean ?: false)
                                        val requiresFlavor = showAll || (customizationOptions?.get("requiresFlavor") as? Boolean ?: false)
                                        val requiresBeverage = showAll || (customizationOptions?.get("requiresBeverage") as? Boolean ?: false)
                                        val requiresDippingSauce = showAll || (customizationOptions?.get("requiresDippingSauce") as? Boolean ?: false)
                                        val allowFriesExchange = showAll || (customizationOptions?.get("allowFriesExchange") as? Boolean ?: false)
                                        val requiresSaladChoice = showAll || (customizationOptions?.get("requiresSaladChoice") as? Boolean ?: false)
                                        
                                        Column(
                                            modifier = Modifier.padding(top = 4.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            // Bone type
                                            if (requiresBoneType) {
                                                val boneType = customization["boneType"] as? String
                                                if (boneType != null && boneType != "None") {
                                                    Text(
                                                        text = boneType,
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            color = Color(0xFF666666),
                                                            fontSize = 12.sp
                                                        )
                                                    )
                                                }
                                            }
                                            
                                            // Flavor
                                            if (requiresFlavor) {
                                                val flavor = customization["flavor"] as? String
                                                if (flavor != null && flavor != "None") {
                                                    Text(
                                                        text = flavor,
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            color = Color(0xFF666666),
                                                            fontSize = 12.sp
                                                        )
                                                    )
                                                }
                                            }
                                            
                                            // Salad
                                            if (requiresSaladChoice) {
                                                val saladType = customization["saladType"] as? String
                                                if (saladType != null && saladType != "None") {
                                                    Text(
                                                        text = "Salad: $saladType",
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            color = Color(0xFF666666),
                                                            fontSize = 12.sp
                                                        )
                                                    )
                                                }
                                            }
                                            
                                            // Fries exchange (sides)
                                            if (allowFriesExchange) {
                                                val friesExchange = customization["friesExchange"] as? Map<String, Any>
                                                if (friesExchange != null) {
                                                    val exchangeName = friesExchange["name"] as? String
                                                    val exchangeSize = friesExchange["selectedSize"] as? String
                                                    val exchangeFlavor = friesExchange["selectedFlavor"] as? String
                                                    
                                                    if (exchangeName != null) {
                                                        val sizeText = if (exchangeSize == "jumbo") " (Jumbo)" else ""
                                                        val flavorText = if (!exchangeFlavor.isNullOrEmpty() && exchangeFlavor != "None") " - $exchangeFlavor" else ""
                                                        Text(
                                                            text = "Side: $exchangeName$sizeText$flavorText",
                                                            style = MaterialTheme.typography.bodySmall.copy(
                                                                color = Color(0xFF666666),
                                                                fontSize = 12.sp
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                            
                                            // Drink
                                            if (requiresBeverage) {
                                                val drink = customization["drink"] as? String
                                                if (drink != null && drink != "None") {
                                                    Text(
                                                        text = "Drink: $drink",
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            color = Color(0xFF666666),
                                                            fontSize = 12.sp
                                                        )
                                                    )
                                                }
                                            }
                                            
                                            // Dipping sauce
                                            if (requiresDippingSauce) {
                                                val dippingSauce = customization["dippingSauce"] as? String
                                                if (dippingSauce != null && dippingSauce != "None") {
                                                    Text(
                                                        text = "Dip: $dippingSauce",
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            color = Color(0xFF666666),
                                                            fontSize = 12.sp
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                Surface(
                                    color = Color(0xFFF5F5F5),
                                    shape = CircleShape,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(
                                            text = quantity.toString(),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF2C2C2C),
                                                fontSize = 14.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Subtotal for this member
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Subtotal",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2C2C2C),
                            fontSize = 16.sp
                        )
                    )
                    Text(
                        text = "${String.format("%.2f", total)}",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2C2C2C),
                            fontSize = 16.sp
                        )
                    )
                }
            }
        }
        
        // Old expandable section - keep for backwards compatibility but hidden
        if (false && isExpanded && itemCount > 0) {
            Divider(color = Color(0xFFEEEEEE), modifier = Modifier.padding(horizontal = 16.dp))
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Order Details",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = DarkGray
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                cartItems?.forEach { item ->
                    val itemMap = item as? Map<String, Any>
                    if (itemMap != null) {
                        val menuItem = itemMap["menuItem"] as? Map<String, Any>
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            // Item name and price
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "${itemMap["quantity"]}×",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = WingZoneOrange
                                        )
                                    )
                                    val itemName = menuItem?.get("name") as? String ?: itemMap["name"] as? String ?: "Item"
                                    Text(
                                        text = itemName,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = DarkGray
                                        )
                                    )
                                }
                                Text(
                                    text = "RM ${String.format("%.2f", (itemMap["subtotal"] as? Number)?.toDouble() ?: 0.0)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = DarkGray
                                    )
                                )
                            }
                            
                            // Customization details
                            val customization = itemMap["customization"] as? Map<String, Any>
                            if (customization != null) {
                                // Get customization options from menuItem to determine what to show
                                val customizationOptions = menuItem?.get("customizationOptions") as? Map<String, Any>
                                
                                // If customizationOptions is missing, show all available customization data
                                val showAll = customizationOptions == null
                                
                                val requiresBoneType = showAll || (customizationOptions?.get("requiresBoneType") as? Boolean ?: false)
                                val requiresFlavor = showAll || (customizationOptions?.get("requiresFlavor") as? Boolean ?: false)
                                val requiresBeverage = showAll || (customizationOptions?.get("requiresBeverage") as? Boolean ?: false)
                                val requiresDippingSauce = showAll || (customizationOptions?.get("requiresDippingSauce") as? Boolean ?: false)
                                val allowFriesExchange = showAll || (customizationOptions?.get("allowFriesExchange") as? Boolean ?: false)
                                val requiresSaladChoice = showAll || (customizationOptions?.get("requiresSaladChoice") as? Boolean ?: false)
                                
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 32.dp, top = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    // Show bone type
                                    if (requiresBoneType) {
                                        val boneType = customization["boneType"] as? String
                                        if (boneType != null && boneType != "None") {
                                            Text(
                                                text = "• $boneType",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = TextSecondary,
                                                    fontSize = 13.sp
                                                )
                                            )
                                        }
                                    }
                                    
                                    // Show flavor
                                    if (requiresFlavor) {
                                        val flavor = customization["flavor"] as? String
                                        if (flavor != null && flavor != "None") {
                                            Text(
                                                text = "• Flavor: $flavor",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = TextSecondary,
                                                    fontSize = 13.sp
                                                )
                                            )
                                        }
                                    }
                                    
                                    // Show salad choice
                                    if (requiresSaladChoice) {
                                        val saladType = customization["saladType"] as? String
                                        if (saladType != null && saladType != "None") {
                                            Text(
                                                text = "• Salad: $saladType",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = TextSecondary,
                                                    fontSize = 13.sp
                                                )
                                            )
                                        }
                                    }
                                    
                                    // Show fries exchange
                                    if (allowFriesExchange) {
                                        val friesExchange = customization["friesExchange"] as? Map<String, Any>
                                        if (friesExchange != null) {
                                            val exchangeName = friesExchange["name"] as? String
                                            val exchangeSize = friesExchange["selectedSize"] as? String
                                            val exchangeFlavor = friesExchange["selectedFlavor"] as? String
                                            
                                            if (exchangeName != null) {
                                                val sizeText = if (exchangeSize == "jumbo") " (Jumbo)" else ""
                                                val flavorText = if (!exchangeFlavor.isNullOrEmpty() && exchangeFlavor != "None") " - $exchangeFlavor" else ""
                                                Text(
                                                    text = "• Side: $exchangeName$sizeText$flavorText",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = TextSecondary,
                                                        fontSize = 13.sp
                                                    )
                                                )
                                            }
                                        }
                                    }
                                    
                                    // Show dipping sauce
                                    if (requiresDippingSauce) {
                                        val dippingSauce = customization["dippingSauce"] as? String
                                        if (dippingSauce != null && dippingSauce != "None") {
                                            Text(
                                                text = "• Dip: $dippingSauce",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = TextSecondary,
                                                    fontSize = 13.sp
                                                )
                                            )
                                        }
                                    }
                                    
                                    // Show drink
                                    if (requiresBeverage) {
                                        val drink = customization["drink"] as? String
                                        if (drink != null && drink != "None") {
                                            Text(
                                                text = "• Drink: $drink",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = TextSecondary,
                                                    fontSize = 13.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                            
                            val specialInstructions = itemMap["specialInstructions"] as? String
                            if (!specialInstructions.isNullOrBlank()) {
                                Text(
                                    text = "Note: $specialInstructions",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = WingZoneOrange,
                                        fontSize = 12.sp,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    ),
                                    modifier = Modifier.padding(start = 32.dp, top = 4.dp)
                                )
                            }
                        }
                        
                        if (cartItems.indexOf(item) < cartItems.size - 1) {
                            Divider(
                                color = Color(0xFFF0F0F0),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
                
                // Pay for Member button (only show if host and member hasn't paid)
                if (isCurrentUserHost && !isHost && !hasPaid) {
                    Button(
                        onClick = onPayForMember,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WingZoneRed
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Pay for This Member",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        
        // Show "Remove member" button if host can kick (always visible when canKick is true)
        if (canKick) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                TextButton(
                    onClick = onKick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                ) {
                    Text(
                        text = "Remove member",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    )
                }
            }
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LobbyDetailScreen(
    lobbyId: String,
    currentUserId: String,
    onNavigateBack: () -> Unit,
    onNavigateToMenu: () -> Unit,
    onNavigateToCart: () -> Unit,
    onLobbyDeleted: () -> Unit = {},
    lobbyViewModel: wingzone.zenith.viewmodel.LobbyViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val firestore = FirebaseFirestore.getInstance()
    
    var lobby by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var memberToKick by remember { mutableStateOf<String?>(null) }
    var showQRDialog by remember { mutableStateOf(false) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showSubmitDialog by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var isOrderSubmitted by remember { mutableStateOf(false) }
    var previousMemberCount by remember { mutableStateOf(0) }

    val isHost = lobby?.get("hostUserId") == currentUserId
    
    // Handle back button - prevent accidental exits
    BackHandler {
        // Show confirmation dialog before leaving
        showLeaveDialog = true
    }
    
    // Generate QR Code
    fun generateQRCode(text: String, size: Int = 512): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    // Load lobby data
    LaunchedEffect(lobbyId) {
        try {
            val doc = firestore.collection("lobbies").document(lobbyId).get().await()
            if (doc.exists()) {
                val data = doc.data?.toMutableMap() ?: mutableMapOf<String, Any>()
                data["id"] = doc.id
                lobby = data
                // Initialize member count
                val members = data["members"] as? List<*> ?: emptyList<Any>()
                previousMemberCount = members.size
            }
            isLoading = false
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to load lobby", Toast.LENGTH_SHORT).show()
            isLoading = false
        }
    }
    
    // Real-time listener
    DisposableEffect(lobbyId) {
        val listener = firestore.collection("lobbies").document(lobbyId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("LobbyDetail", "Listener error: ${error.message}")
                    return@addSnapshotListener
                }
                
                // Handle lobby deletion
                if (snapshot != null && !snapshot.exists()) {
                    // Lobby was deleted - navigate back for all users
                    if (isOrderSubmitted) {
                        Toast.makeText(
                            context,
                            "Order submitted! Lobby closed.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    onNavigateBack()
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    val data = snapshot.data?.toMutableMap() ?: mutableMapOf<String, Any>()
                    data["id"] = snapshot.id
                    
                    // Check for new members joining
                    val currentMembers = data["members"] as? List<*> ?: emptyList<Any>()
                    val currentMemberCount = currentMembers.size
                    
                    if (previousMemberCount > 0 && currentMemberCount > previousMemberCount) {
                        // New member joined - find who joined
                        val newMember = currentMembers.lastOrNull() as? Map<*, *>
                        val newMemberName = newMember?.get("userName") as? String ?: "Someone"
                        
                        Toast.makeText(
                            context,
                            "✓ $newMemberName has joined the lobby",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    
                    previousMemberCount = currentMemberCount
                    lobby = data
                }
            }
        
        onDispose {
            listener.remove()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            text = "Review Order",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = Color(0xFF2C2C2C)
                        )
                        val location = lobby?.get("location") as? Map<String, Any>
                        Text(
                            text = location?.get("name") as? String ?: "",
                            fontSize = 14.sp,
                            color = Color(0xFF666666),
                            fontWeight = FontWeight.Normal
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Back",
                            tint = Color(0xFF2C2C2C)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val code = lobby?.get("code") as? String
                        if (code != null) {
                            qrBitmap = generateQRCode(code, 512)
                            showQRDialog = true
                        }
                    }) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Invite",
                            tint = WingZoneOrange
                        )
                    }
                    IconButton(onClick = { showLeaveDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Leave Lobby",
                            tint = WingZoneRed
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFDF4ED),
                    titleContentColor = Color(0xFF2C2C2C),
                    navigationIconContentColor = Color(0xFF2C2C2C)
                ),
                windowInsets = WindowInsets.statusBars
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = WingZoneOrange)
            }
        } else if (lobby == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Lobby not found", color = TextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFFFDF4ED)),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val currentLobby = lobby ?: return@LazyColumn
                val members = currentLobby["members"] as? List<Map<String, Any>> ?: emptyList()
                val allPaid = members.all { (it["hasPaid"] as? Boolean) ?: false }
                val readyCount = members.count { (it["hasPaid"] as? Boolean) ?: false }
                
                // Billing Card
                item {
                    val isHost = (currentLobby["hostUserId"] as? String) == currentUserId
                    val paymentMethod = currentLobby["paymentMethod"] as? String
                    
                    val billingText = when {
                        isHost && paymentMethod == "host-pays-all" -> "You are paying for everyone"
                        !isHost && paymentMethod == "host-pays-all" -> "Host is paying for you 🎉"
                        else -> "Everyone pays their own share"
                    }
                    
                    val iconPath = when {
                        paymentMethod == "host-pays-all" -> "icons/crown.svg"
                        else -> "icons/groups.svg"
                    }
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Transparent
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 8.dp
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFFFF8C00),
                                            Color(0xFFFF5500)
                                        )
                                    )
                                )
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(
                                            Color.White.copy(alpha = 0.2f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    SvgIcon(
                                        assetPath = iconPath,
                                        contentDescription = "Payment Icon",
                                        modifier = Modifier.size(32.dp),
                                        tint = Color.White
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                Column {
                                    Text(
                                        text = "Payment Method",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = billingText,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 16.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Your basket header with "Add items" link
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Your basket",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2C2C2C),
                                fontSize = 22.sp
                            )
                        )
                        TextButton(onClick = onNavigateToMenu) {
                            Text(
                                text = "Add items",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = WingZoneRed,
                                    fontSize = 16.sp
                                )
                            )
                        }
                    }
                }
                
                // Member List
                items(members, key = { it["userId"] as? String ?: "" }) { member ->
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { -it / 2 }) + fadeOut(),
                        modifier = Modifier.animateItem()
                    ) {
                        MemberCard(
                            member = member,
                            isHost = member["userId"] == currentLobby["hostUserId"],
                            canKick = isHost && member["userId"] != currentUserId,
                            onKick = {
                                memberToKick = member["userId"] as? String
                            },
                            isCurrentUserHost = isHost,
                            onPayForMember = {
                                val memberUserId = member["userId"] as? String ?: ""
                                lobbyViewModel.markAsPaid(lobbyId, memberUserId) { result ->
                                    result.onSuccess {
                                        Toast.makeText(context, "Paid for member", Toast.LENGTH_SHORT).show()
                                    }.onFailure { error ->
                                        Toast.makeText(context, "Failed: ${error.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
                
                // Ready status indicator
                if (allPaid && members.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Everyone's ready",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981),
                                        fontSize = 16.sp
                                    )
                                )
                            }
                        }
                    }
                } else if (members.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "$readyCount of ${members.size} members are ready",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF666666),
                                    fontSize = 16.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = if (members.isNotEmpty()) readyCount.toFloat() / members.size.toFloat() else 0f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = WingZoneOrange,
                                trackColor = Color(0xFFE0E0E0)
                            )
                        }
                    }
                }
                
                // Subtotal
                item {
                    val total = members.sumOf { (it["total"] as? Number)?.toDouble() ?: 0.0 }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Subtotal",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2C2C2C),
                                    fontSize = 22.sp
                                )
                            )
                            Text(
                                text = "(excl. service fees and other charges)",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF999999),
                                    fontSize = 12.sp
                                )
                            )
                        }
                        Text(
                            text = "RM${String.format("%.2f", total)}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2C2C2C),
                                fontSize = 22.sp
                            )
                        )
                    }
                }
                
                // Bottom spacing for button
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Delete Group Order Button (Host only)
                if (isHost) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    item {
                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = WingZoneRed.copy(alpha = 0.7f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                2.dp,
                                WingZoneRed.copy(alpha = 0.7f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Delete Group Order",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
                
                // Bottom spacing for button
                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
            
            // Fixed bottom "Next" button
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.BottomCenter
            ) {
                val currentLobby = lobby
                val members = currentLobby?.get("members") as? List<Map<String, Any>> ?: emptyList()
                val paymentMethod = currentLobby?.get("paymentMethod") as? String ?: "split-equally"
                val hostMember = members.find { it["userId"] == currentUserId }
                val hostHasPaid = hostMember?.get("hasPaid") as? Boolean ?: false
                val allPaid = members.all { (it["hasPaid"] as? Boolean) ?: false }
                val canSubmit = when (paymentMethod) {
                    "host-pays-all" -> true
                    "individual" -> allPaid
                    else -> allPaid
                }
                
                val currentUserMember = members.find { it["userId"] == currentUserId }
                val currentUserHasPaid = currentUserMember?.get("hasPaid") as? Boolean ?: false
                val currentUserHasItems = (currentUserMember?.get("cartItems") as? List<*>)?.isNotEmpty() ?: false
                
                // Button logic:
                // - Host: Can submit when everyone is ready (based on payment method)
                // - Member (host-pays-all): Can mark as ready or edit order
                // - Member (individual/split): Can mark as ready, but CANNOT edit after marking
                // - If user has no items, enable "Go to Menu" button
                val buttonEnabled = if (isHost) {
                    canSubmit && allPaid && members.any { (it["cartItems"] as? List<*>)?.isNotEmpty() == true }
                } else {
                    // Always enable for non-host users (for Go to Menu, Mark Ready, or Edit Order in host-pays-all)
                    true
                }
                
                // For individual payment, users cannot edit after marking as ready
                val canEditOrder = paymentMethod == "host-pays-all" && currentUserHasPaid
                
                val buttonText = when {
                    isHost && canSubmit -> "Next"
                    isHost && !canSubmit -> "Waiting for members..."
                    !isHost && canEditOrder -> "Edit Order"
                    !isHost && currentUserHasPaid -> "Marked as Ready"
                    !isHost && currentUserHasItems -> "Next"
                    else -> "Go to Menu"
                }
                
                Button(
                    onClick = {
                        if (isHost && canSubmit) {
                            if (!allPaid) {
                                Toast.makeText(
                                    context,
                                    "Wait for all members to mark as ready",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                showSubmitDialog = true
                            }
                        } else if (!isHost && canEditOrder) {
                            // User is ready in host-pays-all - unmark as paid and navigate to cart to edit order
                            scope.launch {
                                try {
                                    val lobbyDoc = firestore.collection("lobbies").document(lobbyId).get().await()
                                    val currentMembers = lobbyDoc.get("members") as? List<Map<String, Any>> ?: emptyList()
                                    val updatedMembers = currentMembers.map { member ->
                                        if (member["userId"] == currentUserId) {
                                            member.toMutableMap().apply {
                                                put("hasPaid", false)
                                                put("status", "ordering")
                                            }
                                        } else {
                                            member
                                        }
                                    }
                                    firestore.collection("lobbies").document(lobbyId)
                                        .update("members", updatedMembers).await()
                                    onNavigateToCart()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed to update status: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else if (!isHost && !currentUserHasItems) {
                            // Navigate to menu when user has no items
                            onNavigateToMenu()
                        } else if (!isHost && currentUserHasItems && !currentUserHasPaid) {
                            // Mark as ready/paid
                            lobbyViewModel.markAsPaid(lobbyId, currentUserId) { result ->
                                result.onSuccess {
                                    Toast.makeText(context, "You're ready!", Toast.LENGTH_SHORT).show()
                                }.onFailure { error ->
                                    Toast.makeText(context, "Failed: ${error.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else if (!isHost && currentUserHasPaid && paymentMethod != "host-pays-all") {
                            // For individual/split payment, show message that they've already marked as ready
                            Toast.makeText(
                                context,
                                "You've already marked as ready. Wait for others to complete.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    enabled = buttonEnabled && !(paymentMethod != "host-pays-all" && currentUserHasPaid && !isHost),
                    colors = if (!isHost && canEditOrder) {
                        // Outlined style for "Edit Order" state
                        ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = WingZoneOrange
                        )
                    } else {
                        ButtonDefaults.buttonColors(
                            containerColor = WingZoneRed,
                            disabledContainerColor = Color(0xFFE5E5E5),
                            contentColor = Color.White,
                            disabledContentColor = Color(0xFF999999)
                        )
                    },
                    border = if (!isHost && canEditOrder) {
                        androidx.compose.foundation.BorderStroke(2.dp, WingZoneOrange)
                    } else null,
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 8.dp,
                        disabledElevation = 0.dp
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = buttonText,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        )
                        if (buttonEnabled && isHost && canSubmit) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    
    // QR Code Dialog
    if (showQRDialog && qrBitmap != null) {
        Dialog(onDismissRequest = { showQRDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Scan to Join Lobby",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = DarkGray
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = lobby?.get("code") as? String ?: "",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = WingZoneOrange,
                            letterSpacing = 4.sp
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // QR Code Image
                    Image(
                        bitmap = qrBitmap!!.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier
                            .size(280.dp)
                            .padding(16.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "Share this QR code with others to let them join your lobby instantly!",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Share Invite Link Button
                    OutlinedButton(
                        onClick = {
                            val lobbyCode = lobby?.get("code") as? String ?: ""
                            val joinLink = "https://us-central1-wingzone-app.cloudfunctions.net/joinLobby?code=$lobbyCode"
                            val shareText = "🍗 Join my WingZone lobby!\n\n$joinLink"
                            
                            try {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Share lobby code")
                                context.startActivity(shareIntent)
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "Unable to share: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = WingZoneOrange
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WingZoneOrange)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Share Invite Link",
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = { showQRDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WingZoneOrange
                        )
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
    
    // Delete Dialog
    if (showDeleteDialog) {
        val deleteScope = rememberCoroutineScope()
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        deleteScope.launch {
                            try {
                                // Set flag to prevent listener from showing message
                                isOrderSubmitted = false
                                firestore.collection("lobbies").document(lobbyId).delete().await()
                                Toast.makeText(context, "✓ Lobby deleted", Toast.LENGTH_SHORT).show()
                                showDeleteDialog = false
                                // Small delay to ensure listener is removed
                                kotlinx.coroutines.delay(100)
                            } catch (e: Exception) {
                                if (e !is kotlinx.coroutines.CancellationException) {
                                    Toast.makeText(context, "Failed to delete lobby: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            } finally {
                                // Always navigate away, even if cancelled
                                onLobbyDeleted()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WingZoneRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", fontWeight = FontWeight.Medium)
                }
            },
            icon = { 
                Icon(
                    Icons.Default.Delete, 
                    contentDescription = null, 
                    tint = WingZoneRed,
                    modifier = Modifier.size(32.dp)
                ) 
            },
            title = { 
                Text(
                    "Delete Lobby?",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                ) 
            },
            text = { 
                Text(
                    "This will remove the lobby for all members. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                ) 
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
    
    // Leave Dialog
    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val members = lobby?.get("members") as? List<Map<String, Any>> ?: emptyList()
                                val updatedMembers = members.filter { it["userId"] != currentUserId }
                                
                                if (isHost && updatedMembers.isNotEmpty()) {
                                    // Transfer host to the most recent member (last in list)
                                    val newHost = updatedMembers.last()
                                    val newHostId = newHost["userId"] as? String
                                    
                                    firestore.collection("lobbies").document(lobbyId)
                                        .update(
                                            mapOf(
                                                "members" to updatedMembers,
                                                "hostUserId" to newHostId
                                            )
                                        ).await()
                                    
                                    Toast.makeText(context, "Host transferred to ${newHost["userName"]}", Toast.LENGTH_SHORT).show()
                                } else if (updatedMembers.isEmpty()) {
                                    // Last person leaving - delete the lobby
                                    firestore.collection("lobbies").document(lobbyId).delete().await()
                                    Toast.makeText(context, "Lobby closed", Toast.LENGTH_SHORT).show()
                                } else {
                                    // Regular member leaving
                                    firestore.collection("lobbies").document(lobbyId)
                                        .update("members", updatedMembers).await()
                                    Toast.makeText(context, "Left lobby", Toast.LENGTH_SHORT).show()
                                }
                                
                                showLeaveDialog = false
                                onNavigateBack()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to leave lobby: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isHost) WingZoneRed else WingZoneOrange)
                ) {
                    Text("Leave")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text("Cancel")
                }
            },
            icon = { Icon(Icons.Default.Close, contentDescription = null, tint = WingZoneRed) },
            title = { Text(if (isHost) "Leave & Transfer Host?" else "Leave Lobby?") },
            text = { 
                Text(
                    if (isHost) "You are the host. Leaving will transfer host to the most recent member. You will lose your items."
                    else "Are you sure you want to leave this lobby? You will lose your items."
                )
            }
        )
    }
    
    // Submit Order Confirmation Dialog
    if (showSubmitDialog) {
        val members = lobby?.get("members") as? List<Map<String, Any>> ?: emptyList()
        val paymentMethod = lobby?.get("paymentMethod") as? String ?: "individual"
        val total = members.sumOf { (it["total"] as? Number)?.toDouble() ?: 0.0 }
        val itemCount = members.sumOf { member ->
            val cartItems = member["cartItems"] as? List<*>
            cartItems?.size ?: 0
        }
        val hostHasPaid = members.find { it["userId"] == currentUserId }?.get("hasPaid") as? Boolean ?: false
        val needsHostPayment = paymentMethod == "host-pays-all" && !hostHasPaid
        
        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showSubmitDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        if (needsHostPayment) {
                            // First mark host as paid before submitting
                            isSubmitting = true
                            lobbyViewModel.markAsPaid(lobbyId, currentUserId) { paymentResult ->
                                paymentResult.onSuccess {
                                    // Now submit the order
                                    lobbyViewModel.submitOrder(lobbyId) { result ->
                                        isSubmitting = false
                                        result.onSuccess { orderId ->
                                            isOrderSubmitted = true
                                            showSubmitDialog = false
                                            // Don't navigate here - let the listener handle it when lobby is deleted
                                        }.onFailure { error ->
                                            Toast.makeText(context, "Failed: ${error.message}", Toast.LENGTH_LONG).show()
                                            showSubmitDialog = false
                                        }
                                    }
                                }.onFailure { error ->
                                    isSubmitting = false
                                    Toast.makeText(context, "Payment failed: ${error.message}", Toast.LENGTH_LONG).show()
                                    showSubmitDialog = false
                                }
                            }
                        } else {
                            // Just submit the order
                            isSubmitting = true
                            lobbyViewModel.submitOrder(lobbyId) { result ->
                                isSubmitting = false
                                result.onSuccess { orderId ->
                                    isOrderSubmitted = true
                                    showSubmitDialog = false
                                    // Don't navigate here - let the listener handle it when lobby is deleted
                                }.onFailure { error ->
                                    Toast.makeText(context, "Failed: ${error.message}", Toast.LENGTH_LONG).show()
                                    showSubmitDialog = false
                                }
                            }
                        }
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = WingZoneRed),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 4.dp
                    )
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White
                        )
                    } else {
                        Text(
                            if (needsHostPayment) "Pay & Submit Order" else "Submit Order",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { if (!isSubmitting) showSubmitDialog = false },
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", fontWeight = FontWeight.Medium)
                }
            },
            icon = { 
                Icon(
                    Icons.Default.Check, 
                    contentDescription = null, 
                    tint = WingZoneOrange,
                    modifier = Modifier.size(32.dp)
                ) 
            },
            title = { 
                Text(
                    if (needsHostPayment) "Confirm Payment & Submit" else "Submit Group Order?",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                ) 
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (needsHostPayment) {
                        Text(
                            "As host, you're paying for the entire group order.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    } else {
                        Text(
                            "Ready to submit this group order to the kitchen?",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF5F5F5)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Members:", color = Color(0xFF666666))
                                Text(
                                    "${members.size}",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Items:", color = Color(0xFF666666))
                                Text(
                                    "$itemCount",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            androidx.compose.material3.HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Total:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    "RM ${String.format("%.2f", total)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = WingZoneRed
                                )
                            }
                        }
                    }
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
    
    // Kick Member Dialog
    if (memberToKick != null) {
        AlertDialog(
            onDismissRequest = { memberToKick = null },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val members = lobby?.get("members") as? List<Map<String, Any>> ?: emptyList()
                                val updatedMembers = members.filter { it["userId"] != memberToKick }
                                
                                firestore.collection("lobbies").document(lobbyId)
                                    .update("members", updatedMembers).await()
                                
                                Toast.makeText(context, "Member removed", Toast.LENGTH_SHORT).show()
                                memberToKick = null
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to remove member", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WingZoneRed)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToKick = null }) {
                    Text("Cancel")
                }
            },
            icon = { Icon(Icons.Default.Person, contentDescription = null, tint = WingZoneRed) },
            title = { Text("Remove Member?") },
            text = { Text("Are you sure you want to remove this member from the lobby?") }
        )
    }
}

