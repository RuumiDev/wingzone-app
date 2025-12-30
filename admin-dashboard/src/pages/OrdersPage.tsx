import React, { useEffect, useState } from 'react';
import { Row, Col, Button, Badge, Table, Alert, Nav, NavItem, NavLink } from 'reactstrap';
import Widget from '../components/Widget/Widget';
import { ordersService, individualOrdersService } from '../services/firebase';
import { printService, GroupOrderForPrint } from '../services/printService';
import type { GroupOrder } from '../types';
import ReceiptModal from '../components/ReceiptModal/ReceiptModal';
import PackagingStickerModal from '../components/PackagingStickerModal/PackagingStickerModal';

type OrderTab = 'individual' | 'group';

const OrdersPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<OrderTab>('individual');
  const [groupOrders, setGroupOrders] = useState<GroupOrder[]>([]);
  const [individualOrders, setIndividualOrders] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [receiptModalOpen, setReceiptModalOpen] = useState(false);
  const [stickerModalOpen, setStickerModalOpen] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState<any>(null);

  useEffect(() => {
    // Subscribe to both order types
    const unsubscribeGroup = ordersService.onOrdersChange((newOrders) => {
      setGroupOrders(newOrders);
      setLoading(false);
    });

    const unsubscribeIndividual = individualOrdersService.onOrdersChange((newOrders) => {
      setIndividualOrders(newOrders);
    });

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

  const handlePrintGroupReceipts = (order: any) => {
    const groupOrderForPrint: GroupOrderForPrint = {
      id: order.id,
      groupName: order.groupName || 'Group Order',
      hostUserName: order.userName || order.hostUserName || 'Host',
      participants: (order.participants || []).map((p: any) => ({
        userId: p.userId,
        userName: p.userName,
        items: p.items || [],
        total: p.total || 0
      })),
      orderDate: order.createdAt?.toDate() || new Date(),
      total: order.total || 0
    };

    printService.printGroupOrderReceipts(groupOrderForPrint);
    setSuccess(`Printing ${order.participants?.length || 0} receipts...`);
    setTimeout(() => setSuccess(''), 3000);
  };

  const formatDate = (timestamp: any) => {
    if (!timestamp) return 'N/A';
    let date = new Date();
    if (typeof timestamp.toDate === 'function') {
      date = timestamp.toDate();
    } else if (timestamp instanceof Date) {
      date = timestamp;
    }
    return new Intl.DateTimeFormat('en-MY', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    }).format(date);
  };

  if (loading) {
    return (
      <div>
        <h1 className="mb-4">Orders</h1>
        <div className="text-center py-5">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Loading...</span>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h1>Orders</h1>
        <div className="d-flex gap-2">
          <Badge color="info" className="fs-6 px-3 py-2">
            Individual: {individualOrders.length}
          </Badge>
          <Badge color="primary" className="fs-6 px-3 py-2">
            Group: {groupOrders.length}
          </Badge>
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

      {activeTab === 'individual' ? (
        individualOrders.length === 0 ? (
          <Widget>
            <div className="text-center text-muted py-5">
              <i className="bi bi-inbox" style={{ fontSize: '3rem' }}></i>
              <p className="mt-3">No individual orders found</p>
            </div>
          </Widget>
        ) : (
          <Widget>
            <Table responsive hover>
              <thead>
                <tr>
                  <th>Order ID</th>
                  <th>Customer</th>
                  <th>Items</th>
                  <th>Total</th>
                  <th>Status</th>
                  <th>Date</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {individualOrders.map((order) => (
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
                    <td><small className="text-muted">{formatDate(order.createdAt)}</small></td>
                    <td>
                      <div className="d-flex gap-2">
                        {/* Show "Mark as Ready" button only for preparing orders */}
                        {order.status === 'preparing' && (
                          <Button 
                            size="sm" 
                            color="success"
                            onClick={() => handleUpdateStatus(order.id, 'ready', false)}
                          >
                            <i className="bi bi-bell me-1"></i>Mark Ready
                          </Button>
                        )}
                        {/* Show cancel button for non-final orders */}
                        {order.status !== 'delivered' && order.status !== 'cancelled' && (
                          <Button 
                            size="sm" 
                            color="danger"
                            outline
                            onClick={() => handleUpdateStatus(order.id, 'cancelled', false)}
                          >
                            <i className="bi bi-x-circle"></i>
                          </Button>
                        )}
                        {/* Receipt button for all orders */}
                        <Button 
                          size="sm" 
                          color="info" 
                          outline
                          onClick={() => {
                            setSelectedOrder(order);
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
          </Widget>
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
          groupOrders.map((order: any) => (
        <Widget key={order.id} title={
          <div className="d-flex justify-content-between align-items-center w-100">
            <div>
              <strong>Group Order #{order.groupOrderCode || order.code}</strong>
              <span className="text-muted ms-3">Host: {order.userName || order.hostUserName}</span>
              <Badge color="primary" className="ms-2">{order.memberCount || 0} Members</Badge>
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
              <div className="mb-2">
                <small className="text-muted">Delivery Address:</small>
                <div>{order.deliveryAddress || 'Not specified'}</div>
              </div>
            </Col>
            <Col md={6}>
              <h6 className="mb-2">Members</h6>
              <div style={{ whiteSpace: 'pre-line', backgroundColor: '#f8f9fa', padding: '10px', borderRadius: '5px', fontSize: '0.9em' }}>
                {order.memberDetails || 'No member details available'}
              </div>
            </Col>
          </Row>
          
          <Row className="mb-3">
            <Col>
              <h6 className="mb-2">Items</h6>
              <Table responsive bordered size="sm">
                <thead>
                  <tr>
                    <th>Item</th>
                    <th>Customization</th>
                    <th>Qty</th>
                    <th>Price</th>
                    <th>Subtotal</th>
                  </tr>
                </thead>
                <tbody>
                  {(order.items || []).map((item: any, idx: number) => (
                    <tr key={idx}>
                      <td><strong>{item.name}</strong></td>
                      <td>
                        <small className="text-muted">
                          {item.customization?.flavor && <div>Flavor: {item.customization.flavor}</div>}
                          {item.customization?.dippingSauce && <div>Sauce: {item.customization.dippingSauce}</div>}
                          {item.customization?.drink && <div>Drink: {item.customization.drink}</div>}
                          {!item.customization?.flavor && !item.customization?.dippingSauce && !item.customization?.drink && 'None'}
                        </small>
                      </td>
                      <td className="text-center">{item.quantity}</td>
                      <td>RM {(item.price || 0).toFixed(2)}</td>
                      <td><strong>RM {(item.subtotal || 0).toFixed(2)}</strong></td>
                    </tr>
                  ))}
                  <tr className="table-active">
                    <td colSpan={4} className="text-end"><strong>TOTAL</strong></td>
                    <td><strong>RM {(order.total || 0).toFixed(2)}</strong></td>
                  </tr>
                </tbody>
              </Table>
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
              <i className="bi bi-printer me-2"></i>Print Receipts ({order.participants?.length || 0})
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
              color="info" 
              outline 
              onClick={() => {
                setSelectedOrder(order);
                setReceiptModalOpen(true);
              }}
            >
              <i className="bi bi-printer me-2"></i>
              Print Receipt
            </Button>
            <Button 
              color="warning" 
              outline 
              onClick={() => {
                setSelectedOrder(order);
                setStickerModalOpen(true);
              }}
            >
              <i className="bi bi-tag me-2"></i>
              Print Stickers
            </Button>
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
