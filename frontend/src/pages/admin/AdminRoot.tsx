import { useEffect } from 'react';
import { Outlet } from 'react-router';
import { AdminAuthProvider } from '../../auth/AdminAuthContext';
import { ToastProvider } from '../../components/Toast';

/** Akar rute admin: sesi admin sendiri, toast sendiri, dan halaman tidak boleh diindeks mesin pencari. */
export function AdminRoot() {
  useEffect(() => {
    const meta = document.createElement('meta');
    meta.name = 'robots';
    meta.content = 'noindex, nofollow';
    document.head.appendChild(meta);
    return () => meta.remove();
  }, []);
  return (
    <AdminAuthProvider>
      <ToastProvider>
        <Outlet />
      </ToastProvider>
    </AdminAuthProvider>
  );
}
