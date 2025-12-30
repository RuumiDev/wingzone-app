// Mock data service (will be replaced with Firebase/Firestore)
import { MenuItem, MenuCategory, GroupOrder, DashboardStats, OrderStatus } from '../types';

class DataService {
  // Mock menu items
  private menuItems: MenuItem[] = [
    {
      id: '1',
      name: '10 Wings',
      description: 'Ten delicious wings with your choice of flavor',
      category: MenuCategory.ENTREE,
      price: 18.90,
      imageUrl: '/images/wings.jpg',
      available: true,
      createdAt: new Date(),
      updatedAt: new Date()
    },
    {
      id: '2',
      name: '20 Wings',
      description: 'Twenty delicious wings with your choice of flavor',
      category: MenuCategory.ENTREE,
      price: 35.90,
      imageUrl: '/images/wings.jpg',
      available: true,
      createdAt: new Date(),
      updatedAt: new Date()
    },
    {
      id: '3',
      name: 'Fries',
      description: 'Crispy golden fries',
      category: MenuCategory.SIDE,
      price: 8.90,
      imageUrl: '/images/fries.jpg',
      available: true,
      createdAt: new Date(),
      updatedAt: new Date()
    }
  ];

  private groupOrders: GroupOrder[] = [
    {
      id: 'go-1',
      code: 'WZABC1',
      hostUserId: 'user-1',
      hostUserName: 'John Doe',
      members: [
        {
          userId: 'user-1',
          name: 'John Doe',
          email: 'john@example.com',
          cartItems: [
            {
              menuItemId: '1',
              menuItemName: '10 Wings',
              category: MenuCategory.ENTREE,
              quantity: 2,
              basePrice: 18.90,
              customization: { flavor: 'Buffalo' as any },
              subtotal: 37.80
            }
          ],
          memberTotal: 37.80,
          paymentStatus: 'pending'
        }
      ],
      status: 'active',
      deliveryAddress: '123 Main St',
      deliveryInstructions: 'Ring doorbell',
      totalAmount: 37.80,
      createdAt: new Date(),
      expiresAt: new Date(Date.now() + 3600000)
    }
  ];

  // Menu Management
  async getMenuItems(): Promise<MenuItem[]> {
    return Promise.resolve([...this.menuItems]);
  }

  async getMenuItem(id: string): Promise<MenuItem | null> {
    const item = this.menuItems.find(item => item.id === id);
    return Promise.resolve(item || null);
  }

  async createMenuItem(item: Omit<MenuItem, 'id' | 'createdAt' | 'updatedAt'>): Promise<MenuItem> {
    const newItem: MenuItem = {
      ...item,
      id: `item-${Date.now()}`,
      createdAt: new Date(),
      updatedAt: new Date()
    };
    this.menuItems.push(newItem);
    return Promise.resolve(newItem);
  }

  async updateMenuItem(id: string, updates: Partial<MenuItem>): Promise<MenuItem> {
    const index = this.menuItems.findIndex(item => item.id === id);
    if (index === -1) throw new Error('Item not found');
    
    this.menuItems[index] = {
      ...this.menuItems[index],
      ...updates,
      updatedAt: new Date()
    };
    return Promise.resolve(this.menuItems[index]);
  }

  async deleteMenuItem(id: string): Promise<void> {
    this.menuItems = this.menuItems.filter(item => item.id !== id);
    return Promise.resolve();
  }

  // Group Orders
  async getGroupOrders(): Promise<GroupOrder[]> {
    return Promise.resolve([...this.groupOrders]);
  }

  async getGroupOrder(id: string): Promise<GroupOrder | null> {
    const order = this.groupOrders.find(order => order.id === id);
    return Promise.resolve(order || null);
  }

  async updateGroupOrderStatus(id: string, status: string): Promise<void> {
    const order = this.groupOrders.find(order => order.id === id);
    if (order) {
      order.status = status;
    }
    return Promise.resolve();
  }

  // Dashboard Stats
  async getDashboardStats(): Promise<DashboardStats> {
    return Promise.resolve({
      todayOrders: 15,
      todayRevenue: 542.50,
      pendingOrders: 3,
      activeGroupOrders: this.groupOrders.filter(o => o.status === 'active').length
    });
  }
}

export const dataService = new DataService();
