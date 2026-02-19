const admin = require('firebase-admin');

// Initialize Firebase Admin
if (!admin.apps.length) {
  const serviceAccount = require('./serviceAccountKey.json');
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
    storageBucket: 'wingzone-app.firebasestorage.app'
  });
}

const db = admin.firestore();

async function setupTestBanner() {
  console.log('Creating test banner...');
  
  const testBanner = {
    title: 'Welcome to WingZone!',
    subtitle: 'Hot Wings, Cold Beer, Great Times',
    description: 'Order now and get 10% off your first order!',
    imageUrl: 'https://images.unsplash.com/photo-1608039755401-742074f0548d?w=800', // Wings image
    backgroundColor: '#C8102E',
    accentColor: '#FF6B35',
    order: 0,
    enabled: true,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  };

  try {
    const docRef = await db.collection('homeBanners').add(testBanner);
    console.log('✅ Test banner created successfully!');
    console.log('Banner ID:', docRef.id);
    console.log('Title:', testBanner.title);
    console.log('Image URL:', testBanner.imageUrl);
    
    // Verify it was created
    const doc = await docRef.get();
    console.log('\nVerifying banner data:');
    console.log(doc.data());
    
  } catch (error) {
    console.error('❌ Error creating banner:', error);
  }
  
  process.exit(0);
}

setupTestBanner();
