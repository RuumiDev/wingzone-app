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

async function fixEntree1Beverages() {
  try {
    console.log('Fixing Entree 1 beverage options...\n');

    const snapshot = await db.collection('menuItems')
      .where('name', '==', 'Entree 1')
      .get();

    if (snapshot.empty) {
      console.log('⚠ Entree 1 not found in Firestore');
      process.exit(1);
    }

    const doc = snapshot.docs[0];
    const data = doc.data();
    const existing = data.customizationOptions?.beverages || [];
    console.log('Current beverages:', existing);

    await doc.ref.update({
      requiresCustomization: true,
      'customizationOptions.requiresBeverage': true,
      'customizationOptions.beverages': BEVERAGES
    });

    console.log('✅ Updated Entree 1 beverages to:', BEVERAGES);
    process.exit(0);
  } catch (error) {
    console.error('Error:', error);
    process.exit(1);
  }
}

fixEntree1Beverages();
