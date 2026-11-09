/** Error dari backend (ProblemDetail) atau jaringan. */
export class ApiError extends Error {
  readonly status: number;
  readonly fieldErrors: Record<string, string>;
  readonly retryAfter: number | null;

  constructor(status: number, message: string, fieldErrors: Record<string, string> = {}, retryAfter: number | null = null) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.fieldErrors = fieldErrors;
    this.retryAfter = retryAfter;
  }
}

export function toApiError(status: number, body: unknown, retryAfterHeader?: string | null): ApiError {
  const b = (typeof body === 'object' && body !== null ? body : {}) as { detail?: unknown; errors?: unknown };
  const detail = typeof b.detail === 'string' ? b.detail : '';
  const errors = typeof b.errors === 'object' && b.errors !== null ? (b.errors as Record<string, string>) : {};
  const retry = retryAfterHeader ? Number.parseInt(retryAfterHeader, 10) : NaN;
  return new ApiError(status, localizeDetail(detail, status), localizeFields(errors), Number.isFinite(retry) ? retry : null);
}

const KNOWN: [RegExp, (m: RegExpMatchArray) => string][] = [
  [/^Insufficient stock for (.+) \(available (\d+)\)$/, (m) => `Stok ${m[1]} tidak cukup (tersedia ${m[2]}).`],
  [/^Insufficient stock or unavailable product: (.+)$/, (m) => `Stok ${m[1]} tidak cukup atau produk sudah tidak dijual. Periksa keranjang Anda.`],
  [/^Cart is empty$/, () => 'Keranjang masih kosong.'],
  [/^Email already registered$/, () => 'Email ini sudah terdaftar. Coba masuk.'],
  [/^Invalid email or password$/, () => 'Email atau password salah.'],
  [/^Current password is incorrect$/, () => 'Password saat ini salah.'],
  [/^Category still has products$/, () => 'Kategori masih punya produk. Pindahkan atau nonaktifkan produknya dulu.'],
  [/^Data violates a constraint/, () => 'Nama sudah dipakai. Gunakan nama lain.'],
  [/^Only PENDING_PAYMENT orders can be cancelled/, () => 'Pesanan ini sudah tidak bisa dibatalkan.'],
  [/^Order is .*only PENDING_PAYMENT can be paid/, () => 'Pesanan ini tidak bisa dibayar lagi. Periksa statusnya di riwayat order.'],
  [/^Cannot change order status/, () => 'Perubahan status tidak diizinkan untuk pesanan ini.'],
  [/^Product is no longer available$/, () => 'Produk sudah tidak tersedia.'],
  [/^Too many login attempts/, () => 'Terlalu banyak percobaan masuk. Tunggu sebentar lalu coba lagi.'],
  [/^Data was modified by someone else/, () => 'Data baru saja diubah orang lain. Muat ulang lalu coba lagi.'],
];

/** Pesan backend berbahasa Inggris diterjemahkan; yang tidak dikenal memakai pesan standar per status. */
export function localizeDetail(detail: string, status: number): string {
  for (const [re, fn] of KNOWN) {
    const m = detail.match(re);
    if (m) return fn(m);
  }
  return defaultMessage(status);
}

function localizeFields(fields: Record<string, string>): Record<string, string> {
  return fields; // pesan validasi per kolom ditampilkan oleh Zod di klien; ini cadangan dari server
}

export function defaultMessage(status: number): string {
  if (status === 0) return 'Tidak bisa terhubung ke server. Periksa koneksi Anda lalu coba lagi.';
  if (status === 401) return 'Sesi Anda berakhir. Silakan masuk lagi.';
  if (status === 403) return 'Anda tidak punya izin untuk tindakan ini.';
  if (status === 404) return 'Data yang dicari tidak ditemukan.';
  if (status === 429) return 'Terlalu banyak percobaan. Coba lagi sebentar lagi.';
  if (status >= 500) return 'Server sedang bermasalah. Coba lagi beberapa saat lagi.';
  return 'Permintaan tidak bisa diproses. Periksa isian Anda lalu coba lagi.';
}

/** Pesan yang aman ditampilkan ke pengguna untuk error apa pun. */
export function messageOf(error: unknown): string {
  if (error instanceof ApiError) return error.message;
  return 'Terjadi kesalahan tak terduga. Coba lagi.';
}
