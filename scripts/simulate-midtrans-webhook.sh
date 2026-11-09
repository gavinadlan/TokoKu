#!/usr/bin/env bash
# Mengirim notifikasi Midtrans BUATAN ke backend lokal, ditandatangani dengan MIDTRANS_SERVER_KEY dari .env.
# Pakai: scripts/simulate-midtrans-webhook.sh <order_id_midtrans> <gross_amount> [status] [url]
#   mis. scripts/simulate-midtrans-webhook.sh TKU-12-ab12cd34 60000.00 settlement
# order_id = providerRef dari respons POST /api/orders/{id}/pay. Kunci tidak pernah dicetak.
set -euo pipefail
cd "$(dirname "$0")/.."
set -a; source .env; set +a
: "${MIDTRANS_SERVER_KEY:?MIDTRANS_SERVER_KEY not set in .env}"

ORDER_ID="${1:?order_id (providerRef)}"
GROSS="${2:?gross_amount, e.g. 60000.00}"
STATUS="${3:-settlement}"
URL="${4:-http://localhost:8080/api/payments/webhook}"
CODE=200

# signature_key = SHA512(order_id + status_code + gross_amount + ServerKey)
SIG=$(printf '%s%s%s%s' "$ORDER_ID" "$CODE" "$GROSS" "$MIDTRANS_SERVER_KEY" | shasum -a 512 | cut -d' ' -f1)

curl -sS -w '\nHTTP %{http_code}\n' -X POST "$URL" -H 'Content-Type: application/json' -d "{
  \"order_id\": \"$ORDER_ID\", \"status_code\": \"$CODE\", \"gross_amount\": \"$GROSS\",
  \"signature_key\": \"$SIG\", \"transaction_status\": \"$STATUS\", \"transaction_id\": \"sim-$ORDER_ID\",
  \"payment_type\": \"bank_transfer\"
}"
