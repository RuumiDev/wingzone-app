const admin = require('firebase-admin');
const serviceAccount = require('./serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function updateMenuDrinks() {
  try {
    console.log('Updating drink names in menu...');
    
    // Get all menu items
    const menuRef = db.collection('menuItems');
    const snapshot = await menuRef.get();
    
    let updateCount = 0;
    const batch = db.batch();
    
    snapshot.forEach(doc => {
      const data = doc.data();
      let needsUpdate = false;
      const updates = {};
      
      // Update item name if it's "Coca-Cola"
      if (data.name === 'Coca-Cola') {
        updates.name = 'Coke';
        needsUpdate = true;
        console.log(`  • Updating item: ${data.name} → Coke`);
      }
      
      // Update beverages array if it contains "Coca-Cola"
      if (data.customizationOptions?.beverages) {
        const beverages = data.customizationOptions.beverages;
        const index = beverages.indexOf('Coca-Cola');
        if (index !== -1) {
          const newBeverages = [...beverages];
          newBeverages[index] = 'Coke';
          updates['customizationOptions.beverages'] = newBeverages;
          needsUpdate = true;
          console.log(`  • Updating beverages in: ${data.name}`);
        }
      }
      
      if (needsUpdate) {
        batch.update(doc.ref, updates);
        updateCount++;
      }
    });
    
    if (updateCount > 0) {
      await batch.commit();
      console.log(`\n✅ Updated ${updateCount} menu items with new drink naming!`);
    } else {
      console.log('\n✓ No updates needed - all drink names are already correct!');
    }
    
    process.exit(0);
  } catch (error) {
    console.error('Error updating menu drinks:', error);
    process.exit(1);
  }
}

updateMenuDrinks();
