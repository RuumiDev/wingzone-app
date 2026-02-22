import React, { useState, useEffect } from 'react';
import { Button, Card, CardBody, Row, Col, Badge, Alert } from 'reactstrap';
import { collection, doc, setDoc, getDoc } from 'firebase/firestore';
import { db } from '../lib/firebase';
import UniformLoader from '../components/UniformLoader/UniformLoader';

const FLAVORS = [
  'Buffalo Wing',
  'Sriracha Hot Chilli',
  'Soul of Seoul',
  'Garlic Parm',
  'Mambo Sauce',
  'Sweet Samurai',
  'Honey Q',
  'Blackened Voodoo',
  'Lemon Pepper',
  'Louisiana Smoked',
  'Spicy Alabama',
  'Tokyo Dragon',
  'Thai Chili',
  'Sweet Bombom',
  'Smokin Q'
];

const BEVERAGES = [
  'Coca-Cola',
  'Coke Zero',
  'Sprite',
  'Iced Lemon Tea',
  'Orange Juice'
];

const SIDES = [
  'Premium Wedge Fries',
  'Kettle Chips',
  'Smiley Fries',
  'Rice with Grilled Vege',
  'Flavor Rub Fries',
  'Sweet Potato Fries',
  'Mozzarella Stix',
  'Caesar Salad',
  'Garden Salad'
];

const DIPPING_SAUCES = [
  'Ranch',
  'Bleu Cheese'
];

const BONE_TYPES = [
  'Original',
  'Boneless'
];

interface AvailabilityState {
  flavors: string[];
  beverages: string[];
  sides: string[];
  dippingSauces: string[];
  boneTypes: string[];
}

const AvailabilityPage: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [success, setSuccess] = useState('');
  const [error, setError] = useState('');
  
  const [availability, setAvailability] = useState<AvailabilityState>({
    flavors: FLAVORS,
    beverages: BEVERAGES,
    sides: SIDES,
    dippingSauces: DIPPING_SAUCES,
    boneTypes: BONE_TYPES
  });

  useEffect(() => {
    loadAvailability(); 
  }, []);

  const loadAvailability = async () => {
    try {
      setLoading(true);
      const docRef = doc(db, 'settings', 'availability');
      const docSnap = await getDoc(docRef);
      
      if (docSnap.exists()) {
        const data = docSnap.data();
        const loadedAvailability = {
          flavors: data.flavors || FLAVORS,
          beverages: data.beverages || BEVERAGES,
          sides: data.sides || SIDES,
          dippingSauces: data.dippingSauces || DIPPING_SAUCES,
          boneTypes: data.boneTypes || BONE_TYPES
        };
        setAvailability(loadedAvailability);
        
        // If boneTypes field is missing, save it immediately with default values
        if (!data.boneTypes) {
          console.log('boneTypes field missing, initializing with defaults');
          await setDoc(docRef, loadedAvailability, { merge: true });
        }
      } else {
        // Document doesn't exist, create it with defaults
        console.log('Availability document does not exist, creating with defaults');
        await setDoc(docRef, availability);
      }
    } catch (err: any) {
      console.error('Error loading availability:', err);
      setError('Failed to load availability settings');
    } finally {
      setLoading(false);
    }
  };

  const saveAvailability = async () => {
    try {
      setSaving(true);
      setError('');
      setSuccess('');
      
      const docRef = doc(db, 'settings', 'availability');
      await setDoc(docRef, availability);
      
      setSuccess('Availability settings saved successfully!');
      setTimeout(() => setSuccess(''), 3000);
    } catch (err: any) {
      console.error('Error saving availability:', err);
      setError('Failed to save availability settings');
      setTimeout(() => setError(''), 3000);
    } finally {
      setSaving(false);
    }
  };

  const toggleItem = (category: keyof AvailabilityState, item: string) => {
    setAvailability(prev => {
      const items = prev[category];
      const newItems = items.includes(item)
        ? items.filter(i => i !== item)
        : [...items, item];
      console.log(`Toggling ${category} - ${item}:`, newItems);
      return { ...prev, [category]: newItems };
    });
    // Auto-save after toggle
    setTimeout(() => saveAvailability(), 500);
  };

  const toggleAll = (category: keyof AvailabilityState, allItems: string[], enable: boolean) => {
    setAvailability(prev => ({
      ...prev,
      [category]: enable ? allItems : []
    }));
  };

  const renderSection = (
    title: string,
    category: keyof AvailabilityState,
    allItems: string[],
    icon: string
  ) => {
    const availableItems = availability[category];
    const allEnabled = availableItems.length === allItems.length;
    const someEnabled = availableItems.length > 0 && !allEnabled;

    return (
      <Card className="mb-4">
        <CardBody>
          <div className="d-flex justify-content-between align-items-center mb-3">
            <h4 className="mb-0">
              <i className={`bi ${icon} me-2 text-primary`}></i>
              {title}
              <Badge color="secondary" className="ms-2">
                {availableItems.length}/{allItems.length}
              </Badge>
            </h4>
            <div>
              <Button
                color="success"
                size="sm"
                className="me-2"
                onClick={() => toggleAll(category, allItems, true)}
                disabled={allEnabled}
              >
                <i className="bi bi-check-all me-1"></i>
                Enable All
              </Button>
              <Button
                color="danger"
                size="sm"
                onClick={() => toggleAll(category, allItems, false)}
                disabled={availableItems.length === 0}
              >
                <i className="bi bi-x-circle me-1"></i>
                Disable All
              </Button>
            </div>
          </div>
          
          <Row>
            {allItems.map(item => {
              const isAvailable = availableItems.includes(item);
              return (
                <Col key={item} xs={12} sm={6} md={4} lg={3} className="mb-2">
                  <div
                    className={`p-3 rounded cursor-pointer border ${
                      isAvailable
                        ? 'border-success bg-success bg-opacity-10'
                        : 'border-danger bg-danger bg-opacity-10'
                    }`}
                    style={{ cursor: 'pointer', transition: 'all 0.2s' }}
                    onClick={() => toggleItem(category, item)}
                  >
                    <div className="d-flex align-items-center justify-content-between">
                      <span className={isAvailable ? 'text-success fw-bold' : 'text-danger'}>
                        {item}
                      </span>
                      <i
                        className={`bi ${
                          isAvailable ? 'bi-check-circle-fill text-success' : 'bi-x-circle-fill text-danger'
                        } fs-5`}
                      ></i>
                    </div>
                  </div>
                </Col>
              );
            })}
          </Row>
        </CardBody>
      </Card>
    );
  };

  if (loading) {
    return (
      <div>
        <h1 className="mb-4">Menu Availability</h1>
        <UniformLoader message="Loading menu items..." />
      </div>
    );
  }

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h1>Menu Availability</h1>
          <p className="text-muted mb-1">
            Manage individual availability for flavors, beverages, sides, and dipping sauces.
          </p>
          <p className="text-warning fw-bold mb-0">
            <i className="bi bi-exclamation-triangle me-2"></i>
            Disabling an item here will make it unavailable across ALL menu items that use it.
          </p>
        </div>
        <Button
          color="primary"
          size="lg"
          onClick={saveAvailability}
          disabled={saving}
        >
          {saving ? (
            <>
              <span className="spinner-border spinner-border-sm me-2"></span>
              Saving...
            </>
          ) : (
            <>
              <i className="bi bi-save me-2"></i>
              Save Changes
            </>
          )}
        </Button>
      </div>

      {error && (
        <Alert color="danger" className="mb-3" toggle={() => setError('')}>
          <i className="bi bi-exclamation-triangle me-2"></i>
          {error}
        </Alert>
      )}

      {success && (
        <Alert color="success" className="mb-3" toggle={() => setSuccess('')}>
          <i className="bi bi-check-circle me-2"></i>
          {success}
        </Alert>
      )}

      {renderSection('Bone Types (Wings)', 'boneTypes', BONE_TYPES, 'bi-egg-fried')}
      {renderSection('Flavors', 'flavors', FLAVORS, 'bi-fire')}
      {renderSection('Beverages', 'beverages', BEVERAGES, 'bi-cup-straw')}
      {renderSection('Sides', 'sides', SIDES, 'bi-basket')}
      {renderSection('Dipping Sauces', 'dippingSauces', DIPPING_SAUCES, 'bi-droplet')}
    </div>
  );
};

export default AvailabilityPage;
