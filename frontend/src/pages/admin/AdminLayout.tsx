import { NavLink, Outlet } from 'react-router';

export function AdminLayout() {
  return (
    <div className="wrap admin">
      <nav className="admin-nav" aria-label="Kelola toko">
        <NavLink to="/admin/produk">Produk</NavLink>
        <NavLink to="/admin/kategori">Kategori</NavLink>
        <NavLink to="/admin/order">Order</NavLink>
      </nav>
      <div className="stack" style={{ minWidth: 0 }}>
        <Outlet />
      </div>
    </div>
  );
}
