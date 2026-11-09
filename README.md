# TokoKu

Backend e-commerce Spring Boot (Java 21, Spring Boot 4.1.2, PostgreSQL, Flyway, JWT, Midtrans Snap).
Proyek belajar bertahap. Dokumen produk dan infrastruktur: [docs/PRD.md](docs/PRD.md).
Catatan keputusan: [docs/DECISIONS.md](docs/DECISIONS.md). Log kemajuan dan bukti verifikasi: [docs/PROGRESS.md](docs/PROGRESS.md).

Backend lengkap (auth, katalog, keranjang, order, pembayaran). Frontend belum ada: gunakan Swagger UI.

## Menjalankan lokal

Butuh JDK 21 dan Docker.

```bash
cp .env.example .env                 # isi DB_PASSWORD (dan JWT_SECRET, ADMIN_*, kunci Midtrans bila ada)
docker compose up -d db              # Postgres di localhost:${DB_PORT:-5432}
set -a; source .env; set +a
export DATABASE_PASSWORD="$DB_PASSWORD"
# Port 5432 bentrok? isi DB_PORT=55432 di .env dan: export DATABASE_URL=jdbc:postgresql://localhost:55432/tokoku

# Dengan data contoh (kopi) dan tanpa akun Midtrans:
SPRING_PROFILES_ACTIVE=dev APP_PAYMENT_PROVIDER=fake ./mvnw spring-boot:run
```

- Swagger UI: http://localhost:8080/swagger-ui.html (klik **Authorize**, tempel token dari `POST /api/auth/login`)
- Health: http://localhost:8080/actuator/health
- Admin awal: isi `ADMIN_EMAIL` dan `ADMIN_PASSWORD` di `.env` sebelum start (dibuat sekali jika belum ada admin).
- `SPRING_PROFILES_ACTIVE=dev` mengisi sekitar 30 produk bertema kopi (hanya jika belum ada produk).
- Dengan kunci Midtrans sandbox di `.env`, buang `APP_PAYMENT_PROVIDER=fake`; `/pay` akan memanggil Midtrans sungguhan.

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
| `DATABASE_PASSWORD` | wajib | wajib | Tanpa default |
| `SPRING_PROFILES_ACTIVE` | `dev` (seed data) | `prod` | |
| `PORT` | - | otomatis (Render) | Default 8080 |
| `JWT_SECRET` | opsional | **wajib**, min 32 byte | Kosong di dev = kunci acak sementara (token hangus saat restart). Buat: `openssl rand -base64 48` |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | opsional | opsional | Admin awal, idempoten |
| `APP_CORS_ALLOWED_ORIGINS` | opsional | isi origin frontend | Dipisah koma |
| `APP_PAYMENT_PROVIDER` | `midtrans` / `fake` | `midtrans` | `fake` ditolak di prod |
| `MIDTRANS_SERVER_KEY`, `MIDTRANS_CLIENT_KEY` | wajib jika `midtrans` | wajib | App gagal start jika kosong |
| `MIDTRANS_API_URL` | default Snap sandbox | isi URL Snap produksi bila live | |
| `MIDTRANS_MERCHANT_ID`, `MIDTRANS_CORE_API_URL`, `MIDTRANS_USE_CORE_API` | - | - | Belum dipakai (kita memakai Snap) |
| `APP_ORDER_EXPIRY_MINUTES` | 60 | 60 | Kedaluwarsa order belum dibayar |

## Struktur paket

`com.tokoku`: `auth` (user, JWT, login), `product` (produk dan kategori), `cart`, `order` (checkout, status), `payment` (gateway Midtrans/fake, webhook), `common` (security, error handler, OpenAPI, seeder dev).

## Autentikasi

1. `POST /api/auth/register` membuat akun role `USER`.
2. `POST /api/auth/login` mengembalikan `accessToken` (JWT HS256, 30 menit) dan data `user`.
3. Kirim `Authorization: Bearer <token>` pada setiap request yang butuh login. `GET /api/me` mengembalikan profil dan role.
4. Role: `USER` (belanja) dan `ADMIN` (kelola produk, kategori, order). Tanpa token = 401, role kurang = 403, keduanya dalam format `ProblemDetail`.

## Alur pembayaran

1. Customer checkout: `POST /api/orders` (stok dikurangi atomik, status `PENDING_PAYMENT`).
2. `POST /api/orders/{id}/pay` mengembalikan `token`, `redirectUrl`, dan `clientKey`. Frontend membuka `redirectUrl` (atau Snap.js dengan `token`). Memanggil ulang mengembalikan pembayaran yang sama.
3. Midtrans memanggil `POST /api/payments/webhook`. Signature (`SHA512(order_id+status_code+gross_amount+ServerKey)`) diverifikasi, nominal dicocokkan. `settlement` (atau `capture` dengan fraud `accept`) menjadikan order `PAID`. `expire`/`cancel`/`deny` membatalkan order dan mengembalikan stok. Webhook ganda tidak mengubah apa pun.
4. Admin memajukan status: `PROCESSING` → `SHIPPED` → `COMPLETED`.

Provider `fake` (dev/test): `POST /api/dev/payments/{providerRef}/simulate?status=settlement|expire|deny` (tidak ada di prod).

Atur Notification URL di dashboard Midtrans **sandbox** (Settings → Configuration → Payment Notification URL):
- Lokal: Midtrans tidak bisa memanggil `localhost`. Buka tunnel publik (mis. `cloudflared tunnel --url http://localhost:8080` atau `ngrok http 8080`), lalu isi `https://<tunnel>/api/payments/webhook`.
- Deploy: `https://<service>.onrender.com/api/payments/webhook`.

Tanpa tunnel, uji webhook dengan notifikasi buatan: `scripts/simulate-midtrans-webhook.sh <providerRef> <gross_amount> [status]` (ditandatangani dengan `MIDTRANS_SERVER_KEY` dari `.env`).

## Endpoint

Lihat tabel lengkap di bagian **Kontrak API** di bawah (diperbarui bersama kode); sumber kebenaran: Swagger UI `/swagger-ui.html`.

### Kontrak API

| Method | Path | Akses | Fungsi |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | Publik | Daftar akun |
| POST | `/api/auth/login` | Publik | Login, JWT |
| GET | `/api/me` | Login | Profil dan role |
| GET | `/api/products` | Publik | Daftar produk aktif (`q`, `category`, `page`, `size`, `sort`) |
| GET | `/api/products/{id}` | Publik | Detail produk aktif |
| POST | `/api/products` | Admin | Tambah produk |
| PUT | `/api/products/{id}` | Admin | Ubah produk |
| PATCH | `/api/products/{id}/stock` | Admin | Atur stok (`{"stock": n}`) |
| DELETE | `/api/products/{id}` | Admin | Nonaktifkan produk |
| GET | `/api/admin/products` | Admin | Semua produk, filter `active` |
| GET | `/api/categories` | Publik | Daftar kategori (tambahan di luar tabel PRD) |
| POST | `/api/categories` | Admin | Tambah kategori (tambahan di luar tabel PRD) |
| GET | `/api/cart` | Login | Lihat keranjang |
| POST | `/api/cart/items` | Login | Tambah item |
| PUT | `/api/cart/items/{id}` | Login | Ubah jumlah |
| DELETE | `/api/cart/items/{id}` | Login | Hapus item |
| POST | `/api/orders` | Login | Checkout dari keranjang |
| GET | `/api/orders` | Login | Riwayat order sendiri |
| GET | `/api/orders/{id}` | Login | Detail order sendiri |
| POST | `/api/orders/{id}/pay` | Login | Mulai pembayaran |
| POST | `/api/payments/webhook` | Publik (signature) | Notifikasi Midtrans |
| GET | `/api/admin/orders` | Admin | Semua order (`status`, pagination) |
| GET | `/api/admin/orders/{id}` | Admin | Detail order |
| PATCH | `/api/admin/orders/{id}/status` | Admin | Ubah status |
| POST | `/api/dev/payments/{ref}/simulate` | Dev (provider fake) | Simulasi webhook |

## Deploy (Render + Neon)

Detail: bagian "Infrastruktur & Deployment Gratis" di [docs/PRD.md](docs/PRD.md). Checklist:

- [ ] Repo di GitHub, `.env` tidak ikut commit. CI (`.github/workflows/ci.yml`) **belum pernah berjalan di GitHub**; periksa hasilnya pada push pertama.
- [ ] Neon: buat project, ubah connection string ke `jdbc:postgresql://HOST/DB?sslmode=require`, simpan user dan password terpisah.
- [ ] Render: New Web Service, runtime Docker, instance Free, health check path `/actuator/health`.
- [ ] Env di Render: `SPRING_PROFILES_ACTIVE=prod`, `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`, `JWT_SECRET`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `APP_CORS_ALLOWED_ORIGINS`, `MIDTRANS_SERVER_KEY`, `MIDTRANS_CLIENT_KEY`, `MIDTRANS_API_URL`.
- [ ] Dashboard Midtrans: Notification URL `https://<service>.onrender.com/api/payments/webhook`.
- [ ] Buka `/swagger-ui.html`; request pertama setelah idle sekitar satu menit.
- [ ] Ganti password admin awal; jangan pakai kunci sandbox untuk uang sungguhan.
