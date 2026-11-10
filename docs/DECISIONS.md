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

## Fase 6
- **Urutan kunci baris: order dulu, lalu payment** di semua jalur (pay, webhook, cancel, expire) supaya tidak ada deadlock. Webhook membaca payment tanpa kunci untuk mendapat `order_id`, mengunci order, baru mengunci payment.
- **Pembatalan/kedaluwarsa memakai satu metode `OrderService.cancelAndRestock`**: transisi → `CANCELLED`, menutup payment `PENDING` (`EXPIRED`), mengembalikan stok. Mengunci baris order (`FOR UPDATE`) supaya scheduler, pembacaan malas, pembatalan pemilik, dan webhook tidak mengembalikan stok dua kali (dibuktikan test konkuren cancel vs expiry).
- **Kedaluwarsa malas** (di `GET /api/orders`, `GET /api/orders/{id}`, `POST /pay`, `POST /api/orders`) selain `@Scheduled` tiap 5 menit, karena Render gratis tidur saat idle. `GET /api/admin/orders*` tidak memicunya (hanya baca).
- **Batas bayar Midtrans = sisa umur order** (`expiry-minutes` dikurangi usia order), agar pembayaran tidak bisa masuk setelah order kedaluwarsa.
- **Order dibatalkan tidak membatalkan transaksi di sisi Midtrans** (tidak memanggil API cancel). Pembayaran yang tetap masuk sesudahnya hanya dicatat (log warn) dan butuh refund manual. Risiko terbuka.
- **Package `order` memakai `PaymentRepository`** (menutup payment menggantung), sedangkan `payment` memakai `OrderService`: siklus tingkat paket, bukan siklus bean. Diterima demi kesederhanaan; alternatif (event/antarmuka) dinilai berlebihan.
- **Refresh token opaque** (32 byte acak, base64url), di DB hanya SHA-256. Rotasi dengan kunci baris; pemakaian ulang token yang sudah dicabut mencabut seluruh sesi pengguna (deteksi pencurian). `change-password` mencabut semua refresh token; access token lama tetap valid sampai habis (maks 30 menit), tidak ada daftar tolak access token.
- **`/api/auth/logout` publik** (menerima refresh token), idempoten (204 walau token tak dikenal).
- **Hapus kategori ditolak 409 selama ada produk** (termasuk nonaktif, karena baris produk tetap ada dan FK menahannya).
- **`image_url`**: hanya URL `http(s)`, maks 500 karakter; skema lain (mis. `javascript:`) ditolak 400. Upload file belum ada.
- **Rate limit login**: Bucket4j in-memory, 5/menit per email dan 30/menit per IP, dibatasi 10.000 kunci (LRU). Tidak dibagi antar instance dan hilang saat restart (batas yang diterima). IP asli di prod lewat `server.forward-headers-strategy=framework`. Test memakai batas longgar kecuali test 429 khusus.
- **Butir 7 (cache Redis) DILEWATI.** Alasan: perlu serializer cache untuk `Page` di Spring Data Redis 4 + Jackson 3 (API belum pasti), invalidasi tiap stok berubah, Testcontainers Redis, dan fail-open; manfaatnya kecil untuk beban demo. `redis` tidak dikembalikan ke `docker-compose.yml`.

## Frontend
- **Backend disesuaikan agar klien bertipe akurat:** field respons ditandai `required` lewat `OpenApiCustomizer` (record Java tidak menandainya), parameter `Pageable` memakai `@ParameterObject` (page, size, sort datar), dan `CartItemResponse` mendapat `categoryId` (nada visual thumbnail konsisten dengan katalog). Tidak ada perubahan perilaku API.
- **Provider pembayaran fake di dev:** `scripts/dev-backend.sh` memaksa `APP_PAYMENT_PROVIDER=fake` lewat env proses; `.env` tidak diubah. Halaman `/dev/bayar/:ref` hanya ada di dev server.
- **Batas login dilonggarkan hanya di `scripts/dev-backend.sh`** (1000/menit) karena e2e masuk puluhan kali dari satu IP; default backend tetap 5/menit per email dan 30/menit per IP. Ini sempat membuat e2e tidak stabil sebelum dilonggarkan.
- **Token:** access di memori, refresh di `localStorage` (alasan dan mitigasi XSS di `frontend/README.md`). Alternatif `HttpOnly` cookie butuh perubahan backend; tidak dikerjakan.
- **Keranjang halaman, bukan drawer; redirect penuh ke Midtrans, bukan Snap.js** (disetujui di brief).
- **Pesan error backend diterjemahkan di klien** (pola teks Inggris yang dikenal), karena backend mengembalikan detail berbahasa Inggris. Pesan yang tidak dikenal memakai pesan standar per status.
- **Nada placeholder per kategori:** keluarga terang (nada 1-3) untuk kategori ganjil, gelap (4-6) untuk genap; produk bergeser antar tiga tingkat dalam keluarga itu. Versi pertama (cincin 6 nada) membuat kisi loncat gelap-terang acak.
- **TypeScript 5.9**, bukan rilis terbaru: `openapi-typescript@7` mensyaratkan `typescript@^5`.
- **E2E tidak dijalankan di CI** (butuh backend + Docker + database); CI frontend menjalankan lint, typecheck, test, build.
- Nama produk, harga, dan deskripsi pada mock desain berasal dari data seed; "No. 001" adalah id database.

## Putaran 2: Backend (Tahap B)

### B1 Pemisahan login admin dan pembeli
- **Realm di token:** klaim `realm` = `store` atau `admin`, diubah menjadi authority `REALM_store`/`REALM_admin`. Aturan akses memakai authority itu: `/api/admin/**` (kecuali login/refresh/logout admin) dan tulis produk/kategori butuh `ROLE_ADMIN` **dan** `REALM_admin`; `/api/me`, `/api/cart/**`, `/api/orders/**` (termasuk invoice dan pay), dan `change-password` butuh `REALM_store`. Token lintas realm = 403 (sudah terautentikasi tetapi tidak berhak). Token lama tanpa klaim realm tidak lagi berlaku (maks. 30 menit).
- **`POST /api/auth/login` hanya USER**, `POST /api/admin/auth/login` hanya ADMIN. Akun salah realm mendapat pesan yang **sama** dengan password salah (401), jadi endpoint tidak membocorkan peran; pengecekan password dilakukan sebelum pengecekan peran.
- **Umur:** access token admin 15 menit (`app.jwt.admin-expiration-minutes`), refresh admin 12 jam (`app.jwt.admin-refresh-hours`, toko tetap 14 hari). Kolom `refresh_tokens.realm`; refresh lintas realm ditolak 401 **tanpa** mencabut token aslinya (bukan bukti pencurian); logout lintas realm tidak mencabut.
- **Rate limit admin terpisah** (3/menit per email, 10/menit per IP; `app.ratelimit.admin-login.*`) dengan instans pembatas sendiri.
- **Log:** login admin berhasil (userId, IP) dan gagal/terbatas (email disamarkan `a***@domain`, IP). Tanpa password, token, atau email penuh.
- Admin tetap hanya lewat bootstrap env atau DB; register publik hanya membuat USER.

### B2 Pengambilan dan ongkir
- **`store_settings` singleton** (id = 1, dibuat di migrasi dengan nama "TokoKu" dan identitas kosong). **Terkonfigurasi** = nama, alamat, dan telepon terisi. Data contoh hanya di-seed di profil dev (`StoreSettingsDevSeeder`, idempoten, tidak menimpa isian admin). Di produksi sebelum diisi: invoice 409 ("Store settings are incomplete..."), ambil di toko tidak tersedia (409), `GET /api/store` mengembalikan `configured: false` (frontend menampilkan banner "Lengkapi pengaturan toko" untuk admin).
- **Pajak:** TIDAK dihitung. Ada `tax_id` (NPWP/NIB, dicetak jika diisi) dan `tax_note` (teks bebas di bawah total, mis. "Harga sudah termasuk pajak"). Alasan: aturan pajak tidak boleh dikarang; total order dan nominal Midtrans tetap = subtotal + ongkir.
- **Ongkir:** antarmuka `ShippingCalculator`; implementasi `FlatRateShippingCalculator` (tarif flat; gratis bila subtotal **>=** ambang; ambil di toko selalu 0). Server menghitung sendiri (field ongkir dari klien diabaikan, ada test). Zona atau API kurir bisa menyusul lewat antarmuka yang sama. Default migrasi: ongkir 0 dan tanpa ambang; nilai dev: Rp 15.000 dan ambang Rp 500.000.
- **Midtrans:** ongkir menjadi baris `item_details` sendiri (`SHIPPING`) bila > 0, sehingga jumlah item = `gross_amount`. Cek nominal webhook memakai total baru (sudah termasuk ongkir).
- **State machine per jalur:** DELIVERY `PAID → PROCESSING → SHIPPED → COMPLETED`; PICKUP `PAID → PROCESSING → READY_FOR_PICKUP → COMPLETED`. Status baru `READY_FOR_PICKUP`. Jalur salah = 409; isian kurang (kurir/resi, kode ambil) = 400. Pesan 409 didahulukan atas 400.
- **Kode ambil:** 6 karakter dari `ABCDEFGHJKMNPQRSTUVWXYZ23456789` (tanpa 0/O, 1/I/L), dibuat saat order menjadi PAID, indeks unik. Pencocokan **tidak peka huruf besar-kecil** dan mengabaikan spasi/tanda hubung. Kode hanya muncul di respons order (pemilik dan admin).
- `shipping_address` boleh null (constraint: wajib untuk DELIVERY). Alamat DELIVERY minimal 10 karakter (400). Checkout tanpa `fulfillmentType` = DELIVERY (kompatibel dengan klien lama). Metode bisa dimatikan per toko (`delivery_enabled`, `pickup_enabled`).

### B3 Foto produk
- **Cloudinary, unggah langsung dari browser** dengan tanda tangan server (`POST /api/admin/uploads/sign`); file tidak melewati server Render. Algoritma tanda tangan unggah (urut abjad, `&`, tambah secret, SHA-1) **terverifikasi dari dokumentasi resmi**, lengkap dengan contoh angka resminya sebagai test. **Belum terverifikasi dari dokumentasi:** rumus signature *respons* unggah (halaman dokumentasi yang bisa dibaca tidak memuatnya). Dipakai algoritma yang sama atas `public_id=...&version=...` (sesuai perilaku SDK Cloudinary); begitu kunci asli ada, uji satu unggahan nyata. Sintaks transformasi URL (`f_auto,q_auto,c_fill,ar_4:5,w_N`) dari pengetahuan umum, juga belum dicek ke dokumentasi.
- **Server tidak menerima URL dari klien:** URL dibangun dari `publicId`, `version`, dan format setelah signature diverifikasi; `publicId` wajib berawalan `tokoku/products/`. Unggahan sah yang melanggar aturan (format, > 5 MB, > 6 foto) dihapus asetnya (best-effort).
- **Batas 5 MB** tidak bisa dipaksakan di Cloudinary lewat parameter unggah; ditegakkan di klien (sebelum unggah) dan di server (ukuran `bytes` dari respons, aset ditolak dan dihapus).
- **Tiga ukuran bernama** (thumbnail 160, kartu 480, detail 960 px, rasio 4:5) saja, agar transformasi tidak meledak di paket gratis berbasis kredit.
- **`products.image_url` lama:** dimigrasikan menjadi baris `product_images` eksternal (tanpa `public_id`, menjadi foto utama). Kolom dibiarkan di tabel tetapi **tidak lagi dipetakan** dan `imageUrl` dihapus dari `ProductRequest` (kolom bisa dihapus di migrasi berikutnya). `ProductResponse.imageUrl` tetap ada sebagai foto utama ukuran kartu (kompatibilitas); daftar lengkap di `images[]`.
- **Foto utama:** indeks unik parsial "satu foto utama per produk" dicek per baris oleh Postgres, jadi mengganti foto utama dua langkah (lepas, lalu set). Hapus foto: baris dihapus dulu, baru foto berikutnya dijadikan utama; penghapusan di Cloudinary setelah commit dan tidak menggagalkan permintaan.
- **Provider fake** (test dan dev tanpa kunci): `POST /api/dev/uploads` meniru respons Cloudinary dan menyimpan berkas di memori (60 terakhir) agar e2e dan dev bisa mengunggah sungguhan; hanya `!prod` + provider fake. `app.images.provider` default `cloudinary` dan gagal start bila kunci kosong; `scripts/dev-backend.sh` memakai `fake` bila kunci Cloudinary tidak ada.

### B4 Invoice
- **Nomor `INV-YYYYMM-NNNNN`** dari `invoice_counters` (satu baris per bulan) dengan `INSERT ... ON CONFLICT DO UPDATE ... RETURNING` yang atomik; baris terkunci sampai commit, tanpa celah nomor bila transaksi gagal (dibuktikan test 20 thread). Dibuat saat order menjadi PAID; webhook ganda berhenti di cek status payment, sehingga nomor tidak berubah. Zona waktu `app.timezone` (default Asia/Jakarta).
- **`payment_type`** dari notifikasi Midtrans disimpan di `payments` dan dicetak sebagai metode pembayaran (`bank_transfer` → "Transfer bank", `fake` → "Simulasi pembayaran").
- **PDF:** openhtmltopdf (PDFBox 3), templat XHTML dengan tabel (tanpa flex/grid). Font Fraunces dan Hanken Grotesk sebagai **instans TTF statis** (dibuat dengan fontTools dari font variabel) di `src/main/resources/fonts/`, beserta berkas OFL dan README lisensi. **Reserved Font Name:** hanya definisi OFL yang memuat frasa itu; pernyataan hak cipta kedua font tidak mendeklarasikan RFN, jadi versi instans boleh disertakan tanpa ganti nama (kewajiban OFL lain: lisensi dan hak cipta ikut, font tidak dijual terpisah).
- **Latar PDF putih** (bukan paper krem seperti mock): invoice sering dicetak. Bingkai halaman dan garis memakai nada hangat tipis. **Tanda "Lunas"** memakai bingkai ganda 2,25pt `#1F3A2C` dan teks gelap; test menghitung piksel gelap di citra skala abu-abu agar terbukti tetap terbaca saat dicetak hitam-putih.
- **Memori:** invoice 10 item = ±20 KB, ±30-40 ms, kenaikan heap ±10 MB di test; font dibaca dari jar tiap render (±180 KB) dan di-subset. Cocok untuk RAM 512 MB.
- **Dua rute:** `GET /api/orders/{id}/invoice` (pemilik; milik orang lain 404; realm toko) dan `GET /api/admin/orders/{id}/invoice` (admin). 409 bila order belum dibayar atau pengaturan toko belum lengkap.
- Tidak ada baris pajak terhitung; hanya `NPWP/NIB` dan catatan pajak bila dikonfigurasi (lihat B2).
