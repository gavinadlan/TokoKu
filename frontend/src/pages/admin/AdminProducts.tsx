import { useState } from 'react';
import { Link } from 'react-router';
import { useAdminProducts, useCategories, useDeactivateProduct, type ProductSort } from '../../api/queries';
import type { Product } from '../../api/types';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { MenuButton } from '../../components/MenuButton';
import { Pager } from '../../components/Pager';
import { ProductVisual } from '../../components/ProductVisual';
import { Select } from '../../components/Select';
import { SortHeader, nextSort, type SortDir } from '../../components/SortHeader';
import { EmptyState, ErrorState, ListSkeleton } from '../../components/States';
import { useToast } from '../../components/Toast';
import { messageOf } from '../../lib/errors';
import { formatRupiah, productNumber } from '../../lib/format';
import { useDocumentTitle } from '../../lib/useDocumentTitle';
import { AdminPage } from './AdminShell';

export function AdminProductsPage() {
  useDocumentTitle('Produk | TokoKu Admin');
  const [q, setQ] = useState('');
  const [active, setActive] = useState<'all' | 'true' | 'false'>('all');
  const [category, setCategory] = useState('all');
  const [page, setPage] = useState(1);
  const [sort, setSort] = useState<{ field: string; dir: SortDir }>({ field: 'id', dir: 'desc' });
  const [toDeactivate, setToDeactivate] = useState<Product | null>(null);
  const categories = useCategories();
  const products = useAdminProducts({ q, active: active === 'all' ? null : active === 'true', category: category === 'all' ? null : Number(category), page, sort: `${sort.field},${sort.dir}` as ProductSort });
  const deactivate = useDeactivateProduct();
  const toast = useToast();
  const onSort = (field: string) => { setSort((s) => nextSort(s, field)); setPage(1); };

  const total = products.data?.page.totalElements;

  return (
    <AdminPage title="Produk" sub={total === undefined ? undefined : `${total} produk`} actions={<Link className="btn small" to="/admin/produk/baru">Tambah produk</Link>}>
      <div className="toolrow">
        <label className="fld grow">
          Cari nama
          <input className="ctl" type="search" value={q} onChange={(e) => { setQ(e.target.value); setPage(1); }} />
        </label>
        <div className="fld">
          <label htmlFor="f-status">Status</label>
          <Select id="f-status" value={active} onChange={(v) => { setActive(v as typeof active); setPage(1); }} options={[{ value: 'all', label: 'Semua' }, { value: 'true', label: 'Aktif' }, { value: 'false', label: 'Nonaktif' }]} />
        </div>
        <div className="fld">
          <label htmlFor="f-cat">Kategori</label>
          <Select id="f-cat" value={category} onChange={(v) => { setCategory(v); setPage(1); }} options={[{ value: 'all', label: 'Semua kategori' }, ...(categories.data ?? []).map((c) => ({ value: String(c.id), label: c.name }))]} />
        </div>
      </div>

      {deactivate.error && <div className="alert err" role="alert">{messageOf(deactivate.error)}</div>}

      {products.isPending ? (
        <ListSkeleton />
      ) : products.isError ? (
        <ErrorState error={products.error} onRetry={() => products.refetch()} />
      ) : products.data.content.length === 0 ? (
        <EmptyState title="Tidak ada produk yang cocok">Ubah kata kunci atau filter.</EmptyState>
      ) : (
        <>
          <div className="tablebox" tabIndex={0} role="region" aria-label="Daftar produk">
            <table className="tbl">
              <thead>
                <tr>
                  <SortHeader label="No." field="id" sort={sort} onSort={onSort} className="sticky-col" />
                  <SortHeader label="Nama" field="name" sort={sort} onSort={onSort} />
                  <SortHeader label="Harga" field="price" sort={sort} onSort={onSort} numeric />
                  <SortHeader label="Stok" field="stock" sort={sort} onSort={onSort} numeric />
                  <th scope="col">Status</th>
                  <th scope="col"><span className="sr-only">Aksi</span></th>
                </tr>
              </thead>
              <tbody>
                {products.data.content.map((p) => (
                  <tr key={p.id}>
                    <td className="id sticky-col tnum">{productNumber(p.id)}</td>
                    <td>
                      <div className="nm">
                        <div className="nm-thumb"><ProductVisual id={p.id} categoryId={p.categoryId} name="" images={p.images} size="thumb" /></div>
                        <div>
                          <Link to={`/admin/produk/${p.id}`} className="nm-link">{p.name}</Link>
                          <small>{p.categoryName}{p.images.length > 0 ? ` · ${p.images.length} foto` : ''}</small>
                        </div>
                      </div>
                    </td>
                    <td className="n">{formatRupiah(p.price)}</td>
                    <td className="n">{p.stock}</td>
                    <td><span className={`status ${p.active ? 'done' : 'cancelled'}`}>{p.active ? 'Aktif' : 'Nonaktif'}</span></td>
                    <td>
                      <MenuButton
                        triggerClassName="row-trigger"
                        ariaLabel={`Aksi untuk ${p.name}`}
                        trigger="Aksi"
                        entries={[
                          { key: 'edit', label: 'Ubah produk', asChild: <Link to={`/admin/produk/${p.id}`}>Ubah produk</Link> },
                          { key: 'photos', label: 'Kelola foto', asChild: <Link to={`/admin/produk/${p.id}#foto`}>Kelola foto <span className="hint">{p.images.length} foto</span></Link> },
                          ...(p.active ? [{ key: 'off', label: 'Nonaktifkan', danger: true, separatorBefore: true, onSelect: () => setToDeactivate(p) }] : []),
                        ]}
                      />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pager page={page} totalPages={products.data.page.totalPages} onChange={setPage} />
        </>
      )}

      <ConfirmDialog
        open={toDeactivate !== null}
        title="Nonaktifkan produk ini?"
        confirmLabel="Nonaktifkan"
        danger
        busy={deactivate.isPending}
        onClose={() => setToDeactivate(null)}
        onConfirm={() => toDeactivate && deactivate.mutate(toDeactivate.id, { onSuccess: () => toast.notify(`${toDeactivate.name} dinonaktifkan.`), onSettled: () => setToDeactivate(null) })}
      >
        {toDeactivate?.name} tidak akan tampil di katalog. Datanya tetap tersimpan dan order lama tidak terpengaruh.
      </ConfirmDialog>
    </AdminPage>
  );
}
