# TokoKu frontend

Toko online untuk API TokoKu: katalog dengan foto, keranjang, checkout (kirim atau ambil di toko), pembayaran Midtrans, linimasa order, invoice PDF, dan panel admin terpisah.
Vite + React 19 + TypeScript (strict), React Router 8, TanStack Query, React Hook Form + Zod. Arah visual dan token: [`../docs/DESIGN.md`](../docs/DESIGN.md).

## Menjalankan

Butuh Node 22 atau lebih baru dan backend yang berjalan.

```bash
# terminal 1: backend (Postgres Docker, profil dev + data seed kopi, provider pembayaran fake, CORS untuk :5173)
scripts/dev-backend.sh            # dari root repo; tambah --reset untuk menghapus data database

# terminal 2: frontend
cd frontend
npm install
cp .env.example .env              # opsional; default VITE_API_URL=http://localhost:8080
npm run dev                       # http://localhost:5173
```

Admin lokal: `scripts/dev-backend.sh` membuat `admin@tokoku.local` dengan password acak yang disimpan di `.dev-admin.env` (di-gitignore). Masuk di `http://localhost:5173/admin/masuk`. Tanpa kunci Cloudinary skrip memakai penyimpanan foto `fake` (unggahan diterima server dev).

## Skrip

| Perintah | Fungsi |
| --- | --- |
| `npm run lint` / `typecheck` / `test` / `build` | gerbang kualitas (CI menjalankan keempatnya) |
| `npm run e2e` | Playwright terhadap backend lokal (jalankan otomatis bila belum hidup), termasuk pemeriksaan axe |
| `npm run shots` | screenshot 375/768/1280 ke `docs/design/screens/` |
| `npm run fetch:api` | ambil `/v3/api-docs` dari backend lalu bangkitkan ulang tipe (`src/api/schema.d.ts`) |
| `npm run gen:api` | bangkitkan tipe dari `openapi.json` yang sudah ada |

## Environment variable

| Nama | Default | Keterangan |
| --- | --- | --- |
| `VITE_API_URL` | `http://localhost:8080` | URL backend, tanpa slash di akhir. Dibaca saat **build** (nilai ditanam ke bundel). |

Backend harus mengizinkan origin frontend lewat `APP_CORS_ALLOWED_ORIGINS` (mis. `http://localhost:5173` di lokal).

## Struktur folder

```
src/
  api/        client openapi-fetch bertipe (schema.d.ts dibangkitkan), query dan mutation TanStack Query
  auth/       session (token), AuthContext, keputusan akses rute (guard.ts)
  components/ Header, ProductCard, ProductVisual, Receipt, Field, ConfirmDialog, Toast, state kosong/error/skeleton
  lib/        logika murni yang diuji: format rupiah/tanggal, hitung keranjang, parameter katalog, status order, error
  pages/      halaman pelanggan; pages/admin/ untuk panel admin (shell, sesi, dan kode terpisah, dimuat malas)
  styles/     tokens.css (design token) dan app.css
e2e/          Playwright: alur belanja, admin, aksesibilitas (axe), screenshot
```

## Keputusan penting

- **Tipe dari OpenAPI, bukan tulis tangan.** `openapi-typescript` membaca `openapi.json` (diambil dari backend); `openapi-fetch` memberi klien yang tipenya mengikuti path, parameter, dan body. Agar tipe akurat, backend diubah sedikit: field respons ditandai `required` dan parameter paginasi didokumentasikan datar (`page`, `size`, `sort`).
- **Auth: access token di memori, refresh token di `localStorage`.** Saat API mengembalikan 401, klien menukar refresh token sekali (single-flight, karena token berotasi dan backend menganggap pemakaian ulang sebagai pencurian) lalu mengulang request. *Trade-off:* token di `localStorage` terbaca oleh skrip yang berjalan di halaman, jadi celah XSS bisa mencurinya. Alternatif yang lebih aman (cookie `HttpOnly` + `SameSite`) butuh perubahan backend (cookie, bukan JSON). Mitigasi yang dipakai: tidak ada `dangerouslySetInnerHTML`, semua teks di-escape React, tidak ada skrip pihak ketiga, access token hidup 30 menit, refresh token berotasi dan dicabut saat logout atau ganti password. Untuk produksi, pasang header `Content-Security-Policy` ketat di host statis.
- **Pembayaran: redirect penuh ke `redirect_url` Midtrans** (bukan Snap.js). Paling sederhana: nol skrip pihak ketiga dan satu alur untuk semua perangkat. Setelah bayar Midtrans mengembalikan pengguna ke "finish URL" yang diatur di dashboard; aplikasi menyediakan `/pembayaran/selesai` (membaca `order_id`) dan `/order/:id/status`.
- **Status pembayaran diperiksa berkala (4 detik) selama `PENDING_PAYMENT`.** Status berubah lewat webhook Midtrans. Di lokal Midtrans tidak bisa menjangkau `localhost`, sehingga status bisa tidak berubah sendiri: tombol **Periksa status** memeriksa ulang. Untuk menguji webhook di lokal: `scripts/simulate-midtrans-webhook.sh` (root repo) atau tunnel publik.
- **Simulasi pembayaran (`/dev/bayar/:ref`)** hanya terdaftar ketika `import.meta.env.DEV` bernilai true (dev server), dan hanya dipakai bila backend memakai provider `fake`. Build produksi tidak memuatnya.
- **State di URL:** pencarian, kategori, urutan, dan halaman katalog ada di query string (`?q=&kategori=&urut=&halaman=`).
- **Keranjang:** halaman penuh `/keranjang`; ubah jumlah dan hapus memakai optimistic update dengan rollback bila server menolak (stok kurang).
- **Dua shell dan dua sesi.** Toko di `/` dan admin di `/admin/*` (login `/admin/masuk`, wordmark "TokoKu Admin", rel samping gelap) tidak berbagi header, konteks autentikasi, penyimpanan, atau klien API (`api`/`adminApi`). Refresh toko di `localStorage`, refresh admin di `sessionStorage` (hilang saat tab ditutup; lebih sensitif), access token keduanya di memori, sehingga satu browser bisa masuk sebagai pembeli dan admin sekaligus. Rute admin tanpa sesi admin mengarah ke login admin (bukan 403); header toko tidak pernah memuat tautan admin dan halaman login toko tidak menyebut admin; halaman admin `noindex` (meta dan `robots.txt`); kode admin dimuat malas sehingga pembeli tidak mengunduhnya.
- **Ongkir di UI hanya perkiraan** (`lib/shipping.ts`, aturan sama dengan server); angka yang berlaku selalu dari respons order. Pilihan pengambilan memakai radio kartu; ambil di toko menyembunyikan alamat dan menampilkan info toko dari `GET /api/store`.
- **Invoice:** endpoint butuh Bearer, jadi PDF diambil dengan `fetch` terautentikasi lalu dibuka/diunduh sebagai blob (tab dibuka sinkron dalam gestur klik agar tidak diblokir pop-up blocker).
- **Foto:** unggah langsung ke Cloudinary lewat XHR (agar ada progres), diproses satu per satu; validasi klien sebelum unggah (format, 5 MB, maks. 6). Urutan lewat tombol Kiri/Kanan (aksesibel keyboard). Galeri memakai `srcset` tiga ukuran dan navigasi panah.
- **Kontrol:** select kustom Radix di desktop dan `<select>` native di perangkat sentuh (`pointer: coarse`); menu akun dan menu aksi baris memakai dropdown Radix; dialog tetap `<dialog>` bawaan; toast `aria-live` yang dijeda saat disorot; input harga berformat rupiah; fokus dipindah ke judul setelah pindah halaman.
- **Visual produk:** placeholder "label kemasan" (nomor produk besar dari id) dengan nada netral per kategori, diganti foto bila admin mengisi `imageUrl`.
- **Aksesibilitas:** HTML semantik, label terhubung ke kolom, focus ring, `aria-live` untuk toast dan jumlah, `<dialog>` bawaan untuk konfirmasi, `prefers-reduced-motion` dihormati, dan axe tanpa pelanggaran serious/critical pada halaman utama (di e2e).
- **Versi:** TypeScript 5.9 (bukan rilis terbaru) karena `openapi-typescript` mensyaratkan `typescript@^5`.

## Deploy sebagai Render Static Site (jangan dijalankan sebelum siap)

1. Render: **New → Static Site**, hubungkan repo, **Root Directory** `frontend`.
2. **Build Command:** `npm ci && npm run build`. **Publish Directory:** `dist`.
3. **Environment:** `VITE_API_URL=https://<nama-backend>.onrender.com` (nilai ini ditanam saat build; ubah lalu deploy ulang bila backend pindah).
4. **Redirects/Rewrites:** tambah aturan **Rewrite** `/*` → `/index.html` (agar `/produk/1`, `/keranjang`, dst. tidak 404 saat dibuka langsung). Berkas `public/_redirects` hanya berlaku di Netlify; di Render atur di dashboard (atau `render.yaml`).
5. **Backend:** isi `APP_CORS_ALLOWED_ORIGINS=https://<nama-frontend>.onrender.com` (tanpa slash akhir) di service backend.
6. **Midtrans:** di dashboard, atur *Finish Redirect URL* ke `https://<nama-frontend>.onrender.com/pembayaran/selesai` dan *Notification URL* ke `https://<nama-backend>.onrender.com/api/payments/webhook`.
7. CI untuk frontend (`.github/workflows/ci.yml`, job `frontend`) **belum pernah berjalan di GitHub**.
