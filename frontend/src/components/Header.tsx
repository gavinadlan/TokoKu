import { useEffect, useRef, useState } from 'react';
import { Link, NavLink, useLocation, useNavigate, useSearchParams } from 'react-router';
import { useCart } from '../api/queries';
import { useAuth } from '../auth/AuthContext';
import { parseCatalogParams, toSearchParams } from '../lib/catalog';

function SearchField({ id, className }: { id: string; className: string }) {
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const [sp] = useSearchParams();
  const current = pathname === '/' ? parseCatalogParams(sp).q : '';

  return (
    <form
      className={`search ${className}`}
      role="search"
      onSubmit={(e) => {
        e.preventDefault();
        const q = String(new FormData(e.currentTarget).get('q') ?? '').trim();
        navigate({ pathname: '/', search: toSearchParams({ q, category: null, sort: 'terbaru', page: 1 }).toString() });
      }}
    >
      <label className="sr-only" htmlFor={id}>
        Cari produk
      </label>
      {/* key: kolom ikut berubah saat q di URL berubah (mis. tombol back) */}
      <input key={current} id={id} name="q" type="search" defaultValue={current} placeholder="Cari kopi, dripper, grinder" autoComplete="off" />
    </form>
  );
}

function AccountMenu() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDetailsElement>(null);

  useEffect(() => {
    if (!open) return;
    const onDown = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && setOpen(false);
    document.addEventListener('mousedown', onDown);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('mousedown', onDown);
      document.removeEventListener('keydown', onKey);
    };
  }, [open]);

  return (
    <details className="account" ref={ref} open={open} onToggle={(e) => setOpen((e.target as HTMLDetailsElement).open)}>
      <summary>{user?.name.split(' ')[0] ?? 'Akun'}</summary>
      <div className="menu">
        <p className="muted tiny">{user?.email}</p>
        <Link to="/order">Riwayat order</Link>
        {user?.role === 'ADMIN' && <Link to="/admin/produk">Kelola toko</Link>}
        <button
          type="button"
          onClick={async () => {
            await logout();
            navigate('/');
          }}
        >
          Keluar
        </button>
      </div>
    </details>
  );
}

export function Header() {
  const { status } = useAuth();
  const { pathname } = useLocation();
  const cart = useCart();
  const count = cart.data?.totalQuantity ?? 0;

  return (
    <header className="site-header">
      <div className="wrap">
        <div className="bar">
          <Link className="wordmark" to="/" aria-label="TokoKu, ke beranda">
            Toko<span>Ku</span>
          </Link>
          <SearchField id="q-desktop" className="desktop" />
          <nav className="nav" aria-label="Utama">
            {status === 'authenticated' ? <AccountMenu key={pathname} /> : status === 'anonymous' ? <NavLink to="/masuk">Masuk</NavLink> : null}
            <NavLink to="/keranjang">
              Keranjang
              {count > 0 && (
                <>
                  <span className="cart-count bump tnum" key={count} aria-hidden="true">
                    {count}
                  </span>
                  <span className="sr-only">, {count} barang</span>
                </>
              )}
            </NavLink>
          </nav>
        </div>
        <div className="search-row">
          <SearchField id="q-mobile" className="mobile" />
        </div>
      </div>
    </header>
  );
}
