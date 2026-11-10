import { Navigate, NavLink, Outlet, ScrollRestoration, useLocation } from 'react-router';
import { useAdminStoreSettings } from '../../api/queries';
import { useAdminAuth } from '../../auth/AdminAuthContext';
import { decideAccess } from '../../auth/guard';
import { RouteFocus } from '../../components/RouteFocus';
import { ListSkeleton } from '../../components/States';

/**
 * Kerangka admin: rel samping gelap (ponsel: baris tab), terpisah dari tampilan toko. Tanpa sesi admin
 * pengguna diarahkan ke /admin/masuk (bukan halaman 403, supaya keberadaan area ini tidak "dijelaskan").
 */
export function AdminShell() {
  const { status, user, logout } = useAdminAuth();
  const location = useLocation();
  const decision = decideAccess(status, user, 'ADMIN');
  const store = useAdminStoreSettings();

  if (decision === 'wait') return <div className="adm" style={{ minHeight: '100vh', padding: 'var(--s-7) var(--gutter)' }}><ListSkeleton /></div>;
  if (decision !== 'allow') return <Navigate to="/admin/masuk" replace state={{ from: location.pathname + location.search }} />;

  return (
    <div className="adm adm-shell">
      <a className="skip" href="#konten">
        Lewati ke konten
      </a>
      <aside className="rail">
        <div className="brand">
          <b>TokoKu</b>
          <i>Admin</i>
        </div>
        <nav aria-label="Kelola toko">
          <NavLink to="/admin/produk">Produk</NavLink>
          <NavLink to="/admin/kategori">Kategori</NavLink>
          <NavLink to="/admin/order">Order</NavLink>
          <NavLink to="/admin/pengaturan">Pengaturan toko</NavLink>
        </nav>
        <div className="who">
          <b>{user?.name}</b>
          <span>{user?.email}</span>
          <button type="button" className="rail-logout" onClick={() => logout()}>
            Keluar
          </button>
        </div>
      </aside>
      <div className="adm-main">
        {store.data && !store.data.configured && (
          <div className="banner-warn" role="status">
            <b>Lengkapi pengaturan toko.</b> Nama, alamat, dan telepon toko belum diisi, jadi invoice belum bisa diterbitkan dan pengambilan di toko belum tersedia.{' '}
            <NavLink to="/admin/pengaturan">Buka pengaturan</NavLink>
          </div>
        )}
        <main id="konten" className="adm-content">
          <Outlet />
        </main>
      </div>
      <RouteFocus />
      <ScrollRestoration />
    </div>
  );
}

/** Judul halaman admin yang seragam: serif ringan, keterangan kecil, dan aksi di kanan. */
export function AdminPage({ title, sub, actions, children }: { title: string; sub?: string; actions?: React.ReactNode; children: React.ReactNode }) {
  return (
    <>
      <header className="topbar">
        <div>
          <h1>{title}</h1>
          {sub && <div className="sub">{sub}</div>}
        </div>
        {actions && <div className="topbar-actions">{actions}</div>}
      </header>
      <div className="content">{children}</div>
    </>
  );
}
