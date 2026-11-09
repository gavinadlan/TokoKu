import { useSearchParams } from 'react-router';
import { useCategories, useProducts } from '../api/queries';
import { ErrorState, EmptyState, ProductGridSkeleton } from '../components/States';
import { Pager } from '../components/Pager';
import { ProductCard } from '../components/ProductCard';
import { parseCatalogParams, SORT_OPTIONS, toSearchParams, type CatalogParams, type SortKey } from '../lib/catalog';
import { useDocumentTitle } from '../lib/useDocumentTitle';

function SortSelect({ id, value, onChange }: { id: string; value: SortKey; onChange(v: SortKey): void }) {
  return (
    <label className="sort" htmlFor={id}>
      Urutkan
      <select id={id} value={value} onChange={(e) => onChange(e.target.value as SortKey)}>
        {SORT_OPTIONS.map((o) => (
          <option key={o.key} value={o.key}>
            {o.label}
          </option>
        ))}
      </select>
    </label>
  );
}

export function CatalogPage() {
  useDocumentTitle('Kopi dan perlengkapan seduh');
  const [sp, setSp] = useSearchParams();
  const params = parseCatalogParams(sp);
  const categories = useCategories();
  const products = useProducts(params);

  // Filter berubah -> kembali ke halaman 1; semua state hidup di URL (bisa dibagikan, back berfungsi)
  const update = (patch: Partial<CatalogParams>) => {
    const next = { ...params, ...patch };
    if (!('page' in patch)) next.page = 1;
    setSp(toSearchParams(next));
  };

  const total = products.data?.page.totalElements;
  const totalPages = products.data?.page.totalPages ?? 1;
  const countText = total === undefined ? '' : `${total} produk`;

  return (
    <div className="wrap">
      <div className="catalog-head">
        <h1 className="page-title" style={{ maxWidth: '14ch' }}>
          Kopi dan <em>perlengkapan</em> seduh
        </h1>
        <div className="meta">
          <span className="count tnum" aria-live="polite">
            {countText}
          </span>
          <SortSelect id="sort-desktop" value={params.sort} onChange={(sort) => update({ sort })} />
        </div>
      </div>

      <div className="toolbar">
        <div className="chips" role="group" aria-label="Kategori">
          <button type="button" className="chip" aria-pressed={params.category === null} onClick={() => update({ category: null })}>
            Semua
          </button>
          {categories.data?.map((c) => (
            <button key={c.id} type="button" className="chip" aria-pressed={params.category === c.id} onClick={() => update({ category: c.id })}>
              {c.name}
            </button>
          ))}
        </div>
      </div>
      <div className="sort-mobile">
        <span className="tnum">{countText}</span>
        <SortSelect id="sort-mobile" value={params.sort} onChange={(sort) => update({ sort })} />
      </div>

      {params.q && (
        <p className="muted" style={{ paddingTop: 'var(--s-4)' }}>
          Hasil untuk <strong>&ldquo;{params.q}&rdquo;</strong>
        </p>
      )}

      {products.isPending ? (
        <ProductGridSkeleton />
      ) : products.isError ? (
        <ErrorState error={products.error} onRetry={() => products.refetch()} title="Katalog belum bisa dimuat" />
      ) : products.data.content.length === 0 ? (
        <EmptyState
          title={params.q ? `Tidak ada produk untuk "${params.q}"` : 'Belum ada produk di kategori ini'}
          actions={
            <button className="btn ghost" type="button" onClick={() => setSp(new URLSearchParams())}>
              Hapus semua filter
            </button>
          }
        >
          Coba kata kunci lain, atau lihat semua produk.
        </EmptyState>
      ) : (
        <>
          <ul className="grid" style={products.isPlaceholderData ? { opacity: 0.6 } : undefined}>
            {products.data.content.map((p) => (
              <li key={p.id}>
                <ProductCard product={p} />
              </li>
            ))}
          </ul>
          <Pager page={params.page} totalPages={totalPages} onChange={(page) => update({ page })} />
        </>
      )}
    </div>
  );
}
