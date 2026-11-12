# Palet TokoKu

Satu sumber: `src/main/resources/theme/tema.json` (varian `base`, `a`, `b`, `c`; field `aktif` menentukan varian produksi).

- `node scripts/palet.mjs` memeriksa kontras WCAG AA tiap varian, lalu menulis `frontend/src/styles/themes.css` dan `frontend/public/favicon.svg`.
- Backend (email dan invoice PDF) membaca berkas yang sama. `APP_THEME=a` (atau `-Dapp.theme=a`) mencoba varian tanpa mengubah `aktif`.
- Di browser, `document.documentElement.dataset.palet = 'c'` mengganti varian untuk pratinjau.
- `node scripts/palet-shots.mjs [varian...]` mengambil tangkapan layar ke `img/` (butuh backend dan frontend dev serta data demo portofolio).
- `node scripts/palet-axe.mjs` memindai axe tiap varian. `node scripts/palet.mjs --page` membangun `index.html` dari `rekomendasi.html`.

Memilih varian permanen: ubah `aktif`, jalankan `node scripts/palet.mjs`, commit.
