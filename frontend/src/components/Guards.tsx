import { Navigate, Outlet, useLocation } from 'react-router';
import { useAuth } from '../auth/AuthContext';
import { decideAccess } from '../auth/guard';
import { ListSkeleton } from './States';

/** Rute toko yang butuh login pembeli. Anonim diarahkan ke /masuk lalu dikembalikan ke halaman asal. */
export function RequireAuth() {
  const { status, user } = useAuth();
  const location = useLocation();
  const decision = decideAccess(status, user);

  if (decision === 'wait') {
    return (
      <div className="wrap page-pad">
        <ListSkeleton />
      </div>
    );
  }
  if (decision !== 'allow') {
    return <Navigate to="/masuk" replace state={{ from: location.pathname + location.search, notice: 'Masuk dulu untuk melanjutkan.' }} />;
  }
  return <Outlet />;
}
