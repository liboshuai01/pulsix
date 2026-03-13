#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
cd "$ROOT_DIR"

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

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "[smoke] missing command: $1" >&2
    exit 1
  fi
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

DEFAULT_PAYLOAD=$(cat <<'JSON'
{"event_id":"E_SDK_SMOKE_9104","occur_time_ms":1773287100000,"req":{"traceId":"T_SDK_SMOKE_9104"},"uid":" U9004 ","dev_id":"D9004","client_ip":"88.66.55.45","pay_amt":128800,"trade_result":"ok"}
JSON
)

HOST=${PULSIX_ACCESS_SDK_HOST:-127.0.0.1}
PORT=${PULSIX_ACCESS_SDK_PORT:-19100}
SOURCE_CODE=${PULSIX_ACCESS_SDK_SOURCE_CODE:-trade_sdk_demo}
SCENE_CODE=${PULSIX_ACCESS_SDK_SCENE_CODE:-TRADE_RISK}
EVENT_CODE=${PULSIX_ACCESS_SDK_EVENT_CODE:-TRADE_EVENT}
REQUEST_ID=${PULSIX_ACCESS_SDK_REQUEST_ID:-REQ_SDK_SMOKE_9104}
AUTHORIZATION=${PULSIX_ACCESS_SDK_AUTHORIZATION:-Bearer token-demo-001}
PAYLOAD=${PULSIX_ACCESS_SDK_PAYLOAD:-$DEFAULT_PAYLOAD}
VERIFY_KAFKA=${PULSIX_ACCESS_SDK_VERIFY_KAFKA:-false}
COMPOSE_FILE=${PULSIX_ACCESS_SDK_COMPOSE_FILE:-deploy/docker-compose.yml}
STANDARD_TOPIC=${PULSIX_ACCESS_SDK_STANDARD_TOPIC:-pulsix.event.standard}
VERIFY_EVENT_ID=${PULSIX_ACCESS_SDK_VERIFY_EVENT_ID:-$(json_string_value event_id "$PAYLOAD")}
VERIFY_KAFKA_TIMEOUT_MS=${PULSIX_ACCESS_SDK_VERIFY_KAFKA_TIMEOUT_MS:-8000}
VERIFY_KAFKA_MAX_MESSAGES=${PULSIX_ACCESS_SDK_VERIFY_KAFKA_MAX_MESSAGES:-50}

CMD=(
  mvn -q
  -pl pulsix-access/pulsix-sdk
  -am
  test
  -Dtest=NettyPulsixSdkSmokeTest
  -Dsurefire.failIfNoSpecifiedTests=false
  -Dpulsix.access.sdk.smoke.enabled=true
  "-Dpulsix.access.sdk.smoke.host=${HOST}"
  "-Dpulsix.access.sdk.smoke.port=${PORT}"
  "-Dpulsix.access.sdk.smoke.source-code=${SOURCE_CODE}"
  "-Dpulsix.access.sdk.smoke.scene-code=${SCENE_CODE}"
  "-Dpulsix.access.sdk.smoke.event-code=${EVENT_CODE}"
  "-Dpulsix.access.sdk.smoke.request-id=${REQUEST_ID}"
  "-Dpulsix.access.sdk.smoke.authorization=${AUTHORIZATION}"
  "-Dpulsix.access.sdk.smoke.payload=${PAYLOAD}"
)

echo "[smoke] SDK target ${HOST}:${PORT}"
echo "[smoke] source=${SOURCE_CODE} scene=${SCENE_CODE} event=${EVENT_CODE} requestId=${REQUEST_ID}"
"${CMD[@]}"

if bool_true "$VERIFY_KAFKA"; then
  if [[ -z "$VERIFY_EVENT_ID" ]]; then
    echo "[smoke] cannot infer event_id from payload, please set PULSIX_ACCESS_SDK_VERIFY_EVENT_ID" >&2
    exit 1
  fi
  verify_standard_topic "$VERIFY_EVENT_ID" "$STANDARD_TOPIC" "$VERIFY_KAFKA_TIMEOUT_MS" "$VERIFY_KAFKA_MAX_MESSAGES"
fi

echo "[smoke] SDK smoke passed"
