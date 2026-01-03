import React, { useState, useEffect, useRef } from 'react';
import { Button, Table, Badge, Modal, ModalHeader, ModalBody, ModalFooter, Form, FormGroup, Label, Input, Alert } from 'reactstrap';
import Widget from '../components/Widget/Widget';
import LoadingSpinner from '../components/LoadingSpinner';
import { menuService } from '../services/firebase';
import { ref, uploadBytes, getDownloadURL } from 'firebase/storage';
import { storage } from '../lib/firebase';
import type { MenuItem } from '../types';
import ReactCrop, { type Crop, type PixelCrop } from 'react-image-crop';
import 'react-image-crop/dist/ReactCrop.css';

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
    displayOrder: '',
    ingredients: [] as Array<{ type: string; quantity: number; requiresSelection: boolean }>,
    requiresCustomization: false,
    allowFriesExchange: false,
    flavors: [] as string[],
    requiresBoneType: false,
    availableBoneTypes: [] as string[],
    requiresSaladChoice: false
  });
  const [selectedCategory, setSelectedCategory] = useState<string>('All');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [uploadingImage, setUploadingImage] = useState(false);
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [imagePreview, setImagePreview] = useState<string>('');
  const [cropModal, setCropModal] = useState(false);
  const [crop, setCrop] = useState<Crop>();
  const [completedCrop, setCompletedCrop] = useState<PixelCrop>();
  const [imageToCrop, setImageToCrop] = useState<string>('');
  const imgRef = useRef<HTMLImageElement>(null);
  const [imageValidation, setImageValidation] = useState<{valid: boolean; message: string}>({valid: true, message: ''});

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
        displayOrder: '',
        ingredients: [],
        requiresCustomization: false,
        allowFriesExchange: false,
        flavors: [],
        requiresBoneType: false,
        availableBoneTypes: [],
        requiresSaladChoice: false
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
      displayOrder: item.displayOrder?.toString() || '',
      ingredients: item.kitchenIngredients?.ingredients || [],
      requiresCustomization: item.requiresCustomization || false,
      allowFriesExchange: item.customizationOptions?.allowFriesExchange || false,
      flavors: item.customizationOptions?.availableFlavors || [],
      requiresBoneType: item.customizationOptions?.requiresBoneType || false,
      availableBoneTypes: item.customizationOptions?.availableBoneTypes || [],
      requiresSaladChoice: item.customizationOptions?.requiresSaladChoice || false
    });
    setModal(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    try {
      const kitchenIngredients: any = {};
      if (formData.ingredients.length > 0) {
        kitchenIngredients.ingredients = formData.ingredients;
      }

      const customizationOptions: any = {
        requiresBoneType: formData.requiresBoneType,
        allowFriesExchange: formData.allowFriesExchange,
        requiresFlavor: formData.requiresCustomization && formData.flavors.length > 0,
        requiresDippingSauce: formData.requiresCustomization,
        requiresBeverage: formData.requiresCustomization,
        requiresSaladChoice: formData.requiresSaladChoice,
      };

      // Only add fields that have values (avoid undefined)
      if (formData.availableBoneTypes.length > 0) {
        customizationOptions.availableBoneTypes = formData.availableBoneTypes;
      }
      if (formData.flavors.length > 0) {
        customizationOptions.availableFlavors = formData.flavors;
      }
      if (formData.allowFriesExchange) {
        customizationOptions.friesExchanges = [
          { name: 'Premium Wedge Fries', regularPrice: 0, jumboPrice: 8.00 },
          { name: 'Kettle Chips', regularPrice: 0, jumboPrice: 8.00 },
          { name: 'Smiley Fries', regularPrice: 0, jumboPrice: null },
          { name: 'Rice with Grilled Vege', regularPrice: 0, jumboPrice: null },
          { name: 'Flavor Rub Fries', regularPrice: 5.00, jumboPrice: 10.00 },
          { name: 'Sweet Potato Fries', regularPrice: 5.00, jumboPrice: 12.00 },
          { name: 'Mozzarella Stix', regularPrice: 11.00, jumboPrice: null },
          { name: 'Caesar Salad', regularPrice: 14.00, jumboPrice: null },
          { name: 'Garden Salad', regularPrice: 14.00, jumboPrice: null }
        ];
      }

      const itemData: any = {
        name: formData.name,
        price: parseFloat(formData.price),
        category: formData.category,
        description: formData.description,
        requiresCustomization: formData.requiresCustomization,
        isAvailable: editItem?.isAvailable ?? true,
        customizationOptions
      };

      // Only add optional fields if they have values
      if (formData.imageUrl) {
        itemData.imageUrl = formData.imageUrl;
      }
      if (formData.displayOrder) {
        itemData.displayOrder = parseInt(formData.displayOrder);
      }
      if (formData.ingredients.length > 0) {
        itemData.kitchenIngredients = kitchenIngredients;
      }
      if (formData.flavors.length > 0) {
        itemData.flavors = formData.flavors;
      }

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

  const handleImageFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // Validate file type
    if (!file.type.startsWith('image/')) {
      setError('Please select an image file (JPG, PNG, GIF, WebP)');
      return;
    }

    // Validate file size (max 5MB)
    if (file.size > 5 * 1024 * 1024) {
      setError('Image size must be less than 5MB');
      return;
    }

    // Create reader for crop modal
    const reader = new FileReader();
    reader.onloadend = () => {
      const img = new Image();
      img.onload = () => {
        // Check image dimensions
        const width = img.width;
        const height = img.height;
        
        // Provide feedback on image quality
        let validation = {valid: true, message: ''};
        if (width < 400 || height < 400) {
          validation = {valid: false, message: '⚠️ Image resolution is low. Recommended minimum: 800x600px for best quality.'};
        } else if (width < 800 || height < 600) {
          validation = {valid: true, message: '⚠️ Image resolution is acceptable but may appear slightly blurry. Recommended: 800x600px or higher.'};
        } else {
          validation = {valid: true, message: '✓ Image resolution is good for menu display.'};
        }
        
        setImageValidation(validation);
        setImageToCrop(reader.result as string);
        setImageFile(file);
        setCropModal(true);
        
        // Set default crop to 4:3 aspect ratio
        const aspect = 4 / 3;
        const cropWidth = Math.min(width, height * aspect);
        const cropHeight = cropWidth / aspect;
        setCrop({
          unit: '%',
          x: ((width - cropWidth) / width) * 50,
          y: ((height - cropHeight) / height) * 50,
          width: (cropWidth / width) * 100,
          height: (cropHeight / height) * 100
        });
      };
      img.src = reader.result as string;
    };
    reader.readAsDataURL(file);
  };

  const getCroppedImg = async (image: HTMLImageElement, crop: PixelCrop): Promise<Blob> => {
    const canvas = document.createElement('canvas');
    const scaleX = image.naturalWidth / image.width;
    const scaleY = image.naturalHeight / image.height;
    
    // Recommend 800x600 for optimal display
    const targetWidth = 800;
    const targetHeight = 600;
    
    canvas.width = targetWidth;
    canvas.height = targetHeight;
    const ctx = canvas.getContext('2d');

    if (!ctx) {
      throw new Error('No 2d context');
    }

    ctx.imageSmoothingQuality = 'high';
    ctx.drawImage(
      image,
      crop.x * scaleX,
      crop.y * scaleY,
      crop.width * scaleX,
      crop.height * scaleY,
      0,
      0,
      targetWidth,
      targetHeight
    );

    return new Promise((resolve, reject) => {
      canvas.toBlob(
        (blob) => {
          if (!blob) {
            reject(new Error('Canvas is empty'));
            return;
          }
          resolve(blob);
        },
        'image/jpeg',
        0.95
      );
    });
  };

  const handleCropComplete = async () => {
    if (!completedCrop || !imgRef.current) {
      setError('Please select a crop area');
      return;
    }

    try {
      const croppedBlob = await getCroppedImg(imgRef.current, completedCrop);
      const croppedFile = new File([croppedBlob], imageFile?.name || 'cropped-image.jpg', {
        type: 'image/jpeg'
      });
      
      // Create preview
      const reader = new FileReader();
      reader.onloadend = () => {
        setImagePreview(reader.result as string);
      };
      reader.readAsDataURL(croppedFile);
      
      setImageFile(croppedFile);
      setCropModal(false);
      setImageToCrop('');
    } catch (err: any) {
      setError(`Failed to crop image: ${err.message}`);
    }
  };

  const handleUploadImage = async () => {
    if (!imageFile) return;

    setUploadingImage(true);
    setError('');

    try {
      // Upload to Firebase Storage
      const timestamp = Date.now();
      const fileName = `${timestamp}-${imageFile.name}`;
      const storageRef = ref(storage, `menu-images/${fileName}`);
      
      await uploadBytes(storageRef, imageFile);
      const downloadUrl = await getDownloadURL(storageRef);

      // Update form data
      setFormData(prev => ({ ...prev, imageUrl: downloadUrl }));
      setSuccess('Image uploaded successfully!');
      setTimeout(() => setSuccess(''), 3000);
      
      // Clear the file input
      setImageFile(null);
      setImagePreview('');
    } catch (err: any) {
      setError(`Failed to upload image: ${err.message}`);
    } finally {
      setUploadingImage(false);
    }
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
          
          // Sort Wings by displayOrder (just wings first, then with fries)
          if (category === 'Wings') {
            categoryItems = categoryItems.sort((a, b) => {
              // If both have displayOrder, use it
              if (a.displayOrder !== undefined && b.displayOrder !== undefined) {
                return a.displayOrder - b.displayOrder;
              }
              // If only one has displayOrder, prioritize it
              if (a.displayOrder !== undefined) return -1;
              if (b.displayOrder !== undefined) return 1;
              // Otherwise sort alphabetically
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
              <Label for="imageUrl"><strong>Menu Item Image</strong></Label>
              
              {/* Image Requirements */}
              <Alert color="info" className="mb-3">
                <strong><i className="bi bi-info-circle me-2"></i>Image Guidelines:</strong>
                <ul className="mb-0 mt-2 small">
                  <li><strong>Recommended Size:</strong> 800x600px or higher (4:3 aspect ratio)</li>
                  <li><strong>Minimum Size:</strong> 400x300px (may appear blurry)</li>
                  <li><strong>File Size:</strong> Up to 5MB</li>
                  <li><strong>Format:</strong> JPG, PNG, GIF, or WebP</li>
                  <li><strong>Tip:</strong> High-resolution images will be automatically optimized to 800x600px</li>
                </ul>
              </Alert>
              
              {/* Image Upload Option */}
              <div className="border rounded p-3 mb-3 bg-light">
                <Label className="mb-2">
                  <i className="bi bi-upload me-2"></i>
                  <strong>Upload Image with Crop</strong>
                </Label>
                <Input 
                  type="file" 
                  accept="image/*"
                  onChange={handleImageFileSelect}
                  disabled={uploadingImage}
                  className="mb-2"
                />
                {imagePreview && (
                  <div className="mt-2 mb-2">
                    <Label className="small text-success mb-1">
                      <i className="bi bi-check-circle me-1"></i>
                      Cropped & Ready to Upload
                    </Label>
                    <img 
                      src={imagePreview} 
                      alt="Preview" 
                      style={{ maxWidth: '200px', maxHeight: '150px', objectFit: 'cover' }}
                      className="border rounded d-block"
                    />
                  </div>
                )}
                {imageFile && (
                  <Button 
                    color="primary" 
                    size="sm"
                    onClick={handleUploadImage}
                    disabled={uploadingImage}
                  >
                    {uploadingImage ? (
                      <>
                        <i className="bi bi-hourglass-split me-2"></i>
                        Uploading...
                      </>
                    ) : (
                      <>
                        <i className="bi bi-cloud-upload me-2"></i>
                        Upload Image
                      </>
                    )}
                  </Button>
                )}
                <small className="d-block text-muted mt-2">
                  Select an image to crop and optimize before uploading
                </small>
              </div>

              {/* Or paste URL option */}
              <div className="border rounded p-3">
                <Label className="mb-2">
                  <i className="bi bi-link-45deg me-2"></i>
                  Or Paste Image URL
                </Label>
                <Input 
                  type="text" 
                  id="imageUrl" 
                  placeholder="https://example.com/image.jpg"
                  value={formData.imageUrl}
                  onChange={(e) => setFormData({...formData, imageUrl: e.target.value})}
                />
                <small className="text-muted d-block mt-2">
                  If you already have an image URL, paste it here
                </small>
              </div>

              {/* Current image preview */}
              {formData.imageUrl && !imagePreview && (
                <div className="mt-3">
                  <Label className="small text-muted">Current Image:</Label>
                  <div>
                    <img 
                      src={formData.imageUrl} 
                      alt="Current menu item" 
                      style={{ maxWidth: '200px', maxHeight: '200px', objectFit: 'cover' }}
                      className="border rounded"
                      onError={(e) => {
                        (e.target as HTMLImageElement).style.display = 'none';
                      }}
                    />
                  </div>
                </div>
              )}
            </FormGroup>
            <FormGroup>
              <Label for="displayOrder">Display Order</Label>
              <Input 
                type="number" 
                id="displayOrder" 
                placeholder="e.g., 1, 2, 3... (optional)"
                value={formData.displayOrder}
                onChange={(e) => setFormData({...formData, displayOrder: e.target.value})}
              />
              <small className="text-muted">
                Order in which items appear (lower numbers first). Leave empty for alphabetical order.
              </small>
            </FormGroup>
            <FormGroup>
              <Label><strong>Kitchen Ingredients (for receipt summary)</strong></Label>
              <small className="d-block text-muted mb-3">
                Add ingredients that will be tracked in kitchen summaries. Use type tags like "wings", "fries", "bread", "tenders", "salad", etc.
              </small>
              
              {formData.ingredients.map((ingredient, index) => (
                <div key={index} className="border rounded p-3 mb-2 bg-light">
                  <div className="row align-items-end">
                    <div className="col-md-4">
                      <Label className="small">Type</Label>
                      <Input 
                        type="text" 
                        placeholder="e.g., wings, fries, tenders"
                        value={ingredient.type}
                        onChange={(e) => {
                          const newIngredients = [...formData.ingredients];
                          newIngredients[index].type = e.target.value;
                          setFormData({...formData, ingredients: newIngredients});
                        }}
                      />
                    </div>
                    <div className="col-md-3">
                      <Label className="small">Quantity</Label>
                      <Input 
                        type="number" 
                        placeholder="e.g., 6"
                        value={ingredient.quantity}
                        onChange={(e) => {
                          const newIngredients = [...formData.ingredients];
                          newIngredients[index].quantity = parseInt(e.target.value) || 0;
                          setFormData({...formData, ingredients: newIngredients});
                        }}
                      />
                    </div>
                    <div className="col-md-3">
                      <div className="d-flex align-items-center">
                        <Input 
                          type="checkbox" 
                          checked={ingredient.requiresSelection}
                          onChange={(e) => {
                            const newIngredients = [...formData.ingredients];
                            newIngredients[index].requiresSelection = e.target.checked;
                            setFormData({...formData, ingredients: newIngredients});
                          }}
                          className="me-2"
                        />
                        <Label className="small mb-0">
                          Requires Selection<br/>
                          <span className="text-muted" style={{fontSize: '0.75rem'}}>(e.g., bone type)</span>
                        </Label>
                      </div>
                    </div>
                    <div className="col-md-2">
                      <Button 
                        color="danger" 
                        size="sm"
                        onClick={() => {
                          const newIngredients = formData.ingredients.filter((_, i) => i !== index);
                          setFormData({...formData, ingredients: newIngredients});
                        }}
                      >
                        Remove
                      </Button>
                    </div>
                  </div>
                </div>
              ))}
              
              <Button 
                color="primary" 
                outline 
                size="sm"
                onClick={() => {
                  setFormData({
                    ...formData, 
                    ingredients: [...formData.ingredients, { type: '', quantity: 0, requiresSelection: false }]
                  });
                }}
              >
                <i className="bi bi-plus-circle me-1"></i>
                Add Ingredient
              </Button>
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
            <FormGroup>
              <div className="d-flex align-items-center">
                <Input 
                  type="checkbox" 
                  id="allowFriesExchange"
                  checked={formData.allowFriesExchange}
                  onChange={(e) => setFormData({...formData, allowFriesExchange: e.target.checked})}
                  className="me-2"
                />
                <Label for="allowFriesExchange" className="mb-0">
                  Allow Sides Exchange (for wings with fries)
                </Label>
              </div>
            </FormGroup>
            <FormGroup>
              <div className="d-flex align-items-center">
                <Input 
                  type="checkbox" 
                  id="requiresSaladChoice"
                  checked={formData.requiresSaladChoice}
                  onChange={(e) => setFormData({...formData, requiresSaladChoice: e.target.checked})}
                  className="me-2"
                />
                <Label for="requiresSaladChoice" className="mb-0">
                  Requires Salad Choice (Garden or Caesar Salad)
                </Label>
              </div>
              <small className="text-muted">Use this for items like Entree 9 that include salad selection</small>
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

      {/* Crop Modal */}
      <Modal isOpen={cropModal} toggle={() => setCropModal(false)} size="lg">
        <ModalHeader toggle={() => setCropModal(false)}>
          <i className="bi bi-crop me-2"></i>
          Crop & Optimize Image
        </ModalHeader>
        <ModalBody>
          {imageValidation.message && (
            <Alert color={imageValidation.valid ? 'success' : 'warning'} className="mb-3">
              {imageValidation.message}
            </Alert>
          )}
          
          <div className="mb-3">
            <Alert color="info" className="small">
              <strong><i className="bi bi-scissors me-2"></i>Cropping Tips:</strong>
              <ul className="mb-0 mt-2">
                <li>Drag corners to adjust crop area</li>
                <li>Recommended aspect ratio: 4:3 (landscape)</li>
                <li>Final image will be optimized to 800x600px</li>
                <li>Focus on the main subject of your dish</li>
              </ul>
            </Alert>
          </div>

          {imageToCrop && (
            <div className="d-flex justify-content-center">
              <ReactCrop
                crop={crop}
                onChange={(c) => setCrop(c)}
                onComplete={(c) => setCompletedCrop(c)}
                aspect={4 / 3}
              >
                <img
                  ref={imgRef}
                  src={imageToCrop}
                  alt="Crop preview"
                  style={{ maxWidth: '100%', maxHeight: '500px' }}
                />
              </ReactCrop>
            </div>
          )}
        </ModalBody>
        <ModalFooter>
          <Button color="secondary" onClick={() => {
            setCropModal(false);
            setImageToCrop('');
            setImageFile(null);
          }}>
            Cancel
          </Button>
          <Button color="primary" onClick={handleCropComplete}>
            <i className="bi bi-check-lg me-2"></i>
            Apply Crop
          </Button>
        </ModalFooter>
      </Modal>
    </div>
  );
};

export default MenuPage;
