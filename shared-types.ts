// WingZone Shared TypeScript Types
// Use across Admin Dashboard, Cloud Functions, and Android App (via JSON)

export type OrderStatus = 
  | 'pending' 
  | 'confirmed' 
  | 'preparing' 
  | 'ready' 
  | 'completed' 
  | 'cancelled';

export type PaymentStatus = 'pending' | 'paid' | 'refunded';

export type GroupOrderStatus = 
  | 'open' 
  | 'full' 
  | 'ordering' 
  | 'confirmed' 
  | 'preparing' 
  | 'ready' 
  | 'completed' 
  | 'cancelled';

export type UserRole = 'customer' | 'admin' | 'kitchen_staff';

export type MenuCategory = 'entrees' | 'alacarte' | 'beverages' | 'addons';

export type ReceiptType = 'order' | 'kitchen' | 'label';

export type PrinterStatus = 'pending' | 'printed' | 'failed';

// Menu Item
export interface MenuItem {
  id: string;
  name: string;
  description: string;
  category: MenuCategory;
  price: number;
  imageUrl?: string;
  isAvailable: boolean;
  requiresCustomization: boolean;
  options?: {
    flavors?: string[];
    dippingSauces?: string[];
    drinks?: string[];
  };
  stockCount?: number;
  createdAt: Date | string;
  updatedAt: Date | string;
}

// Fries Exchange Option (for side substitutions)
export interface FriesExchangeOption {
  name: string;
  regularPrice: number;
  jumboPrice: number | null;
  selectedSize?: 'regular' | 'jumbo'; // Track which size customer selected
  selectedFlavor?: string; // For Flavor Rub Fries (Blackened Voodoo or Lemon Pepper)
}

// Customization Options
export interface EntreeCustomization {
  flavor: string;
  dippingSauce: string;
  drink: string;
  boneType?: string; // 'Original' or 'Boneless'
  friesExchange?: FriesExchangeOption; // Track which side customer actually chose
  saladType?: string; // For items that include salad choice (Garden or Caesar)
}

// Cart Item
export interface CartItem {
  menuItemId: string;
  menuItemName: string;
  quantity: number;
  customization?: EntreeCustomization;
  price: number;
  subtotal: number;
}

// Group Member
export interface GroupMember {
  userId: string;
  name: string;
  email: string;
  isHost: boolean;
  cartItems: CartItem[];
  memberTotal: number;
  paymentStatus: PaymentStatus;
}

// Group Order
export interface GroupOrder {
  id: string;
  code: string;
  hostId: string;
  status: GroupOrderStatus;
  members: GroupMember[];
  deliveryAddress?: string;
  specialInstructions?: string;
  totalAmount: number;
  totalItems: number;
  tax: number;
  maxMembers: number;
  createdAt: Date | string;
  expiresAt: Date | string;
  confirmedAt?: Date | string;
  completedAt?: Date | string;
}

// Individual Order
export interface IndividualOrder {
  id: string;
  userId: string;
  userName: string;
  userEmail: string;
  orderNumber: string;
  items: CartItem[];
  subtotal: number;
  tax: number;
  total: number;
  status: OrderStatus;
  paymentStatus: PaymentStatus;
  orderType: 'individual' | 'group';
  groupOrderId?: string;
  seatNumber?: string;
  deliveryAddress?: string;
  specialInstructions?: string;
  createdAt: Date | string;
  updatedAt: Date | string;
  preparedAt?: Date | string;
  completedAt?: Date | string;
}

// User
export interface User {
  id: string;
  email: string;
  name: string;
  phoneNumber?: string;
  role: UserRole;
  wzBalance: number;
  wzPoints: number;
  profileImageUrl?: string;
  addresses?: Address[];
  orderHistory?: string[];
  createdAt: Date | string;
  lastLoginAt?: Date | string;
}

// Address
export interface Address {
  id: string;
  label: string;
  fullAddress: string;
  isDefault: boolean;
}

// Kitchen Queue Item
export interface KitchenQueueItem {
  id: string;
  orderId: string;
  orderNumber: string;
  items: Array<{
    menuItemName: string;
    quantity: number;
    customization?: EntreeCustomization;
    seatNumber?: string;
    customerName: string;
  }>;
  priority: 1 | 2 | 3 | 4 | 5;
  status: 'queued' | 'preparing' | 'ready' | 'served';
  assignedTo?: string;
  queuedAt: Date | string;
  startedAt?: Date | string;
  completedAt?: Date | string;
  estimatedTime: number; // minutes
}

// Receipt
export interface Receipt {
  id: string;
  orderId: string;
  orderNumber: string;
  type: ReceiptType;
  htmlContent: string;
  pdfUrl?: string;
  printedAt?: Date | string;
  printedBy?: string;
  printerStatus: PrinterStatus;
}

// Inventory
export interface InventoryItem {
  id: string;
  menuItemId: string;
  currentStock: number;
  minStock: number;
  maxStock: number;
  unit: string;
  lastRestocked?: Date | string;
  restockedBy?: string;
  lowStockAlert: boolean;
}

// Analytics
export interface DailyAnalytics {
  id: string;
  date: Date | string;
  totalOrders: number;
  totalRevenue: number;
  totalItems: number;
  groupOrderCount: number;
  individualOrderCount: number;
  averageOrderValue: number;
  topSellingItems: Array<{
    menuItemId: string;
    menuItemName: string;
    quantitySold: number;
    revenue: number;
  }>;
  peakHours: Record<number, number>; // hour: orderCount
  cancelledOrders: number;
  refundAmount: number;
}

// API Request/Response Types

export interface CreateMenuItemRequest {
  name: string;
  description: string;
  category: MenuCategory;
  price: number;
  imageFile?: File;
  requiresCustomization?: boolean;
  stockCount?: number;
}

export interface UpdateMenuItemRequest {
  id: string;
  name?: string;
  description?: string;
  price?: number;
  isAvailable?: boolean;
  imageFile?: File;
  stockCount?: number;
}

export interface CreateGroupOrderRequest {
  deliveryAddress?: string;
  specialInstructions?: string;
}

export interface JoinGroupOrderRequest {
  code: string;
}

export interface UpdateOrderStatusRequest {
  orderId: string;
  status: OrderStatus;
}

export interface GenerateReceiptRequest {
  orderId: string;
  type: ReceiptType;
}

export interface PrintLabelRequest {
  orderId: string;
  seatNumber: string;
  customerName: string;
}

// Admin Dashboard Specific Types

export interface DashboardStats {
  todayRevenue: number;
  todayOrders: number;
  activeOrders: number;
  todayCustomers: number;
  revenueChange: number; // percentage
  ordersChange: number; // percentage
}

export interface OrderFilters {
  status?: OrderStatus[];
  orderType?: ('individual' | 'group')[];
  dateFrom?: Date;
  dateTo?: Date;
  searchQuery?: string;
}

export interface MenuFilters {
  category?: MenuCategory[];
  isAvailable?: boolean;
  searchQuery?: string;
}

export interface InventoryAlert {
  menuItemId: string;
  menuItemName: string;
  currentStock: number;
  minStock: number;
  severity: 'low' | 'critical' | 'out_of_stock';
}

// Firebase Cloud Function Types

export interface CloudFunctionResponse {
  success: boolean;
  message?: string;
  data?: any;
  error?: string;
}

export interface SplitBillResult {
  groupOrderId: string;
  individualOrders: IndividualOrder[];
  totalAmount: number;
  memberPayments: Array<{
    userId: string;
    name: string;
    amount: number;
    paymentStatus: PaymentStatus;
  }>;
}

// Notification Types

export interface OrderNotification {
  type: 'order_confirmed' | 'order_preparing' | 'order_ready' | 'group_invite';
  title: string;
  body: string;
  data: {
    orderId?: string;
    groupOrderId?: string;
    orderNumber?: string;
  };
}

// Printer Configuration

export interface PrinterConfig {
  printerName: string;
  paperWidth: number; // mm
  paperHeight: number; // mm
  dpi: number;
  thermal: boolean;
}

// Export default configuration
export const DEFAULT_PRINTER_CONFIG: PrinterConfig = {
  printerName: 'Thermal Printer',
  paperWidth: 40,
  paperHeight: 30,
  dpi: 203,
  thermal: true,
};

// Validation helpers
export const isValidEmail = (email: string): boolean => {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
};

export const isValidPhone = (phone: string): boolean => {
  return /^[0-9]{10,15}$/.test(phone.replace(/[\s-]/g, ''));
};

export const formatCurrency = (amount: number): string => {
  return `RM ${amount.toFixed(2)}`;
};

export const formatOrderNumber = (date: Date, sequence: number): string => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const seq = String(sequence).padStart(3, '0');
  return `WZ${year}${month}${day}-${seq}`;
};
