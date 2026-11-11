export const MAX_IMAGES = 6;
export const MAX_IMAGE_BYTES = 5 * 1024 * 1024;
export const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

/** Validasi sisi klien sebelum unggah (server memvalidasi ulang). null = boleh. */
export function validateImageFile(file: { type: string; size: number; name: string }, currentCount: number): string | null {
  if (currentCount >= MAX_IMAGES) return `Maksimal ${MAX_IMAGES} foto per produk.`;
  if (!ALLOWED_IMAGE_TYPES.includes(file.type)) return 'Format harus jpg, png, atau webp.';
  if (file.size > MAX_IMAGE_BYTES) return `Ukuran ${(file.size / 1024 / 1024).toFixed(1).replace('.', ',')} MB melebihi batas 5 MB.`;
  if (file.size === 0) return 'Berkas kosong.';
  return null;
}

/** Geser satu foto ke kiri (-1) atau kanan (+1); mengembalikan urutan id baru, atau null bila di tepi. */
export function moveImage(ids: number[], id: number, direction: -1 | 1): number[] | null {
  const from = ids.indexOf(id);
  const to = from + direction;
  if (from < 0 || to < 0 || to >= ids.length) return null;
  const next = [...ids];
  next.splice(from, 1);
  next.splice(to, 0, id);
  return next;
}
