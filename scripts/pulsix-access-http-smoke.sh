#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
cd "$ROOT_DIR"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "[smoke] missing command: $1" >&2
    exit 1
  fi
}

require_cmd curl
require_cmd openssl
require_cmd od

DEFAULT_PAYLOAD=$(cat <<'JSON'
{"event_id":"E_RAW_9103","occur_time_ms":1773287100000,"req":{"traceId":"T_RAW_9103"},"uid":" U9003 ","dev_id":"D9003","client_ip":"88.66.55.44","pay_amt":256800,"trade_result":"ok"}
JSON
)

BASE_URL=${PULSIX_ACCESS_HTTP_BASE_URL:-http://127.0.0.1:8080}
INGEST_URL=${PULSIX_ACCESS_HTTP_INGEST_URL:-${BASE_URL}/api/access/ingest/events}
HEALTH_URL=${PULSIX_ACCESS_HTTP_HEALTH_URL:-${BASE_URL}/api/access/health}
METRICS_URL=${PULSIX_ACCESS_HTTP_METRICS_URL:-${BASE_URL}/api/access/metrics/summary}
SOURCE_CODE=${PULSIX_ACCESS_HTTP_SOURCE_CODE:-trade_http_demo}
SCENE_CODE=${PULSIX_ACCESS_HTTP_SCENE_CODE:-TRADE_RISK}
EVENT_CODE=${PULSIX_ACCESS_HTTP_EVENT_CODE:-TRADE_EVENT}
APP_KEY=${PULSIX_ACCESS_HTTP_APP_KEY:-trade-http-demo}
APP_SECRET=${PULSIX_ACCESS_HTTP_APP_SECRET:-$APP_KEY}
TIMESTAMP=${PULSIX_ACCESS_HTTP_TIMESTAMP:-$(date +%s000)}
REQUEST_ID=${PULSIX_ACCESS_HTTP_REQUEST_ID:-REQ_HTTP_SMOKE_9103}
PAYLOAD=${PULSIX_ACCESS_HTTP_PAYLOAD:-$DEFAULT_PAYLOAD}

SIGNATURE=$(printf '%s\n%s\n%s' "$APP_KEY" "$TIMESTAMP" "$PAYLOAD" \
  | openssl dgst -sha256 -hmac "$APP_SECRET" -binary \
  | od -An -vtx1 \
  | tr -d ' \n')

echo "[smoke] POST ${INGEST_URL}"
echo "[smoke] source=${SOURCE_CODE} scene=${SCENE_CODE} event=${EVENT_CODE} requestId=${REQUEST_ID}"

tmp_response=$(mktemp)
cleanup() {
  rm -f "$tmp_response"
}
trap cleanup EXIT

http_code=$(curl -sS -o "$tmp_response" -w '%{http_code}' \
  -X POST "${INGEST_URL}?sourceCode=${SOURCE_CODE}&sceneCode=${SCENE_CODE}&eventCode=${EVENT_CODE}" \
  -H 'Content-Type: application/json' \
  -H "X-Request-Id: ${REQUEST_ID}" \
  -H "X-Pulsix-Timestamp: ${TIMESTAMP}" \
  -H "X-Pulsix-Signature: ${SIGNATURE}" \
  --data-raw "$PAYLOAD")

response_body=$(cat "$tmp_response")
echo "[smoke] ingest response (${http_code}): ${response_body}"

if [[ "$http_code" != "200" ]]; then
  echo "[smoke] HTTP ingest returned unexpected status: ${http_code}" >&2
  exit 1
fi

if ! grep -Eq '"status"[[:space:]]*:[[:space:]]*"ACCEPTED"' <<<"$response_body"; then
  echo "[smoke] response does not contain ACCEPTED status" >&2
  exit 1
fi

echo "[smoke] health snapshot"
curl -fsS "$HEALTH_URL"
printf '\n'

echo "[smoke] metrics summary"
curl -fsS "$METRICS_URL"
printf '\n'

echo "[smoke] HTTP smoke passed"
