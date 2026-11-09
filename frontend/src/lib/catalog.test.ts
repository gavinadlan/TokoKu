import { apiSort, parseCatalogParams, toSearchParams } from './catalog';

describe('parseCatalogParams', () => {
  it('memakai default untuk URL kosong', () => {
    expect(parseCatalogParams(new URLSearchParams())).toEqual({ q: '', category: null, sort: 'terbaru', page: 1 });
  });
  it('membaca semua parameter', () => {
    const p = parseCatalogParams(new URLSearchParams('q=gayo&kategori=2&urut=termurah&halaman=3'));
    expect(p).toEqual({ q: 'gayo', category: 2, sort: 'termurah', page: 3 });
  });
  it('mengabaikan nilai tidak valid', () => {
    const p = parseCatalogParams(new URLSearchParams('kategori=abc&urut=xx&halaman=-4'));
    expect(p).toEqual({ q: '', category: null, sort: 'terbaru', page: 1 });
  });
});

describe('toSearchParams', () => {
  it('hanya menulis nilai non-default', () => {
    expect(toSearchParams({ q: '', category: null, sort: 'terbaru', page: 1 }).toString()).toBe('');
    expect(toSearchParams({ q: 'v60', category: 3, sort: 'nama', page: 2 }).toString()).toBe('q=v60&kategori=3&urut=nama&halaman=2');
  });
  it('bolak-balik konsisten', () => {
    const p = { q: 'kopi susu', category: 1, sort: 'termahal' as const, page: 4 };
    expect(parseCatalogParams(toSearchParams(p))).toEqual(p);
  });
});

describe('apiSort', () => {
  it('memetakan kunci URL ke parameter sort backend', () => {
    expect(apiSort('termurah')).toBe('price,asc');
    expect(apiSort('terbaru')).toBe('id,desc');
  });
});
