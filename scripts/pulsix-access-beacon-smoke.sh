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
  sed -n "s/.*\"${key}\"[[:space:]]*:[[:space:]]*\"\([^\"]*\)\".*/\1/p" <<<"$json_text" | head -n1
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

post_beacon() {
  local request_id="$1"
  local payload="$2"
  local output_file="$3"

  if [[ "$BEACON_MODE" == "text" ]]; then
    curl -sS -o "$output_file" -w '%{http_code}' \
      -X POST "${INGEST_URL}?sourceCode=${SOURCE_CODE}&sceneCode=${SCENE_CODE}&eventCode=${EVENT_CODE}&requestId=${request_id}" \
      -H 'Content-Type: text/plain' \
      --data-raw "$payload"
    return
  fi

  curl -sS -o "$output_file" -w '%{http_code}' \
    -X POST "${INGEST_URL}?sourceCode=${SOURCE_CODE}&sceneCode=${SCENE_CODE}&eventCode=${EVENT_CODE}" \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    --data-urlencode "requestId=${request_id}" \
    --data-urlencode "payload=${payload}"
}

verify_error_log() {
  require_cmd docker

  local tmp_bad_response
  tmp_bad_response=$(mktemp)
  local bad_http_code
  bad_http_code=$(post_beacon "$ERROR_REQUEST_ID" "$ERROR_PAYLOAD" "$tmp_bad_response")

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

  sleep "${PULSIX_ACCESS_BEACON_VERIFY_ERROR_LOG_WAIT_SECONDS:-${PULSIX_ACCESS_HTTP_VERIFY_ERROR_LOG_WAIT_SECONDS:-1}}"

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

DEFAULT_ID_SUFFIX=${PULSIX_ACCESS_BEACON_ID_SUFFIX:-${PULSIX_ACCESS_HTTP_ID_SUFFIX:-$(date +%s)}}
DEFAULT_PAYLOAD=$(cat <<JSON
{"event_id":"E_BEACON_9105_${DEFAULT_ID_SUFFIX}","occur_time_ms":1773287100000,"req":{"traceId":"T_BEACON_9105_${DEFAULT_ID_SUFFIX}"},"uid":" U9003 ","dev_id":"D9003","client_ip":"88.66.55.44","pay_amt":256800,"trade_result":"ok"}
JSON
)

DEFAULT_ERROR_PAYLOAD=$(cat <<JSON
{"event_id":"E_BEACON_ERR_9105_${DEFAULT_ID_SUFFIX}","req":{"traceId":"T_BEACON_SMOKE_ERR_9105_${DEFAULT_ID_SUFFIX}"},"uid":"U9003","dev_id":"D9003","client_ip":"88.66.55.44","pay_amt":256800,"trade_result":"ok"}
JSON
)

BASE_URL=${PULSIX_ACCESS_BEACON_BASE_URL:-${PULSIX_ACCESS_HTTP_BASE_URL:-http://127.0.0.1:8080}}
INGEST_URL=${PULSIX_ACCESS_BEACON_INGEST_URL:-${PULSIX_ACCESS_HTTP_INGEST_URL:-${BASE_URL}/api/access/ingest/beacon}}
HEALTH_URL=${PULSIX_ACCESS_BEACON_HEALTH_URL:-${PULSIX_ACCESS_HTTP_HEALTH_URL:-${BASE_URL}/api/access/health}}
METRICS_URL=${PULSIX_ACCESS_BEACON_METRICS_URL:-${PULSIX_ACCESS_HTTP_METRICS_URL:-${BASE_URL}/api/access/metrics/summary}}
SOURCE_CODE=${PULSIX_ACCESS_BEACON_SOURCE_CODE:-${PULSIX_ACCESS_HTTP_SOURCE_CODE:-trade_beacon_demo}}
SCENE_CODE=${PULSIX_ACCESS_BEACON_SCENE_CODE:-${PULSIX_ACCESS_HTTP_SCENE_CODE:-TRADE_RISK}}
EVENT_CODE=${PULSIX_ACCESS_BEACON_EVENT_CODE:-${PULSIX_ACCESS_HTTP_EVENT_CODE:-TRADE_EVENT}}
REQUEST_ID=${PULSIX_ACCESS_BEACON_REQUEST_ID:-${PULSIX_ACCESS_HTTP_REQUEST_ID:-REQ_BEACON_SMOKE_9105}}
PAYLOAD=${PULSIX_ACCESS_BEACON_PAYLOAD:-${PULSIX_ACCESS_HTTP_PAYLOAD:-$DEFAULT_PAYLOAD}}
VERIFY_KAFKA=${PULSIX_ACCESS_BEACON_VERIFY_KAFKA:-${PULSIX_ACCESS_HTTP_VERIFY_KAFKA:-false}}
VERIFY_ERROR_LOG=${PULSIX_ACCESS_BEACON_VERIFY_ERROR_LOG:-${PULSIX_ACCESS_HTTP_VERIFY_ERROR_LOG:-false}}
COMPOSE_FILE=${PULSIX_ACCESS_BEACON_COMPOSE_FILE:-${PULSIX_ACCESS_HTTP_COMPOSE_FILE:-deploy/docker-compose.yml}}
MYSQL_DATABASE=${PULSIX_ACCESS_BEACON_MYSQL_DATABASE:-${PULSIX_ACCESS_HTTP_MYSQL_DATABASE:-${MYSQL_DATABASE:-pulsix}}}
MYSQL_ROOT_PASSWORD=${PULSIX_ACCESS_BEACON_MYSQL_ROOT_PASSWORD:-${PULSIX_ACCESS_HTTP_MYSQL_ROOT_PASSWORD:-${MYSQL_ROOT_PASSWORD:-pulsix_root_123}}}
STANDARD_TOPIC=${PULSIX_ACCESS_BEACON_STANDARD_TOPIC:-${PULSIX_ACCESS_HTTP_STANDARD_TOPIC:-pulsix.event.standard}}
VERIFY_EVENT_ID=${PULSIX_ACCESS_BEACON_VERIFY_EVENT_ID:-${PULSIX_ACCESS_HTTP_VERIFY_EVENT_ID:-$(json_string_value event_id "$PAYLOAD")}}
VERIFY_KAFKA_TIMEOUT_MS=${PULSIX_ACCESS_BEACON_VERIFY_KAFKA_TIMEOUT_MS:-${PULSIX_ACCESS_HTTP_VERIFY_KAFKA_TIMEOUT_MS:-8000}}
VERIFY_KAFKA_MAX_MESSAGES=${PULSIX_ACCESS_BEACON_VERIFY_KAFKA_MAX_MESSAGES:-${PULSIX_ACCESS_HTTP_VERIFY_KAFKA_MAX_MESSAGES:-50}}
ERROR_REQUEST_ID=${PULSIX_ACCESS_BEACON_ERROR_REQUEST_ID:-${PULSIX_ACCESS_HTTP_ERROR_REQUEST_ID:-REQ_BEACON_SMOKE_ERR_9105}}
ERROR_TRACE_ID=${PULSIX_ACCESS_BEACON_ERROR_TRACE_ID:-${PULSIX_ACCESS_HTTP_ERROR_TRACE_ID:-T_BEACON_SMOKE_ERR_9105_${DEFAULT_ID_SUFFIX}}}
ERROR_PAYLOAD=${PULSIX_ACCESS_BEACON_ERROR_PAYLOAD:-${PULSIX_ACCESS_HTTP_ERROR_PAYLOAD:-$DEFAULT_ERROR_PAYLOAD}}
BEACON_MODE=${PULSIX_ACCESS_BEACON_MODE:-form}

if [[ "$BEACON_MODE" != "form" && "$BEACON_MODE" != "text" ]]; then
  echo "[smoke] unsupported beacon mode: ${BEACON_MODE} (expected form|text)" >&2
  exit 1
fi

echo "[smoke] POST ${INGEST_URL}"
echo "[smoke] source=${SOURCE_CODE} scene=${SCENE_CODE} event=${EVENT_CODE} requestId=${REQUEST_ID} mode=${BEACON_MODE}"

tmp_response=$(mktemp)
cleanup() {
  rm -f "$tmp_response"
}
trap cleanup EXIT

http_code=$(post_beacon "$REQUEST_ID" "$PAYLOAD" "$tmp_response")
response_body=$(cat "$tmp_response")
echo "[smoke] ingest response (${http_code}): ${response_body}"

if [[ "$http_code" != "200" ]]; then
  echo "[smoke] Beacon ingest returned unexpected status: ${http_code}" >&2
  exit 1
fi

if ! grep -Eq '"status"[[:space:]]*:[[:space:]]*"ACCEPTED"' <<<"$response_body"; then
  echo "[smoke] response does not contain ACCEPTED status" >&2
  exit 1
fi

if bool_true "$VERIFY_KAFKA"; then
  if [[ -z "$VERIFY_EVENT_ID" ]]; then
    echo "[smoke] cannot infer event_id from payload, please set PULSIX_ACCESS_BEACON_VERIFY_EVENT_ID" >&2
    exit 1
  fi
  verify_standard_topic "$VERIFY_EVENT_ID" "$STANDARD_TOPIC" "$VERIFY_KAFKA_TIMEOUT_MS" "$VERIFY_KAFKA_MAX_MESSAGES"
fi

if bool_true "$VERIFY_ERROR_LOG"; then
  verify_error_log
fi

echo "[smoke] health snapshot"
curl -fsS "$HEALTH_URL"
printf '\n'

echo "[smoke] metrics summary"
curl -fsS "$METRICS_URL"
printf '\n'

echo "[smoke] Beacon smoke passed"
