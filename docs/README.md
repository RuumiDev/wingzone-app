# WingZone Documentation

This folder contains all documentation for the WingZone Restaurant Management System.

## 📚 Documentation Index

### Setup & Getting Started
- **Quick Start Guide** - Step-by-step setup instructions
- **Installation Guide** - Detailed installation for admin dashboard and Android app

### Features & Fixes
- **[Fixes 2025-01-23](FIXES-2025-01-23.md)** - Latest: Notification overflow, fries exchange, dine-in terminology, scroll smoothness
- **Issues Resolved** - Summary of all fixes and improvements made
- **Menu Reorganization** - Details about menu structure changes

### API & Integration
- **Firebase Setup** - Database structure and configuration
- **Notification System** - Real-time notification implementation

### User Guides
- **Admin Dashboard Guide** - How to use the admin panel
- **Android App Guide** - User manual for the mobile app

---

## 🎯 Latest Updates (December 5, 2025)

### ✅ Completed Features

1. **Animated Notification System**
   - Real-time Firebase integration
   - Smooth bubble animations
   - Bell ring animation for unread notifications
   - Mark as read functionality
   - Delete individual notifications with slide-out animation
   - Better positioning (8px margin from browser edge)

2. **Logout Confirmation**
   - Modal confirmation dialog before logout
   - Loading state during logout process
   - Cancel option

3. **Loading States**
   - Custom animated loading spinner
   - 3-ring rotating spinner with brand colors
   - Applied to all admin pages
   - Smooth transitions

4. **Menu Organization**
   - Combo Meals sorted 1-12 (Admin + App)
   - Fixed scroll positioning in Android app
   - Categories align properly

5. **Code Cleanup**
   - Removed unused template files (~100 files)
   - Deleted layouts/, theme/ folders
   - Organized documentation in /docs folder

---

## 🔧 Technical Stack

### Admin Dashboard
- React 18 + TypeScript
- Vite 7.2.6
- Firebase Firestore (Real-time)
- Reactstrap for UI
- SCSS Modules

### Android App
- Kotlin + Jetpack Compose
- Material 3 Design
- Firebase SDK
- MVVM Architecture

### Firebase Collections
```
/menuItems
  - All menu items with categories
  
/notifications
  - Real-time notifications
  - Fields: type, title, message, createdAt, read
  
/orders
  - Customer orders
  
/settings
  - App configuration
  /availability
    - Item availability toggles
```

---

## 📖 How to Use This Documentation

1. **For Developers**: Start with technical implementation docs
2. **For Admins**: Check the Admin Dashboard Guide
3. **For Users**: Refer to the Android App Guide
4. **For Issues**: Check Issues Resolved document

---

## 🆘 Support

If you encounter any issues:
1. Check the relevant documentation file
2. Review the Issues Resolved document
3. Check Firebase console for data issues
4. Review browser/Android console for errors

---

## 📝 Contributing to Documentation

When adding new features:
1. Document in appropriate .md file
2. Update this README index
3. Save all .md files in /docs folder
4. Keep README.md in root for project overview

---

Last Updated: December 5, 2025
