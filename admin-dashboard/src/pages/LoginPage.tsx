import React, { useState } from 'react';
import { authService } from '../services/auth';
import { showToast } from '../utils/toast';

interface LoginPageProps {
  onLogin: () => void;
}

const LoginPage: React.FC<LoginPageProps> = ({ onLogin }) => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await authService.login(email, password);
      showToast('success', 'Successfully logged in!');
      onLogin();
    } catch (err) {
      setError('Invalid email or password');
      showToast('error', 'Login failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ display: 'flex', height: '100vh' }}>
      {/* Left Image Section - Hidden on mobile */}
      <div 
        style={{
          display: 'none',
          width: '50%',
          backgroundImage: 'url(https://images.unsplash.com/photo-1608039755401-742074f0548d?w=1200)',
          backgroundSize: 'cover',
          backgroundPosition: 'center',
          position: 'relative'
        }}
        className="desktop-only"
      >
        <div style={{
          position: 'absolute',
          inset: 0,
          background: 'rgba(0, 0, 0, 0.4)',
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'center',
          alignItems: 'center',
          padding: '40px',
          color: 'white'
        }}>
          <img src="/wingzone-logo.png" alt="WingZone" style={{ height: '100px', marginBottom: '24px' }} />
          <h1 style={{ fontSize: '2.5rem', fontWeight: 'bold', marginBottom: '16px', textAlign: 'center' }}>
            WingZone Admin Portal
          </h1>
          <p style={{ fontSize: '1.125rem', textAlign: 'center', maxWidth: '500px' }}>
            Manage your restaurant's menu, orders, and operations all in one place
          </p>
        </div>
      </div>

      {/* Right Form Section */}
      <div style={{
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center',
        alignItems: 'center',
        background: 'white',
        padding: '40px 20px'
      }}>
        <div style={{ width: '100%', maxWidth: '440px' }}>
          {/* Mobile Logo */}
          <div style={{ textAlign: 'center', marginBottom: '32px' }} className="mobile-logo">
            <img src="/wingzone-logo.png" alt="WingZone" style={{ height: '80px', marginBottom: '16px' }} />
          </div>

          <div style={{ marginBottom: '32px' }}>
            <h2 style={{ fontSize: '1.875rem', fontWeight: 'bold', color: '#111827', marginBottom: '8px' }}>
              Welcome back
            </h2>
            <p style={{ color: '#6b7280', fontSize: '0.875rem' }}>
              Sign in to access your admin dashboard
            </p>
          </div>

          {error && (
            <div style={{
              padding: '12px 16px',
              borderRadius: '8px',
              background: '#fee2e2',
              border: '1px solid #fecaca',
              color: '#991b1b',
              marginBottom: '24px',
              display: 'flex',
              alignItems: 'center',
              gap: '8px'
            }}>
              <i className="bi bi-exclamation-triangle"></i>
              <span style={{ fontSize: '0.875rem' }}>{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit}>
            <div style={{ marginBottom: '20px' }}>
              <label htmlFor="email" style={{
                display: 'block',
                fontSize: '0.875rem',
                fontWeight: '500',
                color: '#374151',
                marginBottom: '6px'
              }}>
                <i className="bi bi-envelope" style={{ marginRight: '6px' }}></i>
                Email Address
              </label>
              <input
                type="email"
                id="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="admin@wingzone.com"
                required
                style={{
                  width: '100%',
                  padding: '12px 16px',
                  fontSize: '1rem',
                  border: '1px solid #d1d5db',
                  borderRadius: '0.5rem',
                  outline: 'none',
                  transition: 'all 0.2s'
                }}
                onFocus={(e) => e.target.style.borderColor = '#ea580c'}
                onBlur={(e) => e.target.style.borderColor = '#d1d5db'}
              />
            </div>

            <div style={{ marginBottom: '24px' }}>
              <label htmlFor="password" style={{
                display: 'block',
                fontSize: '0.875rem',
                fontWeight: '500',
                color: '#374151',
                marginBottom: '6px'
              }}>
                <i className="bi bi-lock" style={{ marginRight: '6px' }}></i>
                Password
              </label>
              <input
                type="password"
                id="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Enter your password"
                required
                style={{
                  width: '100%',
                  padding: '12px 16px',
                  fontSize: '1rem',
                  border: '1px solid #d1d5db',
                  borderRadius: '0.5rem',
                  outline: 'none',
                  transition: 'all 0.2s'
                }}
                onFocus={(e) => e.target.style.borderColor = '#ea580c'}
                onBlur={(e) => e.target.style.borderColor = '#d1d5db'}
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              style={{
                width: '100%',
                padding: '14px 24px',
                fontSize: '1rem',
                fontWeight: '600',
                color: 'white',
                background: loading ? '#9ca3af' : '#ea580c',
                border: 'none',
                borderRadius: '0.5rem',
                cursor: loading ? 'not-allowed' : 'pointer',
                transition: 'all 0.2s',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '8px'
              }}
              onMouseEnter={(e) => !loading && (e.currentTarget.style.background = '#c2410c')}
              onMouseLeave={(e) => !loading && (e.currentTarget.style.background = '#ea580c')}
            >
              {loading ? (
                <>
                  <span className="spinner-border spinner-border-sm"></span>
                  Signing in...
                </>
              ) : (
                <>
                  <i className="bi bi-box-arrow-in-right"></i>
                  Sign In
                </>
              )}
            </button>
          </form>

          <p style={{
            textAlign: 'center',
            color: '#9ca3af',
            fontSize: '0.75rem',
            marginTop: '32px'
          }}>
            © 2025 WingZone. All rights reserved.
          </p>
        </div>
      </div>

      <style>{`
        @media (min-width: 768px) {
          .desktop-only {
            display: block !important;
          }
          .mobile-logo {
            display: none !important;
          }
        }
      `}</style>
    </div>
  );
};

export default LoginPage;
