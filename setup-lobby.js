const admin = require('firebase-admin');
const serviceAccount = require('./serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function setupLobbyCollections() {
  console.log('Setting up Firestore collections for Lobby feature...\n');
  
  try {
    // Create Restaurant Locations
    console.log('1. Creating restaurantLocations collection...');
    
    const locations = [
      {
        id: 'wingzone-meru',
        name: 'Wingzone Meru',
        displayName: 'Wingzone Meru',
        address: 'Lebuh Meru Raya, Bandar Meru Raya, Ipoh',
        addressLine1: 'Lebuh Meru Raya,',
        addressLine2: 'Bandar Meru Raya, Ipoh',
        city: 'Ipoh',
        coordinates: {
          lat: 4.5975,
          lng: 101.0901
        },
        hours: {
          open: '10:00',
          close: '22:00'
        },
        active: true,
        createdAt: admin.firestore.FieldValue.serverTimestamp()
      },
      {
        id: 'wingzone-greentown',
        name: 'Wingzone GreenTown',
        displayName: 'Wingzone GreenTown',
        address: 'No. 2, Lorong Greentown 8, Greentown Business Centre, 30450 Ipoh, Perak',
        addressLine1: 'No. 2, Lorong Greentown 8,',
        addressLine2: 'Greentown Business Centre, 30450 Ipoh, Perak',
        city: 'Ipoh',
        coordinates: {
          lat: 4.5975,
          lng: 101.0901
        },
        hours: {
          open: '10:00',
          close: '22:00'
        },
        active: true,
        createdAt: admin.firestore.FieldValue.serverTimestamp()
      }
    ];
    
    for (const location of locations) {
      await db.collection('restaurantLocations').doc(location.id).set(location);
      console.log(`   ✓ Added ${location.name}`);
    }
    
    console.log('   ✓ Restaurant locations created\n');
    
    // Update Firestore indexes (if needed)
    console.log('2. Checking Firestore indexes...');
    console.log('   Please ensure these indexes exist in Firebase Console:');
    console.log('   - Collection: lobbies');
    console.log('     Fields: code (Ascending), status (Ascending)');
    console.log('   - Collection: lobbies');
    console.log('     Fields: hostUserId (Ascending), status (Ascending)');
    console.log('   - Collection: lobbies');
    console.log('     Fields: expiresAt (Ascending), status (Ascending)\n');
    
    // Create firestore.indexes.json reference
    console.log('3. Updating firestore.indexes.json...');
    console.log('   Add these indexes to your firestore.indexes.json:\n');
    
    const indexes = {
      indexes: [
        {
          collectionGroup: 'lobbies',
          queryScope: 'COLLECTION',
          fields: [
            { fieldPath: 'code', order: 'ASCENDING' },
            { fieldPath: 'status', order: 'ASCENDING' }
          ]
        },
        {
          collectionGroup: 'lobbies',
          queryScope: 'COLLECTION',
          fields: [
            { fieldPath: 'hostUserId', order: 'ASCENDING' },
            { fieldPath: 'status', order: 'ASCENDING' }
          ]
        },
        {
          collectionGroup: 'lobbies',
          queryScope: 'COLLECTION',
          fields: [
            { fieldPath: 'expiresAt', order: 'ASCENDING' },
            { fieldPath: 'status', order: 'ASCENDING' }
          ]
        }
      ],
      fieldOverrides: []
    };
    
    console.log(JSON.stringify(indexes, null, 2));
    console.log('\n✅ Setup complete!');
    console.log('\nNext steps:');
    console.log('1. Deploy indexes: firebase deploy --only firestore:indexes');
    console.log('2. Update Firestore rules to allow lobby operations');
    console.log('3. Test creating and joining lobbies in the app\n');
    
  } catch (error) {
    console.error('❌ Error setting up collections:', error);
  }
}

// Firestore Security Rules
console.log('\n📋 Firestore Security Rules to add:\n');
console.log(`
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Restaurant Locations - Read only
    match /restaurantLocations/{locationId} {
      allow read: if true;
      allow write: if false; // Admin only via console
    }
    
    // Lobbies
    match /lobbies/{lobbyId} {
      // Anyone can read active lobbies
      allow read: if resource.data.status == 'active';
      
      // Authenticated users can create lobbies
      allow create: if request.auth != null
        && request.resource.data.hostUserId == request.auth.uid
        && request.resource.data.status == 'active';
      
      // Host can update lobby
      allow update: if request.auth != null
        && (resource.data.hostUserId == request.auth.uid
            || isMemberOfLobby(request.auth.uid));
      
      // Host can delete lobby
      allow delete: if request.auth != null
        && resource.data.hostUserId == request.auth.uid;
      
      function isMemberOfLobby(userId) {
        return resource.data.members.hasAny([userId]);
      }
    }
  }
}
`);

setupLobbyCollections();
