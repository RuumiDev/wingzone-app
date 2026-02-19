package wingzone.zenith.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import wingzone.zenith.ui.theme.*
import wingzone.zenith.viewmodel.LobbyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinLobbyScreen(
    viewModel: LobbyViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToScanner: () -> Unit,
    onLobbyJoined: (String) -> Unit,
    prefilledCode: String? = null
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    
    // Handle back button
    BackHandler {
        onNavigateBack()
    }
    
    // State for 6-digit code
    val codeLength = 6
    var code1 by remember { mutableStateOf(TextFieldValue("")) }
    var code2 by remember { mutableStateOf(TextFieldValue("")) }
    var code3 by remember { mutableStateOf(TextFieldValue("")) }
    var code4 by remember { mutableStateOf(TextFieldValue("")) }
    var code5 by remember { mutableStateOf(TextFieldValue("")) }
    var code6 by remember { mutableStateOf(TextFieldValue("")) }
    
    var isJoining by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Focus requesters
    val focusRequesters = remember {
        List(codeLength) { FocusRequester() }
    }
    
    // Pre-fill code if provided
    LaunchedEffect(prefilledCode) {
        prefilledCode?.let { code ->
            if (code.length == codeLength) {
                code1 = TextFieldValue(code[0].toString().uppercase())
                code2 = TextFieldValue(code[1].toString().uppercase())
                code3 = TextFieldValue(code[2].toString().uppercase())
                code4 = TextFieldValue(code[3].toString().uppercase())
                code5 = TextFieldValue(code[4].toString().uppercase())
                code6 = TextFieldValue(code[5].toString().uppercase())
            }
        }
    }
    
    val currentCode = buildString {
        append(code1.text)
        append(code2.text)
        append(code3.text)
        append(code4.text)
        append(code5.text)
        append(code6.text)
    }.uppercase()
    
    // Auto-join when all 6 characters entered
    LaunchedEffect(currentCode) {
        if (currentCode.length == codeLength && !isJoining) {
            isJoining = true
            errorMessage = null
            viewModel.joinLobby(currentCode) { result ->
                isJoining = false
                result.fold(
                    onSuccess = { lobbyId ->
                        Toast.makeText(
                            context,
                            "✓ You have joined the lobby!",
                            Toast.LENGTH_SHORT
                        ).show()
                        // Delay navigation to ensure toast is visible
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            onLobbyJoined(lobbyId)
                        }, 800)
                    },
                    onFailure = { error ->
                        errorMessage = error.message ?: "Failed to join lobby"
                    }
                )
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Join Group Order") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WingZoneOrange,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            // Title
            Text(
                text = "Enter 6-digit code:",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            // Code Input Boxes
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                        CodeInputBox(
                            value = code1,
                            onValueChange = { newValue ->
                                if (newValue.text.length <= 1) {
                                    code1 = TextFieldValue(
                                        newValue.text.uppercase(),
                                        TextRange(newValue.text.length)
                                    )
                                    if (newValue.text.length == 1) {
                                        focusRequesters[1].requestFocus()
                                    }
                                }
                            },
                            focusRequester = focusRequesters[0],
                            onBackspace = { }
                        )
                        
                        CodeInputBox(
                            value = code2,
                            onValueChange = { newValue ->
                                if (newValue.text.length <= 1) {
                                    code2 = TextFieldValue(
                                        newValue.text.uppercase(),
                                        TextRange(newValue.text.length)
                                    )
                                    if (newValue.text.length == 1) {
                                        focusRequesters[2].requestFocus()
                                    }
                                }
                            },
                            focusRequester = focusRequesters[1],
                            onBackspace = {
                                if (code2.text.isEmpty()) {
                                    focusRequesters[0].requestFocus()
                                }
                            }
                        )
                        
                        CodeInputBox(
                            value = code3,
                            onValueChange = { newValue ->
                                if (newValue.text.length <= 1) {
                                    code3 = TextFieldValue(
                                        newValue.text.uppercase(),
                                        TextRange(newValue.text.length)
                                    )
                                    if (newValue.text.length == 1) {
                                        focusRequesters[3].requestFocus()
                                    }
                                }
                            },
                            focusRequester = focusRequesters[2],
                            onBackspace = {
                                if (code3.text.isEmpty()) {
                                    focusRequesters[1].requestFocus()
                                }
                            }
                        )
                        
                        CodeInputBox(
                            value = code4,
                            onValueChange = { newValue ->
                                if (newValue.text.length <= 1) {
                                    code4 = TextFieldValue(
                                        newValue.text.uppercase(),
                                        TextRange(newValue.text.length)
                                    )
                                    if (newValue.text.length == 1) {
                                        focusRequesters[4].requestFocus()
                                    }
                                }
                            },
                            focusRequester = focusRequesters[3],
                            onBackspace = {
                                if (code4.text.isEmpty()) {
                                    focusRequesters[2].requestFocus()
                                }
                            }
                        )
                        
                        CodeInputBox(
                            value = code5,
                            onValueChange = { newValue ->
                                if (newValue.text.length <= 1) {
                                    code5 = TextFieldValue(
                                        newValue.text.uppercase(),
                                        TextRange(newValue.text.length)
                                    )
                                    if (newValue.text.length == 1) {
                                        focusRequesters[5].requestFocus()
                                    }
                                }
                            },
                            focusRequester = focusRequesters[4],
                            onBackspace = {
                                if (code5.text.isEmpty()) {
                                    focusRequesters[3].requestFocus()
                                }
                            }
                        )
                        
                        CodeInputBox(
                            value = code6,
                            onValueChange = { newValue ->
                                if (newValue.text.length <= 1) {
                                    code6 = TextFieldValue(
                                        newValue.text.uppercase(),
                                        TextRange(newValue.text.length)
                                    )
                                    if (newValue.text.length == 1) {
                                        focusManager.clearFocus()
                                    }
                                }
                            },
                            focusRequester = focusRequesters[5],
                            onBackspace = {
                                if (code6.text.isEmpty()) {
                                    focusRequesters[4].requestFocus()
                                }
                            }
                        )
                        }
                    }
                    
                    // Loading or error state
                    if (isJoining) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "Joining lobby...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    errorMessage?.let { error ->
                        Text(
                            text = error,
                            color = Color(0xFF820000),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Button(
                            onClick = {
                                // Clear code
                                code1 = TextFieldValue("")
                                code2 = TextFieldValue("")
                                code3 = TextFieldValue("")
                                code4 = TextFieldValue("")
                                code5 = TextFieldValue("")
                                code6 = TextFieldValue("")
                                errorMessage = null
                                focusRequesters[0].requestFocus()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE0E0E0),
                                contentColor = WingZoneOrange
                            )
                        ) {
                            Text("Try Again", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
            
            // Divider with "or"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Divider(modifier = Modifier.weight(1f))
                Text(
                    text = "or",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Divider(modifier = Modifier.weight(1f))
            }
            
            // Scan QR Code Button
            OutlinedButton(
                onClick = onNavigateToScanner,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = WingZoneOrange
                ),
                border = androidx.compose.foundation.BorderStroke(2.dp, WingZoneOrange)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Scan QR Code",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Tip
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF4E6)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "💡",
                        fontSize = 24.sp
                    )
                    Text(
                        text = "Tip: Ask your friend to share the lobby link directly!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WingZoneOrange,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun CodeInputBox(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    focusRequester: FocusRequester,
    onBackspace: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .width(48.dp)
            .height(62.dp)
            .focusRequester(focusRequester),
        textStyle = LocalTextStyle.current.copy(
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            keyboardType = KeyboardType.Ascii,
            imeAction = ImeAction.Next
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = WingZoneOrange,
            unfocusedBorderColor = Color.Gray,
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black
        )
    )
}
