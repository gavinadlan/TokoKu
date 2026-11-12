#!/usr/bin/env node
// Data demo untuk screenshot portofolio. Jalankan sekali terhadap backend dev (provider pembayaran fake, email mati).
//   node scripts/portfolio-seed.mjs            # pengaturan toko, foto produk, pembeli, pesanan
// Kredensial pembeli demo ditulis ke docs/portfolio/tokoku/.demo-credentials (di-gitignore).
// Butuh: docs/portfolio/foto-produk/*.jpg (lihat CREDITS.md) dan .dev-admin.env. Docker dipakai untuk menggeser tanggal pesanan.
import { readFileSync, writeFileSync, existsSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { randomBytes } from 'node:crypto';

const ROOT = fileURLToPath(new URL('..', import.meta.url));
const API = process.env.API_URL ?? 'http://localhost:8080';
const PHOTOS = `${ROOT}docs/portfolio/foto-produk/`;
const env = Object.fromEntries(readFileSync(`${ROOT}.dev-admin.env`, 'utf8').split('\n').filter(Boolean).map((l) => l.split(/=(.*)/s).slice(0, 2)));

async function j(method, path, { token, body } = {}) {
  const res = await fetch(`${API}${path}`, {
    method,
    headers: { ...(body ? { 'Content-Type': 'application/json' } : {}), ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await res.text();
  if (!res.ok) throw new Error(`${method} ${path} -> ${res.status} ${text.slice(0, 200)}`);
  return text ? JSON.parse(text) : null;
}

const admin = (await j('POST', '/api/admin/auth/login', { body: { email: env.ADMIN_EMAIL, password: env.ADMIN_PASSWORD } })).accessToken;

// 1. Pengaturan toko
await j('PUT', '/api/admin/store-settings', {
  token: admin,
  body: {
    name: 'TokoKu',
    address: 'Jl. Braga No. 42, Sumur Bandung, Kota Bandung 40111',
    phone: '022-5550-1234',
    email: 'halo@tokoku.test',
    openingHours: 'Senin sampai Sabtu, 09.00 sampai 17.00',
    pickupInstructions: 'Tunjukkan kode ambil kepada petugas di kasir. Bawa identitas bila diminta.',
    deliveryEnabled: true,
    pickupEnabled: true,
    flatShippingFee: 15000,
    freeShippingThreshold: 500000,
    invoiceFooter: 'Terima kasih telah berbelanja di TokoKu. Simpan invoice ini sebagai bukti pembayaran.',
  },
});

// 2. Katalog demo. Aplikasi mengurutkan "Terbaru" menurut id, jadi urutan DIBUAT = kebalikan urutan tampil:
// produk yang dibuat terakhir tampil pertama (baris pertama beranda = 4 produk terakhir di daftar ini).
// Produk aktif lama dinonaktifkan lewat API (baris tidak dihapus); pesanan lama dibuang karena merujuk produk lama.
const sql = (input) => execFileSync('docker', ['compose', 'exec', '-T', 'db', 'psql', '-U', 'tokoku', 'tokoku', '-v', 'ON_ERROR_STOP=1'], { cwd: ROOT, input, stdio: ['pipe', 'ignore', 'inherit'] });
sql('delete from payments; delete from email_log; delete from order_items; delete from orders; delete from cart_items; delete from carts; update invoice_counters set last_value=0;');
const cats = Object.fromEntries((await j('GET', '/api/categories')).map((c) => [c.name, c.id]));
const old = await j('GET', '/api/admin/products?active=true&size=200', { token: admin });
for (const p of old.content) await j('DELETE', `/api/products/${p.id}`, { token: admin });

const CATALOG = [ // urutan buat (paling akhir = tampil pertama)
  ['flores', 'Biji Kopi Single Origin', 'Kopi Arabika Flores Bajawa 250g', 88000, 60, 'Light-medium roast, nuansa kacang dan jeruk.', ['flores-1']],
  ['toraja', 'Biji Kopi Single Origin', 'Kopi Arabika Toraja Sapan 250g', 92000, 60, 'Body tebal, after-taste manis karamel.', ['toraja-1']],
  ['frenchpress', 'Alat Seduh Manual', 'French Press 600ml', 185000, 40, 'Gelas borosilikat dan rangka stainless steel.', ['frenchpress-1']],
  ['teko', 'Alat Seduh Manual', 'Teko Leher Angsa 600ml', 275000, 40, 'Teko stainless dengan kontrol tuang presisi.', ['teko-1']],
  ['timbangan', 'Grinder dan Timbangan', 'Timbangan Kopi Digital 0.1g', 325000, 40, 'Timer bawaan, kapasitas 2kg, layar mudah dibaca.', ['timbangan-1', 'timbangan-2']],
  ['toples', 'Gelas dan Aksesori', 'Toples Kedap Udara 500g', 89000, 40, 'Penyimpan biji kopi dengan katup satu arah.', ['toples-1']],
  ['mug', 'Gelas dan Aksesori', 'Mug Keramik Matte 300ml', 55000, 40, 'Mug keramik tebal berglasir doff, menahan panas. Warna putih gading.', ['mug-2', 'mug-1', 'mug-3', 'mug-4']],
  ['grinder', 'Grinder dan Timbangan', 'Grinder Manual Burr Keramik', 395000, 40, 'Burr keramik, pengaturan gilingan bertingkat.', ['grinder-1', 'grinder-2', 'grinder-3']],
  ['v60', 'Alat Seduh Manual', 'Dripper V60 Keramik 02', 165000, 40, 'Dripper kerucut dengan spiral rib untuk aliran seragam.', ['v60-1', 'v60-2', 'v60-3']],
  ['gayo', 'Biji Kopi Single Origin', 'Kopi Arabika Gayo 250g', 85000, 60, 'Medium roast, aroma rempah dan cokelat, asal Aceh Tengah.', ['gayo-1', 'gayo-2', 'gayo-3', 'gayo-4']],
];
const PID = {};
for (const [key, cat, name, price, stock, description, photos] of CATALOG) {
  const id = (await j('POST', '/api/products', { token: admin, body: { categoryId: cats[cat], name, price, stock, description } })).id;
  PID[key] = id;
  // foto lewat jalur resmi: tanda tangan -> unggah langsung ke penyimpanan -> daftarkan; foto pertama = utama
  for (const n of photos) {
    const sig = await j('POST', '/api/admin/uploads/sign', { token: admin });
    const form = new FormData();
    form.set('file', new Blob([readFileSync(`${PHOTOS}${n}.jpg`)], { type: 'image/jpeg' }), `${n}.jpg`);
    for (const [k, v] of [['api_key', sig.apiKey], ['timestamp', sig.timestamp], ['signature', sig.signature], ['folder', sig.folder], ['allowed_formats', sig.allowedFormats]]) form.set(k, String(v));
    const url = sig.uploadUrl.startsWith('http') ? sig.uploadUrl : `${API}${sig.uploadUrl}`;
    const up = await (await fetch(url, { method: 'POST', body: form })).json();
    if (!up.public_id) throw new Error(`unggah ${n} gagal: ${JSON.stringify(up).slice(0, 200)}`);
    await j('POST', `/api/admin/products/${id}/images`, { token: admin, body: { publicId: up.public_id, version: up.version, signature: up.signature, width: up.width, height: up.height, format: up.format, bytes: up.bytes } });
  }
  console.log(`produk ${id}: ${name} (${photos.length} foto)`);
}

// 3. Pembeli dan pesanan
const credPath = `${ROOT}docs/portfolio/tokoku/.demo-credentials`;
// kata sandi acak, dipakai ulang bila berkas kredensial sudah ada (pembeli lama tetap bisa masuk)
const PASS = process.env.DEMO_PASSWORD ?? (existsSync(credPath) ? readFileSync(credPath, 'utf8').split(/\s+/)[1] : `demo-${randomBytes(6).toString('hex')}`);
const people = {
  siti: { name: 'Siti Rahmawati', email: 'siti.rahmawati@surel.test', phone: '0812-5550-0101', addr: 'Jl. Dago Asri No. 17, Coblong, Kota Bandung 40135' },
  budi: { name: 'Budi Santoso', email: 'budi.santoso@surel.test', phone: '0813-5550-0102', addr: 'Jl. Kebon Jeruk Raya No. 8, Kebon Jeruk, Jakarta Barat 11530' },
  dewi: { name: 'Dewi Lestari', email: 'dewi.lestari@surel.test', phone: '0857-5550-0103', addr: 'Jl. Pemuda No. 25, Semarang Tengah, Kota Semarang 50133' },
  andi: { name: 'Andi Pratama', email: 'andi.pratama@surel.test', phone: '0821-5550-0104', addr: 'Jl. Raya Darmo No. 63, Tegalsari, Surabaya 60265' },
  maya: { name: 'Maya Anggraini', email: 'maya.anggraini@surel.test', phone: '0878-5550-0105', addr: 'Jl. Kaliurang Km 5, Sleman, Yogyakarta 55281' },
  rizky: { name: 'Rizky Hidayat', email: 'rizky.hidayat@surel.test', phone: '0896-5550-0106', addr: 'Jl. Gatot Subroto No. 112, Medan Petisah, Medan 20112' },
};
for (const p of Object.values(people)) {
  try { await j('POST', '/api/auth/register', { body: { email: p.email, password: PASS, name: p.name } }); } catch (e) { if (!/409/.test(e.message)) throw e; }
  p.token = (await j('POST', '/api/auth/login', { body: { email: p.email, password: PASS } })).accessToken;
}

// [pembeli, jenis, daysAgo, status akhir, item [produk, jumlah]]
const ORDERS = [
  ['siti', 'PICKUP', 6, 'COMPLETED', [['gayo', 2], ['toples', 1]]],
  ['budi', 'DELIVERY', 5, 'COMPLETED', [['grinder', 1], ['toraja', 1]]],
  ['dewi', 'DELIVERY', 4, 'COMPLETED', [['v60', 1], ['flores', 2]]],
  ['andi', 'DELIVERY', 3, 'SHIPPED', [['teko', 1], ['timbangan', 1]]],
  ['maya', 'DELIVERY', 2, 'SHIPPED', [['frenchpress', 1], ['gayo', 1]]],
  ['rizky', 'PICKUP', 2, 'READY_FOR_PICKUP', [['mug', 2], ['flores', 1]]],
  ['siti', 'DELIVERY', 1, 'PROCESSING', [['grinder', 1], ['timbangan', 1], ['toraja', 2]]],
  ['dewi', 'PICKUP', 1, 'PAID', [['gayo', 1], ['mug', 1]]],
  ['budi', 'DELIVERY', 0, 'PAID', [['v60', 1], ['teko', 1]]],
  ['maya', 'DELIVERY', 0, 'PAID', [['toraja', 3]]],
  ['andi', 'PICKUP', 0, 'PENDING_PAYMENT', [['flores', 2]]],
  ['rizky', 'DELIVERY', 0, 'PENDING_PAYMENT', [['toples', 2], ['frenchpress', 1]]],
];
const shifts = [];
for (const [who, type, days, final, items] of ORDERS) {
  const p = people[who];
  for (const [key, quantity] of items) await j('POST', '/api/cart/items', { token: p.token, body: { productId: PID[key], quantity } });
  const order = await j('POST', '/api/orders', { token: p.token, body: { fulfillmentType: type, shippingName: p.name, shippingPhone: p.phone, shippingAddress: type === 'DELIVERY' ? p.addr : undefined } });
  if (final !== 'PENDING_PAYMENT') {
    const pay = await j('POST', `/api/orders/${order.id}/pay`, { token: p.token });
    await j('POST', `/api/dev/payments/${pay.providerRef}/simulate?status=settlement`);
    const step = (status, extra = {}) => j('PATCH', `/api/admin/orders/${order.id}/status`, { token: admin, body: { status, ...extra } });
    let pickupCode;
    if (final !== 'PAID') await step('PROCESSING');
    if (final === 'SHIPPED' || (final === 'COMPLETED' && type === 'DELIVERY')) await step('SHIPPED', { courier: 'JNE', trackingNumber: `JNE${7000000000 + order.id * 1379}` });
    if (final === 'READY_FOR_PICKUP' || (final === 'COMPLETED' && type === 'PICKUP')) await step('READY_FOR_PICKUP');
    if (final === 'COMPLETED') {
      if (type === 'PICKUP') pickupCode = (await j('GET', `/api/admin/orders/${order.id}`, { token: admin })).pickupCode;
      await step('COMPLETED', pickupCode ? { pickupCode } : {});
    }
  }
  shifts.push([order.id, days, final]);
  console.log(`order ${order.id} ${type} ${final}`);
}

// Sebar tanggal: geser created_at dan stempel turunannya, jam dibuat bervariasi.
const shiftSql = shifts.map(([id, d], i) => {
  const t = `(date_trunc('day', now()) - interval '${d} day' + interval '${9 + ((i * 3) % 8)} hour ${(i * 17) % 60} minute')`;
  return `update orders set created_at=${t}, invoiced_at=case when invoiced_at is not null then ${t}+interval '6 minute' end, pickup_ready_at=case when pickup_ready_at is not null then ${t}+interval '5 hour' end where id=${id}; update payments set created_at=${t}, paid_at=case when paid_at is not null then ${t}+interval '6 minute' end where order_id=${id};`;
}).join('\n');
sql(shiftSql);

// Metode bayar yang wajar di invoice (provider fake menulis "fake" yang tampil sebagai "Simulasi pembayaran")
const types = ['bank_transfer', 'qris', 'gopay', 'bank_transfer', 'qris', 'credit_card'];
sql(shifts.map(([id], i) => `update payments set payment_type='${types[i % types.length]}' where order_id=${id} and payment_type='fake';`).join('\n'));

// Admin demo: nama dan alamat wajar (hanya data demo; kode produksi tidak berubah). .dev-admin.env disamakan supaya login lokal dan e2e tetap jalan.
const ADMIN_DEMO_EMAIL = 'rina@tokoku.test';
sql(`update users set email='${ADMIN_DEMO_EMAIL}', name='Rina Wijaya' where role='ADMIN' and email in ('${env.ADMIN_EMAIL}','${ADMIN_DEMO_EMAIL}');`);
writeFileSync(`${ROOT}.dev-admin.env`, readFileSync(`${ROOT}.dev-admin.env`, 'utf8').replace(/^ADMIN_EMAIL=.*$/m, `ADMIN_EMAIL=${ADMIN_DEMO_EMAIL}`));

const credFile = credPath;
writeFileSync(credFile, Object.values(people).map((p) => `${p.email}  ${PASS}`).join('\n') + '\n');
console.log(`selesai; kredensial pembeli demo di ${credFile}`);
