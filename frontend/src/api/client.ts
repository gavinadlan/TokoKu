import createClient from 'openapi-fetch';
import { refreshSession, session } from '../auth/session';
import { ApiError, toApiError } from '../lib/errors';
import type { paths } from './schema';

export const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';

export const api = createClient<paths>({ baseUrl: API_URL });

api.use({
  onRequest({ request }) {
    const token = session.getAccess();
    if (token) request.headers.set('Authorization', `Bearer ${token}`);
    return request;
  },
});

interface Result<T> {
  data?: T;
  error?: unknown;
  response: Response;
}

async function safely<T>(op: () => Promise<Result<T>>): Promise<Result<T>> {
  try {
    return await op();
  } catch {
    throw new ApiError(0, 'Tidak bisa terhubung ke server. Periksa koneksi Anda lalu coba lagi.');
  }
}

/**
 * Menjalankan satu panggilan API: jika 401 pada request yang membawa token, coba refresh sekali lalu ulangi.
 * Mengembalikan data atau melempar ApiError berisi pesan dari backend.
 */
export async function call<T>(op: () => Promise<Result<T>>): Promise<T> {
  const hadToken = session.getAccess() !== null;
  let res = await safely(op);
  if (res.response.status === 401 && hadToken && session.getRefresh()) {
    if (await refreshSession()) res = await safely(op);
  }
  if (!res.response.ok) {
    throw toApiError(res.response.status, res.error, res.response.headers.get('Retry-After'));
  }
  return res.data as T;
}
