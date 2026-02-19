# Payment Gateway Integration Implementation Guide

## Overview
This guide explains how to split the checkout logic based on payment method (Cash vs Online Banking/FPX) to ensure orders are only created after successful payment for online banking.

## Files Created

### 1. PaymentWebViewScreen.kt
**Location:** `app/src/main/java/wingzone/zenith/ui/screens/PaymentWebViewScreen.kt`
- WebView screen for ToyyibPay payment gateway
- Handles payment callbacks (success/failed/cancelled)
- Shows loading indicator during payment process

### 2. PendingOrderManager.kt  
**Location:** `app/src/main/java/wingzone/zenith/utils/PendingOrderManager.kt`
- Stores pending order data in SharedPreferences
- Retrieves pending orders by ID
- Generates payment gateway URLs
- Cleans up old pending orders (24hr expiry)

## Files Modified

### 3. MainActivity.kt
**Changes Made:**
1. Added `Screen.PaymentWebView` sealed class
2. Added `PendingOrder` data class
3. Extended `DeepLinkHandler` with payment callbacks:
   - `paymentSuccessFlow`
   - `paymentFailedFlow`
4. Updated `handleDeepLink()` to process payment callbacks:
   - `wz://payment/success?order_id=xxx`
   - `wz://payment/failed?order_id=xxx`

### 4. CartScreen.kt
**Changes Required:**
Add `onNavigateToPayment: (String) -> Unit` parameter to function signature

Update checkout button onClick logic around line 965-1040:
```kotlin
if (selectedPaymentMethod == "cash") {
    // CASH: Create order immediately
    val result = orderRepository.createOrder(...)
    // Handle success/failure as before
} else {
    // ONLINE BANKING: Store pending order
    val pendingOrderId = PendingOrderManager.storePendingOrder(
        context = context,
        userId = currentUser?.id ?: "",
        userName = currentUser?.name ?: "Guest",
        cart = cart,
        paymentMethod = selectedPaymentMethod,
        paymentType = selectedPaymentMethod,
        phoneNumber = currentUser?.email,
        orderType = selectedOrderType,
        location = selectedBranch
    )
    
    // Navigate to payment gateway
    showCheckoutDialog = false
    onNavigateToPayment(pendingOrderId)
}
```

Update button text:
```kotlin
text = if (selectedPaymentMethod == "cash") {
    "Place Order"
} else {
    "Proceed to Payment Gateway"
}
```

## Navigation Integration

### In MainActivity.kt AppNavigation composable:

Add PaymentWebView screen handling:
```kotlin
is Screen.PaymentWebView -> {
    val pendingOrderId = (currentScreen as Screen.PaymentWebView).pendingOrderId
    val paymentUrl = PendingOrderManager.getPaymentUrl(
        context = context,
        pendingOrderId = pendingOrderId,
        amount = /* calculate from pending order */
    )
    
    PaymentWebViewScreen(
        paymentUrl = paymentUrl,
        onPaymentSuccess = {
            // Process pending order
            processPendingOrder(context, pendingOrderId, orderRepository)
            currentScreen = Screen.Home
        },
        onPaymentFailed = {
            // Show error and return to cart
            PendingOrderManager.deletePendingOrder(context, pendingOrderId)
            currentScreen = Screen.Home
            // Show toast: "Payment failed"
        },
        onBack = {
            currentScreen = Screen.Home
        }
    )
}
```

Add payment callback listeners:
```kotlin
LaunchedEffect(Unit) {
    launch {
        DeepLinkHandler.paymentSuccessFlow.collect { pendingOrderId ->
            processPendingOrder(context, pendingOrderId, orderRepository)
            currentScreen = Screen.Home
        }
    }
    
    launch {
        DeepLinkHandler.paymentFailedFlow.collect { pendingOrderId ->
            PendingOrderManager.deletePendingOrder(context, pendingOrderId)
            currentScreen = Screen.Home
        }
    }
}
```

Create helper function:
```kotlin
suspend fun processPendingOrder(
    context: Context,
    pendingOrderId: String,
    orderRepository: FirebaseOrderRepository
) {
    val orderData = PendingOrderManager.getPendingOrder(context, pendingOrderId) ?: return
    
    // Deserialize cart
    val cartJson = orderData["cart"] as String
    val cart = PendingOrderManager.deserializeCart(cartJson)
    
    // Create actual order with paid status
    val result = orderRepository.createOrder(
        userId = orderData["userId"] as String,
        userName = orderData["userName"] as String,
        cart = cart,
        paymentMethod = orderData["paymentMethod"] as String,
        phoneNumber = orderData["phoneNumber"] as? String,
        orderType = orderData["orderType"] as? String,
        location = orderData["location"] as? String,
        paymentType = orderData["paymentType"] as String
    )
    
    result.onSuccess {
        PendingOrderManager.deletePendingOrder(context, pendingOrderId)
        // Show success toast
    }.onFailure { error ->
        // Show error toast
        android.util.Log.e("Payment", "Failed to create order: ${error.message}")
    }
}
```

## AndroidManifest.xml Updates

Add deep link intent filter for payment callbacks:
```xml
<activity android:name=".MainActivity">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        
        <!-- Existing lobby deep link -->
        <data android:scheme="wz"
              android:host="join" />
        
        <!-- New payment callbacks -->
        <data android:scheme="wz"
              android:host="payment"
              android:pathPrefix="/success" />
        <data android:scheme="wz"
              android:host="payment"
              android:pathPrefix="/failed" />
    </intent-filter>
</activity>
```

## ToyyibPay Integration

### Update PendingOrderManager.getPaymentUrl():
Replace placeholder with actual ToyyibPay API call:
```kotlin
fun getPaymentUrl(context: Context, pendingOrderId: String, amount: Double): String {
    // Call your backend API to create ToyyibPay bill
    // Backend should:
    // 1. Create bill via ToyyibPay API
    // 2. Set callback URLs to wz://payment/success and wz://payment/failed
    // 3. Return payment URL
    
    val response = yourBackend.createToyyibPayBill(
        amount = amount,
        reference = pendingOrderId,
        successUrl = "wz://payment/success?order_id=$pendingOrderId",
        failedUrl = "wz://payment/failed?order_id=$pendingOrderId"
    )
    
    return response.paymentUrl
}
```

## Testing Checklist

### Cash Payment Flow:
- [ ] Select cash payment method
- [ ] Click "Place Order"
- [ ] Order created immediately with payment status "pending"
- [ ] Cart clears
- [ ] Navigate to order tracking

### Online Banking Flow:
- [ ] Select online banking/FPX method  
- [ ] Click "Proceed to Payment Gateway"
- [ ] Pending order stored (check SharedPreferences)
- [ ] WebView opens with payment URL
- [ ] Complete payment successfully
- [ ] Deep link received: wz://payment/success?order_id=xxx
- [ ] Order created with payment status "paid"
- [ ] Pending order deleted
- [ ] Cart clears
- [ ] Navigate to order tracking

### Payment Failure:
- [ ] Start payment flow
- [ ] Cancel or fail payment
- [ ] Deep link received: wz://payment/failed?order_id=xxx
- [ ] Pending order deleted
- [ ] No order created in Firestore
- [ ] User returned to cart with error message

## Security Considerations

1. **Validate Payment on Backend:** Never trust client-side payment confirmation alone
2. **Use ToyyibPay Signature:** Verify payment callback signatures
3. **Timeout Pending Orders:** PendingOrderManager auto-cleans orders after 24hrs
4. **Idempotency:** Check if order already exists before creating from pending

## Group Orders

For group orders (lobby payment), similar logic applies:
- Cash: Mark as paid immediately in lobby
- Online: Store pending payment, redirect to gateway
- Success: Mark as paid in lobby after callback
- Host submits order only after all members paid (or host-pays-all)

