// CartScreen.kt - Checkout Button Logic Update
// Location: Around line 965-1040 in confirmButton section

// FIND THIS CODE BLOCK (the onClick handler):
Button(
    onClick = {
        isProcessingOrder = true
        coroutineScope.launch {
            if (isInLobby && currentLobbyId != null) {
                // Mark as paid in lobby
                lobbyViewModel?.markAsPaid(currentLobbyId, currentUser?.id ?: "") { result ->
                    // ... existing lobby payment logic
                }
            } else {
                // Individual order (not in lobby)
                val result = orderRepository.createOrder(
                    userId = currentUser?.id ?: "",
                    userName = currentUser?.name ?: "Guest",
                    cart = cart,
                    paymentMethod = selectedPaymentMethod,
                    // ... rest of parameters
                )
                // ... handle result
            }
        }
    },
    // ... other button parameters
) {
    // ... button content
}

// REPLACE WITH THIS:
Button(
    onClick = {
        isProcessingOrder = true
        coroutineScope.launch {
            if (isInLobby && currentLobbyId != null) {
                // Lobby payment - keep existing logic
                lobbyViewModel?.markAsPaid(currentLobbyId, currentUser?.id ?: "") { result ->
                    result.fold(
                        onSuccess = {
                            orderSuccess = true
                            isProcessingOrder = false
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(1500)
                                showCheckoutDialog = false
                                orderSuccess = false
                                onPaymentComplete()
                            }
                        },
                        onFailure = { error ->
                            orderError = error.message
                            isProcessingOrder = false
                        }
                    )
                }
            } else {
                // Individual order - SPLIT LOGIC BY PAYMENT METHOD
                if (selectedPaymentMethod == "cash") {
                    // ====== CASH PAYMENT ======
                    // Create order immediately with pending payment status
                    val result = orderRepository.createOrder(
                        userId = currentUser?.id ?: "",
                        userName = currentUser?.name ?: "Guest",
                        cart = cart,
                        paymentMethod = selectedPaymentMethod,
                        phoneNumber = currentUser?.email,
                        orderType = selectedOrderType,
                        location = selectedBranch,
                        lobbyPaymentMethod = null,
                        paymentType = selectedPaymentMethod
                    )
                    
                    result.onSuccess { orderId ->
                        orderSuccess = true
                        placedOrderId = orderId
                        kotlinx.coroutines.delay(500)
                        cartViewModel.clearCart()
                        showCheckoutDialog = false
                        orderSuccess = false
                        isProcessingOrder = false
                        onOrderPlaced(orderId)
                    }.onFailure { error ->
                        orderError = error.message
                        isProcessingOrder = false
                    }
                } else {
                    // ====== ONLINE BANKING (FPX) ======
                    // Store pending order and redirect to payment gateway
                    try {
                        val pendingOrderId = wingzone.zenith.utils.PendingOrderManager.storePendingOrder(
                            context = context,
                            userId = currentUser?.id ?: "",
                            userName = currentUser?.name ?: "Guest",
                            cart = cart,
                            paymentMethod = selectedPaymentMethod,
                            paymentType = selectedPaymentMethod,
                            phoneNumber = currentUser?.email,
                            orderType = selectedOrderType,
                            location = selectedBranch,
                            lobbyId = null
                        )
                        
                        // Close dialog and navigate to payment webview
                        showCheckoutDialog = false
                        isProcessingOrder = false
                        onNavigateToPayment(pendingOrderId)
                        
                    } catch (e: Exception) {
                        orderError = "Failed to initiate payment: ${e.message}"
                        isProcessingOrder = false
                    }
                }
            }
        }
    },
    modifier = Modifier
        .fillMaxWidth()
        .height(50.dp),
    shape = RoundedCornerShape(12.dp),
    colors = ButtonDefaults.buttonColors(containerColor = WingZoneRed)
) {
    Icon(
        imageVector = Icons.Default.Check,
        contentDescription = null,
        modifier = Modifier.size(20.dp)
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
        // UPDATED BUTTON TEXT
        text = if (selectedPaymentMethod == "cash") {
            if (isInLobby) "Confirm Payment" else "Place Order"
        } else {
            "Proceed to Payment Gateway"
        },
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold
    )
}
