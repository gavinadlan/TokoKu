import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, Navigate, useLocation, useNavigate } from 'react-router';
import { z } from 'zod';
import { useAuth } from '../auth/AuthContext';
import { Field } from '../components/Field';
import { ApiError, messageOf } from '../lib/errors';
import { useDocumentTitle } from '../lib/useDocumentTitle';

const loginSchema = z.object({
  email: z.string().min(1, 'Isi email Anda.').email('Format email belum benar.'),
  password: z.string().min(1, 'Isi password Anda.'),
});
type LoginValues = z.infer<typeof loginSchema>;

const registerSchema = z.object({
  name: z.string().trim().min(2, 'Isi nama Anda.').max(100, 'Nama terlalu panjang.'),
  email: z.string().min(1, 'Isi email Anda.').email('Format email belum benar.'),
  password: z.string().min(8, 'Password minimal 8 karakter.').max(72, 'Password maksimal 72 karakter.'),
});
type RegisterValues = z.infer<typeof registerSchema>;

function useReturnTo() {
  const state = useLocation().state as { from?: string; notice?: string } | null;
  return { from: state?.from ?? '/', notice: state?.notice };
}

function authErrorMessage(e: unknown): string {
  if (e instanceof ApiError && e.status === 429) {
    return e.retryAfter ? `Terlalu banyak percobaan. Coba lagi dalam ${e.retryAfter} detik.` : e.message;
  }
  return messageOf(e);
}

export function LoginPage() {
  useDocumentTitle('Masuk');
  const { login, status } = useAuth();
  const navigate = useNavigate();
  const { from, notice } = useReturnTo();
  const [serverError, setServerError] = useState<string | null>(null);
  const form = useForm<LoginValues>({ resolver: zodResolver(loginSchema), defaultValues: { email: '', password: '' } });
  const { errors, isSubmitting } = form.formState;

  if (status === 'authenticated') return <Navigate to={from} replace />;

  return (
    <div className="wrap narrow page-pad">
      <h1 className="page-title" style={{ marginBottom: 'var(--s-5)' }}>
        Masuk
      </h1>
      {notice && <div className="alert warn" style={{ marginBottom: 'var(--s-5)' }}>{notice}</div>}
      <form
        className="form"
        noValidate
        onSubmit={form.handleSubmit(async (v) => {
          setServerError(null);
          try {
            await login(v.email, v.password);
            navigate(from, { replace: true });
          } catch (e) {
            setServerError(authErrorMessage(e));
          }
        })}
      >
        {serverError && (
          <div className="alert err" role="alert">
            {serverError}
          </div>
        )}
        <Field label="Email" error={errors.email?.message}>
          <input className="input" type="email" autoComplete="email" inputMode="email" {...form.register('email')} />
        </Field>
        <Field label="Password" error={errors.password?.message}>
          <input className="input" type="password" autoComplete="current-password" {...form.register('password')} />
        </Field>
        <button className="btn" type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Memproses...' : 'Masuk'}
        </button>
      </form>
      <p className="muted" style={{ marginTop: 'var(--s-5)' }}>
        Belum punya akun?{' '}
        <Link to="/daftar" state={{ from, notice: undefined }}>
          Daftar di sini
        </Link>
      </p>
    </div>
  );
}

export function RegisterPage() {
  useDocumentTitle('Daftar');
  const { register: signUp, status } = useAuth();
  const navigate = useNavigate();
  const { from } = useReturnTo();
  const [serverError, setServerError] = useState<string | null>(null);
  const form = useForm<RegisterValues>({ resolver: zodResolver(registerSchema), defaultValues: { name: '', email: '', password: '' } });
  const { errors, isSubmitting } = form.formState;
  const [show, setShow] = useState(false);

  if (status === 'authenticated') return <Navigate to={from} replace />;

  return (
    <div className="wrap narrow page-pad">
      <h1 className="page-title" style={{ marginBottom: 'var(--s-5)' }}>
        Buat akun
      </h1>
      <form
        className="form"
        noValidate
        onSubmit={form.handleSubmit(async (v) => {
          setServerError(null);
          try {
            await signUp({ name: v.name.trim(), email: v.email, password: v.password });
            navigate(from, { replace: true });
          } catch (e) {
            setServerError(authErrorMessage(e));
          }
        })}
      >
        {serverError && (
          <div className="alert err" role="alert">
            {serverError} {serverError.includes('terdaftar') && <Link to="/masuk">Masuk sekarang</Link>}
          </div>
        )}
        <Field label="Nama" error={errors.name?.message}>
          <input className="input" autoComplete="name" {...form.register('name')} />
        </Field>
        <Field label="Email" error={errors.email?.message}>
          <input className="input" type="email" autoComplete="email" inputMode="email" {...form.register('email')} />
        </Field>
        <Field
          label="Password"
          hint="Minimal 8 karakter."
          error={errors.password?.message}
          extra={
            <button type="button" className="link-btn" style={{ justifySelf: 'start', minHeight: 32 }} onClick={() => setShow((s) => !s)} aria-pressed={show}>
              {show ? 'Sembunyikan password' : 'Tampilkan password'}
            </button>
          }
        >
          <input className="input" type={show ? 'text' : 'password'} autoComplete="new-password" {...form.register('password')} />
        </Field>
        <button className="btn" type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Memproses...' : 'Daftar dan masuk'}
        </button>
      </form>
      <p className="muted" style={{ marginTop: 'var(--s-5)' }}>
        Sudah punya akun? <Link to="/masuk" state={{ from }}>Masuk</Link>
      </p>
    </div>
  );
}
