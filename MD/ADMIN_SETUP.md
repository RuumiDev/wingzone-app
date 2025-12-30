# WingZone Admin Dashboard Setup

## Prerequisites
- Node.js 18+ installed
- Firebase project created
- Firebase CLI installed: `npm install -g firebase-tools`

## Quick Start

### 1. Create React + TypeScript + Vite Project
```bash
cd wz-app
npm create vite@latest admin-dashboard -- --template react-ts
cd admin-dashboard
```

### 2. Install Dependencies
```bash
npm install firebase
npm install @tanstack/react-query
npm install react-router-dom
npm install react-hook-form zod @hookform/resolvers
npm install recharts
npm install lucide-react
npm install sonner
npm install qrcode.react
npm install react-to-print
npm install date-fns

# Tailwind CSS
npm install -D tailwindcss postcss autoprefixer
npx tailwindcss init -p

# shadcn/ui (optional but recommended)
npx shadcn-ui@latest init
```

### 3. Configure Tailwind
Update `tailwind.config.js`:
```javascript
/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        wingzone: {
          red: '#820000',
          orange: '#ef7725',
          redLight: '#b3282e',
        },
      },
    },
  },
  plugins: [],
}
```

### 4. Firebase Configuration
Create `src/lib/firebase.ts`:
```typescript
import { initializeApp } from 'firebase/app';
import { getAuth } from 'firebase/auth';
import { getFirestore } from 'firebase/firestore';
import { getStorage } from 'firebase/storage';

const firebaseConfig = {
  apiKey: "YOUR_API_KEY",
  authDomain: "YOUR_PROJECT.firebaseapp.com",
  projectId: "YOUR_PROJECT_ID",
  storageBucket: "YOUR_BUCKET.appspot.com",
  messagingSenderId: "YOUR_SENDER_ID",
  appId: "YOUR_APP_ID"
};

const app = initializeApp(firebaseConfig);

export const auth = getAuth(app);
export const db = getFirestore(app);
export const storage = getStorage(app);
```

### 5. Project Structure
```
admin-dashboard/
├── public/
├── src/
│   ├── components/
│   │   ├── layout/
│   │   │   ├── Sidebar.tsx
│   │   │   ├── Header.tsx
│   │   │   └── Layout.tsx
│   │   ├── menu/
│   │   │   ├── MenuList.tsx
│   │   │   ├── MenuForm.tsx
│   │   │   └── MenuCard.tsx
│   │   ├── orders/
│   │   │   ├── OrderList.tsx
│   │   │   ├── OrderCard.tsx
│   │   │   └── OrderDetails.tsx
│   │   └── ui/
│   │       └── (shadcn components)
│   ├── hooks/
│   │   ├── useMenu.ts
│   │   ├── useOrders.ts
│   │   └── useAuth.ts
│   ├── lib/
│   │   ├── firebase.ts
│   │   ├── queries.ts
│   │   └── utils.ts
│   ├── pages/
│   │   ├── Dashboard.tsx
│   │   ├── Menu.tsx
│   │   ├── Orders.tsx
│   │   ├── Kitchen.tsx
│   │   ├── GroupOrders.tsx
│   │   ├── Inventory.tsx
│   │   ├── Receipts.tsx
│   │   ├── Analytics.tsx
│   │   └── Login.tsx
│   ├── types/
│   │   └── index.ts
│   ├── App.tsx
│   ├── main.tsx
│   └── index.css
├── .env
├── package.json
├── tsconfig.json
├── vite.config.ts
└── tailwind.config.js
```

### 6. Environment Variables
Create `.env`:
```
VITE_FIREBASE_API_KEY=your_api_key
VITE_FIREBASE_AUTH_DOMAIN=your_auth_domain
VITE_FIREBASE_PROJECT_ID=your_project_id
VITE_FIREBASE_STORAGE_BUCKET=your_storage_bucket
VITE_FIREBASE_MESSAGING_SENDER_ID=your_sender_id
VITE_FIREBASE_APP_ID=your_app_id
```

### 7. Run Development Server
```bash
npm run dev
```

## Firebase Setup

### 1. Initialize Firebase in Project Root
```bash
firebase login
firebase init
```

Select:
- ✅ Firestore
- ✅ Functions
- ✅ Hosting
- ✅ Storage

### 2. Deploy Firestore Rules
```bash
firebase deploy --only firestore:rules
```

### 3. Deploy Functions
```bash
cd functions
npm install
cd ..
firebase deploy --only functions
```

## Key Features Implementation Order

1. **Phase 1: Authentication & Layout**
   - Admin login
   - Sidebar navigation
   - Protected routes

2. **Phase 2: Menu Management**
   - View menu items
   - Add/Edit/Delete items
   - Toggle availability
   - Image upload

3. **Phase 3: Order Management**
   - View orders
   - Update order status
   - Print receipts

4. **Phase 4: Kitchen Dashboard**
   - Queue management
   - Status updates
   - Print labels

5. **Phase 5: Analytics & Reports**
   - Charts and graphs
   - Export functionality

## Useful Commands

```bash
# Development
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview

# Deploy to Firebase Hosting
firebase deploy --only hosting

# Deploy everything
firebase deploy
```

## Testing

- Admin credentials: Create first admin user manually in Firebase Console
- Test menu CRUD operations
- Test order flow from Android app
- Verify receipt generation
- Test label printing

## Security Checklist

- ✅ Firebase Security Rules deployed
- ✅ Admin role verification in Firestore
- ✅ Protected API routes
- ✅ Environment variables secured
- ✅ CORS configured properly

## Printer Integration

For thermal printer integration:
1. Use browser Print API via `react-to-print`
2. Configure printer settings in Windows/macOS
3. Test with thermal label size (40mm x 30mm)
4. Ensure QR codes render correctly

## Support

For issues:
1. Check Firebase Console logs
2. Review browser console errors
3. Verify Firestore security rules
4. Check network tab for API calls

---

Ready to start building! 🚀
