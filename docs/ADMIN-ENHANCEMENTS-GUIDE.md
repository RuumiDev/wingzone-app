# Admin Dashboard Enhancement Guide

## New Features

### 1. Custom Notification Alert Sounds
The admin dashboard now supports customizable notification sounds with loud, attention-grabbing alerts perfect for kitchen environments.

#### Features:
- **9 Built-in Sound Options:**
  - Default Beep (original)
  - Bell
  - Chime
  - Alert
  - 🔥 **Urgent Alert** (RECOMMENDED FOR KITCHEN)
  - 🔔 **Kitchen Bell** (RECOMMENDED FOR KITCHEN)
  - ⚠️ **Alarm** (RECOMMENDED FOR KITCHEN)
  - Siren
  - Custom Upload

- **Volume Control:** Adjust from 0-100%
- **Custom Sound Upload:** Upload your own MP3/WAV alert sounds
- **Test Function:** Preview sounds before saving

#### Setup Instructions:

1. **Add Alert Sound Files:**
   Navigate to `admin-dashboard/public/sounds/` and add these MP3 files:
   
   - `urgent-alert.mp3` - A loud, attention-grabbing alert
   - `kitchen-bell.mp3` - Service bell / kitchen bell sound
   - `alarm.mp3` - Loud alarm for immediate attention
   
   **Free Sound Resources:**
   - [Freesound.org](https://freesound.org/) - Free sound effects
   - [Zapsplat.com](https://www.zapsplat.com/) - Free sound effects
   - [Mixkit.co](https://mixkit.co/free-sound-effects/) - Free sounds
   
   **Recommended Search Terms:**
   - "kitchen bell"
   - "service bell"
   - "alert alarm"
   - "urgent notification"
   - "emergency alarm"

2. **Access Sound Settings:**
   - Log in to Admin Dashboard
   - Click "Alert Sounds" in the sidebar (🔊 icon)
   - Choose your preferred alert sound
   - Adjust volume (recommended: 80-100% for kitchens)
   - Click "Test Sound" to preview

3. **Upload Custom Sounds:**
   - Select "Custom Sound" option
   - Click "Upload Custom Sound"
   - Choose your audio file (MP3, WAV, OGG)
   - Max file size: 5MB
   - The sound will be saved to Firebase Storage

#### Kitchen Staff Recommendations:
- **Busy/Noisy Kitchens:** Use "Urgent Alert", "Alarm", or "Kitchen Bell" at 90-100% volume
- **Quiet Environments:** Use "Bell" or "Chime" at 50-70% volume
- **Always test sounds** before relying on them for live orders

---

### 2. Direct Image Upload for Menu Items
No more external hosting required! Upload images directly from the admin dashboard.

#### Features:
- **Direct Upload:** Upload images straight to Firebase Storage
- **Image Preview:** See images before uploading
- **Auto-Resize:** Optimized for menu display
- **URL Option:** Still supports pasting image URLs if preferred

#### How to Use:

1. **When Adding/Editing Menu Items:**
   - Scroll to "Menu Item Image" section
   - Click "Choose File" under "Upload Image"
   - Select an image (JPG, PNG, GIF, WebP)
   - Max size: 2MB
   - Preview appears automatically
   - Click "Upload Image" button
   - Image URL is automatically saved

2. **Alternative - Paste URL:**
   - If you already have an image URL, paste it in the "Or Paste Image URL" field

3. **Image Management:**
   - Images are stored in Firebase Storage under `menu-images/`
   - Each image has a unique timestamped filename
   - Images persist even if menu item is deleted

#### Best Practices:
- **Image Size:** Use images around 800x600px for best results
- **File Size:** Keep under 2MB for faster loading
- **Format:** JPG for photos, PNG for logos/graphics
- **Quality:** Use clear, high-resolution images that look appetizing

---

## Technical Details

### Firebase Storage Structure:
```
storage/
├── menu-images/
│   ├── 1735689000000-wings.jpg
│   ├── 1735689100000-burger.png
│   └── ...
└── notification-sounds/
    ├── 1735689200000-custom-alert.mp3
    └── ...
```

### Firestore Collections:
```
appSettings/
├── notificationSound/
│   ├── enabled: boolean
│   ├── soundType: string
│   ├── customSoundUrl: string
│   └── volume: number
```

### File Upload Limits:
- **Images:** Max 2MB (JPG, PNG, GIF, WebP)
- **Sounds:** Max 5MB (MP3, WAV, OGG, M4A)

---

## Troubleshooting

### Sound Not Playing:
1. Check browser permissions (allow audio)
2. Ensure volume is not set to 0
3. Verify sound file exists in `/public/sounds/`
4. Try a different browser
5. Check Firebase Storage rules

### Image Upload Fails:
1. Check file size (must be < 2MB)
2. Verify Firebase Storage is enabled
3. Check Firebase Storage security rules:
   ```
   rules_version = '2';
   service firebase.storage {
     match /b/{bucket}/o {
       match /menu-images/{allPaths=**} {
         allow read: if true;
         allow write: if request.auth != null;
       }
       match /notification-sounds/{allPaths=**} {
         allow read: if true;
         allow write: if request.auth != null;
       }
     }
   }
   ```

### Browser Compatibility:
- **Chrome/Edge:** Full support ✅
- **Firefox:** Full support ✅
- **Safari:** Full support ✅
- **Mobile:** Not recommended for admin dashboard

---

## Future Enhancements

Potential features for future updates:
- [ ] Image compression before upload
- [ ] Multiple image upload (gallery view)
- [ ] Audio waveform preview
- [ ] Sound recording directly in browser
- [ ] Scheduled notification sounds (quiet hours)
- [ ] Per-order-type sound customization

---

### 3. Kitchen Ingredients Tracking
Track raw materials for each menu item to generate combined kitchen summaries on receipts.

#### Features:
- **Ingredient Tracking:** Define what raw materials each menu item uses
- **Auto-Aggregation:** Receipt automatically combines ingredients across multiple orders
- **Smart Summaries:** Kitchen staff see total quantities needed (e.g., "17x Original Wings - Buffalo")

#### How to Add Kitchen Ingredients:

1. **Navigate to Menu Management:**
   - Go to Admin Dashboard → Menu Management
   - Click "Add Menu Item" or edit an existing item

2. **Scroll to "Kitchen Ingredients" Section:**
   - Located below "Display Order" field
   - Shows description: "Add ingredients that will be tracked in kitchen summaries"

3. **Add Each Ingredient:**
   - Click "Add Ingredient" button
   - Fill in three fields:
     - **Type:** Ingredient category (e.g., "wings", "fries", "tenders", "salad")
     - **Quantity:** How many units
     - **Requires Selection:** Check if customer chooses (e.g., bone type for wings)

4. **Remove Ingredients:**
   - Click "Remove" button next to any ingredient to delete it

#### Example Ingredient Configurations:

**Entree 2 (7 pcs wings + fries + veggies):**
```
Ingredient 1:
- Type: wings
- Quantity: 7
- Requires Selection: ✓ (checked)

Ingredient 2:
- Type: fries
- Quantity: 1
- Requires Selection: □ (unchecked)

Ingredient 3:
- Type: grilled_vegetables
- Quantity: 1
- Requires Selection: □ (unchecked)
```

**Wings - 10 pcs (standalone):**
```
Ingredient 1:
- Type: wings
- Quantity: 10
- Requires Selection: ✓ (checked)
```

**Chicken Tenders - 5 pcs:**
```
Ingredient 1:
- Type: tenders
- Quantity: 5
- Requires Selection: □ (unchecked)
```

**Double Cheeseburger + Fries:**
```
Ingredient 1:
- Type: beef_patty
- Quantity: 2
- Requires Selection: □

Ingredient 2:
- Type: burger_bun
- Quantity: 1
- Requires Selection: □

Ingredient 3:
- Type: cheese
- Quantity: 2
- Requires Selection: □

Ingredient 4:
- Type: fries
- Quantity: 1
- Requires Selection: □
```

#### Standard Ingredient Types:

Use consistent naming for best aggregation:

| Type | Description |
|------|-------------|
| `wings` | Chicken wings (requires selection for bone type/flavor) |
| `tenders` | Chicken tenders |
| `fries` | Regular fries (any type) |
| `wedge_fries` | Premium wedge fries |
| `salad` | Salad (requires selection for garden/caesar) |
| `garden_salad_mix` | Specific garden salad |
| `caesar_salad_mix` | Specific caesar salad |
| `grilled_vegetables` | Grilled vegetables side |
| `beef_patty` | Burger patty |
| `grilled_chicken` | Grilled chicken breast |
| `burger_bun` | Hamburger bun |
| `cheese` | Cheese slices |
| `rice` | Rice portion |

#### How Kitchen Summary Works:

**Order Example:**
- Customer A orders: Entree 2 (7 wings, Original, Buffalo flavor, DEFAULT wedge fries)
- Customer B orders: Entree 2 (7 wings, Original, Buffalo flavor, SWAPPED to kettle chips)

**Receipt Shows:**
```
ITEM SUMMARY
• 14x Original Wings - Buffalo
• 1x Wedge Fries
• 1x Kettle Chips
• 2x Bread
```

The system automatically detects side substitutions! When a customer swaps their fries for kettle chips, the summary shows **what they actually ordered**, not the default.

**How it works:**
1. Menu item defines default ingredients (e.g., "wedge_fries: 1")
2. When customer swaps sides, system overrides with their choice
3. Summary aggregates all actual ingredients needed

Kitchen staff see exactly what to prepare without manual counting!

#### Best Practices:
- **Consistent Naming:** Use the same ingredient type across all items
- **Check "Requires Selection"** for items with flavor/bone type choices
- **Don't duplicate sides:** If customer can exchange fries, just list default "fries"
- **Update existing items:** Edit old menu items to add kitchen ingredients

---

## Support

For issues or questions:
1. Check Firebase Console for errors
2. Review browser console logs
3. Verify Firebase configuration
4. Contact system administrator

Last updated: January 1, 2026
