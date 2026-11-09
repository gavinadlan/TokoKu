import { Navigate, Outlet, useLocation } from 'react-router';
import { useAuth } from '../auth/AuthContext';
import { decideAccess } from '../auth/guard';
import { ListSkeleton } from './States';
import { NotFoundView } from '../pages/NotFound';

/** Rute yang butuh login (dan opsional role). Anonim diarahkan ke /masuk lalu dikembalikan ke halaman asal. */
export function RequireAuth({ role }: { role?: 'ADMIN' }) {
  const { status, user } = useAuth();
  const location = useLocation();
  const decision = decideAccess(status, user, role);

  if (decision === 'wait') {
    return (
      <div className="wrap page-pad">
        <ListSkeleton />
      </div>
    );
  }
  if (decision === 'login') {
    return <Navigate to="/masuk" replace state={{ from: location.pathname + location.search, notice: 'Masuk dulu untuk melanjutkan.' }} />;
  }
  if (decision === 'forbidden') {
    return <NotFoundView title="Halaman ini khusus admin" message="Akun Anda tidak punya akses ke bagian ini." />;
  }
  return <Outlet />;
}
