import { useEffect, useRef } from 'react';
import { useLocation } from 'react-router';

/**
 * Setelah pindah halaman (path berubah), fokus dipindah ke judul halaman agar pengguna keyboard dan pembaca layar
 * tidak tertinggal di tautan sebelumnya. Tidak dijalankan pada pemuatan pertama, dan tidak untuk perubahan query
 * (filter katalog).
 */
export function RouteFocus() {
  const { pathname } = useLocation();
  const first = useRef(true);
  useEffect(() => {
    if (first.current) {
      first.current = false;
      return;
    }
    const id = requestAnimationFrame(() => {
      const target = document.querySelector<HTMLElement>('main h1') ?? document.getElementById('konten');
      if (!target) return;
      if (!target.hasAttribute('tabindex')) target.setAttribute('tabindex', '-1');
      target.focus({ preventScroll: true });
    });
    return () => cancelAnimationFrame(id);
  }, [pathname]);
  return null;
}
