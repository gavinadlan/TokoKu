import { Outlet, ScrollRestoration } from 'react-router';
import { Header } from './Header';
import { RouteFocus } from './RouteFocus';
import { ToastProvider } from './Toast';

export function Layout() {
  return (
    <ToastProvider>
      <a className="skip" href="#konten">
        Lewati ke konten
      </a>
      <Header />
      <main id="konten">
        <Outlet />
      </main>
      <RouteFocus />
      <ScrollRestoration />
    </ToastProvider>
  );
}
