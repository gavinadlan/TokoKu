import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Navigate, useNavigate } from 'react-router';
import { z } from 'zod';
import { adminApi, callAdmin } from '../../api/client';
import { useAdminAuth } from '../../auth/AdminAuthContext';
import { Field } from '../../components/Field';
import { messageOf } from '../../lib/errors';
import { useDocumentTitle } from '../../lib/useDocumentTitle';

const schema = z
  .object({
    currentPassword: z.string().min(1, 'Isi password sementara.'),
    newPassword: z.string().min(8, 'Minimal 8 karakter.').max(72, 'Maksimal 72 karakter.'),
    confirm: z.string(),
  })
  .refine((v) => v.newPassword === v.confirm, { path: ['confirm'], message: 'Konfirmasi tidak sama.' });
type Values = z.infer<typeof schema>;

/** Admin awal wajib mengganti password sementara; server menolak endpoint admin lain sampai selesai. */
export function AdminChangePasswordPage() {
  useDocumentTitle('Ganti password | TokoKu Admin');
  const { user, status, logout } = useAdminAuth();
  const navigate = useNavigate();
  const [serverError, setServerError] = useState<string | null>(null);
  const form = useForm<Values>({ resolver: zodResolver(schema), defaultValues: { currentPassword: '', newPassword: '', confirm: '' } });
  const { errors, isSubmitting } = form.formState;

  if (status === 'anonymous') return <Navigate to="/admin/masuk" replace />;

  return (
    <div className="adm alogin">
      <main className="panel-r" id="konten">
        <form
          noValidate
          onSubmit={form.handleSubmit(async (v) => {
            setServerError(null);
            try {
              await callAdmin(() => adminApi.POST('/api/admin/change-password', { body: { currentPassword: v.currentPassword, newPassword: v.newPassword } }));
              await logout(); // server mencabut semua sesi; masuk ulang dengan password baru
              navigate('/admin/masuk', { replace: true });
            } catch (e) {
              setServerError(messageOf(e));
            }
          })}
        >
          <div>
            <h1>Ganti password</h1>
            <p className="note">{user?.mustChangePassword ? 'Password awal bersifat sementara. Ganti sebelum mengelola toko.' : 'Masukkan password lama dan password baru.'}</p>
          </div>
          {serverError && (
            <div className="alert err" role="alert">
              {serverError}
            </div>
          )}
          <Field label="Password saat ini" error={errors.currentPassword?.message}>
            <input className="input" type="password" autoComplete="current-password" {...form.register('currentPassword')} />
          </Field>
          <Field label="Password baru" error={errors.newPassword?.message}>
            <input className="input" type="password" autoComplete="new-password" {...form.register('newPassword')} />
          </Field>
          <Field label="Ulangi password baru" error={errors.confirm?.message}>
            <input className="input" type="password" autoComplete="new-password" {...form.register('confirm')} />
          </Field>
          <button className="btn" type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Memproses...' : 'Simpan password'}
          </button>
        </form>
      </main>
    </div>
  );
}
