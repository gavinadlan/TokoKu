# Keputusan yang menambah atau menyimpang dari PRD

## Fase 2
- **Role di klaim `role`, subject = user id.** `GET /api/me` membaca user dari DB, jadi perubahan data tidak basi. Token tidak berisi password/hash.
- **JWT_SECRET kosong**: prod gagal start; dev/test memakai kunci acak sementara per start (tidak ada secret di repo, token dev hangus saat restart). Secret < 32 byte ditolak di semua profil.
- **Register selalu role USER.** Admin hanya lewat bootstrap env (`ADMIN_EMAIL`/`ADMIN_PASSWORD`) atau DB.
- **Endpoint `/api/auth/*` publik dirinci satu per satu** (bukan `/api/auth/**`), karena `change-password` (Fase 6) harus login.
- **401/403 ditulis manual sebagai JSON ProblemDetail** di filter chain, karena exception handler MVC tidak menjangkau filter.
- **`GET /api/admin/products`** ditambahkan (di luar tabel PRD) untuk melihat produk nonaktif; `GET /api/products/{id}` publik tetap 404 untuk produk nonaktif.

## Fase 3
- **Kolom `shipping_name/phone/address` pada `orders`** (di luar PRD): checkout tanpa alamat tidak masuk akal.
- **`order_items.product_name`** (di luar PRD): snapshot nama bersama `unit_price`, supaya riwayat order tidak berubah saat produk diedit.
- **Pengurangan stok: `UPDATE ... SET stock = stock - :q WHERE id = :id AND stock >= :q`**, bukan optimistic locking + retry. Alasan: satu statement atomik di DB, tanpa loop retry, tanpa exception `@Version` untuk kasus yang normal (rebutan stok). Hasil 0 baris = stok kurang → 409 dan seluruh transaksi rollback. Update juga menaikkan `version` agar pembaca optimistic lain ikut terdeteksi. Baris dikunci berurutan menurut id produk untuk mencegah deadlock antar checkout. `CHECK (stock >= 0)` di DB jadi jaring pengaman terakhir.
- **Stok kurang = 409** (bukan 400), karena request valid tetapi bentrok dengan keadaan data. Keranjang kosong = 400.
- **Admin hanya boleh set `PROCESSING`/`SHIPPED`/`COMPLETED`.** `PAID` hanya lewat pembayaran, `CANCELLED` lewat pembatalan/kedaluwarsa (stok harus kembali). Selain itu 409. Status tak dikenal = 400.
- **Order orang lain = 404**, bukan 403.
- `GET /api/admin/orders/{id}` ditambahkan (di luar PRD) untuk halaman detail admin.
- Item keranjang tidak mereservasi stok; validasi stok terjadi saat tambah/ubah dan diulang secara atomik saat checkout.
