# Quick Start: ToyyibPay Payment Testing

## 1️⃣ Deploy Cloud Functions

Open PowerShell in the project root and run:

```powershell
.\deploy-payment-functions.ps1
```

Or manually:
```powershell
cd functions
npm install
npm run build
firebase deploy --only functions:createToyyibPayBill,functions:paymentCallback
```

---

## 2️⃣ Build Android App

From Android Studio:
1. Sync Gradle
2. Build → Make Project
3. Run on device/emulator

Or from command line:
```powershell
cd app
./gradlew assembleDebug
```

---

## 3️⃣ Test Payment Flow

### Test Scenario: Online Banking Payment

1. **Open App** → Login/Register
2. **Browse Menu** → Add items to cart
3. **Open Cart** → Total shows with tax
4. **Select Payment:**
   - Choose "Online Banking/FPX"
   - Enter phone number (if required)
   - Select order type (Dine-in/Takeaway/Delivery)
   - Select branch

5. **Click "Proceed to Payment Gateway"**
   - App creates ToyyibPay bill via Cloud Function
   - Loading indicator shows
   - WebView opens with ToyyibPay payment page

6. **Enter Test Card Details:**
   ```
   Card Number: 5436 0310 3060 6378
   CVV: 123
   Expiry: 12/28
   Name: Test User
   ```

7. **Complete Payment**
   - Click "Pay Now"
   - ToyyibPay processes payment
   - Redirects to `wz://payment/success`
   - App creates order in Firestore
   - Cart clears
   - Navigates to order tracking

8. **Verify Order:**
   - Check order appears in "Orders" tab
   - Status should be "Confirmed"
   - Payment status: "Paid"

### Test Scenario: Cash Payment (Should still work)

1. Open Cart
2. Select "Cash"
3. Click "Place Order"
4. Order created immediately (no payment gateway)

---

## 4️⃣ Configure ToyyibPay Webhook (Optional)

For production verification:

1. Go to [dev.toyyibpay.com](https://dev.toyyibpay.com)
2. Login with your account
3. Settings → Callback URL
4. Set to: `https://us-central1-wingzone-app.cloudfunctions.net/paymentCallback`
5. Save

---

## 5️⃣ Monitor Logs

Watch real-time logs during testing:

```bash
firebase functions:log --only createToyyibPayBill,paymentCallback
```

Or visit Firebase Console:
```
https://console.firebase.google.com/project/wingzone-app/functions/logs
```

---

## 🐛 Quick Troubleshooting

### "Payment URL not found"
→ Check Cloud Functions deployed: `firebase functions:list`

### "Failed to create payment"
→ Check Functions logs: `firebase functions:log`
→ Verify credentials in `functions/src/index.ts`

### WebView shows error page
→ Verify ToyyibPay credentials are correct
→ Check internet connection
→ Try again (bill creation might have timed out)

### Payment succeeds but no order
→ Check MainActivity logs: `adb logcat | grep "Payment"`
→ Verify cart is not empty during payment
→ Check Firestore security rules

---

## 📊 Expected Behavior

### When "Online Banking" selected:

```
1. User clicks "Proceed to Payment Gateway"
   ↓
2. App calls createToyyibPayBill API
   - POST to Cloud Function
   - Creates bill on ToyyibPay
   - Returns payment URL
   ↓
3. App stores pending order with payment URL
   ↓
4. App opens PaymentWebViewScreen
   - Loads ToyyibPay page
   - User enters card details
   - Completes payment
   ↓
5. ToyyibPay redirects to wz://payment/success?order_id=xxx
   ↓
6. MainActivity.DeepLinkHandler receives callback
   - Retrieves pending order
   - Creates actual Firestore order
   - Clears cart
   - Shows order tracking
   ↓
7. ToyyibPay sends webhook to paymentCallback
   - Updates order with payment reference
   - Logs payment confirmation
```

### When "Cash" selected:

```
1. User clicks "Place Order"
   ↓
2. Order created immediately in Firestore
   ↓
3. Cart cleared
   ↓
4. Order tracking shown
```

---

## ✅ Success Indicators

- [ ] Cloud Functions deployed without errors
- [ ] App builds successfully
- [ ] Opening cart shows correct total
- [ ] Selecting "Online Banking" shows "Proceed to Payment Gateway" button
- [ ] Clicking button shows loading indicator
- [ ] WebView opens with ToyyibPay payment page
- [ ] Entering test card shows payment processing
- [ ] Payment success returns to app
- [ ] Order appears in Orders tab
- [ ] Order status is "Confirmed"
- [ ] Payment status shows "Paid"
- [ ] Cart is empty after payment

---

## 🎯 Test Checklist

Copy this to track your testing:

- [ ] Functions deployed
- [ ] App built and installed
- [ ] Login/register works
- [ ] Add items to cart
- [ ] Cart total calculates correctly
- [ ] Select "Online Banking"
- [ ] Payment gateway opens
- [ ] Test payment completes
- [ ] Order created successfully
- [ ] Cart cleared
- [ ] Order tracking shows order
- [ ] Cash payment still works
- [ ] Payment failure handled
- [ ] Cancel button works

---

**Ready to test?** Run `.\deploy-payment-functions.ps1` now!
