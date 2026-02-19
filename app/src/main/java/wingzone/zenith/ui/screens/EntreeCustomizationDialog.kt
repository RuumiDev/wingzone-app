package wingzone.zenith.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import wingzone.zenith.data.models.*
import wingzone.zenith.data.repository.AvailabilityRepository
import wingzone.zenith.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntreeCustomizationDialog(
    menuItem: MenuItem,
    onDismiss: () -> Unit,
    onConfirm: (quantity: Int, customization: EntreeCustomization) -> Unit,
    activeLobbyId: String? = null,
    activeLobbyCode: String? = null,
    userHasPaid: Boolean = false
) {
    // Availability Repository
    val availabilityRepository = remember { AvailabilityRepository() }
    val availabilitySettings by availabilityRepository.availability.collectAsState()
    
    // Clean up on dispose
    DisposableEffect(Unit) {
        onDispose {
            availabilityRepository.cleanup()
        }
    }
    
    var quantity by remember { mutableStateOf(1) }
    var selectedFlavor by remember { mutableStateOf<Flavor?>(null) }
    var selectedDippingSauce by remember { mutableStateOf<DippingSauce?>(null) }
    var selectedDrink by remember { mutableStateOf<Drink?>(null) }
    var selectedBoneType by remember { mutableStateOf<BoneType?>(null) }
    var selectedSaladType by remember { mutableStateOf<String?>(null) }
    // Initialize fries exchange with default "Fries" if available
    var selectedFriesExchange by remember { 
        mutableStateOf<FriesExchangeOption?>(
            menuItem.customizationOptions?.friesExchanges?.find { it.name.equals("Fries", ignoreCase = true) }
        )
    }
    
    // Calculate total price including fries exchange
    val totalPrice = menuItem.price + (selectedFriesExchange?.regularPrice ?: 0.0)
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            menuItem.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = WingZoneRed
                    )
                )
            },
            containerColor = BackgroundGray
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize()) {
                
                // Content - with bottom padding for button
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = 220.dp  // Increased space for the overlaid button and nav gesture area
                    ),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Item Image (if available)
                    item {
                        if (!menuItem.imageUrl.isNullOrEmpty()) {
                            var isImageLoading by remember { mutableStateOf(true) }
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFF5F5F5)),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(menuItem.imageUrl)
                                        .crossfade(300)
                                        .placeholder(android.graphics.drawable.ColorDrawable(android.graphics.Color.parseColor("#F5F5F5")))
                                        .memoryCacheKey(menuItem.id)
                                        .memoryCachePolicy(CachePolicy.ENABLED)
                                        .diskCachePolicy(CachePolicy.ENABLED)
                                        .build(),
                                    contentDescription = menuItem.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    onSuccess = { isImageLoading = false },
                                    onError = { isImageLoading = false }
                                )
                                
                                // Loading indicator
                                if (isImageLoading) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFFE0E0E0).copy(alpha = 0.6f))
                                    )
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(40.dp),
                                        color = WingZoneOrange
                                    )
                                }
                            }
                        }
                        
                        // Description below image
                        if (menuItem.description.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = menuItem.description,
                                fontSize = 14.sp,
                                color = getAdaptiveTextSecondary(),
                                lineHeight = 20.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    
                    // Bone Type Selection (for wings)
                    // DEBUG: Check if this condition is met
                    android.util.Log.d("EntreeCustomization", "Menu: ${menuItem.name}")
                    android.util.Log.d("EntreeCustomization", "RequiresBoneType: ${menuItem.customizationOptions?.requiresBoneType}")
                    android.util.Log.d("EntreeCustomization", "AvailableTypes: ${menuItem.customizationOptions?.availableBoneTypes}")
                    android.util.Log.d("EntreeCustomization", "AvailabilitySettings BoneTypes: ${availabilitySettings.boneTypes}")
                    
                    if (menuItem.customizationOptions?.requiresBoneType == true) {
                        item {
                            Column {
                                Text(
                                    text = "Choose Type",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = getAdaptiveTextPrimary()
                                )
                                Text(
                                    text = "Required",
                                    fontSize = 12.sp,
                                    color = getAdaptiveTextSecondary()
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                val availableBoneTypes = menuItem.customizationOptions.availableBoneTypes
                                val boneTypesToShow = if (!availableBoneTypes.isNullOrEmpty()) {
                                    BoneType.values().filter { boneType ->
                                        availableBoneTypes.any { it.equals(boneType.displayName, ignoreCase = true) } &&
                                        availabilitySettings.boneTypes.contains(boneType.displayName) // Check availability
                                    }
                                } else {
                                    BoneType.values().filter { boneType ->
                                        availabilitySettings.boneTypes.contains(boneType.displayName) // Check availability
                                    }
                                }
                                
                                android.util.Log.d("EntreeCustomization", "BoneTypesToShow: $boneTypesToShow")
                                
                                ChipGroup(
                                    items = boneTypesToShow.map { it.displayName },
                                    selectedItem = selectedBoneType?.displayName,
                                    onItemSelected = { displayName ->
                                        selectedBoneType = BoneType.fromDisplayName(displayName)
                                    }
                                )
                            }
                        }
                    }
                    
                    // Flavor Selection (only if requiresFlavor is true)
                    if (menuItem.customizationOptions?.requiresFlavor == true) {
                        item {
                            Column {
                                Text(
                                    text = "Choose Flavor (Sauce)",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Required • Pick 1",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                // Get available flavors from Firebase or show all if not specified
                                val availableFlavors = menuItem.customizationOptions.availableFlavors
                                val flavorsToShow = if (!availableFlavors.isNullOrEmpty()) {
                                    Flavor.values().filter { flavor ->
                                        availableFlavors.any { it.equals(flavor.displayName, ignoreCase = true) } &&
                                        availabilitySettings.flavors.contains(flavor.displayName) // Check availability
                                    }
                                } else {
                                    Flavor.values().filter { flavor ->
                                        availabilitySettings.flavors.contains(flavor.displayName) // Check availability
                                    }
                                }
                                
                                // Show chips in rows
                                ChipGroup(
                                    items = flavorsToShow.map { it.displayName },
                                    selectedItem = selectedFlavor?.displayName,
                                    onItemSelected = { name ->
                                        selectedFlavor = flavorsToShow.find { it.displayName == name }
                                    }
                                )
                            }
                        }
                    }
                    
                    // Dipping Sauce Selection (only if requiresDippingSauce is true)
                    if (menuItem.customizationOptions?.requiresDippingSauce == true) {
                        item {
                            Column {
                                Text(
                                    text = "Choose Dipping Sauce",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Required • Pick 1",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                // Get available dipping sauces from Firebase or show all if not specified
                                val availableSauces = menuItem.customizationOptions.availableDippingSauces
                                val saucesToShow = if (!availableSauces.isNullOrEmpty()) {
                                    DippingSauce.values().filter { sauce ->
                                        availableSauces.any { it.equals(sauce.displayName, ignoreCase = true) } &&
                                        availabilitySettings.dippingSauces.contains(sauce.displayName) // Check availability
                                    }
                                } else {
                                    DippingSauce.values().filter { sauce ->
                                        availabilitySettings.dippingSauces.contains(sauce.displayName) // Check availability
                                    }
                                }
                                
                                ChipGroup(
                                    items = saucesToShow.map { it.displayName },
                                    selectedItem = selectedDippingSauce?.displayName,
                                    onItemSelected = { name ->
                                        selectedDippingSauce = saucesToShow.find { it.displayName == name }
                                    }
                                )
                            }
                        }
                    }
                    
                    // Salad Selection (only if requiresSaladChoice is true)
                    if (menuItem.customizationOptions?.requiresSaladChoice == true) {
                        item {
                            Column {
                                Text(
                                    text = "Choose Salad",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Required • Pick 1",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                val saladOptions = listOf("Garden Salad", "Caesar Salad")
                                
                                ChipGroup(
                                    items = saladOptions,
                                    selectedItem = selectedSaladType,
                                    onItemSelected = { name ->
                                        selectedSaladType = name
                                    }
                                )
                            }
                        }
                    }
                    
                    // Drink Selection (only if requiresBeverage is true)
                    if (menuItem.customizationOptions?.requiresBeverage == true) {
                        item {
                            Column {
                                Text(
                                    text = "Choose Drink",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Required • Pick 1",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                // Get available beverages from Firebase or show all if not specified
                                val availableBeverages = menuItem.customizationOptions.availableBeverages
                                // DEBUG: Log beverage options
                                android.util.Log.d("EntreeCustomization", "Item: ${menuItem.name}")
                                android.util.Log.d("EntreeCustomization", "availableBeverages: $availableBeverages")
                                android.util.Log.d("EntreeCustomization", "availabilitySettings.beverages: ${availabilitySettings.beverages}")
                                
                                val drinksToShow = if (!availableBeverages.isNullOrEmpty()) {
                                    Drink.values().filter { drink ->
                                        drink != Drink.NONE && availableBeverages.any { it.equals(drink.displayName, ignoreCase = true) }
                                    }
                                } else {
                                    // Show all drinks except NONE if no specific list provided
                                    Drink.values().filter { drink -> 
                                        drink != Drink.NONE && availabilitySettings.beverages.contains(drink.displayName)
                                    }
                                }
                                
                                android.util.Log.d("EntreeCustomization", "drinksToShow: ${drinksToShow.map { it.displayName }}")
                                
                                ChipGroup(
                                    items = drinksToShow.map { it.displayName },
                                    selectedItem = selectedDrink?.displayName,
                                    onItemSelected = { name ->
                                        selectedDrink = drinksToShow.find { it.displayName == name }
                                    }
                                )
                            }
                        }
                    }
                    
                    // Fries Exchange Selection (if available) 
                    if (menuItem.customizationOptions?.allowFriesExchange == true && 
                        !menuItem.customizationOptions.friesExchanges.isNullOrEmpty()) {
                        item {
                            Column {
                                Text(
                                    text = "Fries Exchange",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = getAdaptiveTextPrimary()
                                )
                                Text(
                                    text = "Required - Default: Fries",
                                    fontSize = 12.sp,
                                    color = WingZoneRed,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                // Filter fries exchanges by availability (sides)
                                val availableFriesExchanges = menuItem.customizationOptions.friesExchanges.filter { exchange ->
                                    availabilitySettings.sides.contains(exchange.name)
                                }
                                
                                ChipGroup(
                                    items = availableFriesExchanges.map { 
                                        val priceText = if (it.regularPrice > 0) {
                                            " (+RM ${String.format("%.2f", it.regularPrice)})"
                                        } else {
                                            ""
                                        }
                                        it.name + priceText
                                    },
                                    selectedItem = selectedFriesExchange?.let {
                                        val priceText = if (it.regularPrice > 0) {
                                            " (+RM ${String.format("%.2f", it.regularPrice)})"
                                        } else {
                                            ""
                                        }
                                        it.name + priceText
                                    },
                                    onItemSelected = { name ->
                                        selectedFriesExchange = availableFriesExchanges.find { 
                                            val priceText = if (it.regularPrice > 0) {
                                                " (+RM ${String.format("%.2f", it.regularPrice)})"
                                            } else {
                                                ""
                                            }
                                            (it.name + priceText) == name
                                        }
                                    }
                                )
                            }
                        }
                    }
                    
                }
                
                // Button overlaid at bottom - with padding to stay above navigation bar
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = 33.dp)  // Fixed padding above navigation bar
                ) {
                    BottomActionBar(
                        quantity = quantity,
                        price = totalPrice,
                    onQuantityChange = { quantity = it },
                    onAddToCart = {
                        val requiresBoneType = menuItem.customizationOptions?.requiresBoneType == true
                        val requiresFlavor = menuItem.customizationOptions?.requiresFlavor == true
                        val requiresDippingSauce = menuItem.customizationOptions?.requiresDippingSauce == true
                        val requiresBeverage = menuItem.customizationOptions?.requiresBeverage == true
                        val requiresFriesExchange = menuItem.customizationOptions?.allowFriesExchange == true && 
                            !menuItem.customizationOptions?.friesExchanges.isNullOrEmpty()
                        val requiresSaladChoice = menuItem.customizationOptions?.requiresSaladChoice == true
                        
                        // DEBUG: Log validation state
                        android.util.Log.d("EntreeCustomization", "=== VALIDATION CHECK ===")
                        android.util.Log.d("EntreeCustomization", "Item: ${menuItem.name}")
                        android.util.Log.d("EntreeCustomization", "requiresFlavor: $requiresFlavor, selected: $selectedFlavor")
                        android.util.Log.d("EntreeCustomization", "requiresDippingSauce: $requiresDippingSauce, selected: $selectedDippingSauce")
                        android.util.Log.d("EntreeCustomization", "requiresBeverage: $requiresBeverage, selected: $selectedDrink")
                        android.util.Log.d("EntreeCustomization", "requiresBoneType: $requiresBoneType, selected: $selectedBoneType")
                        
                        val isValid = (!requiresFlavor || selectedFlavor != null) &&
                                (!requiresDippingSauce || selectedDippingSauce != null) &&
                                (!requiresBeverage || selectedDrink != null) &&
                                (!requiresBoneType || selectedBoneType != null) &&
                                (!requiresFriesExchange || selectedFriesExchange != null) &&
                                (!requiresSaladChoice || selectedSaladType != null)
                        
                        android.util.Log.d("EntreeCustomization", "isValid: $isValid")
                        
                        if (isValid) {
                            val customization = EntreeCustomization(
                                flavor = selectedFlavor ?: Flavor.BUFFALO_WING,
                                dippingSauce = selectedDippingSauce ?: DippingSauce.NONE,
                                drink = selectedDrink ?: Drink.NONE,
                                boneType = selectedBoneType,
                                friesExchange = selectedFriesExchange,
                                saladType = selectedSaladType
                            )
                            onConfirm(quantity, customization)
                        }
                    },
                        enabled = (menuItem.customizationOptions?.requiresFlavor != true || selectedFlavor != null) &&
                                (menuItem.customizationOptions?.requiresDippingSauce != true || selectedDippingSauce != null) &&
                                (menuItem.customizationOptions?.requiresBeverage != true || selectedDrink != null) &&
                                (menuItem.customizationOptions?.requiresBoneType != true || selectedBoneType != null) &&
                                ((menuItem.customizationOptions?.allowFriesExchange != true || menuItem.customizationOptions?.friesExchanges.isNullOrEmpty()) || selectedFriesExchange != null) &&
                                (menuItem.customizationOptions?.requiresSaladChoice != true || selectedSaladType != null),
                        activeLobbyId = activeLobbyId,
                        activeLobbyCode = activeLobbyCode,
                        userHasPaid = userHasPaid
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipGroup(
    items: List<String>,
    selectedItem: String?,
    onItemSelected: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            val isSelected = item == selectedItem
            Surface(
                onClick = { onItemSelected(item) },
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) WingZoneRed else Color.White,
                border = BorderStroke(1.dp, if (isSelected) WingZoneRed else LightGray),
                modifier = Modifier.height(40.dp)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) Color.White else getAdaptiveTextSecondary()
                    )
                }
            }
        }
    }
}

@Composable
fun BottomActionBar(
    quantity: Int,
    price: Double,
    onQuantityChange: (Int) -> Unit,
    onAddToCart: () -> Unit,
    enabled: Boolean,
    activeLobbyId: String? = null,
    activeLobbyCode: String? = null,
    userHasPaid: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(bottom = 24.dp)
        ) {
            // Quantity controls row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (quantity > 1) onQuantityChange(quantity - 1) },
                    enabled = quantity > 1,
                    modifier = Modifier
                        .size(48.dp)
                        .border(2.dp, if (quantity > 1) WingZoneRed else LightGray, CircleShape)
                ) {
                    Text(
                        text = "−",
                        fontSize = 24.sp,
                        color = if (quantity > 1) WingZoneRed else LightGray,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Text(
                    text = quantity.toString(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                
                IconButton(
                    onClick = { onQuantityChange(quantity + 1) },
                    modifier = Modifier
                        .size(48.dp)
                        .background(WingZoneRed, CircleShape)
                ) {
                    Text(
                        text = "+",
                        fontSize = 24.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Add to Cart/Lobby button - FULL WIDTH
            Button(
                onClick = onAddToCart,
                enabled = enabled && !userHasPaid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeLobbyId != null) WingZoneOrange else WingZoneRed,
                    disabledContainerColor = LightGray
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (activeLobbyId != null) "Add to Lobby ${activeLobbyCode ?: ""}" else "Add to Cart",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "RM ${String.format("%.2f", price * quantity)}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
