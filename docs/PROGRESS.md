# Progress

## Fase 1 (selesai sebelumnya)
CRUD produk dan kategori, Flyway V1, Docker, CI. 9 test.

## Fase 2: Keamanan (selesai)
- `V2__users`, register/login/me, JWT HS256 (resource server bawaan), aturan akses, 401/403 ProblemDetail, CORS via env, admin bootstrap, `GET /api/admin/products`.
- Bukti: `./mvnw -B verify` hijau, 22 test (gate: USER ke endpoint admin → 403, ADMIN → 200, tanpa token → 401).
- Terbuka: tidak ada.

## Fase 3: Keranjang dan order (selesai)
- `V3__cart_and_orders`, cart API, checkout transaksional dengan decrement stok atomik, state machine `OrderStatus`, order milik sendiri, admin list/patch status.
- Bukti: `./mvnw -B verify` hijau, 37 test. Gate: `concurrentCheckoutNeverOversells` (4 thread, stok 1 → 1x 201, 3x 409, stok akhir 0).
- Terbuka: pembatalan/pengembalian stok → Fase 4 (webhook) dan Fase 6.
