import React, { useEffect, useState, useMemo } from 'react';
import { Row, Col, Button, Badge, Table, Alert, Nav, NavItem, NavLink, Collapse, Card, CardHeader, CardBody } from 'reactstrap';
import Widget from '../components/Widget/Widget';
import { ordersService, individualOrdersService, menuService } from '../services/firebase';
import { printService, type GroupOrderForPrint } from '../services/printService';
import type { GroupOrder } from '../types';
import ReceiptModal from '../components/ReceiptModal/ReceiptModal';
import PackagingStickerModal from '../components/PackagingStickerModal/PackagingStickerModal';
import { aggregateKitchenIngredients } from '../utils/kitchenIngredients';
import { doc, getDoc } from 'firebase/firestore';
import { db } from '../lib/firebase';

type OrderTab = 'individual' | 'group';

// Helper function to group orders by date
const groupOrdersByDate = (orders: any[]) => {
  const groups: Record<string, any[]> = {};
  
  orders.forEach(order => {
    const orderDate = order.createdAt?.toDate ? order.createdAt.toDate() : new Date(order.createdAt);
    const today = new Date();
    const yesterday = new Date(today);
    yesterday.setDate(yesterday.getDate() - 1);
    
    let dateKey: string;
    let displayDate: string;
    
    // Check if order is from today
    if (orderDate.toDateString() === today.toDateString()) {
      dateKey = 'today';
      displayDate = '📅 Today';
    }
    // Check if order is from yesterday
    else if (orderDate.toDateString() === yesterday.toDateString()) {
      dateKey = 'yesterday';
      displayDate = '📅 Yesterday';
    }
    // Older orders
    else {
      dateKey = orderDate.toDateString();
      displayDate = `📅 ${orderDate.toLocaleDateString('en-US', { 
        weekday: 'long', 
        year: 'numeric', 
        month: 'long', 
        day: 'numeric' 
      })}`;
    }
    
    if (!groups[dateKey]) {
      groups[dateKey] = [];
    }
    groups[dateKey].push({ ...order, displayDate, originalDate: orderDate });
  });
  
  // Sort groups by date (newest first)
  const sortedGroups = Object.entries(groups).sort(([, ordersA], [, ordersB]) => {
    const dateA = ordersA[0].originalDate;
    const dateB = ordersB[0].originalDate;
    return dateB.getTime() - dateA.getTime();
  });
  
  return sortedGroups.map(([key, orders]) => ({
    dateKey: key,
    displayDate: orders[0].displayDate,
    orders: orders.sort((a, b) => b.originalDate.getTime() - a.originalDate.getTime())
  }));
};

const OrdersPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<OrderTab>('individual');
  const [groupOrders, setGroupOrders] = useState<GroupOrder[]>([]);
  const [individualOrders, setIndividualOrders] = useState<any[]>([]);
  const [menuItems, setMenuItems] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [receiptModalOpen, setReceiptModalOpen] = useState(false);
  const [stickerModalOpen, setStickerModalOpen] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState<any>(null);
  const [collapsedSections, setCollapsedSections] = useState<Record<string, boolean>>({
    today: false, // Today's orders are open by default
  });
  const [visibleGroupReceipts, setVisibleGroupReceipts] = useState<Record<string, boolean>>({});
  const [searchQuery, setSearchQuery] = useState('');
  const [printerName, setPrinterName] = useState<string>('');

  // Filter orders by search query
  const filteredIndividualOrders = useMemo(() => {
    if (!searchQuery.trim()) return individualOrders;
    const query = searchQuery.toLowerCase();
    return individualOrders.filter(order => {
      // Search in multiple fields
      const searchableText = [
        order.code,
        order.id,
        order.groupOrderCode,
        order.userName,
        order.hostUserName
      ].filter(Boolean).join(' ').toLowerCase();
      
      return searchableText.includes(query);
    });
  }, [individualOrders, searchQuery]);

  const filteredGroupOrders = useMemo(() => {
    if (!searchQuery.trim()) return groupOrders;
    const query = searchQuery.toLowerCase();
    return groupOrders.filter(order => {
      // Search in multiple fields
      const searchableText = [
        order.code,
        order.id,
        order.groupOrderCode,
        order.userName,
        order.hostUserName,
        order.groupName
      ].filter(Boolean).join(' ').toLowerCase();
      
      return searchableText.includes(query);
    });
  }, [groupOrders, searchQuery]);

  // Group orders by date using useMemo for performance
  const groupedIndividualOrders = useMemo(() => groupOrdersByDate(filteredIndividualOrders), [filteredIndividualOrders]);
  const groupedGroupOrders = useMemo(() => groupOrdersByDate(filteredGroupOrders), [filteredGroupOrders]);

  const toggleSection = (dateKey: string) => {
    setCollapsedSections(prev => ({
      ...prev,
      [dateKey]: !prev[dateKey]
    }));
  };

  const toggleGroupReceipt = (orderId: string) => {
    setVisibleGroupReceipts(prev => ({
      ...prev,
      [orderId]: !prev[orderId]
    }));
  };

  useEffect(() => {
    // Subscribe to both order types
    const unsubscribeGroup = ordersService.onOrdersChange((newOrders) => {
      setGroupOrders(newOrders);
      setLoading(false);
    });

    const unsubscribeIndividual = individualOrdersService.onOrdersChange((newOrders) => {
      setIndividualOrders(newOrders);
    });

    // Fetch menu items for kitchen ingredients lookup
    const fetchMenuItems = async () => {
      try {
        const items = await menuService.getAllMenuItems();
        setMenuItems(items);
      } catch (err) {
        console.error('Error fetching menu items:', err);
      }
    };
    fetchMenuItems();

    // Fetch printer name from settings
    const fetchPrinterSettings = async () => {
      try {
        const settingsRef = doc(db, 'appSettings', 'adminPreferences');
        const settingsSnap = await getDoc(settingsRef);
        if (settingsSnap.exists()) {
          const printer = settingsSnap.data().printerName || '';
          setPrinterName(printer);
          console.log('[OrdersPage] Loaded printer from settings:', printer);
        }
      } catch (err) {
        console.error('Error fetching printer settings:', err);
      }
    };
    fetchPrinterSettings();

    return () => {
      unsubscribeGroup();
      unsubscribeIndividual();
    };
  }, []);

  const getStatusColor = (status: string) => {
    const colors: Record<string, string> = {
      active: 'info',
      confirmed: 'primary',
      preparing: 'warning',
      ready: 'success',
      delivered: 'success',
      cancelled: 'danger',
    };
    return colors[status] || 'secondary';
  };

  const handleUpdateStatus = async (orderId: string, newStatus: 'pending' | 'confirmed' | 'preparing' | 'ready' | 'delivered' | 'cancelled', isGroupOrder: boolean = true) => {
    try {
      if (isGroupOrder) {
        await ordersService.updateOrderStatus(orderId, newStatus);
      } else {
        await individualOrdersService.updateOrderStatus(orderId, newStatus as any);
      }
      setSuccess(`Order updated to ${newStatus}!`);
      setTimeout(() => setSuccess(''), 3000);
    } catch (err: any) {
      setError(err.message || 'Failed to update order status');
      setTimeout(() => setError(''), 3000);
    }
  };

  const handleDeleteOrder = async (orderId: string, isGroupOrder: boolean = true) => {
    if (!confirm('⚠️ Development: Delete this order? This cannot be undone!')) return;
    
    try {
      if (isGroupOrder) {
        await ordersService.deleteOrder(orderId);
      } else {
        await individualOrdersService.deleteOrder(orderId);
      }
      setSuccess('Order deleted successfully');
      setTimeout(() => setSuccess(''), 3000);
    } catch (err: any) {
      setError(err.message || 'Failed to delete order');
      setTimeout(() => setError(''), 3000);
    }
  };

  const handleDeleteAllOrders = async (isGroupOrder: boolean = true) => {
    if (!confirm('⚠️ Development: Delete ALL orders? This cannot be undone!')) return;
    if (!confirm('Are you ABSOLUTELY sure? This will delete ALL orders permanently!')) return;
    
    try {
      if (isGroupOrder) {
        await ordersService.deleteAllOrders();
      } else {
        await individualOrdersService.deleteAllOrders();
      }
      setSuccess('All orders deleted successfully');
      setTimeout(() => setSuccess(''), 3000);
    } catch (err: any) {
      setError(err.message || 'Failed to delete orders');
      setTimeout(() => setError(''), 3000);
    }
  };

  const handlePrintGroupReceipts = async (order: any) => {
    try {
      const groupOrderForPrint: GroupOrderForPrint = {
        id: order.id,
        groupName: order.groupName || 'Group Order',
        hostUserName: order.userName || order.hostUserName || 'Host',
        participants: (order.members || order.participants || []).map((p: any) => ({
          userId: p.userId,
          userName: p.userName || p.name,
          items: p.cartItems || p.items || [],
          total: p.total || (p.cartItems || []).reduce((sum: number, item: any) => sum + (item.subtotal || 0), 0)
        })),
        orderDate: order.createdAt?.toDate() || new Date(),
        total: order.total || (order.members || []).reduce((sum: number, m: any) => 
          sum + (m.cartItems || []).reduce((itemSum: number, item: any) => 
            itemSum + (item.subtotal || 0), 0
          ), 0
        ),
        orderType: order.orderType,
        location: order.location || order.selectedLocation,
        paymentMethod: order.paymentMethod
      };

      setSuccess(`Printing ${(order.members || order.participants || []).length + 1} receipts (1 master + ${order.members?.length || 0} members)...`);
      console.log(`[OrdersPage] Using printer: ${printerName || 'Default'}`);
      
      // Force print = true for manual button clicks (bypasses autoPrintEnabled check)
      // Pass the configured printer name from settings
      await printService.printGroupOrderReceipts(groupOrderForPrint, menuItems, printerName || undefined, true);
      
      setSuccess(`Successfully printed ${(order.members || order.participants || []).length + 1} receipts!`);
      setTimeout(() => setSuccess(''), 3000);
    } catch (error) {
      console.error('Error printing group receipts:', error);
      setError(`Failed to print receipts: ${error instanceof Error ? error.message : 'Unknown error'}`);
      setTimeout(() => setError(''), 5000);
    }
  };

  // Enrich order with kitchen ingredients for receipt preview
  const enrichOrderForPreview = (order: any) => {
    if (!order) return order;

    // For group orders, enrich member cart items
    if (order.members || order.isGroupOrder) {
      return {
        ...order,
        members: (order.members || []).map((member: any) => ({
          ...member,
          cartItems: (member.cartItems || []).map((item: any) => {
            const itemName = item.menuItemName || item.name;
            const menuItem = menuItems.find(m => m.name === itemName);
            
            if (menuItem) {
              return {
                ...item,
                menuItem: menuItem,
                kitchenIngredients: menuItem.kitchenIngredients
              };
            }
            return item;
          })
        }))
      };
    }

    // For individual orders, enrich items array
    if (order.items) {
      return {
        ...order,
        items: order.items.map((item: any) => {
          const itemName = item.menuItemName || item.name;
          const menuItem = menuItems.find(m => m.name === itemName);
          
          if (menuItem) {
            return {
              ...item,
              menuItem: menuItem,
              kitchenIngredients: menuItem.kitchenIngredients
            };
          }
          return item;
        })
      };
    }

    return order;
  };

  // Helper function to categorize items
  const categorizeItems = (items: any[]) => {
    const meals: any[] = [];
    const sides: any[] = [];
    const beverages: any[] = [];
    const drinks: string[] = [];
    const dippingSauces: string[] = [];

    items.forEach((item) => {
      // Check if it's a beverage category
      if (item.name.includes('Coca-Cola') || item.name.includes('Sprite') || 
          item.name.includes('Tea') || item.name.includes('Juice') || 
          item.name.includes('Water')) {
        beverages.push(item);
      }
      // Check if it's a side
      else if (item.name.includes('Fries') || item.name.includes('Salad') || 
               item.name.includes('Chips') || item.name.includes('Rice') || 
               item.name.includes('Stix') || item.name.includes('Mozzarella')) {
        sides.push(item);
      }
      // Otherwise it's a meal
      else {
        meals.push(item);
      }

      // Extract drinks and sauces from customizations
      if (item.customization?.drink) {
        drinks.push(item.customization.drink);
      }
      if (item.customization?.dippingSauce) {
        dippingSauces.push(item.customization.dippingSauce);
      }
    });

    // Combine duplicate drinks and sauces with counts
    const drinkCounts = drinks.reduce((acc: any, drink: string) => {
      acc[drink] = (acc[drink] || 0) + 1;
      return acc;
    }, {});

    const sauceCounts = dippingSauces.reduce((acc: any, sauce: string) => {
      acc[sauce] = (acc[sauce] || 0) + 1;
      return acc;
    }, {});

    // Aggregate raw kitchen ingredients using shared utility
    const kitchenIngredientsSummary = aggregateKitchenIngredients(items, menuItems);

    // Helper to normalize keys for consistent aggregation
    const normalizeKey = (key: string): string => {
      let normalized = key
        .replace(/_/g, ' ')  // Replace underscores with spaces
        .replace(/-/g, ' ')  // Replace hyphens with spaces
        .split(' ')
        .map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
        .join(' ');
      
      // Normalize common synonyms
      if (normalized === 'Smiley Fries') normalized = 'Smiley';
      if (normalized === 'Coca Cola') normalized = 'Coke';
      
      return normalized;
    };

    // Organize kitchen summary into categories
    const mainItems: Record<string, number> = {};
    const sidesItems: Record<string, number> = {};
    const dippingsItems: Record<string, number> = {};
    const drinksItems: Record<string, number> = {};

    // Categorize existing ingredients
    Object.entries(kitchenIngredientsSummary).forEach(([ingredient, count]) => {
      let key = ingredient;
      
      // Strip flavors (everything after " - ")
      key = key.replace(/\s*-\s*.+$/, '').trim();
      
      const lower = key.toLowerCase();
      
      // Convert bone type ingredients to wings
      if (lower === 'boneless' || lower.includes('boneless wings')) {
        key = 'Boneless Wings';
        mainItems[normalizeKey(key)] = (mainItems[normalizeKey(key)] || 0) + count;
      } else if (lower === 'original' || (lower.includes('wings') && lower.includes('original'))) {
        key = 'Wings';
        mainItems[normalizeKey(key)] = (mainItems[normalizeKey(key)] || 0) + count;
      }
      // Main proteins (excluding rice/veggie combos)
      else if ((lower.includes('wings') || lower.includes('tender') || 
                lower.includes('grill chicken') || lower.includes('grilled chicken') ||
                (lower.includes('beef') && !lower.includes('rice') && !lower.includes('veggie'))) &&
               !lower.includes('rice') && !lower.includes('salad') && !lower.includes('veggie')) {
        mainItems[normalizeKey(key)] = (mainItems[normalizeKey(key)] || 0) + count;
      }
      // Fries (including substitutions)
      else if (lower.includes('fries') || lower.includes('wedge')) {
        sidesItems[normalizeKey(key)] = (sidesItems[normalizeKey(key)] || 0) + count;
      }
      // Salads
      else if (lower.includes('salad')) {
        sidesItems[normalizeKey(key)] = (sidesItems[normalizeKey(key)] || 0) + count;
      }
      // Other sides (rice, veggies, chips, etc.)
      else if (lower.includes('rice') || lower.includes('veggies') || lower.includes('vegetables') ||
               lower.includes('grilled veg') || lower.includes('bread') || lower.includes('chips') ||
               lower.includes('stix') || lower.includes('mozzarella') || lower.includes('smiley') ||
               lower.includes('potato') || lower.includes('drumstick')) {
        sidesItems[normalizeKey(key)] = (sidesItems[normalizeKey(key)] || 0) + count;
      } else {
        sidesItems[normalizeKey(key)] = (sidesItems[normalizeKey(key)] || 0) + count;
      }
    });

    // Add drinks from customizations
    Object.entries(drinkCounts).forEach(([drink, count]) => {
      const normalizedDrink = normalizeKey(drink);
      drinksItems[normalizedDrink] = (drinksItems[normalizedDrink] || 0) + (count as number);
    });
    
    // Add sauces to dippings
    Object.entries(sauceCounts).forEach(([sauce, count]) => {
      const normalizedSauce = normalizeKey(sauce);
      dippingsItems[normalizedSauce] = (dippingsItems[normalizedSauce] || 0) + (count as number);
    });

    return { 
      meals, 
      sides, 
      beverages, 
      drinks, 
      dippingSauces, 
      drinkCounts, 
      sauceCounts, 
      mainItems, 
      sidesItems, 
      dippingsItems, 
      drinksItems 
    };
  };

  const formatDate = (timestamp: any) => {
    if (!timestamp) return 'N/A';
    let date = new Date();
    if (typeof timestamp.toDate === 'function') {
      date = timestamp.toDate();
    } else if (timestamp instanceof Date) {
      date = timestamp;
    }
    return date.toLocaleString();
  };

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h1 className="page-title">
          Orders <Badge color="primary">{activeTab === 'individual' ? filteredIndividualOrders.length : filteredGroupOrders.length}</Badge>
        </h1>
        <div>
          <Button
            color="danger" 
            size="sm"
            onClick={() => handleDeleteAllOrders(activeTab === 'group')}
          >
            <i className="bi bi-trash me-1"></i>
            Clear All {activeTab === 'group' ? 'Group' : 'Individual'}
          </Button>
        </div>
      </div>

      {error && (
        <Alert color="danger" className="mb-3" toggle={() => setError('')}>
          <i className="bi bi-exclamation-triangle me-2"></i>
          {error}
        </Alert>
      )}

      {success && (
        <Alert color="success" className="mb-3" toggle={() => setSuccess('')}>
          <i className="bi bi-check-circle me-2"></i>
          {success}
        </Alert>
      )}

      {/* Auto-progression info banner */}
      <Alert color="info" className="mb-3">
        <i className="bi bi-info-circle me-2"></i>
        <strong>Auto Status:</strong> Orders automatically progress: Pending → Confirmed (1s) → Preparing (30s). 
        Click <strong>"Mark as Ready"</strong> when order is complete. Ready orders auto-deliver after 5 min.
      </Alert>

      <Nav tabs className="mb-3">
        <NavItem>
          <NavLink
            active={activeTab === 'individual'}
            onClick={() => setActiveTab('individual')}
            style={{ cursor: 'pointer' }}
          >
            <i className="bi bi-person me-2"></i>
            Individual Orders ({individualOrders.length})
          </NavLink>
        </NavItem>
        <NavItem>
          <NavLink
            active={activeTab === 'group'}
            onClick={() => setActiveTab('group')}
            style={{ cursor: 'pointer' }}
          >
            <i className="bi bi-people me-2"></i>
            Group Orders ({groupOrders.length})
          </NavLink>
        </NavItem>
      </Nav>

      {/* Search Bar */}
      <div className="mb-3">
        <input
          type="text"
          className="form-control"
          placeholder="🔍 Search by order code, username, or group name..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          style={{ maxWidth: '500px' }}
        />
        {searchQuery && (
          <small className="text-muted mt-1 d-block">
            Found {activeTab === 'individual' ? filteredIndividualOrders.length : filteredGroupOrders.length} order(s) matching "{searchQuery}"
          </small>
        )}
      </div>

      {activeTab === 'individual' ? (
        individualOrders.length === 0 ? (
          <Widget>
            <div className="text-center text-muted py-5">
              <i className="bi bi-inbox" style={{ fontSize: '3rem' }}></i>
              <p className="mt-3">No individual orders found</p>
            </div>
          </Widget>
        ) : (
          groupedIndividualOrders.map(({ dateKey, displayDate, orders }) => (
            <Card key={dateKey} className="mb-3">
              <CardHeader 
                onClick={() => toggleSection(dateKey)}
                style={{ cursor: 'pointer', backgroundColor: '#f8f9fa' }}
                className="d-flex justify-content-between align-items-center"
              >
                <div>
                  <strong>{displayDate}</strong>
                  <Badge color="secondary" className="ms-2">{orders.length} orders</Badge>
                </div>
                <i className={`bi bi-chevron-${collapsedSections[dateKey] ? 'down' : 'up'}`}></i>
              </CardHeader>
              <Collapse isOpen={!collapsedSections[dateKey]}>
                <CardBody className="p-0">
                  <Table responsive hover className="mb-0">
                    <thead>
                      <tr>
                        <th>Order ID</th>
                        <th>Customer</th>
                        <th>Items</th>
                        <th>Total</th>
                        <th>Status</th>
                        <th>Time</th>
                        <th>Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {orders.map((order) => (
                        <tr key={order.id}>
                          <td><strong>{order.id.substring(0, 8).toUpperCase()}</strong></td>
                          <td>{order.userName || 'Guest'}</td>
                          <td>{order.items?.length || 0} items</td>
                          <td><strong>RM {(order.total || 0).toFixed(2)}</strong></td>
                          <td>
                            <Badge color={getStatusColor(order.status || 'pending')}>
                              {(order.status || 'pending').toUpperCase()}
                            </Badge>
                          </td>
                          <td>{order.createdAt?.toDate ? order.createdAt.toDate().toLocaleTimeString() : 'N/A'}</td>
                          <td>
                            <div className="d-flex gap-1">
                              {order.status !== 'ready' && order.status !== 'delivered' && (
                                <Button 
                                  size="sm" 
                                  color="success"
                                  outline
                                  onClick={() => handleUpdateStatus(order.id, 'ready', false)}
                                  title="Mark as ready for pickup"
                                >
                                  <i className="bi bi-check-circle"></i> Ready
                                </Button>
                              )}
                              <Button 
                                size="sm" 
                                color="info"
                                outline
                                onClick={() => {
                                  setSelectedOrder(enrichOrderForPreview(order));
                                  setReceiptModalOpen(true);
                                }}
                              >
                                <i className="bi bi-receipt"></i>
                              </Button>
                              <Button 
                                size="sm" 
                                color="danger"
                                outline
                                onClick={() => handleDeleteOrder(order.id, false)}
                                title="Delete order (dev only)"
                              >
                                <i className="bi bi-trash"></i>
                              </Button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </Table>
                </CardBody>
              </Collapse>
            </Card>
          ))
        )
      ) : (
        groupOrders.length === 0 ? (
          <Widget>
            <div className="text-center text-muted py-5">
              <i className="bi bi-inbox" style={{ fontSize: '3rem' }}></i>
              <p className="mt-3">No group orders found</p>
            </div>
          </Widget>
        ) : (
          groupedGroupOrders.map(({ dateKey, displayDate, orders }) => (
            <div key={dateKey} className="mb-4">
              <Card className="mb-2">
                <CardHeader 
                  onClick={() => toggleSection(dateKey)}
                  style={{ cursor: 'pointer', backgroundColor: '#f8f9fa' }}
                  className="d-flex justify-content-between align-items-center"
                >
                  <div>
                    <strong>{displayDate}</strong>
                    <Badge color="secondary" className="ms-2">{orders.length} orders</Badge>
                  </div>
                  <i className={`bi bi-chevron-${collapsedSections[dateKey] ? 'down' : 'up'}`}></i>
                </CardHeader>
              </Card>
              <Collapse isOpen={!collapsedSections[dateKey]}>
                {orders.map((order: any) => (
        <Widget key={order.id} title={
          <div className="d-flex justify-content-between align-items-center w-100">
            <div>
              <strong>Group Order #{order.groupOrderCode || order.code}</strong>
              <span className="text-muted ms-3">Host: {order.userName || order.hostUserName}</span>
              <Badge color="primary" className="ms-2">{order.members?.length || order.memberCount || 0} Members</Badge>
            </div>
            <Badge color={getStatusColor(order.status)} className="text-uppercase">
              {order.status}
            </Badge>
          </div>
        }> 
          <Row className="mb-3">
            <Col md={12} className="mb-3">
              {/* Status Progress Timeline */}
              <div className="d-flex justify-content-between align-items-center" style={{ position: 'relative' }}>
                <div style={{ position: 'absolute', top: '15px', left: '10%', right: '10%', height: '2px', backgroundColor: '#e0e0e0', zIndex: 0 }}></div>
                {['pending', 'confirmed', 'preparing', 'ready', 'delivered'].map((statusStep, idx) => {
                  const statusIndex = ['pending', 'confirmed', 'preparing', 'ready', 'delivered'].indexOf(order.status);
                  const currentIndex = ['pending', 'confirmed', 'preparing', 'ready', 'delivered'].indexOf(statusStep);
                  const isActive = currentIndex <= statusIndex;
                  const isCurrent = statusStep === order.status;
                  
                  return (
                    <div key={statusStep} style={{ flex: 1, textAlign: 'center', position: 'relative', zIndex: 1 }}>
                      <div 
                        style={{ 
                          width: '30px', 
                          height: '30px', 
                          borderRadius: '50%', 
                          backgroundColor: isActive ? (isCurrent ? '#28a745' : '#007bff') : '#e0e0e0',
                          margin: '0 auto 8px',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          color: 'white',
                          fontWeight: 'bold',
                          border: isCurrent ? '3px solid #28a745' : 'none',
                          boxShadow: isCurrent ? '0 0 10px rgba(40, 167, 69, 0.5)' : 'none'
                        }}
                      >
                        {isActive ? <i className="bi bi-check"></i> : idx + 1}
                      </div>
                      <small style={{ fontWeight: isCurrent ? 'bold' : 'normal', color: isActive ? '#000' : '#999' }}>
                        {statusStep.charAt(0).toUpperCase() + statusStep.slice(1)}
                      </small>
                    </div>
                  );
                })}
              </div>
            </Col>
          </Row>
          
          <Row className="mb-3">
            <Col md={6}>
              <h6 className="mb-2">Order Summary</h6>
              <div className="mb-2">
                <small className="text-muted">Order ID:</small>
                <div><strong>{order.id.substring(0, 12).toUpperCase()}</strong></div>
              </div>
              <div className="mb-2">
                <small className="text-muted">Group Code:</small>
                <div><strong>{order.groupOrderCode || order.code}</strong></div>
              </div>
              <div className="mb-2">
                <small className="text-muted">Created:</small>
                <div>{formatDate(order.createdAt)}</div>
              </div>
            </Col>
            <Col md={6}>
              <h6 className="mb-2">Members ({order.members?.length || order.memberCount || 0})</h6>
              <div style={{ backgroundColor: '#f8f9fa', padding: '10px', borderRadius: '5px', fontSize: '0.9em' }}>
                {order.members && order.members.length > 0 ? (
                  <div>
                    {order.members.map((member: any, idx: number) => {
                      const memberTotal = (member.cartItems || []).reduce((sum: number, item: any) => sum + (item.subtotal || 0), 0);
                      const isHost = member.userId === order.hostId || member.isHost;
                      return (
                        <div key={idx} style={{ marginBottom: '8px', paddingBottom: '8px', borderBottom: idx < order.members.length - 1 ? '1px solid #dee2e6' : 'none' }}>
                          <div>
                            <strong>{member.userName || member.name || 'Unknown'}</strong>
                            {isHost && <Badge color="warning" className="ms-2" style={{ fontSize: '0.7em' }}>HOST</Badge>}
                          </div>
                          <div style={{ fontSize: '0.85em', color: '#6c757d' }}>
                            {member.cartItems?.length || 0} item(s) • RM {memberTotal.toFixed(2)}
                          </div>
                        </div>
                      );
                    })}
                  </div>
                ) : (
                  <div style={{ color: '#6c757d' }}>
                    <div className="mb-2">
                      <i className="bi bi-info-circle me-2"></i>
                      This is an old order from before the member tracking update.
                    </div>
                    {order.memberDetails && (
                      <div style={{ fontSize: '0.9em', whiteSpace: 'pre-line' }}>{order.memberDetails}</div>
                    )}
                    {!order.memberDetails && (
                      <div>No detailed member information available</div>
                    )}
                  </div>
                )}
              </div>
            </Col>
          </Row>
          
          <Row className="mb-3">
            <Col>
              <div className="d-flex justify-content-between align-items-center mb-2">
                <h6 className="mb-0">Items & Receipt Preview</h6>
                <Button
                  size="sm"
                  color="info"
                  outline
                  onClick={() => toggleGroupReceipt(order.id)}
                >
                  <i className={`bi bi-chevron-${visibleGroupReceipts[order.id] ? 'up' : 'down'} me-1`}></i>
                  {visibleGroupReceipts[order.id] ? 'Hide' : 'Show'} Receipt Preview
                </Button>
              </div>
              {visibleGroupReceipts[order.id] && (
              <Table responsive bordered size="sm">
                <thead>
                  <tr>
                    <th>Item</th>
                    <th style={{ width: '80px' }}>Qty</th>
                    <th style={{ width: '100px' }}>Price</th>
                    <th style={{ width: '100px' }}>Subtotal</th>
                  </tr>
                </thead>
                <tbody>
                  {(() => {
                    // Group items by member
                    const memberGroups: Record<string, any[]> = {};
                    
                    if (order.members && order.members.length > 0) {
                      order.members.forEach((member: any) => {
                        const memberKey = member.userId || member.userName || member.name;
                        const memberName = member.userName || member.name || 'Unknown';
                        const isHost = member.userId === order.hostId || member.isHost;
                        
                        if (!memberGroups[memberKey]) {
                          memberGroups[memberKey] = [];
                        }
                        
                        (member.cartItems || []).forEach((item: any) => {
                          memberGroups[memberKey].push({
                            ...item,
                            memberName,
                            isHost,
                            name: item.menuItem?.name || item.menuItemName || item.name || 'Unknown Item',
                            price: item.menuItem?.price || item.price || 0,
                            quantity: item.quantity || 1,
                            subtotal: item.subtotal || 0,
                            customization: item.customization
                          });
                        });
                      });
                    }
                    
                    if (Object.keys(memberGroups).length === 0) {
                      return (
                        <tr>
                          <td colSpan={4} className="text-center text-muted">No items found</td>
                        </tr>
                      );
                    }

                    // Get all items for kitchen summary
                    const allItems = Object.values(memberGroups).flat();
                    const { meals, sides, beverages, drinks, dippingSauces, drinkCounts, sauceCounts, mainItems, sidesItems, dippingsItems, drinksItems } = categorizeItems(allItems);
                    
                    return (
                      <>
                        {/* Items Grouped by Member */}
                        {Object.entries(memberGroups).map(([memberKey, items], memberIdx) => {
                          const memberTotal = items.reduce((sum, item) => sum + (item.subtotal || 0), 0);
                          const memberName = items[0]?.memberName || 'Unknown';
                          const isHost = items[0]?.isHost || false;
                          
                          return (
                            <React.Fragment key={memberKey}>
                              {/* Member Header Row */}
                              <tr style={{ 
                                backgroundColor: '#4a5568', 
                                color: 'white',
                                borderTop: memberIdx > 0 ? '3px solid #2d3748' : 'none',
                                borderBottom: '2px solid #2d3748'
                              }}>
                                <td colSpan={4} style={{ padding: '12px' }}>
                                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                    <i className="bi bi-person-fill" style={{ fontSize: '1.2em' }}></i>
                                    <strong style={{ fontSize: '1.15em', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                                      {memberName}
                                    </strong>
                                    {isHost && <Badge color="warning" style={{ fontSize: '0.7em' }}>HOST</Badge>}
                                    <span style={{ fontSize: '0.85em', opacity: 0.9, marginLeft: '8px' }}>
                                      {items.length} item{items.length !== 1 ? 's' : ''} • RM {memberTotal.toFixed(2)}
                                    </span>
                                  </div>
                                </td>
                              </tr>
                              
                              {/* Member's Items */}
                              {items.map((item: any, idx: number) => (
                                <tr key={`${memberKey}-item-${idx}`}>
                                  <td>
                                    <strong>{item.name}</strong>
                                    {item.customization?.boneType && (
                                      <div><small className="text-muted">&gt; {item.customization.boneType}</small></div>
                                    )}
                                    {item.customization?.flavor && (
                                      <div><small className="text-muted">&gt; Flavor: {item.customization.flavor.replace(/_/g, ' ')}</small></div>
                                    )}
                                    {item.customization?.dippingSauce && (
                                      <div style={{ backgroundColor: '#ffeb9c', padding: '2px 4px', marginTop: '2px' }}>
                                        <small><strong>&gt; DIP: {item.customization.dippingSauce.replace(/_/g, ' ').toUpperCase()}</strong></small>
                                      </div>
                                    )}
                                    {item.customization?.friesExchange && (
                                      <div><small className="text-muted">&gt; Side: {item.customization.friesExchange.name}</small></div>
                                    )}
                                    {item.customization?.saladType && (
                                      <div><small className="text-muted">&gt; Salad: {item.customization.saladType}</small></div>
                                    )}
                                    {item.customization?.drink && (
                                      <div><small className="text-muted">&gt; Drink: {item.customization.drink}</small></div>
                                    )}
                                  </td>
                                  <td className="text-center">{item.quantity}</td>
                                  <td>RM {(item.price || 0).toFixed(2)}</td>
                                  <td><strong>RM {(item.subtotal || 0).toFixed(2)}</strong></td>
                                </tr>
                              ))}
                            </React.Fragment>
                          );
                        })}

                        {/* Kitchen Summary with Categories */}
                        {(Object.keys(mainItems).length > 0 || 
                          Object.keys(sidesItems).length > 0 || 
                          Object.keys(dippingsItems).length > 0 || 
                          Object.keys(drinksItems).length > 0) && (
                          <>
                            <tr style={{ backgroundColor: '#ffe6e6', borderTop: '3px solid #000' }}>
                              <td colSpan={4} className="text-center">
                                <strong style={{ fontSize: '1.1em' }}>========================================</strong><br/>
                                <strong style={{ fontSize: '1.1em' }}>&gt;&gt; KITCHEN SUMMARY &lt;&lt;</strong><br/>
                                <strong style={{ fontSize: '1.1em' }}>========================================</strong>
                              </td>
                            </tr>
                            <tr>
                              <td colSpan={4} style={{ border: '2px solid #000', padding: '15px' }}>
                                {Object.keys(mainItems).length > 0 && (
                                  <div style={{ marginBottom: '10px' }}>
                                    <strong>MAIN:</strong>
                                    {Object.entries(mainItems).map(([ingredient, count]: [string, any], idx) => (
                                      <div key={idx}>- {count} {ingredient.toUpperCase()}</div>
                                    ))}
                                  </div>
                                )}
                                {Object.keys(sidesItems).length > 0 && (
                                  <div style={{ marginBottom: '10px' }}>
                                    <strong>SIDES:</strong>
                                    {Object.entries(sidesItems).map(([ingredient, count]: [string, any], idx) => (
                                      <div key={idx}>- {count} {ingredient.toUpperCase()}</div>
                                    ))}
                                  </div>
                                )}
                                {Object.keys(dippingsItems).length > 0 && (
                                  <div style={{ marginBottom: '10px' }}>
                                    <strong>DIPPINGS:</strong>
                                    {Object.entries(dippingsItems).map(([sauce, count]: [string, any], idx) => (
                                      <div key={idx}>- {count} {sauce.toUpperCase()}</div>
                                    ))}
                                  </div>
                                )}
                                {Object.keys(drinksItems).length > 0 && (
                                  <div>
                                    <strong>DRINKS:</strong>
                                    {Object.entries(drinksItems).map(([drink, count]: [string, any], idx) => (
                                      <div key={idx}>- {count} {drink.toUpperCase()}</div>
                                    ))}
                                  </div>
                                )}
                              </td>
                            </tr>
                            <tr style={{ backgroundColor: '#ffe6e6' }}>
                              <td colSpan={4} className="text-center">
                                <strong style={{ fontSize: '1.1em' }}>========================================</strong>
                              </td>
                            </tr>
                          </>
                        )}

                        <tr className="table-active">
                          <td colSpan={3} className="text-end"><strong>TOTAL</strong></td>
                          <td><strong>RM {(
                            order.total || 
                            (order.members || []).reduce((sum: number, m: any) => 
                              sum + (m.cartItems || []).reduce((itemSum: number, item: any) => 
                                itemSum + (item.subtotal || 0), 0
                              ), 0
                            )
                          ).toFixed(2)}</strong></td>
                        </tr>
                      </>
                    );
                  })()}
                </tbody>
              </Table>
              )}
            </Col>
          </Row>
          
          {order.deliveryNotes && (
            <Row className="mb-3">
              <Col>
                <h6 className="mb-2">Delivery Notes</h6>
                <div style={{ whiteSpace: 'pre-line', backgroundColor: '#fff3cd', padding: '10px', borderRadius: '5px', border: '1px solid #ffc107' }}>
                  {order.deliveryNotes}
                </div>
              </Col>
            </Row>
          )}

          <div className="d-flex gap-2">
            {/* Show "Mark as Ready" button only for preparing orders */}
            {order.status === 'preparing' && (
              <Button 
                color="success"
                onClick={() => handleUpdateStatus(order.id, 'ready', false)}
              >
                <i className="bi bi-bell me-2"></i>Mark as Ready
              </Button>
            )}
            {/* Print Individual Receipts Button */}
            <Button 
              color="primary"
              onClick={() => handlePrintGroupReceipts(order)}
            >
              <i className="bi bi-printer me-2"></i>Print Receipts ({order.members?.length || 0})
            </Button>
            {/* Show cancel button for non-final orders */}
            {order.status !== 'delivered' && order.status !== 'cancelled' && (
              <Button 
                color="danger"
                outline
                onClick={() => handleUpdateStatus(order.id, 'cancelled', false)}
              >
                <i className="bi bi-x-circle me-2"></i>Cancel Order
              </Button>
            )}
            <Button 
              color="danger"
              outline
              onClick={() => handleDeleteOrder(order.id, true)}
              title="Delete order (dev only)"
            >
              <i className="bi bi-trash me-2"></i>Delete
            </Button>
          </div>
        </Widget>
          ))}
              </Collapse>
            </div>
          ))
        )
      )}

      {/* Receipt Modal */}
      <ReceiptModal 
        isOpen={receiptModalOpen}
        toggle={() => setReceiptModalOpen(false)}
        order={selectedOrder}
      />

      {/* Sticker Modal */}
      <PackagingStickerModal 
        isOpen={stickerModalOpen}
        toggle={() => setStickerModalOpen(false)}
        order={selectedOrder}
      />
    </div>
  );
};

export default OrdersPage;
