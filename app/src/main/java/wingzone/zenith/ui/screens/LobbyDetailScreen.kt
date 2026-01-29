package wingzone.zenith.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = WingZoneOrange.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = WingZoneOrange,
                        modifier = Modifier
                            .padding(12.dp)
                            .size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = member["userName"] as? String ?: "Unknown",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = DarkGray
                            )
                        )
                        if (isHost) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = WingZoneRed.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "HOST",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = WingZoneRed
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (itemCount > 0) "$itemCount items • RM ${String.format("%.2f", total)}" else "No items yet",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary
                            )
                        )
                        
                        if (hasPaid) {
                            Surface(
                                color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "PAID",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Row {
                if (itemCount > 0) {
                    IconButton(onClick = { isExpanded = !isExpanded }) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = WingZoneOrange
                        )
                    }
                }
                
                if (canKick) {
                    IconButton(onClick = onKick) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove member",
                            tint = WingZoneRed
                        )
                    }
                }
            }
        }
        
        // Expandable order details
        if (isExpanded && itemCount > 0) {
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
                                    val menuItem = itemMap["menuItem"] as? Map<String, Any>
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
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 32.dp, top = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    val boneType = customization["boneType"] as? String
                                    if (boneType != null) {
                                        Text(
                                            text = "• $boneType",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = TextSecondary,
                                                fontSize = 13.sp
                                            )
                                        )
                                    }
                                    
                                    val flavor = customization["flavor"] as? String
                                    if (flavor != null) {
                                        Text(
                                            text = "• Flavor: $flavor",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = TextSecondary,
                                                fontSize = 13.sp
                                            )
                                        )
                                    }
                                    
                                    val friesExchange = customization["friesExchange"] as? Map<String, Any>
                                    if (friesExchange != null) {
                                        val exchangeName = friesExchange["name"] as? String
                                        val exchangeSize = friesExchange["selectedSize"] as? String
                                        val exchangeFlavor = friesExchange["selectedFlavor"] as? String
                                        
                                        val sizeText = if (exchangeSize == "jumbo") " (Jumbo)" else ""
                                        val flavorText = if (exchangeFlavor != null) " - $exchangeFlavor" else ""
                                        Text(
                                            text = "• Side: $exchangeName$sizeText$flavorText",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = TextSecondary,
                                                fontSize = 13.sp
                                            )
                                        )
                                    }
                                    
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
                                    
                                    val saladType = customization["saladType"] as? String
                                    if (saladType != null) {
                                        Text(
                                            text = "• Salad: $saladType",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = TextSecondary,
                                                fontSize = 13.sp
                                            )
                                        )
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
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LobbyDetailScreen(
    lobbyId: String,
    currentUserId: String,
    onNavigateBack: () -> Unit,
    onNavigateToMenu: () -> Unit,
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
    var previousMemberCount by remember { mutableStateOf(0) }

    val isHost = lobby?.get("hostUserId") == currentUserId
    
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
                if (error != null) return@addSnapshotListener
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
                title = { Text("Lobby Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WingZoneOrange,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
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
                    .background(BackgroundGray),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val currentLobby = lobby ?: return@LazyColumn
                
                // Lobby Code Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Lobby Code",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = TextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = WingZoneOrange.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = currentLobby["code"] as? String ?: "",
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = WingZoneOrange,
                                        letterSpacing = 4.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Share button with QR code
                            OutlinedButton(
                                onClick = {
                                    val code = currentLobby["code"] as? String
                                    if (code != null) {
                                        qrBitmap = generateQRCode(code, 512)
                                        showQRDialog = true
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Show QR Code")
                            }
                        }
                    }
                }
                
                // Lobby Info Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Text(
                                text = "Lobby Information",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = DarkGray
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Host
                            LobbyInfoRow(
                                icon = Icons.Default.Person,
                                label = "Host",
                                value = currentLobby["hostUserName"] as? String ?: "Unknown"
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Location
                            val location = currentLobby["location"] as? Map<String, Any>
                            LobbyInfoRow(
                                icon = Icons.Default.LocationOn,
                                label = "Location",
                                value = location?.get("name") as? String ?: "Unknown"
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Order Type
                            LobbyInfoRow(
                                icon = Icons.Default.ShoppingCart,
                                label = "Order Type",
                                value = currentLobby["orderType"] as? String ?: "Pickup"
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Payment Method
                            LobbyInfoRow(
                                icon = Icons.Default.Star,
                                label = "Payment",
                                value = when (currentLobby["paymentMethod"]) {
                                    "host-pays-all" -> "Host Pays All"
                                    "split-equally" -> "Split Equally"
                                    "individual" -> "Individual Payment"
                                    else -> "Unknown"
                                }
                            )
                        }
                    }
                }
                
                // Members Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Members",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = DarkGray
                                    )
                                )
                                
                                val members = currentLobby["members"] as? List<Map<String, Any>>
                                val memberCount = members?.size ?: 0
                                val maxMembers = (currentLobby["maxMembers"] as? Long)?.toInt() ?: 10
                                
                                Surface(
                                    color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text(
                                        text = "$memberCount/$maxMembers",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF4CAF50)
                                        ),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Member List
                val members = currentLobby["members"] as? List<Map<String, Any>> ?: emptyList()
                items(members) { member ->
                    MemberCard(
                        member = member,
                        isHost = member["userId"] == currentLobby["hostUserId"],
                        canKick = isHost && member["userId"] != currentUserId,
                        onKick = {
                            memberToKick = member["userId"] as? String
                        },
                        isCurrentUserHost = isHost,
                        onPayForMember = {
                            // Mark this member as paid by host
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
                
                // Action Buttons
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Add Items Button
                        Button(
                            onClick = onNavigateToMenu,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WingZoneOrange
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Add Items to Cart",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // Host: Submit Order Button (only if conditions are met)
                        if (isHost) {
                            val paymentMethod = currentLobby["paymentMethod"] as? String ?: "split-equally"
                            val hostMember = members.find { it["userId"] == currentUserId }
                            val hostHasPaid = hostMember?.get("hasPaid") as? Boolean ?: false
                            val otherMembers = members.filter { it["userId"] != currentUserId }
                            val allOthersPaid = otherMembers.all { (it["hasPaid"] as? Boolean) ?: false }
                            val allPaid = members.all { (it["hasPaid"] as? Boolean) ?: false }
                            val canSubmit = when (paymentMethod) {
                                "host-pays-all" -> true
                                "individual" -> allPaid
                                else -> allPaid
                            }
                            
                            // Show host payment button first if host hasn't paid (individual mode)
                            if (paymentMethod == "individual" && !hostHasPaid) {
                                Button(
                                    onClick = { 
                                        // Mark host as paid
                                        lobbyViewModel.markAsPaid(lobbyId, currentUserId) { result ->
                                            result.onSuccess {
                                                android.widget.Toast.makeText(context, "Payment confirmed!", android.widget.Toast.LENGTH_SHORT).show()
                                            }.onFailure { error ->
                                                android.widget.Toast.makeText(context, "Payment failed: ${error.message}", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = WingZoneRed,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Pay My Order (RM ${String.format("%.2f", (hostMember?.get("total") as? Number)?.toDouble() ?: 0.0)})",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            
                            Button(
                                onClick = { showSubmitDialog = true },
                                enabled = canSubmit && !isSubmitting,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (canSubmit) WingZoneRed else Color.Gray,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isSubmitting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.White
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (canSubmit) Icons.Default.Check else Icons.Default.Lock,
                                        contentDescription = null
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isSubmitting) "Submitting..." else if (canSubmit) "Submit Group Order" else if (paymentMethod == "host-pays-all") "Submit Order (Host Pays)" else "Waiting for Payments",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            if (!canSubmit && paymentMethod != "host-pays-all") {
                                val unpaidCount = members.count { !(it["hasPaid"] as? Boolean ?: false) }
                                Text(
                                    text = "💡 $unpaidCount member(s) haven't paid yet",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }
                        
                        // Host: Delete Lobby / Member: Leave Lobby
                        if (isHost) {
                            OutlinedButton(
                                onClick = { showDeleteDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = WingZoneRed
                                ),
                                border = androidx.compose.foundation.BorderStroke(2.dp, WingZoneRed),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Delete Lobby",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = { showLeaveDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = WingZoneRed
                                ),
                                border = androidx.compose.foundation.BorderStroke(2.dp, WingZoneRed),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.ExitToApp, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Leave Lobby",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(32.dp)) }
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
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                firestore.collection("lobbies").document(lobbyId).delete().await()
                                Toast.makeText(context, "Lobby deleted", Toast.LENGTH_SHORT).show()
                                showDeleteDialog = false
                                onLobbyDeleted()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to delete lobby", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WingZoneRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = WingZoneRed) },
            title = { Text("Delete Lobby?") },
            text = { Text("This will remove the lobby for all members. This action cannot be undone.") }
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
                                
                                firestore.collection("lobbies").document(lobbyId)
                                    .update("members", updatedMembers).await()
                                
                                Toast.makeText(context, "Left lobby", Toast.LENGTH_SHORT).show()
                                showLeaveDialog = false
                                onNavigateBack()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to leave lobby", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WingZoneOrange)
                ) {
                    Text("Leave")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text("Cancel")
                }
            },
            icon = { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = WingZoneOrange) },
            title = { Text("Leave Lobby?") },
            text = { Text("Are you sure you want to leave this lobby?") }
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
                                            Toast.makeText(context, "Payment confirmed! Order submitted successfully! Order ID: $orderId", Toast.LENGTH_LONG).show()
                                            showSubmitDialog = false
                                            onNavigateBack()
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
                                    Toast.makeText(context, "Order submitted successfully! Order ID: $orderId", Toast.LENGTH_LONG).show()
                                    showSubmitDialog = false
                                    onNavigateBack()
                                }.onFailure { error ->
                                    Toast.makeText(context, "Failed: ${error.message}", Toast.LENGTH_LONG).show()
                                    showSubmitDialog = false
                                }
                            }
                        }
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = WingZoneRed)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White
                        )
                    } else {
                        Text(if (needsHostPayment) "Pay & Submit Order" else "Submit Order")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { if (!isSubmitting) showSubmitDialog = false },
                    enabled = !isSubmitting
                ) {
                    Text("Cancel")
                }
            },
            icon = { 
                Icon(Icons.Default.Check, contentDescription = null, tint = WingZoneRed) 
            },
            title = { Text(if (needsHostPayment) "Confirm Payment & Submit" else "Submit Group Order?") },
            text = {
                Column {
                    if (needsHostPayment) {
                        Text("As host, you're paying for the entire group order.")
                        Spacer(modifier = Modifier.height(8.dp))
                    } else {
                        Text("This will submit the group order to the kitchen.")
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text("• ${members.size} member(s)")
                    Text("• $itemCount item(s)")
                    Text("• RM ${String.format("%.2f", total)} total", fontWeight = FontWeight.Bold)
                }
            }
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

