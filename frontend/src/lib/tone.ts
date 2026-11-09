/**
 * Nada placeholder produk (1..6, netral cokelat dari palet). Nada 1-3 keluarga terang, 4-6 keluarga gelap.
 * Kategori menentukan keluarga (ganjil terang, genap gelap) sehingga satu kategori terbaca sebagai satu rumpun;
 * produk bergeser antar tiga tingkat dalam keluarga itu supaya kisi 30 produk tidak monoton.
 */
export function toneFor(categoryId: number, productId: number): number {
  const darkFamily = categoryId % 2 === 0;
  const shade = (Math.floor(categoryId / 2) + productId) % 3;
  return (darkFamily ? 4 : 1) + shade;
}
