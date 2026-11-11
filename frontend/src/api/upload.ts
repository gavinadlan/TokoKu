import { API_URL } from '../auth/session';
import type { UploadSignature } from './types';

export interface UploadResult {
  public_id: string;
  version: number;
  signature: string;
  width?: number;
  height?: number;
  format: string;
  bytes: number;
}

export class UploadError extends Error {}

/**
 * Unggah langsung dari browser ke penyimpanan gambar (Cloudinary, atau server dev untuk provider fake) dengan
 * tanda tangan dari server. XHR dipakai karena fetch belum punya progres unggah.
 */
export function uploadImage(file: File, sig: UploadSignature, onProgress: (percent: number) => void, signal?: AbortSignal): Promise<UploadResult> {
  return new Promise((resolve, reject) => {
    const url = sig.uploadUrl.startsWith('http') ? sig.uploadUrl : `${API_URL}${sig.uploadUrl}`;
    const form = new FormData();
    form.set('file', file);
    form.set('api_key', sig.apiKey);
    form.set('timestamp', String(sig.timestamp));
    form.set('signature', sig.signature);
    form.set('folder', sig.folder);
    form.set('allowed_formats', sig.allowedFormats);

    const xhr = new XMLHttpRequest();
    xhr.open('POST', url);
    xhr.upload.onprogress = (e) => e.lengthComputable && onProgress(Math.round((e.loaded / e.total) * 100));
    xhr.onerror = () => reject(new UploadError('Tidak bisa terhubung ke penyimpanan gambar. Periksa koneksi lalu coba lagi.'));
    xhr.onabort = () => reject(new UploadError('Unggahan dibatalkan.'));
    xhr.onload = () => {
      let body: { error?: { message?: string } } & Partial<UploadResult> = {};
      try {
        body = JSON.parse(xhr.responseText);
      } catch {
        /* biarkan kosong */
      }
      if (xhr.status >= 200 && xhr.status < 300 && body.public_id) resolve(body as UploadResult);
      else reject(new UploadError(body.error?.message ? `Penyimpanan menolak foto: ${body.error.message}` : 'Foto gagal diunggah. Coba lagi.'));
    };
    signal?.addEventListener('abort', () => xhr.abort());
    xhr.send(form);
  });
}
