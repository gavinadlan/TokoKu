import createClient from 'openapi-fetch';
import { adminSession, API_URL, storeSession, type Session } from '../auth/session';
import { ApiError, toApiError } from '../lib/errors';
import type { paths } from './schema';

export { API_URL };

function makeClient(session: Session) {
  const client = createClient<paths>({ baseUrl: API_URL });
  client.use({
    onRequest({ request }) {
      const token = session.getAccess();
      if (token) request.headers.set('Authorization', `Bearer ${token}`);
      return request;
    },
  });
  return client;
}

/** Klien toko (token pembeli) dan klien admin (token admin): tipe sama, sesi berbeda. */
export const api = makeClient(storeSession);
export const adminApi = makeClient(adminSession);

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

function makeCaller(session: Session) {
  /**
   * Menjalankan satu panggilan API: jika 401 pada request yang membawa token, coba refresh sekali lalu ulangi.
   * Mengembalikan data atau melempar ApiError berisi pesan dari backend.
   */
  return async function call<T>(op: () => Promise<Result<T>>): Promise<T> {
    const hadToken = session.getAccess() !== null;
    let res = await safely(op);
    if (res.response.status === 401 && hadToken && session.getRefresh()) {
      if (await session.refresh()) res = await safely(op);
    }
    if (!res.response.ok) {
      throw toApiError(res.response.status, res.error, res.response.headers.get('Retry-After'));
    }
    return res.data as T;
  };
}

export const call = makeCaller(storeSession);
export const callAdmin = makeCaller(adminSession);

/** Mengambil berkas biner (invoice PDF) dengan token, karena endpoint butuh Bearer dan tidak bisa dibuka lewat tautan biasa. */
export async function fetchBlob(path: string, realm: 'store' | 'admin'): Promise<Blob> {
  const session = realm === 'admin' ? adminSession : storeSession;
  const get = async () => {
    try {
      return await fetch(`${API_URL}${path}`, { headers: session.getAccess() ? { Authorization: `Bearer ${session.getAccess()}` } : {} });
    } catch {
      throw new ApiError(0, 'Tidak bisa terhubung ke server. Periksa koneksi Anda lalu coba lagi.');
    }
  };
  let res = await get();
  if (res.status === 401 && session.getRefresh() && (await session.refresh())) res = await get();
  if (!res.ok) {
    const body = await res.json().catch(() => undefined);
    throw toApiError(res.status, body, res.headers.get('Retry-After'));
  }
  return res.blob();
}
