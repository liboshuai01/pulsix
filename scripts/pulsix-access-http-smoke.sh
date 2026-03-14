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

bool_true() {
  case "${1,,}" in
    1|true|yes|y|on) return 0 ;;
    *) return 1 ;;
  esac
}

json_string_value() {
  local key="$1"
  local json_text="$2"
  sed -n "s/.*\"${key}\"[[:space:]]*:[[:space:]]*\"\\([^\"]*\\)\".*/\\1/p" <<<"$json_text" | head -n1
}

sign_hex() {
  local app_key="$1"
  local app_secret="$2"
  local timestamp="$3"
  local payload="$4"
  printf '%s\n%s\n%s' "$app_key" "$timestamp" "$payload" \
    | openssl dgst -sha256 -hmac "$app_secret" -binary \
    | od -An -vtx1 \
    | tr -d ' \n'
}

verify_standard_topic() {
  local event_id="$1"
  local topic_name="$2"
  local timeout_ms="$3"
  local max_messages="$4"

  require_cmd docker
  echo "[smoke] verify Kafka topic ${topic_name} contains eventId=${event_id}"
  if ! docker compose -f "$COMPOSE_FILE" exec -T kafka bash -lc \
    "/opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:9092 --topic '${topic_name}' --from-beginning --timeout-ms ${timeout_ms} --max-messages ${max_messages}" \
      | grep -F "\"${event_id}\"" >/dev/null; then
    echo "[smoke] Kafka topic ${topic_name} does not contain expected eventId=${event_id}" >&2
    exit 1
  fi
}

verify_error_log() {
  require_cmd docker

  local timestamp="$1"
  local payload="$2"
  local signature
  signature=$(sign_hex "$APP_KEY" "$APP_SECRET" "$timestamp" "$payload")

  local tmp_bad_response
  tmp_bad_response=$(mktemp)
  local bad_http_code
  bad_http_code=$(curl -sS -o "$tmp_bad_response" -w '%{http_code}' \
    -X POST "${INGEST_URL}?sourceCode=${SOURCE_CODE}&sceneCode=${SCENE_CODE}&eventCode=${EVENT_CODE}" \
    -H 'Content-Type: application/json' \
    -H "X-Request-Id: ${ERROR_REQUEST_ID}" \
    -H "X-Pulsix-Timestamp: ${timestamp}" \
    -H "X-Pulsix-Signature: ${signature}" \
    --data-raw "$payload")

  local bad_response_body
  bad_response_body=$(cat "$tmp_bad_response")
  rm -f "$tmp_bad_response"

  echo "[smoke] reject response (${bad_http_code}): ${bad_response_body}"
  if [[ "$bad_http_code" != "200" ]]; then
    echo "[smoke] reject request returned unexpected status: ${bad_http_code}" >&2
    exit 1
  fi
  if ! grep -Eq '"status"[[:space:]]*:[[:space:]]*"REJECTED"' <<<"$bad_response_body"; then
    echo "[smoke] reject request does not contain REJECTED status" >&2
    exit 1
  fi

  sleep "${PULSIX_ACCESS_HTTP_VERIFY_ERROR_LOG_WAIT_SECONDS:-1}"

  local count
  count=$(docker compose -f "$COMPOSE_FILE" exec -T mysql sh -lc \
    "MYSQL_PWD=\"$MYSQL_ROOT_PASSWORD\" mysql -uroot -D \"$MYSQL_DATABASE\" --batch --raw --skip-column-names -e \"SELECT COUNT(*) FROM ingest_error_log WHERE trace_id='${ERROR_TRACE_ID}' AND source_code='${SOURCE_CODE}';\"")
  count=$(tr -d '[:space:]' <<<"${count}")

  echo "[smoke] ingest_error_log count for traceId=${ERROR_TRACE_ID}: ${count}"
  if [[ -z "$count" || "$count" -lt 1 ]]; then
    echo "[smoke] ingest_error_log does not contain expected traceId=${ERROR_TRACE_ID}" >&2
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

DEFAULT_ERROR_PAYLOAD=$(cat <<'JSON'
{"event_id":"E_HTTP_ERR_9103","req":{"traceId":"T_HTTP_SMOKE_ERR_9103"},"uid":"U9003","dev_id":"D9003","client_ip":"88.66.55.44","pay_amt":256800,"trade_result":"ok"}
JSON
)

DEFAULT_APP_KEY="trade-http-demo"
DEFAULT_APP_SECRET="trade-http-demo"
BASE_URL=${PULSIX_ACCESS_HTTP_BASE_URL:-http://127.0.0.1:8080}
INGEST_URL=${PULSIX_ACCESS_HTTP_INGEST_URL:-${BASE_URL}/api/access/ingest/events}
HEALTH_URL=${PULSIX_ACCESS_HTTP_HEALTH_URL:-${BASE_URL}/api/access/health}
METRICS_URL=${PULSIX_ACCESS_HTTP_METRICS_URL:-${BASE_URL}/api/access/metrics/summary}
SOURCE_CODE=${PULSIX_ACCESS_HTTP_SOURCE_CODE:-trade_http_demo}
SCENE_CODE=${PULSIX_ACCESS_HTTP_SCENE_CODE:-TRADE_RISK}
EVENT_CODE=${PULSIX_ACCESS_HTTP_EVENT_CODE:-TRADE_EVENT}
APP_KEY=${PULSIX_ACCESS_HTTP_APP_KEY:-$DEFAULT_APP_KEY}
APP_SECRET=${PULSIX_ACCESS_HTTP_APP_SECRET:-$DEFAULT_APP_SECRET}
TIMESTAMP=${PULSIX_ACCESS_HTTP_TIMESTAMP:-$(date +%s000)}
REQUEST_ID=${PULSIX_ACCESS_HTTP_REQUEST_ID:-REQ_HTTP_SMOKE_9103}
PAYLOAD=${PULSIX_ACCESS_HTTP_PAYLOAD:-$DEFAULT_PAYLOAD}
VERIFY_KAFKA=${PULSIX_ACCESS_HTTP_VERIFY_KAFKA:-false}
VERIFY_ERROR_LOG=${PULSIX_ACCESS_HTTP_VERIFY_ERROR_LOG:-false}
COMPOSE_FILE=${PULSIX_ACCESS_HTTP_COMPOSE_FILE:-deploy/docker-compose.yml}
MYSQL_DATABASE=${PULSIX_ACCESS_HTTP_MYSQL_DATABASE:-${MYSQL_DATABASE:-pulsix}}
MYSQL_ROOT_PASSWORD=${PULSIX_ACCESS_HTTP_MYSQL_ROOT_PASSWORD:-${MYSQL_ROOT_PASSWORD:-pulsix_root_123}}
STANDARD_TOPIC=${PULSIX_ACCESS_HTTP_STANDARD_TOPIC:-pulsix.event.standard}
VERIFY_EVENT_ID=${PULSIX_ACCESS_HTTP_VERIFY_EVENT_ID:-$(json_string_value event_id "$PAYLOAD")}
VERIFY_KAFKA_TIMEOUT_MS=${PULSIX_ACCESS_HTTP_VERIFY_KAFKA_TIMEOUT_MS:-8000}
VERIFY_KAFKA_MAX_MESSAGES=${PULSIX_ACCESS_HTTP_VERIFY_KAFKA_MAX_MESSAGES:-50}
ERROR_REQUEST_ID=${PULSIX_ACCESS_HTTP_ERROR_REQUEST_ID:-REQ_HTTP_SMOKE_ERR_9103}
ERROR_TRACE_ID=${PULSIX_ACCESS_HTTP_ERROR_TRACE_ID:-T_HTTP_SMOKE_ERR_9103}
ERROR_PAYLOAD=${PULSIX_ACCESS_HTTP_ERROR_PAYLOAD:-$DEFAULT_ERROR_PAYLOAD}
ERROR_TIMESTAMP=${PULSIX_ACCESS_HTTP_ERROR_TIMESTAMP:-$(date +%s000)}

if [[ "$APP_KEY" != "$DEFAULT_APP_KEY" && -z "${PULSIX_ACCESS_HTTP_APP_SECRET:-}" ]]; then
  echo "[smoke] custom APP_KEY requires explicit PULSIX_ACCESS_HTTP_APP_SECRET" >&2
  exit 1
fi

SIGNATURE=$(sign_hex "$APP_KEY" "$APP_SECRET" "$TIMESTAMP" "$PAYLOAD")

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

if bool_true "$VERIFY_KAFKA"; then
  if [[ -z "$VERIFY_EVENT_ID" ]]; then
    echo "[smoke] cannot infer event_id from payload, please set PULSIX_ACCESS_HTTP_VERIFY_EVENT_ID" >&2
    exit 1
  fi
  verify_standard_topic "$VERIFY_EVENT_ID" "$STANDARD_TOPIC" "$VERIFY_KAFKA_TIMEOUT_MS" "$VERIFY_KAFKA_MAX_MESSAGES"
fi

if bool_true "$VERIFY_ERROR_LOG"; then
  verify_error_log "$ERROR_TIMESTAMP" "$ERROR_PAYLOAD"
fi

echo "[smoke] health snapshot"
curl -fsS "$HEALTH_URL"
printf '\n'

echo "[smoke] metrics summary"
curl -fsS "$METRICS_URL"
printf '\n'

echo "[smoke] HTTP smoke passed"
