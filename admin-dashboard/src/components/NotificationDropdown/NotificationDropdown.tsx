import React, { useState, useEffect } from 'react';
import { Dropdown, DropdownToggle, DropdownMenu, DropdownItem, Badge } from 'reactstrap';
import { collection, query, orderBy, onSnapshot, doc, updateDoc, deleteDoc, Timestamp, where } from 'firebase/firestore';
import { db } from '../../lib/firebase';
import s from './NotificationDropdown.module.scss';

interface Notification {
  id: string;
  type: 'order' | 'system' | 'info';
  title: string;
  message: string;
  orderId?: string;
  orderType?: 'individual' | 'group';
  orderTotal?: number;
  customerName?: string;
  createdAt: Timestamp;
  read: boolean;
}

interface NotificationDropdownProps {
  onViewOrder?: (orderId: string) => void;
}

const NotificationDropdown: React.FC<NotificationDropdownProps> = ({ onViewOrder }) => {
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [deletingIds, setDeletingIds] = useState<Set<string>>(new Set());

  useEffect(() => {
    // Listen to real-time notifications from Firebase
    const notificationsRef = collection(db, 'notifications');
    const q = query(notificationsRef, orderBy('createdAt', 'desc'));

    const unsubscribe = onSnapshot(q, (snapshot) => {
      const notifs = snapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data()
      } as Notification));
      setNotifications(notifs);
    });

    return () => unsubscribe();
  }, []);

  const toggle = () => setDropdownOpen(!dropdownOpen);

  const unreadCount = notifications.filter(n => !n.read).length;

  const handleMarkRead = async (id: string, e: React.MouseEvent) => {
    e.stopPropagation();
    try {
      const notifRef = doc(db, 'notifications', id);
      await updateDoc(notifRef, { read: true });
    } catch (error) {
      console.error('Error marking notification as read:', error);
    }
  };

  const handleMarkAllRead = async () => {
    try {
      const updates = notifications
        .filter(n => !n.read)
        .map(n => updateDoc(doc(db, 'notifications', n.id), { read: true }));
      await Promise.all(updates);
    } catch (error) {
      console.error('Error marking all as read:', error);
    }
  };

  const handleDelete = async (id: string, e: React.MouseEvent) => {
    e.stopPropagation();
    setDeletingIds(prev => new Set([...prev, id]));
    
    setTimeout(async () => {
      try {
        await deleteDoc(doc(db, 'notifications', id));
      } catch (error) {
        console.error('Error deleting notification:', error);
      }
      setDeletingIds(prev => {
        const newSet = new Set(prev);
        newSet.delete(id);
        return newSet;
      });
    }, 300); // Animation duration
  };

  const handleViewOrder = (orderId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    if (onViewOrder) {
      onViewOrder(orderId);
      setDropdownOpen(false);
    }
  };

  const getIcon = (type: string) => {
    switch (type) {
      case 'order': return 'bi-basket';
      case 'system': return 'bi-info-circle';
      default: return 'bi-bell';
    }
  };

  const getTimeAgo = (timestamp: Timestamp) => {
    const now = new Date();
    const notifDate = timestamp.toDate();
    const diffMs = now.getTime() - notifDate.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    
    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    const diffHours = Math.floor(diffMins / 60);
    if (diffHours < 24) return `${diffHours}h ago`;
    const diffDays = Math.floor(diffHours / 24);
    return `${diffDays}d ago`;
  };

  return (
    <Dropdown isOpen={dropdownOpen} toggle={toggle} className={s.notificationDropdown}>
      <DropdownToggle tag="button" className={`${s.toggleButton} ${unreadCount > 0 ? s.hasUnread : ''}`}>
        <i className="bi bi-bell" />
        {unreadCount > 0 && (
          <span className={s.badge}>
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </DropdownToggle>
      <DropdownMenu className={s.menu}>
        <div className={s.header}>
          <h6 className="mb-0">Notifications</h6>
          {unreadCount > 0 && (
            <button className={s.markAllBtn} onClick={handleMarkAllRead}>
              <i className="bi bi-check-all me-1"></i>
              Mark all read
            </button>
          )}
        </div>
        <div className={s.body}>
          {notifications.length === 0 ? (
            <div className={s.empty}>
              <i className="bi bi-bell-slash"></i>
              <p>No notifications</p>
            </div>
          ) : (
            notifications.map((notification) => (
              <div
                key={notification.id}
                className={`${s.item} ${!notification.read ? s.unread : ''} ${
                  deletingIds.has(notification.id) ? s.deleting : ''
                }`}
              >
                <div className={s.itemContent}>
                  <div 
                    className={s.iconWrapper}
                    onClick={(e) => !notification.read && handleMarkRead(notification.id, e)}
                  >
                    <i className={`${getIcon(notification.type)} ${s.icon}`}></i>
                  </div>
                  <div className={s.text}>
                    <div className={s.title}>
                      {notification.title}
                      {!notification.read && <span className={s.dot}></span>}
                    </div>
                    <div className={s.message}>{notification.message}</div>
                    {notification.orderId && (
                      <div className={s.orderInfo}>
                        <span className={s.orderId}>#{notification.orderId.substring(0, 8).toUpperCase()}</span>
                        {notification.orderType && (
                          <Badge 
                            color={notification.orderType === 'group' ? 'primary' : 'secondary'} 
                            className={s.orderTypeBadge}
                            style={{ fontSize: '0.7rem', padding: '2px 6px' }}
                          >
                            {notification.orderType === 'group' ? 'Group' : 'Individual'}
                          </Badge>
                        )}
                      </div>
                    )}
                    <div className={s.footer}>
                      <span className={s.time}>{getTimeAgo(notification.createdAt)}</span>
                      {notification.orderId && onViewOrder && (
                        <button 
                          className={s.viewBtn}
                          onClick={(e) => handleViewOrder(notification.orderId!, e)}
                          title="View Order"
                        >
                          <i className="bi bi-eye"></i> View
                        </button>
                      )}
                    </div>
                  </div>
                  <button 
                    className={s.deleteBtn}
                    onClick={(e) => handleDelete(notification.id, e)}
                    title="Delete notification"
                  >
                    <i className="bi bi-x-lg"></i>
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      </DropdownMenu>
    </Dropdown>
  );
};

export default NotificationDropdown;
