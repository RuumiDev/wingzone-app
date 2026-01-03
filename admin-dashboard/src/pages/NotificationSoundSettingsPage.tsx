import React, { useState, useEffect } from 'react';
import { Card, CardBody, Form, FormGroup, Label, Input, Button, Alert, Badge } from 'reactstrap';
import { ref, uploadBytes, getDownloadURL } from 'firebase/storage';
import { storage } from '../lib/firebase';
import { notificationService } from '../services/notifications';

const NotificationSoundSettingsPage: React.FC = () => {
  const [soundSettings, setSoundSettings] = useState({
    enabled: true,
    soundType: 'default' as 'default' | 'bell' | 'chime' | 'alert' | 'urgent-alert' | 'kitchen-bell' | 'alarm' | 'siren' | 'custom',
    customSoundUrl: '',
    volume: 70
  });

  const [uploading, setUploading] = useState(false);
  const [success, setSuccess] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    const settings = notificationService.getSoundSettings();
    setSoundSettings(settings);
  }, []);

  const soundOptions = [
    { value: 'default', label: 'Default Beep', description: 'Simple electronic beep' },
    { value: 'bell', label: 'Bell', description: 'Gentle bell chime' },
    { value: 'chime', label: 'Chime', description: 'Pleasant chime sound' },
    { value: 'alert', label: 'Alert', description: 'Standard alert tone' },
    { value: 'urgent-alert', label: '🔥 Urgent Alert', description: 'LOUD attention-grabbing alert for kitchen' },
    { value: 'kitchen-bell', label: '🔔 Kitchen Bell', description: 'Service bell sound - easy to hear' },
    { value: 'alarm', label: '⚠️ Alarm', description: 'Loud alarm for immediate action' },
    { value: 'siren', label: 'Siren', description: 'Emergency siren (very loud)' },
    { value: 'custom', label: 'Custom Sound', description: 'Upload your own alert sound' }
  ];

  const handleSoundTypeChange = async (newType: string) => {
    setSoundSettings(prev => ({ ...prev, soundType: newType as any }));
    await notificationService.updateSoundSettings({ soundType: newType as any });
  };

  const handleVolumeChange = async (newVolume: number) => {
    setSoundSettings(prev => ({ ...prev, volume: newVolume }));
    await notificationService.updateSoundSettings({ volume: newVolume });
  };

  const handleTestSound = () => {
    notificationService.previewSound(soundSettings.soundType, soundSettings.customSoundUrl);
  };

  const handleCustomSoundUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // Validate file type
    if (!file.type.startsWith('audio/')) {
      setError('Please upload an audio file (MP3, WAV, OGG, etc.)');
      return;
    }

    // Validate file size (max 5MB)
    if (file.size > 5 * 1024 * 1024) {
      setError('File size must be less than 5MB');
      return;
    }

    setUploading(true);
    setError('');

    try {
      // Upload to Firebase Storage
      const timestamp = Date.now();
      const storageRef = ref(storage, `notification-sounds/${timestamp}-${file.name}`);
      await uploadBytes(storageRef, file);
      const downloadUrl = await getDownloadURL(storageRef);

      // Update settings
      setSoundSettings(prev => ({ ...prev, customSoundUrl: downloadUrl, soundType: 'custom' }));
      await notificationService.updateSoundSettings({ 
        customSoundUrl: downloadUrl,
        soundType: 'custom'
      });

      setSuccess('Custom sound uploaded successfully!');
      setTimeout(() => setSuccess(''), 3000);
    } catch (err: any) {
      setError(`Failed to upload sound: ${err.message}`);
    } finally {
      setUploading(false);
    }
  };

  const handleToggleEnabled = async () => {
    const newEnabled = !soundSettings.enabled;
    setSoundSettings(prev => ({ ...prev, enabled: newEnabled }));
    await notificationService.updateSoundSettings({ enabled: newEnabled });
  };

  return (
    <div>
      <h1 className="mb-4">
        <i className="bi bi-volume-up me-2"></i>
        Notification Sound Settings
      </h1>

      {success && (
        <Alert color="success">
          <i className="bi bi-check-circle me-2"></i>
          {success}
        </Alert>
      )}

      {error && (
        <Alert color="danger" toggle={() => setError('')}>
          <i className="bi bi-exclamation-triangle me-2"></i>
          {error}
        </Alert>
      )}

      <Card className="mb-4">
        <CardBody>
          <h5 className="mb-3">Sound Notification Settings</h5>
          
          <FormGroup className="mb-4">
            <div className="d-flex justify-content-between align-items-center p-3 border rounded">
              <div>
                <Label className="mb-1 fw-bold">
                  Enable Sound Notifications
                </Label>
                <div className="text-muted small">
                  Play sound when new orders arrive
                </div>
              </div>
              <div className="form-check form-switch">
                <Input
                  type="checkbox"
                  checked={soundSettings.enabled}
                  onChange={handleToggleEnabled}
                  style={{ width: '3rem', height: '1.5rem', cursor: 'pointer' }}
                />
              </div>
            </div>
          </FormGroup>

          {soundSettings.enabled && (
            <>
              <FormGroup className="mb-4">
                <Label className="fw-bold mb-3">
                  <i className="bi bi-music-note-beamed me-2"></i>
                  Select Alert Sound
                </Label>
                <div className="alert alert-info">
                  <i className="bi bi-lightbulb me-2"></i>
                  <strong>Kitchen Staff Recommendation:</strong> Use "Urgent Alert", "Kitchen Bell", or "Alarm" for loud, attention-grabbing sounds that are easy to hear in a busy kitchen environment.
                </div>
                <div className="row g-3">
                  {soundOptions.map(option => (
                    <div key={option.value} className="col-md-6 col-lg-4">
                      <div 
                        className={`p-3 border rounded cursor-pointer ${soundSettings.soundType === option.value ? 'border-primary bg-light' : ''}`}
                        onClick={() => handleSoundTypeChange(option.value)}
                        style={{ cursor: 'pointer' }}
                      >
                        <div className="d-flex justify-content-between align-items-start mb-2">
                          <strong>{option.label}</strong>
                          {soundSettings.soundType === option.value && (
                            <Badge color="primary">Active</Badge>
                          )}
                        </div>
                        <small className="text-muted">{option.description}</small>
                      </div>
                    </div>
                  ))}
                </div>
              </FormGroup>

              {soundSettings.soundType === 'custom' && (
                <FormGroup className="mb-4">
                  <Label className="fw-bold">
                    <i className="bi bi-upload me-2"></i>
                    Upload Custom Sound
                  </Label>
                  <div className="alert alert-warning">
                    <i className="bi bi-info-circle me-2"></i>
                    Upload your own alert sound (MP3, WAV, OGG). Max file size: 5MB
                  </div>
                  <Input
                    type="file"
                    accept="audio/*"
                    onChange={handleCustomSoundUpload}
                    disabled={uploading}
                  />
                  {uploading && (
                    <div className="mt-2 text-primary">
                      <i className="bi bi-hourglass-split me-2"></i>
                      Uploading...
                    </div>
                  )}
                  {soundSettings.customSoundUrl && (
                    <div className="mt-2 text-success">
                      <i className="bi bi-check-circle me-2"></i>
                      Custom sound uploaded
                    </div>
                  )}
                </FormGroup>
              )}

              <FormGroup className="mb-4">
                <Label className="fw-bold">
                  <i className="bi bi-volume-down me-2"></i>
                  Volume: {soundSettings.volume}%
                </Label>
                <Input
                  type="range"
                  min="0"
                  max="100"
                  value={soundSettings.volume}
                  onChange={(e) => handleVolumeChange(parseInt(e.target.value))}
                  className="form-range"
                />
                <div className="d-flex justify-content-between text-muted small">
                  <span>Silent</span>
                  <span>Maximum</span>
                </div>
              </FormGroup>

              <div className="border-top pt-3">
                <Button 
                  color="primary" 
                  size="lg"
                  onClick={handleTestSound}
                >
                  <i className="bi bi-play-circle me-2"></i>
                  Test Sound
                </Button>
                <div className="text-muted small mt-2">
                  Click to preview the selected sound at current volume
                </div>
              </div>
            </>
          )}
        </CardBody>
      </Card>

      <Card>
        <CardBody>
          <h5 className="mb-3">
            <i className="bi bi-info-circle me-2"></i>
            Tips for Kitchen Staff
          </h5>
          <ul className="mb-0">
            <li className="mb-2">
              <strong>Recommended Sounds:</strong> "Urgent Alert", "Kitchen Bell", or "Alarm" for busy kitchen environments
            </li>
            <li className="mb-2">
              <strong>Volume:</strong> Set to 80-100% for noisy environments, 50-70% for quiet areas
            </li>
            <li className="mb-2">
              <strong>Custom Sounds:</strong> You can upload your own MP3/WAV file - consider using a sound that matches your restaurant's vibe
            </li>
            <li>
              <strong>Test First:</strong> Always test the sound before relying on it for live orders
            </li>
          </ul>
        </CardBody>
      </Card>
    </div>
  );
};

export default NotificationSoundSettingsPage;
