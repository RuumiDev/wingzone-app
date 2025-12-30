import {
  collection,
  doc,
  getDocs,
  getDoc,
  addDoc,
  updateDoc,
  deleteDoc,
  query,
  where,
  orderBy,
  limit,
  Timestamp,
  onSnapshot,
} from 'firebase/firestore';
import { db } from '../lib/firebase';
import type { MenuItem, GroupOrder, DashboardStats } from '../types';

// Menu Items Service
export const menuService = {
  // Get all menu items
  async getAllMenuItems(): Promise<MenuItem[]> {
    try {
      const menuRef = collection(db, 'menuItems');
      const snapshot = await getDocs(menuRef);
      return snapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data(),
      })) as MenuItem[];
    } catch (error) {
      console.error('Error fetching menu items:', error);
      throw error;
    }
  },

  // Get single menu item
  async getMenuItem(id: string): Promise<MenuItem | null> {
    try {
      const docRef = doc(db, 'menuItems', id);
      const docSnap = await getDoc(docRef);
      if (docSnap.exists()) {
        return { id: docSnap.id, ...docSnap.data() } as MenuItem;
      }
      return null;
    } catch (error) {
      console.error('Error fetching menu item:', error);
      throw error;
    }
  },

  // Add menu item
  async addMenuItem(item: Omit<MenuItem, 'id'>): Promise<string> {
    try {
      const menuRef = collection(db, 'menuItems');
      const docRef = await addDoc(menuRef, {
        ...item,
        createdAt: Timestamp.now(),
        updatedAt: Timestamp.now(),
      });
      return docRef.id;
    } catch (error) {
      console.error('Error adding menu item:', error);
      throw error;
    }
  },

  // Update menu item
  async updateMenuItem(id: string, updates: Partial<MenuItem>): Promise<void> {
    try {
      const docRef = doc(db, 'menuItems', id);
      await updateDoc(docRef, {
        ...updates,
        updatedAt: Timestamp.now(),
      });
    } catch (error) {
      console.error('Error updating menu item:', error);
      throw error;
    }
  },

  // Delete menu item
  async deleteMenuItem(id: string): Promise<void> {
    try {
      await deleteDoc(doc(db, 'menuItems', id));
    } catch (error) {
      console.error('Error deleting menu item:', error);
      throw error;
    }
  },

  // Listen to menu items changes (real-time)
  onMenuItemsChange(callback: (items: MenuItem[]) => void) {
    const menuRef = collection(db, 'menuItems');
    return onSnapshot(menuRef, (snapshot) => {
      const items = snapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data(),
      })) as MenuItem[];
      callback(items);
    });
  },
};

// Group Orders Service (from orders collection with isGroupOrder=true)
export const ordersService = {
  // Get all group orders
  async getAllOrders(): Promise<GroupOrder[]> {
    try {
      const ordersRef = collection(db, 'orders');
      const q = query(ordersRef, where('isGroupOrder', '==', true), orderBy('createdAt', 'desc'));
      const snapshot = await getDocs(q);
      return snapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data(),
      })) as GroupOrder[];
    } catch (error) {
      console.error('Error fetching orders:', error);
      throw error;
    }
  },

  // Get active orders
  async getActiveOrders(): Promise<GroupOrder[]> {
    try {
      const ordersRef = collection(db, 'orders');
      const q = query(
        ordersRef,
        where('isGroupOrder', '==', true),
        where('status', 'in', ['pending', 'confirmed', 'preparing', 'ready']),
        orderBy('createdAt', 'desc')
      );
      const snapshot = await getDocs(q);
      return snapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data(),
      })) as GroupOrder[];
    } catch (error) {
      console.error('Error fetching active orders:', error);
      throw error;
    }
  },

  // Get single order
  async getOrder(id: string): Promise<GroupOrder | null> {
    try {
      const docRef = doc(db, 'orders', id);
      const docSnap = await getDoc(docRef);
      if (docSnap.exists()) {
        return { id: docSnap.id, ...docSnap.data() } as GroupOrder;
      }
      return null;
    } catch (error) {
      console.error('Error fetching order:', error);
      throw error;
    }
  },

  // Update order status
  async updateOrderStatus(
    id: string,
    status: 'pending' | 'confirmed' | 'preparing' | 'ready' | 'delivered' | 'cancelled'
  ): Promise<void> {
    try {
      const docRef = doc(db, 'orders', id);
      await updateDoc(docRef, {
        status,
        updatedAt: Timestamp.now(),
      });
      
      // Auto-progress to next status after delays
      this.scheduleAutoProgression(id, status);
    } catch (error) {
      console.error('Error updating order status:', error);
      throw error;
    }
  },

  // Schedule automatic status progression (same as individual orders)
  scheduleAutoProgression(orderId: string, currentStatus: string) {
    if (currentStatus === 'pending') {
      setTimeout(() => {
        this.updateOrderStatus(orderId, 'confirmed').catch(console.error);
      }, 1000);
    } else if (currentStatus === 'confirmed') {
      setTimeout(() => {
        this.updateOrderStatus(orderId, 'preparing').catch(console.error);
      }, 30000);
    } else if (currentStatus === 'ready') {
      setTimeout(() => {
        this.updateOrderStatus(orderId, 'delivered').catch(console.error);
      }, 300000);
    }
  },

  // Cancel order
  async cancelOrder(id: string): Promise<void> {
    try {
      await this.updateOrderStatus(id, 'cancelled');
    } catch (error) {
      console.error('Error cancelling order:', error);
      throw error;
    }
  },
  // Delete order (for development)
  async deleteOrder(id: string): Promise<void> {
    try {
      const docRef = doc(db, 'orders', id);
      await deleteDoc(docRef);
    } catch (error) {
      console.error('Error deleting order:', error);
      throw error;
    }
  },

  // Delete all group orders (for development)
  async deleteAllOrders(): Promise<void> {
    try {
      const ordersRef = collection(db, 'orders');
      const q = query(ordersRef, where('isGroupOrder', '==', true));
      const snapshot = await getDocs(q);
      
      const deletePromises = snapshot.docs.map(doc => deleteDoc(doc.ref));
      await Promise.all(deletePromises);
    } catch (error) {
      console.error('Error deleting all orders:', error);
      throw error;
    }
  },
  // Listen to orders changes (real-time)
  onOrdersChange(callback: (orders: GroupOrder[]) => void) {
    const ordersRef = collection(db, 'orders');
    // Removed orderBy to avoid composite index requirement with where clause
    const q = query(ordersRef, where('isGroupOrder', '==', true));
    return onSnapshot(q, (snapshot) => {
      const orders = snapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data(),
      })) as GroupOrder[];      // Sort by createdAt descending on client side
      orders.sort((a: any, b: any) => {
        const aTime = a.createdAt?.toMillis?.() || 0;
        const bTime = b.createdAt?.toMillis?.() || 0;
        return bTime - aTime;
      });      callback(orders);
    });
  },
};

// Individual Orders Service (from orders collection with isGroupOrder=false)
export const individualOrdersService = {
  // Listen to individual orders changes (real-time)
  onOrdersChange(callback: (orders: any[]) => void) {
    const ordersRef = collection(db, 'orders');
    // Removed orderBy to avoid composite index requirement with where clause
    const q = query(ordersRef, where('isGroupOrder', '==', false));
    return onSnapshot(q, (snapshot) => {
      const orders = snapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data(),
      }));
      // Sort by createdAt descending on client side
      orders.sort((a: any, b: any) => {
        const aTime = a.createdAt?.toMillis?.() || 0;
        const bTime = b.createdAt?.toMillis?.() || 0;
        return bTime - aTime;
      });
      callback(orders);
    });
  },
  
  // Update order status
  async updateOrderStatus(
    id: string,
    status: 'pending' | 'confirmed' | 'preparing' | 'ready' | 'delivered' | 'cancelled'
  ): Promise<void> {
    try {
      const docRef = doc(db, 'orders', id);
      await updateDoc(docRef, {
        status,
        updatedAt: Timestamp.now(),
      });
      
      // Auto-progress to next status after delays (except for 'ready' which requires manual confirmation)
      this.scheduleAutoProgression(id, status);
    } catch (error) {
      console.error('Error updating order status:', error);
      throw error;
    }
  },

  // Schedule automatic status progression
  scheduleAutoProgression(orderId: string, currentStatus: string) {
    // Auto-progression logic:
    // pending -> confirmed (immediately)
    // confirmed -> preparing (after 30 seconds)
    // preparing -> (stays until manually marked as ready)
    // ready -> delivered (after 5 minutes)
    
    if (currentStatus === 'pending') {
      // Auto-accept: pending → confirmed immediately
      setTimeout(() => {
        this.updateOrderStatus(orderId, 'confirmed').catch(console.error);
      }, 1000); // 1 second delay
    } else if (currentStatus === 'confirmed') {
      // Auto-progress: confirmed → preparing after 30 seconds
      setTimeout(() => {
        this.updateOrderStatus(orderId, 'preparing').catch(console.error);
      }, 30000); // 30 seconds
    } else if (currentStatus === 'ready') {
      // Auto-complete: ready → delivered after 5 minutes
      setTimeout(() => {
        this.updateOrderStatus(orderId, 'delivered').catch(console.error);
      }, 300000); // 5 minutes
    }
    // 'preparing' stays until manually marked as 'ready'
    // 'delivered' and 'cancelled' are final states
  },

  // Delete order (for development)
  async deleteOrder(id: string): Promise<void> {
    try {
      const docRef = doc(db, 'orders', id);
      await deleteDoc(docRef);
    } catch (error) {
      console.error('Error deleting order:', error);
      throw error;
    }
  },

  // Delete all orders (for development)
  async deleteAllOrders(): Promise<void> {
    try {
      const ordersRef = collection(db, 'orders');
      const q = query(ordersRef, where('isGroupOrder', '==', false));
      const snapshot = await getDocs(q);
      
      const deletePromises = snapshot.docs.map(doc => deleteDoc(doc.ref));
      await Promise.all(deletePromises);
    } catch (error) {
      console.error('Error deleting all orders:', error);
      throw error;
    }
  },
};

//Dashboard Stats Service
export const dashboardService = {
  // Get dashboard statistics
  async getDashboardStats(): Promise<DashboardStats> {
    try {
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      const todayTimestamp = Timestamp.fromDate(today);

      // Get all orders from 'orders' collection (includes both individual and group orders)
      const ordersRef = collection(db, 'orders');

      // Get today's orders
      const todayQuery = query(
        ordersRef,
        where('createdAt', '>=', todayTimestamp)
      );
      const todaySnapshot = await getDocs(todayQuery);

      // Get pending orders
      const pendingQuery = query(
        ordersRef,
        where('status', '==', 'pending')
      );
      const pendingSnapshot = await getDocs(pendingQuery);

      // Get active group orders (isGroupOrder = true)
      const activeQuery = query(
        ordersRef,
        where('isGroupOrder', '==', true),
        where('status', 'in', ['confirmed', 'preparing', 'ready'])
      );
      const activeSnapshot = await getDocs(activeQuery);

      // Calculate today's revenue
      let todayRevenue = 0;
      todaySnapshot.docs.forEach(doc => {
        const order = doc.data() as any;
        todayRevenue += order.total || order.totalAmount || 0;
      });

      // Get recent orders (last 10) - both individual and group
      const recentQuery = query(
        ordersRef,
        orderBy('createdAt', 'desc'),
        limit(10)
      );
      const recentSnapshot = await getDocs(recentQuery);
      const recentOrders = recentSnapshot.docs.map(doc => {
        const data = doc.data();
        const order = data as any;
        
        // Handle Firebase Timestamp
        let orderDate = new Date();
        if (order.createdAt) {
          if (typeof order.createdAt.toDate === 'function') {
            orderDate = order.createdAt.toDate();
          } else if (order.createdAt instanceof Date) {
            orderDate = order.createdAt;
          }
        }
        const timeAgo = this.getTimeAgo(orderDate);
        
        // Calculate items count
        let itemsCount = 0;
        if (order.isGroupOrder && order.members) {
          itemsCount = order.members.reduce((sum: number, m: any) => sum + (m.cartItems?.length || 0), 0);
        } else if (order.items) {
          itemsCount = order.items.length;
        }
        
        return {
          id: doc.id.substring(0, 8).toUpperCase(),
          items: `${itemsCount} items`,
          amount: order.total || order.totalAmount || 0,
          status: order.status || 'pending',
          time: timeAgo
        };
      });

      return {
        todayOrders: todaySnapshot.size,
        todayRevenue,
        pendingOrders: pendingSnapshot.size,
        activeGroupOrders: activeSnapshot.size,
        recentOrders
      };
    } catch (error) {
      console.error('Error fetching dashboard stats:', error);
      throw error;
    }
  },

  // Helper function to format time ago
  getTimeAgo(date: Date): string {
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    return `${diffDays}d ago`;
  },

  // Listen to stats changes (real-time)
  onStatsChange(callback: (stats: DashboardStats) => void) {
    const ordersRef = collection(db, 'orders');
    return onSnapshot(ordersRef, async () => {
      const stats = await this.getDashboardStats();
      callback(stats);
    });
  },
};

