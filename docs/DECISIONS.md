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

## Fase 4
- **Provider default `midtrans`** (dev dan prod); test otomatis memaksa `fake` lewat `@SpringBootTest(properties=...)`. Dev tanpa kunci: `APP_PAYMENT_PROVIDER=fake`. `fake` ditolak start di profil prod, dan endpoint `/api/dev/**` hanya ada jika `fake` dan profil bukan prod (dibuktikan `DevEndpointAvailabilityTest`).
- **Env Midtrans yang dipakai:** `MIDTRANS_SERVER_KEY`, `MIDTRANS_CLIENT_KEY`, `MIDTRANS_API_URL` (URL Snap). `MIDTRANS_MERCHANT_ID`, `MIDTRANS_CORE_API_URL`, `MIDTRANS_USE_CORE_API` ada di `.env` tetapi **belum dipakai** (kita memakai Snap, bukan Core API).
- **Satu `provider_ref` per percobaan bayar** (`TKU-<orderId>-<8 hex>`), karena Midtrans hanya menerima `order_id` sekali. Webhook dicocokkan lewat `provider_ref`.
- **Idempotensi `/pay`:** baris order dikunci (`FOR UPDATE`), dan unique index parsial menjamin maksimal satu payment `PENDING` per order.
- **Idempotensi webhook:** baris payment dikunci; jika status payment sudah bukan `PENDING`, notifikasi diabaikan (200). Jadi `expire` yang datang terlambat tidak membatalkan order yang sudah `PAID`.
- **Respons webhook:** signature salah = 403, payment tidak dikenal = 404, nominal beda = 400, sisanya 200. Midtrans mencoba ulang pada non-2xx.
- **`capture` dianggap sukses hanya jika `fraud_status` kosong atau `accept`.** `pending`/`authorize`/`refund` diabaikan.
- **Pembayaran masuk setelah order dibatalkan** (settlement terlambat) hanya dicatat (log warn); butuh refund manual. Risiko terbuka.
- **Nominal harus rupiah bulat** untuk Midtrans (`gross_amount` integer). Harga pecahan ditolak dengan 502.
- **Tidak memakai `callbacks.finish`**; URL frontend belum ada.
- **Belum terverifikasi dari dokumentasi:** batas panjang `order_id` dan nama item (dipatuhi konservatif: 50), serta rumus `gross_amount == sum(item)` (dipatuhi).

## Fase 5
- **Gate JaCoCo** (`verify`): cakupan baris minimal 70% untuk kelas `com/tokoku/**/*Service.class`, satu bundle. Hasil saat ini ±94% (sebagian besar dari test integrasi, yang menjalankan service sungguhan).
- **Tema data seed: kopi dan perlengkapan seduh**, 6 kategori x 5 produk, harga rupiah masuk akal. `DevDataSeeder` hanya aktif di profil `dev`, dan dilewati bila tabel produk sudah berisi.
- **OpenAPI:** skema Bearer global; endpoint publik menimpa dengan `@SecurityRequirements` kosong sehingga tidak bergembok di Swagger. Tag dan ringkasan dalam bahasa Indonesia.
- **CI**: ditambah `permissions: contents: read`; tidak dijalankan di GitHub (belum ada remote). `actionlint` tidak tersedia, file dibaca ulang manual: `mvnw` executable, Docker tersedia di `ubuntu-latest` untuk Testcontainers, test tidak butuh secret (provider `fake`, JWT acak).
