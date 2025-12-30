import React, { useState, useEffect } from 'react';
import { Button, Table, Badge, Modal, ModalHeader, ModalBody, ModalFooter, Form, FormGroup, Label, Input, Alert } from 'reactstrap';
import Widget from '../components/Widget/Widget';
import LoadingSpinner from '../components/LoadingSpinner';
import { menuService } from '../services/firebase';
import type { MenuItem } from '../types';

const CATEGORIES = [
  'Combo Meals',
  'Wings',
  'Tenders',
  'Burgers & Sandwiches',
  'Local Favorites',
  'Salads',
  'Sides',
  'Beverages'
];

// Wing Zone Official Flavors (from WingZoneMenu.md)
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

const BONE_TYPES = [
  'Original',
  'Boneless'
];

const MenuPage: React.FC = () => {
  const [menuItems, setMenuItems] = useState<MenuItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [modal, setModal] = useState(false);
  const [editItem, setEditItem] = useState<MenuItem | null>(null);
  const [formData, setFormData] = useState({
    name: '',
    price: '',
    category: 'Combo Meals',
    description: '',
    imageUrl: '',
    requiresCustomization: false,
    flavors: [] as string[],
    requiresBoneType: false,
    availableBoneTypes: [] as string[]
  });
  const [selectedCategory, setSelectedCategory] = useState<string>('All');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    // Subscribe to real-time menu items updates
    const unsubscribe = menuService.onMenuItemsChange((items) => {
      setMenuItems(items);
      setLoading(false);
    });

    return () => unsubscribe();
  }, []);

  const toggleModal = () => {
    setModal(!modal);
    if (!modal) {
      setEditItem(null);
      setFormData({
        name: '',
        price: '',
        category: 'Combo Meals',
        description: '',
        imageUrl: '',
        requiresCustomization: false,
        flavors: [],
        requiresBoneType: false,
        availableBoneTypes: []
      });
      setError('');
    }
  };

  const handleEdit = (item: MenuItem) => {
    setEditItem(item);
    setFormData({
      name: item.name,
      price: item.price.toString(),
      category: item.category,
      description: item.description || '',
      imageUrl: item.imageUrl || '',
      requiresCustomization: item.requiresCustomization || false,
      flavors: item.flavors || [],
      requiresBoneType: (item as any).customizationOptions?.requiresBoneType || false,
      availableBoneTypes: (item as any).customizationOptions?.availableBoneTypes || []
    });
    setModal(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    try {
      const itemData: any = {
        name: formData.name,
        price: parseFloat(formData.price),
        category: formData.category,
        description: formData.description,
        imageUrl: formData.imageUrl || undefined,
        requiresCustomization: formData.requiresCustomization,
        flavors: formData.flavors.length > 0 ? formData.flavors : undefined,
        isAvailable: editItem?.isAvailable ?? true,
        customizationOptions: {
          requiresBoneType: formData.requiresBoneType,
          availableBoneTypes: formData.availableBoneTypes.length > 0 ? formData.availableBoneTypes : undefined,
          availableFlavors: formData.flavors.length > 0 ? formData.flavors : undefined,
          requiresFlavor: formData.requiresCustomization && formData.flavors.length > 0,
          requiresDippingSauce: formData.requiresCustomization,
          requiresBeverage: formData.requiresCustomization
        }
      };

      if (editItem) {
        await menuService.updateMenuItem(editItem.id, itemData);
        setSuccess('Menu item updated successfully!');
      } else {
        await menuService.addMenuItem(itemData as Omit<MenuItem, 'id'>);
        setSuccess('Menu item added successfully!');
      }
      
      toggleModal();
      setTimeout(() => setSuccess(''), 3000);
    } catch (err: any) {
      setError(err.message || 'Failed to save menu item');
    }
  };

  const handleToggleAvailability = async (id: string, currentStatus: boolean) => {
    try {
      await menuService.updateMenuItem(id, { isAvailable: !currentStatus });
    } catch (err: any) {
      setError(err.message || 'Failed to update availability');
      setTimeout(() => setError(''), 3000);
    }
  };

  const handleFlavorToggle = (flavor: string) => {
    setFormData(prev => ({
      ...prev,
      flavors: prev.flavors.includes(flavor)
        ? prev.flavors.filter(f => f !== flavor)
        : [...prev.flavors, flavor]
    }));
  };

  const handleBoneTypeToggle = (boneType: string) => {
    setFormData(prev => ({
      ...prev,
      availableBoneTypes: prev.availableBoneTypes.includes(boneType)
        ? prev.availableBoneTypes.filter(b => b !== boneType)
        : [...prev.availableBoneTypes, boneType]
    }));
  };

  const handleDelete = async (id: string) => {
    if (!window.confirm('Are you sure you want to delete this menu item?')) return;
    
    try {
      await menuService.deleteMenuItem(id);
      setSuccess('Menu item deleted successfully!');
      setTimeout(() => setSuccess(''), 3000);
    } catch (err: any) {
      setError(err.message || 'Failed to delete menu item');
      setTimeout(() => setError(''), 3000);
    }
  };

  if (loading) {
    return <LoadingSpinner message="Loading menu items..." />;
  }

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h1>Menu Management</h1>
        <Button color="primary" size="lg" onClick={toggleModal}>
          <i className="bi bi-plus-circle me-2"></i>
          Add Menu Item
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

      {/* Category Filter */}
      <div className="mb-3">
        <Button
          color={selectedCategory === 'All' ? 'primary' : 'outline-primary'}
          size="sm"
          className="me-2"
          onClick={() => setSelectedCategory('All')}
        >
          All ({menuItems.length})
        </Button>
        {CATEGORIES.map(category => {
          const count = menuItems.filter(item => item.category === category).length;
          return (
            <Button
              key={category}
              color={selectedCategory === category ? 'primary' : 'outline-primary'}
              size="sm"
              className="me-2 mb-2"
              onClick={() => setSelectedCategory(category)}
            >
              {category} ({count})
            </Button>
          );
        })}
      </div>

      <Widget>
        {CATEGORIES.map(category => {
          let categoryItems = menuItems.filter(item => item.category === category);
          
          // Sort Combo Meals by entree number (Entree 1, Entree 2, ..., Entree 12)
          if (category === 'Combo Meals') {
            categoryItems = categoryItems.sort((a, b) => {
              const aMatch = a.name.match(/Entree (\d+)/);
              const bMatch = b.name.match(/Entree (\d+)/);
              if (aMatch && bMatch) {
                return parseInt(aMatch[1]) - parseInt(bMatch[1]);
              }
              return a.name.localeCompare(b.name);
            });
          }
          
          if (categoryItems.length === 0 || (selectedCategory !== 'All' && selectedCategory !== category)) return null;

          return (
            <div key={category} className="mb-4">
              <h4 className="mb-3 text-primary">{category}</h4>
              <Table responsive hover className="mb-4">
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Price</th>
                    <th>Description</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {categoryItems.map((item) => (
                    <tr key={item.id}>
                      <td><strong>{item.name}</strong></td>
                      <td>RM {item.price.toFixed(2)}</td>
                      <td className="text-muted small">{item.description || '-'}</td>
                      <td>
                        <Badge color={item.isAvailable ? 'success' : 'secondary'}>
                          {item.isAvailable ? 'Available' : 'Unavailable'}
                        </Badge>
                      </td>
                      <td>
                        <Button color="info" size="sm" className="me-2" onClick={() => handleEdit(item)}>
                          <i className="bi bi-pencil"></i>
                        </Button>
                        <Button
                          color={item.isAvailable ? 'warning' : 'success'}
                          size="sm"
                          className="me-2"
                          onClick={() => handleToggleAvailability(item.id, item.isAvailable)}
                        >
                          <i className={`bi bi-${item.isAvailable ? 'eye-slash' : 'eye'}`}></i>
                        </Button>
                        <Button color="danger" size="sm" onClick={() => handleDelete(item.id)}>
                          <i className="bi bi-trash"></i>
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </Table>
            </div>
          );
        })}
        {menuItems.length === 0 && (
          <div className="text-center text-muted py-5">
            No menu items found. Click "Add Menu Item" to get started.
          </div>
        )}
      </Widget>

      <Modal isOpen={modal} toggle={toggleModal}>
        <ModalHeader toggle={toggleModal}>
          {editItem ? 'Edit Menu Item' : 'Add Menu Item'}
        </ModalHeader>
        <Form onSubmit={handleSubmit}>
          <ModalBody>
            {error && (
              <Alert color="danger">
                <i className="bi bi-exclamation-triangle me-2"></i>
                {error}
              </Alert>
            )}
            <FormGroup>
              <Label for="name">Name *</Label>
              <Input 
                type="text" 
                id="name" 
                placeholder="Enter item name" 
                value={formData.name}
                onChange={(e) => setFormData({...formData, name: e.target.value})}
                required
              />
            </FormGroup>
            <FormGroup>
              <Label for="price">Price (RM) *</Label>
              <Input 
                type="number" 
                id="price" 
                placeholder="0.00" 
                step="0.01" 
                value={formData.price}
                onChange={(e) => setFormData({...formData, price: e.target.value})}
                required
              />
            </FormGroup>
            <FormGroup>
              <Label for="category">Category *</Label>
              <Input 
                type="select" 
                id="category"
                value={formData.category}
                onChange={(e) => setFormData({...formData, category: e.target.value})}
              >
                {CATEGORIES.map(cat => (
                  <option key={cat} value={cat}>{cat}</option>
                ))}
              </Input>
            </FormGroup>
            <FormGroup>
              <Label for="description">Description</Label>
              <Input 
                type="textarea" 
                id="description" 
                placeholder="Enter item description (optional)"
                value={formData.description}
                onChange={(e) => setFormData({...formData, description: e.target.value})}
                rows={3}
              />
            </FormGroup>
            <FormGroup>
              <Label for="imageUrl">Image URL</Label>
              <Input 
                type="text" 
                id="imageUrl" 
                placeholder="https://example.com/image.jpg (optional)"
                value={formData.imageUrl}
                onChange={(e) => setFormData({...formData, imageUrl: e.target.value})}
              />
              <small className="text-muted">
                Upload image to a hosting service and paste the URL here
              </small>
            </FormGroup>
            <FormGroup>
              <div className="d-flex align-items-center">
                <Input 
                  type="checkbox" 
                  id="requiresCustomization"
                  checked={formData.requiresCustomization}
                  onChange={(e) => setFormData({...formData, requiresCustomization: e.target.checked})}
                  className="me-2"
                />
                <Label for="requiresCustomization" className="mb-0">
                  Requires Customization (flavor, size, etc.)
                </Label>
              </div>
            </FormGroup>
            {formData.requiresCustomization && (
              <>
                <FormGroup>
                  <Label>Available Flavors</Label>
                  <div className="d-flex flex-wrap gap-2">
                    {FLAVORS.map(flavor => (
                      <Badge
                        key={flavor}
                        color={formData.flavors.includes(flavor) ? 'primary' : 'secondary'}
                        style={{ cursor: 'pointer', padding: '8px 12px' }}
                        onClick={() => handleFlavorToggle(flavor)}
                      >
                        {flavor}
                      </Badge>
                    ))}
                  </div>
                  <small className="text-muted mt-2 d-block">
                    Click to select/deselect flavors. Selected: {formData.flavors.length}
                  </small>
                </FormGroup>
                
                <FormGroup>
                  <div className="d-flex align-items-center mb-2">
                    <Input 
                      type="checkbox" 
                      id="requiresBoneType"
                      checked={formData.requiresBoneType}
                      onChange={(e) => setFormData({...formData, requiresBoneType: e.target.checked})}
                      className="me-2"
                    />
                    <Label for="requiresBoneType" className="mb-0">
                      Requires Bone Type Selection (for wings)
                    </Label>
                  </div>
                  {formData.requiresBoneType && (
                    <>
                      <Label className="mt-2">Available Bone Types</Label>
                      <div className="d-flex flex-wrap gap-2">
                        {BONE_TYPES.map(boneType => (
                          <Badge
                            key={boneType}
                            color={formData.availableBoneTypes.includes(boneType) ? 'success' : 'secondary'}
                            style={{ cursor: 'pointer', padding: '8px 12px' }}
                            onClick={() => handleBoneTypeToggle(boneType)}
                          >
                            {boneType}
                          </Badge>
                        ))}
                      </div>
                      <small className="text-muted mt-2 d-block">
                        Click to select/deselect bone types. Selected: {formData.availableBoneTypes.length}
                      </small>
                    </>
                  )}
                </FormGroup>
              </>
            )}
          </ModalBody>
          <ModalFooter>
            <Button type="button" color="secondary" onClick={toggleModal}>Cancel</Button>
            <Button type="submit" color="primary">{editItem ? 'Update' : 'Add'}</Button>
          </ModalFooter>
        </Form>
      </Modal>
    </div>
  );
};

export default MenuPage;
