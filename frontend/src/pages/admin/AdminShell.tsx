import { useCallback, useEffect, useRef, useState } from 'react';
import { Navigate, NavLink, Outlet, ScrollRestoration, useLocation } from 'react-router';
import { useAdminOrderSummary, useAdminStoreSettings } from '../../api/queries';
import { useAdminAuth } from '../../auth/AdminAuthContext';
import { decideAccess } from '../../auth/guard';
import { getSeenAt } from '../../lib/adminSeen';
import { RouteFocus } from '../../components/RouteFocus';
import { ListSkeleton } from '../../components/States';

/**
 * Tab menu admin di ponsel: bisa digeser, tepi memudar bila masih ada tab di luar layar, dan tab aktif
 * digulirkan ke tampilan. Di desktop (rel samping) tidak berefek.
 */
function useScrollFade(activeKey: string) {
  const ref = useRef<HTMLElement>(null);
  const [fade, setFade] = useState({ l: false, r: false });
  const update = useCallback(() => {
    const el = ref.current;
    if (el) setFade({ l: el.scrollLeft > 4, r: el.scrollLeft + el.clientWidth < el.scrollWidth - 4 });
  }, []);
  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    const cur = el.querySelector<HTMLElement>('[aria-current="page"]');
    if (cur && el.scrollWidth > el.clientWidth) el.scrollTo({ left: cur.offsetLeft - (el.clientWidth - cur.offsetWidth) / 2, behavior: 'auto' });
    update();
    const ro = new ResizeObserver(update);
    ro.observe(el);
    return () => ro.disconnect();
  }, [activeKey, update]);
  return { ref, update, fadeL: fade.l, fadeR: fade.r };
}

/**
 * Kerangka admin: rel samping gelap (ponsel: baris tab), terpisah dari tampilan toko. Tanpa sesi admin
 * pengguna diarahkan ke /admin/masuk (bukan halaman 403, supaya keberadaan area ini tidak "dijelaskan").
 */
export function AdminShell() {
  const { status, user, logout } = useAdminAuth();
  const location = useLocation();
  const decision = decideAccess(status, user, 'ADMIN');
  const store = useAdminStoreSettings();
  // Lencana "Order": pesanan dibayar sejak kunjungan terakhir. Di halaman Order polling dipegang halaman itu sendiri.
  const onOrders = location.pathname.startsWith('/admin/order');
  const seenAt = getSeenAt();
  const summary = useAdminOrderSummary(seenAt, !onOrders && seenAt !== null);
  const { ref: navRef, update: navScroll, fadeL, fadeR } = useScrollFade(location.pathname + decision);
  const fresh = !onOrders && seenAt ? summary.data?.paidSince ?? 0 : 0;

  if (decision === 'wait') return <div className="adm" style={{ minHeight: '100vh', padding: 'var(--s-7) var(--gutter)' }}><ListSkeleton /></div>;
  if (decision !== 'allow') return <Navigate to="/admin/masuk" replace state={{ from: location.pathname + location.search }} />;

  if (user?.mustChangePassword) return <Navigate to="/admin/ganti-password" replace />;

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
        <nav aria-label="Kelola toko" ref={navRef} onScroll={navScroll} data-fade-l={fadeL || undefined} data-fade-r={fadeR || undefined}>
          <NavLink to="/admin/produk">Produk</NavLink>
          <NavLink to="/admin/kategori">Kategori</NavLink>
          <NavLink to="/admin/order">
            Order
            <span role="status" aria-live="polite" className="nav-badge-wrap">
              {fresh > 0 && (
                <>
                  <span className="nav-badge" aria-hidden="true">{fresh}</span>
                  <span className="sr-only"> {fresh} order baru dibayar</span>
                </>
              )}
            </span>
          </NavLink>
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
