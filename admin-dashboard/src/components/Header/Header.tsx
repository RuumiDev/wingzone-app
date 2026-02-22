import React from 'react';
import { Button } from 'reactstrap';
import NotificationDropdown from '../NotificationDropdown/NotificationDropdown';
import s from './Header.module.scss';
import Swal from 'sweetalert2';
import { showToast } from '../../utils/toast';

interface HeaderProps {
  sidebarToggle: () => void;
  onLogout?: () => void;
  onSettingsClick?: () => void;
  onViewOrder?: (orderId: string) => void;
}

const Header: React.FC<HeaderProps> = ({ sidebarToggle, onLogout, onSettingsClick, onViewOrder }) => {
  const handleLogoutClick = async () => {
    const result = await Swal.fire({
      title: 'Confirm Logout',
      text: 'Are you sure you want to logout?',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#dc3545',
      cancelButtonColor: '#6c757d',
      confirmButtonText: '<i class="bi bi-box-arrow-right"></i> Logout',
      cancelButtonText: 'Cancel',
      showLoaderOnConfirm: true,
      preConfirm: async () => {
        // Simulate logout process
        await new Promise(resolve => setTimeout(resolve, 800));
        if (onLogout) {
          onLogout();
        }
      },
      allowOutsideClick: () => !Swal.isLoading()
    });

    if (result.isConfirmed) {
      showToast('success', 'Logged out successfully!');
    }
  };

  return (
    <>
      <header className={s.root}>
        <div className={s.leftSection}>
          <Button
            color="link"
            className={s.sidebarToggle}
            onClick={sidebarToggle}
          >
            <i className="bi bi-list" />
          </Button>
          <h4 className={s.title}>WingZone Admin Dashboard</h4>
        </div>
        <div className={s.rightSection}>
          <NotificationDropdown onViewOrder={onViewOrder} />
          <Button color="link" className={s.iconButton} onClick={onSettingsClick}>
            <i className="bi bi-gear" />
          </Button>
          <Button 
            color="link" 
            className={s.logoutButton}
            onClick={handleLogoutClick}
          >
            <i className="bi bi-box-arrow-right" />
            Logout
          </Button>
        </div>
      </header>
    </>
  );
};

export default Header;
