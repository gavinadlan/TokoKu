import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { useAdminProducts, useCategories, useDeactivateProduct, useSaveProduct } from '../../api/queries';
import type { Product } from '../../api/types';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { Field } from '../../components/Field';
import { Pager } from '../../components/Pager';
import { EmptyState, ErrorState, ListSkeleton } from '../../components/States';
import { messageOf } from '../../lib/errors';
import { formatRupiah, productNumber } from '../../lib/format';
import { useDocumentTitle } from '../../lib/useDocumentTitle';

const schema = z.object({
  name: z.string().trim().min(2, 'Isi nama produk.').max(200, 'Nama terlalu panjang.'),
  categoryId: z.number({ error: 'Pilih kategori.' }).int().positive('Pilih kategori.'),
  description: z.string().max(2000, 'Deskripsi terlalu panjang.'),
  price: z.number({ error: 'Isi harga dalam angka.' }).min(0, 'Harga tidak boleh negatif.').max(9_999_999_999, 'Harga terlalu besar.'),
  stock: z.number({ error: 'Isi stok dalam angka.' }).int('Stok harus bilangan bulat.').min(0, 'Stok tidak boleh negatif.'),
  imageUrl: z.string().trim().max(500, 'URL terlalu panjang.').refine((v) => v === '' || /^https?:\/\/\S+$/.test(v), 'Isi URL lengkap berawalan http:// atau https://'),
});
type Values = z.infer<typeof schema>;

function ProductForm({ product, onDone }: { product: Product | null; onDone(): void }) {
  const categories = useCategories();
  const save = useSaveProduct();
  const [error, setError] = useState<string | null>(null);
  const form = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: product
      ? { name: product.name, categoryId: product.categoryId, description: product.description ?? '', price: product.price, stock: product.stock, imageUrl: product.imageUrl ?? '' }
      : { name: '', categoryId: undefined, description: '', price: undefined, stock: 0, imageUrl: '' },
  });
  const { errors } = form.formState;

  return (
    <form
      className="form-2 cols"
      noValidate
      aria-label={product ? 'Ubah produk' : 'Tambah produk'}
      onSubmit={form.handleSubmit(async (v) => {
        setError(null);
        try {
          await save.mutateAsync({
            id: product?.id,
            body: { name: v.name.trim(), categoryId: v.categoryId, description: v.description.trim() || undefined, price: v.price, stock: v.stock, imageUrl: v.imageUrl.trim() || undefined },
          });
          onDone();
        } catch (e) {
          setError(messageOf(e));
        }
      })}
    >
      <h2 className="span" style={{ fontFamily: 'var(--font-display)', fontWeight: 440, fontSize: 'var(--text-xl)' }}>
        {product ? `Ubah produk No. ${productNumber(product.id)}` : 'Produk baru'}
      </h2>
      {error && <div className="alert err span" role="alert">{error}</div>}
      <div className="span">
        <Field label="Nama produk" error={errors.name?.message}>
          <input className="input" {...form.register('name')} />
        </Field>
      </div>
      <Field label="Kategori" error={errors.categoryId?.message}>
        <select className="input" {...form.register('categoryId', { setValueAs: (v) => (v === '' ? undefined : Number(v)) })}>
          <option value="">Pilih kategori</option>
          {categories.data?.map((c) => (
            <option key={c.id} value={c.id}>{c.name}</option>
          ))}
        </select>
      </Field>
      <Field label="Harga (Rp)" error={errors.price?.message}>
        <input className="input" type="number" inputMode="numeric" min={0} step={1} {...form.register('price', { valueAsNumber: true })} />
      </Field>
      <Field label="Stok" error={errors.stock?.message}>
        <input className="input" type="number" inputMode="numeric" min={0} step={1} {...form.register('stock', { valueAsNumber: true })} />
      </Field>
      <Field label="URL gambar (opsional)" hint="Penyimpanan file belum ada; tempel alamat gambar yang sudah online." error={errors.imageUrl?.message}>
        <input className="input" type="url" inputMode="url" {...form.register('imageUrl')} />
      </Field>
      <div className="span">
        <Field label="Deskripsi (opsional)" error={errors.description?.message}>
          <textarea className="input" rows={3} {...form.register('description')} />
        </Field>
      </div>
      <div className="span" style={{ display: 'flex', gap: 'var(--s-3)' }}>
        <button className="btn" type="submit" disabled={save.isPending}>{save.isPending ? 'Menyimpan...' : 'Simpan produk'}</button>
        <button className="btn ghost" type="button" onClick={onDone}>Batal</button>
      </div>
    </form>
  );
}

export function AdminProductsPage() {
  useDocumentTitle('Kelola produk');
  const [q, setQ] = useState('');
  const [active, setActive] = useState<'all' | 'true' | 'false'>('all');
  const [page, setPage] = useState(1);
  const [editing, setEditing] = useState<Product | 'new' | null>(null);
  const [toDeactivate, setToDeactivate] = useState<Product | null>(null);
  const products = useAdminProducts({ q, active: active === 'all' ? null : active === 'true', page });
  const deactivate = useDeactivateProduct();

  return (
    <>
      <div className="row-between" style={{ flexWrap: 'wrap' }}>
        <h1>Produk</h1>
        <button className="btn" type="button" onClick={() => setEditing('new')}>Tambah produk</button>
      </div>

      {editing && <ProductForm key={editing === 'new' ? 'new' : editing.id} product={editing === 'new' ? null : editing} onDone={() => setEditing(null)} />}

      <div className="inline-form">
        <Field label="Cari nama">
          <input className="input" type="search" value={q} onChange={(e) => { setQ(e.target.value); setPage(1); }} />
        </Field>
        <Field label="Status">
          <select className="input" value={active} onChange={(e) => { setActive(e.target.value as typeof active); setPage(1); }}>
            <option value="all">Semua</option>
            <option value="true">Aktif</option>
            <option value="false">Nonaktif</option>
          </select>
        </Field>
      </div>

      {deactivate.error && <div className="alert err" role="alert">{messageOf(deactivate.error)}</div>}

      {products.isPending ? (
        <ListSkeleton />
      ) : products.isError ? (
        <ErrorState error={products.error} onRetry={() => products.refetch()} />
      ) : products.data.content.length === 0 ? (
        <EmptyState title="Tidak ada produk yang cocok">Ubah kata kunci atau filter status.</EmptyState>
      ) : (
        <>
          <div className="table-wrap" tabIndex={0} role="region" aria-label="Daftar produk">
            <table className="data">
              <thead>
                <tr><th>No.</th><th>Nama</th><th>Kategori</th><th className="num">Harga</th><th className="num">Stok</th><th><span className="sr-only">Aksi</span></th></tr>
              </thead>
              <tbody>
                {products.data.content.map((p) => (
                  <tr key={p.id}>
                    <td className="tnum">{productNumber(p.id)}</td>
                    <td>{p.name}{!p.active && <span className="inactive-tag">nonaktif</span>}</td>
                    <td className="muted">{p.categoryName}</td>
                    <td className="num">{formatRupiah(p.price)}</td>
                    <td className="num">{p.stock}</td>
                    <td style={{ whiteSpace: 'nowrap' }}>
                      <button type="button" className="link-btn" onClick={() => setEditing(p)} aria-label={`Ubah ${p.name}`}>Ubah</button>
                      {p.active && (
                        <button type="button" className="link-btn" style={{ marginLeft: 'var(--s-4)' }} onClick={() => setToDeactivate(p)} aria-label={`Nonaktifkan ${p.name}`}>Nonaktifkan</button>
                      )}
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
        onConfirm={() => toDeactivate && deactivate.mutate(toDeactivate.id, { onSettled: () => setToDeactivate(null) })}
      >
        {toDeactivate?.name} tidak akan tampil di katalog. Datanya tetap tersimpan dan order lama tidak terpengaruh.
      </ConfirmDialog>
    </>
  );
}
