import { createBrowserRouter, Navigate, type RouteObject } from 'react-router';
import { Guarded } from './components/GuardedRoute';
import { Layout } from './components/Layout';
import { NotFoundPage, RouteError } from './pages/NotFound';

/* Halaman dimuat malas (code splitting): katalog tidak mengunduh kode admin, dan sebaliknya. */
const lazy = <T extends Record<string, unknown>>(load: () => Promise<T>, key: keyof T) => async () => ({ Component: (await load())[key] as never });

const storeRoutes: RouteObject = {
  path: '/',
  Component: Layout,
  ErrorBoundary: RouteError,
  children: [
    { index: true, lazy: lazy(() => import('./pages/Catalog'), 'CatalogPage') },
    { path: 'produk/:id', lazy: lazy(() => import('./pages/Product'), 'ProductPage') },
    { path: 'masuk', lazy: lazy(() => import('./pages/Auth'), 'LoginPage') },
    { path: 'daftar', lazy: lazy(() => import('./pages/Auth'), 'RegisterPage') },
    {
      Component: Guarded,
      children: [
        { path: 'keranjang', lazy: lazy(() => import('./pages/Cart'), 'CartPage') },
        { path: 'checkout', lazy: lazy(() => import('./pages/Checkout'), 'CheckoutPage') },
        { path: 'order', lazy: lazy(() => import('./pages/Orders'), 'OrdersPage') },
        { path: 'order/:id', lazy: lazy(() => import('./pages/Orders'), 'OrderDetailPage') },
        { path: 'order/:id/status', lazy: lazy(() => import('./pages/PaymentStatus'), 'OrderStatusPage') },
        { path: 'pembayaran/selesai', lazy: lazy(() => import('./pages/PaymentStatus'), 'PaymentReturnPage') },
        ...(import.meta.env.DEV ? [{ path: 'dev/bayar/:ref', lazy: lazy(() => import('./pages/DevPay'), 'DevPayPage') }] : []),
      ],
    },
    { path: '*', Component: NotFoundPage },
  ],
};

// Area admin: kerangka, sesi, dan kode sendiri; tidak berbagi header atau sesi dengan toko
const adminRoutes: RouteObject = {
  path: '/admin',
  lazy: lazy(() => import('./pages/admin/AdminRoot'), 'AdminRoot'),
  ErrorBoundary: RouteError,
  children: [
    { path: 'masuk', lazy: lazy(() => import('./pages/admin/AdminLogin'), 'AdminLoginPage') },
    {
      lazy: lazy(() => import('./pages/admin/AdminShell'), 'AdminShell'),
      children: [
        { index: true, element: <Navigate to="/admin/produk" replace /> },
        { path: 'produk', lazy: lazy(() => import('./pages/admin/AdminProducts'), 'AdminProductsPage') },
        { path: 'produk/baru', lazy: lazy(() => import('./pages/admin/AdminProductEdit'), 'AdminProductEditPage') },
        { path: 'produk/:id', lazy: lazy(() => import('./pages/admin/AdminProductEdit'), 'AdminProductEditPage') },
        { path: 'kategori', lazy: lazy(() => import('./pages/admin/AdminCategories'), 'AdminCategoriesPage') },
        { path: 'order', lazy: lazy(() => import('./pages/admin/AdminOrders'), 'AdminOrdersPage') },
        { path: 'pengaturan', lazy: lazy(() => import('./pages/admin/AdminStore'), 'AdminStorePage') },
        { path: '*', Component: NotFoundPage },
      ],
    },
  ],
};

export const router = createBrowserRouter([adminRoutes, storeRoutes]);
