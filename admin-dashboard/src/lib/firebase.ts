import { initializeApp } from 'firebase/app';
import { getAuth } from 'firebase/auth';
import { getFirestore } from 'firebase/firestore';
import { getStorage } from 'firebase/storage';
import { getFunctions } from 'firebase/functions';

// Firebase configuration from your Firebase project
const firebaseConfig = {
  apiKey: "AIzaSyBhJf12zZDS9_5-arJQzxkCF7Mzg5yj7qs",
  authDomain: "wingzone-app.firebaseapp.com",
  projectId: "wingzone-app",
  storageBucket: "wingzone-app.firebasestorage.app",
  messagingSenderId: "24304517780",
  appId: "1:24304517780:web:817b463c9677e3b62aa81",
  measurementId: "G-LGM2BTYPE4"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);

// Initialize Firebase services
export const auth = getAuth(app);
export const db = getFirestore(app);
export const storage = getStorage(app);
export const functions = getFunctions(app);

export default app;
