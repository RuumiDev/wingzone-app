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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LobbyDetailScreen(
    lobbyId: String,
    currentUserId: String,
    onNavigateBack: () -> Unit,
    onNavigateToMenu: () -> Unit,
    onLobbyDeleted: () -> Unit = {}
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
                val data = doc.data?.toMutableMap() ?: mutableMapOf()
                data["id"] = doc.id
                lobby = data
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
                    val data = snapshot.data?.toMutableMap() ?: mutableMapOf()
                    data["id"] = snapshot.id
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
                                    "host-pays" -> "Host Pays All"
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
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = WingZoneRed) },
            title = { Text("Delete Lobby?") },
            text = { Text("This will remove the lobby for all members. This action cannot be undone.") },
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
            }
        )
    }
    
    // Leave Dialog
    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            icon = { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = WingZoneOrange) },
            title = { Text("Leave Lobby?") },
            text = { Text("Are you sure you want to leave this lobby?") },
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
            }
        )
    }
    
    // Kick Member Dialog
    if (memberToKick != null) {
        AlertDialog(
            onDismissRequest = { memberToKick = null },
            icon = { Icon(Icons.Default.Person, contentDescription = null, tint = WingZoneRed) },
            title = { Text("Remove Member?") },
            text = { Text("Are you sure you want to remove this member from the lobby?") },
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
            }
        )
    }
}

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
    onKick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
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
                    
                    val cartItems = member["cartItems"] as? List<*>
                    val itemCount = cartItems?.size ?: 0
                    Text(
                        text = if (itemCount > 0) "$itemCount items in cart" else "No items yet",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary
                        )
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
}
