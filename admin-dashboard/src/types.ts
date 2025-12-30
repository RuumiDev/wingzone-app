// Shared types matching Kotlin models
export enum MenuCategory {
  ENTREE = 'entree',
  SIDE = 'side',
  BEVERAGE = 'beverage',
  DESSERT = 'dessert'
}

export enum Flavor {
  BUFFALO = 'Buffalo',
  BBQ = 'BBQ',
  HONEY_GARLIC = 'Honey Garlic',
  LEMON_PEPPER = 'Lemon Pepper',
  TERIYAKI = 'Teriyaki',
  PLAIN = 'Plain',
  SPICY_KOREAN = 'Spicy Korean',
  GARLIC_PARMESAN = 'Garlic Parmesan'
}

export enum OrderStatus {
  PENDING = 'pending',
  CONFIRMED = 'confirmed',
  PREPARING = 'preparing',
  READY = 'ready',
  DELIVERED = 'delivered',
  CANCELLED = 'cancelled'
}

export interface MenuItem {
  id: string;
  name: string;
  description: string;
  category: string; // Changed from enum to string to match Firestore
  price: number;
  imageUrl?: string; // Optional, can be uploaded
  isAvailable: boolean; // Changed from 'available' to match Firestore
  requiresCustomization: boolean; // For items that need flavor/size selection
  // Optional fields for customization options
  flavors?: string[]; // Available flavors for this item
  sizes?: Array<{ name: string; priceModifier: number }>; // Size options
  addOns?: Array<{ name: string; price: number }>; // Add-on options
  customizationOptions?: {
    requiresFlavor?: boolean;
    requiresBeverage?: boolean;
    requiresDippingSauce?: boolean;
    allowFriesExchange?: boolean;
    allowedFlavors?: number;
    beverages?: string[];
    dippingSauces?: string[];
    friesExchanges?: Array<{ name: string; regularPrice: number; jumboPrice: number | null }>;
    wingType?: string[];
    saladType?: string[];
    dressingType?: string[];
  };
  createdAt?: Date;
  updatedAt?: Date;
}

export interface CartItem {
  menuItemId: string;
  menuItemName: string;
  category: MenuCategory;
  quantity: number;
  basePrice: number;
  customization: {
    flavor?: Flavor;
    dippingSauce?: string;
    drink?: string;
  };
  subtotal: number;
}

export interface GroupOrder {
  id: string;
  code: string;
  hostUserId: string;
  hostUserName: string;
  members: GroupMember[];
  status: string;
  deliveryAddress: string;
  deliveryInstructions: string;
  totalAmount: number;
  createdAt: Date;
  expiresAt: Date;
}

export interface GroupMember {
  userId: string;
  name: string;
  email: string;
  cartItems: CartItem[];
  memberTotal: number;
  paymentStatus: string;
}

export interface User {
  id: string;
  email: string;
  name: string;
  phoneNumber?: string;
  role: 'customer' | 'admin' | 'kitchen';
  createdAt: Date;
}

export interface DashboardStats {
  todayOrders: number;
  todayRevenue: number;
  pendingOrders: number;
  activeGroupOrders: number;
  recentOrders: Array<{
    id: string;
    items: string;
    amount: number;
    status: string;
    time: string;
  }>;
}
