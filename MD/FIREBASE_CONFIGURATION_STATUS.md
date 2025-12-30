# Firebase Configuration Status

## ✅ Completed Steps

### Android App
- [x] App added to Firebase Console
- [x] `google-services.json` downloaded and placed in `app/` directory
- [x] Firebase dependencies added to `build.gradle.kts`
- [x] Google Services plugin configured

**Firebase Dependencies Added:**
```kotlin
- firebase-auth-ktx (Authentication)
- firebase-firestore-ktx (Database)
- firebase-storage-ktx (File Storage)
- firebase-messaging-ktx (Push Notifications)
- firebase-analytics-ktx (Analytics)
```

### Admin Dashboard
- [x] Firebase SDK installed
- [x] Configuration partially set up in `src/lib/firebase.ts`
- [x] Auth service ready with Firebase integration
- [x] Firestore services created for menu, orders, dashboard

## ⏳ Next Steps

### 1. Add Web App to Firebase (5 minutes)

You've added the Android app, now add a web app for the admin dashboard:

1. Go to [Firebase Console](https://console.firebase.google.com/project/wingzone-app)
2. Click the **⚙️ gear icon** > **Project settings**
3. Scroll down to "Your apps" section
4. Click the **</>** (Web) icon to add a web app
5. App nickname: `WingZone Admin Dashboard`
6. ✅ Check "Also set up Firebase Hosting" (optional)
7. Click **Register app**
8. **Copy the `appId`** value (looks like `1:24304517780:web:xxxxxxxxxx`)
9. Update `admin-dashboard/src/lib/firebase.ts`:
   ```typescript
   appId: "1:24304517780:web:YOUR_ACTUAL_WEB_APP_ID"
   ```

### 2. Enable Firebase Services (10 minutes)

#### Enable Authentication
1. Firebase Console > **Authentication**
2. Click **Get Started**
3. Click **Sign-in method** tab
4. Enable **Email/Password**
5. Click **Save**

#### Enable Firestore Database
1. Firebase Console > **Firestore Database**
2. Click **Create database**
3. Choose **Start in test mode** (for now)
4. Select your preferred location (e.g., `us-central`)
5. Click **Enable**

#### Enable Storage
1. Firebase Console > **Storage**
2. Click **Get started**
3. Start in **test mode**
4. Click **Done**

### 3. Create Admin User (5 minutes)

1. Firebase Console > **Authentication** > **Users**
2. Click **Add user**
3. Email: `admin@wingzone.com`
4. Password: `admin123` (or your preferred secure password)
5. Click **Add user**
6. **📋 COPY THE USER UID** (you'll need this next!)

### 4. Add Admin Role in Firestore (3 minutes)

1. Firebase Console > **Firestore Database**
2. Click **Start collection**
3. Collection ID: `users`
4. Document ID: **Paste the User UID from step 3**
5. Add these fields:

| Field | Type | Value |
|-------|------|-------|
| `email` | string | `admin@wingzone.com` |
| `role` | string | `admin` |
| `displayName` | string | `Administrator` |
| `createdAt` | timestamp | (click "current timestamp") |

6. Click **Save**

### 5. Set Up Security Rules (5 minutes)

#### Firestore Rules
1. Firestore Database > **Rules** tab
2. Replace with:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    function isAuthenticated() {
      return request.auth != null;
    }
    
    function isAdmin() {
      return isAuthenticated() && 
             get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin';
    }
    
    match /users/{userId} {
      allow read, write: if isAdmin();
    }
    
    match /menuItems/{itemId} {
      allow read: if true;
      allow write: if isAdmin();
    }
    
    match /groupOrders/{orderId} {
      allow read: if isAuthenticated();
      allow create: if isAuthenticated();
      allow update, delete: if isAdmin() || resource.data.hostId == request.auth.uid;
    }
    
    match /orderMembers/{memberId} {
      allow read: if isAuthenticated();
      allow create: if isAuthenticated();
      allow update: if isAdmin() || resource.data.userId == request.auth.uid;
      allow delete: if isAdmin();
    }
  }
}
```

3. Click **Publish**

#### Storage Rules
1. Storage > **Rules** tab
2. Replace with:

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /menu/{imageId} {
      allow read: if true;
      allow write: if request.auth != null;
    }
    
    match /receipts/{receiptId} {
      allow read, write: if request.auth != null;
    }
  }
}
```

3. Click **Publish**

### 6. Sync Android Project (2 minutes)

```bash
# In Android Studio or terminal
cd C:\Users\Rog\Documents\Amiki\Chp5\MP\wz-app
./gradlew build
```

Or in Android Studio:
- Click **Sync Now** button (should appear at top of file)
- Or: **File** > **Sync Project with Gradle Files**

### 7. Test Everything! (10 minutes)

#### Test Admin Dashboard:
1. Make sure dev server is running: `cd admin-dashboard && npm run dev`
2. Go to http://localhost:5174
3. Login with `admin@wingzone.com` / `admin123`
4. Check browser console for any Firebase errors
5. Try adding a menu item
6. Check Firestore to see if it appears

#### Test Android App:
1. Open Android Studio
2. Run the app on emulator
3. Check logcat for Firebase initialization
4. Try registering a new user
5. Try logging in

## 📋 Quick Checklist

- [ ] Web app added to Firebase
- [ ] `appId` updated in `admin-dashboard/src/lib/firebase.ts`
- [ ] Authentication enabled (Email/Password)
- [ ] Firestore Database created
- [ ] Storage enabled
- [ ] Admin user created in Authentication
- [ ] Admin role added in Firestore `users` collection
- [ ] Firestore security rules published
- [ ] Storage security rules published
- [ ] Android project synced with Gradle
- [ ] Admin dashboard tested
- [ ] Android app tested

## 🎯 Current Status

✅ **Android App**: Configured with Firebase
✅ **google-services.json**: Downloaded and added
✅ **Firebase Dependencies**: Added to build.gradle.kts
✅ **Admin Config**: Partially configured (needs web app ID)
⏳ **Web App**: Needs to be added to Firebase
⏳ **Services**: Need to be enabled (Auth, Firestore, Storage)
⏳ **Admin User**: Needs to be created
⏳ **Security Rules**: Need to be set up

## 🚀 Estimated Time to Complete

- Steps 1-5: **30 minutes**
- Steps 6-7: **15 minutes**
- **Total: ~45 minutes**

## 💡 Pro Tips

1. **Keep both Firebase Console tabs open**: One for Android settings, one for Web settings
2. **Save your admin user UID**: You'll need it for the Firestore document
3. **Test with browser console open**: You'll see Firebase connection status
4. **Start with test mode**: Switch to production rules later after testing

## 🆘 Need Help?

If you get stuck:
1. Check browser console for errors
2. Verify all config values match Firebase Console
3. Make sure services are enabled
4. Check security rules are published
5. Verify admin user has correct role in Firestore

## Next: Real-Time Features! 🔥

Once Firebase is working:
- [ ] Add real-time menu updates
- [ ] Implement live order tracking
- [ ] Set up push notifications
- [ ] Deploy Cloud Functions
- [ ] Connect Android app to same Firebase project
