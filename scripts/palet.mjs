#!/usr/bin/env node
// Sumber tunggal tema: src/main/resources/theme/tema.json (dibaca juga oleh backend untuk email dan invoice).
// Skrip ini (1) memeriksa kontras WCAG AA tiap varian, (2) menulis frontend/src/styles/themes.css dan favicon.svg.
//   node scripts/palet.mjs            periksa + tulis
//   node scripts/palet.mjs --check    periksa saja (gagal bila ada pasangan di bawah AA)
import { readFileSync, writeFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, join } from 'node:path'

const root = join(dirname(fileURLToPath(import.meta.url)), '..')
const tema = JSON.parse(readFileSync(join(root, 'src/main/resources/theme/tema.json'), 'utf8'))
const varian = Object.fromEntries(Object.entries(tema.varian).map(([k, v]) => [k, { ...tema.varian.base, ...v }]))

const lum = (hex) => {
  const c = [1, 3, 5].map((i) => parseInt(hex.slice(i, i + 2), 16) / 255)
    .map((v) => (v <= 0.03928 ? v / 12.92 : ((v + 0.055) / 1.055) ** 2.4))
  return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2]
}
const ratio = (a, b) => { const [x, y] = [lum(a), lum(b)].sort((p, q) => q - p); return (x + 0.05) / (y + 0.05) }

// [teks/komponen, latar, minimum, keterangan]
const PASANGAN = [
  ['ink', 'paper', 4.5, 'teks utama'], ['ink', 'surface', 4.5, 'teks di kartu/input'],
  ['ink-muted', 'paper', 4.5, 'teks sekunder'], ['ink-muted', 'surface', 4.5, 'teks sekunder di kartu'],
  ['ink-subtle', 'paper', 4.5, 'placeholder/nonaktif'], ['ink-subtle', 'surface', 4.5, 'placeholder di input'],
  ['on-accent', 'accent', 4.5, 'teks tombol'], ['on-accent', 'accent-hover', 4.5, 'teks tombol (hover)'],
  ['accent', 'paper', 4.5, 'tautan/teks aksen di kertas'], ['accent', 'surface', 4.5, 'teks aksen di kartu'],
  ['accent', 'paper', 3, 'cincin fokus'], ['accent', 'adm-bg', 3, 'cincin fokus admin'],
  ['line-strong', 'paper', 3, 'batas input/tombol'], ['line-strong', 'surface', 3, 'batas input di kartu'],
  ['ok-ink', 'ok-bg', 4.5, 'status ok'], ['warn-ink', 'warn-bg', 4.5, 'status peringatan'], ['err-ink', 'err-bg', 4.5, 'status error'],
  ['err', 'paper', 4.5, 'teks error'], ['err', 'surface', 4.5, 'teks error di input'],
  ['paper', 'rail', 4.5, 'teks rail admin'], ['rail-muted', 'rail', 4.5, 'teks sekunder rail'], ['paper', 'rail', 3, 'fokus rail'],
  ['invoice-accent', '#FFFFFF', 4.5, 'stempel Lunas invoice'], ['ink', '#FFFFFF', 4.5, 'teks invoice'], ['ink-muted', '#FFFFFF', 4.5, 'teks sekunder invoice/email'],
  ['accent', '#FFFFFF', 3, 'garis aksen email'], ['on-accent', 'accent', 4.5, 'tombol email'],
]

let gagal = 0
for (const [nama, v] of Object.entries(varian)) {
  const bad = []
  for (const [fg, bg, min, ket] of PASANGAN) {
    const r = ratio(v[fg] ?? fg, v[bg] ?? bg)
    if (r < min) bad.push(`  ${ket}: ${fg} ${v[fg] ?? fg} di ${bg} ${v[bg] ?? bg} = ${r.toFixed(2)} (< ${min})`)
  }
  const min = Math.min(...PASANGAN.filter((p) => p[2] === 4.5).map(([fg, bg]) => ratio(v[fg] ?? fg, v[bg] ?? bg)))
  console.log(`varian ${nama}: ${bad.length ? 'GAGAL' : 'lulus AA'} (teks terendah ${min.toFixed(2)}:1)`)
  bad.forEach((b) => console.log(b)); gagal += bad.length
}
if (process.argv.includes('--check')) process.exit(gagal ? 1 : 0)

const rgb = (hex) => [1, 3, 5].map((i) => parseInt(hex.slice(i, i + 2), 16)).join(' ')
const FONT = { serif: '"Fraunces Variable", "Iowan Old Style", Georgia, serif', sans: '"Hanken Grotesk Variable", system-ui, -apple-system, "Segoe UI", sans-serif' }
const blok = (v) => Object.entries(v).filter(([k]) => k !== 'display').map(([k, x]) => `  --${k}: ${x};`).join('\n')
  + `\n  --ink-rgb: ${rgb(v.ink)}; --paper-rgb: ${rgb(v.paper)}; --accent-rgb: ${rgb(v.accent)};`
  + `\n  --font-display: ${FONT[v.display]};`
  + `\n  --shadow-float: 0 8px 24px rgb(${rgb(v.ink)} / 0.12);`

let css = '/* DIHASILKAN oleh scripts/palet.mjs dari src/main/resources/theme/tema.json. Jangan edit tangan. */\n'
css += `:root {\n${blok(varian[tema.aktif])}\n}\n`
for (const [nama, v] of Object.entries(varian)) css += `:root[data-palet="${nama}"] {\n${blok(v)}\n}\n`
writeFileSync(join(root, 'frontend/src/styles/themes.css'), css)

const a = varian[tema.aktif]
writeFileSync(join(root, 'frontend/public/favicon.svg'),
  `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32"><rect width="32" height="32" rx="6" fill="${a.ink}"/><text x="16" y="23" text-anchor="middle" font-family="Georgia, serif" font-size="20" fill="${a.paper}">K</text></svg>\n`)
console.log(`ditulis themes.css + favicon.svg (aktif: ${tema.aktif})`)
