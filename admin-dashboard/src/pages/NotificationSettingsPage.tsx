import React, { useState, useEffect } from 'react';
import { notificationService } from '../services/notifications';
import { ref, uploadBytes, getDownloadURL } from 'firebase/storage';
import { storage } from '../lib/firebase';
import type { NotificationSoundSettings } from '../services/notifications';

const NotificationSettingsPage: React.FC = () => {
  const [settings, setSettings] = useState<NotificationSoundSettings>({
    enabled: true,
    soundType: 'default',
    volume: 70
  });
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState<string>('');
  const [saveMessage, setSaveMessage] = useState<string>('');
  const [autoPrintEnabled, setAutoPrintEnabled] = useState(true);
  const [selectedPrinter, setSelectedPrinter] = useState<string>('');
  const [availablePrinters, setAvailablePrinters] = useState<string[]>([]);

  useEffect(() => {
    // Load current settings
    const currentSettings = notificationService.getSoundSettings();
    setSettings(currentSettings);
    
    // Load auto-print settings
    loadAutoPrintSettings();
    
    // Get available printers
    loadAvailablePrinters();
  }, []);

  const loadAutoPrintSettings = async () => {
    try {
      const { doc, getDoc } = await import('firebase/firestore');
      const { db } = await import('../lib/firebase');
      const docRef = doc(db, 'appSettings', 'autoPrint');
      const docSnap = await getDoc(docRef);
      if (docSnap.exists()) {
        setAutoPrintEnabled(docSnap.data().enabled !== false);
        setSelectedPrinter(docSnap.data().printerName || '');
      }
    } catch (error) {
      console.error('Error loading auto-print settings:', error);
    }
  };

  const loadAvailablePrinters = async () => {
    try {
      // Try to get available printers using the browser's print API
      // Note: This is limited by browser security - full printer list may not be available
      // In production, you might need a native app or browser extension
      const printers = ['Default Printer', 'Kitchen Printer', 'Receipt Printer', 'Thermal Printer'];
      setAvailablePrinters(printers);
    } catch (error) {
      console.error('Error loading printers:', error);
      setAvailablePrinters(['Default Printer']);
    }
  };

  const handleAutoPrintToggle = async () => {
    const newValue = !autoPrintEnabled;
    setAutoPrintEnabled(newValue);
    
    try {
      const { doc, setDoc } = await import('firebase/firestore');
      const { db } = await import('../lib/firebase');
      const docRef = doc(db, 'appSettings', 'autoPrint');
      await setDoc(docRef, { enabled: newValue, printerName: selectedPrinter }, { merge: true });
      setSaveMessage('Auto-print settings saved!');
      setTimeout(() => setSaveMessage(''), 2000);
    } catch (error) {
      console.error('Error saving auto-print settings:', error);
    }
  };

  const handlePrinterChange = async (printerName: string) => {
    setSelectedPrinter(printerName);
    
    try {
      const { doc, setDoc } = await import('firebase/firestore');
      const { db } = await import('../lib/firebase');
      const docRef = doc(db, 'appSettings', 'autoPrint');
      await setDoc(docRef, { enabled: autoPrintEnabled, printerName }, { merge: true });
      setSaveMessage('Printer settings saved!');
      setTimeout(() => setSaveMessage(''), 2000);
    } catch (error) {
      console.error('Error saving printer settings:', error);
    }
  };

  const handleSoundTypeChange = async (soundType: string) => {
    const newSettings = { ...settings, soundType: soundType as any };
    setSettings(newSettings);
    await notificationService.updateSoundSettings({ soundType: soundType as any });
    setSaveMessage('Settings saved!');
    setTimeout(() => setSaveMessage(''), 2000);
  };

  const handleVolumeChange = async (volume: number) => {
    const newSettings = { ...settings, volume };
    setSettings(newSettings);
    await notificationService.updateSoundSettings({ volume });
  };

  const handleEnabledToggle = async () => {
    const newSettings = { ...settings, enabled: !settings.enabled };
    setSettings(newSettings);
    await notificationService.updateSoundSettings({ enabled: !settings.enabled });
    setSaveMessage('Settings saved!');
    setTimeout(() => setSaveMessage(''), 2000);
  };

  const handlePreview = () => {
    notificationService.previewSound(settings.soundType, settings.customSoundUrl);
  };

  const handleCustomUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // Validate file type
    const validTypes = ['audio/mpeg', 'audio/wav', 'audio/ogg', 'audio/mp3'];
    if (!validTypes.includes(file.type) && !file.name.match(/\.(mp3|wav|ogg)$/i)) {
      setUploadError('Please upload a valid audio file (MP3, WAV, or OGG)');
      return;
    }

    // Validate file size (max 5MB)
    if (file.size > 5 * 1024 * 1024) {
      setUploadError('File size must be less than 5MB');
      return;
    }

    setUploading(true);
    setUploadError('');

    try {
      // Upload to Firebase Storage
      const storageRef = ref(storage, `notification-sounds/custom-${Date.now()}.${file.name.split('.').pop()}`);
      await uploadBytes(storageRef, file);
      const downloadUrl = await getDownloadURL(storageRef);

      // Update settings with custom URL
      const newSettings = { 
        ...settings, 
        soundType: 'custom' as const,
        customSoundUrl: downloadUrl 
      };
      setSettings(newSettings);
      await notificationService.updateSoundSettings({ 
        soundType: 'custom',
        customSoundUrl: downloadUrl 
      });

      setSaveMessage('Custom sound uploaded successfully!');
      setTimeout(() => setSaveMessage(''), 3000);
    } catch (error) {
      console.error('Error uploading custom sound:', error);
      setUploadError('Failed to upload sound. Please try again.');
    } finally {
      setUploading(false);
    }
  };

  const soundOptions = [
    { value: 'default', label: 'Default Beep', description: 'Simple double beep tone' },
    { value: 'bell', label: 'Bell', description: 'Pleasant bell chime' },
    { value: 'chime', label: 'Chime', description: 'Soft chime sound' },
    { value: 'alert', label: 'Alert', description: 'Urgent alert tone' },
    { value: 'siren', label: 'Siren', description: 'High-priority siren sound' },
    { value: 'custom', label: 'Custom Sound', description: 'Upload your own sound file' }
  ];

  return (
    <div className="p-6 max-w-4xl mx-auto">
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Notification Settings</h1>
        <p className="text-gray-600">Customize notification sounds for incoming orders</p>
      </div>

      {saveMessage && (
        <div className="mb-4 p-3 bg-green-100 border border-green-400 text-green-700 rounded">
          {saveMessage}
        </div>
      )}

      {/* Enable/Disable Toggle */}
      <div className="bg-white rounded-lg shadow p-6 mb-6">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-xl font-semibold text-gray-900 mb-1">Notification Sounds</h2>
            <p className="text-gray-600 text-sm">Enable or disable all notification sounds</p>
          </div>
          <label className="relative inline-flex items-center cursor-pointer">
            <input
              type="checkbox"
              className="sr-only peer"
              checked={settings.enabled}
              onChange={handleEnabledToggle}
            />
            <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
          </label>
        </div>
      </div>

      {/* Sound Type Selection */}
      <div className="bg-white rounded-lg shadow p-6 mb-6">
        <h2 className="text-xl font-semibold text-gray-900 mb-4">Sound Type</h2>
        <div className="space-y-3">
          {soundOptions.map((option) => (
            <label
              key={option.value}
              className={`flex items-start p-4 border-2 rounded-lg cursor-pointer transition-all ${
                settings.soundType === option.value
                  ? 'border-blue-500 bg-blue-50'
                  : 'border-gray-200 hover:border-gray-300'
              }`}
            >
              <input
                type="radio"
                name="soundType"
                value={option.value}
                checked={settings.soundType === option.value}
                onChange={() => handleSoundTypeChange(option.value)}
                className="mt-1 h-4 w-4 text-blue-600 focus:ring-blue-500"
                disabled={!settings.enabled}
              />
              <div className="ml-3 flex-1">
                <div className="font-medium text-gray-900">{option.label}</div>
                <div className="text-sm text-gray-600">{option.description}</div>
              </div>
            </label>
          ))}
        </div>
      </div>

      {/* Custom Upload Section */}
      {settings.soundType === 'custom' && (
        <div className="bg-white rounded-lg shadow p-6 mb-6">
          <h2 className="text-xl font-semibold text-gray-900 mb-4">Upload Custom Sound</h2>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Select Audio File (MP3, WAV, or OGG - Max 5MB)
              </label>
              <input
                type="file"
                accept="audio/mpeg,audio/wav,audio/ogg,.mp3,.wav,.ogg"
                onChange={handleCustomUpload}
                disabled={uploading || !settings.enabled}
                className="block w-full text-sm text-gray-900 border border-gray-300 rounded-lg cursor-pointer bg-gray-50 focus:outline-none"
              />
            </div>
            {uploading && (
              <div className="text-blue-600 text-sm">Uploading...</div>
            )}
            {uploadError && (
              <div className="text-red-600 text-sm">{uploadError}</div>
            )}
            {settings.customSoundUrl && (
              <div className="text-green-600 text-sm">✓ Custom sound uploaded</div>
            )}
          </div>
        </div>
      )}

      {/* Volume Control */}
      <div className="bg-white rounded-lg shadow p-6 mb-6">
        <h2 className="text-xl font-semibold text-gray-900 mb-4">Volume</h2>
        <div className="space-y-4">
          <div className="flex items-center gap-4">
            <span className="text-sm text-gray-600 w-12">🔈</span>
            <input
              type="range"
              min="0"
              max="100"
              value={settings.volume}
              onChange={(e) => handleVolumeChange(Number(e.target.value))}
              disabled={!settings.enabled}
              className="flex-1 h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer"
            />
            <span className="text-sm text-gray-600 w-12">🔊</span>
            <span className="text-sm font-medium text-gray-900 w-12">{settings.volume}%</span>
          </div>
        </div>
      </div>

      {/* Preview Button */}
      <div className="bg-white rounded-lg shadow p-6 mb-6">
        <button
          onClick={handlePreview}
          disabled={!settings.enabled}
          className="w-full bg-blue-600 hover:bg-blue-700 disabled:bg-gray-400 text-white font-medium py-3 px-4 rounded-lg transition-colors"
        >
          🔊 Preview Sound
        </button>
        <p className="text-sm text-gray-600 text-center mt-2">
          Click to test the current notification sound
        </p>
      </div>

      {/* Auto-Print Settings */}
      <div className="bg-white rounded-lg shadow p-6 mb-6">
        <h2 className="text-xl font-semibold text-gray-900 mb-4">Auto-Print Settings</h2>
        <p className="text-gray-600 text-sm mb-4">
          Automatically print receipts when new orders arrive
        </p>
        
        {/* Enable/Disable Auto-Print */}
        <div className="flex items-center justify-between mb-6 pb-4 border-b">
          <div>
            <h3 className="font-medium text-gray-900 mb-1">Enable Auto-Print</h3>
            <p className="text-gray-600 text-sm">Print receipts automatically for new group orders</p>
          </div>
          <label className="relative inline-flex items-center cursor-pointer">
            <input
              type="checkbox"
              className="sr-only peer"
              checked={autoPrintEnabled}
              onChange={handleAutoPrintToggle}
            />
            <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
          </label>
        </div>

        {/* Printer Selection */}
        <div className={`space-y-4 ${!autoPrintEnabled ? 'opacity-50 pointer-events-none' : ''}`}>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Target Printer
            </label>
            <select
              value={selectedPrinter}
              onChange={(e) => handlePrinterChange(e.target.value)}
              disabled={!autoPrintEnabled}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            >
              <option value="">Select a printer...</option>
              {availablePrinters.map((printer) => (
                <option key={printer} value={printer}>
                  {printer}
                </option>
              ))}
            </select>
            <p className="text-sm text-gray-500 mt-2">
              ℹ️ Note: Browser limitations may restrict direct printer selection. For best results, set your default system printer or use a thermal printer.
            </p>
          </div>

          {/* Additional Info */}
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <h4 className="font-medium text-blue-900 mb-2">How Auto-Print Works</h4>
            <ul className="text-sm text-blue-800 space-y-1">
              <li>• Receipts print automatically when new group orders arrive</li>
              <li>• Each participant gets their own individual receipt</li>
              <li>• Receipts include order details and kitchen summary</li>
              <li>• Make sure your printer is connected and ready</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
};

export default NotificationSettingsPage;
