package wingzone.zenith.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import wingzone.zenith.data.models.CartItem
import wingzone.zenith.data.models.GroupMember
import wingzone.zenith.data.models.GroupOrder
import wingzone.zenith.data.models.GroupOrderStatus
import wingzone.zenith.ui.theme.*
import wingzone.zenith.viewmodel.AuthViewModel
import wingzone.zenith.viewmodel.GroupOrderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupOrderDetailsScreen(
    groupOrder: GroupOrder,
    authViewModel: AuthViewModel,
    groupOrderViewModel: GroupOrderViewModel,
    onBack: () -> Unit,
    onNavigateToMenu: () -> Unit
) {
    val currentUser = authViewModel.currentUser.collectAsState().value
    val currentUserId = currentUser?.id ?: ""
    val isHost = groupOrder.hostId == currentUserId
    val currentMember = groupOrder.members.find { it.userId == currentUserId }
    
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showFinalizeDialog by remember { mutableStateOf(false) }
    var showPayAllDialog by remember { mutableStateOf(false) }
    var memberToPayFor by remember { mutableStateOf<GroupMember?>(null) }
    var memberToKick by remember { mutableStateOf<GroupMember?>(null) }
    
    val unpaidMembers = remember(groupOrder) {
        groupOrder.members.filter { !it.hasPaid && it.cartItems.isNotEmpty() }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            "Lobby ${groupOrder.code}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = groupOrder.status.name,
                            fontSize = 12.sp,
                            color = when (groupOrder.status) {
                                GroupOrderStatus.OPEN -> Color(0xFF4CAF50)
                                GroupOrderStatus.ORDERING -> WingZoneOrange
                                GroupOrderStatus.CONFIRMED -> WingZoneRed
                                else -> Color.Gray
                            }
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            if (groupOrder.status == GroupOrderStatus.ORDERING) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Total Summary
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total Order",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "RM ${String.format("%.2f", groupOrder.totalAmount)}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = WingZoneRed
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Action buttons
                        if (isHost) {
                            if (groupOrder.canFinalize) {
                                Button(
                                    onClick = { showFinalizeDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = WingZoneRed),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Finalize Order",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = { showPayAllDialog = true },
                                        modifier = Modifier.weight(1f),
                                        enabled = unpaidMembers.isNotEmpty(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF4CAF50),
                                            disabledContainerColor = Color.Gray
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.AccountCircle, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Pay for ${unpaidMembers.size}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    
                                    Button(
                                        onClick = { /* Disabled until all paid */ },
                                        modifier = Modifier.weight(1f),
                                        enabled = false,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = WingZoneRed,
                                            disabledContainerColor = Color.Gray
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Finalize",
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        } else if (currentMember != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = onNavigateToMenu,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = WingZoneOrange),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Items")
                                }
                                
                                Button(
                                    onClick = { showPaymentDialog = true },
                                    modifier = Modifier.weight(1f),
                                    enabled = !currentMember.hasPaid && currentMember.cartItems.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = WingZoneRed,
                                        disabledContainerColor = Color.Gray
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        if (currentMember.hasPaid) Icons.Default.CheckCircle else Icons.Default.Send,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (currentMember.hasPaid) "Paid" else "Pay Now")
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Banner
            item {
                if (groupOrder.status == GroupOrderStatus.OPEN && isHost) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = WingZoneOrange.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = WingZoneOrange
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Waiting for members",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    "Start ordering when everyone has joined",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                            Button(
                                onClick = {
                                    groupOrderViewModel.startOrdering(groupOrder.id) { result ->
                                        if (result.isFailure) {
                                            // Handle error
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = WingZoneOrange)
                            ) {
                                Text("Start Ordering")
                            }
                        }
                    }
                }
            }
            
            // Current User's Order Section
            if (currentMember != null && groupOrder.status == GroupOrderStatus.ORDERING) {
                item {
                    MyOrderCard(
                        member = currentMember,
                        onNavigateToMenu = onNavigateToMenu,
                        onRemoveItem = { index ->
                            groupOrderViewModel.removeItemFromGroupOrder(
                                groupOrder.id,
                                currentUserId,
                                index
                            ) { }
                        }
                    )
                }
            }
            
            // Payment Status Summary
            if (groupOrder.status == GroupOrderStatus.ORDERING) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                "Payment Status",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkGray
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            val paidCount = groupOrder.members.count { it.hasPaid || it.cartItems.isEmpty() }
                            val totalCount = groupOrder.members.size
                            
                            LinearProgressIndicator(
                                progress = { paidCount.toFloat() / totalCount.toFloat() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (groupOrder.allMembersPaid) Color(0xFF4CAF50) else WingZoneOrange,
                                trackColor = Color.LightGray
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                "$paidCount of $totalCount members paid",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
            
            // Members and their orders
            item {
                Text(
                    "Members (${groupOrder.members.size}/${groupOrder.maxMembers})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkGray
                )
            }
            
            groupOrder.members.forEach { member ->
                item {
                    MemberOrderCard(
                        member = member,
                        isCurrentUser = member.userId == currentUserId,
                        isHost = member.isHost,
                        isViewerHost = isHost,
                        canRemoveItems = member.userId == currentUserId && groupOrder.status == GroupOrderStatus.ORDERING && !member.hasPaid,
                        onRemoveItem = { itemIndex ->
                            groupOrderViewModel.removeItemFromGroupOrder(
                                groupOrder.id,
                                member.userId,
                                itemIndex
                            ) { result ->
                                if (result.isFailure) {
                                    // Handle error
                                }
                            }
                        },
                        onPayForMember = {
                            memberToPayFor = member
                        },
                        onKickMember = {
                            memberToKick = member
                        }
                    )
                }
            }
        }
    }
    
    // Payment Dialog
    if (showPaymentDialog && currentMember != null) {
        PaymentConfirmationDialog(
            memberTotal = currentMember.memberTotal,
            onDismiss = { showPaymentDialog = false },
            onConfirm = {
                groupOrderViewModel.markMemberAsPaid(groupOrder.id, currentUserId) { result ->
                    if (result.isSuccess) {
                        showPaymentDialog = false
                    }
                }
            }
        )
    }
    
    // Finalize Dialog
    if (showFinalizeDialog) {
        FinalizeOrderDialog(
            totalAmount = groupOrder.totalAmount,
            onDismiss = { showFinalizeDialog = false },
            onConfirm = {
                groupOrderViewModel.finalizeGroupOrder(groupOrder.id) { result ->
                    if (result.isSuccess) {
                        showFinalizeDialog = false
                        onBack()
                    }
                }
            }
        )
    }
    
    // Pay All Dialog
    if (showPayAllDialog) {
        PayAllMembersDialog(
            unpaidMembers = unpaidMembers,
            onDismiss = { showPayAllDialog = false },
            onConfirm = {
                // Mark all unpaid members as paid
                unpaidMembers.forEach { member ->
                    groupOrderViewModel.payForMember(groupOrder.id, member.userId) { result ->
                        // Handle result
                    }
                }
                showPayAllDialog = false
            }
        )
    }
    
    // Pay For Member Dialog
    if (memberToPayFor != null) {
        PayForMemberDialog(
            member = memberToPayFor!!,
            onDismiss = { memberToPayFor = null },
            onConfirm = {
                groupOrderViewModel.payForMember(groupOrder.id, memberToPayFor!!.userId) { result ->
                    if (result.isSuccess) {
                        memberToPayFor = null
                    }
                }
            }
        )
    }
    
    // Kick Member Dialog
    if (memberToKick != null) {
        KickMemberDialog(
            member = memberToKick!!,
            onDismiss = { memberToKick = null },
            onConfirm = {
                groupOrderViewModel.kickMember(groupOrder.id, memberToKick!!.userId) { result ->
                    if (result.isSuccess) {
                        memberToKick = null
                    }
                }
            }
        )
    }
}

@Composable
fun MemberOrderCard(
    member: GroupMember,
    isCurrentUser: Boolean,
    isHost: Boolean,
    canRemoveItems: Boolean,
    isViewerHost: Boolean = false,
    onRemoveItem: (Int) -> Unit,
    onPayForMember: () -> Unit = {},
    onKickMember: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser) WingZoneOrange.copy(alpha = 0.05f) else Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (isCurrentUser) BorderStroke(2.dp, WingZoneOrange) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Member Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(WingZoneOrange.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = member.name.first().uppercase(),
                            color = WingZoneRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = member.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (isHost) {
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
                            if (isCurrentUser) {
                                Surface(
                                    color = WingZoneOrange,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "YOU",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (member.cartItems.isEmpty()) "No items yet" else "${member.cartItems.size} items",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
                
                // Payment Status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (member.cartItems.isNotEmpty()) {
                        Text(
                            text = "RM ${String.format("%.2f", member.memberTotal)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = WingZoneRed
                        )
                    }
                    Icon(
                        imageVector = if (member.hasPaid || member.cartItems.isEmpty()) Icons.Default.CheckCircle else Icons.Default.Clear,
                        contentDescription = if (member.hasPaid) "Paid" else "Unpaid",
                        tint = if (member.hasPaid || member.cartItems.isEmpty()) Color(0xFF4CAF50) else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Member's Items
            if (member.cartItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                
                member.cartItems.forEachIndexed { index, item ->
                    CartItemRow(
                        item = item,
                        canRemove = canRemoveItems,
                        onRemove = { onRemoveItem(index) }
                    )
                    if (index < member.cartItems.size - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
            
            // Host Actions (only show for unpaid members, not for host themselves)
            if (isViewerHost && !isHost && !member.hasPaid && member.cartItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onKickMember,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.Red
                        ),
                        border = BorderStroke(1.dp, Color.Red)
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kick", fontSize = 12.sp)
                    }
                    
                    Button(
                        onClick = onPayForMember,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pay for them", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun CartItemRow(
    item: CartItem,
    canRemove: Boolean,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.menuItem.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            if (item.customization != null) {
                Text(
                    text = "${item.customization.flavor.name} • ${item.customization.boneType?.name ?: ""}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "x${item.quantity}",
                fontSize = 14.sp,
                color = TextSecondary
            )
            Text(
                text = "RM ${String.format("%.2f", item.subtotal)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = WingZoneRed
            )
            if (canRemove) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = Color.Red,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PaymentConfirmationDialog(
    memberTotal: Double,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Send,
                contentDescription = null,
                tint = WingZoneRed,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                "Confirm Payment",
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "You are about to mark your order as paid",
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "RM ${String.format("%.2f", memberTotal)}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = WingZoneRed
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Please ensure you have completed the payment",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = WingZoneRed)
            ) {
                Text("Confirm Payment")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun FinalizeOrderDialog(
    totalAmount: Double,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                "Finalize Group Order",
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "All members have paid. Ready to send to kitchen?",
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "RM ${String.format("%.2f", totalAmount)}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = WingZoneRed
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "This action cannot be undone",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("Finalize Order")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PayAllMembersDialog(
    unpaidMembers: List<GroupMember>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val totalUnpaid = unpaidMembers.sumOf { it.memberTotal }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                "Pay for All Members",
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "You are about to pay for ${unpaidMembers.size} member${if (unpaidMembers.size > 1) "s" else ""}",
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // List unpaid members
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        unpaidMembers.forEach { member ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = member.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "RM ${String.format("%.2f", member.memberTotal)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WingZoneRed
                                )
                            }
                        }
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "RM ${String.format("%.2f", totalUnpaid)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = WingZoneRed
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    "This will mark all listed members as paid",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("Confirm Payment")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PayForMemberDialog(
    member: GroupMember,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                "Pay for ${member.name}",
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "You are about to pay for ${member.name}'s order",
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "RM ${String.format("%.2f", member.memberTotal)}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = WingZoneRed
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "${member.name} will be marked as paid",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("Confirm Payment")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun KickMemberDialog(
    member: GroupMember,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = Color.Red,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                "Kick ${member.name}?",
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Are you sure you want to remove ${member.name} from this lobby?",
                    textAlign = TextAlign.Center
                )
                
                if (member.cartItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Their order will be removed:",
                                fontSize = 12.sp,
                                color = Color.Red,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${member.cartItems.size} item${if (member.cartItems.size > 1) "s" else ""}",
                                fontSize = 12.sp
                            )
                            Text(
                                "RM ${String.format("%.2f", member.memberTotal)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = WingZoneRed
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "This action cannot be undone",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Kick Member")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun MyOrderCard(
    member: GroupMember,
    onNavigateToMenu: () -> Unit,
    onRemoveItem: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = WingZoneOrange.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(2.dp, WingZoneOrange)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = WingZoneOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Your Order",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkGray
                    )
                }
                Icon(
                    if (member.hasPaid) Icons.Default.CheckCircle else Icons.Default.Clear,
                    contentDescription = if (member.hasPaid) "Paid" else "Unpaid",
                    tint = if (member.hasPaid) Color(0xFF4CAF50) else Color.Gray,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            if (member.cartItems.isEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "No items added yet",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onNavigateToMenu,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = WingZoneOrange
                    ),
                    border = BorderStroke(2.dp, WingZoneOrange)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Items to Order")
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = WingZoneOrange.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))
                
                // User's items
                member.cartItems.forEachIndexed { index, item ->
                    MyCartItemRow(
                        item = item,
                        canEdit = !member.hasPaid,
                        onRemove = { onRemoveItem(index) }
                    )
                    if (index < member.cartItems.size - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = WingZoneOrange.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Your Total",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                        Text(
                            "RM ${String.format("%.2f", member.memberTotal)}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = WingZoneOrange
                        )
                    }
                    
                    if (!member.hasPaid) {
                        OutlinedButton(
                            onClick = onNavigateToMenu,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = WingZoneOrange
                            ),
                            border = BorderStroke(1.dp, WingZoneOrange)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add More", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    highlighted: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (highlighted) WingZoneOrange else Color.Gray,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "$label:",
            fontSize = 12.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(50.dp)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = if (highlighted) WingZoneOrange else DarkGray,
            fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun MyCartItemRow(
    item: CartItem,
    canEdit: Boolean,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header: Item name, quantity, price, delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.menuItem.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkGray,
                    modifier = Modifier.weight(1f)
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = WingZoneOrange.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "x${item.quantity}",
                            fontSize = 13.sp,
                            color = WingZoneOrange,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    
                    Text(
                        text = "RM ${String.format("%.2f", item.subtotal)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = WingZoneOrange
                    )
                    
                    if (canEdit) {
                        IconButton(
                            onClick = onRemove,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Remove",
                                tint = Color.Red,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
            
            // Customization details
            if (item.customization != null) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Flavor
                    DetailRow(
                        icon = Icons.Default.Star,
                        label = "Flavor",
                        value = item.customization.flavor.name
                    )
                    
                    // Bone Type
                    if (item.customization.boneType != null) {
                        DetailRow(
                            icon = Icons.Default.Info,
                            label = "Type",
                            value = item.customization.boneType.name
                        )
                    }
                    
                    // Dipping Sauce
                    DetailRow(
                        icon = Icons.Default.Star,
                        label = "Sauce",
                        value = item.customization.dippingSauce.name
                    )
                    
                    // Drink
                    DetailRow(
                        icon = Icons.Default.Star,
                        label = "Drink",
                        value = item.customization.drink.name
                    )
                    
                    // Fries Exchange
                    if (item.customization.friesExchange != null) {
                        DetailRow(
                            icon = Icons.Default.Add,
                            label = "Extra",
                            value = "${item.customization.friesExchange.name} (+RM ${String.format("%.2f", item.customization.friesExchange.regularPrice)})",
                            highlighted = true
                        )
                    }
                }
            }
        }
    }
}
