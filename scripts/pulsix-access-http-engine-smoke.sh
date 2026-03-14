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

trim_text() {
  local value="$1"
  value="${value#${value%%[![:space:]]*}}"
  value="${value%${value##*[![:space:]]}}"
  printf '%s' "$value"
}

ensure_ingest_ready() {
  if curl -fsS "$HEALTH_URL" >/dev/null 2>&1; then
    return
  fi
  echo "[smoke] pulsix-ingest is not reachable: ${HEALTH_URL}" >&2
  echo "[smoke] start pulsix-ingest first, then rerun this script" >&2
  exit 1
}

wait_for_engine_bootstrap() {
  local wait_seconds="$1"
  if [[ "$wait_seconds" -le 0 ]]; then
    return
  fi
  echo "[smoke] waiting ${wait_seconds}s for DecisionEngineJob to subscribe Kafka"
  sleep "$wait_seconds"
}

require_cmd bash
require_cmd curl
require_cmd grep
require_cmd sed
require_cmd timeout

DEFAULT_ID_SUFFIX=${PULSIX_ACCESS_HTTP_ENGINE_ID_SUFFIX:-$(date +%s)}
DEFAULT_PAYLOAD=$(cat <<JSON
{"event_id":"E_RAW_9103_${DEFAULT_ID_SUFFIX}","occur_time_ms":1773287100000,"req":{"traceId":"T_RAW_9103_${DEFAULT_ID_SUFFIX}"},"uid":" U9003 ","dev_id":"D9003","client_ip":"88.66.55.44","pay_amt":256800,"trade_result":"ok"}
JSON
)

BASE_URL=${PULSIX_ACCESS_HTTP_ENGINE_BASE_URL:-http://127.0.0.1:8080}
HEALTH_URL=${PULSIX_ACCESS_HTTP_ENGINE_HEALTH_URL:-${BASE_URL}/api/access/health}
HTTP_SMOKE_SCRIPT=${PULSIX_ACCESS_HTTP_ENGINE_HTTP_SMOKE_SCRIPT:-scripts/pulsix-access-http-smoke.sh}
ENGINE_SMOKE_SCRIPT=${PULSIX_ACCESS_HTTP_ENGINE_ENGINE_SMOKE_SCRIPT:-scripts/decision-engine-job-demo-smoke.sh}
COMPOSE_FILE=${PULSIX_ACCESS_HTTP_ENGINE_COMPOSE_FILE:-deploy/docker-compose.yml}
PAYLOAD=${PULSIX_ACCESS_HTTP_ENGINE_PAYLOAD:-$DEFAULT_PAYLOAD}
EVENT_ID=${PULSIX_ACCESS_HTTP_ENGINE_EVENT_ID:-$(json_string_value event_id "$PAYLOAD")}
if [[ -z "$EVENT_ID" ]]; then
  EVENT_ID=$(json_string_value eventId "$PAYLOAD")
fi
TRACE_ID=${PULSIX_ACCESS_HTTP_ENGINE_TRACE_ID:-$(json_string_value traceId "$PAYLOAD")}
REQUEST_ID=${PULSIX_ACCESS_HTTP_ENGINE_REQUEST_ID:-REQ_HTTP_ENGINE_SMOKE_9103}
SOURCE_CODE=${PULSIX_ACCESS_HTTP_ENGINE_SOURCE_CODE:-trade_http_demo}
SCENE_CODE=${PULSIX_ACCESS_HTTP_ENGINE_SCENE_CODE:-TRADE_RISK}
EVENT_CODE=${PULSIX_ACCESS_HTTP_ENGINE_EVENT_CODE:-TRADE_EVENT}
STANDARD_TOPIC=${PULSIX_ACCESS_HTTP_ENGINE_STANDARD_TOPIC:-pulsix.event.standard}
VERIFY_KAFKA=${PULSIX_ACCESS_HTTP_ENGINE_VERIFY_KAFKA:-true}
VERIFY_ERROR_LOG=${PULSIX_ACCESS_HTTP_ENGINE_VERIFY_ERROR_LOG:-false}
EXPECTED_FINAL_ACTION=${PULSIX_ACCESS_HTTP_ENGINE_EXPECTED_FINAL_ACTION:-PASS}
ALLOW_FALLBACK_ENGINE_ERROR=${PULSIX_ACCESS_HTTP_ENGINE_ALLOW_FALLBACK_ENGINE_ERROR:-true}
EXPECTED_FALLBACK_ERROR_CODE=${PULSIX_ACCESS_HTTP_ENGINE_EXPECTED_FALLBACK_ERROR_CODE:-LOOKUP_VALUE_MISSING}
EXPECTED_FALLBACK_MODE=${PULSIX_ACCESS_HTTP_ENGINE_EXPECTED_FALLBACK_MODE:-DEFAULT_VALUE}
SNAPSHOT_SCENE_CODE=${PULSIX_ACCESS_HTTP_ENGINE_SNAPSHOT_SCENE_CODE:-TRADE_RISK}
SNAPSHOT_VERSION=${PULSIX_ACCESS_HTTP_ENGINE_SNAPSHOT_VERSION:-14}
EXPECTED_VERSION=${PULSIX_ACCESS_HTTP_ENGINE_EXPECTED_VERSION:-$SNAPSHOT_VERSION}
KAFKA_BOOTSTRAP_SERVERS=${PULSIX_ACCESS_HTTP_ENGINE_KAFKA_BOOTSTRAP_SERVERS:-127.0.0.1:29092}
ENGINE_GROUP_ID=${PULSIX_ACCESS_HTTP_ENGINE_GROUP_ID:-pulsix-engine-http-smoke-$(date +%s)}
MYSQL_HOST=${PULSIX_ACCESS_HTTP_ENGINE_MYSQL_HOST:-127.0.0.1}
MYSQL_PORT=${PULSIX_ACCESS_HTTP_ENGINE_MYSQL_PORT:-3306}
MYSQL_DATABASE=${PULSIX_ACCESS_HTTP_ENGINE_MYSQL_DATABASE:-pulsix}
MYSQL_USER=${PULSIX_ACCESS_HTTP_ENGINE_MYSQL_USER:-pulsix}
MYSQL_PASSWORD=${PULSIX_ACCESS_HTTP_ENGINE_MYSQL_PASSWORD:-pulsix_123}
MYSQL_JDBC_PARAMS=${PULSIX_ACCESS_HTTP_ENGINE_MYSQL_JDBC_PARAMS:-useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&nullCatalogMeansCurrent=true&rewriteBatchedStatements=true}
REDIS_HOST=${PULSIX_ACCESS_HTTP_ENGINE_REDIS_HOST:-127.0.0.1}
REDIS_PORT=${PULSIX_ACCESS_HTTP_ENGINE_REDIS_PORT:-6379}
REDIS_DATABASE=${PULSIX_ACCESS_HTTP_ENGINE_REDIS_DATABASE:-0}
REDIS_PASSWORD=${PULSIX_ACCESS_HTTP_ENGINE_REDIS_PASSWORD:-pulsix_redis_123}
ENGINE_OUT_FILE=${PULSIX_ACCESS_HTTP_ENGINE_OUT_FILE:-/tmp/pulsix-http-engine-smoke.out}
ENGINE_LOG_FILE=${PULSIX_ACCESS_HTTP_ENGINE_LOG_FILE:-/tmp/pulsix-http-engine-smoke.log}
ENGINE_CONFIG_FILE=${PULSIX_ACCESS_HTTP_ENGINE_CONFIG_FILE:-/tmp/pulsix-http-engine-smoke.properties}
ENGINE_TIMEOUT_SECONDS=${PULSIX_ACCESS_HTTP_ENGINE_TIMEOUT_SECONDS:-60}
ENGINE_START_WAIT_SECONDS=${PULSIX_ACCESS_HTTP_ENGINE_START_WAIT_SECONDS:-10}
ENGINE_STARTING_OFFSETS=${PULSIX_ACCESS_HTTP_ENGINE_STARTING_OFFSETS:-earliest}

if [[ -z "$EVENT_ID" || -z "$TRACE_ID" ]]; then
  echo "[smoke] cannot infer eventId / traceId from payload, please set PULSIX_ACCESS_HTTP_ENGINE_EVENT_ID and PULSIX_ACCESS_HTTP_ENGINE_TRACE_ID" >&2
  exit 1
fi

ensure_ingest_ready

cat > "$ENGINE_CONFIG_FILE" <<CONFIG
pulsix.engine.job.name=pulsix-http-kafka-engine-smoke
pulsix.engine.parallelism=1
pulsix.engine.object-reuse-enabled=true
pulsix.engine.state-backend=hashmap
pulsix.engine.checkpoint.interval-ms=30000
pulsix.engine.checkpoint.min-pause-ms=5000
pulsix.engine.checkpoint.timeout-ms=60000
pulsix.engine.checkpoint.tolerable-failure-number=3
pulsix.engine.event.out-of-orderness-seconds=1
pulsix.engine.task.cpu-cores=1.0
pulsix.engine.task.slots=1
pulsix.engine.task.heap-mb=256
pulsix.engine.task.off-heap-mb=128
pulsix.engine.task.network-mb=64
pulsix.engine.task.managed-mb=128
pulsix.engine.snapshot-source=jdbc
pulsix.engine.snapshot-jdbc-url=jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}/${MYSQL_DATABASE}?${MYSQL_JDBC_PARAMS}
pulsix.engine.snapshot-jdbc-user=${MYSQL_USER}
pulsix.engine.snapshot-jdbc-password=${MYSQL_PASSWORD}
pulsix.engine.snapshot-scene-code=${SNAPSHOT_SCENE_CODE}
pulsix.engine.snapshot-version=${SNAPSHOT_VERSION}
pulsix.engine.event-source=kafka
pulsix.engine.kafka-bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS}
pulsix.engine.event-kafka-topic=${STANDARD_TOPIC}
pulsix.engine.event-kafka-group-id=${ENGINE_GROUP_ID}
pulsix.engine.event-kafka-starting-offsets=${ENGINE_STARTING_OFFSETS}
pulsix.engine.lookup-source=redis
pulsix.engine.lookup-redis-host=${REDIS_HOST}
pulsix.engine.lookup-redis-port=${REDIS_PORT}
pulsix.engine.lookup-redis-database=${REDIS_DATABASE}
pulsix.engine.lookup-redis-password=${REDIS_PASSWORD}
pulsix.engine.output-sink=print
CONFIG

rm -f "$ENGINE_OUT_FILE" "$ENGINE_LOG_FILE"

echo "[smoke] engine config: ${ENGINE_CONFIG_FILE}"
echo "[smoke] engine stdout: ${ENGINE_OUT_FILE}"
echo "[smoke] engine local log: ${ENGINE_LOG_FILE}"

PULSIX_ENGINE_SMOKE_OUT_FILE="$ENGINE_OUT_FILE" \
PULSIX_ENGINE_SMOKE_LOG_FILE="$ENGINE_LOG_FILE" \
PULSIX_ENGINE_SMOKE_TIMEOUT_SECONDS="$ENGINE_TIMEOUT_SECONDS" \
PULSIX_ENGINE_SMOKE_COMPOSE_FILE="$COMPOSE_FILE" \
bash "$ENGINE_SMOKE_SCRIPT" --config-file "$ENGINE_CONFIG_FILE" &
ENGINE_PID=$!

cleanup() {
  local status=$?
  if [[ -n "${ENGINE_PID:-}" ]] && kill -0 "$ENGINE_PID" >/dev/null 2>&1; then
    kill "$ENGINE_PID" >/dev/null 2>&1 || true
    wait "$ENGINE_PID" >/dev/null 2>&1 || true
  fi
  return "$status"
}
trap cleanup EXIT

wait_for_engine_bootstrap "$ENGINE_START_WAIT_SECONDS"

PULSIX_ACCESS_HTTP_BASE_URL="$BASE_URL" \
PULSIX_ACCESS_HTTP_REQUEST_ID="$REQUEST_ID" \
PULSIX_ACCESS_HTTP_PAYLOAD="$PAYLOAD" \
PULSIX_ACCESS_HTTP_SOURCE_CODE="$SOURCE_CODE" \
PULSIX_ACCESS_HTTP_SCENE_CODE="$SCENE_CODE" \
PULSIX_ACCESS_HTTP_EVENT_CODE="$EVENT_CODE" \
PULSIX_ACCESS_HTTP_STANDARD_TOPIC="$STANDARD_TOPIC" \
PULSIX_ACCESS_HTTP_VERIFY_EVENT_ID="$EVENT_ID" \
PULSIX_ACCESS_HTTP_VERIFY_KAFKA="$VERIFY_KAFKA" \
PULSIX_ACCESS_HTTP_VERIFY_ERROR_LOG="$VERIFY_ERROR_LOG" \
bash "$HTTP_SMOKE_SCRIPT"

wait "$ENGINE_PID"

if [[ ! -f "$ENGINE_OUT_FILE" ]]; then
  echo "[smoke] engine output file not found: ${ENGINE_OUT_FILE}" >&2
  exit 1
fi

RESULT_LINE=$(grep '^decision-result>' "$ENGINE_OUT_FILE" | grep "traceId=${TRACE_ID}" | tail -n 1 || true)
LOG_LINE=$(grep '^decision-log>' "$ENGINE_OUT_FILE" | grep "traceId=${TRACE_ID}" | tail -n 1 || true)
ERROR_LINES=$(grep '^engine-error>' "$ENGINE_OUT_FILE" | grep "traceId=${TRACE_ID}" || true)

if [[ -z "$RESULT_LINE" ]]; then
  echo "[smoke] DecisionResult not found for traceId=${TRACE_ID}" >&2
  tail -n 120 "$ENGINE_OUT_FILE" || true
  exit 1
fi
if [[ -z "$LOG_LINE" ]]; then
  echo "[smoke] DecisionLogRecord not found for traceId=${TRACE_ID}" >&2
  tail -n 120 "$ENGINE_OUT_FILE" || true
  exit 1
fi
if [[ -n "$ERROR_LINES" ]]; then
  if bool_true "$ALLOW_FALLBACK_ENGINE_ERROR"; then
    while IFS= read -r error_line; do
      [[ -z "$error_line" ]] && continue
      if ! grep -F "errorCode=${EXPECTED_FALLBACK_ERROR_CODE}" <<<"$error_line" >/dev/null \
        || ! grep -F "fallbackMode=${EXPECTED_FALLBACK_MODE}" <<<"$error_line" >/dev/null; then
        echo "[smoke] unexpected EngineErrorRecord found: ${error_line}" >&2
        tail -n 120 "$ENGINE_OUT_FILE" || true
        exit 1
      fi
    done <<<"$ERROR_LINES"
    echo "[smoke] allowed fallback EngineErrorRecord(s):"
    printf '%s\n' "$ERROR_LINES"
  else
    echo "[smoke] unexpected EngineErrorRecord found: ${ERROR_LINES}" >&2
    tail -n 120 "$ENGINE_OUT_FILE" || true
    exit 1
  fi
fi
if ! grep -F "eventId=${EVENT_ID}" <<<"$RESULT_LINE" >/dev/null; then
  echo "[smoke] DecisionResult eventId mismatch: expected ${EVENT_ID}" >&2
  echo "$RESULT_LINE" >&2
  exit 1
fi
if ! grep -F "version=${EXPECTED_VERSION}" <<<"$RESULT_LINE" >/dev/null; then
  echo "[smoke] DecisionResult version mismatch: expected ${EXPECTED_VERSION}" >&2
  echo "$RESULT_LINE" >&2
  exit 1
fi
if ! grep -F "finalAction=${EXPECTED_FINAL_ACTION}" <<<"$RESULT_LINE" >/dev/null; then
  echo "[smoke] DecisionResult finalAction mismatch: expected ${EXPECTED_FINAL_ACTION}" >&2
  echo "$RESULT_LINE" >&2
  exit 1
fi
if ! grep -F "finalAction=${EXPECTED_FINAL_ACTION}" <<<"$LOG_LINE" >/dev/null; then
  echo "[smoke] DecisionLogRecord finalAction mismatch: expected ${EXPECTED_FINAL_ACTION}" >&2
  echo "$LOG_LINE" >&2
  exit 1
fi

echo "[smoke] matched DecisionResult: ${RESULT_LINE}"
echo "[smoke] matched DecisionLogRecord: ${LOG_LINE}"
echo "[smoke] HTTP -> Kafka -> Engine smoke passed"
