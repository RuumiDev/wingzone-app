package wingzone.zenith.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalContext
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import wingzone.zenith.ui.theme.*
import wingzone.zenith.viewmodel.AuthViewModel
import java.util.concurrent.TimeUnit

/**
 * FIREBASE PHONE AUTHENTICATION SETUP REQUIRED:
 * 
 * 1. Enable Phone Authentication in Firebase Console:
 *    - Go to Firebase Console → Authentication → Sign-in method
 *    - Enable "Phone" provider
 * 
 * 2. Add SHA-256 fingerprint to Firebase:
 *    - Run: .\gradlew signingReport
 *    - Copy SHA-256 from debug variant
 *    - Add to Firebase Console → Project Settings → Your apps → Add fingerprint
 * 
 * 3. Download updated google-services.json
 * 
 * 4. For testing, you can add test phone numbers in Firebase Console:
 *    - Authentication → Sign-in method → Phone → Add test phone number
 *    - Example: +60123456789 with code 123456
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneVerificationScreen(
    authViewModel: AuthViewModel,
    onVerified: () -> Unit,
    onSkipForNow: () -> Unit = {}
) {
    var phoneNumber by remember { mutableStateOf("") }
    var showOtpDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }

    val activity = LocalActivity.current
    val currentUser by authViewModel.currentUser.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verify Your Account", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = BackgroundGray
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Icon
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        color = WingZoneRed.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(60.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    tint = WingZoneRed,
                    modifier = Modifier.size(60.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Title
            Text(
                text = "It looks like your account hasn't been verified",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Description
            Text(
                text = "By verifying your account, you will get full access of the app. You will also receive all upcoming notifications via SMS.",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Phone Number Input
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { 
                    if (it.length <= 15) phoneNumber = it.filter { char -> char.isDigit() || char == '+' }
                },
                label = { Text("Phone Number") },
                placeholder = { Text("+60123456789") },
                leadingIcon = {
                    Icon(Icons.Default.Phone, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WingZoneRed,
                    focusedLabelColor = WingZoneRed,
                    cursorColor = WingZoneRed,
                    focusedLeadingIconColor = WingZoneRed
                ),
                shape = RoundedCornerShape(12.dp)
            )

            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Send Verification Button
            Button(
                onClick = {
                    if (phoneNumber.length < 10) {
                        errorMessage = "Please enter a valid phone number"
                    } else {
                        isLoading = true
                        errorMessage = ""
                        
                        // Send SMS verification code using Firebase Phone Auth
                        val auth = FirebaseAuth.getInstance()
                        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                                // Auto-verification successful
                                isLoading = false
                                coroutineScope.launch {
                                    try {
                                        auth.currentUser?.updatePhoneNumber(credential)?.await()
                                        FirebaseFirestore.getInstance()
                                            .collection("users")
                                            .document(auth.currentUser?.uid ?: "")
                                            .update(mapOf(
                                                "phoneNumber" to phoneNumber,
                                                "isPhoneVerified" to true
                                            ))
                                            .await()
                                        onVerified()
                                    } catch (e: Exception) {
                                        errorMessage = "Verification failed: ${e.message}"
                                    }
                                }
                            }

                            override fun onVerificationFailed(e: FirebaseException) {
                                isLoading = false
                                errorMessage = "Failed to send code: ${e.message}"
                            }

                            override fun onCodeSent(
                                verificationIdReceived: String,
                                token: PhoneAuthProvider.ForceResendingToken
                            ) {
                                isLoading = false
                                verificationId = verificationIdReceived
                                showOtpDialog = true
                            }
                        }

                        if (activity != null) {
                            val options = PhoneAuthOptions.newBuilder(auth)
                                .setPhoneNumber(phoneNumber)
                                .setTimeout(60L, TimeUnit.SECONDS)
                                .setActivity(activity)
                                .setCallbacks(callbacks)
                                .build()
                            
                            PhoneAuthProvider.verifyPhoneNumber(options)
                        } else {
                            isLoading = false
                            errorMessage = "Unable to send verification code. Please try again."
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading && phoneNumber.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WingZoneRed,
                    disabledContainerColor = Color.Gray
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "SEND VERIFICATION CODE",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Skip for now
            TextButton(onClick = onSkipForNow) {
                Text(
                    text = "Skip for now",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Info text
            Text(
                text = "We'll send you a 6-digit verification code via SMS",
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }

    // OTP Dialog
    if (showOtpDialog && verificationId != null) {
        OtpVerificationDialog(
            phoneNumber = phoneNumber,
            verificationId = verificationId!!,
            onDismiss = { showOtpDialog = false },
            onVerified = {
                showOtpDialog = false
                onVerified()
            }
        )
    }
}

@Composable
fun OtpVerificationDialog(
    phoneNumber: String,
    verificationId: String,
    onDismiss: () -> Unit,
    onVerified: () -> Unit
) {
    var otpCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }
    var timeLeft by remember { mutableStateOf(60) }
    
    val coroutineScope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()

    // Countdown timer
    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = WingZoneRed,
                    modifier = Modifier.size(64.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Enter Verification Code",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "We've sent a 6-digit code to",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = phoneNumber,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // OTP Input
                OutlinedTextField(
                    value = otpCode,
                    onValueChange = { 
                        if (it.length <= 6) otpCode = it.filter { char -> char.isDigit() }
                    },
                    label = { Text("6-Digit Code") },
                    placeholder = { Text("000000") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WingZoneRed,
                        focusedLabelColor = WingZoneRed,
                        cursorColor = WingZoneRed
                    )
                )

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = Color.Red,
                        fontSize = 12.sp
                    )
                }

                if (successMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = successMessage,
                        color = Color(0xFF388E3C),
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Timer and Resend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (timeLeft > 0) {
                        Text(
                            text = "Code expires in ${timeLeft}s",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    } else {
                        Text(
                            text = "Code expired",
                            fontSize = 12.sp,
                            color = Color.Red
                        )
                    }

                    TextButton(
                        onClick = { 
                            timeLeft = 60
                            errorMessage = ""
                            successMessage = "Code resent!"
                        },
                        enabled = timeLeft == 0
                    ) {
                        Text(
                            text = "Resend Code",
                            fontSize = 12.sp,
                            color = if (timeLeft == 0) WingZoneRed else Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Verify Button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            if (otpCode.length != 6) {
                                errorMessage = "Please enter a valid 6-digit code"
                                return@launch
                            }

                            isLoading = true
                            errorMessage = ""

                            try {
                                // Verify the OTP code with Firebase
                                val credential = PhoneAuthProvider.getCredential(verificationId, otpCode)
                                
                                // Link the phone credential to the current user
                                auth.currentUser?.updatePhoneNumber(credential)?.await()

                                // Update user verification status in Firestore
                                val user = auth.currentUser
                                if (user != null) {
                                    FirebaseFirestore.getInstance()
                                        .collection("users")
                                        .document(user.uid)
                                        .update(
                                            mapOf(
                                                "phoneNumber" to phoneNumber,
                                                "isPhoneVerified" to true
                                            )
                                        )
                                        .await()

                                    successMessage = "Phone verified successfully!"
                                    delay(1000)
                                    onVerified()
                                } else {
                                    errorMessage = "User not found"
                                }
                            } catch (e: Exception) {
                                errorMessage = when {
                                    e.message?.contains("invalid-verification-code") == true -> 
                                        "Invalid code. Please try again."
                                    e.message?.contains("session-expired") == true -> 
                                        "Code expired. Please request a new one."
                                    else -> "Verification failed: ${e.message}"
                                }
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isLoading && otpCode.length == 6,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WingZoneRed
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = "Verify",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Cancel",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
