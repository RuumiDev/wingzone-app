import { signInWithEmailAndPassword, signOut, onAuthStateChanged } from 'firebase/auth';
import { doc, getDoc } from 'firebase/firestore';
import { auth, db } from '../lib/firebase';

export interface AuthUser {
  id: string;
  email: string;
  name: string;
  role: 'admin' | 'kitchen';
}

class AuthService {
  private currentUser: AuthUser | null = null;

  // Firebase authentication
  async login(email: string, password: string): Promise<AuthUser> {
    try {
      // Sign in with Firebase
      const userCredential = await signInWithEmailAndPassword(auth, email, password);
      const firebaseUser = userCredential.user;
      
      // Get user data from Firestore
      const userDoc = await getDoc(doc(db, 'users', firebaseUser.uid));
      
      if (!userDoc.exists()) {
        throw new Error('User data not found');
      }
      
      const userData = userDoc.data();
      
      // Check if user is admin
      if (userData.role !== 'admin' && userData.role !== 'kitchen') {
        await signOut(auth);
        throw new Error('Access denied. Admin privileges required.');
      }
      
      this.currentUser = {
        id: firebaseUser.uid,
        email: firebaseUser.email || '',
        name: userData.displayName || 'Admin User',
        role: userData.role,
      };
      
      localStorage.setItem('user', JSON.stringify(this.currentUser));
      return this.currentUser;
      
    } catch (error: any) {
      console.error('Login error:', error);
      throw new Error(error.message || 'Invalid credentials');
    }
  }

  async logout(): Promise<void> {
    try {
      await signOut(auth);
    } catch (error) {
      console.error('Logout error:', error);
    }
    this.currentUser = null;
    localStorage.removeItem('user');
  }

  getCurrentUser(): AuthUser | null {
    if (this.currentUser) return this.currentUser;
    
    const stored = localStorage.getItem('user');
    if (stored) {
      this.currentUser = JSON.parse(stored);
      return this.currentUser;
    }
    return null;
  }

  isAuthenticated(): boolean {
    return this.getCurrentUser() !== null;
  }

  // Listen to auth state changes
  onAuthStateChange(callback: (user: AuthUser | null) => void) {
    return onAuthStateChanged(auth, async (firebaseUser) => {
      if (firebaseUser) {
        try {
          const userDoc = await getDoc(doc(db, 'users', firebaseUser.uid));
          if (userDoc.exists()) {
            const userData = userDoc.data();
            this.currentUser = {
              id: firebaseUser.uid,
              email: firebaseUser.email || '',
              name: userData.displayName || 'Admin User',
              role: userData.role,
            };
            localStorage.setItem('user', JSON.stringify(this.currentUser));
            callback(this.currentUser);
            return;
          }
        } catch (error) {
          console.error('Error fetching user data:', error);
        }
      }
      this.currentUser = null;
      localStorage.removeItem('user');
      callback(null);
    });
  }
}

export const authService = new AuthService();
