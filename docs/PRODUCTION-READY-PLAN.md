# Production-Ready Fixes Implementation Plan

## Phase 1: Critical Fixes (Immediate - Blocking Issues)

### 1.1 Settings Not Persisting
**Issue**: Tax, delivery fee, minimum order revert after save
**Root Cause**: Need to verify Firebase write permissions and data structure
**Fix**: Check SettingsPage.tsx save logic and Firebase rules

### 1.2 Tax Rates Not Syncing to Mobile App  
**Issue**: Mobile app not receiving updates from Firebase
**Root Cause**: Check if FirebaseCartRepository is properly listening
**Fix**: Verify snapshot listener in FirebaseCartRepository.kt

### 1.3 User Persistence
**Issue**: Users not stored, "account not found" after restart
**Root Cause**: FirebaseAuth users not being synced to Firestore users collection
**Fix**: Create user document in Firestore on signup

### 1.4 Bone Type Selection Not Showing
**Issue**: Added in admin but not appearing in mobile
**Root Cause**: Need to verify customizationOptions structure
**Fix**: Debug Firebase document structure and app logic

### 1.5 Add to Cart Button Buried
**Issue**: Despite 200dp padding, button still not visible
**Fix**: Increase to 250dp or make button truly floating with elevated position

## Phase 2: Dashboard & Real-time Updates

### 2.1 Dashboard Orders Not Updating
**Issue**: Dashboard doesn't show new orders
**Root Cause**: Missing real-time listener or wrong collection
**Fix**: Implement onSnapshot for orders collection

### 2.2 Notification Badge Positioning
**Issue**: Badge floats far left instead of near bell icon
**Fix**: Update CSS positioning in Header component

### 2.3 Order Notifications
**Issue**: No notifications when orders placed
**Fix**: Implement notification system with Firebase listeners

### 2.4 Availability Sync
**Issue**: Flavor/sides/drinks availability not syncing
**Fix**: Ensure mobile app listens to customizationOptions changes

## Phase 3: Quick Actions & Navigation

### 3.1 Quick Action Buttons
- **New Menu Item**: Navigate to MenuPage with modal open
- **View All Orders**: Navigate to OrdersPage  
- **Print Receipt**: Implement print functionality

### 3.2 Order Status Dropdown
**Current**: Single button to change status
**Upgrade**: Dropdown with all status options

### 3.3 User Profile Functionality
- Username editing
- Password change
- Profile photo upload

## Phase 4: Order Tracking

### 4.1 User-Facing Order Tracking Page
- Real-time status updates
- Estimated preparation time
- Notification when ready

## Phase 5: Advanced Features

### 5.1 Group Orders Complete Redesign
- Lobby creation/joining
- Real-time member updates  
- Split bill calculation
- Master order consolidation

### 5.2 Receipt/Invoice System
- Thermal printer-ready format (40mm x 30mm)
- QR code generation
- Individual & group receipts
- Sticker labels with seat ID

## Implementation Order

**Week 1 - Critical Fixes:**
- Day 1-2: Settings persistence + Tax sync
- Day 3-4: User persistence + Admin users display
- Day 5: Bone type selection + Button padding

**Week 2 - Dashboard & Sync:**
- Day 1-2: Dashboard real-time updates
- Day 3: Notification system
- Day 4-5: Availability sync + Quick actions

**Week 3 - Enhanced Features:**
- Day 1-2: Order status management + User profile
- Day 3-5: Order tracking page

**Week 4 - Advanced Features:**
- Day 1-3: Group orders redesign
- Day 4-5: Receipt/invoice system

---

## Starting Implementation Now...
