package com.example.wz_app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class GroupOrderData(
    val id: String = "",
    val code: String = "",
    val hostUserId: String = "",
    val hostUserName: String = "",
    val status: String = "active", // active, confirmed, preparing, ready, delivered, cancelled
    val members: List<GroupMemberData> = emptyList(),
    val totalAmount: Double = 0.0,
    val createdAt: Timestamp = Timestamp.now()
)

data class GroupMemberData(
    val userId: String = "",
    val name: String = "",
    val cartItems: List<Map<String, Any>> = emptyList(),
    val memberTotal: Double = 0.0,
    val paymentStatus: String = "pending" // pending, paid
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupOrderScreen(
    userId: String,
    userName: String,
    onNavigateBack: () -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    var activeTab by remember { mutableStateOf("lobby") } // lobby, create, join, myOrders
    var orderCode by remember { mutableStateOf("") }
    var currentLobby by remember { mutableStateOf<GroupOrderData?>(null) }
    var myGroupOrders by remember { mutableStateOf<List<GroupOrderData>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var listener by remember { mutableStateOf<ListenerRegistration?>(null) }
    
    // Real-time listener for current lobby
    LaunchedEffect(currentLobby?.id) {
        listener?.remove()
        currentLobby?.let { lobby ->
            listener = firestore.collection("groupOrders")
                .document(lobby.id)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        errorMessage = error.message
                        return@addSnapshotListener
                    }
                    snapshot?.let {
                        val members = (it.get("members") as? List<Map<String, Any>>)?.map { member ->
                            GroupMemberData(
                                userId = member["userId"] as? String ?: "",
                                name = member["name"] as? String ?: "",
                                cartItems = member["cartItems"] as? List<Map<String, Any>> ?: emptyList(),
                                memberTotal = (member["memberTotal"] as? Number)?.toDouble() ?: 0.0,
                                paymentStatus = member["paymentStatus"] as? String ?: "pending"
                            )
                        } ?: emptyList()
                        
                        currentLobby = GroupOrderData(
                            id = it.id,
                            code = it.getString("code") ?: "",
                            hostUserId = it.getString("hostUserId") ?: "",
                            hostUserName = it.getString("hostUserName") ?: "",
                            status = it.getString("status") ?: "active",
                            members = members,
                            totalAmount = (it.get("totalAmount") as? Number)?.toDouble() ?: 0.0,
                            createdAt = it.getTimestamp("createdAt") ?: Timestamp.now()
                        )
                    }
                }
        }
    }
    
    // Load user's group orders
    LaunchedEffect(Unit) {
        firestore.collection("groupOrders")
            .whereArrayContains("memberUserIds", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                myGroupOrders = snapshot.documents.mapNotNull { doc ->
                    val members = (doc.get("members") as? List<Map<String, Any>>)?.map { member ->
                        GroupMemberData(
                            userId = member["userId"] as? String ?: "",
                            name = member["name"] as? String ?: "",
                            cartItems = member["cartItems"] as? List<Map<String, Any>> ?: emptyList(),
                            memberTotal = (member["memberTotal"] as? Number)?.toDouble() ?: 0.0,
                            paymentStatus = member["paymentStatus"] as? String ?: "pending"
                        )
                    } ?: emptyList()
                    
                    GroupOrderData(
                        id = doc.id,
                        code = doc.getString("code") ?: "",
                        hostUserId = doc.getString("hostUserId") ?: "",
                        hostUserName = doc.getString("hostUserName") ?: "",
                        status = doc.getString("status") ?: "active",
                        members = members,
                        totalAmount = (doc.get("totalAmount") as? Number)?.toDouble() ?: 0.0,
                        createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now()
                    )
                }
            }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            listener?.remove()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Group Orders", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFDC2626),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            // Tab Navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TabButton("Lobby", activeTab == "lobby") { activeTab = "lobby" }
                TabButton("Create", activeTab == "create") { activeTab = "create" }
                TabButton("Join", activeTab == "join") { activeTab = "join" }
                TabButton("My Orders", activeTab == "myOrders") { activeTab = "myOrders" }
            }
            
            // Error Message
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFDC2626)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(error, color = Color(0xFFDC2626))
                    }
                }
            }
            
            // Content based on active tab
            when (activeTab) {
                "lobby" -> LobbyContent(currentLobby, userId, onLeaveLobby = {
                    scope.launch {
                        try {
                            currentLobby?.let { lobby ->
                                firestore.collection("groupOrders")
                                    .document(lobby.id)
                                    .update(
                                        "members", FieldValue.arrayRemove(
                                            lobby.members.find { it.userId == userId }
                                        ),
                                        "memberUserIds", FieldValue.arrayRemove(userId)
                                    )
                                    .await()
                                currentLobby = null
                            }
                        } catch (e: Exception) {
                            errorMessage = e.message
                        }
                    }
                })
                
                "create" -> CreateLobbyContent(
                    loading = loading,
                    onCreateLobby = {
                        scope.launch {
                            loading = true
                            errorMessage = null
                            try {
                                val code = generateOrderCode()
                                val orderData = hashMapOf(
                                    "code" to code,
                                    "hostUserId" to userId,
                                    "hostUserName" to userName,
                                    "status" to "active",
                                    "members" to listOf(
                                        hashMapOf(
                                            "userId" to userId,
                                            "name" to userName,
                                            "cartItems" to emptyList<Map<String, Any>>(),
                                            "memberTotal" to 0.0,
                                            "paymentStatus" to "pending"
                                        )
                                    ),
                                    "memberUserIds" to listOf(userId),
                                    "totalAmount" to 0.0,
                                    "createdAt" to FieldValue.serverTimestamp()
                                )
                                
                                val docRef = firestore.collection("groupOrders")
                                    .add(orderData)
                                    .await()
                                
                                currentLobby = GroupOrderData(
                                    id = docRef.id,
                                    code = code,
                                    hostUserId = userId,
                                    hostUserName = userName,
                                    status = "active",
                                    members = listOf(
                                        GroupMemberData(
                                            userId = userId,
                                            name = userName,
                                            cartItems = emptyList(),
                                            memberTotal = 0.0,
                                            paymentStatus = "pending"
                                        )
                                    ),
                                    totalAmount = 0.0
                                )
                                activeTab = "lobby"
                            } catch (e: Exception) {
                                errorMessage = "Failed to create lobby: ${e.message}"
                            } finally {
                                loading = false
                            }
                        }
                    }
                )
                
                "join" -> JoinLobbyContent(
                    code = orderCode,
                    loading = loading,
                    onCodeChange = { orderCode = it },
                    onJoinLobby = {
                        scope.launch {
                            loading = true
                            errorMessage = null
                            try {
                                val snapshot = firestore.collection("groupOrders")
                                    .whereEqualTo("code", orderCode.uppercase())
                                    .whereEqualTo("status", "active")
                                    .get()
                                    .await()
                                
                                if (snapshot.isEmpty) {
                                    errorMessage = "Lobby not found or already closed"
                                } else {
                                    val doc = snapshot.documents.first()
                                    val memberUserIds = doc.get("memberUserIds") as? List<String> ?: emptyList()
                                    
                                    if (memberUserIds.contains(userId)) {
                                        errorMessage = "You're already in this lobby"
                                    } else {
                                        firestore.collection("groupOrders")
                                            .document(doc.id)
                                            .update(
                                                "members", FieldValue.arrayUnion(
                                                    hashMapOf(
                                                        "userId" to userId,
                                                        "name" to userName,
                                                        "cartItems" to emptyList<Map<String, Any>>(),
                                                        "memberTotal" to 0.0,
                                                        "paymentStatus" to "pending"
                                                    )
                                                ),
                                                "memberUserIds", FieldValue.arrayUnion(userId)
                                            )
                                            .await()
                                        
                                        val members = (doc.get("members") as? List<Map<String, Any>>)?.map { member ->
                                            GroupMemberData(
                                                userId = member["userId"] as? String ?: "",
                                                name = member["name"] as? String ?: "",
                                                cartItems = member["cartItems"] as? List<Map<String, Any>> ?: emptyList(),
                                                memberTotal = (member["memberTotal"] as? Number)?.toDouble() ?: 0.0,
                                                paymentStatus = member["paymentStatus"] as? String ?: "pending"
                                            )
                                        }?.toMutableList() ?: mutableListOf()
                                        
                                        members.add(
                                            GroupMemberData(
                                                userId = userId,
                                                name = userName,
                                                cartItems = emptyList(),
                                                memberTotal = 0.0,
                                                paymentStatus = "pending"
                                            )
                                        )
                                        
                                        currentLobby = GroupOrderData(
                                            id = doc.id,
                                            code = doc.getString("code") ?: "",
                                            hostUserId = doc.getString("hostUserId") ?: "",
                                            hostUserName = doc.getString("hostUserName") ?: "",
                                            status = doc.getString("status") ?: "active",
                                            members = members,
                                            totalAmount = (doc.get("totalAmount") as? Number)?.toDouble() ?: 0.0,
                                            createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now()
                                        )
                                        activeTab = "lobby"
                                        orderCode = ""
                                    }
                                }
                            } catch (e: Exception) {
                                errorMessage = "Failed to join lobby: ${e.message}"
                            } finally {
                                loading = false
                            }
                        }
                    }
                )
                
                "myOrders" -> MyOrdersContent(myGroupOrders)
            }
        }
    }
}

@Composable
fun TabButton(
    text: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isActive) Color(0xFFDC2626) else Color.Transparent,
            contentColor = if (isActive) Color.White else Color.Gray
        ),
        modifier = Modifier.height(40.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        Text(text, fontSize = 14.sp)
    }
}

@Composable
fun LobbyContent(
    lobby: GroupOrderData?,
    currentUserId: String,
    onLeaveLobby: () -> Unit
) {
    if (lobby == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No active lobby",
                    fontSize = 18.sp,
                    color = Color.Gray
                )
                Text(
                    "Create or join a lobby to start",
                    fontSize = 14.sp,
                    color = Color.LightGray
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // QR Code Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Scan to Join",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Generate QR Code
                        val qrBitmap = remember(lobby.code) {
                            generateQRCode(lobby.code, 300)
                        }
                        qrBitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "QR Code",
                                modifier = Modifier
                                    .size(200.dp)
                                    .border(2.dp, Color(0xFFDC2626), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Code: ${lobby.code}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626)
                        )
                    }
                }
            }
            
            // Lobby Info
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Host", fontSize = 12.sp, color = Color.Gray)
                                Text(
                                    lobby.hostUserName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Badge(
                                containerColor = when (lobby.status) {
                                    "active" -> Color(0xFF3B82F6)
                                    "confirmed" -> Color(0xFF10B981)
                                    else -> Color.Gray
                                }
                            ) {
                                Text(
                                    lobby.status.uppercase(),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Members", fontSize = 12.sp, color = Color.Gray)
                                Text(
                                    "${lobby.members.size}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Column {
                                Text("Total", fontSize = 12.sp, color = Color.Gray)
                                Text(
                                    "RM ${String.format("%.2f", lobby.totalAmount)}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDC2626)
                                )
                            }
                        }
                    }
                }
            }
            
            // Members List
            item {
                Text(
                    "Members (${lobby.members.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            items(lobby.members) { member ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (member.userId == currentUserId) 
                            Color(0xFFFEE2E2) 
                        else 
                            Color.White
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDC2626)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    member.name.firstOrNull()?.uppercase() ?: "?",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        member.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (member.userId == lobby.hostUserId) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = "Host",
                                            tint = Color(0xFFFBBF24),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    if (member.userId == currentUserId) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "(You)",
                                            fontSize = 12.sp,
                                            color = Color(0xFFDC2626)
                                        )
                                    }
                                }
                                Text(
                                    "${member.cartItems.size} items",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "RM ${String.format("%.2f", member.memberTotal)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Badge(
                                containerColor = if (member.paymentStatus == "paid") 
                                    Color(0xFF10B981) 
                                else 
                                    Color(0xFFFBBF24)
                            ) {
                                Text(
                                    member.paymentStatus.uppercase(),
                                    fontSize = 10.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
            
            // Actions
            item {
                if (lobby.status == "active") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (currentUserId == lobby.hostUserId) {
                            Button(
                                onClick = { /* TODO: Finalize order */ },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFDC2626)
                                )
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Finalize Group Order")
                            }
                        }
                        
                        Button(
                            onClick = onLeaveLobby,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFFDC2626)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                Color(0xFFDC2626)
                            )
                        ) {
                            Icon(Icons.Default.ExitToApp, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Leave Lobby")
                        }
                    }
                }
            }
            
            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun CreateLobbyContent(
    loading: Boolean,
    onCreateLobby: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFFDC2626)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Create Group Order",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Start a new group order lobby and invite your friends to join",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onCreateLobby,
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC2626)
                    )
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Lobby")
                    }
                }
            }
        }
    }
}

@Composable
fun JoinLobbyContent(
    code: String,
    loading: Boolean,
    onCodeChange: (String) -> Unit,
    onJoinLobby: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFFDC2626)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Join Group Order",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Enter the code to join an existing group order",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = code,
                    onValueChange = { onCodeChange(it.uppercase()) },
                    label = { Text("Order Code") },
                    placeholder = { Text("e.g., WZ1234") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFDC2626),
                        focusedLabelColor = Color(0xFFDC2626)
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onJoinLobby,
                    enabled = !loading && code.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC2626)
                    )
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(Icons.Default.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Join Lobby")
                    }
                }
            }
        }
    }
}

@Composable
fun MyOrdersContent(orders: List<GroupOrderData>) {
    if (orders.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.ShoppingCart,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No group orders yet",
                    fontSize = 18.sp,
                    color = Color.Gray
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(orders) { order ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Order #${order.code}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Host: ${order.hostUserName}",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            
                            Badge(
                                containerColor = when (order.status) {
                                    "active" -> Color(0xFF3B82F6)
                                    "confirmed" -> Color(0xFF10B981)
                                    "preparing" -> Color(0xFFFBBF24)
                                    "ready" -> Color(0xFF10B981)
                                    "delivered" -> Color(0xFF10B981)
                                    else -> Color.Gray
                                }
                            ) {
                                Text(
                                    order.status.uppercase(),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "${order.members.size} members",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                            Text(
                                "RM ${String.format("%.2f", order.totalAmount)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Helper Functions
fun generateOrderCode(): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    return "WZ" + (1..4).map { chars.random() }.joinToString("")
}

fun generateQRCode(text: String, size: Int): Bitmap? {
    return try {
        val qrCodeWriter = QRCodeWriter()
        val bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(
                    x,
                    y,
                    if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                )
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}
