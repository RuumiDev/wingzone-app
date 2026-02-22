import React from 'react';
import { useGSAP } from '@gsap/react';
import gsap from 'gsap';

interface UniformLoaderProps {
  message?: string;
}

const UniformLoader: React.FC<UniformLoaderProps> = ({ message = 'Loading...' }) => {
  useGSAP(() => {
    gsap.to('.loader-dot', {
      y: -20,
      stagger: 0.15,
      repeat: -1,
      yoyo: true,
      ease: 'power1.inOut',
      duration: 0.6
    });
  }, []);

  const containerStyle: React.CSSProperties = {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: '200px',
    padding: '20px'
  };

  const dotsContainerStyle: React.CSSProperties = {
    display: 'flex',
    gap: '12px',
    marginBottom: '16px'
  };

  const dotStyle: React.CSSProperties = {
    width: '12px',
    height: '12px',
    borderRadius: '50%',
    background: 'linear-gradient(135deg, #dc2626 0%, #b91c1c 100%)',
    boxShadow: '0 2px 8px rgba(220, 38, 38, 0.4)'
  };

  const messageStyle: React.CSSProperties = {
    color: '#6b7280',
    fontWeight: 500,
    fontSize: '14px',
    marginTop: '8px'
  };

  return (
    <div style={containerStyle}>
      <div style={dotsContainerStyle}>
        <div className="loader-dot" style={dotStyle} />
        <div className="loader-dot" style={dotStyle} />
        <div className="loader-dot" style={dotStyle} />
      </div>
      <p style={messageStyle}>{message}</p>
    </div>
  );
};

export default UniformLoader;
