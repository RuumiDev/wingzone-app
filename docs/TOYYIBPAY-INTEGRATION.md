# ToyyibPay Payment Gateway Integration

## Overview

This document explains how to set up and configure ToyyibPay payment gateway integration for the WingZone app.

## Architecture

The payment flow follows this sequence:

1. **User selects "Online Banking" payment** → CartScreen creates ToyyibPay bill via Firebase Cloud Function
2. **Cloud Function creates bill** → Returns payment URL to app
3. **App stores pending order** → Navigates to PaymentWebViewScreen with payment URL
4. **User completes payment** → ToyyibPay redirects to `wz://payment/success?order_id=xxx`
5. **App receives callback** → Creates actual order in Firestore, clears cart
6. **ToyyibPay webhook** → Confirms payment status to backend (optional verification)

## Firebase Cloud Functions Setup

### 1. Install Dependencies
```bash
cd functions
npm install
```

### 2. Set environment variables

**✅ CONFIGURED** - Credentials are now integrated directly in the Cloud Function:

- **Secret Key:** `9bullxmy-9v7d-fqfc-hupo-zvzer7wo3i4x`
- **Category Code:** `r90repsm`
- **Functions URL:** `https://us-central1-wingzone-app.cloudfunctions.net`

> **Note:** For production, consider using Firebase environment config or Secret Manager for better security:

```bash
firebase functions:config:set \
  toyyibpay.secret_key="9bullxmy-9v7d-fqfc-hupo-zvzer7wo3i4x" \
  toyyibpay.category_code="r90repsm" \
  firebase.functions_url="https://us-central1-wingzone-app.cloudfunctions.net"
```

Or using `.env` file for local development:

```env
TOYYIBPAY_SECRET_KEY=9bullxmy-9v7d-fqfc-hupo-zvzer7wo3i4x
TOYYIBPAY_CATEGORY_CODE=r90repsm
FIREBASE_FUNCTIONS_URL=https://us-central1-wingzone-app.cloudfunctions.net
```

### 3. Deploy Cloud Functions

```bash
firebase deploy --only functions:createToyyibPayBill,functions:paymentCallback
```

### 4. Get your Cloud Functions URL

After deployment, note your Cloud Functions base URL:
```
https://us-central1-wingzone-app.cloudfunctions.net
```

## Android App Configuration

### 1. Update PaymentRepository.kt

**✅ CONFIGURED** - The BASE_URL is now set to your Firebase project:

```kotlin
companion object {
    private const val TAG = "PaymentRepository"
    private const val BASE_URL = "https://us-central1-wingzone-app.cloudfunctions.net"
}
```

### 2. Verify AndroidManifest.xml

Ensure the deep link intent filter for payment callbacks is present (already added):

```xml
<intent-filter android:autoVerify="true">
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="wz" android:host="payment" />
</intent-filter>
```

## ToyyibPay Account Setup

### 1. Register for ToyyibPay Account

1. Go to [dev.toyyibpay.com](https://dev.toyyibpay.com) for testing
2. Create an account
3. Verify your email

### 2. Get API Credentials

1. Login to ToyyibPay dashboard
2. Go to **Settings** → **API Key**
3. Copy your **Secret Key**
4. Go to **Category** → Create a category for "Food Orders"
5. Note the **Category Code**

### 3. Configure Webhook URL

In ToyyibPay dashboard:
1. Go to **Settings** → **Callback URL**
2. Set callback URL to: `https://us-central1-wingzone-app.cloudfunctions.net/paymentCallback`

## Testing

### Development Mode (Test Payments)

ToyyibPay provides test card numbers for development:

**Test Card Numbers:**
- Success: Use card `5436 0310 3060 6378` with any CVV and future expiry
- Failed: Use other random card numbers

**API Endpoint:**
- Development: `https://dev.toyyibpay.com`
- Production: `https://toyyibpay.com`

### Test Flow

1. Add items to cart
2. Select "Online Banking" payment method
3. Click "Proceed to Payment Gateway"
4. Wait for payment URL to load (shows loading indicator)
5. Enter test card details in ToyyibPay page
6. Complete payment
7. App should redirect to order tracking screen
8. Check Firestore to verify order was created with `paymentStatus: 'paid'`

### Verify Webhook

Check Firebase Functions logs to see if webhook callback was received:

```bash
firebase functions:log --only paymentCallback
```

## Production Checklist

Before going live:

- [ ] **Switch to production API**: Change `dev.toyyibpay.com` to `toyyibpay.com` in Cloud Function
- [ ] **Update environment variables** with production credentials
- [ ] **Configure production webhook URL** in ToyyibPay dashboard
- [ ] **Test with real payment amount** (minimum RM 1.00)
- [ ] **Enable SSL pinning** in Android app for security
- [ ] **Set up error monitoring** (Firebase Crashlytics)
- [ ] **Test refund flow** if applicable
- [ ] **Review ToyyibPay fees** and adjust pricing accordingly

## Troubleshooting

### Payment URL not loading

**Check:**
1. Firebase Functions deployed successfully
2. Environment variables set correctly
3. BASE_URL in PaymentRepository matches your project
4. Check Firebase Functions logs: `firebase functions:log`

### Payment success but order not created

**Check:**
1. Deep link handler in MainActivity.kt
2. Cart state is preserved during payment
3. Firebase Functions logs for errors
4. Firestore security rules allow order creation

### Webhook not receiving callbacks

**Check:**
1. Webhook URL configured correctly in ToyyibPay dashboard
2. Firebase Functions `paymentCallback` deployed
3. Check Functions logs for incoming requests
4. Verify ToyyibPay sends callbacks (may be delayed in dev mode)

### Amount mismatch

**Remember:**
- ToyyibPay requires amount in cents: `amount * 100`
- Verify cart total calculation includes tax
- Check for rounding errors in price calculations

## API Reference

### createToyyibPayBill Function

**Endpoint:** `POST /createToyyibPayBill`

**Request Body:**
```json
{
  "orderId": "pending-order-uuid",
  "customerName": "John Doe",
  "customerEmail": "john@example.com",
  "totalAmount": 25.50
}
```

**Response:**
```json
{
  "success": true,
  "billCode": "abc123xyz",
  "paymentUrl": "https://dev.toyyibpay.com/abc123xyz",
  "orderId": "pending-order-uuid"
}
```

### paymentCallback Webhook

**Endpoint:** `POST /paymentCallback`

**Payload from ToyyibPay:**
```json
{
  "refno": "1234567890",
  "status": "1",
  "reason": "Payment successful",
  "billcode": "abc123xyz",
  "order_id": "pending-order-uuid",
  "amount": "2550",
  "billpaymentStatus": "1"
}
```

**Status Codes:**
- `1` = Successful payment
- `2` = Pending payment
- `3` = Failed payment

## Security Considerations

1. **Never expose API keys** in client-side code
2. **Validate webhook signatures** (if ToyyibPay provides them)
3. **Verify payment amounts** match order totals
4. **Implement idempotency** to prevent duplicate order creation
5. **Use HTTPS** for all API calls
6. **Implement rate limiting** on Cloud Functions if needed

## Cost Optimization

**Firebase Cloud Functions:**
- Using `maxInstances: 10` to prevent runaway costs
- Functions timeout after 10 seconds for payment creation
- Consider upgrading to Blaze plan for production use

**ToyyibPay Fees:**
- Check their website for current transaction fees
- Factor fees into your pricing model

## Support

**ToyyibPay:**
- Documentation: [https://toyyibpay.com/apireference/](https://toyyibpay.com/apireference/)
- Support: support@toyyibpay.com

**Firebase:**
- Documentation: [https://firebase.google.com/docs/functions](https://firebase.google.com/docs/functions)
- Community: [https://stackoverflow.com/questions/tagged/firebase](https://stackoverflow.com/questions/tagged/firebase)
