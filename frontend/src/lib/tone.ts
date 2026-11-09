/**
 * Nada placeholder produk (1..6, netral cokelat dari palet). Dasar nada ditentukan kategori, lalu bergeser
 * sedikit per produk supaya kisi 30 produk tidak monoton tetapi tiap kategori tetap terbaca sebagai satu rumpun.
 */
export function toneFor(categoryId: number, productId: number): number {
  return ((categoryId + (productId % 3)) % 6) + 1;
}
