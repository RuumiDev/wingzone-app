# 🚀 Deployment Guide - WingZone App

## Prerequisites

Before deploying, ensure you have:
- ✅ Firebase CLI installed (`npm install -g firebase-tools`)
- ✅ Logged into Firebase (`firebase login`)
- ✅ Firebase project initialized in this directory

## 📦 Deployment Steps

### 1️⃣ Build Admin Dashboard

```bash
# Navigate to admin dashboard
cd admin-dashboard

# Install dependencies (if not already done)
npm install

# Build for production
npm run build
```

This creates an optimized production build in `admin-dashboard/dist/`

### 2️⃣ Deploy to Firebase Hosting & Firestore

From the **root directory** (`wz-app`):

```bash
# Deploy everything (Hosting + Firestore + Storage Rules)
firebase deploy

# OR deploy individually:

# Deploy only hosting (admin dashboard)
firebase deploy --only hosting

# Deploy only Firestore rules and indexes
firebase deploy --only firestore

# Deploy only storage rules
firebase deploy --only storage

# Deploy only functions
firebase deploy --only functions
```

## 📋 What Gets Deployed

### 🌐 Firebase Hosting
- **URL**: Your admin dashboard will be available at: `https://YOUR-PROJECT-ID.web.app`
- **Content**: Admin dashboard (React app from `admin-dashboard/dist`)
- **Features**: 
  - Menu management
  - Order management
  - User management
  - **Reviews management** ⭐ (NEW)
  - Settings & configuration

### 🔥 Firestore Rules
- User authentication rules
- Menu items (public read)
- Orders (authenticated access)
- Reviews (public read for enabled/approved only)
- Lobbies (active lobby access)

### 🗂️ Firestore Indexes
- Lobby queries (code, status, host)
- **Review queries** (isEnabled, moderationStatus, createdAt) ⭐ (NEW)
- Optimized for filtering and sorting

### 📦 Storage Rules
- Image upload rules for menu items and banners
- User authentication required for uploads

## 🔍 Verify Deployment

After deployment:

1. **Check Hosting URL**:
   ```bash
   firebase hosting:channel:open live
   ```

2. **Test Admin Dashboard**:
   - Visit: `https://YOUR-PROJECT-ID.web.app`
   - Login with admin credentials
   - Navigate to Reviews page
   - Test enable/disable functionality

3. **Test Mobile App**:
   - Open Android app
   - Check if reviews appear on Home screen
   - Try rating a completed order
   - Verify only enabled reviews are visible

## 🛠️ Troubleshooting

### Build Errors
```bash
# Clear node modules and reinstall
cd admin-dashboard
rm -rf node_modules package-lock.json
npm install
npm run build
```

### Deployment Errors
```bash
# Check Firebase project
firebase projects:list

# Use specific project
firebase use YOUR-PROJECT-ID

# Check deployment status
firebase deploy --debug
```

### Index Creation
If you see index errors in mobile app:
- Firebase will show a link in the error message
- Click the link to auto-create the index
- Or wait 5-10 minutes after deploying indexes

## 🔄 Continuous Deployment

For easier deployments, you can create a deploy script:

**deploy.sh** (Linux/Mac):
```bash
#!/bin/bash
cd admin-dashboard
npm run build
cd ..
firebase deploy
```

**deploy.bat** (Windows):
```batch
@echo off
cd admin-dashboard
call npm run build
cd ..
firebase deploy
```

## 📱 Mobile App Updates

The Android app doesn't need redeployment for:
- ✅ Menu changes (real-time from Firestore)
- ✅ Review moderation (instant)
- ✅ Settings updates (real-time)

Rebuild the APK only when:
- Changing Kotlin code
- Adding new features
- Updating dependencies

## 🔐 Security Notes

- Admin dashboard is **publicly accessible** but requires authentication
- Firestore rules protect data access
- Reviews are filtered server-side (only enabled + approved shown)
- Regular security audits recommended

## 📊 Post-Deployment Checklist

- [ ] Admin dashboard loads correctly
- [ ] Login functionality works
- [ ] All menu items display
- [ ] Orders page shows data
- [ ] Reviews page shows all reviews
- [ ] Enable/Disable toggle works
- [ ] Mobile app shows only enabled reviews
- [ ] Rating submission works from mobile app
- [ ] New reviews appear in admin dashboard

## 🆘 Support

If you encounter issues:
1. Check Firebase Console for errors
2. Review browser console for frontend errors
3. Check Logcat for Android app errors
4. Verify Firestore rules are deployed correctly

---

**Last Updated**: February 18, 2026
**Version**: 1.0.0 with Reviews Feature 🌟
