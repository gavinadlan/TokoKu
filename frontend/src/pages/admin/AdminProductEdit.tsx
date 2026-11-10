import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { Link, useNavigate, useParams } from 'react-router';
import { z } from 'zod';
import { useAdminProduct, useCategories, useSaveProduct } from '../../api/queries';
import type { Product } from '../../api/types';
import { Field } from '../../components/Field';
import { RupiahInput } from '../../components/RupiahInput';
import { Select } from '../../components/Select';
import { ErrorState, ListSkeleton } from '../../components/States';
import { useToast } from '../../components/Toast';
import { ApiError, messageOf } from '../../lib/errors';
import { productNumber } from '../../lib/format';
import { useDocumentTitle } from '../../lib/useDocumentTitle';
import { AdminPage } from './AdminShell';
import { ProductImagesManager } from './ProductImagesManager';

const schema = z.object({
  name: z.string().trim().min(2, 'Isi nama produk.').max(200, 'Nama terlalu panjang.'),
  categoryId: z.number({ error: 'Pilih kategori.' }).int().positive('Pilih kategori.'),
  description: z.string().max(2000, 'Deskripsi terlalu panjang.'),
  price: z.number({ error: 'Isi harga dalam rupiah.' }).min(0, 'Harga tidak boleh negatif.'),
  stock: z.number({ error: 'Isi stok dalam angka.' }).int('Stok harus bilangan bulat.').min(0, 'Stok tidak boleh negatif.'),
});
type Values = z.infer<typeof schema>;

function ProductForm({ product }: { product: Product | null }) {
  const categories = useCategories();
  const save = useSaveProduct();
  const toast = useToast();
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);
  const form = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: product
      ? { name: product.name, categoryId: product.categoryId, description: product.description ?? '', price: product.price, stock: product.stock }
      : { name: '', categoryId: undefined, description: '', price: undefined, stock: 0 },
  });
  const { errors } = form.formState;

  return (
    <form
      className="panel-box"
      noValidate
      aria-label={product ? 'Data produk' : 'Produk baru'}
      onSubmit={form.handleSubmit(async (v) => {
        setError(null);
        try {
          const saved = await save.mutateAsync({ id: product?.id, body: { name: v.name.trim(), categoryId: v.categoryId, description: v.description.trim() || undefined, price: v.price, stock: v.stock } });
          if (product) toast.notify('Perubahan disimpan.');
          else {
            toast.notify('Produk dibuat. Sekarang tambahkan fotonya.');
            navigate(`/admin/produk/${saved.id}#foto`, { replace: true });
          }
        } catch (e) {
          setError(e instanceof ApiError && e.status === 400 ? 'Periksa kembali isian Anda.' : messageOf(e));
        }
      })}
    >
      <h2>Data produk</h2>
      {error && <div className="alert err" role="alert">{error}</div>}
      <div className="form-2 cols">
        <div className="span">
          <Field label="Nama produk" error={errors.name?.message}>
            <input className="input" {...form.register('name')} />
          </Field>
        </div>
        <Field label="Kategori" error={errors.categoryId?.message}>
          <Controller control={form.control} name="categoryId" render={({ field }) => (
            <Select className="input" value={field.value ? String(field.value) : ''} onChange={(v) => field.onChange(Number(v))} placeholder="Pilih kategori" invalid={Boolean(errors.categoryId)} options={(categories.data ?? []).map((c) => ({ value: String(c.id), label: c.name }))} />
          )} />
        </Field>
        <Field label="Harga" hint="Dalam rupiah, tanpa pecahan." error={errors.price?.message}>
          <Controller control={form.control} name="price" render={({ field }) => (
            <RupiahInput value={field.value} onChange={field.onChange} onBlur={field.onBlur} invalid={Boolean(errors.price)} />
          )} />
        </Field>
        <Field label="Stok" error={errors.stock?.message}>
          <input className="input" type="number" inputMode="numeric" min={0} step={1} {...form.register('stock', { valueAsNumber: true })} />
        </Field>
        <div className="span">
          <Field label="Deskripsi (opsional)" error={errors.description?.message}>
            <textarea className="input" rows={3} {...form.register('description')} />
          </Field>
        </div>
      </div>
      <div className="btnrow">
        <button className="btn sm" type="submit" disabled={save.isPending}>{save.isPending ? 'Menyimpan...' : product ? 'Simpan perubahan' : 'Buat produk'}</button>
        <Link className="btn ghost sm" to="/admin/produk">Kembali ke daftar</Link>
      </div>
    </form>
  );
}

export function AdminProductEditPage() {
  const { id } = useParams();
  const productId = id ? Number(id) : null;
  const product = useAdminProduct(productId);
  useDocumentTitle(productId === null ? 'Produk baru | TokoKu Admin' : product.data ? `${product.data.name} | TokoKu Admin` : 'Produk | TokoKu Admin');

  if (productId === null) {
    return (
      <AdminPage title="Produk baru" sub="Foto bisa ditambahkan setelah produk dibuat.">
        <ProductForm product={null} />
      </AdminPage>
    );
  }
  if (product.isPending) return <AdminPage title="Produk"><ListSkeleton /></AdminPage>;
  if (product.isError) return <AdminPage title="Produk"><ErrorState error={product.error} onRetry={() => product.refetch()} title="Produk belum bisa dimuat" /></AdminPage>;

  return (
    <AdminPage title={product.data.name} sub={`Produk No. ${productNumber(product.data.id)}${product.data.active ? '' : ' · nonaktif'}`} actions={<Link className="btn ghost sm" to="/admin/produk">Kembali ke daftar</Link>}>
      <ProductForm key={product.data.id} product={product.data} />
      <ProductImagesManager product={product.data} />
    </AdminPage>
  );
}
