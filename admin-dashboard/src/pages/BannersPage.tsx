import React, { useState, useEffect, useRef } from 'react';
import { collection, getDocs, doc, updateDoc, setDoc } from 'firebase/firestore';
import { ref, uploadBytes, getDownloadURL } from 'firebase/storage';
import { db, storage } from '../lib/firebase';
import Cropper from 'react-easy-crop';
import Swal from 'sweetalert2';
import { showToast } from '../utils/toast';
import UniformLoader from '../components/UniformLoader/UniformLoader';

interface Point {
  x: number;
  y: number;
}

interface Area {
  x: number;
  y: number;
  width: number;
  height: number;
}

interface Banner {
  id: string;
  title: string;
  subtitle: string;
  description: string;
  imageUrl: string;
  backgroundColor: string;
  accentColor: string;
  order: number;
  enabled: boolean;
}

const BannersPage: React.FC = () => {
  const [banners, setBanners] = useState<Banner[]>([]);
  const [loading, setLoading] = useState(true);
  const [editingBanner, setEditingBanner] = useState<Banner | null>(null);
  const [showCropper, setShowCropper] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [imageSrc, setImageSrc] = useState<string | null>(null);
  const [crop, setCrop] = useState<Point>({ x: 0, y: 0 });
  const [zoom, setZoom] = useState(1);
  const [croppedAreaPixels, setCroppedAreaPixels] = useState<Area | null>(null);
  const [uploading, setUploading] = useState(false);
  const [dirtyBanners, setDirtyBanners] = useState<Set<string>>(new Set());
  const [savingBanners, setSavingBanners] = useState<Set<string>>(new Set());
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    fetchBanners();
  }, []);

  const fetchBanners = async () => {
    try {
      const bannersSnapshot = await getDocs(collection(db, 'homeBanners'));
      const bannersData = bannersSnapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data()
      })) as Banner[];
      setBanners(bannersData.sort((a, b) => a.order - b.order));
    } catch (error) {
      console.error('Error fetching banners:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setSelectedFile(file);
      const reader = new FileReader();
      reader.onload = () => {
        setImageSrc(reader.result as string);
        setShowCropper(true);
      };
      reader.readAsDataURL(file);
    }
  };

  const onCropComplete = (_: Area, croppedAreaPixels: Area) => {
    setCroppedAreaPixels(croppedAreaPixels);
  };

  const createImage = (url: string): Promise<HTMLImageElement> =>
    new Promise((resolve, reject) => {
      const image = new Image();
      image.addEventListener('load', () => resolve(image));
      image.addEventListener('error', error => reject(error));
      image.src = url;
    });

  const getCroppedImg = async (
    imageSrc: string,
    pixelCrop: Area
  ): Promise<Blob> => {
    const image = await createImage(imageSrc);
    const canvas = document.createElement('canvas');
    const ctx = canvas.getContext('2d');

    if (!ctx) {
      throw new Error('No 2d context');
    }

    canvas.width = pixelCrop.width;
    canvas.height = pixelCrop.height;

    ctx.drawImage(
      image,
      pixelCrop.x,
      pixelCrop.y,
      pixelCrop.width,
      pixelCrop.height,
      0,
      0,
      pixelCrop.width,
      pixelCrop.height
    );

    return new Promise((resolve) => {
      canvas.toBlob((blob) => {
        resolve(blob as Blob);
      }, 'image/jpeg', 0.95);
    });
  };

  const handleCropSave = async () => {
    if (!imageSrc || !croppedAreaPixels || !editingBanner) return;

    try {
      setUploading(true);
      const croppedImage = await getCroppedImg(imageSrc, croppedAreaPixels);
      
      // Upload to Firebase Storage
      const storageRef = ref(storage, `banners/${editingBanner.id}_${Date.now()}.jpg`);
      await uploadBytes(storageRef, croppedImage);
      const downloadURL = await getDownloadURL(storageRef);

      // Update Firestore
      await updateDoc(doc(db, 'homeBanners', editingBanner.id), {
        imageUrl: downloadURL
      });

      // Update local state
      setBanners(banners.map(b => 
        b.id === editingBanner.id ? { ...b, imageUrl: downloadURL } : b
      ));

      setShowCropper(false);
      setImageSrc(null);
      setSelectedFile(null);
      setEditingBanner(null);
      showToast('success', 'Banner image updated successfully!');
    } catch (error) {
      console.error('Error uploading image:', error);
      showToast('error', 'Failed to upload image');
    } finally {
      setUploading(false);
    }
  };

  const handleBannerUpdate = async (bannerId: string, updates: Partial<Banner>) => {
    try {
      setSavingBanners(prev => new Set(prev).add(bannerId));
      await updateDoc(doc(db, 'homeBanners', bannerId), updates);
      setBanners(banners.map(b => b.id === bannerId ? { ...b, ...updates } : b));
      setDirtyBanners(prev => {
        const newSet = new Set(prev);
        newSet.delete(bannerId);
        return newSet;
      });
      showToast('success', 'Banner saved successfully!');
    } catch (error) {
      console.error('Error updating banner:', error);
      showToast('error', 'Failed to save banner');
    } finally {
      setSavingBanners(prev => {
        const newSet = new Set(prev);
        newSet.delete(bannerId);
        return newSet;
      });
    }
  };

  const handleLocalUpdate = (bannerId: string, updates: Partial<Banner>) => {
    // Update local state immediately
    setBanners(prev => prev.map(b => b.id === bannerId ? { ...b, ...updates } : b));
    // Mark as dirty
    setDirtyBanners(prev => new Set(prev).add(bannerId));
  };

  const handleToggleUpdate = async (bannerId: string, enabled: boolean) => {
    try {
      await updateDoc(doc(db, 'homeBanners', bannerId), { enabled });
      setBanners(banners.map(b => b.id === bannerId ? { ...b, enabled } : b));
      showToast('success', `Banner ${enabled ? 'enabled' : 'disabled'} successfully!`);
    } catch (error) {
      console.error('Error updating banner:', error);
      showToast('error', 'Failed to update banner');
    }
  };

  const handleAddBanner = async () => {
    try {
      const newBanner: Banner = {
        id: `banner${Date.now()}`,
        title: 'NEW',
        subtitle: 'BANNER',
        description: 'EDIT ME',
        imageUrl: '',
        backgroundColor: '#C8102E',
        accentColor: '#FF6B35',
        order: banners.length + 1,
        enabled: true
      };
      await setDoc(doc(db, 'homeBanners', newBanner.id), newBanner);
      setBanners([...banners, newBanner]);
      showToast('success', 'Banner added successfully!');
    } catch (error) {
      console.error('Error adding banner:', error);
      showToast('error', 'Failed to add banner');
    }
  };

  const handleDeleteBanner = async (bannerId: string) => {
    const result = await Swal.fire({
      title: 'Delete Banner?',
      text: 'Are you sure you want to delete this banner?',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#dc3545',
      cancelButtonColor: '#6c757d',
      confirmButtonText: 'Yes, delete it',
      cancelButtonText: 'Cancel'
    });

    if (result.isConfirmed) {
      try {
        await updateDoc(doc(db, 'homeBanners', bannerId), { enabled: false });
        setBanners(banners.filter(b => b.id !== bannerId));
        showToast('success', 'Banner deleted successfully!');
      } catch (error) {
        console.error('Error deleting banner:', error);
        showToast('error', 'Failed to delete banner');
      }
    }
  };

  if (loading) {
    return <UniformLoader message="Loading banners..." />;
  }

  return (
    <div className="container-fluid p-4">
      <input
        ref={fileInputRef}
        type="file"
        accept="image/*"
        style={{ display: 'none' }}
        onChange={handleFileSelect}
      />

      {showCropper && imageSrc && (
        <div className="modal show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.8)' }}>
          <div className="modal-dialog modal-lg modal-dialog-centered">
            <div className="modal-content">
              <div className="modal-header">
                <h5 className="modal-title">Crop Banner Image</h5>
                <button 
                  type="button" 
                  className="btn-close" 
                  onClick={() => setShowCropper(false)}
                  disabled={uploading}
                ></button>
              </div>
              <div className="modal-body">
                <div className="alert alert-info">
                  <i className="bi bi-info-circle me-2"></i>
                  <strong>Image Guidelines:</strong>
                  <ul className="mb-0 mt-2">
                    <li>Recommended aspect ratio: 16:9 (landscape)</li>
                    <li>Minimum dimensions: 1200x675 pixels</li>
                    <li>Maximum file size: 5MB</li>
                    <li>Supported formats: JPG, PNG</li>
                  </ul>
                </div>
                <div style={{ position: 'relative', width: '100%', height: '400px' }}>
                  <Cropper
                    image={imageSrc}
                    crop={crop}
                    zoom={zoom}
                    aspect={16 / 9}
                    onCropChange={setCrop}
                    onZoomChange={setZoom}
                    onCropComplete={onCropComplete}
                  />
                </div>
                <div className="mt-3">
                  <label className="form-label">Zoom</label>
                  <input
                    type="range"
                    min={1}
                    max={3}
                    step={0.1}
                    value={zoom}
                    onChange={(e) => setZoom(parseFloat(e.target.value))}
                    className="form-range"
                  />
                </div>
              </div>
              <div className="modal-footer">
                <button 
                  className="btn btn-secondary" 
                  onClick={() => setShowCropper(false)}
                  disabled={uploading}
                >
                  Cancel
                </button>
                <button 
                  className="btn btn-danger" 
                  onClick={handleCropSave}
                  disabled={uploading}
                >
                  {uploading ? (
                    <>
                      <span className="spinner-border spinner-border-sm me-2"></span>
                      Uploading...
                    </>
                  ) : (
                    'Save & Upload'
                  )}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h2 className="mb-0">Home Page Banners</h2>
          <p className="text-muted mb-0">Manage carousel banners displayed on the home screen</p>
        </div>
        <button className="btn btn-danger" onClick={handleAddBanner}>
          <i className="bi bi-plus-circle me-2"></i>
          Add Banner
        </button>
      </div>

      <div className="row g-4">
        {banners.map((banner) => (
          <div key={banner.id} className="col-12">
            <div className="card shadow-sm">
              <div className="card-body">
                <div className="row">
                  <div className="col-md-4">
                    <div 
                      className="position-relative" 
                      style={{ 
                        aspectRatio: '16/9',
                        backgroundColor: banner.backgroundColor || '#dc3545',
                        borderRadius: '8px',
                        overflow: 'hidden'
                      }}
                    >
                      {banner.imageUrl ? (
                        <img 
                          src={banner.imageUrl} 
                          alt={banner.title}
                          style={{ 
                            width: '100%', 
                            height: '100%', 
                            objectFit: 'cover' 
                          }}
                        />
                      ) : (
                        <div className="d-flex align-items-center justify-content-center h-100 text-white">
                          <i className="bi bi-image" style={{ fontSize: '3rem' }}></i>
                        </div>
                      )}
                      <button
                        className="btn btn-sm btn-light position-absolute top-50 start-50 translate-middle"
                        onClick={() => {
                          setEditingBanner(banner);
                          fileInputRef.current?.click();
                        }}
                      >
                        <i className="bi bi-camera me-2"></i>
                        Change Image
                      </button>
                    </div>
                  </div>
                  <div className="col-md-8">
                    <div className="mb-3">
                      <label className="form-label fw-bold">Title</label>
                      <input
                        type="text"
                        className="form-control"
                        value={banner.title}
                        onChange={(e) => handleLocalUpdate(banner.id, { title: e.target.value })}
                        placeholder="e.g., SPICY"
                      />
                    </div>
                    <div className="mb-3">
                      <label className="form-label fw-bold">Subtitle</label>
                      <input
                        type="text"
                        className="form-control"
                        value={banner.subtitle}
                        onChange={(e) => handleLocalUpdate(banner.id, { subtitle: e.target.value })}
                        placeholder="e.g., WING COMBO"
                      />
                    </div>
                    <div className="mb-3">
                      <label className="form-label fw-bold">Description</label>
                      <input
                        type="text"
                        className="form-control"
                        value={banner.description}
                        onChange={(e) => handleLocalUpdate(banner.id, { description: e.target.value })}
                        placeholder="e.g., LIMITED TIME"
                      />
                    </div>
                    <div className="row">
                      <div className="col-md-6 mb-3">
                        <label className="form-label fw-bold">Background Color</label>
                        <input
                          type="color"
                          className="form-control form-control-color w-100"
                          value={banner.backgroundColor}
                          onChange={(e) => handleLocalUpdate(banner.id, { backgroundColor: e.target.value })}
                        />
                      </div>
                      <div className="col-md-6 mb-3">
                        <label className="form-label fw-bold">Accent Color</label>
                        <input
                          type="color"
                          className="form-control form-control-color w-100"
                          value={banner.accentColor}
                          onChange={(e) => handleLocalUpdate(banner.id, { accentColor: e.target.value })}
                        />
                      </div>
                    </div>
                    <div className="d-flex justify-content-between align-items-center gap-2">
                      <div className="form-check form-switch">
                        <input
                          className="form-check-input"
                          type="checkbox"
                          checked={banner.enabled}
                          onChange={(e) => handleToggleUpdate(banner.id, e.target.checked)}
                        />
                        <label className="form-check-label">
                          {banner.enabled ? 'Enabled' : 'Disabled'}
                        </label>
                      </div>
                      <div className="d-flex gap-2">
                        {dirtyBanners.has(banner.id) && (
                          <button
                            className="btn btn-sm btn-success"
                            onClick={() => handleBannerUpdate(banner.id, {
                              title: banner.title,
                              subtitle: banner.subtitle,
                              description: banner.description,
                              backgroundColor: banner.backgroundColor,
                              accentColor: banner.accentColor
                            })}
                            disabled={savingBanners.has(banner.id)}
                          >
                            {savingBanners.has(banner.id) ? (
                              <>
                                <span className="spinner-border spinner-border-sm me-1"></span>
                                Saving...
                              </>
                            ) : (
                              <>
                                <i className="bi bi-check-circle me-1"></i>
                                Save Changes
                              </>
                            )}
                          </button>
                        )}
                        <button
                          className="btn btn-sm btn-outline-danger"
                          onClick={() => handleDeleteBanner(banner.id)}
                        >
                          <i className="bi bi-trash me-1"></i>
                          Delete
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default BannersPage;
