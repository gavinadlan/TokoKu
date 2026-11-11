# TokoKu

Backend e-commerce Spring Boot (Java 21, Spring Boot 4.1.2, PostgreSQL, Flyway, JWT, Midtrans Snap).
Proyek belajar bertahap. Dokumen produk dan infrastruktur: [docs/PRD.md](docs/PRD.md).
Catatan keputusan: [docs/DECISIONS.md](docs/DECISIONS.md). Log kemajuan dan bukti verifikasi: [docs/PROGRESS.md](docs/PROGRESS.md).

Backend lengkap (auth terpisah toko/admin, katalog dengan foto, keranjang, order kirim/ambil, pembayaran Midtrans, invoice PDF). Frontend belum ada: gunakan Swagger UI.
Belum ada: cache Redis (opsional di PRD, dilewati), login Google, ongkir per zona atau API kurir.

## Frontend

Aplikasi web ada di [`frontend/`](frontend/README.md) (React + TypeScript). Arah desain: [docs/DESIGN.md](docs/DESIGN.md).

```bash
scripts/dev-backend.sh      # terminal 1: backend dev + data seed + CORS untuk :5173; pembayaran Midtrans bila kunci ada di .env, kalau tidak fake
cd frontend && npm install && npm run dev      # terminal 2: http://localhost:5173
```

## Menjalankan lokal

Butuh JDK 21 dan Docker.

```bash
cp .env.example .env                 # isi DB_PASSWORD (dan JWT_SECRET, ADMIN_*, kunci Midtrans bila ada)
docker compose up -d db              # Postgres di localhost:${DB_PORT:-5432}
set -a; source .env; set +a
export DATABASE_PASSWORD="$DB_PASSWORD"
# Port 5432 bentrok? isi DB_PORT=55432 di .env dan: export DATABASE_URL=jdbc:postgresql://localhost:55432/tokoku

# Dengan data contoh (kopi, identitas toko contoh) dan tanpa akun Midtrans:
SPRING_PROFILES_ACTIVE=dev APP_PAYMENT_PROVIDER=fake ./mvnw spring-boot:run
```

- Swagger UI: http://localhost:8080/swagger-ui.html (klik **Authorize**, tempel token dari `POST /api/auth/login`)
- Health: http://localhost:8080/actuator/health
- Admin awal: isi `ADMIN_EMAIL` dan `ADMIN_PASSWORD` di `.env` sebelum start (dibuat sekali jika belum ada admin). Masuk lewat `/admin/masuk` di frontend atau `POST /api/admin/auth/login`.
- `SPRING_PROFILES_ACTIVE=dev` mengisi sekitar 30 produk bertema kopi (hanya jika belum ada produk).
- Memilih provider pembayaran di `scripts/dev-backend.sh`: `APP_PAYMENT_PROVIDER` eksplisit (di `.env` atau shell) selalu menang; kalau kosong, `midtrans` bila `MIDTRANS_SERVER_KEY` dan `MIDTRANS_CLIENT_KEY` terisi, selain itu `fake`. Paksa simulasi: `APP_PAYMENT_PROVIDER=fake scripts/dev-backend.sh`. Backend mencatat `Payment provider: ...` saat start (tanpa kunci), gagal start bila `midtrans` tanpa kunci, dan halaman/endpoint simulasi hanya ada saat `fake`. Test dan CI selalu `fake`.

Test (unit + integrasi dengan Testcontainers; Docker harus aktif) dan gate cakupan JaCoCo:

```bash
./mvnw -B verify          # laporan: target/site/jacoco/index.html
```

Seluruh stack lewat Docker (profil `prod`, butuh `JWT_SECRET`, `DB_PASSWORD`, kunci Midtrans atau `APP_PAYMENT_PROVIDER=fake` tidak boleh di prod):

```bash
docker compose up --build
```

## Environment variable

| Nama | Dev | Prod | Keterangan |
| --- | --- | --- | --- |
| `DB_PASSWORD` | compose | compose | Password Postgres container |
| `DB_PORT` | opsional | - | Port host Postgres, default 5432 |
| `DATABASE_URL` | default `jdbc:postgresql://localhost:5432/tokoku` | wajib | Format JDBC (Neon: tambah `?sslmode=require`) |
| `DATABASE_USER` | default `tokoku` | wajib | |
| `DATABASE_PASSWORD` | wajib | wajib — **RAHASIA** | Tanpa default |
| `SPRING_PROFILES_ACTIVE` | `dev` (seed data) | `prod` | |
| `PORT` | - | otomatis (Render) | Default 8080 |
| `JWT_SECRET` | opsional | **wajib** — **RAHASIA**, min 32 byte | Kosong di dev = kunci acak sementara (token hangus saat restart). Buat: `openssl rand -base64 48` |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | opsional | opsional; `ADMIN_PASSWORD` **RAHASIA** | Admin awal, idempoten; password awal sementara (wajib diganti) |
| `MIDTRANS_CORE_API_URL` | opsional | `https://api.sandbox.midtrans.com/v2` | Status API; produksi `https://api.midtrans.com/v2` |
| `APP_PAYMENT_RECONCILE_MS` | opsional | `300000` | Jeda rekonsiliasi berkala |
| `APP_FRONTEND_BASE_URL` | wajib di prod | `http://localhost:5173` | URL publik frontend; dikirim sebagai `callbacks.finish` ke Midtrans (`{URL}/pembayaran/selesai`). Prod gagal start bila kosong/localhost |
| `APP_MAIL_ENABLED` | opsional | `true` | `false` = email dimatikan (no-op) |
| `APP_MAIL_PROVIDER` | `smtp` | `brevo` di Render | `smtp` \| `brevo` (API HTTPS) \| `noop`. Render free memblokir SMTP keluar |
| `BREVO_API_KEY` | opsional | wajib jika `brevo` — **RAHASIA** | Kosong = no-op (dengan peringatan di log). Tidak pernah dicetak |
| `APP_SEED_DEMO` | `false` | `false` | `true` = produk contoh + toko demo, hanya bila katalog kosong / toko belum diisi |
| `DEMO_USER_EMAIL` | `demo@tokoku.example` | idem | Akun pembeli demo |
| `DEMO_USER_PASSWORD` | kosong | kosong — **RAHASIA** | Kosong = akun demo tidak dibuat (tidak ada password bawaan) |
| `APP_ADMIN_FORCE_PASSWORD_CHANGE` | `false` di `scripts/dev-backend.sh` | `true` | Admin dari `ADMIN_EMAIL`/`ADMIN_PASSWORD` wajib ganti password di login pertama |
| `VITE_API_URL` | `http://localhost:8080` | URL backend | Frontend, dibaca saat **build** |
| `MAIL_HOST`, `MAIL_PORT` | opsional | `smtp.gmail.com`, `587` | STARTTLS wajib |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | untuk email smtp | kosong; `MAIL_PASSWORD` **RAHASIA** | Gmail + App Password; kosong = no-op |
| `MAIL_FROM` | opsional | `TokoKu <noreply@tokoku.local>` | Gmail menulis ulang pengirim ke akun Anda; isi alamat yang sama dengan `MAIL_USERNAME` |
| `APP_CORS_ALLOWED_ORIGINS` | opsional | isi origin frontend | Dipisah koma |
| `APP_PAYMENT_PROVIDER` | `midtrans` / `fake` | `midtrans` | `fake` ditolak di prod |
| `MIDTRANS_SERVER_KEY`, `MIDTRANS_CLIENT_KEY` | wajib jika `midtrans` | wajib; `MIDTRANS_SERVER_KEY` **RAHASIA** | App gagal start jika kosong |
| `MIDTRANS_API_URL` | default Snap sandbox | isi URL Snap produksi bila live | |
| `MIDTRANS_MERCHANT_ID`, `MIDTRANS_CORE_API_URL`, `MIDTRANS_USE_CORE_API` | - | - | Belum dipakai (kita memakai Snap) |
| `APP_IMAGES_PROVIDER` | `fake` bila kunci Cloudinary kosong | `cloudinary` | `fake` dilarang di prod |
| `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` | opsional | wajib jika `cloudinary`; `CLOUDINARY_API_SECRET` **RAHASIA** | App gagal start jika kosong |
| `APP_ORDER_EXPIRY_MINUTES` | 60 | 60 | Kedaluwarsa order belum dibayar |
| `APP_RATELIMIT_LOGIN_PER_EMAIL`, `APP_RATELIMIT_LOGIN_PER_IP` | 5 / 30 | 5 / 30 | Percobaan login toko per menit |
| `APP_RATELIMIT_ADMIN_LOGIN_PER_EMAIL`, `APP_RATELIMIT_ADMIN_LOGIN_PER_IP` | 3 / 10 | 3 / 10 | Percobaan login admin per menit (terpisah dari toko) |

## Login admin dan pembeli terpisah

Dua realm: token pembeli (`store`) hanya untuk endpoint pembeli (`/api/me`, keranjang, order, bayar, invoice), token admin (`admin`, umur 15 menit) hanya untuk `/api/admin/**` dan tulis produk/kategori. Akun admin tidak bisa masuk di `/api/auth/login`, akun pembeli tidak bisa masuk di `/api/admin/auth/login` (keduanya 401 dengan pesan yang sama dengan password salah). Refresh token punya realm yang sama dan tidak bisa dipakai lintas realm. Token lintas realm = 403.

## Pengambilan, ongkir, dan pengaturan toko

Checkout memilih `DELIVERY` (alamat wajib) atau `PICKUP` (cukup nama dan telepon). **Ongkir dihitung server** dari pengaturan toko (tarif flat; gratis bila subtotal mencapai ambang; ambil di toko selalu gratis) dan menjadi baris sendiri di `item_details` Midtrans, jadi `gross_amount` selalu sama dengan jumlah item. Status per jalur: kirim `PAID → PROCESSING → SHIPPED → COMPLETED` (resi wajib), ambil `PAID → PROCESSING → READY_FOR_PICKUP → COMPLETED` (kode ambil 6 karakter dibuat saat dibayar, dicocokkan tanpa peka huruf besar-kecil). **Di produksi** isi dulu Pengaturan toko di admin (nama, alamat, telepon): selama belum lengkap invoice mengembalikan 409 dan ambil di toko tidak tersedia. Data contoh hanya di-seed di profil `dev`.

## Foto produk

Foto disimpan di Cloudinary (paket gratis), diunggah **langsung dari browser** dengan tanda tangan dari server (`POST /api/admin/uploads/sign`), lalu didaftarkan ke produk (server memverifikasi signature dan membangun URL sendiri). Maksimal 6 foto per produk, jpg/png/webp, 5 MB. Respons produk memuat `images[]` dengan tiga ukuran bernama (thumbnail, kartu, detail; `f_auto,q_auto`, rasio 4:5).

**Mengaktifkan Cloudinary:** (1) daftar di cloudinary.com (paket gratis tanpa kartu), (2) salin *cloud name*, *API key*, dan *API secret* dari dashboard, (3) isi `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` di `.env` lokal (dan di environment Render), (4) jalankan ulang backend; tanpa kunci `scripts/dev-backend.sh` memakai penyimpanan `fake` yang menerima unggahan di server dev. Rumus signature *respons* unggah belum terverifikasi dari dokumentasi (lihat `docs/DECISIONS.md`): uji satu unggahan nyata setelah kunci terpasang.

## Invoice

`GET /api/orders/{id}/invoice` (pembeli) dan `GET /api/admin/orders/{id}/invoice` mengembalikan PDF A4 (nomor `INV-YYYYMM-NNNNN` unik dan tidak berubah walau webhook ganda). Dirender backend (openhtmltopdf) dengan font Fraunces dan Hanken Grotesk yang tertanam (lisensi OFL di `src/main/resources/fonts/`). Identitas toko dan catatan kaki dari Pengaturan toko; NPWP/NIB dan catatan pajak hanya dicetak bila diisi (aplikasi tidak menghitung pajak). Contoh: `docs/design/screens/invoice-delivery.png`.

## Struktur paket

`com.tokoku`: `auth` (user, JWT per realm, login), `product` (produk, kategori, foto), `cart`, `order` (checkout, status per jalur, ongkir), `payment` (gateway Midtrans/fake, webhook), `store` (pengaturan toko), `image` (Cloudinary/fake), `invoice` (nomor dan PDF), `common` (security, error handler, OpenAPI, seeder dev).

## Autentikasi

1. `POST /api/auth/register` membuat akun role `USER`.
2. `POST /api/auth/login` mengembalikan `accessToken` (JWT HS256, 30 menit) dan data `user`.
3. Kirim `Authorization: Bearer <token>` pada setiap request yang butuh login. `GET /api/me` mengembalikan profil dan role.
4. Login juga mengembalikan `refreshToken` (acak, berlaku 14 hari, hanya hash-nya disimpan di DB). Saat access token habis, `POST /api/auth/refresh` menukarnya dengan access token baru **dan refresh token baru**; yang lama dicabut (rotasi). Memakai ulang refresh token lama dianggap pencurian: seluruh sesi pengguna dicabut.
5. `POST /api/auth/logout` (body `refreshToken`) mencabut sesi. `POST /api/auth/change-password` (login, butuh password lama) mencabut semua refresh token.
6. Login dibatasi 5 percobaan per menit per email dan 30 per menit per IP (429 + header `Retry-After`; in-memory, atur lewat `app.ratelimit.login.*`).
7. Role: `USER` (belanja) dan `ADMIN` (kelola produk, kategori, order). Tanpa token = 401, role kurang = 403, keduanya dalam format `ProblemDetail`.

## Alur pembayaran

1. Customer checkout: `POST /api/orders` (stok dikurangi atomik, status `PENDING_PAYMENT`).
2. `POST /api/orders/{id}/pay` mengembalikan `token`, `redirectUrl`, dan `clientKey`. Frontend membuka `redirectUrl` (atau Snap.js dengan `token`). Memanggil ulang mengembalikan pembayaran yang sama.
3. Midtrans memanggil `POST /api/payments/webhook`. Signature (`SHA512(order_id+status_code+gross_amount+ServerKey)`) diverifikasi, nominal dicocokkan. `settlement` (atau `capture` dengan fraud `accept`) menjadikan order `PAID`. `expire`/`cancel`/`deny` membatalkan order dan mengembalikan stok. Webhook ganda tidak mengubah apa pun.
4. Admin memajukan status: `PROCESSING` → `SHIPPED` → `COMPLETED`.

Pembatalan dan kedaluwarsa: customer dapat `POST /api/orders/{id}/cancel` selama `PENDING_PAYMENT` (stok kembali). Order yang belum dibayar melewati `APP_ORDER_EXPIRY_MINUTES` (default 60) dibatalkan otomatis dan stoknya kembali, lewat scheduler (tiap 5 menit) **dan** pengecekan "malas" saat order dibaca, `/pay`, dan checkout. Pengecekan malas dibutuhkan karena di Render gratis aplikasi tidur saat idle sehingga scheduler tidak berjalan.

Provider `fake` (dev/test): `POST /api/dev/payments/{providerRef}/simulate?status=settlement|expire|deny` (tidak ada di prod).

Atur Notification URL di dashboard Midtrans **sandbox** (Settings → Configuration → Payment Notification URL):
- Lokal: Midtrans tidak bisa memanggil `localhost`. Buka tunnel publik (mis. `cloudflared tunnel --url http://localhost:8080` atau `ngrok http 8080`), lalu isi `https://<tunnel>/api/payments/webhook`.
- Deploy: `https://<service>.onrender.com/api/payments/webhook`.

**Tanpa webhook pun status ikut berubah:** halaman `/pembayaran/selesai` dan tombol "Periksa status" memanggil `POST /api/orders/{id}/payment/refresh`, yang menanyakan Midtrans Status API (`MIDTRANS_CORE_API_URL`, default `https://api.sandbox.midtrans.com/v2`) lalu menerapkan hasilnya lewat jalur yang sama dengan webhook. Scheduler juga merekonsiliasi payment PENDING berumur 2 menit sampai 24 jam (maks 20 per putaran, tiap 5 menit, `APP_PAYMENT_RECONCILE_MS`).

Tanpa tunnel, uji webhook dengan notifikasi buatan: `scripts/simulate-midtrans-webhook.sh <providerRef> <gross_amount> [status]` (ditandatangani dengan `MIDTRANS_SERVER_KEY` dari `.env`).

## Endpoint

Lihat tabel lengkap di bagian **Kontrak API** di bawah (diperbarui bersama kode); sumber kebenaran: Swagger UI `/swagger-ui.html`.

### Kontrak API

| Method | Path | Akses | Fungsi |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | Publik | Daftar akun |
| POST | `/api/auth/login` | Publik | Login **pembeli** (hanya USER), JWT realm `store` + refresh token (429 bila terlalu sering) |
| POST | `/api/auth/refresh` | Publik | Tukar refresh token (rotasi) |
| POST | `/api/auth/logout` | Publik | Cabut refresh token |
| POST | `/api/auth/change-password` | Login | Ganti password |
| GET | `/api/me` | Pembeli | Profil dan role |
| POST | `/api/admin/auth/login` | Publik | Login **admin** (hanya ADMIN), JWT realm `admin` (15 menit), limiter lebih ketat |
| POST | `/api/admin/auth/refresh`, `/api/admin/auth/logout` | Publik | Refresh/logout admin (token toko ditolak) |
| GET | `/api/admin/me` | Admin | Profil admin |
| GET | `/api/store` | Publik | Info toko (alamat, jam buka, ongkir, metode aktif) |
| GET, PUT | `/api/admin/store-settings` | Admin | Pengaturan toko (identitas, ongkir, invoice, pajak/NPWP) |
| GET | `/api/products` | Publik | Daftar produk aktif (`q`, `category`, `page`, `size`, `sort`) |
| GET | `/api/products/{id}` | Publik | Detail produk aktif |
| POST | `/api/products` | Admin | Tambah produk (foto lewat endpoint foto) |
| PUT | `/api/products/{id}` | Admin | Ubah produk |
| PATCH | `/api/products/{id}/stock` | Admin | Atur stok (`{"stock": n}`) |
| DELETE | `/api/products/{id}` | Admin | Nonaktifkan produk |
| GET | `/api/admin/products` | Admin | Semua produk, filter `active` |
| GET | `/api/categories` | Publik | Daftar kategori (tambahan di luar tabel PRD) |
| POST | `/api/categories` | Admin | Tambah kategori (tambahan di luar tabel PRD) |
| PUT | `/api/categories/{id}` | Admin | Ubah nama kategori |
| DELETE | `/api/categories/{id}` | Admin | Hapus kategori (409 jika masih punya produk) |
| GET | `/api/cart` | Login | Lihat keranjang |
| POST | `/api/cart/items` | Login | Tambah item |
| PUT | `/api/cart/items/{id}` | Login | Ubah jumlah |
| DELETE | `/api/cart/items/{id}` | Login | Hapus item |
| POST | `/api/orders` | Pembeli | Checkout dari keranjang (`fulfillmentType`: `DELIVERY` atau `PICKUP`; ongkir dihitung server) |
| GET | `/api/orders/{id}/invoice` | Pembeli | Invoice PDF (hanya order yang sudah dibayar) |
| GET | `/api/admin/orders/{id}/invoice` | Admin | Invoice PDF order apa pun |
| GET | `/api/orders` | Login | Riwayat order sendiri |
| GET | `/api/orders/{id}` | Login | Detail order sendiri |
| POST | `/api/orders/{id}/cancel` | Login | Batalkan order belum dibayar |
| POST | `/api/orders/{id}/pay` | Login | Mulai pembayaran |
| POST | `/api/payments/webhook` | Publik (signature) | Notifikasi Midtrans |
| GET | `/api/admin/orders` | Admin | Semua order (`status`, pagination) |
| GET | `/api/admin/orders/{id}` | Admin | Detail order |
| PATCH | `/api/admin/orders/{id}/status` | Admin | Ubah status sesuai jalur; `SHIPPED` butuh `courier` + `trackingNumber`, menyelesaikan PICKUP butuh `pickupCode` |
| GET | `/api/admin/products/{id}` | Admin | Detail produk (termasuk nonaktif) dengan foto |
| POST | `/api/admin/uploads/sign` | Admin | Tanda tangan unggah foto langsung ke Cloudinary |
| POST | `/api/admin/products/{id}/images` | Admin | Daftarkan foto yang sudah diunggah (signature diverifikasi, maks. 6) |
| PUT | `/api/admin/products/{id}/images/order` | Admin | Urutkan ulang foto |
| PATCH | `/api/admin/products/{id}/images/{imageId}/primary` | Admin | Jadikan foto utama |
| DELETE | `/api/admin/products/{id}/images/{imageId}` | Admin | Hapus foto |
| POST | `/api/dev/uploads` | Dev (images fake) | Meniru unggahan Cloudinary di dev |
| POST | `/api/dev/payments/{ref}/simulate` | Dev (provider fake) | Simulasi webhook |

## Email notifikasi

Pembeli menerima email saat pesanan dibuat, dibayar, diproses, siap diambil (dengan alamat, jam toko, kode ambil), dikirim (dengan resi), selesai, dan dibatalkan/kedaluwarsa. Pengiriman berjalan di thread pool kecil setelah transaksi commit, dicoba maksimal 3 kali, dan kegagalan hanya di-log (tidak pernah menggagalkan webhook atau perubahan status). Tabel `email_log` (unik per pesanan + jenis) mencegah email dobel saat webhook dikirim ulang. Tanpa `MAIL_USERNAME`/`MAIL_PASSWORD` (atau `APP_MAIL_ENABLED=false`) email dilewati dan hanya dicatat di log tanpa isi dan alamat penuh. Test dan CI selalu no-op.

**Struk dan lampiran:** email "pesanan dibuat" dan "pembayaran diterima" berupa struk (identitas toko, tanggal WIB, pengambilan atau alamat kirim, tabel barang, ongkir, total, status). Email "pembayaran diterima" melampirkan PDF invoice bila sudah terbit; bila identitas toko belum diisi, email tetap terkirim tanpa lampiran. Pratinjau semua template (backend dev berjalan): buka `http://localhost:8080/api/dev/email-preview/PAID?variant=delivery` (jenis: `ORDER_CREATED`, `PAID`, `PROCESSING`, `READY_FOR_PICKUP`, `SHIPPED`, `COMPLETED`, `CANCELLED`; tambah `&format=text` untuk versi teks), atau `scripts/email-preview.sh` untuk menulis semuanya ke `docs/design/email/` (screenshot: `cd frontend && npx playwright test --project=screens email-shots`).

**Membuat Gmail App Password:** (1) aktifkan Verifikasi 2 Langkah di akun Google, (2) buka myaccount.google.com/apppasswords, (3) buat sandi untuk "TokoKu", salin 16 karakternya (tanpa spasi), (4) isi `.env`: `MAIL_USERNAME=alamat@gmail.com`, `MAIL_PASSWORD=<16 karakter>`, `MAIL_FROM="TokoKu <alamat@gmail.com>"` (pakai tanda kutip), (5) jalankan ulang `scripts/dev-backend.sh` (log menampilkan `Email: SMTP aktif`), lalu coba `scripts/test-email.sh alamat-tujuan@contoh.com` (hanya di dev). Gmail membatasi sekitar 500 email per hari untuk akun biasa; cukup untuk toko kecil, ganti `EmailSender` bila perlu lebih.

## Deploy (Render + Neon)

Detail: bagian "Infrastruktur & Deployment Gratis" di [docs/PRD.md](docs/PRD.md). Checklist:

- [ ] Repo di GitHub, `.env` tidak ikut commit. CI (`.github/workflows/ci.yml`) **belum pernah berjalan di GitHub**; periksa hasilnya pada push pertama.
- [ ] Neon: buat project, ubah connection string ke `jdbc:postgresql://HOST/DB?sslmode=require`, simpan user dan password terpisah.
- [ ] Render: New Web Service, runtime Docker, instance Free, health check path `/actuator/health`.
- [ ] Env di Render: `SPRING_PROFILES_ACTIVE=prod`, `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`, `JWT_SECRET`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `APP_CORS_ALLOWED_ORIGINS`, `APP_FRONTEND_BASE_URL`, `APP_MAIL_ENABLED`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, `MIDTRANS_SERVER_KEY`, `MIDTRANS_CLIENT_KEY`, `MIDTRANS_API_URL`, `MIDTRANS_CORE_API_URL`.
- [ ] Dashboard Midtrans: Notification URL `https://<service>.onrender.com/api/payments/webhook`.
- [ ] Buka `/swagger-ui.html`; request pertama setelah idle sekitar satu menit.
- [ ] Ganti password admin awal; jangan pakai kunci sandbox untuk uang sungguhan.

## Deploy gratis (Render + Neon)

Blueprint: [`render.yaml`](render.yaml) (backend Docker `tokoku-api` + static site `tokoku-web` dengan rewrite SPA). Cabang `deploy-prep`; belum pernah dijalankan di Render (YAML belum divalidasi dengan `render blueprint validate`).

1. **Neon**: buat project, salin koneksi **langsung** (bukan host `-pooler`; Flyway memakai advisory lock). `DATABASE_URL=jdbc:postgresql://<host>/<db>?sslmode=require`, user dan password terpisah di `DATABASE_USER`/`DATABASE_PASSWORD`. Pool Hikari prod 4 koneksi tanpa koneksi diam (Neon menutup koneksi menganggur); Flyway berjalan saat start dan mencoba ulang 5 kali sampai database bangun.
2. **Brevo**: buat API key (v3), verifikasi pengirim, isi `BREVO_API_KEY` dan `MAIL_FROM` (`Nama <alamat-terverifikasi>`).
3. **Render**: New > Blueprint, isi semua nilai `sync: false`. Isi `APP_FRONTEND_BASE_URL`, `APP_CORS_ALLOWED_ORIGINS` (URL `tokoku-web`) dan `VITE_API_URL` (URL `tokoku-api`) setelah URL kedua service diketahui; deploy ulang `tokoku-web` bila `VITE_API_URL` berubah.
4. **Login pertama admin**: masuk di `/admin/masuk` dengan `ADMIN_PASSWORD`; halaman ganti password muncul otomatis dan seluruh API admin menolak (403) sampai password diganti.
5. **Demo** (opsional): `APP_SEED_DEMO=true` (+ `DEMO_USER_PASSWORD` untuk akun pembeli demo), lalu kembalikan ke `false` setelah data terbentuk.

Waktu start di container dan batas memori: lihat [docs/DECISIONS.md](docs/DECISIONS.md).
