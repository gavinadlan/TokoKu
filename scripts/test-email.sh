#!/usr/bin/env bash
# Kirim satu email uji ke alamat pilihan Anda untuk mencoba kredensial SMTP (hanya backend profil dev yang berjalan).
#   scripts/test-email.sh alamat@contoh.com
# Isi MAIL_USERNAME dan MAIL_PASSWORD (Gmail App Password) di .env, jalankan ulang scripts/dev-backend.sh dulu.
set -euo pipefail
cd "$(dirname "$0")/.."
to="${1:?Pakai: scripts/test-email.sh alamat@contoh.com}"
[ -f .dev-admin.env ] || { echo "Jalankan scripts/dev-backend.sh sekali dulu (membuat .dev-admin.env)"; exit 1; }
set -a; source .dev-admin.env; set +a
api="${API_URL:-http://localhost:8080}"
token=$(curl -sf -H 'Content-Type: application/json' -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}" "$api/api/admin/auth/login" \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["accessToken"])')
curl -s -w '\nHTTP %{http_code}\n' -H "Authorization: Bearer $token" -H 'Content-Type: application/json' -d "{\"to\":\"$to\"}" "$api/api/admin/dev/test-email"
