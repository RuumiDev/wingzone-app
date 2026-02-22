# WingZone Admin Dashboard

A modern, premium SaaS-style admin dashboard for managing WingZone restaurant operations, built with React, TypeScript, and Vite.

## 🚀 Live Demo

**Production URL:** [https://wingzone-app.web.app](https://wingzone-app.web.app)

## ✨ Features

### Dashboard
- **Real-time Statistics**: Today's orders, revenue, pending orders, and active group orders
- **Modern Stat Cards**: Circular colored icon backgrounds with smooth hover effects
- **Recent Orders Table**: Live updates of order activity
- **Quick Actions**: Fast access to common tasks

### Orders Management
- **Individual Orders**: Manage single customer orders
- **Group Orders**: Handle multi-user collaborative orders
- **Date Grouping**: Orders organized by Today, Yesterday, and older dates
- **Status Updates**: Mark orders as ready, confirm payments, cancel orders
- **Receipt Printing**: Generate and print thermal receipts
- **Kitchen Integration**: Packaging stickers with ingredient lists
- **Search Functionality**: Find orders by ID, code, or customer name

### Menu Management
- **CRUD Operations**: Create, read, update, and delete menu items
- **Category Organization**: Entrees, sides, beverages, and more
- **Image Upload**: Product photos with Firebase Storage integration
- **Availability Toggle**: Enable/disable items
- **Price Management**: Real-time pricing updates

### User Management
- **Customer List**: View all registered users
- **User Details**: Name, email, phone, registration date
- **Account Status**: Active/inactive toggle
- **Search & Filter**: Find users quickly

### Reviews Management
- **Customer Feedback**: View all product reviews
- **Rating System**: 5-star rating display
- **Moderation**: Approve/reject reviews
- **Pagination**: 10 reviews per page with GSAP animations

### Banners Management
- **Promotional Banners**: Create and manage app banners
- **Image Upload**: Banner graphics with Firebase Storage
- **Active/Inactive**: Toggle banner visibility
- **Order Priority**: Set display order

### Settings
- **Admin Preferences**: Customize dashboard behavior
- **Printer Configuration**: Thermal printer setup for receipts
- **Tax Settings**: Configure tax rates and calculations

## 🎨 UI Features

### Modern SaaS Design
- **Split-Screen Login**: Food imagery with elegant form layout
- **Premium Sidebar**: Dark slate background (#1e293b) with orange accents
- **Modern Cards**: Rounded corners, soft shadows, smooth hover effects
- **Pill Badges**: Rounded-full badges with semantic colors
- **Clean Tables**: Borderless design with subtle row separators

### Animations (GSAP)
- **Sidebar Animation**: Smooth slide-in effect on mount
- **Review Pagination**: Fade-in animations for smooth transitions
- **Loader Component**: Bouncing dots with UniformLoader component

### Notifications (SweetAlert2)
- **Toast Notifications**: Non-intrusive success/error messages
- **Confirmation Modals**: Beautiful confirm dialogs for destructive actions
- **Auto-dismiss**: Timer-based with progress bar

## 🛠️ Tech Stack

### Core
- **React 18.3**: Modern hooks and concurrent features
- **TypeScript 5.6**: Type-safe development
- **Vite 7.2**: Lightning-fast build tool with HMR

### UI/UX
- **Reactstrap**: Bootstrap 5 components
- **Bootstrap 5.3**: Responsive grid and utilities
- **GSAP 3.12**: Professional-grade animations
- **SweetAlert2**: Beautiful alert/toast system
- **SCSS Modules**: Component-scoped styling

### Backend/Services
- **Firebase 11.2**: Backend-as-a-Service
  - **Firestore**: Real-time NoSQL database
  - **Authentication**: Admin login system
  - **Storage**: Image and file uploads
  - **Hosting**: Production deployment
- **Firebase Admin SDK**: Server-side operations

### Additional Libraries
- **date-fns**: Date formatting and manipulation
- **classnames**: Conditional CSS classes
- **canvas**: Receipt/sticker generation

## 📁 Project Structure

```
admin-dashboard/
├── src/
│   ├── components/          # Reusable UI components
│   │   ├── Header/         # Top navigation bar
│   │   ├── Sidebar/        # Premium navigation sidebar
│   │   ├── Widget/         # Modern card containers
│   │   ├── UniformLoader/  # GSAP-animated loader
│   │   ├── ReceiptModal/   # Receipt preview & print
│   │   └── ...
│   ├── pages/              # Main application pages
│   │   ├── LoginPage.tsx   # Split-screen authentication
│   │   ├── DashboardPage.tsx
│   │   ├── OrdersPage.tsx
│   │   ├── MenuPage.tsx
│   │   ├── ReviewsPage.tsx
│   │   ├── BannersPage.tsx
│   │   ├── UsersPage.tsx
│   │   ├── AvailabilityPage.tsx
│   │   └── SettingsPage.tsx
│   ├── services/           # Business logic & API calls
│   │   ├── firebase.ts     # Firestore services
│   │   ├── auth.ts         # Authentication
│   │   ├── printService.ts # Receipt printing
│   │   └── ...
│   ├── utils/              # Helper functions
│   │   ├── toast.ts        # SweetAlert2 wrapper
│   │   └── ...
│   ├── lib/                # Third-party configs
│   │   └── firebase.ts     # Firebase initialization
│   ├── types.ts            # TypeScript definitions
│   ├── App.tsx             # Root component
│   └── main.tsx            # Application entry
├── public/                 # Static assets
│   ├── wingzone-logo.png
│   ├── sounds/             # Notification sounds
│   └── ...
└── dist/                   # Production build output
```

## 🚦 Getting Started

### Prerequisites
- Node.js 18+ and npm
- Firebase account and project
- Firebase CLI installed globally

### Installation

1. **Clone the repository**
   ```bash
   cd admin-dashboard
   ```

2. **Install dependencies**
   ```bash
   npm install
   ```

3. **Configure Firebase**
   - Create `src/lib/firebase.ts` with your Firebase config
   - Add `serviceAccountKey.json` to project root (for admin operations)

4. **Start development server**
   ```bash
   npm run dev
   ```
   Dashboard will be available at `http://localhost:5173`

### Build for Production

```bash
npm run build
```

Output will be in the `dist/` directory.

## 🚀 Deployment

### Firebase Hosting

1. **Build the project**
   ```bash
   npm run build
   ```

2. **Deploy to Firebase**
   ```bash
   firebase deploy --only hosting:wingzone-app
   ```

### Firebase Configuration

Ensure `firebase.json` has the correct hosting config:

```json
{
  "hosting": [
    {
      "target": "wingzone-app",
      "public": "admin-dashboard/dist",
      "ignore": ["firebase.json", "**/.*", "**/node_modules/**"],
      "rewrites": [
        {
          "source": "**",
          "destination": "/index.html"
        }
      ]
    }
  ]
}
```

## 🎯 Key Components

### UniformLoader
Animated loading component with GSAP bouncing dots:
```tsx
<UniformLoader message="Loading dashboard..." />
```

### Toast Notifications
SweetAlert2-based toast system:
```tsx
import { showToast } from '../utils/toast';

showToast('success', 'Order updated successfully!');
showToast('error', 'Failed to save changes');
```

### Confirmation Dialogs
```tsx
const result = await Swal.fire({
  title: 'Are you sure?',
  text: 'This action cannot be undone',
  icon: 'warning',
  showCancelButton: true,
  confirmButtonColor: '#ea580c',
  cancelButtonColor: '#6c757d',
  confirmButtonText: 'Yes, delete it!'
});

if (result.isConfirmed) {
  // Proceed with action
}
```

## 🎨 Design System

### Colors
- **Primary Orange**: `#ea580c` (hover: `#c2410c`)
- **Background**: `#1e293b` (sidebar), `#f9fafb` (hover states)
- **Text**: `#111827` (headings), `#6b7280` (labels), `#94a3b8` (inactive)

### Status Badge Colors
- **Success/Delivered**: Green `#d1fae5` / `#065f46`
- **Warning/Pending**: Yellow `#fef3c7` / `#92400e`
- **Danger/Cancelled**: Red `#fee2e2` / `#991b1b`
- **Info/Confirmed**: Blue `#dbeafe` / `#1e40af`

### Shadows
- **Default**: `0 1px 3px rgba(0, 0, 0, 0.08)`
- **Hover**: `0 4px 12px rgba(0, 0, 0, 0.12)`

## 📝 Scripts

```bash
npm run dev          # Start development server
npm run build        # Build for production
npm run preview      # Preview production build locally
npm run lint         # Run ESLint
```

## 🔐 Authentication

Admin login uses Firebase Authentication. Default credentials should be configured in Firebase Console.

## 📊 Real-time Updates

The dashboard uses Firebase Firestore's real-time listeners for live data updates:
- Order status changes
- New orders notification
- Revenue calculations
- Menu item updates

## 🖨️ Printing

Supports thermal receipt printing with configurable printer settings in Settings page.

## 🤝 Contributing

1. Follow the existing code style
2. Use TypeScript for type safety
3. Test thoroughly before committing
4. Update documentation as needed

## 📄 License

© 2025 WingZone. All rights reserved.

## 🐛 Known Issues

- Large bundle size warning (consider code splitting for optimization)
- Some Firebase imports are both static and dynamic (does not affect functionality)

## 📞 Support

For issues or questions, refer to the project documentation in the `docs/` directory.

---

**Built with ❤️ for WingZone Restaurant**
