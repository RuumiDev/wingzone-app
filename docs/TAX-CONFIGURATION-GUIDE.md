# Tax Rate Configuration - Complete

## What Was Done

Added **App Configuration** section to the existing admin dashboard Settings page with Firebase integration.

### Location
**File**: `admin-dashboard/src/pages/SettingsPage.tsx`

### Features Added

1. **App Configuration Card** (appears FIRST on Settings page)
   - Tax Rate (%) - with decimal precision
   - Delivery Fee (RM)
   - Minimum Order Amount (RM)

2. **Firebase Integration**
   - Loads settings from `appSettings/general` Firestore document on page load
   - Saves changes back to Firebase with "Save App Settings" button
   - Real-time sync - mobile app will receive updates immediately
   - Shows loading spinner while fetching data
   - Shows saving state on button when submitting

3. **UI Enhancements**
   - Input validation (min/max values, step increments)
   - Current value display below each field
   - Success alerts on save
   - Disabled button during save operation
   - Bootstrap Icons integration

### How It Works

#### On Page Load:
1. Fetches `appSettings/general` document from Firestore
2. Converts `taxRate` from decimal (0.06) to percentage (6%) for display
3. Displays loading spinner during fetch

#### On Save:
1. Converts tax rate from percentage back to decimal
2. Saves to Firebase with all three values
3. Adds `updatedAt` timestamp
4. Mobile app receives update immediately via snapshot listener

### Mobile App Integration

The mobile app (`FirebaseCartRepository.kt`) already has:
- ✅ Snapshot listener on `appSettings/general`
- ✅ Real-time tax rate updates
- ✅ Automatic cart recalculation

### Firebase Structure

```
appSettings/
  └── general/
      ├── taxRate: 0.06 (decimal, e.g., 6%)
      ├── deliveryFee: 5 (number)
      ├── minimumOrderAmount: 20 (number)
      └── updatedAt: "2024-01-15T10:30:00Z"
```

### Existing Sections (Kept)
- Restaurant Information (name, email, phone, address)
- Notification Preferences (order/email/sound alerts)

## Usage

1. **Access Settings**
   - Log into admin dashboard
   - Navigate to Settings page
   - See "App Configuration" section at the top

2. **Update Tax Rate**
   - Enter new percentage (e.g., 8.5)
   - Click "Save App Settings"
   - Mobile app updates immediately

3. **Update Delivery Fee**
   - Enter new amount in RM
   - Click "Save App Settings"
   - Changes reflect in mobile app

4. **Update Minimum Order**
   - Enter new minimum amount in RM
   - Click "Save App Settings"
   - Checkout validates against new minimum

## Testing

1. ✅ Open admin dashboard Settings page
2. ✅ Verify App Configuration section loads with current values
3. ✅ Change tax rate to 8.5%
4. ✅ Click "Save App Settings"
5. ✅ Open mobile app and add items to cart
6. ✅ Verify tax is calculated at 8.5%

## Notes

- Tax rate stored as decimal in Firebase (6% = 0.06)
- Admin UI shows/edits as percentage for clarity
- Mobile app listens for changes in real-time
- No app restart needed - updates apply immediately
- All three settings saved together in one document
