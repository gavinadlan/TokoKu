#!/usr/bin/env bash
# Menulis pratinjau semua template email ke docs/design/email/*.html (backend profil dev harus berjalan).
set -euo pipefail
cd "$(dirname "$0")/.."
api="${API_URL:-http://localhost:8080}"
mkdir -p docs/design/email
for kind in ORDER_CREATED PAID PROCESSING READY_FOR_PICKUP SHIPPED COMPLETED CANCELLED; do
  for variant in pickup delivery; do
    curl -sf "$api/api/dev/email-preview/$kind?variant=$variant" -o "docs/design/email/$(echo $kind | tr 'A-Z_' 'a-z-')-$variant.html"
  done
done
curl -sf "$api/api/dev/email-preview/PAID?variant=delivery&format=text" -o docs/design/email/paid-delivery.txt
ls docs/design/email
