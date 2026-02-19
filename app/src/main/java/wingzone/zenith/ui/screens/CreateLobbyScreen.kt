package wingzone.zenith.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import wingzone.zenith.ui.components.DisclaimerDialog
import wingzone.zenith.ui.components.SvgIcon
import wingzone.zenith.viewmodel.LobbyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateLobbyScreen(
    viewModel: LobbyViewModel,
    onNavigateBack: () -> Unit,
    onLobbyCreated: (String) -> Unit
) {
    val context = LocalContext.current
    
    // Handle back button
    BackHandler {
        onNavigateBack()
    }
    
    // State
    var showDisclaimer by remember { mutableStateOf(false) }
    var orderType by remember { mutableStateOf<String?>(null) }
    var selectedLocation by remember { mutableStateOf<Location?>(null) }
    var paymentMethod by remember { mutableStateOf<String?>(null) }
    var paymentType by remember { mutableStateOf<String?>(null) }
    var locationExpanded by remember { mutableStateOf(false) }
    var isCreating by remember { mutableStateOf(false) }
    
    val locations by viewModel.locations.collectAsState()
    val shouldShowDisclaimer by viewModel.shouldShowDisclaimer.collectAsState()
    
    // Check disclaimer on first launch
    LaunchedEffect(Unit) {
        viewModel.loadLocations()
        if (shouldShowDisclaimer) {
            showDisclaimer = true
        }
    }
    
    // Disclaimer Dialog
    DisclaimerDialog(
        visible = showDisclaimer,
        onAccept = {
            showDisclaimer = false
        },
        onCancel = {
            showDisclaimer = false
            onNavigateBack()
        },
        onDontShowAgainChanged = { dontShow ->
            if (dontShow) {
                viewModel.setDisclaimerAcknowledged()
            }
        }
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Create Group Lobby",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = wingzone.zenith.ui.theme.WingZoneOrange,
                    titleContentColor = androidx.compose.ui.graphics.Color.White,
                    navigationIconContentColor = androidx.compose.ui.graphics.Color.White
                )
            )
        },
        containerColor = Color(0xFFFDF4ED)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    bottom = 32.dp,
                    top = 16.dp,
                    start = 16.dp,
                    end = 16.dp
                )
            ) {
                item {
                
            // Order Type Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .graphicsLayer { clip = false }
                        .padding(
                            start = 20.dp,
                            top = 20.dp,
                            end = 20.dp,
                            bottom = 32.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    wingzone.zenith.ui.theme.WingZoneOrange.copy(alpha = 0.15f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🥡",
                                fontSize = 20.sp
                            )
                        }
                        Text(
                            text = "Order Type",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = wingzone.zenith.ui.theme.WingZoneRed
                        )
                        Text(
                            text = "*",
                            color = wingzone.zenith.ui.theme.WingZoneRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    
                    // Order Type Large Icon Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SelectableIconCard(
                            iconPath = "icons/delivery.svg",
                            label = "Pickup",
                            selected = orderType == "pickup",
                            onClick = { orderType = "pickup" },
                            modifier = Modifier.weight(1f)
                        )
                        SelectableIconCard(
                            iconPath = "icons/dinein.svg",
                            label = "Dine-In",
                            selected = orderType == "dine-in",
                            onClick = { orderType = "dine-in" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    // Spacer for inner shadow rendering
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            
            // Location Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = androidx.compose.ui.graphics.Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .graphicsLayer { clip = false }
                        .padding(
                            start = 20.dp,
                            top = 20.dp,
                            end = 20.dp,
                            bottom = 32.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    wingzone.zenith.ui.theme.WingZoneOrange.copy(alpha = 0.15f),
                                    androidx.compose.foundation.shape.CircleShape
                                ),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Text(
                                text = "📍",
                                fontSize = 20.sp
                            )
                        }
                        Text(
                            text = "Location",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = wingzone.zenith.ui.theme.WingZoneRed
                        )
                        Text(
                            text = "*",
                            color = wingzone.zenith.ui.theme.WingZoneRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    
                    // Location Selection Card
                    Card(
                        onClick = { locationExpanded = !locationExpanded },
                        modifier = Modifier
                            .padding(4.dp)
                            .fillMaxWidth()
                            .heightIn(min = 72.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = Color.LightGray
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 2.dp
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Location Icon in circle
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(
                                        wingzone.zenith.ui.theme.WingZoneOrange.copy(alpha = 0.15f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Location",
                                    modifier = Modifier.size(32.dp),
                                    tint = wingzone.zenith.ui.theme.WingZoneOrange
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            // Store name and address
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                // Caption label
                                Text(
                                    text = "Pick a Branch",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = Color(0xFF999999),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                
                                // Location name
                                Text(
                                    text = selectedLocation?.name ?: "Select location",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (selectedLocation != null) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 16.sp,
                                    color = if (selectedLocation != null) 
                                        Color(0xFF2C2C2C)
                                    else 
                                        Color(0xFF616161)
                                )
                                selectedLocation?.let { location ->
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = location.address,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                            
                            // Chevron down icon
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Expand",
                                tint = Color(0xFF757575),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    
                    // Dropdown Menu
                    DropdownMenu(
                        expanded = locationExpanded,
                        onDismissRequest = { locationExpanded = false },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .background(
                                Color.White,
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        locations.forEach { location ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = location.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = location.address,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                },
                                onClick = {
                                    selectedLocation = location
                                    locationExpanded = false
                                },
                                contentPadding = PaddingValues(
                                    horizontal = 20.dp,
                                    vertical = 12.dp
                                )
                            )
                        }
                    }
                    
                    // Spacer for inner shadow rendering
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            
            // Payment Method Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = androidx.compose.ui.graphics.Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .graphicsLayer { clip = false }
                        .padding(
                            start = 20.dp,
                            top = 20.dp,
                            end = 20.dp,
                            bottom = 32.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    wingzone.zenith.ui.theme.WingZoneOrange.copy(alpha = 0.15f),
                                    androidx.compose.foundation.shape.CircleShape
                                ),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Text(
                                text = "💰",
                                fontSize = 20.sp
                            )
                        }
                        Text(
                            text = "Payment Method",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = wingzone.zenith.ui.theme.WingZoneRed
                        )
                        Text(
                            text = "*",
                            color = wingzone.zenith.ui.theme.WingZoneRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    
                    // Payment Method Large Cards - Vertical
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PaymentOptionCard(
                            iconPath = "icons/crown.svg",
                            title = "Treat Everyone",
                            subtitle = "You pay the full bill",
                            selected = paymentMethod == "host-pays-all",
                            onClick = { paymentMethod = "host-pays-all" }
                        )
                        
                        PaymentOptionCard(
                            iconPath = "icons/groups.svg",
                            title = "Split the Bill",
                            subtitle = "Everyone pays their own share",
                            selected = paymentMethod == "individual",
                            onClick = { paymentMethod = "individual" }
                        )
                    }
                    
                    // Spacer for inner shadow rendering
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            
            // Payment Type Section (Cash vs FPX/Online)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = androidx.compose.ui.graphics.Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .graphicsLayer { clip = false }
                        .padding(
                            start = 20.dp,
                            top = 20.dp,
                            end = 20.dp,
                            bottom = 32.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    wingzone.zenith.ui.theme.WingZoneOrange.copy(alpha = 0.15f),
                                    androidx.compose.foundation.shape.CircleShape
                                ),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Text(
                                text = "💳",
                                fontSize = 20.sp
                            )
                        }
                        Text(
                            text = "Payment Type",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = wingzone.zenith.ui.theme.WingZoneRed
                        )
                        Text(
                            text = "*",
                            color = wingzone.zenith.ui.theme.WingZoneRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    
                    // Payment Type Cards
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PaymentTypeCard(
                            iconPath = "icons/payment/cash.svg",
                            title = "Cash",
                            subtitle = "Pay at counter",
                            selected = paymentType == "cash",
                            onClick = { paymentType = "cash" }
                        )
                        
                        PaymentTypeCard(
                            iconPath = "icons/payment/card.svg",
                            title = "Online Banking",
                            subtitle = "FPX / Payment Gateway",
                            selected = paymentType == "online",
                            onClick = { paymentType = "online" }
                        )
                    }
                    
                    // Spacer for inner shadow rendering
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Create Button
            Button(
                onClick = {
                    if (orderType != null && selectedLocation != null && paymentMethod != null && paymentType != null) {
                        isCreating = true
                        viewModel.createLobby(
                            orderType = orderType!!,
                            location = selectedLocation!!,
                            paymentMethod = paymentMethod!!,
                            paymentType = paymentType!!
                        ) { result ->
                            isCreating = false
                            result.fold(
                                onSuccess = { lobbyId ->
                                    onLobbyCreated(lobbyId)
                                },
                                onFailure = { error ->
                                    Toast.makeText(
                                        context,
                                        "Failed to create lobby: ${error.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .height(56.dp),
                enabled = !isCreating && orderType != null && selectedLocation != null && paymentMethod != null && paymentType != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = wingzone.zenith.ui.theme.WingZoneRed,
                    disabledContainerColor = Color(0xFFE0E0E0),
                    contentColor = Color.White,
                    disabledContentColor = Color(0xFF999999)
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 3.dp,
                    pressedElevation = 6.dp
                )
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = "Create Lobby",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
                // Bottom spacer to prevent shadow clipping
                Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
fun SelectableIconCard(
    iconPath: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .padding(4.dp)
            .then(modifier)
            .height(140.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) 
                wingzone.zenith.ui.theme.WingZoneOrange.copy(alpha = 0.1f)
            else 
                Color.White
        ),
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(
                width = 2.dp,
                color = wingzone.zenith.ui.theme.WingZoneOrange
            )
        } else {
            androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = Color.LightGray
            )
        },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (selected) 0.dp else 2.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                SvgIcon(
                    assetPath = iconPath,
                    contentDescription = label,
                    modifier = Modifier,
                    size = 32.dp,
                    tint = if (selected) 
                        wingzone.zenith.ui.theme.WingZoneOrange
                    else 
                        Color(0xFF757575)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (selected) 
                        wingzone.zenith.ui.theme.WingZoneOrange
                    else 
                        Color(0xFF2C2C2C),
                    fontSize = 16.sp
                )
            }
            
            // Checkmark in top-right corner when selected
            if (selected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = wingzone.zenith.ui.theme.WingZoneOrange,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun PaymentOptionCard(
    iconPath: String,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) 
                wingzone.zenith.ui.theme.WingZoneOrange.copy(alpha = 0.1f)
            else 
                Color.White
        ),
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(
                width = 2.dp,
                color = wingzone.zenith.ui.theme.WingZoneOrange
            )
        } else {
            androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = Color.LightGray
            )
        },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (selected) 0.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon in a circle background
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected)
                            wingzone.zenith.ui.theme.WingZoneOrange.copy(alpha = 0.15f)
                        else
                            Color(0xFFF5F5F5)
                    ),
                contentAlignment = Alignment.Center
            ) {
                SvgIcon(
                    assetPath = iconPath,
                    contentDescription = title,
                    modifier = Modifier,
                    size = 28.dp,
                    tint = if (selected) 
                        wingzone.zenith.ui.theme.WingZoneOrange
                    else 
                        Color(0xFF757575)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Title and subtitle
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (selected) 
                        wingzone.zenith.ui.theme.WingZoneOrange
                    else 
                        Color(0xFF2C2C2C)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            
            // Selection indicator
            if (selected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = wingzone.zenith.ui.theme.WingZoneOrange,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun PaymentTypeCard(
    iconPath: String,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) 
                wingzone.zenith.ui.theme.WingZoneOrange.copy(alpha = 0.1f)
            else 
                Color.White
        ),
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(
                width = 2.dp,
                color = wingzone.zenith.ui.theme.WingZoneOrange
            )
        } else {
            androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = Color.LightGray
            )
        },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (selected) 0.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected)
                            wingzone.zenith.ui.theme.WingZoneOrange.copy(alpha = 0.15f)
                        else
                            Color(0xFFF5F5F5)
                    ),
                contentAlignment = Alignment.Center
            ) {
                SvgIcon(
                    assetPath = iconPath,
                    contentDescription = title,
                    modifier = Modifier,
                    size = 28.dp,
                    tint = if (selected) 
                        wingzone.zenith.ui.theme.WingZoneOrange
                    else 
                        Color(0xFF757575)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Title and subtitle
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (selected) 
                        wingzone.zenith.ui.theme.WingZoneOrange
                    else 
                        Color(0xFF2C2C2C)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            
            // Selection indicator
            if (selected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = wingzone.zenith.ui.theme.WingZoneOrange,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

// Data class for Location
data class Location(
    val id: String,
    val name: String,
    val address: String,
    val addressLine1: String,
    val addressLine2: String,
    val city: String
)
