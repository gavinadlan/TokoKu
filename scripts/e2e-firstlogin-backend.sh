#!/usr/bin/env bash
# Backend KEDUA khusus e2e "login admin pertama kali": port 8081, database sendiri (dibuat ulang tiap jalan),
# admin dari env dengan APP_ADMIN_FORCE_PASSWORD_CHANGE=true. Tidak menyentuh backend dev di :8080.
set -euo pipefail
cd "$(dirname "$0")/.."
[ -f .env ] || { echo "Butuh .env (DB_PASSWORD, JWT_SECRET)"; exit 1; }
set -a; source .env; set +a
if [ -z "${JAVA_HOME:-}" ] && [ -d /opt/homebrew/opt/openjdk@21 ]; then
  export JAVA_HOME=/opt/homebrew/opt/openjdk@21 PATH=/opt/homebrew/opt/openjdk@21/bin:$PATH
fi
docker compose up -d --wait db >/dev/null
docker compose exec -T db psql -U tokoku -d postgres -qc "DROP DATABASE IF EXISTS tokoku_firstlogin" -c "CREATE DATABASE tokoku_firstlogin OWNER tokoku"
export DATABASE_PASSWORD="$DB_PASSWORD" DATABASE_URL="jdbc:postgresql://localhost:${DB_PORT:-5432}/tokoku_firstlogin"
export SPRING_PROFILES_ACTIVE=dev SERVER_PORT=8081
export APP_PAYMENT_PROVIDER=fake APP_IMAGES_PROVIDER=fake APP_MAIL_ENABLED=false
export ADMIN_EMAIL=pertama@tokoku.local ADMIN_PASSWORD=sementara-uji-123 APP_ADMIN_FORCE_PASSWORD_CHANGE=true
export APP_CORS_ALLOWED_ORIGINS=http://localhost:5174
export APP_RATELIMIT_ADMIN_LOGIN_PER_EMAIL=1000 APP_RATELIMIT_ADMIN_LOGIN_PER_IP=1000
exec ./mvnw -B -q spring-boot:run
