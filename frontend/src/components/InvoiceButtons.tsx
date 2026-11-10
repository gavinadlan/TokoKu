import { useState } from 'react';
import { fetchInvoice } from '../api/queries';
import { messageOf } from '../lib/errors';

/**
 * "Lihat invoice" membuka PDF di tab baru; "Unduh PDF" menyimpan berkas. Endpoint butuh Bearer token, jadi PDF
 * diambil dengan fetch terautentikasi lalu dibuka sebagai blob (tidak bisa lewat tautan biasa).
 */
export function InvoiceButtons({ orderId, number, realm, compact }: { orderId: number; number: string; realm: 'store' | 'admin'; compact?: boolean }) {
  const [busy, setBusy] = useState<'view' | 'download' | null>(null);
  const [error, setError] = useState<string | null>(null);

  const run = async (mode: 'view' | 'download') => {
    setBusy(mode);
    setError(null);
    // Tab dibuka sinkron (dalam gestur klik) agar tidak diblokir pop-up blocker, lalu diisi setelah PDF siap
    const tab = mode === 'view' ? window.open('', '_blank') : null;
    try {
      const blob = await fetchInvoice(orderId, realm);
      const url = URL.createObjectURL(blob);
      if (mode === 'view') {
        if (tab) tab.location.href = url;
        else window.location.assign(url);
      } else {
        const a = document.createElement('a');
        a.href = url;
        a.download = `${number}.pdf`;
        document.body.appendChild(a);
        a.click();
        a.remove();
      }
      setTimeout(() => URL.revokeObjectURL(url), 60_000);
    } catch (e) {
      tab?.close();
      setError(messageOf(e));
    } finally {
      setBusy(null);
    }
  };

  return (
    <div className={`invoice-actions ${compact ? 'compact' : ''}`}>
      <div className="btnrow">
        <button type="button" className="btn ghost small" disabled={busy !== null} onClick={() => run('view')} aria-label={compact ? `Lihat invoice ${number}` : undefined}>
          {busy === 'view' ? 'Menyiapkan...' : compact ? 'Lihat' : 'Lihat invoice'}
        </button>
        <button type="button" className="btn ghost small" disabled={busy !== null} onClick={() => run('download')} aria-label={compact ? `Unduh PDF invoice ${number}` : undefined}>
          {busy === 'download' ? 'Mengunduh...' : compact ? 'Unduh PDF' : 'Unduh PDF'}
        </button>
      </div>
      {error && (
        <div className="alert err" role="alert">
          {error}
        </div>
      )}
    </div>
  );
}
