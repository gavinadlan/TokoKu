import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Navigate, useLocation, useNavigate } from 'react-router';
import { z } from 'zod';
import { useAdminAuth } from '../../auth/AdminAuthContext';
import { Field } from '../../components/Field';
import { ApiError, messageOf } from '../../lib/errors';
import { useDocumentTitle } from '../../lib/useDocumentTitle';

const schema = z.object({ email: z.string().min(1, 'Isi email admin.').email('Format email belum benar.'), password: z.string().min(1, 'Isi password.') });
type Values = z.infer<typeof schema>;

export function AdminLoginPage() {
  useDocumentTitle('Masuk pengelola | TokoKu Admin');
  const { login, status } = useAdminAuth();
  const navigate = useNavigate();
  const from = (useLocation().state as { from?: string } | null)?.from ?? '/admin/produk';
  const [serverError, setServerError] = useState<string | null>(null);
  const form = useForm<Values>({ resolver: zodResolver(schema), defaultValues: { email: '', password: '' } });
  const { errors, isSubmitting } = form.formState;

  if (status === 'authenticated') return <Navigate to={from} replace />;

  return (
    <div className="adm alogin">
      <section className="panel-l">
        <div className="brand">
          <b>TokoKu</b>
          <i>Admin</i>
        </div>
        <p>Khusus pengelola toko. Pembeli masuk lewat halaman toko.</p>
        <div className="frame" aria-hidden="true">
          <span>Panel pengelola</span>
        </div>
      </section>
      <main className="panel-r" id="konten">
        <form
          noValidate
          onSubmit={form.handleSubmit(async (v) => {
            setServerError(null);
            try {
              const user = await login(v.email, v.password);
              navigate(user.mustChangePassword ? '/admin/ganti-password' : from, { replace: true });
            } catch (e) {
              setServerError(e instanceof ApiError && e.status === 429 && e.retryAfter ? `Terlalu banyak percobaan. Coba lagi dalam ${e.retryAfter} detik.` : messageOf(e));
            }
          })}
        >
          <div>
            <h1>Masuk pengelola</h1>
            <p className="note">Gunakan akun admin. Akun pembeli tidak bisa masuk di sini.</p>
          </div>
          {serverError && (
            <div className="alert err" role="alert">
              {serverError}
            </div>
          )}
          <Field label="Email" error={errors.email?.message}>
            <input className="input" type="email" autoComplete="username" {...form.register('email')} />
          </Field>
          <Field label="Password" error={errors.password?.message}>
            <input className="input" type="password" autoComplete="current-password" {...form.register('password')} />
          </Field>
          <button className="btn" type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Memproses...' : 'Masuk'}
          </button>
          <p className="note">Sesi admin berlaku 15 menit dan diperbarui otomatis selama tab terbuka.</p>
        </form>
      </main>
    </div>
  );
}
