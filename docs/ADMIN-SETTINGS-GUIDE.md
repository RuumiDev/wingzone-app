# Admin Dashboard - Settings Configuration

## Overview
The Settings page provides a user-friendly interface to configure app-wide settings without directly accessing the Firestore database.

## Accessing Settings
1. Log in to the admin dashboard at: `http://localhost:3000` (or your deployed URL)
2. Navigate to: **Management > Settings**
3. The Settings page will load current values from Firebase

## Available Settings

### 1. Tax Rate (%)
- **Description**: Sales tax applied to all orders
- **Format**: Percentage (e.g., enter `6` for 6% tax)
- **Default**: 6%
- **Range**: 0 - 100%
- **Example**: 
  - 6% tax = enter `6.00`
  - 8.5% tax = enter `8.50`
  - No tax = enter `0.00`

### 2. Delivery Fee (RM)
- **Description**: Standard delivery charge
- **Format**: Malaysian Ringgit (RM)
- **Default**: 0.00
- **Example**:
  - Free delivery = `0.00`
  - Standard delivery = `5.00`
  - Express delivery = `10.00`

### 3. Minimum Order Amount (RM)
- **Description**: Minimum cart total required for checkout
- **Format**: Malaysian Ringgit (RM)
- **Default**: 0.00
- **Example**:
  - No minimum = `0.00`
  - Minimum RM20 = `20.00`
  - Minimum RM50 = `50.00`

## How to Update Settings

1. **Edit Values**:
   - Click on any field to edit
   - Enter the new value
   - Use the number input controls to adjust precisely

2. **Save Changes**:
   - Click the "Save Settings" button
   - Wait for the success message
   - Changes take effect immediately in the mobile app

3. **Reset Changes**:
   - Click the "Reset" button to reload values from Firebase
   - This discards any unsaved changes

## Real-Time Updates

Changes made in the Settings page are:
- ✅ Immediately saved to Firebase Firestore (`appSettings/general` document)
- ✅ Automatically synced to all mobile app users in real-time
- ✅ Applied to the next cart calculation
- ✅ Visible in the mobile app's cart summary (Tax line item)

## Mobile App Integration

### Tax Rate
- **Where it appears**: Cart Screen, Order Summary
- **Display format**: "Tax (X%): RM Y.YY"
- **Calculation**: `subtotal × (taxRate / 100)`

### Example Cart Display:
```
Order Summary
─────────────────────────
Subtotal         RM 25.90
Tax (6%)          RM 1.55
─────────────────────────
Total            RM 27.45
```

## Firebase Structure

Settings are stored in Firestore:
```
Collection: appSettings
└── Document: general
    ├── taxRate: 0.06          (6% = 0.06)
    ├── deliveryFee: 0.00      (RM 0.00)
    └── minimumOrderAmount: 0.00 (RM 0.00)
```

## Troubleshooting

### Settings not loading
- **Check**: Firebase connection
- **Check**: Browser console for errors
- **Try**: Refresh the page

### Changes not appearing in mobile app
- **Check**: Mobile app has internet connection
- **Try**: Close and reopen the mobile app
- **Verify**: Firebase Firestore has the correct values

### Permission errors
- **Check**: User has admin privileges
- **Check**: Firebase security rules allow write access

## Development Notes

### Tech Stack
- **Frontend**: React + TypeScript + Ant Design
- **Backend**: Firebase Firestore
- **Real-time**: Firestore snapshot listeners

### File Locations
- Settings Page: `slash-admin-template/src/pages/management/settings/index.tsx`
- Route Config: `slash-admin-template/src/routes/sections/dashboard/frontend.tsx`
- Menu Config: `slash-admin-template/src/_mock/assets.ts`

### Adding New Settings

To add a new setting field:

1. **Update AppSettings interface** in `settings/index.tsx`:
```typescript
interface AppSettings {
  taxRate: number;
  deliveryFee: number;
  minimumOrderAmount: number;
  newSetting: number; // Add new field
}
```

2. **Add Form.Item** in the Settings page:
```tsx
<Form.Item
  label="New Setting"
  name="newSetting"
  rules={[{ required: true }]}
>
  <InputNumber />
</Form.Item>
```

3. **Update Kotlin data model** in Android app:
```kotlin
data class AppSettings(
    val taxRate: Double = 0.06,
    val deliveryFee: Double = 0.0,
    val minimumOrderAmount: Double = 0.0,
    val newSetting: Double = 0.0 // Add new field
)
```

4. **Update FirebaseCartRepository** to read new setting

## Security

- Only users with admin privileges can access Settings
- All changes are logged in Firebase
- Validation prevents invalid values (negative numbers, out of range)

## Support

For issues or questions:
1. Check browser console for errors
2. Verify Firebase configuration
3. Review Firebase Firestore security rules
4. Check mobile app logs for sync issues
