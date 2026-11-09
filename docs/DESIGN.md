# DESIGN: TokoKu (frontend)

Status: **draf untuk disetujui**. Belum ada kode aplikasi. Mock statis ada di `docs/design/mock/` (`catalog.html`, `product.html`), screenshot di `docs/design/` (`catalog-375.png`, `catalog-1280.png`, `product-375.png`, `product-375-full.png`, `product-1280.png`).

Design Read: toko online kopi dan perlengkapan seduh untuk pembeli Indonesia di ponsel, bahasa visual minimalis hangat bertipografi editorial, dial **ENERGY 2 / RHYTHM 2 / MOTION 1**.

## 1. Audiens, nada, satu ide visual

**Audiens.** Pembeli Indonesia, kemungkinan besar membuka dari ponsel (satu tangan, jaringan seluler, sering lewat tautan yang dibagikan). Mereka datang dengan niat cukup jelas: mencari biji kopi atau alat seduh, membandingkan harga, lalu membeli. Admin (satu orang, pemilik toko) memakai layar lebar.

**Nada suara.** Tenang, spesifik, hangat, tidak menjual dengan berteriak. Bahasa Indonesia sehari-hari yang rapi: "Tambah ke keranjang", "Sisa 3", "Bayar sekarang". Tanpa seruan, tanpa kata sifat promosi, tanpa emoji. Pesan error menyebut apa yang gagal dan apa yang bisa dilakukan.

**Satu ide yang membedakan: label kemasan.** Toko kopi yang baik mencetak label di kantong: nomor, bingkai tipis, huruf bersih. TokoKu memakai bahasa itu sebagai satu-satunya motif dan mengulangnya dengan disiplin:

- Visual produk (sampai ada foto asli) adalah **label kemasan dirancang**: blok warna cokelat-netral, bingkai hairline di dalam, **nomor produk besar** bertipe serif tipis (diambil dari id produk, jadi nyata, bukan karangan), dan "No. 001" kecil di sudut. Foto asli nanti memakai bingkai tipis yang sama.
- Motif yang sama muncul di struk: ringkasan checkout dan detail order dirancang seperti struk (baris dengan garis tipis, total tabular), bukan kartu bersusun.
- Tidak ada ikon dekoratif; hierarki dibangun dari tipografi, hairline, dan jarak.

Alasan: katalog bertema kopi sudah punya budaya cetaknya sendiri, jadi motif ini berasal dari produknya (R-20), murah dirender, dan tetap jujur saat belum ada foto: placeholder tidak menyamar sebagai foto (R-23). Ganti nama dan logo dan tampilan ini tetap tidak akan terlihat seperti toko lain.

**Arti "minimalis modern" di sini (keputusan konkret):**
1. Satu serif berkontras tinggi dengan sumbu optik (judul besar, ringan, rapat) melawan satu sans netral yang sangat terbaca untuk isi dan harga: ketegangan antara "cetak" dan "fungsi".
2. Warna hampir seluruhnya netral hangat; satu aksen hijau tua dipakai untuk tindakan utama dan status berhasil saja.
3. Proporsi tetap: kartu 4:5, grid 4/3/2 kolom, satu lebar halaman 1200px. Konsistensi dibuat oleh skala token, bukan dekorasi.
4. Pemisah memakai garis 1px dan jarak, bukan bayangan. Radius 4 sampai 8px.
5. Satu hal terpenting per layar: katalog = produk, detail = tombol beli, checkout = total, order = status.

## 2. Warna

Netral hangat + **satu** aksen. Warna status memakai aksen (sukses) dan dua warna fungsional redup (peringatan, error) yang hanya muncul saat dibutuhkan.

| Token | Hex | Pemakaian | Kontras |
| --- | --- | --- | --- |
| `--paper` | `#F6F1E9` | latar halaman | dasar |
| `--surface` | `#FBF8F3` | input, stepper, panel terangkat tipis | dasar |
| `--ink` | `#1B1613` | teks utama, chip terpilih, penghitung keranjang | 15,95:1 di paper |
| `--ink-muted` | `#5F564D` | teks sekunder (kategori, label) | 6,39:1 di paper, 6,78:1 di surface |
| `--ink-subtle` | `#6B6055` | placeholder input, keterangan | 5,45:1 di paper |
| `--line` | `#D8CDBE` | hairline dekoratif (pemisah) | 1,39:1 (bukan satu-satunya penanda) |
| `--line-strong` | `#8F8478` | batas kontrol (input, chip, stepper) | 3,25:1 di paper (WCAG 1.4.11 butuh 3:1) |
| `--accent` | `#2E4B3C` | tombol utama, status berhasil, focus ring | 8,54:1 di paper; teks `--on-accent` di atasnya 8,54:1 |
| `--accent-hover` | `#243C30` | hover tombol utama | 10,59:1 dengan teks paper |
| `--ok-bg` / `--ok-ink` | `#DCE7DD` / `#24392D` | pesan sukses (mis. pembayaran diterima) | 9,73:1 |
| `--warn-bg` / `--warn-ink` | `#F1E3C4` / `#5E3C00` | menunggu, stok menipis | 7,79:1 |
| `--err` | `#A02A20` | error, "Habis" | 6,56:1 di paper |
| `--err-bg` / `--err-ink` | `#F3D9D3` / `#7E1F17` | pesan error blok | 7,48:1 |

Nada visual produk (netral cokelat, bukan warna tambahan; teks di dalamnya diuji): `#E9E0D2`, `#DACBB4`, `#C8B293` (teks ink: 13,72 / 11,26 / 8,75:1) dan `#7A624E`, `#5B4636`, `#3A2D25` (teks paper: 5,07 / 7,87 / 11,81:1). Nada dipilih berurutan dari nomor produk supaya kisi terasa berirama.

Aturan: aksen hanya untuk tombol utama, status berhasil, dan focus ring. **Harga tidak diberi warna aksen**: harga adalah angka dalam ink tebal (hierarki lewat ukuran dan bobot), sehingga hijau tetap berarti "tindakan atau berhasil". Chip terpilih memakai ink, bukan aksen. Tanpa gradien. Mode gelap tidak dikerjakan (token sudah semantik, jadi bisa ditambah nanti).

## 3. Tipografi

Dua keluarga, self-hosted lewat `@fontsource-variable/*` (tanpa CDN):

- **Fraunces** (variable, sumbu `opsz` dan `wght`): judul, nama produk, nomor besar, wordmark. Dipakai ringan (bobot 300 sampai 440) pada ukuran besar dengan `opsz` tinggi untuk kontras halus; nama produk 18px bobot 440. Alasan: serif berkarakter hangat dengan kait lembut cocok untuk kopi dan terasa cetak tanpa kuno.
- **Hanken Grotesk** (variable): isi, kontrol, harga, label. Alasan: sans netral dengan bentuk terbuka, sangat terbaca di ukuran kecil di layar ponsel, angka lining dengan lebar tabular (`font-variant-numeric: tabular-nums`) untuk harga dan jumlah.

Skala (rem, basis 16px): `xs` 0,8125 (label, 13px) · `sm` 0,875 · `base` 1 · `lg` 1,125 · `xl` 1,375 · `2xl` 1,75 · `3xl` 2,5 · `4xl` `clamp(2.25rem, 1.4rem + 3.6vw, 3.75rem)` (judul halaman). Tinggi baris: 1,04 sampai 1,25 untuk judul, 1,55 untuk isi. Teks isi tidak pernah di bawah 14px; label 13px hanya untuk keterangan sekunder.

Aturan: judul sentence case (tanpa huruf kapital semua); tanpa tracking lebar; angka uang selalu tabular dan berformat `Rp 85.000` (`Intl.NumberFormat('id-ID')`, spasi tak terputus); tanggal `id-ID`; panjang baris isi maksimal sekitar 46 karakter di panel produk.

## 4. Jarak, radius, border, bayangan

- **Jarak** kelipatan 4: `4, 8, 12, 16, 24, 32, 48, 64, 96`. Gutter halaman 16px (ponsel) dan 32px (≥768px); lebar konten maksimum 1200px.
- **Radius:** 4px (bingkai dalam, fokus), 6px (tombol, input, chip), 8px (visual produk). Tidak ada pil.
- **Border:** 1px. `--line` untuk pemisah, `--line-strong` untuk batas kontrol.
- **Bayangan:** hanya satu, `0 8px 24px rgb(27 22 19 / 0.12)`, khusus panel yang melayang di atas halaman (menu akun, dialog). Kartu, tombol, dan panel biasa datar.
- **Gerak:** hover dan fokus 150ms; bingkai label melebar 2px saat hover kartu; penghitung keranjang berubah dengan jeda singkat saat item masuk; pengumuman `aria-live`. `prefers-reduced-motion` mematikan semuanya.
- **Area sentuh:** minimal 44px (chip, tombol, stepper, input).

## 5. Inventaris komponen dan tata letak halaman

**Komponen:** `Wordmark` (teks, **placeholder, mohon konfirmasi**), `Header` (wordmark, pencarian, Masuk/Akun, Keranjang + jumlah), `SearchField`, `CategoryChips`, `SortSelect`, `ProductCard`, `ProductVisual` (label kemasan, atau foto bila `imageUrl`), `Price`, `StockNote` (teks "Sisa 3" / "Habis", bukan stiker), `Stepper`, `Button` (utama, ghost, disabled), `Field` (label, kontrol, pesan error), `OrderStatusLabel` (teks berwarna + penanda, bukan hanya warna), `Receipt` (ringkasan gaya struk), `Skeleton`, `EmptyState`, `ErrorState`, `Toast/LiveRegion`, `Dialog` (konfirmasi batal), `DataTable` (admin), `Pagination`/muat lebih banyak.

**Katalog `/`.** Header lengket 64px (ponsel: baris pencarian di bawah). Judul halaman besar rata kiri ("Kopi dan *perlengkapan* seduh") dengan jumlah produk dan urutan di sisi kanan (desktop). Chip kategori satu baris (ponsel: geser horizontal, desktop: membungkus). Grid 2/3/4 kolom, kartu 4:5, nama (serif), kategori kecil di atas nama, harga tabular; "Sisa N" kuning-tua bila ≤5, "Habis" merah dan visual dipudarkan bila 0. Tanpa banner, tanpa carousel. Semua state (q, kategori, urutan, halaman) di query string. Loading: skeleton berbentuk kartu. Kosong: "Tidak ada produk untuk 'xyz'" + tombol "Hapus filter". Error: pesan + "Coba lagi".

**Detail `/produk/:id`.** Jejak halaman. Desktop: kiri visual 4:5 besar, kanan panel lengket (kategori, nama serif besar, harga, deskripsi, ketersediaan, jumlah, tombol "Tambah ke keranjang", daftar fakta bergaris tipis: nomor produk, kategori, ketersediaan). Ponsel: visual 1:1, lalu info, dan **bilah beli lengket di bawah** (total + tombol). Produk habis: tombol diganti keterangan "Stok habis" (non-aktif, terbaca), bukan tombol yang gagal diam-diam. Produk nonaktif/tidak ada: 404 yang dirancang.

**Keranjang `/keranjang`.** Halaman bersih (bukan drawer): alasan, URL sendiri, tombol back bekerja, mudah di ponsel. Baris item bergaris tipis (visual kecil, nama, stepper, subtotal, hapus), ringkasan gaya struk di kanan (desktop) atau bawah (ponsel) dengan total dan tombol "Lanjut ke checkout". Optimistic update; stok kurang menampilkan pesan di baris itu. Kosong: arahan "Belum ada isi. Lihat katalog".

**Auth `/masuk`, `/daftar`.** Satu kolom sempit, label di atas kolom, error per kolom di bawahnya, password dengan tombol tampil/sembunyi. Setelah masuk kembali ke halaman asal.

**Checkout `/checkout`.** Satu halaman: kiri formulir pengiriman (nama, telepon, alamat), kanan struk ringkasan. Satu tombol "Buat pesanan". Error stok kembali ke keranjang dengan penjelasan.

**Pembayaran.** Setelah order dibuat, tombol "Bayar sekarang" mengarahkan ke `redirect_url` Midtrans (alur terpendek, tanpa memuat Snap.js; alasan di README). Halaman `/order/:id/status` memeriksa status berkala (maks. beberapa menit) dengan tiga pesan: berhasil, menunggu (+ tombol "Periksa status"), gagal atau kedaluwarsa (+ "Coba bayar lagi"/"Lihat order").

**Order `/order`, `/order/:id`.** Daftar bergaris tipis (nomor order, tanggal `id-ID`, total, label status). Detail seperti struk: item, total, alamat, status; tombol "Bayar" bila `PENDING_PAYMENT`, "Batalkan pesanan" (dialog konfirmasi) bila diizinkan.

**Admin `/admin/*`** (hanya ADMIN, tata letak terpisah dengan navigasi samping ringkas): Produk (tabel termasuk nonaktif, filter, buat/ubah dengan `imageUrl` dan stok, nonaktifkan), Kategori (tambah, ubah nama, hapus dengan penjelasan 409), Order (filter status, tombol aksi hanya transisi yang sah: Proses, Kirim, Selesai). Tata letak dibangun di sekitar keputusan admin (order mana yang perlu tindakan), bukan templat dasbor berisi kartu statistik; tanpa angka karangan.

**404 dan error global.** Halaman berbahasa Indonesia dengan jalan keluar ("Kembali ke katalog"), memakai bingkai label dan nomor "404".

## 6. Mock dan bukti

`docs/design/mock/catalog.html` dan `product.html` memakai nama dan harga dari data seed backend. Screenshot 375px dan 1280px (Playwright) ada di `docs/design/`. Pemeriksaan: tidak ada overflow horizontal di keempat tangkapan.

## Alasan keputusan (R-31, satu baris)

- Palet netral hangat + hijau tua: cocok dengan kopi dan kemasan cetak; hijau berbeda tegas dari merah error.
- Fraunces + Hanken Grotesk: kontras cetak vs fungsi; Hanken terbaca di ponsel dan angka seragam lebarnya.
- Kartu 4:5: proporsi kantong kopi/label; konsisten membuat kisi tenang.
- Nomor produk sebagai visual: nyata (id), unik per produk, tidak menyamar sebagai foto.
- Tanpa ikon: tidak ada ikon yang membawa informasi yang tidak sudah ada di teks; keranjang cukup kata + angka.
- Keranjang halaman, bukan drawer: URL, back, dan ponsel lebih sederhana.
- Redirect ke Midtrans, bukan Snap.js: nol skrip pihak ketiga di aplikasi, satu alur untuk semua perangkat.

## Hal yang perlu keputusan Anda

1. **Wordmark "TokoKu"** saya set sebagai teks bertipe (placeholder). Konfirmasi atau beri logo sendiri (R-23).
2. **Foto produk:** sekarang tidak ada; placeholder label kemasan dipakai, dan `imageUrl` dari admin menggantikannya otomatis.
3. **Mode gelap** tidak dikerjakan di tahap ini.
4. **Nama lain "No."**: nomor produk memakai id database; jika Anda ingin kode SKU sendiri, backend perlu kolom baru (di luar lingkup).
