package wingzone.zenith.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import wingzone.zenith.data.models.MenuItem
import wingzone.zenith.ui.theme.*
import wingzone.zenith.viewmodel.AuthViewModel
import wingzone.zenith.viewmodel.CartViewModel
import wingzone.zenith.viewmodel.MenuViewModel
import wingzone.zenith.viewmodel.MenuState
import wingzone.zenith.viewmodel.MenuCategory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    menuViewModel: MenuViewModel = MenuViewModel(),
    cartViewModel: CartViewModel = CartViewModel(),
    authViewModel: AuthViewModel = AuthViewModel(),
    onAuthRequired: () -> Unit = {}
) {
    val menuState by menuViewModel.menuState.collectAsState()
    var selectedCategoryIndex by remember { mutableStateOf(0) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var showCustomizationDialog by remember { mutableStateOf(false) }
    var selectedMenuItem by remember { mutableStateOf<MenuItem?>(null) }
    val cart by cartViewModel.cart.collectAsState()
    val isAuthenticated = authViewModel.isAuthenticated()
    
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
                    // Refresh button
                    IconButton(onClick = { menuViewModel.refreshMenu() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh menu",
                            tint = WingZoneRed
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        containerColor = BackgroundGray
    ) { paddingValues ->
        when (val state = menuState) {
            is MenuState.Loading -> {
                // Loading state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = WingZoneRed)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Loading menu...",
                            color = getAdaptiveTextSecondary(),
                            fontSize = 14.sp
                        )
                    }
                }
            }
            is MenuState.Error -> {
                // Error state with fallback to hardcoded menu
                val fallbackCategories = remember { getMenuCategories() }
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Error banner
                    Surface(
                        color = WingZoneOrange.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = WingZoneOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Using offline menu. ${state.message}",
                                fontSize = 12.sp,
                                color = getAdaptiveTextSecondary()
                            )
                        }
                    }
                    
                    // Show fallback menu
                    Row(
                        modifier = Modifier.fillMaxSize()
                    ) {
                    CategorySidebar(
                        categories = fallbackCategories,
                        selectedIndex = selectedCategoryIndex,
                        onCategoryClick = { index ->
                            selectedCategoryIndex = index
                            coroutineScope.launch {
                                var itemIndex = 0
                                for (i in 0 until index) {
                                    itemIndex += fallbackCategories[i].items.size + 1
                                }
                                // Scroll with offset to account for category header
                                listState.animateScrollToItem(
                                    index = itemIndex,
                                    scrollOffset = -20  // Adjust offset to show header properly
                                )
                            }
                        }
                    )
                    
                    MenuContent(
                        categories = fallbackCategories,
                        listState = listState,
                        onItemClick = { menuItem ->
                            if (!isAuthenticated) {
                                onAuthRequired()
                                return@MenuContent
                            }
                            selectedMenuItem = menuItem
                            if (menuItem.requiresCustomization) {
                                showCustomizationDialog = true
                            } else {
                                cartViewModel.addItem(menuItem, 1)
                            }
                        }
                    )
                    }
                }
            }
            is MenuState.Success -> {
                val categories = state.categories
                
                // Track which category is visible based on scroll position
                LaunchedEffect(listState.firstVisibleItemIndex) {
                    val index = listState.firstVisibleItemIndex
                    var itemCount = 0
                    for (i in categories.indices) {
                        itemCount += categories[i].items.size + 1
                        if (index < itemCount) {
                            selectedCategoryIndex = i
                            break
                        }
                    }
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Left Category Navigation
                        CategorySidebar(
                            categories = categories,
                            selectedIndex = selectedCategoryIndex,
                            onCategoryClick = { index ->
                                selectedCategoryIndex = index
                                coroutineScope.launch {
                                    var itemIndex = 0
                                    for (i in 0 until index) {
                                        itemIndex += categories[i].items.size + 1
                                    }
                                    // Smooth scroll with offset to account for category header
                                    listState.animateScrollToItem(
                                        index = itemIndex,
                                        scrollOffset = -20
                                    )
                                }
                            }
                        )                    // Right Menu Content
                    MenuContent(
                        categories = categories,
                        listState = listState,
                        onItemClick = { menuItem ->
                            if (!isAuthenticated) {
                                onAuthRequired()
                                return@MenuContent
                            }
                            selectedMenuItem = menuItem
                            if (menuItem.requiresCustomization) {
                                showCustomizationDialog = true
                            } else {
                                cartViewModel.addItem(menuItem, 1)
                            }
                        }
                    )
                }
            }
        }
        
        // Customization Dialog
        if (showCustomizationDialog && selectedMenuItem != null) {
            EntreeCustomizationDialog(
                menuItem = selectedMenuItem!!,
                onDismiss = { showCustomizationDialog = false },
                onConfirm = { quantity, customization ->
                    cartViewModel.addItem(selectedMenuItem!!, quantity, customization)
                    showCustomizationDialog = false
                }
            )
        }
        
        // Cart Floating Action Button
        if (cart.totalItems > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                FloatingActionButton(
                    onClick = { /* Navigate to cart */ },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    containerColor = WingZoneRed
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Cart",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${cart.totalItems}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategorySidebar(
    categories: List<MenuCategory>,
    selectedIndex: Int,
    onCategoryClick: (Int) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(80.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(categories.size) { index ->
                CategoryItem(
                    category = categories[index],
                    isSelected = selectedIndex == index,
                    onClick = { onCategoryClick(index) }
                )
            }
        }
    }
}


@Composable
fun CategoryItem(
    category: MenuCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isSelected) WingZoneOrange.copy(alpha = 0.1f)
                else Color.Transparent
            )
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = category.icon,
            contentDescription = category.name,
            tint = if (isSelected) WingZoneRed else Color.Gray,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = category.name,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) WingZoneRed else TextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 12.sp
        )
        
        if (isSelected) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(3.dp)
                    .background(
                        color = WingZoneRed,
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

@Composable
fun MenuContent(
    categories: List<MenuCategory>,
    listState: LazyListState,
    onItemClick: (MenuItem) -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        flingBehavior = androidx.compose.foundation.gestures.ScrollableDefaults.flingBehavior()
    ) {
        categories.forEach { category ->
            // Category Header
            item(key = "header_${category.id}") {
                CategoryHeader(category.name)
            }
            
            // Menu Items
            items(category.items.size) { index ->
                MenuItemCard(
                    item = category.items[index],
                    onClick = { onItemClick(category.items[index]) }
                )
                if (index < category.items.size - 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
            
            // Spacer between categories
            item(key = "spacer_${category.id}") {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun CategoryHeader(categoryName: String) {
    Text(
        text = categoryName,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary,
        modifier = Modifier.padding(vertical = 12.dp)
    )
}

@Composable
fun MenuItemCard(
    item: MenuItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Product Image
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
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
                    // Placeholder icon
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = item.name,
                        tint = Color.Gray.copy(alpha = 0.3f),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Product Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = item.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (item.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.description,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 14.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "RM ${String.format("%.2f", item.price)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = WingZoneRed
                )
            }
            
            // Add Button
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.Bottom
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = WingZoneRed,
                    onClick = onClick
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add to cart",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(20.dp)
                    )
                }
            }
        }
    }
}

// Sample data
fun getMenuCategories(): List<MenuCategory> {
    return listOf(
        MenuCategory(
            id = "combos",
            name = "Combo Meals",
            icon = Icons.Default.Star,
            items = listOf(
                MenuItem(
                    name = "Entree 1",
                    description = "6 pcs Boneless Wings + Fries + Fresh Veg + Dipping Sauce + Drink",
                    price = 25.90,
                    category = "Combo Meals",
                    requiresCustomization = true
                ),
                MenuItem(
                    name = "Entree 2",
                    description = "7 pcs Original Wings + Fries + Fresh Veg + Dipping Sauce + Drink",
                    price = 29.90,
                    category = "Combo Meals",
                    requiresCustomization = true
                ),
                MenuItem(
                    name = "Entree 3",
                    description = "3 pcs Chicken Tenders + Fries + Fresh Veg + Drink",
                    price = 25.90,
                    category = "Combo Meals",
                    requiresCustomization = true
                ),
                MenuItem(
                    name = "Entree 4",
                    description = "Premium Beef Cheeseburger + Fries + Drink",
                    price = 27.90,
                    category = "Combo Meals",
                    requiresCustomization = false
                ),
                MenuItem(
                    name = "Entree 5",
                    description = "Supreme Grilled Chicken Sandwich + Fries + Drink",
                    price = 27.90,
                    category = "Combo Meals",
                    requiresCustomization = false
                ),
                MenuItem(
                    name = "Entree 6",
                    description = "Supreme Chicken Tender Sandwich + Fries + Drink",
                    price = 27.90,
                    category = "Combo Meals",
                    requiresCustomization = false
                ),
                MenuItem(
                    name = "Entree 7",
                    description = "2 pcs Chicken Tenders + Smiley Fries + Drink (Kid's Meal)",
                    price = 14.90,
                    category = "Combo Meals",
                    requiresCustomization = true
                ),
                MenuItem(
                    name = "Entree 8",
                    description = "2 pcs Drumsticks + Fries + Fresh Veg + Dipping Sauce + Drink",
                    price = 24.90,
                    category = "Combo Meals",
                    requiresCustomization = true
                ),
                MenuItem(
                    name = "Entree 9",
                    description = "Garden OR Caesar Salad + Fries + Fresh Veg + Drink",
                    price = 22.90,
                    category = "Combo Meals",
                    requiresCustomization = false
                ),
                MenuItem(
                    name = "Entree 10",
                    description = "Supreme Grilled Chicken Tortilla + Kettle Chips + Drink",
                    price = 27.90,
                    category = "Combo Meals",
                    requiresCustomization = false
                ),
                MenuItem(
                    name = "Entree 11",
                    description = "Supreme Chicken Tender Tortilla + Kettle Chips + Drink",
                    price = 27.90,
                    category = "Combo Meals",
                    requiresCustomization = false
                ),
                MenuItem(
                    name = "Entree 12",
                    description = "Premium Beef Tortilla + Kettle Chips + Drink",
                    price = 27.90,
                    category = "Combo Meals",
                    requiresCustomization = false
                )
            )
        ),
        MenuCategory(
            id = "wings",
            name = "Wings",
            icon = Icons.Default.ShoppingCart,
            items = listOf(
                MenuItem(
                    name = "Wings - 5 pcs",
                    description = "Original or Boneless • 1 flavor",
                    price = 18.90,
                    category = "Wings",
                    requiresCustomization = true
                ),
                MenuItem(
                    name = "Wings - 7 pcs",
                    description = "Original or Boneless • 1 flavor",
                    price = 22.90,
                    category = "Wings",
                    requiresCustomization = true
                ),
                MenuItem(
                    name = "Wings - 10 pcs",
                    description = "Original or Boneless • 1 flavor",
                    price = 28.90,
                    category = "Wings",
                    requiresCustomization = true
                ),
                MenuItem(
                    name = "Wings - 15 pcs",
                    description = "Original or Boneless • 2 flavors",
                    price = 40.90,
                    category = "Wings",
                    requiresCustomization = true
                ),
                MenuItem(
                    name = "Wings - 20 pcs",
                    description = "Original or Boneless • 2 flavors",
                    price = 52.90,
                    category = "Wings",
                    requiresCustomization = true
                ),
                MenuItem(
                    name = "Wings - 30 pcs",
                    description = "Original or Boneless • 2 flavors",
                    price = 72.90,
                    category = "Wings",
                    requiresCustomization = true
                ),
                MenuItem(
                    name = "Wings - 50 pcs",
                    description = "Original or Boneless • 3 flavors",
                    price = 113.90,
                    category = "Wings",
                    requiresCustomization = true
                ),
                MenuItem(
                    name = "Wings - 100 pcs",
                    description = "Original or Boneless • 4 flavors",
                    price = 199.90,
                    category = "Wings",
                    requiresCustomization = true
                ),
                MenuItem(
                    name = "Wings + Fries - 7 pcs",
                    description = "Original or Boneless + Premium Wedge Fries • 1 flavor",
                    price = 24.50,
                    category = "Wings",
                    requiresCustomization = true
                ),
                MenuItem(
                    name = "Wings + Fries - 10 pcs",
                    description = "Original or Boneless + Premium Wedge Fries • 1 flavor",
                    price = 30.50,
                    category = "Wings",
                    requiresCustomization = true
                )
            )
        ),
        MenuCategory(
            id = "tenders",
            name = "Tenders",
            icon = Icons.Default.ShoppingCart,
            items = listOf(
                MenuItem(
                    name = "Tenders - 3 pcs",
                    description = "A La Carte • 1 flavor",
                    price = 16.90,
                    category = "Tenders",
                    requiresCustomization = true
                ),
                MenuItem(
                    name = "Tenders + Fries - 3 pcs",
                    description = "Served with Premium Wedge Fries • 1 flavor",
                    price = 21.90,
                    category = "Tenders",
                    requiresCustomization = true
                ),
                MenuItem(
                    name = "Tenders - 5 pcs",
                    description = "A La Carte • 1 flavor",
                    price = 23.90,
                    category = "Tenders",
                    requiresCustomization = true
                ),
                MenuItem(
                    name = "Chicken Tenders - 5 pcs w/ Fries",
                    description = "Served with Premium Wedge Fries • 1 flavor",
                    price = 27.90,
                    category = "Chicken Tenders",
                    requiresCustomization = true
                ),
                MenuItem(
                    name = "Chicken Tenders - 10 pcs",
                    description = "A La Carte • 2 flavors",
                    price = 40.90,
                    category = "Chicken Tenders",
                    requiresCustomization = true
                )
            )
        ),
        MenuCategory(
            id = "burgers",
            name = "Burgers & Sandwiches",
            icon = Icons.Default.ShoppingCart,
            items = listOf(
                MenuItem(
                    name = "Double Cheeseburger + Fries",
                    description = "Double Stack Premium Beef with Fries",
                    price = 29.90,
                    category = "Burgers & Sandwiches"
                ),
                MenuItem(
                    name = "Cheeseburger",
                    description = "Premium Beef Cheeseburger",
                    price = 23.90,
                    category = "Burgers & Sandwiches"
                ),
                MenuItem(
                    name = "Beef Tortilla Wrap",
                    description = "Premium Beef in Tortilla",
                    price = 23.90,
                    category = "Burgers & Sandwiches"
                ),
                MenuItem(
                    name = "Grilled Chicken Sandwich",
                    description = "Supreme Grilled Chicken",
                    price = 23.90,
                    category = "Burgers & Sandwiches"
                ),
                MenuItem(
                    name = "Grilled Chicken Tortilla Wrap",
                    description = "Supreme Grilled Chicken in Tortilla",
                    price = 23.90,
                    category = "Burgers & Sandwiches"
                ),
                MenuItem(
                    name = "Chicken Tender Sandwich",
                    description = "Supreme Chicken Tenders",
                    price = 23.90,
                    category = "Burgers & Sandwiches"
                ),
                MenuItem(
                    name = "Chicken Tender Tortilla Wrap",
                    description = "Supreme Chicken Tenders in Tortilla",
                    price = 23.90,
                    category = "Burgers & Sandwiches"
                )
            )
        ),
        MenuCategory(
            id = "local",
            name = "Local Favorites",
            icon = Icons.Default.Star,
            items = listOf(
                MenuItem(
                    name = "Flavorholic Boneless",
                    description = "With Aromatic Rice & Grilled Veg",
                    price = 14.90,
                    category = "Local Favorites",
                    requiresCustomization = true
                ),
                MenuItem(
                    name = "Flavorholic Drums",
                    description = "With Aromatic Rice & Grilled Veg",
                    price = 20.90,
                    category = "Local Favorites",
                    requiresCustomization = true
                ),
                MenuItem(
                    name = "Flavorholic Nasi Ayam",
                    description = "With Grilled Veg",
                    price = 20.90,
                    category = "Local Favorites",
                    requiresCustomization = true
                ),
                MenuItem(
                    name = "2 pcs Drumsticks",
                    description = "A La Carte",
                    price = 16.90,
                    category = "Local Favorites",
                    requiresCustomization = true
                ),
                MenuItem(
                    name = "3 pcs Drumsticks",
                    description = "A La Carte",
                    price = 21.90,
                    category = "Local Favorites",
                    requiresCustomization = true
                ),
                MenuItem(
                    name = "5 pcs Drumsticks",
                    description = "A La Carte",
                    price = 32.90,
                    category = "Local Favorites",
                    requiresCustomization = true
                )
            )
        ),
        MenuCategory(
            id = "salads",
            name = "Fresh Salads",
            icon = Icons.Default.ShoppingCart,
            items = listOf(
                MenuItem(
                    name = "Garden Salad",
                    description = "Fresh garden vegetables",
                    price = 18.90,
                    category = "Fresh Salads"
                ),
                MenuItem(
                    name = "Caesar Salad",
                    description = "Classic Caesar with dressing",
                    price = 18.90,
                    category = "Fresh Salads"
                ),
                MenuItem(
                    name = "Garden Salad with Grilled Chicken",
                    description = "Fresh vegetables with grilled chicken",
                    price = 25.90,
                    category = "Fresh Salads"
                ),
                MenuItem(
                    name = "Caesar Salad with Grilled Chicken",
                    description = "Classic Caesar with grilled chicken",
                    price = 25.90,
                    category = "Fresh Salads"
                ),
                MenuItem(
                    name = "Garden Salad with Chicken Tender",
                    description = "Fresh vegetables with chicken tenders",
                    price = 25.90,
                    category = "Fresh Salads"
                ),
                MenuItem(
                    name = "Caesar Salad with Chicken Tender",
                    description = "Classic Caesar with chicken tenders",
                    price = 25.90,
                    category = "Fresh Salads"
                )
            )
        ),
        MenuCategory(
            id = "sides",
            name = "On The Side",
            icon = Icons.Default.Add,
            items = listOf(
                MenuItem(
                    name = "Wedge Fries - Regular",
                    description = "Premium wedge fries",
                    price = 10.90,
                    category = "On The Side"
                ),
                MenuItem(
                    name = "Wedge Fries - Jumbo",
                    description = "Premium wedge fries",
                    price = 16.90,
                    category = "On The Side"
                ),
                MenuItem(
                    name = "Sweet Potato Fries - Regular",
                    description = "Crispy sweet potato fries",
                    price = 15.90,
                    category = "On The Side"
                ),
                MenuItem(
                    name = "Sweet Potato Fries - Jumbo",
                    description = "Crispy sweet potato fries",
                    price = 24.90,
                    category = "On The Side"
                ),
                MenuItem(
                    name = "Kettle Chips - Regular",
                    description = "Crunchy kettle chips",
                    price = 10.90,
                    category = "On The Side"
                ),
                MenuItem(
                    name = "Kettle Chips - Jumbo",
                    description = "Crunchy kettle chips",
                    price = 16.90,
                    category = "On The Side"
                ),
                MenuItem(
                    name = "Flavor Rub Fries - Regular",
                    description = "Fries with flavor rub seasoning",
                    price = 13.90,
                    category = "On The Side"
                ),
                MenuItem(
                    name = "Flavor Rub Fries - Jumbo",
                    description = "Fries with flavor rub seasoning",
                    price = 19.90,
                    category = "On The Side"
                ),
                MenuItem(
                    name = "Mozzarella Stix",
                    description = "Fried mozzarella cheese sticks",
                    price = 20.90,
                    category = "On The Side"
                ),
                MenuItem(
                    name = "Smiley Fries",
                    description = "Fun smiley-shaped fries",
                    price = 9.90,
                    category = "On The Side"
                ),
                MenuItem(
                    name = "Aromatic Rice",
                    description = "Fragrant aromatic rice",
                    price = 4.90,
                    category = "On The Side"
                ),
                MenuItem(
                    name = "Aromatic Rice w/ Grilled Veg",
                    description = "Aromatic rice with grilled vegetables",
                    price = 6.90,
                    category = "On The Side"
                ),
                MenuItem(
                    name = "Celeries",
                    description = "Fresh celery sticks",
                    price = 4.90,
                    category = "On The Side"
                ),
                MenuItem(
                    name = "Ranch Sauce",
                    description = "Creamy ranch dipping sauce",
                    price = 4.90,
                    category = "On The Side"
                ),
                MenuItem(
                    name = "Bleu Cheese Sauce",
                    description = "Tangy bleu cheese dipping sauce",
                    price = 4.90,
                    category = "On The Side"
                ),
                MenuItem(
                    name = "Extra Flavors",
                    description = "Additional flavor sauce",
                    price = 4.90,
                    category = "On The Side"
                )
            )
        ),
        MenuCategory(
            id = "beverages",
            name = "Beverages",
            icon = Icons.Default.ShoppingCart,
            items = listOf(
                MenuItem(
                    name = "Coca-Cola",
                    description = "330ml can",
                    price = 3.90,
                    category = "Beverages"
                ),
                MenuItem(
                    name = "Sprite",
                    description = "330ml can",
                    price = 3.90,
                    category = "Beverages"
                ),
                MenuItem(
                    name = "Fanta Orange",
                    description = "330ml can",
                    price = 3.90,
                    category = "Beverages"
                ),
                MenuItem(
                    name = "Iced Lemon Tea",
                    description = "Refreshing lemon tea",
                    price = 4.90,
                    category = "Beverages"
                ),
                MenuItem(
                    name = "Mineral Water",
                    description = "500ml bottled water",
                    price = 2.50,
                    category = "Beverages"
                ),
                MenuItem(
                    name = "Fresh Orange Juice",
                    description = "Freshly squeezed",
                    price = 8.90,
                    category = "Beverages"
                )
            )
        )
    )
}
