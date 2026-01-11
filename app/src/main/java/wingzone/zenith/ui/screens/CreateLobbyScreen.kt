package wingzone.zenith.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
                title = { Text("Create Group Order") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
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
                            text = "📍",
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Location",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "*",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
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
                            text = "💰",
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Payment Method",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "*",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Payment Method Chips
                    PaymentMethodChip(
                        label = "Host Pays All",
                        description = "You'll pay for everyone",
                        selected = paymentMethod == "host-pays-all",
                        onClick = { paymentMethod = "host-pays-all" }
                    )
                    
                    PaymentMethodChip(
                        label = "Split Equally",
                        description = "Total divided by members",
                        selected = paymentMethod == "split-equally",
                        onClick = { paymentMethod = "split-equally" }
                    )
                    
                    PaymentMethodChip(
                        label = "Individual Payment",
                        description = "Each pays their own order",
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
                    .height(56.dp),
                enabled = !isCreating && orderType != null && selectedLocation != null && paymentMethod != null
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
                style = MaterialTheme.typography.bodyLarge
            )
        },
        modifier = modifier.height(48.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
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
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
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
