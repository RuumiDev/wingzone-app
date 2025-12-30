# Notification System Implementation

## Overview
The WingZone admin dashboard features a real-time notification system with smooth animations and Firebase integration.

## Features

### ✨ Animations
1. **Bell Ring Animation**
   - Bell icon wiggles when unread notifications exist
   - Continuous 2-second animation loop
   - Stops when all notifications are read

2. **Badge Bubble Pop**
   - Red notification count badge
   - Pops in with scale animation (0 → 1.2 → 1)
   - Gradient background with shadow

3. **Dropdown Slide Down**
   - Smooth slide-down animation on open
   - 300ms cubic-bezier easing

4. **Notification Slide In**
   - Each notification slides in from left
   - Staggered 400ms animation

5. **Delete Slide Out**
   - Notifications slide right and fade out
   - 300ms animation before removal

6. **Icon Bubble Effect**
   - Circular gradient background
   - Hover scales and rotates icon
   - Smooth shadow transitions

### 🔔 Notification Actions

#### Mark as Read
- Click icon bubble to mark single notification
- "Mark all read" button for bulk action
- Updates Firebase in real-time

#### Delete Notification
- Hover over notification to reveal delete button (X)
- Click to delete with smooth animation
- Permanently removes from Firebase

#### Visual States
- **Unread**: Blue background + blue left border + dot indicator
- **Read**: White background + gray text
- **Hover**: Slight translate right + darker background

### 🔥 Firebase Integration

#### Collection Structure
```typescript
/notifications
  {
    id: string (auto-generated)
    type: 'order' | 'system' | 'info'
    title: string
    message: string
    createdAt: Timestamp
    read: boolean
  }
```

#### Real-time Updates
- Uses Firebase `onSnapshot` listener
- Auto-updates when notifications added/removed
- No page refresh needed

#### Service Layer
```typescript
// Add notification
await notificationService.addNotification({
  type: 'order',
  title: 'New Order Received',
  message: 'Order #1234 - Table 5'
});

// Add order notification (shorthand)
await notificationService.addOrderNotification('1234', '5');

// Add system notification
await notificationService.addSystemNotification(
  'Menu Updated',
  'Successfully imported 67 items'
);
```

### 🎨 Styling Details

#### Positioning
- Dropdown: 380px width, 520px max-height
- Margin-right: 8px (spacing from browser edge)
- Margin-top: 12px (spacing from header)

#### Colors
- Primary: #007bff (blue)
- Danger: #dc3545 (red)
- Warning: #ffc107 (yellow)
- Unread background: #e7f3ff (light blue)

#### Animations
```scss
@keyframes bellRing {
  0%, 100% { transform: rotate(0deg); }
  10%, 30% { transform: rotate(-10deg); }
  20%, 40% { transform: rotate(10deg); }
}

@keyframes bubblePop {
  0% { transform: scale(0); opacity: 0; }
  50% { transform: scale(1.2); }
  100% { transform: scale(1); opacity: 1; }
}

@keyframes slideIn {
  from { opacity: 0; transform: translateX(-20px); }
  to { opacity: 1; transform: translateX(0); }
}

@keyframes slideOut {
  from { 
    opacity: 1; 
    max-height: 100px; 
    transform: translateX(0); 
  }
  to { 
    opacity: 0; 
    max-height: 0; 
    padding: 0; 
    transform: translateX(100%); 
  }
}
```

## Usage Examples

### Triggering Notifications

#### From Menu Import
```typescript
await seedMenuItems();
await notificationService.addSystemNotification(
  'Menu Updated',
  `Successfully imported ${count} menu items`
);
```

#### From Order Creation
```typescript
const order = await createOrder(orderData);
await notificationService.addOrderNotification(
  order.id,
  order.tableNumber
);
```

#### From System Events
```typescript
await notificationService.addSystemNotification(
  'Low Stock Alert',
  'Buffalo Wing sauce running low'
);
```

## Component Structure

```
NotificationDropdown/
├── NotificationDropdown.tsx       # Main component
├── NotificationDropdown.module.scss # Styles with animations
└── index.ts                       # Export
```

## State Management

```typescript
const [dropdownOpen, setDropdownOpen] = useState(false);
const [notifications, setNotifications] = useState<Notification[]>([]);
const [deletingIds, setDeletingIds] = useState<Set<string>>(new Set());
```

## Time Formatting

```typescript
const getTimeAgo = (timestamp: Timestamp) => {
  const diffMins = Math.floor(diffMs / 60000);
  
  if (diffMins < 1) return 'Just now';
  if (diffMins < 60) return `${diffMins}m ago`;
  if (diffHours < 24) return `${diffHours}h ago`;
  return `${diffDays}d ago`;
};
```

## Best Practices

1. **Always use the service layer** - Don't directly add to Firebase
2. **Keep messages concise** - Max 50 characters for title, 100 for message
3. **Use appropriate types** - 'order' for orders, 'system' for admin events
4. **Clean old notifications** - Implement auto-deletion after 7 days
5. **Batch operations** - Use Promise.all for marking multiple as read

## Future Enhancements

- [ ] Push notifications for mobile admin app
- [ ] Email notifications for critical events
- [ ] Notification preferences per admin user
- [ ] Category filtering (show only orders, only system, etc.)
- [ ] Sound alerts for new notifications
- [ ] Desktop notifications API integration

---

Last Updated: December 5, 2025
