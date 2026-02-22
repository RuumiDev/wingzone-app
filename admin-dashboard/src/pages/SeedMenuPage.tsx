import React, { useState } from 'react';
import { Button, Alert, Card, CardBody } from 'reactstrap';
import { seedMenuItems } from '../scripts/seedMenu';
import Swal from 'sweetalert2';
import { showToast } from '../utils/toast';

const SeedMenuPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState('');
  const [error, setError] = useState('');

  const handleSeed = async () => {
    const result = await Swal.fire({
      title: 'Reset Menu Items?',
      text: 'This will delete all existing menu items and add new ones. Continue?',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#dc3545',
      cancelButtonColor: '#6c757d',
      confirmButtonText: 'Yes, reset menu',
      cancelButtonText: 'Cancel'
    });

    if (!result.isConfirmed) {
      return;
    }

    setLoading(true);
    setError('');
    setSuccess('');

    try {
      const result = await seedMenuItems();
      setSuccess(`Successfully added ${result.count} menu items from WingZoneMenu.md!`);
    } catch (err: any) {
      setError(err.message || 'Failed to seed menu items');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <h1 className="mb-4">Seed Menu Database</h1>

      {success && (
        <Alert color="success" className="mb-3">
          <i className="bi bi-check-circle me-2"></i>
          {success}
        </Alert>
      )}

      {error && (
        <Alert color="danger" className="mb-3">
          <i className="bi bi-exclamation-triangle me-2"></i>
          {error}
        </Alert>
      )}

      <Card>
        <CardBody>
          <h5>Import Menu from WingZoneMenu.md</h5>
          <p className="text-muted mb-3">
            This will populate the database with all menu items from WingZoneMenu.md including:
          </p>
          <ul className="text-muted">
            <li><strong>12 Combo Meals</strong> - Entree 1-12 with drinks & sides</li>
            <li><strong>10 Wings</strong> - 5 to 100 pcs with flavor selection</li>
            <li><strong>5 Tenders</strong> - Chicken tenders with flavor options</li>
            <li><strong>7 Burgers & Sandwiches</strong> - A la carte items</li>
            <li><strong>6 Local Favorites</strong> - Flavorholic & drumsticks</li>
            <li><strong>6 Salads</strong> - Fresh garden & caesar salads</li>
            <li><strong>16 Sides</strong> - Fries, chips, rice, and more</li>
            <li><strong>5 Beverages</strong> - Coca-Cola, Coke Zero, Sprite, Iced Lemon Tea, Orange Juice</li>
          </ul>
          <p className="text-muted mb-2">
            <strong>Total: 67 menu items</strong> organized into 8 clear categories
          </p>
          <p className="text-warning mb-3">
            <i className="bi bi-exclamation-triangle me-2"></i>
            <strong>Warning:</strong> This will delete all existing menu items first!
          </p>
          <Button 
            color="primary" 
            size="lg" 
            onClick={handleSeed}
            disabled={loading}
          >
            {loading ? (
              <>
                <span className="spinner-border spinner-border-sm me-2"></span>
                Importing...
              </>
            ) : (
              <>
                <i className="bi bi-upload me-2"></i>
                Import Menu Items
              </>
            )}
          </Button>
        </CardBody>
      </Card>
    </div>
  );
};

export default SeedMenuPage;
