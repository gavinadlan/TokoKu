import { Link, isRouteErrorResponse, useRouteError } from 'react-router';
import { useDocumentTitle } from '../lib/useDocumentTitle';

export function NotFoundView({ title = 'Halaman tidak ditemukan', message = 'Alamat yang Anda buka tidak ada, atau produknya sudah tidak dijual.', code = '404' }: { title?: string; message?: string; code?: string }) {
  useDocumentTitle(title);
  return (
    <div className="wrap page-pad notfound">
      <p className="nf-num" aria-hidden="true">
        {code}
      </p>
      <div className="state" style={{ paddingTop: 0 }}>
        <h1 className="page-title" style={{ fontSize: 'var(--text-3xl)' }}>
          {title}
        </h1>
        <p className="muted">{message}</p>
        <div className="actions">
          <Link className="btn" to="/">
            Kembali ke katalog
          </Link>
        </div>
      </div>
    </div>
  );
}

export function NotFoundPage() {
  return <NotFoundView />;
}

/** Penangkap error rute/render: halaman yang dirancang, bukan layar putih. */
export function RouteError() {
  const error = useRouteError();
  const notFound = isRouteErrorResponse(error) && error.status === 404;
  if (notFound) return <NotFoundView />;
  return <NotFoundView code="Oops" title="Terjadi kesalahan" message="Halaman gagal ditampilkan. Muat ulang halaman, atau kembali ke katalog." />;
}
