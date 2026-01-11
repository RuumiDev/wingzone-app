package wingzone.zenith.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import wingzone.zenith.ui.theme.*
import wingzone.zenith.viewmodel.AuthViewModel
import wingzone.zenith.viewmodel.CartViewModel
import wingzone.zenith.viewmodel.GroupOrderViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    authViewModel: AuthViewModel = AuthViewModel(),
    cartViewModel: CartViewModel = CartViewModel(),
    groupOrderViewModel: GroupOrderViewModel = GroupOrderViewModel(),
    lobbyViewModel: wingzone.zenith.viewmodel.LobbyViewModel,
    onAuthRequired: () -> Unit = {},
    onNavigateToOrderTracking: () -> Unit = {},
    onNavigateToOrderHistory: () -> Unit = {},
    onNavigateToOrderDetails: (String) -> Unit = {},
    onNavigateToCreateLobby: () -> Unit = {},
    onNavigateToJoinLobby: () -> Unit = {},
    onNavigateToGroupOrder: () -> Unit = {},
    onNavigateToLobbyDetail: (String) -> Unit = {},
    initialTab: Int = 0
) {
    var selectedTab by remember { mutableStateOf(initialTab) }
    var showExitDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = context as? Activity
    
    // Handle back button press - show exit confirmation only on home tab
    BackHandler(enabled = selectedTab == 0) {
        showExitDialog = true
    }
    
    // Back handler for other tabs - return to home
    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }
    
    // Clear currentGroupOrder when navigating away from menu tab (not in lobby context)
    LaunchedEffect(selectedTab) {
        if (selectedTab != 1) {
            // Clear group order context when leaving menu
            // This ensures personal cart is used by default
            val currentOrder = groupOrderViewModel.currentGroupOrder.value
            if (currentOrder != null && selectedTab != 3) {
                // Only clear if not going to Group Order tab
                groupOrderViewModel.setCurrentGroupOrder(null)
            }
        }
    }
    
    // Exit confirmation dialog
    if (showExitDialog) {
        ExitConfirmationDialog(
            onConfirm = {
                activity?.finish()
            },
            onDismiss = {
                showExitDialog = false
            }
        )
    }
     
    
    Scaffold(
        topBar = { 
            if (selectedTab == 0) HomeTopBar(authViewModel = authViewModel) 
        },
        bottomBar = { BottomNavigationBar(selectedTab) { selectedTab = it } },
        containerColor = BackgroundGray
    ) { paddingValues ->
        when (selectedTab) {
            0 -> {
                // Home Tab
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Balance Cards Section
                    BalanceCardsSection()
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Promo Carousel
                    PromoCarousel()
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Group Order Feature Banner
                    GroupOrderBanner(
                        onNavigateToCreateLobby = onNavigateToCreateLobby,
                        onNavigateToJoinLobby = onNavigateToJoinLobby,
                        onOrderNow = { selectedTab = 3 }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Delivery & Pickup Options
                    DeliveryPickupSection()
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            1 -> {
                // Menu Tab - 2x2 Grid with Seamless Scrolling
                MenuScreenGrid(
                    cartViewModel = cartViewModel,
                    authViewModel = authViewModel,
                    groupOrderViewModel = groupOrderViewModel,
                    onAuthRequired = onAuthRequired,
                    onNavigateToCart = { selectedTab = 2 }
                )
            }
            2 -> {
                // Cart Tab
                CartScreen(
                    cartViewModel = cartViewModel,
                    authViewModel = authViewModel,
                    onAuthRequired = onAuthRequired,
                    onCheckoutClick = { /* TODO: Navigate to checkout */ }
                )
            }
            3 -> {
                // Group Order Tab
                GroupOrderScreen(
                    authViewModel = authViewModel,
                    groupOrderViewModel = groupOrderViewModel,
                    lobbyViewModel = lobbyViewModel,
                    onAuthRequired = onAuthRequired,
                    onNavigateToCreateLobby = onNavigateToCreateLobby,
                    onNavigateToJoinLobby = onNavigateToJoinLobby,
                    onNavigateToLobbyDetail = onNavigateToLobbyDetail
                )
            }
            4 -> {
                // Account Tab
                AccountScreen(
                    authViewModel = authViewModel,
                    onAuthRequired = onAuthRequired,
                    onNavigateToOrderTracking = onNavigateToOrderTracking,
                    onNavigateToOrderHistory = onNavigateToOrderHistory
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    authViewModel: AuthViewModel = AuthViewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    
    val greeting = remember {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            else -> "Good evening"
        }
    }
    
    val displayName = currentUser?.name ?: "Wingers"
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "$greeting, $displayName",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = getAdaptiveTextPrimary(),
                        fontSize = 16.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Language Selector
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF5F5F5),
                    onClick = { /* Language selection */ }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "EN",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Select language",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                // Country Flag (Placeholder)
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFE3F2FD)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Country",
                            tint = WingZoneRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BalanceCardsSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // WZ Balance Card
        BalanceCard(
            modifier = Modifier.weight(1f),
            title = "WZ Balance (RM)",
            value = "0.00",
            badge = "Get 20% OFF",
            badgeColor = WingZoneRed,
            icon = Icons.Default.ShoppingCart
        )
        
        // Easy Goer Card
        BalanceCard(
            modifier = Modifier.weight(1f),
            title = "Easy Goer",
            value = "0 / 10",
            badge = null,
            badgeColor = null,
            icon = Icons.Default.Add
        )
        
        // WZ Points Card
        BalanceCard(
            modifier = Modifier.weight(1f),
            title = "WZ Points",
            value = "0 pts",
            badge = "Daily check-in",
            badgeColor = WingZoneOrange,
            icon = Icons.Default.Star
        )
    }
}

@Composable
fun BalanceCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    badge: String?,
    badgeColor: Color?,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Box(modifier = modifier) {
        // Main Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .padding(top = if (badge != null) 8.dp else 0.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.sp,
                        color = TextSecondary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                    )
                    
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = WingZoneRed,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        
        // Badge Overlay (positioned on top)
        if (badge != null && badgeColor != null) {
            Surface(
                color = badgeColor,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 8.dp, top = 4.dp)
            ) {
                Text(
                    text = badge,
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun PromoCarousel() {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    
    // Simulate loading delay
    LaunchedEffect(Unit) {
        delay(800) // Simulate data loading
        isLoading = false
    }
    
    // Auto-scroll effect
    LaunchedEffect(isLoading) {
        if (!isLoading) {
            while (true) {
                delay(5000) // 5 seconds delay
                val nextPage = (pagerState.currentPage + 1) % 3
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isLoading) {
            // Skeleton Loader
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.LightGray.copy(alpha = 0.3f))
            )
        } else {
            // Carousel
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) { page ->
                PromoBanner(pageIndex = page)
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Page Indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .size(if (pagerState.currentPage == index) 24.dp else 8.dp, 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (pagerState.currentPage == index) WingZoneRed 
                            else Color.Gray.copy(alpha = 0.3f)
                        )
                        .clickable {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                )
                if (index < 2) {
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }
    }
}

@Composable
fun PromoBanner(pageIndex: Int) {
    val promoData = when (pageIndex) {
        0 -> PromoData(
            title = "SPICY",
            subtitle = "WING COMBO",
            description = "LIMITED TIME",
            backgroundColor = WingZoneRed,
            accentColor = WingZoneOrange
        )
        1 -> PromoData(
            title = "CRISPY",
            subtitle = "TENDERS",
            description = "NEW FLAVOR",
            backgroundColor = WingZoneRedLight,
            accentColor = WingZoneOrange
        )
        else -> PromoData(
            title = "FAMILY",
            subtitle = "FEAST DEAL",
            description = "SAVE 30%",
            backgroundColor = WingZoneOrange,
            accentColor = WingZoneRed
        )
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = promoData.backgroundColor)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Promo content
                Column {
                    Text(
                        text = promoData.title,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = promoData.subtitle,
                        color = promoData.accentColor,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = promoData.description,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Center area for product image (placeholder)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "Product",
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(120.dp)
                    )
                }
                
                // Bottom button
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.2f),
                    onClick = { /* Find out more */ }
                ) {
                    Text(
                        text = "Find Out More",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

data class PromoData(
    val title: String,
    val subtitle: String,
    val description: String,
    val backgroundColor: Color,
    val accentColor: Color
)

@Composable
fun GroupOrderBanner(
    onNavigateToCreateLobby: () -> Unit,
    onNavigateToJoinLobby: () -> Unit,
    onOrderNow: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WingZoneOrange),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Special",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "NEW FEATURE",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Lobby / Group Order",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    fontSize = 24.sp
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Order together & save more!",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.95f),
                    fontWeight = FontWeight.Medium
                ),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Order Now Button
            Button(
                onClick = onOrderNow,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = WingZoneOrange
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
            ) {
                Text(
                    text = "Order Now!",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateToCreateLobby,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Create",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                
                OutlinedButton(
                    onClick = onNavigateToJoinLobby,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Join",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun DeliveryPickupSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Dine-in Card
        OrderTypeCard(
            modifier = Modifier.weight(1f),
            title = "DINE-IN",
            icon = Icons.Default.Home,
            onClick = { /* Navigate to dine-in */ }
        )
        
        // Pickup Card
        OrderTypeCard(
            modifier = Modifier.weight(1f),
            title = "PICKUP",
            icon = Icons.Default.ShoppingCart,
            onClick = { /* Navigate to pickup */ }
        )
    }
}

@Composable
fun OrderTypeCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.height(180.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon placeholder
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        color = Color(0xFFF5F5F5),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = WingZoneRed,
                    modifier = Modifier.size(64.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = WingZoneRed,
                    fontSize = 18.sp
                )
            )
        }
    }
}

@Composable
fun BottomNavigationBar(
    selectedItem: Int,
    onItemSelected: (Int) -> Unit
) {
    val items = listOf("Home", "Menu", "Cart", "Group", "Account")
    val icons = listOf(
        Icons.Default.Home,
        Icons.Default.Menu,
        Icons.Default.ShoppingCart,
        Icons.Default.Person,
        Icons.Default.AccountCircle
    )
    
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = icons[index],
                        contentDescription = item,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = item,
                        fontSize = 11.sp,
                        fontWeight = if (selectedItem == index) FontWeight.Bold else FontWeight.Normal
                    )
                },
                selected = selectedItem == index,
                onClick = { onItemSelected(index) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = WingZoneRed,
                    selectedTextColor = WingZoneRed,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = WingZoneOrange.copy(alpha = 0.2f)
                )
            )
        }
    }
}

@Composable
fun ExitConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Exit App",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = WingZoneRed
            )
        },
        text = {
            Text(
                text = "Do you want to exit?",
                fontSize = 16.sp,
                color = TextPrimary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = WingZoneRed
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    "Yes",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = WingZoneRed
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    "No",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}
