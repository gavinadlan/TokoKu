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

---

# Putaran 2 (addendum, draf untuk disetujui)

Mock statis di `docs/design/round2/*.html`, screenshot 375 dan 1280 di `docs/design/round2/*.png` (invoice memakai lebar A4 794px; di 375 diperkecil proporsional). Data produk dan harga dari data seed; nama pembeli, nomor order, dan identitas toko adalah **data contoh** (diberi catatan di tiap mock). Foto produk belum ada, jadi mock memakai placeholder label kemasan.

## R2-1. Shell admin terpisah dari toko

Tujuan: terasa seperti alat kerja yang tenang dan padat, tetapi jelas "dapur", bukan etalase.
- **Pembeda dari toko (keputusan konkret):** rel samping gelap (`#1B1613`) dengan teks paper, bukan header terang; isi di latar `--surface` (lebih terang dari paper toko); judul halaman serif ringan 28px (bukan 60px); isi 14px; tabel padat (baris 48px); tanpa kartu produk, tanpa placeholder besar. Wordmark "TokoKu *Admin*" dengan kata Admin miring redup.
- **Bahasa yang sama:** token warna, Fraunces untuk judul, Hanken Grotesk untuk isi, aksen hijau yang sama untuk tombol utama, radius 4/6/8, hairline.
- **Login admin** (`/admin/masuk`): dua panel. Kiri gelap (wordmark, satu kalimat "Khusus pengelola toko", bingkai label kosong sebagai motif), kanan form sempit. Salinan tegas: akun pembeli tidak bisa masuk di sini. Halaman login toko tidak menyebut admin sama sekali.
- **Navigasi:** desktop rel kiri (Produk, Order, Pengaturan toko; email admin dan Keluar di bawah); ponsel jadi baris tab horizontal di bawah wordmark. Tidak ada tautan ke toko di header toko menuju admin, dan sebaliknya hanya lewat alamat.
- **Tabel:** header menempel, kolom bisa diurutkan (`aria-sort`), kolom nomor menempel di ponsel, aksi baris dalam menu "Aksi" (bukan deretan tautan), status sebagai titik + teks, chip kecil "Kirim"/"Ambil" di order, panel tindakan di bawah tabel untuk mengisi resi dan memverifikasi kode ambil.

## R2-2. Pilihan pengambilan di checkout

Kontrol segmen dua kartu bergaris (radio sungguhan, bisa dipilih keyboard) di atas formulir: **Kirim ke alamat** (biaya kirim dan ambang gratis ongkir dari pengaturan toko) dan **Ambil di toko** (Gratis). Terpilih ditandai garis 2px ink + radio terisi, tidak hanya warna. Saat "Ambil di toko": formulir hanya nama pengambil dan telepon, plus kotak info toko bergaya label (alamat, jam buka, telepon, petunjuk). Ringkasan struk memisahkan **Subtotal**, **Biaya kirim** (atau "Ambil di toko: Gratis"), lalu **Total**; satu kalimat bantuan ("Tambah Rp 123.000 lagi untuk gratis ongkir") hanya bila ambang dikonfigurasi. Angka yang dikirim ke Midtrans selalu dari server.

## R2-3. Linimasa status order (dua jalur)

Linimasa vertikal dengan rel tipis: langkah selesai berdot ink, langkah sekarang berdot hijau dengan lingkar lembut, langkah berikutnya redup. Waktu tabular di kanan. **Kirim:** Pesanan dibuat, Dibayar, Diproses, Dikirim (blok label: kurir, nomor resi, tombol "Salin nomor resi"), Selesai. **Ambil:** Pesanan dibuat, Dibayar, Diproses, **Siap diambil** (blok label berisi **kode ambil** besar bergaya serif berjarak, plus lokasi dan jam buka), Selesai. Order dibatalkan tidak punya linimasa lanjutan, hanya pesan blok merah dengan alasan. Kode ambil hanya muncul untuk pemilik setelah order dibayar.

## R2-4. Invoice A4

Kertas paper dengan bingkai hairline 18px dari tepi (gema label kemasan, satu-satunya motif). Kiri atas identitas toko (wordmark serif, alamat, telepon, email); kanan atas "Invoice" serif ringan 44px, nomor `INV-YYYYMM-NNNNN` tabular, dan tanda **Lunas**: kotak berbingkai ganda hairline hijau dengan "Lunas" miring serif dan "dibayar 10 November 2026" kecil, tanpa rotasi, tanpa merah, tanpa cap. Garis ink tegas memisahkan kepala. Baris meta empat kolom (nomor order, tanggal terbit, tanggal bayar, metode pembayaran), dua blok pihak ("Ditagihkan kepada" dan "Dikirim ke" dengan kurir dan resi, atau "Pengambilan di toko" dengan lokasi, jam buka, dan kode ambil), tabel item (No., Deskripsi + kategori kecil, Jml, Harga satuan, Jumlah), ringkasan Subtotal / Biaya kirim (atau Ambil di toko: Gratis) / **Total** besar, baris status dan metode pembayaran, lalu catatan kaki yang diatur lewat konfigurasi. **Pajak dan NPWP/NIB hanya tampil bila dikonfigurasi**; tidak ada aturan pajak karangan. Dua varian: `invoice-delivery` dan `invoice-pickup`.
- **Catatan teknis yang memengaruhi desain:** PDF dirender backend (openhtmltopdf) yang tidak membaca font variabel/WOFF2. Font toko (Fraunces, Hanken Grotesk, lisensi OFL) akan dibuat sebagai **instans statis TTF** lewat fontTools dan disertakan di jar; fitur OpenType lanjutan tidak tersedia, tetapi angka Hanken sudah selebar tabular. Lebih dari satu halaman menambah halaman A4 dengan kepala tabel berulang.

## R2-5. Pengelola foto dan galeri

- **Admin:** panel "Foto produk (3 dari 6)" dengan petunjuk format dan batas (jpg, png, webp, 5 MB). Grid kotak 4:5; tiap kotak: tag "Utama" pada foto utama, tombol teks **Kiri / Kanan** (urutan lewat tombol agar aksesibel keyboard, tanpa seret-lepas wajib), **Jadikan utama**, **Hapus** (konfirmasi dialog). State: mengunggah (bar progres 3px + persen, `role=progressbar`, bisa dibatalkan), gagal (kotak merah dengan alasan dan "Pilih file lain"), kosong ("Tambah foto"), batas 6 tercapai (tombol tambah hilang dengan keterangan).
- **Galeri toko:** panggung besar (1:1 ponsel, 4:5 desktop) + deret thumbnail (vertikal di kiri pada desktop, horizontal di ponsel), thumbnail aktif diberi bingkai ink, tombol sebelumnya/berikutnya 44px dengan penghitung "2 / 4" (`aria-live`), panah kiri/kanan di keyboard. Gambar memakai `srcset` ukuran thumbnail, kartu, dan detail. Tanpa foto: placeholder label kemasan seperti sekarang.

## R2-6. Lembar komponen (`components.html`)

Setiap komponen ditampilkan normal, hover, fokus, nonaktif, dan loading bila relevan.
- **Select/dropdown kustom:** Radix Select (headless; panah, Home/End, ketik huruf awal, Escape). Di perangkat sentuh (`pointer: coarse`) dipakai `<select>` native bergaya yang sama, karena pemilih bawaan ponsel lebih nyaman dan sudah aksesibel.
- **Menu akun:** dropdown Radix (Enter membuka, panah memilih, Escape menutup dan mengembalikan fokus).
- **Toast:** tetap komponen sendiri dengan `aria-live`, tiga varian (berhasil dengan aksi, error, peringatan), hilang 5 detik dan dijeda saat disorot/difokus.
- **Dialog konfirmasi:** tetap `<dialog>` bawaan (fokus terkurung, Escape menutup, fokus kembali ke pemicu); tombol berbahaya berkata kerja spesifik ("Hapus foto"); state loading mematikan kedua tombol.
- **Input harga:** awalan "Rp", pemisah ribuan saat mengetik, rata kanan, angka tabular, hanya digit; nilai tersimpan bulat.
- **Stepper:** 44px, batas stok dijelaskan dengan teks.
- **Tombol:** normal, hover, fokus, nonaktif, loading, sekunder, bahaya.
- **Tabel admin:** header menempel, urut kolom, menu "Aksi".
- **Loading/fokus halaman:** skeleton, progres tipis, fokus dipindah ke judul setelah navigasi, judul dokumen per halaman.

## Alasan keputusan Putaran 2 (R-31, satu baris)
- Rel gelap di admin: sinyal visual instan "ini ruang kerja"; sisanya sama agar merek konsisten.
- Radio kartu di checkout: dua pilihan saling meniadakan, terbaca jelas di ponsel, aksesibel bawaan.
- Kode ambil serif besar berbingkai: satu-satunya hal yang pembeli tunjukkan di kasir, jadi ia jadi titik fokus halaman.
- Invoice bertipografi sama dengan toko tanpa dekorasi: dokumen resmi harus tenang; bingkai tipis dan "Lunas" berbingkai ganda cukup sebagai identitas.
- Urutan foto dengan tombol Kiri/Kanan: aksesibel keyboard dan sederhana; seret-lepas bisa menyusul.
- Instans TTF statis untuk PDF: satu-satunya cara font merek ikut masuk PDF dengan pustaka ringan.

## Keputusan yang saya butuhkan dari Anda
1. **Identitas toko di dev** memakai data contoh (`Jl. Contoh No. 1, Kota Anda`, `0800-0000-0000`, `halo@tokoku.example`). Anda mengisi yang asli di Pengaturan toko nanti. Setuju?
2. **Biaya kirim dan ambang** pada contoh: Rp 15.000 flat, gratis di atas Rp 500.000 (nilai awal dev yang bisa diubah di pengaturan).
3. **Kode ambil:** 6 karakter dari huruf dan angka tanpa karakter membingungkan (tanpa 0/O, 1/I/L). Setuju?
4. **Urutan foto** lewat tombol Kiri/Kanan (bukan seret-lepas). Setuju?
5. **Select kustom** di desktop dan native di sentuh, seperti dijelaskan di R2-6. Setuju?

---

# Putaran 3: galeri foto di detail produk

Masalah yang diperbaiki: foto terasa kecil saat produk dibuka, dan produk dengan banyak foto tidak nyaman dilihat. Screenshot sebelum dan sesudah (desktop 1440×900, ponsel 390×844, produk 1 foto dan 5 foto, plus lightbox) ada di `docs/design/round3/`, dibuat oleh `frontend/e2e/round3-shots.spec.ts` dengan foto contoh yang dirender di tempat (bukan foto produk asli). Putaran ini menggantikan butir "Galeri toko" di R2-5.

## R3-1. Ukuran dan tata letak

- **Rasio 4:5 di semua lebar**, sama dengan kartu katalog dan dengan potongan Cloudinary (`c_fill,ar_4:5`). Rasio 1:1 di ponsel dari Putaran 2 dihapus, karena foto 4:5 dari server terpotong lagi di kotak 1:1.
- **Ponsel (< 640px):** foto selebar layar penuh tanpa gutter dan tanpa radius, 390×488 di layar 390 (sebelumnya 358×358). Lebar 640 sampai 1023px: galeri dibatasi 560px.
- **Desktop (≥ 1024px):** dua kolom, keduanya lengket. Dengan lebih dari satu foto, kolom thumbnail vertikal (72px + jarak 12px) berada di kiri foto utama, setinggi foto utama; kolom info mengisi sisanya. `--gw` adalah lebar foto utama, `--th` kolom thumbnail (0 untuk satu foto, 84px untuk banyak foto): `--gw = min(share × (konten − jarak − th), (100dvh − header − 56px) × 0,8)`, `share` 0,58 untuk satu foto dan 0,6 untuk banyak foto. Angka 56px = jarak 48px ditambah 8px untuk breadcrumb, supaya foto 4:5 selesai di atas tepi bawah layar.
- **Ukuran terukur di 1440×900** (Playwright `boundingBox`, px CSS, dicetak oleh `round3-shots.spec.ts` sebagai `UKURAN`; spec gagal bila foto utama banyak foto < 620px):

  | | foto utama | thumbnail | kolom info |
  |---|---|---|---|
  | 1 foto | x 120, y 118,7, **624 × 780** | tidak ada | x 792, lebar 528 |
  | 5 foto | x 204, y 118,7, **624 × 780** | kolom x 120, 72 × 780; tiap thumbnail 64 × 80, yang pertama di x 124, y 122,7 | x 876, lebar 444 |

  Foto utama berakhir di y 898,7 (≤ 900), jadi tidak terpotong lipatan. Sebelumnya (thumbnail di bawah) 630 × 788 yang berakhir di y 907; pada percobaan pertama thumbnail ke samping dengan `share` 0,58 foto utama menyusut ke 584 × 730, lebih kecil dari sebelumnya, jadi `share` naik ke 0,6 dan batas tinggi dikoreksi. Harga yang dibayar: kolom info 444px (sebelumnya 484px).
- **Kompromi yang disadari:** panah dan penanda "1 / 5" berada di dalam foto utama dan tetap terlihat; seluruh thumbnail terlihat tanpa menggulir. Bila produk punya lebih banyak thumbnail daripada tinggi foto (layar pendek, 6 foto di 1100×600), kolom menggulir sendiri dan mengikuti foto aktif; halaman tidak ikut bergulir.
- **Tanpa layout shift:** kotak 4:5 memakai `aspect-ratio`, gambar diposisikan absolut di dalamnya, atribut `width`/`height` diisi, dan latar memakai nada `--tone-1` selama gambar dimuat.
- **`object-fit: cover`** di panggung dan thumbnail (potongan tengah, sama dengan kartu). Lightbox memakai `contain` supaya gambar tidak dipotong lagi.

## R3-2. Slider

- **CSS scroll-snap**, tanpa pustaka carousel: lintasan `overflow-x: auto` dengan `scroll-snap-type: x mandatory`, satu foto per langkah. Geser dengan jari atau trackpad berjalan bawaan.
- **Tombol sebelumnya/berikutnya** hanya di desktop: 44px, hairline, latar paper tembus, opasitas 0,72 dan penuh saat panggung disorot atau tombol difokus. Di ujung ditandai `aria-disabled` (bukan `disabled`, supaya fokus keyboard tidak hilang) dan dipudarkan. Tidak berputar dari foto terakhir ke pertama.
- **Thumbnail** di kolom vertikal sebelah kiri panggung (desktop): 64×80, foto aktif diberi bingkai ink 2px dan `aria-current`, yang lain sedikit dipudarkan. Di ponsel thumbnail dan panah disembunyikan; cukup penanda "2 / 5".
- **Penanda posisi** "2 / 5" di sudut kanan bawah foto, `output` dengan `aria-live`.
- **Keyboard:** panah kiri/kanan saat fokus berada di dalam galeri atau lightbox.
- **Pindah lewat tombol, thumbnail, dan keyboard tidak dianimasikan** (MOTION 1); geser dengan jari tetap mengikuti gerak bawaan. Ini juga membuat `prefers-reduced-motion` terpenuhi tanpa cabang khusus.
- **Urutan:** foto utama selalu pertama, sisanya mengikuti urutan admin.
- **Satu foto:** tanpa panah, thumbnail, dan penanda. **Tanpa foto:** placeholder label kemasan di kotak dan posisi yang sama.

## R3-3. Lightbox

Klik foto membuka tampilan layar penuh di atas ink, dengan tombol "Tutup" (teks, bukan ikon), panah, dan penanda posisi. Memakai **`<dialog>` bawaan** seperti `ConfirmDialog`: fokus terkurung, Escape menutup, fokus kembali ke foto pemicu. Klik di luar gambar juga menutup. Radix Dialog tidak ada di project, jadi tidak ditambahkan. Foto tampil utuh dengan rasio aslinya (tegak maupun mendatar), tidak pernah lebih besar dari layar. Cubit dua jari memperbesar sampai 4×, geser saat diperbesar, ketuk dua kali memperbesar 2,5× atau mengembalikan; dikerjakan `ZoomImage.tsx` dengan Pointer Events tanpa pustaka. Navigasi di dalam lightbox tidak menggeser galeri di belakangnya, sehingga fokus kembali tepat ke foto yang tadi diklik.

## R3-4. Ukuran gambar

- Ukuran bernama keempat di backend: **`LARGE` 1600px** (`f_auto,q_auto,c_limit,w_1600,h_1600`, tanpa potong 4:5, rasio asli), dikirim sebagai `largeUrl`. Dipakai lightbox dan layar rapat piksel (panggung 624 sampai 668px butuh lebih dari 960px di layar 2x).
- `srcset` panggung: 160w, 480w, 960w, 1600w dengan `sizes="(min-width: 1024px) min(56vw, 670px), 100vw"`. Lightbox: 480w, 960w, 1600w dengan `sizes="(min-aspect-ratio: 4/5) 80vh, 100vw"`.
- Foto pertama `loading="eager"` dan `fetchpriority="high"`; foto lain `loading="lazy"`. Gambar lightbox baru diminta saat dibuka.

## R3-5. Aksesibilitas

`alt` berisi nama produk dan posisi ("Kopi Gayo, foto 2 dari 5"; satu foto: nama produk saja). Tombol foto punya nama tambahan "perbesar" dan `aria-haspopup="dialog"`. Tiap slide adalah `group` dengan `aria-roledescription="slide"` dan label "2 dari 5". Hanya foto aktif yang masuk urutan Tab. Cincin fokus di lightbox memakai paper supaya terlihat di atas ink. Axe tanpa pelanggaran serious/critical untuk galeri desktop, ponsel, dan lightbox terbuka.

## Kritik diri Putaran 3 (sebelum lawan sesudah) dan perbaikannya

1. Versi pertama membuat galeri selebar 58% tanpa batas tinggi: foto 835px terpotong lipatan di 1440×900. Diperbaiki dengan batas tinggi layar di `--gw`.
2. Penanda "1 / 5" semula disembunyikan di desktop karena thumbnail sudah menandai posisi. Dengan thumbnail di bawah lipatan pada layar 900px, jumlah foto jadi tidak terlihat. Penanda sekarang tampil di semua lebar.
3. Halaman ponsel bisa digulir ke samping sejauh lima foto, karena teks khusus pembaca layar diposisikan absolut di luar lintasan. Diperbaiki, dan e2e sekarang memeriksa tidak ada overflow horizontal.
4. Lightbox semula memotong foto 4:5 dan tidak bisa diperbesar di ponsel. Diperbaiki: `LARGE` tidak dipotong, foto tampil utuh, ada zoom cubit dan ketuk dua kali.
5. Thumbnail di bawah foto jatuh di bawah lipatan 900px. Dipindah ke kolom kiri; pemindahan pertama menyusutkan foto utama (584px), lalu `share` dan batas tinggi dikoreksi sampai 624px.
6. Screenshot ponsel gagal di langkah "tunggu gambar": selector `.gallery img` `.first()` mengenai `<img>` thumbnail yang `display:none` di ponsel (thumbnail ada lebih dulu di DOM). Salah selector di spec, bukan bug aplikasi; sekarang `.gallery .track img`.

## Alasan keputusan Putaran 3 (satu baris)

- 4:5 di mana-mana: satu potongan untuk kartu, panggung, dan thumbnail, tanpa potong ganda.
- Lebar dibatasi tinggi layar: foto yang utuh terlihat lebih berguna daripada foto lebih lebar yang terpotong lipatan.
- scroll-snap dan `<dialog>` bawaan: nol dependensi baru, perilaku sentuh dan fokus dikerjakan browser.
- `LARGE` utuh (`c_limit`): lightbox harus menunjukkan foto apa adanya; tiga ukuran kecil tetap 4:5 agar kartu seragam, jadi tetap hanya empat variasi transformasi.
- Kolom thumbnail di samping: semua thumbnail dan foto utama muat di 900px tanpa menggulir; biayanya kolom info 40px lebih sempit.

## Putaran Admin Ponsel: kartu, Badge, tab menu

**Masalah (screenshot 390px):** lencana "Ambil di toko" pecah dua baris dan garisnya memotong teks; tabel order terpotong di kanan (status, total, tombol tak terlihat); tanggal dan nama terbungkus 3 baris; tab menu mepet.

**Keputusan**
- **Satu `Badge`** (`components/Badge.tsx`, kelas `.badge-*`) untuk status order, jenis pengambilan, dan Aktif/Nonaktif. `white-space: nowrap`, `width: fit-content`, tinggi mengikuti isi (padding 3px 10px, `line-height` 1.25), teks 13px. Nada memakai token tema (`warn-*`, `ok-*`, `err-*`, `accent-hover`), jadi ikut palet aktif. Lencana bukan kontrol, jadi tidak diberi target 44px; tombol aksi tetap min 44px. `StatusLabel` kini membungkus `Badge`, jadi halaman pembeli ikut berubah dari titik+teks menjadi pil.
- **Daftar menjadi kartu di bawah 1100px**, bukan 768px: tabel order butuh sekitar 880px dan rel samping 232px ikut menyempit di 900-1100px, jadi di 768px dan 1024px tabel tetap terpotong. Kartu memakai `<ul>/<li>` (`OrderCard`, `.pcard`), dua kolom di lebar tablet. Tabel dan kartu sama-sama ada di DOM; CSS menyembunyikan satu (`display: none`, jadi tidak ada di pohon aksesibilitas). Kartu order: nomor + waktu "12 Nov, 22.50", penerima (detail di `<details>`), total, Badge status + jenis, tombol utama selebar kartu bernama "Proses pesanan, order 0063", lalu Lihat/Unduh PDF dua kolom. Order baru: titik dan sorotan lembut tetap ada.
- **Urutan** hilang bersama kepala kolom, jadi di mode kartu ada `Select` "Urutkan" (Order dan Produk).
- **Bar atas:** di ponsel Status dan Urutkan selebar layar; hitungan, "Diperbarui", dan tombol Muat ulang satu baris (teks kiri, tombol kanan). `aria-live="polite"` tetap pada hitungan saja.
- **Tab menu** (`useScrollFade`): digeser horizontal, tepi memudar (mask) hanya di sisi yang masih punya tab, tab aktif digulirkan ke tengah saat halaman berganti. Fokus tetap berurutan di DOM.
- **Halaman lain:** Produk jadi kartu; Kategori (dua kolom) dan Pengaturan sudah muat di 360px; aksi foto produk `nowrap` dan boleh turun baris per kelompok.
- **Audit:** `node scripts/admin-shots.mjs sebelum|sesudah` memotret Order, Produk, Kategori, Pengaturan, dan Ubah produk di 360/390/768/1440 ke `docs/design/admin-mobile/` dan gagal bila halaman melebar atau tabel tampak terpotong.

**Kritik diri (satu putaran)**
1. Kartu muncul juga di desktop: `.pcards { display: grid }` ditulis setelah `.only-narrow { display: none }` dan menang. Tertangkap oleh e2e (tombol "Aksi" ganda, strict mode). Display dasar dihapus; media query yang mengatur tampil.
2. Titik ganti 768px ternyata salah: di 768px tabel masih terpotong dan tanggal terbungkus 3 baris. Dinaikkan ke 1100px; sel tanggal dan nomor `nowrap`.
3. Di kisi dua kolom Badge ikut melar tinggi (baris grid melebar). Ditambah `align-content: start` pada kartu dan `align-items: flex-start` pada baris Badge.
4. Badge baru memutus selektor e2e lama (`.status`). Tes diganti ke `.badge`; tiga tes juga memakai `localhost:8080` tertulis, kini lewat `API`.
5. Di 360px kolom grid admin melebar ke 370px (tab menu yang lebar menentukan lebar kolom `1fr`), jadi halaman bisa digeser 10px di ponsel sungguhan. Audit awal lolos karena membandingkan dengan `innerWidth`, yang ikut melebar di emulasi ponsel; pembanding kini `visualViewport.width` (skrip dan e2e), dan kolom dibuat `minmax(0, 1fr)`.
6. Kelemahan yang tersisa: kolom Alamat di Pengaturan masih `input` satu baris (alamat panjang terpotong di layar sempit; bisa dibuat textarea); `<details>` nama penerima di kartu menambah satu ketukan untuk melihat telepon/alamat; tabel dan kartu sama-sama dirender (dua kali kerja render per halaman 15 baris, tidak terukur menjadi masalah).

**Pengujian:** unit `Badge`, `OrderCard`, `formatDateTimeCompact`; e2e `admin-mobile.spec.ts` (proses order sampai "Siap diambil" dari kartu, tab aktif terlihat di 360px, axe tanpa serious/critical di 4 halaman admin) berjalan di backend dan DB terpisah (`npm run e2e:mobile`, DB `tokoku_mobile`, port 18082); `npm run e2e:isolated` menjalankan suite desktop di sana, bukan di DB dev.
