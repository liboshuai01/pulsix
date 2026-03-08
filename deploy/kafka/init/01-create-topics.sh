#!/bin/bash
set -euo pipefail

marker_file="${KAFKA_INIT_MARKER_FILE:-/var/lib/pulsix-kafka-init/.topics-initialized}"
bootstrap_server="${KAFKA_BOOTSTRAP_SERVER:-kafka:9092}"
default_partitions="${KAFKA_DEFAULT_PARTITIONS:-3}"
default_replication_factor="${KAFKA_DEFAULT_REPLICATION_FACTOR:-1}"
raw_topics="${KAFKA_INIT_TOPICS:-pulsix.event.raw,pulsix.decision.result,pulsix.decision.log,pulsix.event.dlq,pulsix.config.snapshot}"

echo "[kafka-init] 准备检查 Topic 初始化状态"

if [[ -f "${marker_file}" ]]; then
  echo "[kafka-init] 已检测到初始化标记，跳过 Topic 创建: ${marker_file}"
  exit 0
fi

mkdir -p "$(dirname "${marker_file}")"
IFS=',' read -r -a topics <<< "${raw_topics}"

for topic in "${topics[@]}"; do
  topic="$(echo "${topic}" | xargs)"
  if [[ -z "${topic}" ]]; then
    continue
  fi

  echo "[kafka-init] 创建 Topic: ${topic}"
  /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server "${bootstrap_server}" \
    --create \
    --if-not-exists \
    --topic "${topic}" \
    --partitions "${default_partitions}" \
    --replication-factor "${default_replication_factor}"
done

touch "${marker_file}"
echo "[kafka-init] Topic 初始化完成，已写入标记文件: ${marker_file}"
