# Keputusan yang menambah atau menyimpang dari PRD

## Fase 2
- **Role di klaim `role`, subject = user id.** `GET /api/me` membaca user dari DB, jadi perubahan data tidak basi. Token tidak berisi password/hash.
- **JWT_SECRET kosong**: prod gagal start; dev/test memakai kunci acak sementara per start (tidak ada secret di repo, token dev hangus saat restart). Secret < 32 byte ditolak di semua profil.
- **Register selalu role USER.** Admin hanya lewat bootstrap env (`ADMIN_EMAIL`/`ADMIN_PASSWORD`) atau DB.
- **Endpoint `/api/auth/*` publik dirinci satu per satu** (bukan `/api/auth/**`), karena `change-password` (Fase 6) harus login.
- **401/403 ditulis manual sebagai JSON ProblemDetail** di filter chain, karena exception handler MVC tidak menjangkau filter.
- **`GET /api/admin/products`** ditambahkan (di luar tabel PRD) untuk melihat produk nonaktif; `GET /api/products/{id}` publik tetap 404 untuk produk nonaktif.
