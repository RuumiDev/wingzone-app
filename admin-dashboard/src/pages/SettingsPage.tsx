import React, { useState, useEffect } from 'react';
import { Card, CardBody, Form, FormGroup, Label, Input, Button, Alert, Spinner, Badge } from 'reactstrap';
import { doc, getDoc, setDoc } from 'firebase/firestore';
import { ref, uploadBytes, getDownloadURL, deleteObject } from 'firebase/storage';
import { db, storage } from '../lib/firebase';
import { notificationService } from '../services/notifications';

const SettingsPage: React.FC = () => {
  const [appSettings, setAppSettings] = useState({
    darkMode: false,
    notificationSound: true,
    autoRefreshOrders: true,
    autoPrintReceipts: true,
    printerName: '',
    useThermalPrinter: true
  });
  const [availablePrinters, setAvailablePrinters] = useState<string[]>([]);
  const [loadingPrinters, setLoadingPrinters] = useState(false);

  const [soundSettings, setSoundSettings] = useState<{
    enabled: boolean;
    soundType: string;
    customSounds: Array<{ id: string; name: string; url: string; uploadedAt: string }>;
    volume: number;
  }>({
    enabled: true,
    soundType: 'default',
    customSounds: [],
    volume: 70
  });

  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [uploadingSound, setUploadingSound] = useState(false);

  // Load app settings from Firebase
  useEffect(() => {
    const loadAppSettings = async () => {
      try {
        const docRef = doc(db, 'appSettings', 'adminPreferences');
        const docSnap = await getDoc(docRef);
        
        if (docSnap.exists()) {
          const data = docSnap.data();
          setAppSettings({
            darkMode: data.darkMode || false,
            notificationSound: data.notificationSound !== false,
            autoRefreshOrders: data.autoRefreshOrders !== false,
            autoPrintReceipts: data.autoPrintReceipts !== false,
            printerName: data.printerName || '',
            useThermalPrinter: data.useThermalPrinter !== false
          });
        }

        // Load sound settings directly from Firebase
        const soundDocRef = doc(db, 'appSettings', 'notificationSound');
        const soundDocSnap = await getDoc(soundDocRef);
        
        if (soundDocSnap.exists()) {
          const soundData = soundDocSnap.data();
          const loadedSettings = {
            enabled: soundData.enabled !== false,
            soundType: soundData.soundType || 'default',
            customSounds: soundData.customSounds || [],
            volume: soundData.volume ?? 70
          };
          setSoundSettings(loadedSettings);
          console.log('Loaded sound settings from Firebase:', loadedSettings);
        } else {
          // Fallback to service if document doesn't exist
          await notificationService.loadSoundPreference();
          const soundSettingsData = notificationService.getSoundSettings();
          setSoundSettings(soundSettingsData);
          console.log('Loaded sound settings from service:', soundSettingsData);
        }
      } catch (error) {
        console.error('Error loading app settings:', error);
      } finally {
        setLoading(false);
      }
    };

    loadAppSettings();
  }, []);

  const handleToggleChange = (name: string) => {
    setAppSettings(prev => ({
      ...prev,
      [name]: !prev[name as keyof typeof prev]
    }));
  };

  const handleSaveAppSettings = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    
    try {
      const docRef = doc(db, 'appSettings', 'adminPreferences');
      const dataToSave = {
        darkMode: appSettings.darkMode,
        notificationSound: appSettings.notificationSound,
        autoRefreshOrders: appSettings.autoRefreshOrders,
        autoPrintReceipts: appSettings.autoPrintReceipts,
        printerName: appSettings.printerName,
        useThermalPrinter: appSettings.useThermalPrinter,
        updatedAt: new Date().toISOString()
      };
      
      console.log('Saving admin preferences:', dataToSave);
      await setDoc(docRef, dataToSave, { merge: true });
      
      // Save sound settings
      console.log('Saving sound settings:', soundSettings);
      await notificationService.updateSoundSettings(soundSettings);
      
      // Reload notification service settings to ensure they're applied
      await notificationService.loadSoundPreference();
      
      setSuccess('Settings saved successfully!');
      setTimeout(() => setSuccess(''), 3000);
    } catch (error: any) {
      console.error('Error saving app settings:', error);
      alert(`Failed to save settings: ${error.message || 'Unknown error'}. Please check Firebase permissions.`);
    } finally {
      setSaving(false);
    }
  };

  const handleSoundTypeChange = (newType: string) => {
    setSoundSettings(prev => ({ ...prev, soundType: newType as any }));
  };

  const handleVolumeChange = (newVolume: number) => {
    setSoundSettings(prev => ({ ...prev, volume: newVolume }));
  };

  const handleTestSound = () => {
    const activeSound = soundSettings.customSounds.find(s => s.id === soundSettings.soundType);
    notificationService.previewSound(soundSettings.soundType, activeSound?.url);
  };

  const handleCustomSoundUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // Prompt for custom name
    const soundName = prompt('Enter a name for this custom sound:', file.name.replace(/\.[^/.]+$/, ''));
    if (!soundName) {
      e.target.value = '';
      return;
    }

    if (!file.type.startsWith('audio/')) {
      alert('Please upload an audio file (MP3, WAV, OGG, etc.)');
      e.target.value = '';
      return;
    }

    if (file.size > 5 * 1024 * 1024) {
      alert('File size must be less than 5MB');
      e.target.value = '';
      return;
    }

    setUploadingSound(true);
    console.log('Starting upload for:', file.name, 'Size:', file.size);

    try {
      const timestamp = Date.now();
      const soundId = `custom-${timestamp}`;
      const storageRef = ref(storage, `notification-sounds/${soundId}-${file.name}`);
      
      console.log('Uploading to Firebase Storage...');
      await uploadBytes(storageRef, file);
      console.log('Upload complete, getting download URL...');
      
      const downloadUrl = await getDownloadURL(storageRef);
      console.log('Download URL obtained:', downloadUrl);

      const newSound = {
        id: soundId,
        name: soundName,
        url: downloadUrl,
        uploadedAt: new Date().toISOString()
      };

      setSoundSettings(prev => ({
        ...prev,
        customSounds: [...prev.customSounds, newSound],
        soundType: soundId // Automatically select the newly uploaded sound
      }));

      setSuccess(`Custom sound "${soundName}" uploaded successfully!`);
      setTimeout(() => setSuccess(''), 3000);
      
      // Reset file input
      e.target.value = '';
    } catch (err: any) {
      console.error('Upload error:', err);
      alert(`Failed to upload sound: ${err.message}`);
      e.target.value = '';
    } finally {
      setUploadingSound(false);
      console.log('Upload process finished');
    }
  };

  const handleDeleteCustomSound = async (soundId: string) => {
    if (!confirm('Delete this custom sound?')) return;

    const sound = soundSettings.customSounds.find(s => s.id === soundId);
    if (!sound) return;

    try {
      // Delete from Firebase Storage
      const storageRef = ref(storage, sound.url);
      await deleteObject(storageRef);

      // Remove from settings
      setSoundSettings(prev => ({
        ...prev,
        customSounds: prev.customSounds.filter(s => s.id !== soundId),
        soundType: prev.soundType === soundId ? 'default' : prev.soundType
      }));

      setSuccess(`Custom sound "${sound.name}" deleted`);
      setTimeout(() => setSuccess(''), 3000);
    } catch (err: any) {
      console.error('Delete error:', err);
      alert(`Failed to delete sound: ${err.message}`);
    }
  };

  return (
    <div>
      <h1 className="mb-4">Settings</h1>

      {success && (
        <Alert color="success" className="mb-3">
          <i className="bi bi-check-circle me-2"></i>
          {success}
        </Alert>
      )}

      {loading ? (
        <div className="text-center my-5">
          <Spinner color="primary" />
        </div>
      ) : (
        <>
          <Card className="mb-3">
            <CardBody>
              <h5 className="mb-3">
                <i className="bi bi-sliders me-2"></i>
                Admin Preferences
              </h5>
              <Form onSubmit={handleSaveAppSettings}>
                <FormGroup className="mb-4">
                  <div className="d-flex justify-content-between align-items-center">
                    <div>
                      <Label className="mb-1 fw-bold">
                        <i className="bi bi-moon-stars me-2"></i>
                        Dark Mode
                      </Label>
                      <div className="text-muted small">
                        Enable dark theme for the admin dashboard
                      </div>
                    </div>
                    <div className="form-check form-switch">
                      <Input
                        type="checkbox"
                        id="darkMode"
                        checked={appSettings.darkMode}
                        onChange={() => handleToggleChange('darkMode')}
                        className="form-check-input"
                        style={{ width: '3rem', height: '1.5rem', cursor: 'pointer' }}
                      />
                    </div>
                  </div>
                </FormGroup>

                <FormGroup className="mb-4">
                  <div className="d-flex justify-content-between align-items-center">
                    <div>
                      <Label className="mb-1 fw-bold">
                        <i className="bi bi-volume-up me-2"></i>
                        Notification Sound
                      </Label>
                      <div className="text-muted small">
                        Play sound alerts when new orders arrive
                      </div>
                    </div>
                    <div className="form-check form-switch">
                      <Input
                        type="checkbox"
                        id="notificationSound"
                        checked={appSettings.notificationSound}
                        onChange={() => handleToggleChange('notificationSound')}
                        className="form-check-input"
                        style={{ width: '3rem', height: '1.5rem', cursor: 'pointer' }}
                      />
                    </div>
                  </div>
                </FormGroup>

                {appSettings.notificationSound && (
                  <FormGroup className="mb-4 ms-4">
                    <Label className="fw-bold mb-3">
                      <i className="bi bi-music-note-beamed me-2"></i>
                      Select Alert Sound
                    </Label>
                    <div className="row g-2 mb-3">
                      {/* Preset sounds */}
                      {[
                        { value: 'default', label: 'Default Beep', icon: 'bi-volume-up' },
                        { value: 'urgent-alert', label: '🔥 Urgent Alert', icon: 'bi-bell-fill' }
                      ].map(option => (
                        <div key={option.value} className="col-md-4 col-lg-3">
                          <div 
                            className={`p-3 border-2 rounded cursor-pointer text-center ${
                              soundSettings.soundType === option.value 
                                ? 'border-primary bg-primary text-white shadow-sm' 
                                : 'border-secondary bg-white'
                            }`}
                            onClick={() => handleSoundTypeChange(option.value)}
                            style={{ 
                              cursor: 'pointer',
                              transition: 'all 0.2s ease',
                              border: soundSettings.soundType === option.value ? '2px solid' : '1px solid'
                            }}
                          >
                            <div className="d-flex flex-column align-items-center gap-2">
                              <i className={`bi ${option.icon} fs-4`}></i>
                              <small className="fw-bold">{option.label}</small>
                              {soundSettings.soundType === option.value && (
                                <Badge color="light" className="text-primary">
                                  <i className="bi bi-check-circle-fill me-1"></i>
                                  Active
                                </Badge>
                              )}
                            </div>
                          </div>
                        </div>
                      ))}
                      
                      {/* Custom sounds */}
                      {soundSettings.customSounds.map(sound => (
                        <div key={sound.id} className="col-md-4 col-lg-3">
                          <div 
                            className={`p-3 border-2 rounded cursor-pointer text-center position-relative ${
                              soundSettings.soundType === sound.id 
                                ? 'border-primary bg-primary text-white shadow-sm' 
                                : 'border-secondary bg-white'
                            }`}
                            onClick={() => handleSoundTypeChange(sound.id)}
                            style={{ 
                              cursor: 'pointer',
                              transition: 'all 0.2s ease',
                              border: soundSettings.soundType === sound.id ? '2px solid' : '1px solid'
                            }}
                          >
                            <Button
                              size="sm"
                              color="danger"
                              className="position-absolute top-0 end-0 m-1"
                              style={{ padding: '0.15rem 0.4rem', fontSize: '0.75rem' }}
                              onClick={(e) => {
                                e.stopPropagation();
                                handleDeleteCustomSound(sound.id);
                              }}
                            >
                              <i className="bi bi-x"></i>
                            </Button>
                            <div className="d-flex flex-column align-items-center gap-2">
                              <i className="bi bi-music-note-beamed fs-4"></i>
                              <small className="fw-bold">{sound.name}</small>
                              {soundSettings.soundType === sound.id && (
                                <Badge color="light" className="text-primary">
                                  <i className="bi bi-check-circle-fill me-1"></i>
                                  Active
                                </Badge>
                              )}
                            </div>
                          </div>
                        </div>
                      ))}
                      
                      {/* Add new custom sound button */}
                      <div className="col-md-4 col-lg-3">
                        <div 
                          className="p-3 border-2 border-dashed rounded text-center bg-light"
                          style={{ 
                            cursor: 'pointer',
                            transition: 'all 0.2s ease',
                            border: '2px dashed #ccc'
                          }}
                          onClick={() => document.getElementById('customSoundUpload')?.click()}
                        >
                          <div className="d-flex flex-column align-items-center gap-2 text-muted">
                            <i className="bi bi-plus-circle fs-4"></i>
                            <small className="fw-bold">Add Custom</small>
                          </div>
                        </div>
                        <input
                          id="customSoundUpload"
                          type="file"
                          accept="audio/*"
                          onChange={handleCustomSoundUpload}
                          style={{ display: 'none' }}
                        />
                      </div>
                    </div>

                    {uploadingSound && (
                      <Alert color="info" className="mb-3">
                        <Spinner size="sm" className="me-2" />
                        Uploading custom sound...
                      </Alert>
                    )}

                    <div className="mb-3">
                      <Label className="small">Volume: {soundSettings.volume}%</Label>
                      <Input
                        type="range"
                        min="0"
                        max="100"
                        value={soundSettings.volume}
                        onChange={(e) => handleVolumeChange(parseInt(e.target.value))}
                        className="form-range"
                      />
                    </div>

                    <Button 
                      color="secondary" 
                      size="sm"
                      onClick={handleTestSound}
                    >
                      <i className="bi bi-play-circle me-1"></i>
                      Test Sound
                    </Button>
                  </FormGroup>
                )}

                <FormGroup className="mb-4">
                  <div className="d-flex justify-content-between align-items-center">
                    <div>
                      <Label className="mb-1 fw-bold">
                        <i className="bi bi-arrow-clockwise me-2"></i>
                        Auto Refresh Orders
                      </Label>
                      <div className="text-muted small">
                        Automatically refresh order list in real-time
                      </div>
                    </div>
                    <div className="form-check form-switch">
                      <Input
                        type="checkbox"
                        id="autoRefreshOrders"
                        checked={appSettings.autoRefreshOrders}
                        onChange={() => handleToggleChange('autoRefreshOrders')}
                        className="form-check-input"
                        style={{ width: '3rem', height: '1.5rem', cursor: 'pointer' }}
                      />
                    </div>
                  </div>
                </FormGroup>

                <FormGroup className="mb-4">
                  <div className="d-flex justify-content-between align-items-center mb-3">
                    <div>
                      <Label className="mb-1 fw-bold">
                        <i className="bi bi-printer me-2"></i>
                        Auto-Print Group Receipts
                      </Label>
                      <div className="text-muted small">
                        Automatically print individual receipts for group orders
                      </div>
                    </div>
                    <div className="form-check form-switch">
                      <Input
                        type="checkbox"
                        id="autoPrintReceipts"
                        checked={appSettings.autoPrintReceipts}
                        onChange={() => handleToggleChange('autoPrintReceipts')}
                        className="form-check-input"
                        style={{ width: '3rem', height: '1.5rem', cursor: 'pointer' }}
                      />
                    </div>
                  </div>
                  
                  {appSettings.autoPrintReceipts && (
                    <div className="ms-4 mt-3 p-3 bg-light rounded">
                      {/* Thermal Printer Toggle */}
                      <div className="d-flex justify-content-between align-items-center mb-3">
                        <Label className="mb-0 small fw-bold">
                          <i className="bi bi-receipt me-2"></i>
                          Use QZ Tray Thermal Printer
                        </Label>
                        <div className="form-check form-switch">
                          <Input
                            type="checkbox"
                            checked={appSettings.useThermalPrinter}
                            onChange={() => setAppSettings(prev => ({ ...prev, useThermalPrinter: !prev.useThermalPrinter }))}
                            className="form-check-input"
                            style={{ width: '2.5rem', height: '1.25rem', cursor: 'pointer' }}
                          />
                        </div>
                      </div>
                      <div className="text-muted small mb-3">
                        Direct printing to thermal printers without dialogs (requires QZ Tray)
                      </div>

                      {appSettings.useThermalPrinter ? (
                        <>
                          {/* QZ Tray Printer Selection */}
                          <Button
                            size="sm"
                            color="secondary"
                            outline
                            onClick={async () => {
                              setLoadingPrinters(true);
                              try {
                                const { default: thermalPrinter } = await import('../services/thermalPrinter');
                                const printers = await thermalPrinter.getPrinters();
                                setAvailablePrinters(printers);
                                if (printers.length === 0) {
                                  alert('No printers found. Make sure QZ Tray is running.');
                                }
                              } catch (error) {
                                console.error('Error loading printers:', error);
                                alert('QZ Tray is not running. Please install and start QZ Tray.');
                              } finally {
                                setLoadingPrinters(false);
                              }
                            }}
                            className="mb-2 w-100"
                            disabled={loadingPrinters}
                          >
                            {loadingPrinters ? (
                              <><Spinner size="sm" className="me-2" />Detecting...</>
                            ) : (
                              <><i className="bi bi-arrow-clockwise me-2"></i>Detect Printers</>
                            )}
                          </Button>

                          {availablePrinters.length > 0 && (
                            <>
                              <Label className="mb-2 small fw-bold">
                                <i className="bi bi-printer-fill me-2"></i>
                                Select Printer
                              </Label>
                              <Input
                                type="select"
                                value={appSettings.printerName}
                                onChange={(e) => setAppSettings(prev => ({ ...prev, printerName: e.target.value }))}
                                className="form-select mb-2"
                              >
                                <option value="">Auto-select (first thermal printer found)</option>
                                {availablePrinters.map(printer => (
                                  <option key={printer} value={printer}>{printer}</option>
                                ))}
                              </Input>
                            </>
                          )}

                          <div className="alert alert-success mt-3 mb-0 small">
                            <i className="bi bi-check-circle me-2"></i>
                            <strong>QZ Tray Mode:</strong> Receipts will print directly to thermal printer without dialogs.
                            <ul className="mb-0 mt-2">
                              <li>Download QZ Tray: <a href="https://qz.io/download/" target="_blank" rel="noopener noreferrer">qz.io/download</a></li>
                              <li>Install and run QZ Tray on your computer</li>
                              <li>Click "Detect Printers" to find your thermal printer</li>
                            </ul>
                          </div>
                        </>
                      ) : (
                        <>
                          {/* Browser Print Mode */}
                          <Label className="mb-2 small fw-bold">
                            <i className="bi bi-printer-fill me-2"></i>
                            Printer Name (Optional)
                          </Label>
                          <Input
                            type="text"
                            value={appSettings.printerName}
                            onChange={(e) => setAppSettings(prev => ({ ...prev, printerName: e.target.value }))}
                            placeholder="e.g., Kitchen Printer, Thermal Printer, etc."
                            className="form-control"
                          />
                          <div className="alert alert-info mt-3 mb-0 small">
                            <i className="bi bi-info-circle me-2"></i>
                            <strong>Browser Print Mode:</strong> Print dialog will appear for each receipt.
                            <ul className="mb-0 mt-2">
                              <li>Set printer as system default to reduce clicks</li>
                              <li>Enable QZ Tray mode above for automatic printing</li>
                            </ul>
                          </div>
                        </>
                      )}
                    </div>
                  )}
                </FormGroup>

                <div className="border-top pt-3">
                  <Button color="primary" type="submit" disabled={saving} size="lg">
                    {saving ? (
                      <>
                        <Spinner size="sm" className="me-2" />
                        Saving...
                      </>
                    ) : (
                      <>
                        <i className="bi bi-check-circle me-2"></i>
                        Save Preferences
                      </>
                    )}
                  </Button>
                </div>
              </Form>
            </CardBody>
          </Card>
        </>
      )}
    </div>
  );
};

export default SettingsPage;
