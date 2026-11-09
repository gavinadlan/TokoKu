# PRD & Infrastruktur: E-Commerce Spring Boot

Nov 10, 2026 · @gavin adlan hidayat

## Ringkasan & Tujuan

**TokoKu** adalah backend e-commerce berbasis Java Spring Boot yang dibangun bertahap oleh satu orang untuk belajar, dengan target akhir ter-deploy publik tanpa biaya dan layak jadi portofolio.

**Latar belakang.** Project e-commerce dipilih karena satu domain ini menyentuh hampir semua konsep backend: CRUD, relasi data, autentikasi, transaksi, integrasi pihak ketiga, caching, dan deployment.

**Tujuan belajar**

- Menguasai layering Spring Boot: controller, service, repository, DTO, exception handling.
- Memahami Spring Data JPA: relasi, pagination, query, migrasi skema dengan Flyway.
- Mengamankan API dengan Spring Security dan JWT, lengkap dengan role.
- Menjaga konsistensi data lewat `@Transactional` (stok dan order).
- Menulis test otomatis (JUnit 5, Mockito, Testcontainers).
- Membungkus aplikasi dengan Docker dan mengirimnya lewat CI/CD ke hosting gratis.

**Tujuan produk (kriteria selesai MVP)**

- Customer bisa register, login, melihat produk, mengisi keranjang, membuat order, dan membayar lewat sandbox.
- Admin bisa mengelola produk, stok, dan status order.
- Semua endpoint MVP terdokumentasi di Swagger dan punya minimal satu test.
- Aplikasi hidup di URL publik dengan biaya Rp0.

**Non-goals**

- Frontend lengkap. Cukup Swagger UI; frontend sederhana (React atau Thymeleaf) hanya opsional di fase akhir.
- Pembayaran uang sungguhan. Payment memakai mode sandbox.
- Skala besar. Target beban adalah demo dan portofolio, bukan ribuan pengguna bersamaan.

## Pengguna & User Stories

TokoKu melayani dua peran: **Customer** yang berbelanja, dan **Admin** yang mengelola katalog dan order. MVP mencakup 12 user stories berikut.

| ID | Peran | Saya ingin... | Agar... | Prioritas |
| --- | --- | --- | --- | --- |
| US-01 | Customer | mendaftar dan login dengan email dan password | punya akun pribadi | MVP |
| US-02 | Customer | melihat daftar produk dengan pencarian, filter kategori, dan pagination | cepat menemukan barang | MVP |
| US-03 | Customer | melihat detail produk beserta stok | tahu barang tersedia | MVP |
| US-04 | Customer | menambah, mengubah, dan menghapus item di keranjang | menyiapkan belanjaan | MVP |
| US-05 | Customer | membuat order dari keranjang | membeli barang | MVP |
| US-06 | Customer | membayar order lewat payment gateway sandbox | menyelesaikan transaksi | MVP |
| US-07 | Customer | melihat riwayat dan status order | memantau pesanan | MVP |
| US-08 | Customer | membatalkan order yang belum dibayar | stok kembali tersedia | Next |
| US-09 | Admin | menambah, mengubah, dan menonaktifkan produk | katalog selalu terbaru | MVP |
| US-10 | Admin | mengatur stok produk | stok sesuai gudang | MVP |
| US-11 | Admin | mengubah status order (diproses, dikirim, selesai) | customer tahu progres | MVP |
| US-12 | Admin | mengunggah gambar produk | katalog lebih menarik | Next |

## Fitur & Requirements

MVP berisi 7 modul inti; fitur bertanda Next dikerjakan setelah MVP stabil, dan Later hanya jika waktu cukup.

| Modul | Fitur | Prioritas |
| --- | --- | --- |
| Auth | Register, login, JWT access token, role USER dan ADMIN | MVP |
| Auth | Refresh token, ganti password, logout | Next |
| Auth | Reset password lewat email, login Google (OAuth2) | Later |
| Produk | CRUD produk (admin), daftar publik dengan search, filter kategori, sort, pagination | MVP |
| Produk | Kategori produk, gambar produk | Next |
| Keranjang | Tambah, ubah jumlah, hapus item, hitung subtotal | MVP |
| Order | Checkout dari keranjang, pengurangan stok atomik, riwayat order, status order | MVP |
| Order | Pembatalan order, pengembalian stok otomatis saat order kedaluwarsa (`@Scheduled`) | Next |
| Pembayaran | Pembayaran sandbox (Midtrans Snap atau Xendit), webhook notifikasi pembayaran | MVP |
| Admin | Kelola produk, stok, status order | MVP |
| Notifikasi | Email konfirmasi order | Later |
| Performa | Cache daftar produk dengan Redis, rate limiting login | Next |

**Kebutuhan non-fungsional**

- **Keamanan:** password di-hash BCrypt, token JWT berumur pendek, input divalidasi (`@Valid`), semua secret lewat environment variable dan tidak pernah masuk repository.
- **Konsistensi data:** stok tidak boleh negatif walau dua customer checkout bersamaan; dijaga dengan `@Transactional` dan optimistic locking (`@Version`).
- **Kualitas:** test otomatis untuk service layer (target cakupan 70% baris) dan integration test API memakai Testcontainers.
- **Dokumentasi:** OpenAPI/Swagger di `/swagger-ui.html`, README dengan langkah menjalankan lokal.
- **Observabilitas:** `/actuator/health` untuk health check, log berformat konsisten.
- **Performa (target belajar):** daftar produk merespons di bawah 500 ms untuk 10.000 produk saat aplikasi sudah hangat. Di hosting gratis, request pertama setelah idle bisa jauh lebih lambat (lihat bagian infrastruktur).

## Arsitektur & Tech Stack

TokoKu dimulai sebagai **modular monolith**: satu aplikasi Spring Boot dengan paket terpisah per fitur (`auth`, `product`, `cart`, `order`, `payment`). Pola ini lebih mudah dipelajari dan di-debug, dan tiap modul nanti bisa dipecah menjadi service sendiri.

**Tech stack**

| Lapisan | Pilihan | Alasan |
| --- | --- | --- |
| Bahasa dan runtime | Java 21 (LTS) | Versi LTS yang didukung luas oleh Spring Boot |
| Framework | Spring Boot versi stabil terbaru (buat lewat start.spring.io) | Web, Data JPA, Security, Validation, Actuator |
| Database | PostgreSQL | Relasional, gratis di banyak provider |
| Migrasi skema | Flyway | Skema ter-versi di Git, bukan `ddl-auto=update` |
| Cache dan rate limit | Redis (fase Next) | Latihan caching dan counter |
| Auth | Spring Security + JWT | Standar API stateless |
| Dokumentasi API | springdoc-openapi | Swagger UI otomatis |
| Test | JUnit 5, Mockito, Testcontainers | Unit dan integration test dengan DB sungguhan |
| Build | Maven atau Gradle | Pilih satu dan konsisten |
| Container dan CI | Docker, GitHub Actions | Build dan deploy otomatis |

**Tabel utama database**

| Tabel | Kolom penting | Relasi |
| --- | --- | --- |
| `users` | id, email (unik), password\_hash, name, role, created\_at | 1 user punya 1 cart dan banyak order |
| `categories` | id, name | 1 kategori punya banyak produk |
| `products` | id, category\_id, name, description, price, stock, active, version | banyak produk ke 1 kategori |
| `carts` | id, user\_id | 1 cart punya banyak cart\_items |
| `cart_items` | id, cart\_id, product\_id, quantity | menghubungkan cart dan produk |
| `orders` | id, user\_id, status, total\_amount, created\_at | 1 order punya banyak order\_items dan 1 payment |
| `order_items` | id, order\_id, product\_id, quantity, unit\_price | harga disalin saat checkout |
| `payments` | id, order\_id, provider\_ref, status, paid\_at | 1 order ke 1 payment |

Status order: `PENDING_PAYMENT`, `PAID`, `PROCESSING`, `SHIPPED`, `COMPLETED`, `CANCELLED`.

**Daftar endpoint MVP**

| Method | Path | Akses | Fungsi |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | Publik | Daftar akun |
| POST | `/api/auth/login` | Publik | Login, mengembalikan JWT |
| GET | `/api/products` | Publik | Daftar produk (`q`, `category`, `page`, `size`, `sort`) |
| GET | `/api/products/{id}` | Publik | Detail produk |
| POST | `/api/products` | Admin | Tambah produk |
| PUT | `/api/products/{id}` | Admin | Ubah produk |
| PATCH | `/api/products/{id}/stock` | Admin | Atur stok |
| DELETE | `/api/products/{id}` | Admin | Nonaktifkan produk |
| GET | `/api/cart` | Customer | Lihat keranjang |
| POST | `/api/cart/items` | Customer | Tambah item |
| PUT | `/api/cart/items/{id}` | Customer | Ubah jumlah |
| DELETE | `/api/cart/items/{id}` | Customer | Hapus item |
| POST | `/api/orders` | Customer | Checkout dari keranjang |
| GET | `/api/orders` | Customer | Riwayat order sendiri |
| GET | `/api/orders/{id}` | Customer | Detail order |
| POST | `/api/orders/{id}/pay` | Customer | Mulai pembayaran sandbox |
| POST | `/api/payments/webhook` | Provider | Notifikasi status pembayaran |
| GET | `/api/admin/orders` | Admin | Semua order |
| PATCH | `/api/admin/orders/{id}/status` | Admin | Ubah status order |

## Infrastruktur & Deployment Gratis

**Rekomendasi: aplikasi di Render, database di Neon, Redis di Upstash.** Ketiganya punya tier gratis yang menurut sumber pihak ketiga tidak meminta kartu kredit (cek lagi saat mendaftar), dan total biayanya Rp0. Harga yang harus diterima: aplikasi akan tidur setelah 15 menit tanpa traffic, dan request pertama setelah itu butuh sekitar satu menit.

Sebagai alternatif jika ingin belajar DevOps, aplikasi, Postgres, dan Redis bisa dijalankan dengan Docker Compose di VM gratis Oracle Cloud. VM itu selalu hidup (tanpa cold start), tetapi setup dan perawatannya manual.

&#91;embedded content: arsitektur deployment · 5 komponen\]

Developer push ke GitHub, Actions menjalankan test, lalu Render membangun image Docker dan men-deploy-nya; aplikasi membaca data dari Neon dan menerima webhook dari payment sandbox.

### Pilihan layanan gratis (per 10 November 2026)

| Layanan | Dipakai untuk | Batas gratis | Catatan |
| --- | --- | --- | --- |
| [Render](https://render.com/docs/free) web service | Menjalankan aplikasi (Docker) | 750 jam instans per bulan per workspace; tidur setelah 15 menit tanpa traffic, bangun sekitar 1 menit | Dipakai. Filesystem hilang saat restart. RAM sekitar 512 MB menurut ringkasan pihak ketiga, bukan dari halaman resmi |
| [Neon](https://neon.com/faqs/free-plan-limits-and-quotas) Postgres | Database | 1 GB storage dan 100 CU-jam compute per project per bulan | Dipakai. Compute tidur saat idle, jadi query pertama bisa lebih lambat |
| [Upstash](https://upstash.com/blog/upstash-vs-aws-elasticache-serverless-redis-pricing-and-performance-2026) Redis | Cache dan rate limit (fase Next) | 256 MB dan 500.000 command per bulan | Dipakai nanti |
| [Oracle Cloud Always Free](https://docs.oracle.com/en-us/iaas/Content/FreeTier/resourceref.htm) | Alternatif: VM untuk Docker Compose | 1.500 OCPU-jam dan 9.000 GB-jam per bulan (setara 2 OCPU dan 12 GB, Arm), transfer keluar 10 TB per bulan | Instance yang dianggap idle bisa diambil kembali Oracle. Banyak tutorial lama masih menulis 4 OCPU dan 24 GB, tetapi dokumentasi resmi sekarang menulis 2 OCPU dan 12 GB |
| Render Postgres | Tidak dipakai | 1 GB, kedaluwarsa 30 hari setelah dibuat, lalu dihapus setelah masa tenggang 14 hari | Tidak cocok untuk project jangka panjang |
| Fly.io, Koyeb | Tidak dipakai | Fly.io tidak punya free tier untuk akun baru ([sumber](https://render.com/articles/platforms-with-a-real-free-tier-for-developers-in-2026)); Koyeb dilaporkan menutup tier gratis untuk pengguna baru ([sumber](https://kuberns.com/blogs/koyeb-alternatives/)) | Keduanya dari sumber sekunder, cek ulang sebelum memutuskan |

### Konfigurasi aplikasi

**Dockerfile** (di root repo). Build bertahap membuat image akhir hanya berisi JRE, dan opsi JVM dibatasi supaya muat di RAM kecil.

```dockerfile
# ---- build stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -q dependency:go-offline
COPY src src
RUN ./mvnw -q package -DskipTests

# ---- runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd -r -u 1001 appuser
COPY --from=build /workspace/target/*.jar app.jar
USER appuser
EXPOSE 8080
# Batasi memori dan percepat startup untuk instance kecil
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65 -XX:+UseSerialGC -Xss512k -XX:TieredStopAtLevel=1"
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**`src/main/resources/application-prod.yml`**. Semua nilai sensitif dibaca dari environment variable. Neon memberi connection string berformat `postgresql://user:pass@host/db`; Spring butuh bentuk JDBC (`jdbc:postgresql://host/db?sslmode=require`) dengan username dan password terpisah.

```yaml
server:
  port: ${PORT:8080}
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USER}
    password: ${DATABASE_PASSWORD}
    hikari:
      maximum-pool-size: 5
      connection-timeout: 30000   # beri waktu saat database baru bangun
  jpa:
    hibernate:
      ddl-auto: validate          # skema dikelola Flyway
  flyway:
    enabled: true
app:
  jwt:
    secret: ${JWT_SECRET}
    expiration-minutes: 30
management:
  endpoints:
    web:
      exposure:
        include: health
```

**`docker-compose.yml`**. Dipakai untuk development lokal, dan bisa dipakai apa adanya di VM Oracle (tambahkan Caddy di depannya jika butuh HTTPS dengan domain sendiri).

```yaml
services:
  db:
    image: postgres:16
    environment:
      POSTGRES_DB: tokoku
      POSTGRES_USER: tokoku
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U tokoku"]
      interval: 5s
      retries: 10
  redis:
    image: redis:7-alpine
  app:
    build: .
    depends_on:
      db:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DATABASE_URL: jdbc:postgresql://db:5432/tokoku
      DATABASE_USER: tokoku
      DATABASE_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
    ports:
      - "8080:8080"
volumes:
  pgdata:
```

**`.github/workflows/ci.yml`**. Menjalankan build dan semua test (termasuk Testcontainers) pada setiap push ke `main` dan setiap pull request.

```yaml
name: ci
on:
  push:
    branches: [main]
  pull_request:
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21
          cache: maven
      - run: ./mvnw -B verify
```

### Langkah deploy (Render + Neon)

1. Push repo ke GitHub dan pastikan file `.env` dan secret tidak ikut ter-commit.
2. Buat project di Neon, salin host, nama database, user, dan password, lalu ubah ke format JDBC seperti di atas.
3. Di Render pilih **New → Web Service**, hubungkan repo, runtime **Docker**, instance type **Free**.
4. Isi environment variable: `SPRING_PROFILES_ACTIVE=prod`, `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`, `JWT_SECRET`, dan kunci sandbox payment.
5. Atur health check path ke `/actuator/health`. Render akan build image dan deploy otomatis dari Git setiap ada push.
6. Buka `https://<nama-service>.onrender.com/swagger-ui.html`. Request pertama setelah idle bisa memakan sekitar satu menit.
7. Daftarkan `https://<nama-service>.onrender.com/api/payments/webhook` sebagai URL notifikasi di dashboard sandbox payment.

### Batasan yang perlu diantisipasi

- **Cold start.** Satu service yang menyala 24 jam memakai sekitar 720 jam, di bawah jatah 750 jam, jadi memanggil `/actuator/health` secara berkala dari monitor eksternal bisa menjaganya tetap bangun. Cek dulu syarat layanan Render sebelum memakai cara ini.
- **Filesystem sementara.** Perubahan file lokal hilang saat redeploy atau restart, sehingga gambar produk (fitur Next) harus disimpan di object storage, bukan di disk aplikasi.
- **Email.** Render memblokir port SMTP keluar 25, 465, dan 587. Untuk fitur email (Later) pakai penyedia email berbasis HTTP API.
- **Jaringan privat.** Free web service tidak bisa menerima traffic jaringan privat. Saat fase microservices, antar-service harus lewat URL publik, atau pindah ke Docker Compose di VM Oracle.
- **RAM kecil.** Opsi JVM di Dockerfile wajib dipertahankan. Tanpa pembatasan, JVM mudah terkena out-of-memory di instance 512 MB.

## Roadmap Belajar

MVP selesai dalam lima fase pertama (sekitar delapan minggu); fase terakhir adalah pengayaan. Tiap fase menambah satu kelompok konsep di atas project yang sama, dan fase berikutnya baru dimulai setelah syarat lulus (gate) fase ini terpenuhi. Deploy pertama sengaja dilakukan di fase 1, supaya pipeline terbukti jalan sebelum fitur menumpuk. Durasi hanyalah perkiraan untuk belajar sekitar 8 sampai 10 jam per minggu, dan boleh digeser.

&#91;embedded content: roadmap belajar · 6 fase, 6 gate\]

Fase 6 baru dimulai setelah MVP lolos gate fase 5; microservices (memecah modul menjadi service sendiri) adalah bonus di dalamnya, bukan syarat.

## Sumber

Batas gratis dicek pada 10 November 2026 dan bisa berubah; konfirmasi di halaman resmi sebelum mendaftar.

- [Render: Deploy for Free](https://render.com/docs/free)
- [Neon: Free plan limits and quotas](https://neon.com/faqs/free-plan-limits-and-quotas)
- [Upstash vs AWS ElastiCache (harga Redis)](https://upstash.com/blog/upstash-vs-aws-elasticache-serverless-redis-pricing-and-performance-2026)
- [Oracle: Always Free Resources](https://docs.oracle.com/en-us/iaas/Content/FreeTier/resourceref.htm)
- [Render: platforms with a free tier in 2026](https://render.com/articles/platforms-with-a-real-free-tier-for-developers-in-2026) dan [Kuberns: Koyeb alternatives](https://kuberns.com/blogs/koyeb-alternatives/) (sumber sekunder untuk status Fly.io dan Koyeb)
