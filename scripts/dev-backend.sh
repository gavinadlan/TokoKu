#!/usr/bin/env bash
# Menjalankan backend untuk pengembangan frontend: Postgres (Docker), profil dev (data seed kopi),
# provider pembayaran (midtrans bila kunci ada, kalau tidak fake), CORS untuk frontend.
#   scripts/dev-backend.sh            jalankan
#   scripts/dev-backend.sh --reset    hapus data database dulu (volume Docker) lalu jalankan
# Variabel opsional: FRONTEND_ORIGIN (default http://localhost:5173), APP_PAYMENT_PROVIDER (default: midtrans bila kunci terisi, kalau tidak fake)
set -euo pipefail
cd "$(dirname "$0")/.."

[ -f .env ] || { echo "Buat .env dari .env.example dulu (butuh DB_PASSWORD dan JWT_SECRET)"; exit 1; }
# Nilai yang diberikan lewat shell (mis. APP_PAYMENT_PROVIDER=fake APP_MAIL_ENABLED=false scripts/dev-backend.sh) menang atas .env
_pp="${APP_PAYMENT_PROVIDER-}"; _me="${APP_MAIL_ENABLED-}"
set -a; source .env; set +a
[ -z "$_pp" ] || export APP_PAYMENT_PROVIDER="$_pp"
[ -z "$_me" ] || export APP_MAIL_ENABLED="$_me"

if [ -z "${JAVA_HOME:-}" ] && [ -d /opt/homebrew/opt/openjdk@21 ]; then
  export JAVA_HOME=/opt/homebrew/opt/openjdk@21 PATH=/opt/homebrew/opt/openjdk@21/bin:$PATH
fi

if [ "${1:-}" = "--reset" ]; then docker compose down -v >/dev/null 2>&1 || true; fi
docker compose up -d --wait db >/dev/null

# Admin lokal: dibuat sekali, disimpan di .dev-admin.env (di-gitignore). Dipakai juga oleh e2e.
if [ ! -f .dev-admin.env ]; then
  printf 'ADMIN_EMAIL=admin@tokoku.local\nADMIN_PASSWORD=%s\n' "$(openssl rand -hex 12)" > .dev-admin.env
fi
set -a; source .dev-admin.env; set +a

export DATABASE_PASSWORD="$DB_PASSWORD"
export DATABASE_URL="jdbc:postgresql://localhost:${DB_PORT:-5432}/tokoku"
export SPRING_PROFILES_ACTIVE=dev
export APP_ADMIN_FORCE_PASSWORD_CHANGE=false   # admin lokal/e2e tidak dipaksa ganti password
# Pembayaran: Midtrans bila kedua kunci ada di .env, kalau tidak fake. APP_PAYMENT_PROVIDER eksplisit selalu menang.
if [ -z "${APP_PAYMENT_PROVIDER:-}" ]; then
  if [ -n "${MIDTRANS_SERVER_KEY:-}" ] && [ -n "${MIDTRANS_CLIENT_KEY:-}" ]; then
    export APP_PAYMENT_PROVIDER=midtrans
  else
    export APP_PAYMENT_PROVIDER=fake
  fi
fi
# Dev lokal: batas login dilonggarkan (e2e masuk berkali-kali dari satu IP). Default backend: toko 5/menit per email dan 30/menit per IP, admin 3 dan 10.
export APP_RATELIMIT_LOGIN_PER_EMAIL="${APP_RATELIMIT_LOGIN_PER_EMAIL:-1000}" APP_RATELIMIT_LOGIN_PER_IP="${APP_RATELIMIT_LOGIN_PER_IP:-1000}"
export APP_RATELIMIT_ADMIN_LOGIN_PER_EMAIL="${APP_RATELIMIT_ADMIN_LOGIN_PER_EMAIL:-1000}" APP_RATELIMIT_ADMIN_LOGIN_PER_IP="${APP_RATELIMIT_ADMIN_LOGIN_PER_IP:-1000}"
# Foto produk: Cloudinary bila kunci ada di .env, kalau tidak provider fake (unggahan diterima server dev)
if [ -z "${APP_IMAGES_PROVIDER:-}" ]; then
  if [ -n "${CLOUDINARY_CLOUD_NAME:-}" ] && [ -n "${CLOUDINARY_API_KEY:-}" ] && [ -n "${CLOUDINARY_API_SECRET:-}" ]; then
    export APP_IMAGES_PROVIDER=cloudinary
  else
    export APP_IMAGES_PROVIDER=fake
  fi
fi
export APP_CORS_ALLOWED_ORIGINS="${FRONTEND_ORIGIN:-http://localhost:5173}"

echo "Backend: http://localhost:8080  (pembayaran: $APP_PAYMENT_PROVIDER, foto: $APP_IMAGES_PROVIDER, admin: $ADMIN_EMAIL, password di .dev-admin.env)"
exec ./mvnw -B -q spring-boot:run
