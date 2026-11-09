import { RequireAuth } from './Guards';

export function Guarded({ role }: { role?: 'ADMIN' }) {
  return <RequireAuth role={role} />;
}
