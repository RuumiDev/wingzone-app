import { collection, addDoc, Timestamp, query, where, onSnapshot, Unsubscribe, orderBy, limit, doc, getDoc } from 'firebase/firestore';
import { db } from '../lib/firebase';
import { printService, GroupOrderForPrint } from './printService';

export interface NotificationData {
  type: 'order' | 'system' | 'info';
  title: string;
  message: string;
  orderId?: string;
  orderType?: 'individual' | 'group';
  orderTotal?: number;
  customerName?: string;
}

class NotificationService {
  private notificationsRef = collection(db, 'notifications');
  private orderListener: Unsubscribe | null = null;
  private lastOrderTimestamp: Date | null = null;
  private audioContext: AudioContext | null = null;
  private hasPlayedSound = false;
  private toastCallback: ((title: string, message: string, type: 'order') => void) | null = null;
  private soundEnabled: boolean = true;

  constructor() {
    // Initialize audio context for notification sounds
    if (typeof window !== 'undefined') {
      this.audioContext = new (window.AudioContext || (window as any).webkitAudioContext)();
    }
    this.loadSoundPreference();
  }

  // Load sound preference from Firebase
  private async loadSoundPreference() {
    try {
      const { doc, getDoc } = await import('firebase/firestore');
      const docRef = doc(db, 'appSettings', 'adminPreferences');
      const docSnap = await getDoc(docRef);
      if (docSnap.exists()) {
        this.soundEnabled = docSnap.data().notificationSound !== false;
      }
    } catch (error) {
      console.error('Error loading sound preference:', error);
    }
  }

  // Update sound preference
  setSoundEnabled(enabled: boolean) {
    this.soundEnabled = enabled;
  }

  // Register toast callback
  setToastCallback(callback: (title: string, message: string, type: 'order') => void) {
    this.toastCallback = callback;
  }

  // Play notification sound
  private playNotificationSound() {
    if (!this.audioContext || !this.soundEnabled) return;

    try {
      // Create a simple beep sound
      const oscillator = this.audioContext.createOscillator();
      const gainNode = this.audioContext.createGain();

      oscillator.connect(gainNode);
      gainNode.connect(this.audioContext.destination);

      oscillator.frequency.value = 800; // Frequency in Hz
      oscillator.type = 'sine';

      gainNode.gain.setValueAtTime(0.3, this.audioContext.currentTime);
      gainNode.gain.exponentialRampToValueAtTime(0.01, this.audioContext.currentTime + 0.5);

      oscillator.start(this.audioContext.currentTime);
      oscillator.stop(this.audioContext.currentTime + 0.5);

      // Second beep
      const oscillator2 = this.audioContext.createOscillator();
      const gainNode2 = this.audioContext.createGain();

      oscillator2.connect(gainNode2);
      gainNode2.connect(this.audioContext.destination);

      oscillator2.frequency.value = 1000;
      oscillator2.type = 'sine';

      gainNode2.gain.setValueAtTime(0.3, this.audioContext.currentTime + 0.2);
      gainNode2.gain.exponentialRampToValueAtTime(0.01, this.audioContext.currentTime + 0.7);

      oscillator2.start(this.audioContext.currentTime + 0.2);
      oscillator2.stop(this.audioContext.currentTime + 0.7);

      console.log('Notification sound played');
    } catch (error) {
      console.error('Error playing notification sound:', error);
    }
  }

  // Show browser notification
  private async showBrowserNotification(title: string, body: string) {
    if ('Notification' in window && Notification.permission === 'granted') {
      try {
        new Notification(title, {
          body,
          icon: '/wingzone-logo.png',
          badge: '/wingzone-logo.png',
          tag: 'order-notification',
          requireInteraction: true
        });
      } catch (error) {
        console.error('Error showing browser notification:', error);
      }
    }
  }

  // Request notification permission
  async requestNotificationPermission() {
    if ('Notification' in window && Notification.permission === 'default') {
      const permission = await Notification.requestPermission();
      return permission === 'granted';
    }
    return Notification.permission === 'granted';
  }

  async addNotification(data: NotificationData) {
    try {
      await addDoc(this.notificationsRef, {
        ...data,
        createdAt: Timestamp.now(),
        read: false
      });
    } catch (error) {
      console.error('Error adding notification:', error);
      throw error;
    }
  }

  // Start listening for new orders
  startOrderMonitoring() {
    if (this.orderListener) {
      console.log('Order monitoring already active');
      return;
    }

    // Set initial timestamp to avoid notifying for old orders
    this.lastOrderTimestamp = new Date();
    this.hasPlayedSound = false;

    const ordersRef = collection(db, 'orders');
    const q = query(ordersRef, orderBy('createdAt', 'desc'), limit(20));

    this.orderListener = onSnapshot(q, (snapshot) => {
      snapshot.docChanges().forEach(async (change) => {
        if (change.type === 'added') {
          const orderData = change.doc.data();
          const orderCreatedAt = orderData.createdAt?.toDate();

          // Only notify for orders created after monitoring started
          if (orderCreatedAt && this.lastOrderTimestamp && orderCreatedAt > this.lastOrderTimestamp) {
            const isGroupOrder = orderData.isGroupOrder === true;
            const orderId = change.doc.id.substring(0, 8).toUpperCase();
            const customerName = orderData.userName || orderData.hostUserName || 'Customer';
            const total = orderData.total || 0;
            const itemCount = orderData.items?.length || 0;

            const title = isGroupOrder ? '🍗 New Group Order!' : '🍗 New Order Received!';
            const message = `${customerName} • ${itemCount} items • RM ${total.toFixed(2)}`;

            // Add to Firebase notifications
            await this.addNotification({
              type: 'order',
              title,
              message,
              orderId: change.doc.id,
              orderType: isGroupOrder ? 'group' : 'individual',
              orderTotal: total,
              customerName
            });

            // Play sound
            this.playNotificationSound();

            // Show browser notification
            await this.showBrowserNotification(title, message);

            // Show toast notification if callback is registered
            if (this.toastCallback) {
              this.toastCallback(title, message, 'order');
            }

            // Auto-print receipts for group orders
            if (isGroupOrder) {
              await this.autoPrintGroupOrderReceipts(change.doc.id, orderData);
            }

            console.log(`New order notification: ${orderId}`);
          }
        }
      });
    });

    console.log('Order monitoring started');
  }

  // Stop listening for new orders
  stopOrderMonitoring() {
    if (this.orderListener) {
      this.orderListener();
      this.orderListener = null;
      console.log('Order monitoring stopped');
    }
  }

  // Auto-print group order receipts
  private async autoPrintGroupOrderReceipts(orderId: string, orderData: any) {
    try {
      // Check if auto-print is enabled in settings
      const settingsRef = doc(db, 'appSettings', 'adminPreferences');
      const settingsSnap = await getDoc(settingsRef);
      const autoPrintEnabled = settingsSnap.exists() ? settingsSnap.data().autoPrintReceipts !== false : true;

      if (!autoPrintEnabled) {
        console.log('Auto-print is disabled in settings');
        return;
      }

      // Format order data for printing
      const groupOrderForPrint: GroupOrderForPrint = {
        id: orderId,
        groupName: orderData.groupName || 'Group Order',
        hostUserName: orderData.hostUserName || 'Host',
        participants: (orderData.participants || []).map((p: any) => ({
          userId: p.userId,
          userName: p.userName,
          items: p.items || [],
          total: p.total || 0
        })),
        orderDate: orderData.createdAt?.toDate() || new Date(),
        total: orderData.total || 0
      };

      // Print receipts for all participants
      await printService.printGroupOrderReceipts(groupOrderForPrint);
      
      console.log(`Auto-printed receipts for group order ${orderId}`);
    } catch (error) {
      console.error('Error auto-printing group order receipts:', error);
    }
  }

  async addSystemNotification(title: string, message: string) {
    await this.addNotification({
      type: 'system',
      title,
      message
    });
  }
}

export const notificationService = new NotificationService();
