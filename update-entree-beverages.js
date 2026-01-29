const admin = require('firebase-admin');
const serviceAccount = require('./serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

const BEVERAGES = [
  'Coke',
  'Coke Zero',
  'Sprite',
  'Iced Lemon Tea',
  'Orange Juice'
];

async function updateEntreeBeverages() {
  try {
    console.log('Updating entree beverages...\n');
    
    const entrees = [
      { name: 'Entree 10', description: 'Supreme Grilled Chicken Tortilla + Kettle Chips + Drink' },
      { name: 'Entree 11', description: 'Supreme Chicken Tender Tortilla + Kettle Chips + Drink' },
      { name: 'Entree 12', description: 'Premium Beef Tortilla + Kettle Chips + Drink' }
    ];
    
    const batch = db.batch();
    let updateCount = 0;
    
    for (const entree of entrees) {
      const snapshot = await db.collection('menuItems')
        .where('name', '==', entree.name)
        .get();
      
      if (!snapshot.empty) {
        const doc = snapshot.docs[0];
        const data = doc.data();
        
        console.log(`Updating ${entree.name}...`);
        
        batch.update(doc.ref, {
          requiresCustomization: true,
          'customizationOptions.requiresBeverage': true,
          'customizationOptions.beverages': BEVERAGES
        });
        
        updateCount++;
      } else {
        console.log(`⚠ ${entree.name} not found in Firestore`);
      }
    }
    
    if (updateCount > 0) {
      await batch.commit();
      console.log(`\n✅ Updated ${updateCount} entree items with beverage options!`);
    } else {
      console.log('\n⚠ No entrees found to update');
    }
    
    process.exit(0);
  } catch (error) {
    console.error('Error updating entrees:', error);
    process.exit(1);
  }
}

updateEntreeBeverages();
