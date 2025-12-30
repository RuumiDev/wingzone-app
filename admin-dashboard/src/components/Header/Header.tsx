import React, { useState } from 'react';
import { Button, Modal, ModalHeader, ModalBody, ModalFooter } from 'reactstrap';
import NotificationDropdown from '../NotificationDropdown/NotificationDropdown';
import s from './Header.module.scss';

interface HeaderProps {
  sidebarToggle: () => void;
  onLogout?: () => void;
  onSettingsClick?: () => void;
  onViewOrder?: (orderId: string) => void;
}

const Header: React.FC<HeaderProps> = ({ sidebarToggle, onLogout, onSettingsClick, onViewOrder }) => {
  const [showLogoutModal, setShowLogoutModal] = useState(false);
  const [isLoggingOut, setIsLoggingOut] = useState(false);

  const handleLogoutClick = () => {
    setShowLogoutModal(true);
  };

  const handleConfirmLogout = async () => {
    setIsLoggingOut(true);
    // Simulate logout process
    await new Promise(resolve => setTimeout(resolve, 800));
    if (onLogout) {
      onLogout();
    }
    setIsLoggingOut(false);
    setShowLogoutModal(false);
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

      {/* Logout Confirmation Modal */}
      <Modal isOpen={showLogoutModal} toggle={() => !isLoggingOut && setShowLogoutModal(false)} centered>
        <ModalHeader toggle={() => !isLoggingOut && setShowLogoutModal(false)}>
          <i className="bi bi-question-circle-fill me-2 text-warning"></i>
          Confirm Logout
        </ModalHeader>
        <ModalBody>
          <p className="mb-0">Are you sure you want to logout?</p>
        </ModalBody>
        <ModalFooter>
          <Button 
            color="secondary" 
            onClick={() => setShowLogoutModal(false)}
            disabled={isLoggingOut}
          >
            Cancel
          </Button>
          <Button 
            color="danger" 
            onClick={handleConfirmLogout}
            disabled={isLoggingOut}
          >
            {isLoggingOut ? (
              <>
                <span className="spinner-border spinner-border-sm me-2"></span>
                Logging out...
              </>
            ) : (
              <>
                <i className="bi bi-box-arrow-right me-2"></i>
                Logout
              </>
            )}
          </Button>
        </ModalFooter>
      </Modal>
    </>
  );
};

export default Header;
