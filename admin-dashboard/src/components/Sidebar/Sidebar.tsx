import React from 'react';
import s from './Sidebar.module.scss';

interface SidebarProps {
  activePage?: string;
  onNavigate?: (page: string) => void;
}

const Sidebar: React.FC<SidebarProps> = ({ activePage = 'dashboard', onNavigate }) => {
  const menuItems = [
    { id: 'dashboard', icon: 'bi-speedometer2', label: 'Dashboard' },
    { id: 'menu', icon: 'bi-card-list', label: 'Menu Management' },
    { id: 'availability', icon: 'bi-toggles', label: 'Availability' },
    { id: 'orders', icon: 'bi-bag-check', label: 'Orders' },
    { id: 'users', icon: 'bi-people', label: 'Users' },
    { id: 'settings', icon: 'bi-gear', label: 'Settings' },
    { id: 'seed', icon: 'bi-upload', label: 'Import Menu' },
  ];

  return (
    <nav className={s.root}>
      <header className={s.logo}>
        <img src="/wingzone-logo.png" alt="WingZone" />
        <span className={s.logoText}>WingZone Admin</span>
      </header>
      <ul className={s.nav}>
        {menuItems.map((item) => (
          <li
            key={item.id}
            className={activePage === item.id ? s.active : ''}
            onClick={() => onNavigate?.(item.id)}
          >
            <i className={`bi ${item.icon}`}></i>
            <span>{item.label}</span>
          </li>
        ))}
      </ul>
      <div className={s.footer}>
        <div className={s.user}>
          <i className="bi bi-person-circle"></i>
          <div className={s.userInfo}>
            <span className={s.userName}>Administrator</span>
            <span className={s.userEmail}>admin@wingzone.com</span>
          </div>
        </div>
      </div>
    </nav>
  );
};

export default Sidebar;
