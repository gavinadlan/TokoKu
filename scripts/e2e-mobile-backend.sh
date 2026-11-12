#!/usr/bin/env bash
# Backend KETIGA khusus e2e admin ponsel: port 18082, database sendiri (dibuat ulang tiap jalan), data seed dev.
# Tidak menyentuh backend dev (:8080) maupun data demo di database utama.
set -euo pipefail
cd "$(dirname "$0")/.."
[ -f .env ] || { echo "Butuh .env (DB_PASSWORD, JWT_SECRET)"; exit 1; }
set -a; source .env; set +a
if [ -z "${JAVA_HOME:-}" ] && [ -d /opt/homebrew/opt/openjdk@21 ]; then
  export JAVA_HOME=/opt/homebrew/opt/openjdk@21 PATH=/opt/homebrew/opt/openjdk@21/bin:$PATH
fi
docker compose up -d --wait db >/dev/null
docker compose exec -T db psql -U tokoku -d postgres -qc "DROP DATABASE IF EXISTS tokoku_mobile" -c "CREATE DATABASE tokoku_mobile OWNER tokoku"
export DATABASE_PASSWORD="$DB_PASSWORD" DATABASE_URL="jdbc:postgresql://localhost:${DB_PORT:-5432}/tokoku_mobile"
export SPRING_PROFILES_ACTIVE=dev SERVER_PORT=18082
export APP_PAYMENT_PROVIDER=fake APP_IMAGES_PROVIDER=fake APP_MAIL_ENABLED=false
export ADMIN_EMAIL=ponsel@tokoku.local ADMIN_PASSWORD=ponsel-uji-123 APP_ADMIN_FORCE_PASSWORD_CHANGE=false
export APP_CORS_ALLOWED_ORIGINS=http://localhost:5175 APP_IMAGES_PUBLIC_BASE_URL=http://localhost:18082
export APP_RATELIMIT_LOGIN_PER_EMAIL=1000 APP_RATELIMIT_LOGIN_PER_IP=1000 APP_RATELIMIT_ADMIN_LOGIN_PER_EMAIL=1000 APP_RATELIMIT_ADMIN_LOGIN_PER_IP=1000
exec ./mvnw -B -q spring-boot:run
