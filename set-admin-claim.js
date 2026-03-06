const admin = require('firebase-admin');
const serviceAccount = require('./serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const auth = admin.auth();

async function setAdminClaim() {
  try {
    const email = 'superadmin@wingzone.com';
    const user = await auth.getUserByEmail(email);
    console.log('Found user:', user.uid, user.email);
    console.log('Current custom claims:', user.customClaims);

    await auth.setCustomUserClaims(user.uid, { admin: true });
    console.log('✅ Custom claim admin:true set on', email);
    console.log('\nIMPORTANT: The user must sign out and sign back in for the');
    console.log('new token to take effect in the admin dashboard.');
    process.exit(0);
  } catch (error) {
    console.error('Error:', error);
    process.exit(1);
  }
}

setAdminClaim();
