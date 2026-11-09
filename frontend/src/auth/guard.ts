import type { User } from '../api/types';

export type AuthStatus = 'loading' | 'authenticated' | 'anonymous';

export type AccessDecision = 'wait' | 'allow' | 'login' | 'forbidden';

/** Keputusan akses rute: tunggu sesi dipulihkan, izinkan, minta masuk, atau tolak karena role. */
export function decideAccess(status: AuthStatus, user: User | null, requiredRole?: 'ADMIN'): AccessDecision {
  if (status === 'loading') return 'wait';
  if (status === 'anonymous' || !user) return 'login';
  if (requiredRole && user.role !== requiredRole) return 'forbidden';
  return 'allow';
}
