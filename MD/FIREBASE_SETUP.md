# Firebase Setup Guide for WingZone

## Step 1: Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add Project"
3. Enter project name: `wingzone-app` (or your preferred name)
4. Enable Google Analytics (optional)
5. Click "Create Project"

## Step 2: Register Web App

1. In Firebase Console, click the **Web icon** (</>) to add a web app
2. App nickname: `WingZone Admin Dashboard`
3. Check "Also set up Firebase Hosting" (optional)
4. Click "Register app"
5. **Copy the Firebase configuration** - you'll need this!

## Step 3: Enable Firebase Services

### Authentication
1. Go to **Authentication** > **Get Started**
2. Click **Sign-in method** tab
3. Enable **Email/Password** provider
4. Click **Save**

### Firestore Database
1. Go to **Firestore Database** > **Create database**
2. Choose **Start in test mode** (for development)
3. Select location closest to your users
4. Click **Enable**

### Storage
1. Go to **Storage** > **Get started**
2. Start in **test mode**
3. Click **Done**

### Functions
1. Upgrade to **Blaze Plan** (pay as you go - required for Cloud Functions)
2. Go to **Functions** section
3. Click **Get started** (we'll deploy functions later)

## Step 4: Configure Admin Dashboard

1. Open `admin-dashboard/src/lib/firebase.ts`
2. Replace the `firebaseConfig` object with your values from Step 2
3. Save the file

Example:
```typescript
const firebaseConfig = {
  apiKey: "AIzaSyXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX",
  authDomain: "wingzone-app.firebaseapp.com",
  projectId: "wingzone-app",
  storageBucket: "wingzone-app.appspot.com",
  messagingSenderId: "123456789012",
  appId: "1:123456789012:web:abcdefghijklmnop"
};
```

## Step 5: Set Up Firestore Security Rules

1. Go to **Firestore Database** > **Rules** tab
2. Replace with the following rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Helper functions
    function isAuthenticated() {
      return request.auth != null;
    }
    
    function isAdmin() {
      return isAuthenticated() && 
             get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin';
    }
    
    // Users collection - only admins can read/write
    match /users/{userId} {
      allow read, write: if isAdmin();
    }
    
    // Menu items - admins can write, anyone can read
    match /menuItems/{itemId} {
      allow read: if true;
      allow write: if isAdmin();
    }
    
    // Group orders - users can read their own, admins can read all
    match /groupOrders/{orderId} {
      allow read: if isAuthenticated();
      allow create: if isAuthenticated();
      allow update, delete: if isAdmin() || 
                               resource.data.hostId == request.auth.uid;
    }
    
    // Order members - users can read their own, admins can read all
    match /orderMembers/{memberId} {
      allow read: if isAuthenticated();
      allow create: if isAuthenticated();
      allow update: if isAdmin() || 
                     resource.data.userId == request.auth.uid;
      allow delete: if isAdmin();
    }
    
    // Cart items - users can read/write their own
    match /cartItems/{cartId} {
      allow read, write: if isAuthenticated() && 
                            resource.data.userId == request.auth.uid;
    }
  }
}
```

3. Click **Publish**

## Step 6: Set Up Storage Security Rules

1. Go to **Storage** > **Rules** tab
2. Replace with:

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    // Menu item images - admins can upload, anyone can read
    match /menu/{imageId} {
      allow read: if true;
      allow write: if request.auth != null;
    }
    
    // Receipts - authenticated users only
    match /receipts/{receiptId} {
      allow read, write: if request.auth != null;
    }
  }
}
```

3. Click **Publish**

## Step 7: Create First Admin User

1. Go to **Authentication** > **Users** tab
2. Click **Add user**
3. Email: `admin@wingzone.com`
4. Password: `admin123` (or your preferred password)
5. Click **Add user**
6. **Copy the User UID** - you'll need this!

## Step 8: Add Admin Role to Firestore

1. Go to **Firestore Database** > **Data** tab
2. Click **Start collection**
3. Collection ID: `users`
4. Click **Next**
5. Document ID: (paste the User UID from Step 7)
6. Add fields:
   - Field: `email`, Type: string, Value: `admin@wingzone.com`
   - Field: `role`, Type: string, Value: `admin`
   - Field: `displayName`, Type: string, Value: `Administrator`
   - Field: `createdAt`, Type: timestamp, Value: (current timestamp)
7. Click **Save**

## Step 9: Install Firebase CLI (for Functions deployment)

```bash
npm install -g firebase-tools
```

## Step 10: Initialize Firebase Project

```bash
cd wz-app
firebase login
firebase init
```

Select:
- **Firestore**: Configure rules
- **Functions**: Set up Cloud Functions
- **Hosting**: Configure hosting (optional)
- **Storage**: Configure security rules

Choose:
- Language: **TypeScript**
- ESLint: **Yes**
- Install dependencies: **Yes**

## Step 11: Update Admin Dashboard Config

Update `admin-dashboard/src/lib/firebase.ts` with your actual Firebase config values.

## Step 12: Test Connection

1. Start admin dashboard: `npm run dev`
2. Login with: `admin@wingzone.com` / `admin123`
3. Verify authentication works

## Next Steps

- [ ] Deploy Cloud Functions for order processing
- [ ] Set up Android app Firebase connection
- [ ] Configure Firebase Cloud Messaging for notifications
- [ ] Set up Firebase Analytics
- [ ] Deploy admin dashboard to Firebase Hosting

## Troubleshooting

**Authentication fails:**
- Check Firebase config values are correct
- Ensure Email/Password provider is enabled
- Verify admin user exists

**Firestore permission denied:**
- Check security rules are published
- Verify user has admin role in Firestore
- Check user is authenticated

**Can't deploy functions:**
- Ensure you're on Blaze plan
- Run `firebase login` first
- Check Node.js version (should be 18+)

## Useful Commands

```bash
# Deploy Firestore rules
firebase deploy --only firestore:rules

# Deploy Storage rules
firebase deploy --only storage

# Deploy Cloud Functions
firebase deploy --only functions

# Deploy everything
firebase deploy

# View logs
firebase functions:log
```
