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
    onAuthRequired: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Join Lobby", "My Lobbies")
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    val userGroupOrders = remember { mutableStateListOf<GroupOrder>() }
    var currentLobbyCode by remember { mutableStateOf<String?>(null) }
    
    // Load user's group orders
    LaunchedEffect(Unit) {
        if (authViewModel.isAuthenticated()) {
            userGroupOrders.clear()
            userGroupOrders.addAll(groupOrderViewModel.getUserGroupOrders())
        }
    }
    
    // Real-time listener for current lobby
    LaunchedEffect(currentLobbyCode) {
        currentLobbyCode?.let { code ->
            groupOrderViewModel.startListeningToGroupOrder(code) { updatedOrder ->
                if (updatedOrder != null) {
                    val index = userGroupOrders.indexOfFirst { it.code == code }
                    if (index >= 0) {
                        userGroupOrders[index] = updatedOrder
                    } else {
                        userGroupOrders.add(updatedOrder)
                    }
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { 
                        Text(
                            "Group Order",
                            fontWeight = FontWeight.Bold,
                            color = WingZoneRed,
                            fontSize = 24.sp
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White
                    ),
                    modifier = Modifier.statusBarsPadding()
                )
                
                // Banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = WingZoneRed
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.wingzone),
                            contentDescription = "WingZone Logo",
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Order Together, Save Together!",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
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
            
            // Content of the wingzone app
            when (selectedTab) {
                0 -> JoinLobbyTab(
                    onCreateClick = {
                        if (!authViewModel.isAuthenticated()) {
                            onAuthRequired()
                        } else {
                            showCreateDialog = true
                        }
                    },
                    onJoinClick = {
                        if (!authViewModel.isAuthenticated()) {
                            onAuthRequired()
                        } else {
                            showJoinDialog = true
                        }
                    },
                    groupOrders = userGroupOrders,
                    onJoinLobby = { code ->
                        if (!authViewModel.isAuthenticated()) {
                            onAuthRequired()
                        } else {
                            groupOrderViewModel.joinGroupOrder(code) { result ->
                                if (result.isSuccess) {
                                    currentLobbyCode = code
                                    userGroupOrders.clear()
                                    userGroupOrders.addAll(groupOrderViewModel.getUserGroupOrders())
                                }
                            }
                        }
                    }
                )
                1 -> MyLobbiesTab(
                    groupOrders = userGroupOrders,
                    currentUserId = authViewModel.getCurrentUser()?.id ?: "",
                    onViewDetails = { order ->
                        groupOrderViewModel.setCurrentGroupOrder(order)
                        // TODO: Navigate to GroupOrderDetailsScreen
                    },
                    onLeaveLobby = { orderId ->
                        groupOrderViewModel.leaveGroupOrder(orderId) { result ->
                            if (result.isSuccess) {
                                userGroupOrders.clear()
                                userGroupOrders.addAll(groupOrderViewModel.getUserGroupOrders())
                            }
                        }
                    },
                    onCheckout = { order ->
                        // Create ONE consolidated group order for admin
                        val orderRepository = FirebaseOrderRepository()
                        val allItems = order.members.flatMap { it.cartItems }
                        
                        if (allItems.isNotEmpty()) {
                            CoroutineScope(Dispatchers.Main).launch {
                                val consolidatedCart = Cart(items = allItems)
                                
                                // Build member details for delivery notes
                                val memberDetails = order.members.joinToString("\n") { member ->
                                    val itemCount = member.cartItems.sumOf { it.quantity }
                                    val memberTotal = member.cartItems.sumOf { it.subtotal }
                                    "${member.name}: $itemCount items (RM ${String.format("%.2f", memberTotal)})"
                                }
                                
                                val result = orderRepository.createGroupOrder(
                                    hostUserId = order.hostId,
                                    hostUserName = order.members.first { it.userId == order.hostId }.name,
                                    cart = consolidatedCart,
                                    groupOrderCode = order.code,
                                    memberCount = order.members.size,
                                    memberDetails = memberDetails,
                                    paymentMethod = "Group Order - Cash on Delivery",
                                    deliveryAddress = order.deliveryAddress,
                                    deliveryNotes = "GROUP ORDER: ${order.code}\nMembers: ${order.members.size}\n\n$memberDetails\n\n${order.specialInstructions ?: ""}",
                                    phoneNumber = ""
                                )
                                
                                if (result.isSuccess) {
                                    groupOrderViewModel.finalizeGroupOrder(order.id) { _ ->
                                        userGroupOrders.clear()
                                        userGroupOrders.addAll(groupOrderViewModel.getUserGroupOrders())
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
        
        // Create Lobby Dialog
        if (showCreateDialog) {
            CreateLobbyDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { deliveryAddress, instructions ->
                    groupOrderViewModel.createGroupOrder(deliveryAddress, instructions) { result ->
                        if (result.isSuccess) {
                            result.getOrNull()?.let { order ->
                                currentLobbyCode = order.code
                            }
                            userGroupOrders.clear()
                            userGroupOrders.addAll(groupOrderViewModel.getUserGroupOrders())
                            showCreateDialog = false
                        }
                    }
                }
            )
        }
        
        // Join Lobby Dialog
        if (showJoinDialog) {
            JoinLobbyDialog(
                onDismiss = { showJoinDialog = false },
                onJoin = { code ->
                    groupOrderViewModel.joinGroupOrder(code) { result ->
                        if (result.isSuccess) {
                            currentLobbyCode = code
                            userGroupOrders.clear()
                            userGroupOrders.addAll(groupOrderViewModel.getUserGroupOrders())
                            showJoinDialog = false
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun JoinLobbyTab(
    onCreateClick: () -> Unit,
    onJoinClick: () -> Unit,
    groupOrders: List<GroupOrder>,
    onJoinLobby: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        // Create Lobby Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = WingZoneOrange),
                onClick = onCreateClick
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Create New Lobby",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Start a group order and invite friends",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(16.dp)) }
        
        // Join by Code Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                onClick = onJoinClick
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Join",
                        tint = WingZoneRed,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Join with Code",
                            color = DarkGray,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Enter a lobby code to join",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
        
        // Available Lobbies
        if (groupOrders.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Available Lobbies",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkGray
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            items(groupOrders.filter { it.status == GroupOrderStatus.OPEN }) { order ->
                LobbyCard(
                    order = order,
                    onClick = { onJoinLobby(order.code) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun MyLobbiesTab(
    groupOrders: List<GroupOrder>,
    currentUserId: String,
    onViewDetails: (GroupOrder) -> Unit,
    onLeaveLobby: (String) -> Unit,
    onCheckout: (GroupOrder) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        if (groupOrders.isEmpty()) {
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
                        text = "No Active Group Orders",
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
            items(groupOrders) { order ->
                MyLobbyCard(
                    order = order,
                    isHost = order.hostId == currentUserId,
                    onViewDetails = { onViewDetails(order) },
                    onLeave = { onLeaveLobby(order.id) },
                    onCheckout = { onCheckout(order) }
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        onClick = onClick
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
                    Text(
                        text = "Lobby ${order.code}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkGray
                    )
                    Text(
                        text = "Host: ${order.members.find { it.isHost }?.name ?: "Unknown"}",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
                
                Surface(
                    color = WingZoneOrange.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = order.status.name,
                        color = WingZoneOrange,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoChip(
                    icon = Icons.Default.Person,
                    text = "${order.members.size}/${order.maxMembers}"
                )
                InfoChip(
                    icon = Icons.Default.ShoppingCart,
                    text = "${order.totalItems} items"
                )
                InfoChip(
                    icon = Icons.Default.Star,
                    text = "RM ${String.format("%.2f", order.totalAmount)}"
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(BackgroundGray, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = WingZoneRed,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            color = DarkGray,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun CreateLobbyDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var deliveryAddress by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Create Group Lobby",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = WingZoneRed
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = deliveryAddress,
                    onValueChange = { deliveryAddress = it },
                    label = { Text("Delivery Address (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Special Instructions (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = { onCreate(deliveryAddress, instructions) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = WingZoneRed)
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}

@Composable
fun JoinLobbyDialog(
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Join Group Lobby",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = WingZoneRed
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = code,
                    onValueChange = { 
                        code = it.uppercase()
                        error = null
                    },
                    label = { Text("Lobby Code") },
                    placeholder = { Text("e.g., WZABCD") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    isError = error != null
                )
                
                if (error != null) {
                    Text(
                        text = error!!,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            if (code.isBlank()) {
                                error = "Please enter a lobby code"
                            } else {
                                onJoin(code)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = WingZoneRed)
                    ) {
                        Text("Join")
                    }
                }
            }
        }
    }
}
