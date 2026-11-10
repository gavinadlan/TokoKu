import { RequireAuth } from './Guards';

export function Guarded() {
  return <RequireAuth />;
}
