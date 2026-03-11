#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
CP_FILE=${PULSIX_ENGINE_CP_FILE:-/tmp/pulsix-engine.cp}
OUT_FILE=${PULSIX_ENGINE_SMOKE_OUT_FILE:-/tmp/pulsix-engine-job-smoke.out}
LOG_FILE=${PULSIX_ENGINE_SMOKE_LOG_FILE:-/tmp/pulsix-engine-job-smoke.log}
BLOB_PORT_RANGE=${PULSIX_ENGINE_BLOB_PORT_RANGE:-46000-46020}
TIMEOUT_SECONDS=${PULSIX_ENGINE_SMOKE_TIMEOUT_SECONDS:-60}

cd "$ROOT_DIR"

if [[ ! -d pulsix-engine/target/classes || ! -d pulsix-framework/pulsix-kernel/target/classes ]]; then
  echo "[smoke] compiling pulsix-engine and pulsix-kernel classes..."
  mvn -q -pl pulsix-framework/pulsix-kernel,pulsix-engine -am -DskipTests compile
fi

if [[ ! -f "$CP_FILE" ]]; then
  echo "[smoke] building runtime classpath at $CP_FILE ..."
  mvn -q -pl pulsix-engine -am -DskipTests dependency:build-classpath -Dmdep.outputFile="$CP_FILE"
fi

JAVA_CP="pulsix-engine/target/classes:pulsix-framework/pulsix-kernel/target/classes:$(cat "$CP_FILE")"
CMD=(
  java
  "-Dpulsix.engine.blob-server-port-range=${BLOB_PORT_RANGE}"
  -cp
  "$JAVA_CP"
  cn.liboshuai.pulsix.engine.flink.DecisionEngineJob
  --local-log-file
  "$LOG_FILE"
)

if [[ $# -gt 0 ]]; then
  CMD+=("$@")
fi

echo "[smoke] writing stdout to $OUT_FILE"
echo "[smoke] writing flink log to $LOG_FILE"
echo "[smoke] blob server port range: $BLOB_PORT_RANGE"

set +e
timeout "${TIMEOUT_SECONDS}s" "${CMD[@]}" >"$OUT_FILE" 2>&1
STATUS=$?
set -e

if [[ $STATUS -ne 0 && $STATUS -ne 124 ]]; then
  echo "[smoke] DecisionEngineJob failed with exit code $STATUS"
  tail -n 120 "$OUT_FILE" || true
  exit $STATUS
fi

RESULT_COUNT=$(grep -c '^decision-result>' "$OUT_FILE" || true)
LOG_COUNT=$(grep -c '^decision-log>' "$OUT_FILE" || true)
ERROR_COUNT=$(grep -c '^engine-error>' "$OUT_FILE" || true)
FINAL_RESULT=$(grep '^decision-result>' "$OUT_FILE" | tail -n 1 || true)

echo "[smoke] timeout exit code: $STATUS"
echo "[smoke] decision-result count: $RESULT_COUNT"
echo "[smoke] decision-log count: $LOG_COUNT"
echo "[smoke] engine-error count: $ERROR_COUNT"
if [[ -n "$FINAL_RESULT" ]]; then
  echo "[smoke] final decision:"
  echo "$FINAL_RESULT"
fi
