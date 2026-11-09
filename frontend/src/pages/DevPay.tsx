import { useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router';
import { api, call } from '../api/client';
import { messageOf } from '../lib/errors';

/** Simulasi pembayaran untuk provider "fake". Rute ini hanya didaftarkan di build dev (lihat router). */
export function DevPayPage() {
  const { ref = '' } = useParams();
  const orderId = useSearchParams()[0].get('order');
  const navigate = useNavigate();
  const qc = useQueryClient();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const simulate = async (status: 'settlement' | 'expire' | 'deny') => {
    setBusy(true);
    setError(null);
    try {
      await call(() => api.POST('/api/dev/payments/{ref}/simulate', { params: { path: { ref }, query: { status } } }));
      await qc.invalidateQueries();
      navigate(`/order/${orderId}/status`);
    } catch (e) {
      setError(messageOf(e));
      setBusy(false);
    }
  };

  return (
    <div className="wrap narrow page-pad">
      <div className="pay-result">
        <h1>Simulasi pembayaran</h1>
        <p className="muted">
          Halaman ini hanya ada di lingkungan pengembangan dengan provider pembayaran <code>fake</code>. Referensi: <code>{ref}</code>
        </p>
        {error && <div className="alert err" role="alert">{error}</div>}
        <div style={{ display: 'grid', gap: 'var(--s-3)' }}>
          <button className="btn" type="button" disabled={busy} onClick={() => simulate('settlement')}>Simulasikan pembayaran berhasil</button>
          <button className="btn ghost" type="button" disabled={busy} onClick={() => simulate('expire')}>Simulasikan kedaluwarsa</button>
          <button className="btn ghost" type="button" disabled={busy} onClick={() => simulate('deny')}>Simulasikan ditolak</button>
        </div>
      </div>
    </div>
  );
}
