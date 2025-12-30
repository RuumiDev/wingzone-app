import React, { useState, useEffect } from 'react';
import { Card, CardBody, Form, FormGroup, Label, Input, Button, Alert, Spinner } from 'reactstrap';
import { doc, getDoc, setDoc } from 'firebase/firestore';
import { db } from '../lib/firebase';

const SettingsPage: React.FC = () => {
  const [appSettings, setAppSettings] = useState({
    darkMode: false,
    notificationSound: true,
    autoRefreshOrders: true,
    autoPrintReceipts: true
  });

  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

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
            autoPrintReceipts: data.autoPrintReceipts !== false
          });
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
        updatedAt: new Date().toISOString()
      };
      
      console.log('Saving admin preferences:', dataToSave);
      await setDoc(docRef, dataToSave, { merge: true });
      
      setSuccess('Settings saved successfully!');
      setTimeout(() => setSuccess(''), 3000);
    } catch (error: any) {
      console.error('Error saving app settings:', error);
      alert(`Failed to save settings: ${error.message || 'Unknown error'}. Please check Firebase permissions.`);
    } finally {
      setSaving(false);
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
                  <div className="d-flex justify-content-between align-items-center">
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
