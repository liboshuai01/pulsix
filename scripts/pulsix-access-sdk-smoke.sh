#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
cd "$ROOT_DIR"

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
echo "[smoke] SDK smoke passed"
