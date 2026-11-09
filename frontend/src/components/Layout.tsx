import { Outlet, ScrollRestoration } from 'react-router';
import { Header } from './Header';

export function Layout() {
  return (
    <>
      <a className="skip" href="#konten">
        Lewati ke konten
      </a>
      <Header />
      <main id="konten" tabIndex={-1}>
        <Outlet />
      </main>
      <ScrollRestoration />
    </>
  );
}
