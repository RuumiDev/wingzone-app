package wingzone.zenith.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import wingzone.zenith.R
import wingzone.zenith.data.models.Cart
import wingzone.zenith.data.models.GroupOrder
import wingzone.zenith.data.models.GroupOrderStatus
import wingzone.zenith.data.repository.FirebaseOrderRepository
import wingzone.zenith.ui.theme.*
import wingzone.zenith.viewmodel.AuthViewModel
import wingzone.zenith.viewmodel.GroupOrderViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupOrderScreen(
    authViewModel: AuthViewModel = AuthViewModel(),
    groupOrderViewModel: GroupOrderViewModel = GroupOrderViewModel(),
    lobbyViewModel: wingzone.zenith.viewmodel.LobbyViewModel,
    onAuthRequired: () -> Unit = {},
    onNavigateToCreateLobby: () -> Unit = {},
    onNavigateToJoinLobby: () -> Unit = {},
    onNavigateToLobbyDetail: (String) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Join Lobby", "My Lobbies")
    val userGroupOrders = remember { mutableStateListOf<GroupOrder>() }
    val userLobbies = remember { mutableStateListOf<Map<String, Any>>() }
    var refreshTrigger by remember { mutableStateOf(0) }
    
    // Load user's lobbies from Firestore
    LaunchedEffect(refreshTrigger) {
        if (authViewModel.isAuthenticated()) {
            userLobbies.clear()
            val lobbies = lobbyViewModel.getUserLobbies()
            userLobbies.addAll(lobbies)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Group Orders",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WingZoneOrange,
                    titleContentColor = Color.White
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        containerColor = BackgroundGray
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = WingZoneRed
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
            
            // Content
            when (selectedTab) {
                0 -> JoinLobbyTab(
                    onCreateClick = {
                        if (!authViewModel.isAuthenticated()) {
                            onAuthRequired()
                        } else {
                            onNavigateToCreateLobby()
                        }
                    },
                    onJoinClick = {
                        if (!authViewModel.isAuthenticated()) {
                            onAuthRequired()
                        } else {
                            onNavigateToJoinLobby()
                        }
                    },
                    lobbies = userLobbies,
                    onJoinLobby = { code ->
                        if (!authViewModel.isAuthenticated()) {
                            onAuthRequired()
                        } else {
                            lobbyViewModel.joinLobby(code) { result ->
                                if (result.isSuccess) {
                                    refreshTrigger++
                                }
                            }
                        }
                    },
                    onViewLobbyDetail = { lobbyId ->
                        onNavigateToLobbyDetail(lobbyId)
                    }
                )
                1 -> MyLobbiesTab(
                    lobbies = userLobbies,
                    currentUserId = authViewModel.getCurrentUser()?.id ?: "",
                    onViewDetails = { lobby ->
                        val lobbyId = lobby["id"] as? String
                        if (lobbyId != null) {
                            onNavigateToLobbyDetail(lobbyId)
                        }
                    },
                    onLeaveLobby = { lobbyId ->
                        // TODO: Implement leave lobby
                        refreshTrigger++
                    },
                    onRefresh = {
                        refreshTrigger++
                    }
                )
            }
        }
    }
}

@Composable
fun JoinLobbyTab(
    onCreateClick: () -> Unit,
    onJoinClick: () -> Unit,
    lobbies: List<Map<String, Any>>,
    onJoinLobby: (String) -> Unit,
    onViewLobbyDetail: (String) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = WingZoneOrange),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Feature",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "Group Orders",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Order together and save more!",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.95f),
                            fontWeight = FontWeight.Medium
                        ),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onCreateClick,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = WingZoneOrange
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Create",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        
                        OutlinedButton(
                            onClick = onJoinClick,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            ),
                            border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Join",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }
        
        // Available Lobbies
        if (lobbies.isNotEmpty()) {
            item {
                Text(
                    text = "Available Lobbies",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = DarkGray
                    )
                )
            }
            
            items(lobbies.filter { (it["status"] as? String) == "active" }) { lobby ->
                NewLobbyCard(
                    lobby = lobby,
                    onClick = { 
                        val lobbyId = lobby["id"] as? String
                        if (lobbyId != null) {
                            onViewLobbyDetail(lobbyId)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun MyLobbiesTab(
    lobbies: List<Map<String, Any>>,
    currentUserId: String,
    onViewDetails: (Map<String, Any>) -> Unit,
    onLeaveLobby: (String) -> Unit,
    onRefresh: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        if (lobbies.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Active Lobbies",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Text(
                        text = "Create or join a lobby to get started",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(lobbies) { lobby ->
                NewMyLobbyCard(
                    lobby = lobby,
                    isHost = lobby["hostUserId"] == currentUserId,
                    onViewDetails = { onViewDetails(lobby) },
                    onLeave = { onLeaveLobby(lobby["id"] as? String ?: "") }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun LobbyCard(
    order: GroupOrder,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        color = WingZoneOrange.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "CODE: ${order.code}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = WingZoneOrange,
                                letterSpacing = 1.sp
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Host",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = order.members.find { it.isHost }?.name ?: "Unknown",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = DarkGray,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
                
                Surface(
                    color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "OPEN",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoChip(
                    icon = Icons.Default.Person,
                    text = "${order.members.size}/${order.maxMembers}",
                    color = WingZoneOrange
                )
                InfoChip(
                    icon = Icons.Default.ShoppingCart,
                    text = "${order.totalItems} items",
                    color = WingZoneRed
                )
                InfoChip(
                    icon = Icons.Default.Star,
                    text = "RM ${String.format("%.2f", order.totalAmount)}",
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }
}

@Composable
fun MyLobbyCard(
    order: GroupOrder,
    isHost: Boolean,
    onViewDetails: () -> Unit,
    onLeave: () -> Unit,
    onCheckout: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onViewDetails
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Lobby ${order.code}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkGray
                        )
                        if (isHost) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = WingZoneRed,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "HOST",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Created ${SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(order.createdAt)}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                
                Surface(
                    color = when (order.status) {
                        GroupOrderStatus.OPEN -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                        GroupOrderStatus.FULL -> WingZoneOrange.copy(alpha = 0.2f)
                        else -> Color.Gray.copy(alpha = 0.2f)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = order.status.name,
                        color = when (order.status) {
                            GroupOrderStatus.OPEN -> Color(0xFF4CAF50)
                            GroupOrderStatus.FULL -> WingZoneOrange
                            else -> Color.Gray
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))
            
            // Members
            Text(
                text = "Members (${order.members.size}/${order.maxMembers})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = DarkGray
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            order.members.forEach { member ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(WingZoneOrange.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = member.name.first().uppercase(),
                                color = WingZoneRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = member.name,
                                fontSize = 14.sp,
                                color = DarkGray,
                                fontWeight = FontWeight.Medium
                            )
                            if (member.cartItems.isNotEmpty()) {
                                Text(
                                    text = "${member.cartItems.size} item${if (member.cartItems.size > 1) "s" else ""}",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                        if (member.cartItems.isNotEmpty()) {
                            Text(
                                text = "RM ${String.format("%.2f", member.memberTotal)}",
                                fontSize = 13.sp,
                                color = WingZoneRed,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = "No order",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                    
                    // Show member's items with full details
                    if (member.cartItems.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 44.dp)
                                .background(
                                    WingZoneOrange.copy(alpha = 0.05f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            member.cartItems.take(3).forEach { item ->
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Item name and quantity
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "• ${item.menuItem.name}",
                                            fontSize = 12.sp,
                                            color = DarkGray,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "x${item.quantity}",
                                                fontSize = 11.sp,
                                                color = TextSecondary,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "RM ${String.format("%.2f", item.subtotal)}",
                                                fontSize = 11.sp,
                                                color = WingZoneRed,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    
                                    // Customization details
                                    if (item.customization != null) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            // Flavor
                                            Text(
                                                text = "→ ${item.customization.flavor.name}",
                                                fontSize = 10.sp,
                                                color = TextSecondary,
                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                            )
                                            // Bone type
                                            if (item.customization.boneType != null) {
                                                Text(
                                                    text = "→ ${item.customization.boneType.name}",
                                                    fontSize = 10.sp,
                                                    color = TextSecondary,
                                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                                )
                                            }
                                            // Dipping sauce
                                            Text(
                                                text = "→ ${item.customization.dippingSauce.name} sauce",
                                                fontSize = 10.sp,
                                                color = TextSecondary,
                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                            )
                                            // Drink
                                            Text(
                                                text = "→ ${item.customization.drink.name}",
                                                fontSize = 10.sp,
                                                color = TextSecondary,
                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                            )
                                            // Fries exchange
                                            if (item.customization.friesExchange != null) {
                                                Text(
                                                    text = "→ ${item.customization.friesExchange.name} (+RM ${String.format("%.2f", item.customization.friesExchange.regularPrice)})",
                                                    fontSize = 10.sp,
                                                    color = WingZoneOrange,
                                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            if (member.cartItems.size > 3) {
                                Text(
                                    text = "+ ${member.cartItems.size - 3} more item${if (member.cartItems.size - 3 > 1) "s" else ""}",
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "RM ${String.format("%.2f", order.totalAmount)}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = WingZoneRed
                    )
                }
                
                if (isHost) {
                    // Host can checkout or close the lobby
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onLeave,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.Red
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(Color.Red)
                            )
                        ) {
                            Text("Close")
                        }
                        Button(
                            onClick = onCheckout,
                            colors = ButtonDefaults.buttonColors(containerColor = WingZoneRed),
                            enabled = order.totalAmount > 0
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Checkout")
                        }
                    }
                } else {
                    // Members can only leave
                    OutlinedButton(
                        onClick = onLeave,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.Red
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(Color.Red)
                        )
                    ) {
                        Text("Leave Lobby")
                    }
                }
            }
        }
    }
}

@Composable
fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color = WingZoneOrange
) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

// New lobby card components for the lobbies collection
@Composable
fun NewLobbyCard(
    lobby: Map<String, Any>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Lobby Code
            Surface(
                color = WingZoneOrange.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "CODE: ${lobby["code"]}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = WingZoneOrange,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Host info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Host",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = lobby["hostUserName"] as? String ?: "Unknown",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = DarkGray,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Location
            val location = lobby["location"] as? Map<String, Any>
            if (location != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = location["name"] as? String ?: "Unknown",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Members count
            val members = lobby["members"] as? List<*>
            val memberCount = members?.size ?: 0
            val maxMembers = (lobby["maxMembers"] as? Long)?.toInt() ?: 10
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                InfoChip(
                    text = "$memberCount/$maxMembers Members",
                    icon = Icons.Default.Person,
                    color = Color(0xFF4CAF50)
                )
                
                InfoChip(
                    text = lobby["orderType"] as? String ?: "Pickup",
                    icon = Icons.Default.ShoppingCart,
                    color = WingZoneOrange
                )
            }
        }
    }
}

@Composable
fun NewMyLobbyCard(
    lobby: Map<String, Any>,
    isHost: Boolean,
    onViewDetails: () -> Unit,
    onLeave: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header with code and host badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    color = WingZoneOrange.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = lobby["code"] as? String ?: "",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = WingZoneOrange,
                            letterSpacing = 2.sp
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
                
                if (isHost) {
                    Surface(
                        color = WingZoneRed.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "HOST",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = WingZoneRed
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Members
            val members = lobby["members"] as? List<Map<String, Any>>
            if (members != null && members.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Members",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${members.size} members",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = DarkGray,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Location
            val location = lobby["location"] as? Map<String, Any>
            if (location != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = location["name"] as? String ?: "Unknown",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("View Details")
                }
                
                if (!isHost) {
                    OutlinedButton(
                        onClick = onLeave,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = WingZoneRed
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WingZoneRed)
                    ) {
                        Text("Leave")
                    }
                }
            }
        }
    }
}
