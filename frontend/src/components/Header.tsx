import { Link, NavLink, useLocation, useNavigate, useSearchParams } from 'react-router';
import { useCart } from '../api/queries';
import { useAuth } from '../auth/AuthContext';
import { parseCatalogParams, toSearchParams } from '../lib/catalog';
import { MenuButton } from './MenuButton';

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

/** Menu akun pembeli. Header toko tidak pernah memuat tautan ke admin. */
function AccountMenu() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  return (
    <MenuButton
      ariaLabel={`Menu akun ${user?.name ?? ''}`}
      trigger={
        <>
          <span>{user?.name.split(' ')[0] ?? 'Akun'}</span>
          <span className="sel-chev" aria-hidden="true" />
        </>
      }
      header={user?.email}
      entries={[
        { key: 'orders', label: 'Riwayat order', asChild: <Link to="/order">Riwayat order</Link> },
        {
          key: 'logout',
          label: 'Keluar',
          separatorBefore: true,
          onSelect: async () => {
            await logout();
            navigate('/');
          },
        },
      ]}
    />
  );
}

export function Header() {
  const { status } = useAuth();
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
            {status === 'authenticated' ? <AccountMenu /> : status === 'anonymous' ? <NavLink to="/masuk">Masuk</NavLink> : null}
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
