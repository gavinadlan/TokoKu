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

## Fase 4: Pembayaran (selesai)
- `V4__payments`, `PaymentGateway` (Midtrans Snap + Fake), `POST /api/orders/{id}/pay` idempoten, `POST /api/payments/webhook` (signature, idempoten, cek nominal, expire/deny mengembalikan stok), `/api/dev/payments/{ref}/simulate` (fake, non-prod), `scripts/simulate-midtrans-webhook.sh`.
- Bukti otomatis: `./mvnw -B verify` hijau, 56 test (10 integrasi pembayaran, 5 gateway Midtrans dengan server HTTP lokal, 4 ketersediaan endpoint dev).
- Dokumen yang diverifikasi: rumus signature `SHA512(order_id+status_code+gross_amount+ServerKey)`, Basic auth `base64(serverKey:)`, `POST /snap/v1/transactions` → `token` + `redirect_url`, `expiry{unit,duration}`, status notifikasi. Nilai uji signature dihitung independen (Python hashlib); dokumentasi tidak memuat contoh angka.
- **Verifikasi sandbox nyata (manual):** backend lokal + kunci dari `.env` (nilai tidak dicetak). Alur: login admin → buat produk → register customer → cart → checkout (order 1, Rp170.000) → `POST /api/orders/1/pay` → **HTTP 200**, Midtrans sandbox mengembalikan `token` (UUID 36 karakter) dan `redirect_url` berbentuk `https://app.sandbox.midtrans.com/snap/v4/redirection/<token>`.
- Webhook (notifikasi BUATAN ditandatangani server key dari `.env`, dikirim ke endpoint lokal): signature salah → 403; settlement → 200 dan order `PAID`; kirim ulang → 200 tanpa perubahan.
- **Belum terbukti:** pembayaran sandbox sungguhan dan webhook asli dari Midtrans (butuh tunnel publik dan menyelesaikan pembayaran di halaman Snap).

## Fase 5: Test dan dokumentasi (selesai)
- JaCoCo + gate 70% kelas service (aktual ±94%), OpenAPI Bearer + tag + deskripsi Indonesia, `DevDataSeeder` (kopi), test alur penuh register → PAID, README lengkap.
- Bukti: `./mvnw -B verify` hijau, 60 test, jacoco `check` lolos.
- CI: belum pernah berjalan di GitHub (tidak ada remote); `actionlint` tidak ada, diperiksa manual.

## Fase 6: Penyempurnaan (selesai, butir 7 dilewati)
1. Pembatalan order oleh pemilik. 2. Kedaluwarsa: `@Scheduled` + pengecekan malas. 3. Refresh token (hash, rotasi, deteksi pemakaian ulang), logout, change-password. 4. Kategori PUT/DELETE (409 bila ada produk). 5. `image_url`. 6. Rate limit login Bucket4j (429).
7. **Cache Redis: dilewati** (lihat DECISIONS).
- Bukti: `./mvnw -B verify` hijau, 80 test, gate JaCoCo lolos. Test konkuren: cancel vs expiry mengembalikan stok tepat sekali.
