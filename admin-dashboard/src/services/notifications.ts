import { collection, addDoc, Timestamp, query, where, onSnapshot, orderBy, limit, doc, getDoc } from 'firebase/firestore';
import { db } from '../lib/firebase';
import { printService } from './printService';
import type { GroupOrderForPrint } from './printService';

export interface NotificationData {
  type: 'order' | 'system' | 'info';
  title: string;
  message: string;
  orderId?: string;
  orderType?: 'individual' | 'group';
  orderTotal?: number;
  customerName?: string;
}

export interface CustomSound {
  id: string;
  name: string;
  url: string;
  uploadedAt: string;
}

export interface NotificationSoundSettings {
  enabled: boolean;
  soundType: string; // 'default', 'urgent-alert', or custom sound ID
  customSounds: CustomSound[]; // Array of custom sounds
  volume: number; // 0-100
}

class NotificationService {
  private notificationsRef = collection(db, 'notifications');
  private orderListener: (() => void) | null = null;
  private lastOrderTimestamp: Date | null = null;
  private audioContext: AudioContext | null = null;
  private hasPlayedSound = false;
  private toastCallback: ((title: string, message: string, type: 'order') => void) | null = null;
  private soundSettings: NotificationSoundSettings = {
    enabled: true,
    soundType: 'default',
    customSounds: [],
    volume: 70
  };
  private audioElement: HTMLAudioElement | null = null;

  constructor() {
    // Initialize audio context for fallback notification sounds
    if (typeof window !== 'undefined') {
      this.audioContext = new (window.AudioContext || (window as any).webkitAudioContext)();
      this.audioElement = new Audio();
    }
    this.loadSoundPreference();
  }

  // Load sound preference from Firebase
  async loadSoundPreference() {
    try {
      const { doc, getDoc } = await import('firebase/firestore');
      const docRef = doc(db, 'appSettings', 'notificationSound');
      const docSnap = await getDoc(docRef);
      if (docSnap.exists()) {
        this.soundSettings = {
          enabled: docSnap.data().enabled !== false,
          soundType: docSnap.data().soundType || 'default',
          customSounds: docSnap.data().customSounds || [],
          volume: docSnap.data().volume ?? 70
        };
        console.log('Loaded sound settings:', this.soundSettings);
      }
    } catch (error) {
      console.error('Error loading sound preference:', error);
    }
  }

  // Update sound settings
  async updateSoundSettings(settings: Partial<NotificationSoundSettings>) {
    this.soundSettings = { ...this.soundSettings, ...settings };
    
    try {
      const { doc, setDoc } = await import('firebase/firestore');
      const docRef = doc(db, 'appSettings', 'notificationSound');
      await setDoc(docRef, this.soundSettings, { merge: true });
      console.log('Sound settings saved:', this.soundSettings);
    } catch (error) {
      console.error('Error saving sound settings:', error);
    }
  }

  // Get current sound settings
  getSoundSettings(): NotificationSoundSettings {
    return { ...this.soundSettings };
  }

  // Update sound preference (legacy method)
  setSoundEnabled(enabled: boolean) {
    this.updateSoundSettings({ enabled });
  }

  // Register toast callback
  setToastCallback(callback: (title: string, message: string, type: 'order') => void) {
    this.toastCallback = callback;
  }

  // Play notification sound with selected type
  private playNotificationSound() {
    if (!this.soundSettings.enabled) return;

    const soundMap: Record<string, string> = {
      'urgent-alert': '/sounds/Urgent Alert.mp3'
    };

    // Check if it's a preset sound
    if (soundMap[this.soundSettings.soundType]) {
      this.playAudioFile(soundMap[this.soundSettings.soundType]);
    }
    // Check if it's a custom sound
    else if (this.soundSettings.soundType !== 'default') {
      const customSound = this.soundSettings.customSounds.find(s => s.id === this.soundSettings.soundType);
      if (customSound) {
        this.playAudioFile(customSound.url);
      } else {
        this.playDefaultBeep();
      }
    } 
    // Fallback to default beep
    else {
      this.playDefaultBeep();
    }
  }

  // Play audio file
  private playAudioFile(url: string) {
    if (!this.audioElement) return;

    try {
      this.audioElement.src = url;
      this.audioElement.volume = this.soundSettings.volume / 100;
      this.audioElement.play().catch(error => {
        console.error('Error playing audio file:', error);
        // Fallback to default beep if file fails
        this.playDefaultBeep();
      });
      console.log('Playing audio file:', url);
    } catch (error) {
      console.error('Error setting up audio:', error);
      this.playDefaultBeep();
    }
  }

  // Play default beep sound (original implementation)
  private playDefaultBeep() {
    if (!this.audioContext) return;

    try {
      const volume = this.soundSettings.volume / 100;
      
      // Create a simple beep sound
      const oscillator = this.audioContext.createOscillator();
      const gainNode = this.audioContext.createGain();

      oscillator.connect(gainNode);
      gainNode.connect(this.audioContext.destination);

      oscillator.frequency.value = 800; // Frequency in Hz
      oscillator.type = 'sine';

      gainNode.gain.setValueAtTime(volume * 0.3, this.audioContext.currentTime);
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

      gainNode2.gain.setValueAtTime(volume * 0.3, this.audioContext.currentTime + 0.2);
      gainNode2.gain.exponentialRampToValueAtTime(0.01, this.audioContext.currentTime + 0.7);

      oscillator2.start(this.audioContext.currentTime + 0.2);
      oscillator2.stop(this.audioContext.currentTime + 0.7);

      console.log('Notification sound played (default beep)');
    } catch (error) {
      console.error('Error playing notification sound:', error);
    }
  }

  // Preview sound (public method for settings page)
  previewSound(soundType: string, customUrl?: string) {
    const originalSettings = { ...this.soundSettings };
    this.soundSettings.soundType = soundType as any;
    if (customUrl) this.soundSettings.customSoundUrl = customUrl;
    
    this.playNotificationSound();
    
    // Restore original settings after preview
    setTimeout(() => {
      this.soundSettings = originalSettings;
    }, 100);
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
            const total = orderData.total || orderData.groupTotal || 0;
            
            // Calculate item count based on order type
            let itemCount = 0;
            if (isGroupOrder && orderData.members) {
              // For group orders, count items from all members
              itemCount = orderData.members.reduce((sum: number, member: any) => {
                return sum + (member.cartItems?.reduce((cartSum: number, item: any) => {
                  return cartSum + (item.quantity || 1);
                }, 0) || 0);
              }, 0);
            } else {
              // For individual orders, count from items array
              itemCount = orderData.items?.reduce((sum: number, item: any) => sum + (item.quantity || 1), 0) || 0;
            }

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

            // Auto-print receipts for ALL orders (both group and individual)
            await this.autoPrintReceipt(change.doc.id, orderData, isGroupOrder);

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

  // Auto-print receipts for both individual and group orders
  private async autoPrintReceipt(orderId: string, orderData: any, isGroupOrder: boolean) {
    try {
      // Check if auto-print is enabled in settings
      const settingsRef = doc(db, 'appSettings', 'adminPreferences');
      const settingsSnap = await getDoc(settingsRef);
      const autoPrintEnabled = settingsSnap.exists() ? settingsSnap.data().autoPrintReceipts !== false : false;
      const printerName = settingsSnap.exists() ? settingsSnap.data().printerName : '';
      const useThermalPrinter = settingsSnap.exists() ? settingsSnap.data().useThermalPrinter !== false : true;

      if (!autoPrintEnabled) {
        console.log('Auto-print is disabled in settings');
        return;
      }

      console.log(`Auto-print enabled for ${isGroupOrder ? 'group' : 'individual'} order. Printer: ${printerName || 'Auto'}, Thermal: ${useThermalPrinter}`);

      // Use QZ Tray thermal printer if enabled
      if (useThermalPrinter) {
        try {
          const { default: thermalPrinter } = await import('./thermalPrinter');
          
          // Format order for thermal printing
          const order = {
            id: orderId,
            ...orderData,
            items: orderData.items || [],
            total: orderData.total || 0,
            createdAt: orderData.createdAt || Timestamp.now()
          };

          console.log(`Attempting QZ Tray print for order ${orderId.substring(0, 8)}...`);
          const success = await thermalPrinter.printReceipt(order, {
            printerName: printerName || undefined,
            paperWidth: 80
          });

          if (success) {
            console.log(`✓ Master receipt printed via QZ Tray for order ${orderId.substring(0, 8)}`);
            
            // For group orders, also print individual member receipts
            if (isGroupOrder && orderData.members && orderData.members.length > 0) {
              console.log(`[QZ Tray] Printing ${orderData.members.length} individual member receipts...`);
              
              for (let i = 0; i < orderData.members.length; i++) {
                const member = orderData.members[i];
                const memberData = {
                  name: member.name,
                  cartItems: member.cartItems || [],
                  index: i,
                  total: member.totalAmount || 0
                };
                
                console.log(`[QZ Tray] Printing receipt for member: ${member.name}`);
                const memberSuccess = await thermalPrinter.printReceipt(order, {
                  printerName: printerName || undefined,
                  paperWidth: 80
                }, memberData);
                
                if (memberSuccess) {
                  console.log(`[QZ Tray] ✓ Member receipt printed: ${member.name}`);
                } else {
                  console.error(`[QZ Tray] ✗ Failed to print receipt for: ${member.name}`);
                }
                
                // Small delay between prints to avoid overwhelming the printer
                if (i < orderData.members.length - 1) {
                  await new Promise(resolve => setTimeout(resolve, 1000));
                }
              }
              
              console.log('[QZ Tray] ✓ All receipts printed');
            }
            
            return;
          } else {
            console.log('✗ QZ Tray print failed, falling back to browser print');
          }
        } catch (error) {
          console.error('✗ QZ Tray error:', error);
          console.log('Falling back to browser print service');
        }
      }

      // Fallback to browser print service (only for group orders)
      if (isGroupOrder) {
        const groupOrderForPrint: GroupOrderForPrint = {
          id: orderId,
          groupName: orderData.groupName || 'Group Order',
          hostUserName: orderData.hostUserName || 'Host',
          participants: (orderData.members || orderData.participants || []).map((p: any) => ({
            userId: p.userId,
            userName: p.userName || p.name,
            items: p.cartItems || p.items || [],
            total: p.total || (p.cartItems || []).reduce((sum: number, item: any) => sum + (item.subtotal || 0), 0)
          })),
          orderDate: orderData.createdAt?.toDate() || new Date(),
          total: orderData.total || (orderData.members || []).reduce((sum: number, m: any) => 
            sum + (m.cartItems || []).reduce((itemSum: number, item: any) => 
              itemSum + (item.subtotal || 0), 0
            ), 0
          ),
          orderType: orderData.orderType,
          location: orderData.location || orderData.selectedLocation,
          paymentMethod: orderData.paymentMethod
        };

        console.log(`Auto-printing 1 MASTER + ${groupOrderForPrint.participants.length} MEMBER receipts for group order ${orderId.substring(0, 8)}`);
        await printService.printGroupOrderReceipts(groupOrderForPrint, undefined, printerName);
        console.log(`✓ Auto-printed all receipts for group order ${orderId.substring(0, 8)}`);
      } else {
        console.log(`Individual order ${orderId.substring(0, 8)}: Browser auto-print not supported for individual orders. Use QZ Tray.`);
      }
      
    } catch (error) {
      console.error('Error auto-printing receipt:', error);
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
