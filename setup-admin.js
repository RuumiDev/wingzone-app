const admin = require('firebase-admin');
const serviceAccount = require('./serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function setupAdmin() {
  try {
    await db.collection('users').doc('e23OzZSHxZNiDnHiDcQ62N43yX63').set({
      email: 'admin123@wingzone.com',
      role: 'admin',
      displayName: 'Administrator',
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });
    console.log('✓ Admin user role added successfully!');
    process.exit(0);
  } catch (error) {
    console.error('Error:', error);
    process.exit(1);
  }
}

setupAdmin();
