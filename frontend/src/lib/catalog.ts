/** Parameter katalog yang tersimpan di URL, supaya bisa dibagikan dan tombol back berfungsi. */
export type SortKey = 'terbaru' | 'termurah' | 'termahal' | 'nama';

export const SORT_OPTIONS: { key: SortKey; label: string; api: string }[] = [
  { key: 'terbaru', label: 'Terbaru', api: 'id,desc' },
  { key: 'termurah', label: 'Harga terendah', api: 'price,asc' },
  { key: 'termahal', label: 'Harga tertinggi', api: 'price,desc' },
  { key: 'nama', label: 'Nama A ke Z', api: 'name,asc' },
];

export interface CatalogParams {
  q: string;
  category: number | null;
  sort: SortKey;
  page: number; // 1-based di URL
}

export const PAGE_SIZE = 12;

export function parseCatalogParams(sp: URLSearchParams): CatalogParams {
  const sort = SORT_OPTIONS.find((o) => o.key === sp.get('urut'))?.key ?? 'terbaru';
  const category = Number.parseInt(sp.get('kategori') ?? '', 10);
  const page = Number.parseInt(sp.get('halaman') ?? '', 10);
  return {
    q: (sp.get('q') ?? '').trim(),
    category: Number.isFinite(category) && category > 0 ? category : null,
    sort,
    page: Number.isFinite(page) && page > 0 ? page : 1,
  };
}

/** Hanya nilai non-default yang ditulis ke URL, sehingga tautan tetap pendek. */
export function toSearchParams(p: CatalogParams): URLSearchParams {
  const sp = new URLSearchParams();
  if (p.q) sp.set('q', p.q);
  if (p.category) sp.set('kategori', String(p.category));
  if (p.sort !== 'terbaru') sp.set('urut', p.sort);
  if (p.page > 1) sp.set('halaman', String(p.page));
  return sp;
}

export function apiSort(sort: SortKey): string {
  return SORT_OPTIONS.find((o) => o.key === sort)?.api ?? 'id,desc';
}
