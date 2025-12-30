import { useState, useEffect } from 'react';
import { authService } from './services/auth';
import { notificationService } from './services/notifications';
import { doc, getDoc, onSnapshot } from 'firebase/firestore';
import { db } from './lib/firebase';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import MenuPage from './pages/MenuPage';
import OrdersPage from './pages/OrdersPage';
import UsersPage from './pages/UsersPage';
import SeedMenuPage from './pages/SeedMenuPage';
import AvailabilityPage from './pages/AvailabilityPage';
import SettingsPage from './pages/SettingsPage';
import Sidebar from './components/Sidebar/Sidebar';
import Header from './components/Header/Header';
import ToastNotification from './components/ToastNotification/ToastNotification';

type Page = 'dashboard' | 'menu' | 'orders' | 'users' | 'seed' | 'availability' | 'settings';

interface ToastData {
  show: boolean;
  title: string;
  message: string;
  type: 'order' | 'success' | 'error' | 'info';
}

function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [currentPage, setCurrentPage] = useState<Page>('dashboard');
  const [darkMode, setDarkMode] = useState(false);
  const [toast, setToast] = useState<ToastData>({
    show: false,
    title: '',
    message: '',
    type: 'info'
  });

  useEffect(() => {
    setIsAuthenticated(authService.isAuthenticated());
  }, []);

  // Listen to admin preferences for dark mode and notification settings
  useEffect(() => {
    if (isAuthenticated) {
      const docRef = doc(db, 'appSettings', 'adminPreferences');
      const unsubscribe = onSnapshot(docRef, (snapshot) => {
        if (snapshot.exists()) {
          const data = snapshot.data();
          const isDark = data.darkMode || false;
          const soundEnabled = data.notificationSound !== false;
          
          setDarkMode(isDark);
          notificationService.setSoundEnabled(soundEnabled);
          
          // Apply dark mode to body
          document.body.setAttribute('data-theme', isDark ? 'dark' : 'light');
        }
      });
      
      return () => unsubscribe();
    }
  }, [isAuthenticated]);

  // Start order monitoring when authenticated
  useEffect(() => {
    if (isAuthenticated) {
      // Register toast callback
      notificationService.setToastCallback((title, message, type) => {
        setToast({ show: true, title, message, type });
      });

      // Request notification permission
      notificationService.requestNotificationPermission();
      
      // Start listening for new orders
      notificationService.startOrderMonitoring();
      
      return () => {
        // Clean up listener on unmount
        notificationService.stopOrderMonitoring();
      };
    }
  }, [isAuthenticated]);

  const handleCloseToast = () => {
    setToast(prev => ({ ...prev, show: false }));
  };

  const handleLogin = () => {
    setIsAuthenticated(true);
  };

  const handleLogout = () => {
    authService.logout();
    setIsAuthenticated(false);
  };

  const handleViewOrder = (orderId: string) => {
    // Navigate to orders page when viewing an order from notification
    setCurrentPage('orders');
    // You could also scroll to the specific order or highlight it
    console.log('Navigating to view order:', orderId);
  };

  if (!isAuthenticated) {
    return <LoginPage onLogin={handleLogin} />;
  }

  const renderPage = () => {
    switch (currentPage) {
      case 'menu':
        return <MenuPage />;
      case 'orders':
        return <OrdersPage />;
      case 'users':
        return <UsersPage />;
      case 'seed':
        return <SeedMenuPage />;
      case 'availability':
        return <AvailabilityPage />;
      case 'settings':
        return <SettingsPage />;
      default:
        return <DashboardPage onNavigate={(page) => setCurrentPage(page as Page)} />;
    }
  };

  return (
    <div style={{ display: 'flex', height: '100vh' }} data-theme={darkMode ? 'dark' : 'light'}>
      <Sidebar activePage={currentPage} onNavigate={(page) => setCurrentPage(page as Page)} />
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
        <Header 
          sidebarToggle={() => {}} 
          onLogout={handleLogout}
          onSettingsClick={() => setCurrentPage('settings')}
          onViewOrder={handleViewOrder}
        />
        <main style={{ flex: 1, padding: '30px', background: '#f4f5f7', overflowY: 'auto' }}>
          {renderPage()}
        </main>
      </div>
      
      {/* Toast Notification */}
      <ToastNotification
        show={toast.show}
        title={toast.title}
        message={toast.message}
        type={toast.type}
        onClose={handleCloseToast}
        duration={5000}
      />
    </div>
  );
}

export default App;
