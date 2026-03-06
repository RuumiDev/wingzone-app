package wingzone.zenith.ui.screens

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import wingzone.zenith.ui.theme.WingZoneRed
import android.view.ViewGroup
import android.webkit.WebSettings
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentWebViewScreen(
    paymentUrl: String,
    onPaymentSuccess: () -> Unit,
    onPaymentFailed: () -> Unit,
    onBack: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var showCancelDialog by remember { mutableStateOf(false) }

    // Confirmation dialog when user tries to leave mid-payment
    if (showCancelDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Payment?") },
            text = { Text("Going back will cancel this payment session. You can start a new payment from your cart.") },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showCancelDialog = false
                        onBack()
                    }
                ) { Text("Yes, Go Back") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showCancelDialog = false }
                ) { Text("Continue Payment") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Complete Payment", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { showCancelDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WingZoneRed,
                    titleContentColor = androidx.compose.ui.graphics.Color.White,
                    navigationIconContentColor = androidx.compose.ui.graphics.Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            cacheMode = WebSettings.LOAD_NO_CACHE
                        }
                        
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                            }
                            
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val url = request?.url?.toString() ?: return false
                                
                                // Handle payment callback URLs
                                when {
                                    // Success callback - check for success page or status
                                    url.contains("payment-success") ||
                                    url.contains("payment/success") || 
                                    url.contains("status_id=1") ||
                                    url.contains("payment_status=success") -> {
                                        onPaymentSuccess()
                                        return true
                                    }
                                    // Failed callback
                                    url.contains("payment/failed") || 
                                    url.contains("status_id=2") ||
                                    url.contains("status_id=3") ||
                                    url.contains("payment_status=failed") -> {
                                        onPaymentFailed()
                                        return true
                                    }
                                    // Cancelled
                                    url.contains("payment/cancelled") -> {
                                        onBack()
                                        return true
                                    }
                                }
                                
                                return false
                            }
                        }
                        
                        loadUrl(paymentUrl)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // Loading indicator
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = WingZoneRed
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading payment gateway...")
                    }
                }
            }
        }
    }
}
