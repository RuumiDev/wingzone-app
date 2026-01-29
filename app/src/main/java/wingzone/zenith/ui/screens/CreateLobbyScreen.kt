package wingzone.zenith.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import wingzone.zenith.ui.components.DisclaimerDialog
import wingzone.zenith.viewmodel.LobbyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateLobbyScreen(
    viewModel: LobbyViewModel,
    onNavigateBack: () -> Unit,
    onLobbyCreated: (String) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    // State
    var showDisclaimer by remember { mutableStateOf(false) }
    var orderType by remember { mutableStateOf<String?>(null) }
    var selectedLocation by remember { mutableStateOf<Location?>(null) }
    var paymentMethod by remember { mutableStateOf<String?>(null) }
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
        containerColor = wingzone.zenith.ui.theme.BackgroundGray
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Order Type Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "🥡",
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Order Type",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "*",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Order Type Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OrderTypeChip(
                            label = "Pickup",
                            selected = orderType == "pickup",
                            onClick = { orderType = "pickup" },
                            modifier = Modifier.weight(1f)
                        )
                        OrderTypeChip(
                            label = "Dine-In",
                            selected = orderType == "dine-in",
                            onClick = { orderType = "dine-in" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            // Location Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = androidx.compose.ui.graphics.Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
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
                    
                    // Location Dropdown
                    ExposedDropdownMenuBox(
                        expanded = locationExpanded,
                        onExpandedChange = { locationExpanded = !locationExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedLocation?.name ?: "Select location",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            trailingIcon = {
                                Icon(Icons.Default.ArrowDropDown, "Dropdown")
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                        
                        ExposedDropdownMenu(
                            expanded = locationExpanded,
                            onDismissRequest = { locationExpanded = false }
                        ) {
                            locations.forEach { location ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = location.name,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = location.address,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedLocation = location
                                        locationExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    // Show selected location address
                    selectedLocation?.let { location ->
                        Text(
                            text = location.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
            
            // Payment Method Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = androidx.compose.ui.graphics.Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
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
                    
                    // Payment Method Chips
                    PaymentMethodChip(
                        label = "Host Pays All",
                        description = "You'll pay for everyone's order",
                        selected = paymentMethod == "host-pays-all",
                        onClick = { paymentMethod = "host-pays-all" }
                    )
                    
                    PaymentMethodChip(
                        label = "Individual Payment",
                        description = "Each member pays for their own order",
                        selected = paymentMethod == "individual",
                        onClick = { paymentMethod = "individual" }
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Create Button
            Button(
                onClick = {
                    if (orderType != null && selectedLocation != null && paymentMethod != null) {
                        isCreating = true
                        viewModel.createLobby(
                            orderType = orderType!!,
                            location = selectedLocation!!,
                            paymentMethod = paymentMethod!!
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
                    .height(60.dp),
                enabled = !isCreating && orderType != null && selectedLocation != null && paymentMethod != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = wingzone.zenith.ui.theme.WingZoneOrange,
                    disabledContainerColor = androidx.compose.ui.graphics.Color.Gray
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
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
        }
    }
}

@Composable
fun OrderTypeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        },
        modifier = modifier.height(52.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = wingzone.zenith.ui.theme.WingZoneOrange,
            selectedLabelColor = androidx.compose.ui.graphics.Color.White,
            containerColor = androidx.compose.ui.graphics.Color(0xFFF5F5F5),
            labelColor = wingzone.zenith.ui.theme.TextPrimary
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = if (selected) wingzone.zenith.ui.theme.WingZoneOrange else androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.3f),
            selectedBorderColor = wingzone.zenith.ui.theme.WingZoneOrange,
            borderWidth = if (selected) 2.dp else 1.dp,
            selectedBorderWidth = 2.dp
        )
    )
}

@Composable
fun PaymentMethodChip(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = label,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 13.sp,
                    color = if (selected) {
                        androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f)
                    } else {
                        wingzone.zenith.ui.theme.TextSecondary
                    }
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = wingzone.zenith.ui.theme.WingZoneOrange,
            selectedLabelColor = androidx.compose.ui.graphics.Color.White,
            containerColor = androidx.compose.ui.graphics.Color(0xFFF5F5F5),
            labelColor = wingzone.zenith.ui.theme.TextPrimary
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = if (selected) wingzone.zenith.ui.theme.WingZoneOrange else androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.3f),
            selectedBorderColor = wingzone.zenith.ui.theme.WingZoneOrange,
            borderWidth = if (selected) 2.dp else 1.dp,
            selectedBorderWidth = 2.dp
        )
    )
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
