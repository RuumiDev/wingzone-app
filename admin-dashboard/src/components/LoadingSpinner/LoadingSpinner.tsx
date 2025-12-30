import React from 'react';
import './LoadingSpinner.scss';

interface LoadingSpinnerProps {
  message?: string;
  fullPage?: boolean;
}

const LoadingSpinner: React.FC<LoadingSpinnerProps> = ({ 
  message = 'Loading...', 
  fullPage = true 
}) => {
  const content = (
    <div className="loading-content">
      <div className="spinner-wrapper">
        <div className="spinner">
          <div className="spinner-inner"></div>
          <div className="spinner-inner"></div>
          <div className="spinner-inner"></div>
        </div>
      </div>
      <p className="loading-message">{message}</p>
    </div>
  );

  if (fullPage) {
    return (
      <div className="loading-overlay">
        {content}
      </div>
    );
  }

  return <div className="loading-inline">{content}</div>;
};

export default LoadingSpinner;
