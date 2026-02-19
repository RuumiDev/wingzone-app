package wingzone.zenith.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.CachePolicy
import wingzone.zenith.data.model.HomeBanner
import wingzone.zenith.data.repository.FirebaseBannerRepository
import wingzone.zenith.ui.components.SvgIcon
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
    onNavigateToMyReviews: () -> Unit = {},
    onNavigateToPayment: (String) -> Unit = {}, // Navigate to payment webview
    initialTab: Int = 0
) {
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var showExitDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = context as? Activity
    val pagerState = rememberPagerState(pageCount = { 5 })
    val coroutineScope = rememberCoroutineScope()
    
    // Update selected tab when initialTab changes (for navigation back to specific tab)
    LaunchedEffect(initialTab) {
        if (selectedTab != initialTab) {
            selectedTab = initialTab
        }
    }
    
    // Sync pager with selected tab from bottom navigation
    LaunchedEffect(selectedTab) {
        if (pagerState.currentPage != selectedTab) {
            pagerState.animateScrollToPage(selectedTab)
        }
    }
    
    // Sync selected tab with pager swipe
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress && selectedTab != pagerState.currentPage) {
            selectedTab = pagerState.currentPage
        }
    }
    
    // Persistent ViewModels - survive tab switches
    val menuViewModel = remember { wingzone.zenith.viewmodel.MenuViewModel() }
    
    // Persistent scroll state for menu - survives tab switches and recompositions
    val menuScrollState = rememberLazyListState()
    
    // Persistent banner state - loads once and survives tab navigation
    var banners by remember { mutableStateOf<List<HomeBanner>>(emptyList()) }
    var areBannersLoaded by remember { mutableStateOf(false) }
    val bannerRepository = remember { FirebaseBannerRepository() }
    
    // Reviews state
    var reviews by remember { mutableStateOf<List<wingzone.zenith.data.models.Review>>(emptyList()) }
    var areReviewsLoaded by remember { mutableStateOf(false) }
    val reviewRepository = remember { wingzone.zenith.data.repository.FirebaseReviewRepository() }
    
    // Load banners once on first composition
    LaunchedEffect(Unit) {
        if (!areBannersLoaded) {
            try {
                android.util.Log.d("HomeScreen", "Fetching banners...")
                banners = bannerRepository.getActiveBanners()
                android.util.Log.d("HomeScreen", "Received ${banners.size} banners")
                areBannersLoaded = true
                
                // Preload all banner images into cache for instant display
                banners.forEach { banner ->
                    if (banner.imageUrl.isNotEmpty()) {
                        coil.ImageLoader(context).enqueue(
                            ImageRequest.Builder(context)
                                .data(banner.imageUrl)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .build()
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeScreen", "Error loading banners: ${e.message}", e)
                areBannersLoaded = true
            }
        }
        
        // Load reviews
        if (!areReviewsLoaded) {
            try {
                android.util.Log.d("HomeScreen", "Fetching reviews...")
                reviews = reviewRepository.getRecentReviews(limit = 10)
                android.util.Log.d("HomeScreen", "Received ${reviews.size} reviews")
                areReviewsLoaded = true
            } catch (e: Exception) {
                android.util.Log.e("HomeScreen", "Error loading reviews: ${e.message}", e)
                areReviewsLoaded = true
            }
        }
    }
    
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
            if (currentOrder != null && selectedTab != 2 && selectedTab != 3) {
                // Only clear if not going to Cart tab (2) or Group Order tab (3)
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
            AnimatedVisibility(
                visible = pagerState.currentPage == 0,
                enter = slideInVertically(initialOffsetY = { -it }),
                exit = slideOutVertically(targetOffsetY = { -it })
            ) {
                HomeTopBar(authViewModel = authViewModel)
            }
        },
        bottomBar = { AnimatedBottomNavigationBar(
            pagerState = pagerState,
            selectedItem = selectedTab
        ) { selectedTab = it } },
        containerColor = BackgroundGray
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = true
        ) { page ->
            when (page) {
            0 -> {
                // Home Tab
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Promo Carousel
                    PromoCarousel(
                        banners = banners,
                        isLoading = !areBannersLoaded,
                        onBannerClick = { selectedTab = 1 }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Group Order Feature Banner
                    GroupOrderBanner(
                        onNavigateToCreateLobby = onNavigateToCreateLobby,
                        onNavigateToJoinLobby = onNavigateToJoinLobby,
                        onOrderNow = { selectedTab = 3 }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // What People Say Section
                    if (reviews.isNotEmpty()) {
                        WhatPeopleSaySection(reviews = reviews)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            1 -> {
                // Menu Tab - 2x2 Grid with Seamless Scrolling
                MenuScreenGrid(
                    menuViewModel = menuViewModel,
                    cartViewModel = cartViewModel,
                    authViewModel = authViewModel,
                    groupOrderViewModel = groupOrderViewModel,
                    lobbyViewModel = lobbyViewModel,
                    onAuthRequired = onAuthRequired,
                    onNavigateToCart = { selectedTab = 2 }
                )
            }
            2 -> {
                // Cart Tab
                val currentLobbyIdFromViewModel by lobbyViewModel.currentLobbyId.collectAsState()
                CartScreen(
                    cartViewModel = cartViewModel,
                    authViewModel = authViewModel,
                    groupOrderViewModel = groupOrderViewModel,
                    lobbyViewModel = lobbyViewModel,
                    currentLobbyId = currentLobbyIdFromViewModel,
                    onAuthRequired = onAuthRequired,
                    onCheckoutClick = { 
                        // Navigate to menu tab when user wants to add more items
                        selectedTab = 1
                    },
                    onNavigateToPayment = onNavigateToPayment,
                    onNavigateToOrderDetails = onNavigateToOrderDetails
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
                    onNavigateToOrderHistory = onNavigateToOrderHistory,
                    onNavigateToMyReviews = onNavigateToMyReviews
                )
            }
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
fun PromoCarousel(
    banners: List<HomeBanner>,
    isLoading: Boolean,
    onBannerClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    
    val pagerState = rememberPagerState(pageCount = { banners.size.coerceAtLeast(1) })
    
    // Auto-scroll effect
    LaunchedEffect(isLoading, banners.size) {
        if (!isLoading && banners.isNotEmpty()) {
            while (true) {
                delay(5000) // 5 seconds delay
                val nextPage = (pagerState.currentPage + 1) % banners.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Carousel container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            if (isLoading) {
                // Clean loading placeholder with subtle gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    WingZoneRed.copy(alpha = 0.08f),
                                    WingZoneOrange.copy(alpha = 0.08f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = WingZoneRed.copy(alpha = 0.5f),
                        strokeWidth = 3.dp
                    )
                }
            } else if (banners.isEmpty()) {
                // No banners available
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(WingZoneRed.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No promotions available",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            } else {
                // Carousel
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    PromoBanner(banner = banners[page], onBannerClick = onBannerClick)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Page Indicators
        if (banners.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(banners.size) { index ->
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
                    if (index < banners.size - 1) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PromoBanner(banner: HomeBanner, onBannerClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)  // Maintain 16:9 aspect ratio dynamically
            .clickable { onBannerClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Background Image (full opacity, no overlay)
            if (banner.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(banner.imageUrl)
                        .crossfade(300)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = banner.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
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
                SvgIcon(
                    assetPath = "icons/groups.svg",
                    contentDescription = "Group Order",
                    tint = Color.White,
                    size = 20.dp
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
fun AnimatedBottomNavigationBar(
    pagerState: androidx.compose.foundation.pager.PagerState,
    selectedItem: Int,
    onItemSelected: (Int) -> Unit
) {
    val items = listOf("Home", "Menu", "Cart", "Group", "Account")
    val icons = listOf(
        Icons.Default.Home,
        Icons.Default.Menu,
        null, // Cart icon will be SVG
        null, // Group icon will be SVG
        Icons.Default.AccountCircle
    )
    
    // Calculate indicator position with animation
    val indicatorOffset by animateFloatAsState(
        targetValue = pagerState.currentPage + pagerState.currentPageOffsetFraction,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "indicator_offset"
    )
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        BoxWithConstraints {
            val containerWidth = maxWidth
            val itemWidth = containerWidth / items.size
            
            Column {
                // Animated indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(itemWidth)
                            .height(3.dp)
                            .offset(x = itemWidth * indicatorOffset)
                            .background(WingZoneRed)
                    )
                }
                
                // Navigation items
                Row(
                    modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    val isSelected = selectedItem == index
                    val alpha by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0.6f,
                        label = "alpha_$index"
                    )
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0.85f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "scale_$index"
                    )
                    
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onItemSelected(index) }
                            .padding(vertical = 8.dp)
                            .graphicsLayer {
                                this.alpha = alpha
                                scaleX = scale
                                scaleY = scale
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (index == 2) { // Cart tab
                            SvgIcon(
                                assetPath = "icons/cart.svg",
                                contentDescription = item,
                                tint = if (isSelected) WingZoneRed else Color.Gray,
                                size = 24.dp
                            )
                        } else if (index == 3) { // Group tab
                            SvgIcon(
                                assetPath = "icons/groups.svg",
                                contentDescription = item,
                                tint = if (isSelected) WingZoneRed else Color.Gray,
                                size = 24.dp
                            )
                        } else {
                            Icon(
                                imageVector = icons[index]!!,
                                contentDescription = item,
                                tint = if (isSelected) WingZoneRed else Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item,
                            fontSize = 11.sp,
                            color = if (isSelected) WingZoneRed else Color.Gray,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
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

/**
 * What People Say Section - Displays recent customer reviews
 */
@Composable
fun WhatPeopleSaySection(reviews: List<wingzone.zenith.data.models.Review>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Section Title
        Text(
            text = "What People Say",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // Horizontal scrolling list of reviews
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            reviews.forEach { review ->
                ReviewCard(review = review)
            }
        }
    }
}

/**
 * Review Card - Individual review item with premium styling
 */
@Composable
fun ReviewCard(review: wingzone.zenith.data.models.Review) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .height(160.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = WingZoneOrange.copy(alpha = 0.3f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Background watermark quote
            Text(
                text = "\u201C",
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = WingZoneOrange.copy(alpha = 0.15f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 8.dp, y = (-8).dp)
            )
            
            // Content column
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Review text
                if (review.comment.isNotBlank()) {
                    Text(
                        text = review.comment,
                        fontSize = 15.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = TextPrimary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 22.sp
                    )
                }
                
                // Push footer to bottom
                Spacer(modifier = Modifier.weight(1f))
                
                // Footer: User name (bold)
                Text(
                    text = review.userName.ifBlank { "Anonymous" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Star rating in gold
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(5) { index ->
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Rounded.Star,
                            contentDescription = null,
                            tint = if (index < review.rating) Color(0xFFFFD700) else Color(0xFFE0E0E0),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
