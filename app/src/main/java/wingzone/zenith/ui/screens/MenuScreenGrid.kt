package wingzone.zenith.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import wingzone.zenith.data.models.MenuItem
import wingzone.zenith.ui.components.SimpleItemBottomSheet
import wingzone.zenith.ui.theme.*
import wingzone.zenith.viewmodel.AuthViewModel
import wingzone.zenith.viewmodel.CartViewModel
import wingzone.zenith.viewmodel.MenuViewModel
import wingzone.zenith.viewmodel.MenuState
import wingzone.zenith.viewmodel.MenuCategory
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreenGrid(
    menuViewModel: MenuViewModel = MenuViewModel(),
    cartViewModel: CartViewModel = CartViewModel(),
    authViewModel: AuthViewModel = AuthViewModel(),
    groupOrderViewModel: wingzone.zenith.viewmodel.GroupOrderViewModel = wingzone.zenith.viewmodel.GroupOrderViewModel(),
    lobbyViewModel: wingzone.zenith.viewmodel.LobbyViewModel? = null,
    onAuthRequired: () -> Unit = {},
    onNavigateToCart: () -> Unit = {}
) {
    val menuState by menuViewModel.menuState.collectAsState()
    val cart by cartViewModel.cart.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val currentGroupOrder by groupOrderViewModel.currentGroupOrder.collectAsState()
    val currentLobbyId by (lobbyViewModel?.currentLobbyId ?: kotlinx.coroutines.flow.MutableStateFlow(null)).collectAsState()
    val isAuthenticated = currentUser != null
    
    var selectedMenuItem by remember { mutableStateOf<MenuItem?>(null) }
    var showCustomizationDialog by remember { mutableStateOf(false) }
    var showSimpleItemSheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var userHasPaid by remember { mutableStateOf(false) }
    
    // Check if user has paid in lobby
    LaunchedEffect(currentLobbyId, currentUser?.id) {
        if (currentLobbyId != null && currentUser?.id != null) {
            try {
                val lobbyDoc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("lobbies")
                    .document(currentLobbyId!!)
                    .get()
                    .await()
                
                if (lobbyDoc.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    val members = lobbyDoc.get("members") as? List<Map<String, Any>> ?: emptyList()
                    val currentMember = members.find { it["userId"] == currentUser?.id }
                    userHasPaid = currentMember?.get("hasPaid") as? Boolean ?: false
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            userHasPaid = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Menu",
                        fontWeight = FontWeight.Bold,
                        color = WingZoneRed,
                        fontSize = 24.sp
                    )
                },
                actions = {
                    // Cart Badge
                    if (cart.totalItems > 0) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = WingZoneRed,
                                    contentColor = Color.White
                                ) {
                                    Text(cart.totalItems.toString())
                                }
                            }
                        ) {
                            IconButton(onClick = onNavigateToCart) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = "Cart",
                                    tint = WingZoneRed
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = BackgroundGray
    ) { paddingValues ->
        when (val state = menuState) {
            is MenuState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = WingZoneRed)
                }
            }
            
            is MenuState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(state.message, color = Color.Red)
                }
            }
            
            is MenuState.Success -> {
                var selectedCategoryIndex by remember { mutableStateOf(0) }
                
                // Track visible category based on scroll position
                LaunchedEffect(listState.firstVisibleItemIndex) {
                    var itemCount = 0
                    for (i in state.categories.indices) {
                        val categoryItemCount = 1 + ((state.categories[i].items.size + 1) / 2)
                        if (listState.firstVisibleItemIndex < itemCount + categoryItemCount) {
                            selectedCategoryIndex = i
                            break
                        }
                        itemCount += categoryItemCount
                    }
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Category Sidebar
                    CategorySidebarGrid(
                        categories = state.categories,
                        selectedIndex = selectedCategoryIndex,
                        onCategorySelected = { index ->
                            selectedCategoryIndex = index
                            coroutineScope.launch {
                                // Calculate item index for this category
                                var itemIndex = 0
                                for (i in 0 until index) {
                                    itemIndex += 1 + ((state.categories[i].items.size + 1) / 2)
                                }
                                listState.animateScrollToItem(itemIndex)
                            }
                        }
                    )
                    
                    // Menu Grid Content
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        state.categories.forEach { category ->
                            // Category Header
                            item(key = "header_${category.name}") {
                                Text(
                                    text = category.name,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WingZoneRed,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        
                        // Items in 2x2 Grid
                        val items = category.items
                        val rows = (items.size + 1) / 2
                        
                        items(rows, key = { rowIndex -> "row_${category.name}_$rowIndex" }) { rowIndex ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                for (columnIndex in 0..1) {
                                    val itemIndex = rowIndex * 2 + columnIndex
                                    if (itemIndex < items.size) {
                                        MenuItemGridCard(
                                            item = items[itemIndex],
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                if (!isAuthenticated) {
                                                    onAuthRequired()
                                                    return@MenuItemGridCard
                                                }
                                                selectedMenuItem = items[itemIndex]
                                                if (items[itemIndex].requiresCustomization) {
                                                    showCustomizationDialog = true
                                                } else {
                                                    // Show confirmation sheet for simple items
                                                    showSimpleItemSheet = true
                                                }
                                            }
                                        )
                                    } else {
                                        // Empty spacer for odd number of items
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
                }
            }
        }
    }
    
    // Simple Item Bottom Sheet
    if (showSimpleItemSheet && selectedMenuItem != null) {
        SimpleItemBottomSheet(
            menuItem = selectedMenuItem!!,
            onDismiss = {
                showSimpleItemSheet = false
                selectedMenuItem = null
            },
            onAddToCart = { quantity ->
                // Add to personal cart
                cartViewModel.addItem(selectedMenuItem!!, quantity)
                
                // If in lobby, also sync to lobby
                if (currentGroupOrder != null) {
                    val cartItem = wingzone.zenith.data.models.CartItem(
                        menuItem = selectedMenuItem!!,
                        quantity = quantity,
                        customization = null
                    )
                    groupOrderViewModel.addItemToGroupOrder(currentGroupOrder!!.id, cartItem) { result ->
                        // Silently handle - item is in personal cart
                    }
                }
                
                showSimpleItemSheet = false
                selectedMenuItem = null
            }
        )
    }
    
    // Show customization dialog
    if (showCustomizationDialog && selectedMenuItem != null) {
        EntreeCustomizationDialog(
            menuItem = selectedMenuItem!!,
            onDismiss = { showCustomizationDialog = false },
            onConfirm = { quantity, customization ->
                // Always dismiss dialog first for better UX
                showCustomizationDialog = false
                
                // ALWAYS add to personal cart first
                cartViewModel.addItem(selectedMenuItem!!, quantity, customization)
                
                // If in lobby, also sync to lobby
                if (currentGroupOrder != null) {
                    val cartItem = wingzone.zenith.data.models.CartItem(
                        menuItem = selectedMenuItem!!,
                        quantity = quantity,
                        customization = customization
                    )
                    // Use currentLobbyId from LobbyViewModel for group order syncing
                    if (currentLobbyId != null) {
                        groupOrderViewModel.addItemToGroupOrder(currentLobbyId!!, cartItem) { result ->
                            // Handle result if needed (show toast, etc.)
                        }
                    }
                }
            },
            activeLobbyId = currentLobbyId,
            activeLobbyCode = currentGroupOrder?.code,
            userHasPaid = userHasPaid
        )
    }
}

@Composable
fun MenuItemGridCard(
    item: MenuItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = if (item.isAvailable) onClick else ({})
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Image with overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                if (!item.imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = item.name,
                        tint = Color.Gray.copy(alpha = 0.3f),
                        modifier = Modifier.size(60.dp)
                    )
                }
                
                // White overlay for unavailable items
                if (!item.isAvailable) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "NOT AVAILABLE",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = WingZoneRed,
                            modifier = Modifier
                                .rotate(-15f)
                                .padding(8.dp)
                        )
                    }
                }
            }
            
            // Info
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = item.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = getAdaptiveTextPrimary(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (item.description.isNotEmpty()) {
                    Text(
                        text = item.description,
                        fontSize = 12.sp,
                        color = getAdaptiveTextSecondary(),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RM ${String.format("%.2f", item.price)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = WingZoneRed
                    )
                    
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = WingZoneRed
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            tint = Color.White,
                            modifier = Modifier.padding(4.dp).size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategorySidebarGrid(
    categories: List<MenuCategory>,
    selectedIndex: Int,
    onCategorySelected: (Int) -> Unit
) {
    Surface(
        modifier = Modifier
            .width(100.dp)
            .fillMaxHeight(),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
        ) {
            items(categories.size) { index ->
                CategoryTabItemGrid(
                    category = categories[index],
                    isSelected = index == selectedIndex,
                    onClick = { onCategorySelected(index) }
                )
            }
        }
    }
}

@Composable
fun CategoryTabItemGrid(
    category: MenuCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) WingZoneRed.copy(alpha = 0.1f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon placeholder
        Icon(
            imageVector = Icons.Default.ShoppingCart,
            contentDescription = category.name,
            tint = if (isSelected) WingZoneRed else Color.Gray,
            modifier = Modifier.size(32.dp)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = category.name,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) WingZoneRed else TextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        
        if (isSelected) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(3.dp)
                    .background(WingZoneRed, RoundedCornerShape(2.dp))
            )
        }
    }
}
