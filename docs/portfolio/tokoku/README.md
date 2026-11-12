# Screenshot portofolio TokoKu

Berkas PNG di folder ini dibuat otomatis oleh `scripts/portfolio-shots.sh` (Playwright, Chromium, tanpa bingkai browser, animasi dimatikan).

| Berkas | Isi | Ukuran |
| --- | --- | --- |
| `home.png` | beranda dan katalog (gambar utama portofolio) | 1600x1000 |
| `produk-galeri.png` | Dripper V60 dengan 4 foto dan thumbnail | 1600x1000 |
| `keranjang-checkout.png` | checkout, pilihan kirim atau ambil di toko | 1600x1000 |
| `admin-pesanan.png` | daftar pesanan admin, lencana "perlu diproses" | 1600x1000 |
| `admin-produk.png` | kelola produk dan foto | 1600x1000 |
| `email-struk.png` | pratinjau email struk (template asli, data disesuaikan dengan katalog demo) | 1600x1000 |
| `invoice.png` | PDF invoice order selesai, dirender ke PNG | 1600x1000 |
| `mobile-katalog.png`, `mobile-produk.png`, `mobile-checkout.png` | ponsel 390x844 @2x | 780x1688 |
| `mockup-gabungan.png` | laptop dan ponsel di atas krem `#F6F1E9` (bingkai generik) | 1600x1000 |

## Membuat ulang

```bash
scripts/portfolio-shots.sh           # ambil ulang gambar dari data yang sekarang
scripts/portfolio-shots.sh --seed    # isi data demo dulu (hanya pada DB yang sudah dirapikan, lihat bawah)
```

Skrip menyalakan backend (profil dev, email mati, pembayaran fake, foto disimpan lokal) dan frontend bila belum hidup. Butuh `.env`, `.dev-admin.env`, Docker, dan `pngquant` (opsional, untuk mengecilkan PNG). Invoice dirender dengan `qlmanage` (macOS). Desktop diambil 1440x900 @2x lalu diperkecil ke 1600x1000 dengan `sips`.

Foto produk tidak ada di repo (lisensi Unsplash tidak menyinggung penyimpanan di repositori): unduh dari tautan di `../foto-produk/CREDITS.md` dengan nama berkas yang sama sebelum `--seed`.

## Data demo (`scripts/portfolio-seed.mjs`)

- Toko: TokoKu, Jl. Braga No. 42 Bandung, 022-5550-1234, halo@tokoku.test, Senin sampai Sabtu 09.00 sampai 17.00; ongkir Rp 15.000, gratis di atas Rp 500.000.
- 10 produk aktif di 4 kategori, dibuat dalam urutan khusus karena sort "Terbaru" memakai id: baris pertama beranda = Gayo, V60, grinder, mug (logika sort aplikasi tidak diubah). Gayo, V60, grinder, mug, dan timbangan punya 2 sampai 4 foto.
- 6 pembeli `@surel.test` dan 12 pesanan selama 7 hari: 3 selesai, 2 dikirim, 1 siap diambil, 1 diproses, 3 dibayar (perlu diproses), 2 menunggu pembayaran; campuran ambil di toko dan kirim.
- Admin demo "Rina Wijaya" (`rina@tokoku.test`; seed mengganti email di DB dan `.dev-admin.env`, password tetap di `.dev-admin.env`).
- Kata sandi pembeli demo ada di `.demo-credentials` di folder ini (di-gitignore, tidak ikut commit).
