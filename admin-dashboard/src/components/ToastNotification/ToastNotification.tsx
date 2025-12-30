import React, { useEffect } from 'react';
import s from './ToastNotification.module.scss';

interface ToastNotificationProps {
  show: boolean;
  title: string;
  message: string;
  type?: 'order' | 'success' | 'error' | 'info';
  onClose: () => void;
  duration?: number;
}

const ToastNotification: React.FC<ToastNotificationProps> = ({
  show,
  title,
  message,
  type = 'info',
  onClose,
  duration = 5000
}) => {
  useEffect(() => {
    if (show && duration > 0) {
      const timer = setTimeout(() => {
        onClose();
      }, duration);
      return () => clearTimeout(timer);
    }
  }, [show, duration, onClose]);

  if (!show) return null;

  const getIcon = () => {
    switch (type) {
      case 'order':
        return '🍗';
      case 'success':
        return '✅';
      case 'error':
        return '❌';
      default:
        return 'ℹ️';
    }
  };

  const getClassName = () => {
    return `${s.toast} ${s[type]} ${show ? s.show : ''}`;
  };

  return (
    <div className={getClassName()}>
      <div className={s.toastContent}>
        <div className={s.icon}>{getIcon()}</div>
        <div className={s.text}>
          <div className={s.title}>{title}</div>
          <div className={s.message}>{message}</div>
        </div>
        <button className={s.closeBtn} onClick={onClose}>
          <i className="bi bi-x-lg"></i>
        </button>
      </div>
      <div className={s.progressBar}>
        <div 
          className={s.progress} 
          style={{ animationDuration: `${duration}ms` }}
        />
      </div>
    </div>
  );
};

export default ToastNotification;
