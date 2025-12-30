# Real-Time Notification System for WingZone Admin Dashboard

## Overview
The admin dashboard now features a comprehensive real-time notification system that alerts administrators immediately when new orders are placed. The system includes sound alerts, browser notifications, toast popups, and a notification dropdown.

## Features

### 1. **Automatic Order Detection**
- Real-time monitoring of Firebase `orders` collection
- Detects both individual and group orders instantly
- Only notifies for new orders (not historical orders)

### 2. **Multi-Channel Notifications**

#### Sound Alerts
- Two-tone beep sound (800Hz + 1000Hz)
- Plays automatically when new order arrives
- Uses Web Audio API for consistent playback

#### Browser Notifications
- Native OS notifications with WingZone branding
- Requests permission on first login
- Shows order details: customer name, item count, total
- Requires interaction for visibility

#### Toast Notifications
- In-app popup notification in top-right corner
- Auto-dismisses after 5 seconds
- Shows animated progress bar
- Includes order emoji 🍗 for visual appeal
- Smooth slide-in animation

#### Notification Dropdown
- Bell icon in header with unread count badge
- Real-time list of all notifications
- Shows order ID, type (Individual/Group), customer name
- "View Order" button to navigate to Orders page
- Mark as read individually or all at once
- Delete notifications individually
- Animated bell ringing when unread notifications exist

## Technical Implementation

### Files Created/Modified

1. **`services/notifications.ts`**
   - `NotificationService` class for managing notifications
   - Order monitoring with Firebase snapshot listeners
   - Sound generation using Web Audio API
   - Browser notification API integration
   - Toast callback registration

2. **`components/ToastNotification/`**
   - Toast notification component with auto-dismiss
   - Animated progress bar
   - Responsive design
   - Multiple types: order, success, error, info

3. **`components/NotificationDropdown/NotificationDropdown.tsx`**
   - Enhanced with order-specific details
   - Order ID display (first 8 chars, uppercase)
   - Order type badge (Individual/Group)
   - "View Order" button functionality
   - Improved styling and animations

4. **`App.tsx`**
   - Integrates notification monitoring on authentication
   - Manages toast state
   - Registers toast callback with notification service
   - Handles "View Order" navigation

5. **`components/Header/Header.tsx`**
   - Passes `onViewOrder` prop to NotificationDropdown

## How It Works

### Initialization Flow
```
1. User logs in → App.tsx authenticated state changes
2. App.tsx registers toast callback with notificationService
3. App.tsx requests browser notification permission
4. notificationService.startOrderMonitoring() begins listening
```

### Order Detection Flow
```
1. Customer places order in mobile app
2. Order document created in Firebase 'orders' collection
3. notificationService's snapshot listener triggers
4. System checks: orderCreatedAt > lastOrderTimestamp
5. If true → Execute notification sequence:
   a) Add notification to Firebase 'notifications' collection
   b) Play two-tone sound
   c) Show browser notification
   d) Trigger toast callback (shows toast popup)
6. NotificationDropdown automatically updates via its own listener
```

### Data Structure

#### Notification Document (Firebase)
```typescript
{
  type: 'order' | 'system' | 'info',
  title: '🍗 New Order Received!' | '🍗 New Group Order!',
  message: '{customerName} • {itemCount} items • RM {total}',
  orderId: string,              // Full Firebase document ID
  orderType: 'individual' | 'group',
  orderTotal: number,
  customerName: string,
  createdAt: Timestamp,
  read: boolean
}
```

## User Experience

### When New Order Arrives:
1. **Sound**: Two-tone beep plays
2. **Toast**: Slide-in popup appears in top-right corner
3. **Browser**: OS notification displays (if permission granted)
4. **Badge**: Bell icon shows unread count
5. **Animation**: Bell icon animates with ringing motion

### Interacting with Notifications:
- **Click bell icon**: Opens dropdown with notification history
- **Click notification**: Marks as read (if unread)
- **Click "View Order"**: Navigates to Orders page
- **Click "Mark all as read"**: Marks all notifications as read
- **Click X button**: Deletes individual notification
- **Click toast X**: Dismisses toast immediately

## Configuration

### Sound Settings
Located in `services/notifications.ts`:
```typescript
oscillator.frequency.value = 800;  // First beep frequency
oscillator2.frequency.value = 1000; // Second beep frequency
gainNode.gain.setValueAtTime(0.3, ...); // Volume (0-1)
```

### Toast Duration
In `App.tsx`:
```typescript
<ToastNotification
  duration={5000}  // 5 seconds (in milliseconds)
/>
```

### Query Limit
In `services/notifications.ts`:
```typescript
const q = query(ordersRef, orderBy('createdAt', 'desc'), limit(20));
// Monitors last 20 orders for efficiency
```

## Testing

### Manual Testing Steps:
1. Open admin dashboard: http://localhost:5174
2. Log in with admin credentials
3. Allow browser notifications when prompted
4. Open mobile app (or another browser)
5. Place a test order as a customer
6. Observe admin dashboard:
   - Sound should play
   - Toast should appear
   - Browser notification should show
   - Bell badge should update
   - Click bell to see notification details

### Expected Results:
- ✅ Sound plays within 1 second of order creation
- ✅ Toast appears in top-right corner
- ✅ Browser notification displays (if permissions granted)
- ✅ Notification appears in dropdown
- ✅ Unread count badge shows correct number
- ✅ "View Order" button navigates to Orders page
- ✅ Mark as read functionality works
- ✅ Delete functionality works

## Browser Compatibility

### Audio API Support:
- ✅ Chrome/Edge: Full support
- ✅ Firefox: Full support
- ✅ Safari: Full support (iOS may require user interaction first)

### Browser Notifications:
- ✅ Chrome/Edge: Full support
- ✅ Firefox: Full support
- ✅ Safari: macOS full support, iOS limited

### Web Audio Context:
- Automatically handles both `AudioContext` and `webkitAudioContext`
- Falls back gracefully if not supported

## Security Considerations

1. **Permission Requests**: User must grant permission for browser notifications
2. **Firebase Rules**: Ensure proper read/write rules on 'notifications' collection
3. **Timestamp Validation**: Only notifies for orders after monitoring started
4. **User Authentication**: Notification system only activates when authenticated

## Performance

- **Real-time Sync**: Uses Firebase snapshot listeners (WebSocket)
- **Query Optimization**: Limits to 20 most recent orders
- **Memory Management**: Properly unsubscribes listeners on unmount
- **Animation Performance**: Uses CSS transforms for 60fps animations

## Troubleshooting

### No Sound Playing?
- Check browser audio permissions
- Ensure user has interacted with page (click anywhere)
- Check browser console for audio context errors

### No Browser Notifications?
- Check if permission was granted (click lock icon in address bar)
- Re-request permission: `notificationService.requestNotificationPermission()`

### Notifications Not Appearing?
- Verify Firebase connection
- Check browser console for errors
- Ensure 'notifications' collection has proper read permissions

### Old Orders Triggering Notifications?
- This is prevented by `lastOrderTimestamp` check
- Only orders created AFTER login will trigger notifications

## Future Enhancements

### Possible Improvements:
1. **Sound Selection**: Allow admin to choose notification sound
2. **Volume Control**: Add volume slider in settings
3. **Notification Filters**: Filter by order type, amount, etc.
4. **Notification History**: Archive older notifications
5. **Custom Alerts**: Set alerts for high-value orders
6. **Sound Toggle**: Mute/unmute button in header
7. **Notification Preferences**: Per-user notification settings
8. **SMS/Email**: Integrate external notification channels

## Maintenance

### Cleanup Tasks:
- Periodically delete old read notifications (Firebase function)
- Monitor notification collection size
- Review and optimize query limits
- Update sound files if needed

### Monitoring:
- Track notification delivery success rate
- Monitor Firebase reads usage
- Check for notification spam/duplicates

## Summary

The notification system provides administrators with immediate awareness of new orders through multiple channels. The multi-layered approach (sound + toast + browser + dropdown) ensures no order is missed, while the clean UI keeps the admin informed without being intrusive.

**Key Benefits:**
- ⚡ Instant real-time alerts
- 🔊 Multi-channel notifications
- 🎨 Beautiful, modern UI
- 📱 Responsive design
- 🔧 Easy to maintain and extend
