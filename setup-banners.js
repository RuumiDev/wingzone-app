const admin = require('firebase-admin');
const serviceAccount = require('./serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

const initialBanners = [
  {
    id: 'banner1',
    title: 'SPICY',
    subtitle: 'WING COMBO',
    description: 'LIMITED TIME',
    imageUrl: '',
    backgroundColor: '#C8102E',
    accentColor: '#FF6B35',
    order: 1,
    enabled: true
  },
  {
    id: 'banner2',
    title: 'CRISPY',
    subtitle: 'TENDERS',
    description: 'NEW FLAVOR',
    imageUrl: '',
    backgroundColor: '#D32F2F',
    accentColor: '#FF6B35',
    order: 2,
    enabled: true
  },
  {
    id: 'banner3',
    title: 'FAMILY',
    subtitle: 'FEAST DEAL',
    description: 'SAVE 30%',
    imageUrl: '',
    backgroundColor: '#FF6B35',
    accentColor: '#C8102E',
    order: 3,
    enabled: true
  }
];

async function setupBanners() {
  try {
    console.log('Setting up home banners...');
    
    for (const banner of initialBanners) {
      await db.collection('homeBanners').doc(banner.id).set(banner);
      console.log(`✓ Created banner: ${banner.id}`);
    }
    
    console.log('\n✅ Banners collection initialized successfully!');
    console.log('You can now manage banners through the admin dashboard.');
    process.exit(0);
  } catch (error) {
    console.error('❌ Error setting up banners:', error);
    process.exit(1);
  }
}

setupBanners();
