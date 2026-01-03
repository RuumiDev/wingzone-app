# Notification Sounds

This directory contains preset notification sound files for the admin dashboard.

## Required Sound Files

Place the following audio files in this directory:

1. **bell.mp3** - Pleasant bell chime sound (recommended: 1-2 seconds)
2. **chime.mp3** - Soft chime sound (recommended: 1-2 seconds)
3. **alert.mp3** - Urgent alert tone (recommended: 0.5-1 second)
4. **urgent-alert.mp3** - 🔥 LOUD attention-grabbing alert for busy kitchens (RECOMMENDED)
5. **kitchen-bell.mp3** - 🔔 Service bell / kitchen bell sound (RECOMMENDED)
6. **alarm.mp3** - ⚠️ Loud alarm for immediate action (RECOMMENDED)
7. **siren.mp3** - High-priority siren sound (recommended: 2-3 seconds)

## Audio File Specifications

- **Format**: MP3 (preferred), WAV, or OGG
- **Duration**: 0.5 - 3 seconds
- **Sample Rate**: 44.1 kHz or 48 kHz
- **Bit Rate**: 128 kbps or higher
- **File Size**: Keep under 500KB for fast loading

## Where to Find Sounds

You can find free notification sounds from:
- **Freesound.org** - Community audio library (CC licensed)
- **Zapsplat.com** - Free sound effects for commercial use
- **Mixkit.co** - Free sound effects and music
- **Soundbible.com** - Free sound clips

## Testing

After adding sound files:
1. Go to Notification Settings in the admin dashboard
2. Select a sound type (Bell, Chime, Alert, or Siren)
3. Click "Preview Sound" to test
4. Adjust volume as needed

## Custom Sounds

Users can also upload their own custom notification sounds through the settings page. Custom sounds are stored in Firebase Storage and must be:
- MP3, WAV, or OGG format
- Under 5MB in size

## Fallback

If a sound file is missing or fails to load, the system will automatically fallback to the default beep sound (synthesized tone).
