import type { ReactNode } from 'react';
import { messageOf } from '../lib/errors';

export function EmptyState({ title, children, actions }: { title: string; children?: ReactNode; actions?: ReactNode }) {
  return (
    <div className="state">
      <h2>{title}</h2>
      {children && <p className="muted">{children}</p>}
      {actions && <div className="actions">{actions}</div>}
    </div>
  );
}

export function ErrorState({ error, onRetry, title = 'Gagal memuat data' }: { error: unknown; onRetry?: () => void; title?: string }) {
  return (
    <div className="state" role="alert">
      <h2>{title}</h2>
      <p className="muted">{messageOf(error)}</p>
      {onRetry && (
        <div className="actions">
          <button className="btn ghost" type="button" onClick={onRetry}>
            Coba lagi
          </button>
        </div>
      )}
    </div>
  );
}

export function ProductGridSkeleton({ count = 8 }: { count?: number }) {
  return (
    <ul className="grid" aria-busy="true" aria-label="Memuat produk">
      {Array.from({ length: count }, (_, i) => (
        <li key={i}>
          <div className="skeleton vis-s" />
          <div className="skeleton line-s short" />
          <div className="skeleton line-s" />
          <div className="skeleton line-s short" />
        </li>
      ))}
    </ul>
  );
}

export function ListSkeleton({ rows = 4 }: { rows?: number }) {
  return (
    <div aria-busy="true" aria-label="Memuat">
      {Array.from({ length: rows }, (_, i) => (
        <div key={i} className="skeleton line-s" style={{ height: 56, marginTop: 12 }} />
      ))}
    </div>
  );
}
