import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { z } from 'zod';
import { useAdminStoreSettings, useSaveStoreSettings } from '../../api/queries';
import type { StoreSettings } from '../../api/types';
import { Field } from '../../components/Field';
import { RupiahInput } from '../../components/RupiahInput';
import { ErrorState, ListSkeleton } from '../../components/States';
import { useToast } from '../../components/Toast';
import { messageOf } from '../../lib/errors';
import { useDocumentTitle } from '../../lib/useDocumentTitle';
import { AdminPage } from './AdminShell';

const schema = z.object({
  name: z.string().trim().min(1, 'Isi nama toko.').max(100),
  address: z.string().trim().min(5, 'Isi alamat toko.').max(300),
  phone: z.string().trim().min(5, 'Isi nomor telepon toko.').max(40),
  email: z.string().trim().max(150).refine((v) => v === '' || /^\S+@\S+\.\S+$/.test(v), 'Format email belum benar.'),
  openingHours: z.string().max(200),
  pickupInstructions: z.string().max(500),
  deliveryEnabled: z.boolean(),
  pickupEnabled: z.boolean(),
  flatShippingFee: z.number({ error: 'Isi biaya kirim (boleh 0).' }).min(0),
  freeShippingThreshold: z.number().min(0).optional(),
  invoiceFooter: z.string().max(500),
  taxId: z.string().max(60),
  taxNote: z.string().max(200),
});
type Values = z.infer<typeof schema>;

function StoreForm({ s }: { s: StoreSettings }) {
  const save = useSaveStoreSettings();
  const toast = useToast();
  const [error, setError] = useState<string | null>(null);
  const form = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: s.name, address: s.address, phone: s.phone, email: s.email, openingHours: s.openingHours, pickupInstructions: s.pickupInstructions,
      deliveryEnabled: s.deliveryEnabled, pickupEnabled: s.pickupEnabled, flatShippingFee: s.flatShippingFee,
      freeShippingThreshold: s.freeShippingThreshold ?? undefined, invoiceFooter: s.invoiceFooter, taxId: s.taxId ?? '', taxNote: s.taxNote ?? '',
    },
  });
  const { errors } = form.formState;

  return (
    <form
      className="stack-form"
      noValidate
      onSubmit={form.handleSubmit(async (v) => {
        setError(null);
        try {
          await save.mutateAsync({ ...v, freeShippingThreshold: v.freeShippingThreshold ?? undefined, taxId: v.taxId.trim() || undefined, taxNote: v.taxNote.trim() || undefined });
          toast.notify('Pengaturan toko disimpan.');
        } catch (e) {
          setError(messageOf(e));
        }
      })}
    >
      {error && <div className="alert err" role="alert">{error}</div>}

      <section className="panel-box">
        <h2>Identitas toko</h2>
        <p className="muted small-note">Dicetak di invoice dan ditampilkan sebagai info pengambilan. Nama, alamat, dan telepon wajib sebelum invoice bisa diterbitkan.</p>
        <div className="form-2 cols">
          <Field label="Nama toko" error={errors.name?.message}><input className="input" {...form.register('name')} /></Field>
          <Field label="Telepon" error={errors.phone?.message}><input className="input" type="tel" {...form.register('phone')} /></Field>
          <div className="span"><Field label="Alamat" error={errors.address?.message}><input className="input" {...form.register('address')} /></Field></div>
          <Field label="Email (opsional)" error={errors.email?.message}><input className="input" type="email" {...form.register('email')} /></Field>
          <Field label="NPWP atau NIB (opsional)" hint="Dicetak di invoice hanya jika diisi."><input className="input" {...form.register('taxId')} /></Field>
        </div>
      </section>

      <section className="panel-box">
        <h2>Pengambilan dan pengiriman</h2>
        <div className="form-2 cols">
          <label className="check"><input type="checkbox" {...form.register('deliveryEnabled')} /> Terima pesanan dikirim ke alamat</label>
          <label className="check"><input type="checkbox" {...form.register('pickupEnabled')} /> Terima pesanan ambil di toko</label>
          <Field label="Biaya kirim flat" hint="Isi 0 bila tidak memungut ongkir." error={errors.flatShippingFee?.message}>
            <Controller control={form.control} name="flatShippingFee" render={({ field }) => <RupiahInput value={field.value} onChange={field.onChange} onBlur={field.onBlur} invalid={Boolean(errors.flatShippingFee)} />} />
          </Field>
          <Field label="Gratis ongkir mulai belanja (opsional)" hint="Kosongkan bila tidak ada gratis ongkir.">
            <Controller control={form.control} name="freeShippingThreshold" render={({ field }) => <RupiahInput value={field.value} onChange={field.onChange} onBlur={field.onBlur} />} />
          </Field>
          <div className="span"><Field label="Jam buka" error={errors.openingHours?.message}><input className="input" placeholder="mis. Senin sampai Sabtu, 09.00 sampai 17.00" {...form.register('openingHours')} /></Field></div>
          <div className="span"><Field label="Petunjuk pengambilan" error={errors.pickupInstructions?.message}><textarea className="input" rows={2} {...form.register('pickupInstructions')} /></Field></div>
        </div>
      </section>

      <section className="panel-box">
        <h2>Invoice</h2>
        <div className="form-2">
          <Field label="Catatan kaki" hint="Dicetak di bagian bawah setiap invoice."><textarea className="input" rows={2} {...form.register('invoiceFooter')} /></Field>
          <Field label="Catatan pajak (opsional)" hint="Teks bebas di bawah total, mis. keterangan pajak yang berlaku untuk toko Anda. Aplikasi tidak menghitung pajak."><input className="input" {...form.register('taxNote')} /></Field>
        </div>
      </section>

      <div className="btnrow"><button className="btn sm" type="submit" disabled={save.isPending}>{save.isPending ? 'Menyimpan...' : 'Simpan pengaturan'}</button></div>
    </form>
  );
}

export function AdminStorePage() {
  useDocumentTitle('Pengaturan toko | TokoKu Admin');
  const store = useAdminStoreSettings();
  return (
    <AdminPage title="Pengaturan toko" sub={store.data ? (store.data.configured ? 'Identitas toko lengkap' : 'Identitas toko belum lengkap') : undefined}>
      {store.isPending ? <ListSkeleton /> : store.isError ? <ErrorState error={store.error} onRetry={() => store.refetch()} /> : <StoreForm s={store.data} />}
    </AdminPage>
  );
}
