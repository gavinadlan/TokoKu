const rupiah = new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', maximumFractionDigits: 0 });
const dateTime = new Intl.DateTimeFormat('id-ID', { dateStyle: 'long', timeStyle: 'short' });
const dateTimeShort = new Intl.DateTimeFormat('id-ID', { dateStyle: 'medium', timeStyle: 'short' });
const dateOnly = new Intl.DateTimeFormat('id-ID', { dateStyle: 'long' });

/** 85000 -> "Rp 85.000" (locale id-ID; spasi di antara Rp dan angka adalah spasi tak terputus) */
export function formatRupiah(value: number): string {
  return rupiah.format(value);
}

export function formatDateTime(iso: string): string {
  return dateTime.format(new Date(iso));
}

/** "10 Nov 2026, 07.42": untuk linimasa dan tabel yang padat. */
export function formatDateTimeShort(iso: string): string {
  return dateTimeShort.format(new Date(iso));
}

export function formatDate(iso: string): string {
  return dateOnly.format(new Date(iso));
}

/** Nomor produk 1 -> "001" (dipakai di label kemasan dan daftar fakta) */
export function productNumber(id: number): string {
  return String(id).padStart(3, '0');
}
