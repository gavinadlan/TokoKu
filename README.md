# TokoKu

Backend e-commerce Spring Boot (Java 21, Spring Boot 4.1.2, Postgres, Flyway). Proyek belajar bertahap.
Dokumen produk dan infrastruktur: [docs/PRD.md](docs/PRD.md).

**Status: Fase 1** (CRUD produk + kategori, test, Docker, CI). Auth/JWT, cart, order, payment, Redis belum ada.
Semua endpoint saat ini `permitAll` (`TODO Fase 2`), jadi jangan deploy untuk data nyata.

## Menjalankan lokal

Butuh JDK 21 dan Docker.

```bash
cp .env.example .env            # isi DB_PASSWORD
docker compose up -d db         # Postgres di localhost:5432
set -a; source .env; set +a
export DATABASE_PASSWORD="$DB_PASSWORD"
./mvnw spring-boot:run
```

Jika port 5432 sudah terpakai, isi `DB_PORT=5433` (atau lainnya) di `.env` dan `export DATABASE_URL=jdbc:postgresql://localhost:5433/tokoku`.

- Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

Test (unit + integrasi, Testcontainers butuh Docker aktif):

```bash
./mvnw -B verify
```

Seluruh stack lewat Docker (profil `prod`):

```bash
docker compose up --build
```

## Environment variable

| Nama | Dipakai | Keterangan |
| --- | --- | --- |
| `DB_PASSWORD` | docker compose | Password Postgres container |
| `DB_PORT` | docker compose | Port host Postgres, default 5432 |
| `DATABASE_URL` | prod (wajib), dev (default `jdbc:postgresql://localhost:5432/tokoku`) | Format JDBC |
| `DATABASE_USER` | prod (wajib), dev (default `tokoku`) | |
| `DATABASE_PASSWORD` | semua | Tanpa default |
| `SPRING_PROFILES_ACTIVE` | prod | `prod` |
| `PORT` | prod | Default 8080 (Render mengisi sendiri) |

## Struktur paket

`com.tokoku`: `product` (Fase 1), `common` (security config, error handler), `auth`, `cart`, `order`, `payment` (placeholder).

## Endpoint Fase 1

Sesuai tabel PRD: `GET /api/products` (`q`, `category`, `page`, `size`, `sort`), `GET /api/products/{id}`, `POST`, `PUT /{id}`,
`PATCH /{id}/stock` (body `{"stock": n}`, mengatur nilai stok, minimal 0), `DELETE /{id}` (set `active=false`).

**Tambahan di luar tabel PRD:** `GET /api/categories` dan `POST /api/categories`, karena filter kategori butuh data kategori.

## Deploy (Render + Neon)

Ringkasan; detail di bagian "Infrastruktur & Deployment Gratis" di [docs/PRD.md](docs/PRD.md):
1. Push repo ke GitHub (pastikan `.env` tidak ikut).
2. Buat project Neon; ubah connection string ke format JDBC (`jdbc:postgresql://host/db?sslmode=require`).
3. Render: New Web Service, runtime Docker, instance Free.
4. Set `SPRING_PROFILES_ACTIVE=prod`, `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`.
5. Health check path `/actuator/health`; buka `/swagger-ui.html`.
