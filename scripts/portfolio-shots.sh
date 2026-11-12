#!/usr/bin/env bash
# Membuat ulang screenshot portofolio ke docs/portfolio/tokoku/. Lihat README di folder itu.
#   scripts/portfolio-shots.sh          jalankan (backend dev dan frontend dev dinyalakan bila belum hidup)
#   scripts/portfolio-shots.sh --seed   isi data demo dulu (foto, pembeli, pesanan; sekali saja pada DB yang sudah dirapikan)
# Email dimatikan dan pembayaran fake; foto produk memakai penyimpanan lokal dev.
set -euo pipefail
cd "$(dirname "$0")/.."
started=()
if ! curl -sf localhost:8080/actuator/health >/dev/null; then
  APP_PAYMENT_PROVIDER=fake APP_MAIL_ENABLED=false APP_IMAGES_PROVIDER=fake scripts/dev-backend.sh >/tmp/tokoku-backend.log 2>&1 & started+=($!)
  until curl -sf localhost:8080/actuator/health >/dev/null; do sleep 2; done
fi
if ! curl -sf localhost:5173 >/dev/null; then
  (cd frontend && npm run dev >/tmp/tokoku-frontend.log 2>&1) & started+=($!)
  until curl -sf localhost:5173 >/dev/null; do sleep 1; done
fi
mkdir -p docs/portfolio/tokoku
[ "${1:-}" = "--seed" ] && node scripts/portfolio-seed.mjs
node scripts/portfolio-shots.mjs
[ ${#started[@]} -eq 0 ] || kill "${started[@]}" 2>/dev/null || true
