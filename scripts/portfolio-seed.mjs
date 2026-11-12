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

// 2. Foto produk lewat jalur resmi: tanda tangan -> unggah langsung ke penyimpanan -> daftarkan. Foto pertama = utama.
const PRODUCT_PHOTOS = { 1: ['gayo-1', 'gayo-2', 'gayo-3', 'gayo-4'], 2: ['toraja-1'], 3: ['flores-1'], 11: ['v60-1', 'v60-2', 'v60-3', 'v60-4'], 12: ['frenchpress-1'], 15: ['teko-1'], 16: ['grinder-1', 'grinder-2', 'grinder-3'], 19: ['timbangan-1'], 21: ['mug-2', 'mug-1', 'mug-3', 'mug-4'], 25: ['toples-1'] };
for (const [id, names] of Object.entries(PRODUCT_PHOTOS)) {
  const current = await j('GET', `/api/admin/products/${id}`, { token: admin });
  if (current.images.length) { console.log(`produk ${id}: sudah punya ${current.images.length} foto, dilewati`); continue; }
  for (const n of names) {
    const sig = await j('POST', '/api/admin/uploads/sign', { token: admin });
    const form = new FormData();
    form.set('file', new Blob([readFileSync(`${PHOTOS}${n}.jpg`)], { type: 'image/jpeg' }), `${n}.jpg`);
    for (const [k, v] of [['api_key', sig.apiKey], ['timestamp', sig.timestamp], ['signature', sig.signature], ['folder', sig.folder], ['allowed_formats', sig.allowedFormats]]) form.set(k, String(v));
    const url = sig.uploadUrl.startsWith('http') ? sig.uploadUrl : `${API}${sig.uploadUrl}`;
    const up = await (await fetch(url, { method: 'POST', body: form })).json();
    if (!up.public_id) throw new Error(`unggah ${n} gagal: ${JSON.stringify(up).slice(0, 200)}`);
    await j('POST', `/api/admin/products/${id}/images`, { token: admin, body: { publicId: up.public_id, version: up.version, signature: up.signature, width: up.width, height: up.height, format: up.format, bytes: up.bytes } });
    console.log(`produk ${id}: ${n} -> ${up.public_id}`);
  }
}

// 3. Pembeli dan pesanan
const PASS = process.env.DEMO_PASSWORD ?? `demo-${randomBytes(6).toString('hex')}`; // acak, ditulis ke .demo-credentials (di-gitignore)
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
  ['siti', 'PICKUP', 6, 'COMPLETED', [[1, 2], [25, 1]]],
  ['budi', 'DELIVERY', 5, 'COMPLETED', [[16, 1], [2, 1]]],
  ['dewi', 'DELIVERY', 4, 'COMPLETED', [[11, 1], [3, 2]]],
  ['andi', 'DELIVERY', 3, 'SHIPPED', [[15, 1], [19, 1]]],
  ['maya', 'DELIVERY', 2, 'SHIPPED', [[12, 1], [1, 1]]],
  ['rizky', 'PICKUP', 2, 'READY_FOR_PICKUP', [[21, 2], [3, 1]]],
  ['siti', 'DELIVERY', 1, 'PROCESSING', [[16, 1], [19, 1], [2, 2]]],
  ['dewi', 'PICKUP', 1, 'PAID', [[1, 1], [21, 1]]],
  ['budi', 'DELIVERY', 0, 'PAID', [[11, 1], [15, 1]]],
  ['maya', 'DELIVERY', 0, 'PAID', [[2, 3]]],
  ['andi', 'PICKUP', 0, 'PENDING_PAYMENT', [[3, 2]]],
  ['rizky', 'DELIVERY', 0, 'PENDING_PAYMENT', [[25, 2], [12, 1]]],
];
const shifts = [];
for (const [who, type, days, final, items] of ORDERS) {
  const p = people[who];
  for (const [productId, quantity] of items) await j('POST', '/api/cart/items', { token: p.token, body: { productId, quantity } });
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
const sql = shifts.map(([id, d], i) => {
  const t = `(date_trunc('day', now()) - interval '${d} day' + interval '${9 + ((i * 3) % 8)} hour ${(i * 17) % 60} minute')`;
  return `update orders set created_at=${t}, invoiced_at=case when invoiced_at is not null then ${t}+interval '6 minute' end, pickup_ready_at=case when pickup_ready_at is not null then ${t}+interval '5 hour' end where id=${id}; update payments set created_at=${t}, paid_at=case when paid_at is not null then ${t}+interval '6 minute' end where order_id=${id};`;
}).join('\n');
execFileSync('docker', ['compose', 'exec', '-T', 'db', 'psql', '-U', 'tokoku', 'tokoku', '-v', 'ON_ERROR_STOP=1'], { cwd: ROOT, input: sql, stdio: ['pipe', 'ignore', 'inherit'] });

// Metode bayar yang wajar di invoice (provider fake menulis "fake" yang tampil sebagai "Simulasi pembayaran")
const types = ['bank_transfer', 'qris', 'gopay', 'bank_transfer', 'qris', 'credit_card'];
execFileSync('docker', ['compose', 'exec', '-T', 'db', 'psql', '-U', 'tokoku', 'tokoku', '-v', 'ON_ERROR_STOP=1'], { cwd: ROOT, stdio: ['pipe', 'ignore', 'inherit'],
  input: shifts.map(([id], i) => `update payments set payment_type='${types[i % types.length]}' where order_id=${id} and payment_type='fake';`).join('\n') });

const credFile = `${ROOT}docs/portfolio/tokoku/.demo-credentials`;
writeFileSync(credFile, Object.values(people).map((p) => `${p.email}  ${PASS}`).join('\n') + '\n');
console.log(`selesai; kredensial pembeli demo di ${credFile}`);
