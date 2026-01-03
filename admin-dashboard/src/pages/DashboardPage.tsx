import React, { useEffect, useState } from 'react';
import { Row, Col, Table, Button, Badge } from 'reactstrap';
import Widget from '../components/Widget/Widget';
import { dashboardService, individualOrdersService } from '../services/firebase';
import s from './DashboardPage.module.scss';

interface DashboardStats {
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

interface DashboardPageProps {
  onNavigate?: (page: string) => void;
}

const DashboardPage: React.FC<DashboardPageProps> = ({ onNavigate }) => {
  const [stats, setStats] = useState<DashboardStats>({
    todayOrders: 0,
    todayRevenue: 0,
    pendingOrders: 0,
    activeGroupOrders: 0,
    recentOrders: []
  });
  const [individualOrdersCount, setIndividualOrdersCount] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Subscribe to real-time stats updates
    const unsubscribeStats = dashboardService.onStatsChange((newStats) => {
      setStats(newStats);
      setLoading(false);
    });
    
    // Subscribe to individual orders count
    const unsubscribeIndividual = individualOrdersService.onOrdersChange((orders) => {
      setIndividualOrdersCount(orders.length);
    });

    return () => {
      unsubscribeStats();
      unsubscribeIndividual();
    };
  }, []);

  const statsCards = [
    { label: "Today's Orders", value: stats.todayOrders.toString(), icon: 'bi-bag-check', color: 'primary' },
    { label: "Today's Revenue", value: `RM ${stats.todayRevenue.toFixed(2)}`, icon: 'bi-cash-stack', color: 'success' },
    { label: 'Pending Orders', value: stats.pendingOrders.toString(), icon: 'bi-clock-history', color: 'warning' },
    { label: 'Active Group Orders', value: stats.activeGroupOrders.toString(), icon: 'bi-people', color: 'info' },
  ];

  const getStatusColor = (status: string) => {
    const colors: Record<string, string> = {
      delivered: 'success',
      preparing: 'info',
      pending: 'warning',
      cancelled: 'danger',
    };
    return colors[status] || 'secondary';
  };

  if (loading) {
    return (
      <div className={s.root}>
        <h1 className="mb-4">Dashboard</h1>
        <div className="text-center py-5">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Loading...</span>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className={s.root}>
      <h1 className="mb-4">Dashboard</h1>

      {/* Stats Cards */}
      <Row>
        {statsCards.map((stat, idx) => (
          <Col key={idx} lg={3} md={6} sm={12} className="mb-4">
            <Widget>
              <div className={s.statsCard}>
                <div className={s.statsIcon}>
                  <i className={`bi ${stat.icon}`} style={{ color: `var(--bs-${stat.color})` }}></i>
                </div>
                <div className={s.statsContent}>
                  <div className={s.statsValue}>{stat.value}</div>
                  <div className={s.statsLabel}>{stat.label}</div>
                </div>
              </div>
            </Widget>
          </Col>
        ))}
      </Row>

      {/* Recent Orders */}
      <Row>
        <Col lg={12}>
          <Widget title={<><i className="bi bi-list-check me-2"></i>Recent Orders</>}>
            <Table responsive hover className="mb-0">
              <thead>
                <tr>
                  <th>Order ID</th>
                  <th>Items</th>
                  <th>Amount</th>
                  <th>Status</th>
                  <th>Time</th>
                </tr>
              </thead>
              <tbody>
                {stats.recentOrders.map((order) => (
                  <tr key={order.id}>
                    <td><strong>{order.id}</strong></td>
                    <td>{order.items}</td>
                    <td><strong>RM {order.amount.toFixed(2)}</strong></td>
                    <td><Badge color={getStatusColor(order.status)}>{order.status}</Badge></td>
                    <td>{order.time}</td>
                  </tr>
                ))}
              </tbody>
            </Table>
          </Widget>
        </Col>
      </Row>

      {/* Quick Actions */}
      <Row>
        <Col>
          <Widget title={<><i className="bi bi-lightning me-2"></i>Quick Actions</>}>
            <div className={s.quickActions}>
              <Button color="primary" size="lg" onClick={() => onNavigate?.('menu')}>
                <i className="bi bi-plus-circle me-2"></i>
                New Menu Item
              </Button>
              <Button color="success" size="lg" onClick={() => onNavigate?.('orders')}>
                <i className="bi bi-list-check me-2"></i>
                View All Orders ({individualOrdersCount})
              </Button>
            </div>
          </Widget>
        </Col>
      </Row>
    </div>
  );
};

export default DashboardPage;


