import { initializeApp } from 'firebase/app';
import { getAuth, GoogleAuthProvider, signInWithPopup, signOut } from 'firebase/auth';
import { getFirestore, doc, getDocFromServer } from 'firebase/firestore';

const requiredFirebaseEnv = {
  apiKey: 'VITE_FIREBASE_API_KEY',
  authDomain: 'VITE_FIREBASE_AUTH_DOMAIN',
  projectId: 'VITE_FIREBASE_PROJECT_ID',
  storageBucket: 'VITE_FIREBASE_STORAGE_BUCKET',
  messagingSenderId: 'VITE_FIREBASE_MESSAGING_SENDER_ID',
  appId: 'VITE_FIREBASE_APP_ID',
  firestoreDatabaseId: 'VITE_FIREBASE_DATABASE_ID',
} as const;

const missingFirebaseEnv = Object.values(requiredFirebaseEnv).filter(key => !import.meta.env[key]);
if (missingFirebaseEnv.length) {
  throw new Error(`Missing Firebase configuration: ${missingFirebaseEnv.join(', ')}`);
}

const firebaseConfig = {
  apiKey: import.meta.env[requiredFirebaseEnv.apiKey],
  authDomain: import.meta.env[requiredFirebaseEnv.authDomain],
  projectId: import.meta.env[requiredFirebaseEnv.projectId],
  storageBucket: import.meta.env[requiredFirebaseEnv.storageBucket],
  messagingSenderId: import.meta.env[requiredFirebaseEnv.messagingSenderId],
  appId: import.meta.env[requiredFirebaseEnv.appId],
};

const app = initializeApp(firebaseConfig);
export const db = getFirestore(app, import.meta.env[requiredFirebaseEnv.firestoreDatabaseId]);
export const auth = getAuth(app);
export const googleProvider = new GoogleAuthProvider();

export enum OperationType {
  CREATE = 'create',
  UPDATE = 'update',
  DELETE = 'delete',
  LIST = 'list',
  GET = 'get',
  WRITE = 'write',
}

export interface FirestoreErrorInfo {
  error: string;
  operationType: OperationType;
  path: string | null;
  authInfo: {
    userId?: string | null;
    email?: string | null;
    emailVerified?: boolean | null;
    isAnonymous?: boolean | null;
    tenantId?: string | null;
    providerInfo?: {
      providerId?: string | null;
      email?: string | null;
    }[];
  };
}

export function handleFirestoreError(error: unknown, operationType: OperationType, path: string | null): never {
  const errInfo: FirestoreErrorInfo = {
    error: error instanceof Error ? error.message : String(error),
    authInfo: {
      userId: auth.currentUser?.uid,
      email: auth.currentUser?.email,
      emailVerified: auth.currentUser?.emailVerified,
      isAnonymous: auth.currentUser?.isAnonymous,
      tenantId: auth.currentUser?.tenantId,
      providerInfo: auth.currentUser?.providerData?.map(provider => ({
        providerId: provider.providerId,
        email: provider.email,
      })) || []
    },
    operationType,
    path
  };
  console.error('Firestore Error: ', JSON.stringify(errInfo));
  throw new Error(JSON.stringify(errInfo));
}

export async function testConnection() {
  try {
    await getDocFromServer(doc(db, 'test', 'connection'));
  } catch (error) {
    if (error instanceof Error && error.message.includes('the client is offline')) {
      console.error('Please check your Firebase configuration.');
    }
  }
}

export { signInWithPopup, signOut };
