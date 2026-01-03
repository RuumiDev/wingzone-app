const admin = require('firebase-admin');
const serviceAccount = require('./serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();
const auth = admin.auth();

async function setupAdmin() {
  console.log('Firebase Project ID:', admin.app().options.projectId);
  console.log('Starting admin setup...\n');
  
  try {
    // Create new admin user with email and password
    const newAdmin = await auth.createUser({
      email: 'superadmin@wingzone.com',
      password: 'WingZone2026!',
      displayName: 'Super Admin',
      emailVerified: true // Set as verified so no email confirmation needed
    });

    console.log('✓ Admin user created with UID:', newAdmin.uid);
    console.log('✓ Email:', newAdmin.email);
    console.log('✓ Email Verified:', newAdmin.emailVerified);
    
    // Set admin role in Firestore
    await db.collection('users').doc(newAdmin.uid).set({
      email: 'superadmin@wingzone.com',
      role: 'admin',
      displayName: 'Super Admin',
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });
    
    console.log('✓ Admin role set in Firestore!');
    console.log('\n=================================');
    console.log('Admin Login Credentials:');
    console.log('Email: superadmin@wingzone.com');
    console.log('Password: WingZone2026!');
    console.log('=================================\n');
    
    process.exit(0);
  } catch (error) {
    if (error.code === 'auth/email-already-exists') {
      console.log('⚠ User already exists. Updating password and admin role...');
      const existingUser = await auth.getUserByEmail('superadmin@wingzone.com');
      console.log('Found existing user with UID:', existingUser.uid);
      
      // Update password
      await auth.updateUser(existingUser.uid, {
        password: 'WingZone2026!',
        emailVerified: true
      });
      console.log('✓ Password updated!');
      
      // Update role in Firestore
      await db.collection('users').doc(existingUser.uid).set({
        email: 'superadmin@wingzone.com',
        role: 'admin',
        displayName: 'Super Admin',
        createdAt: admin.firestore.FieldValue.serverTimestamp()
      }, { merge: true });
      
      console.log('✓ Admin role updated in Firestore!');
      console.log('\n=================================');
      console.log('Admin Login Credentials:');
      console.log('Email: superadmin@wingzone.com');
      console.log('Password: WingZone2026!');
      console.log('=================================\n');
    } else {
      console.error('Error:', error);
    }
    process.exit(error.code === 'auth/email-already-exists' ? 0 : 1);
  }
}

setupAdmin();
// To run this script: uses the node command, use the terminal