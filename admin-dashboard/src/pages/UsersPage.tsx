import React, { useState, useEffect } from 'react';
import { Table, Badge, Button, Alert } from 'reactstrap';
import Widget from '../components/Widget/Widget';
import { collection, getDocs, query, orderBy, onSnapshot } from 'firebase/firestore';
import { db } from '../lib/firebase';
import type { User } from '../types';

const UsersPage: React.FC = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    // Subscribe to real-time users updates
    const usersRef = collection(db, 'users');
    // Removed orderBy to avoid index requirements for documents without createdAt
    const q = query(usersRef);
    
    const unsubscribe = onSnapshot(
      q,
      (snapshot) => {
        const usersData = snapshot.docs.map(doc => {
          const data = doc.data();
          return {
            id: doc.id,
            email: data.email || '',
            name: data.name || data.displayName || 'Unknown',
            phoneNumber: data.phoneNumber,
            role: data.role || 'customer',
            createdAt: data.createdAt?.toDate() || new Date()
          } as User;
        });
        // Sort by createdAt descending on client side
        usersData.sort((a, b) => b.createdAt.getTime() - a.createdAt.getTime());
        setUsers(usersData);
        setLoading(false);
      },
      (err) => {
        console.error('Error fetching users:', err);
        setError(err.message || 'Failed to load users');
        setLoading(false);
      }
    );

    return () => unsubscribe();
  }, []);

  const formatDate = (date: Date) => {
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
        <h1 className="mb-4">Users Management</h1>
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
        <h1>Users Management</h1>
        <Badge color="info" className="fs-6 px-3 py-2">
          Total Users: {users.length}
        </Badge>
      </div>

      {error && (
        <Alert color="danger" className="mb-3" toggle={() => setError('')}>
          <i className="bi bi-exclamation-triangle me-2"></i>
          {error}
        </Alert>
      )}

      <Widget>
        <Table responsive hover>
          <thead>
            <tr>
              <th>Name</th>
              <th>Email</th>
              <th>Phone</th>
              <th>Role</th>
              <th>Registered</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {users.length === 0 ? (
              <tr>
                <td colSpan={6} className="text-center text-muted py-4">
                  No users found.
                </td>
              </tr>
            ) : (
              users.map((user) => (
                <tr key={user.id}>
                  <td><strong>{user.name}</strong></td>
                  <td>{user.email}</td>
                  <td>{user.phoneNumber || '-'}</td>
                  <td>
                    <Badge 
                      color={
                        user.role === 'admin' ? 'danger' :
                        user.role === 'kitchen' ? 'warning' :
                        'success'
                      }
                    >
                      {user.role}
                    </Badge>
                  </td>
                  <td>
                    <small className="text-muted">
                      {formatDate(user.createdAt)}
                    </small>
                  </td>
                  <td>
                    <Button 
                      color="info" 
                      size="sm" 
                      outline
                      onClick={() => {
                        // Future: View user details/orders
                        alert(`View details for ${user.name}`);
                      }}
                    >
                      <i className="bi bi-eye me-1"></i>
                      View Details
                    </Button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </Table>
      </Widget>

      <div className="mt-4">
        <Alert color="info">
          <i className="bi bi-info-circle me-2"></i>
          <strong>User Authentication Info:</strong>
          <ul className="mb-0 mt-2">
            <li>Users are automatically created when they sign up in the mobile app</li>
            <li>Authentication is handled by Firebase Authentication</li>
            <li>User profiles are stored in Firestore 'users' collection</li>
            <li>All new users receive a welcome bonus of 100 WZ Points</li>
          </ul>
        </Alert>
      </div>
    </div>
  );
};

export default UsersPage;
