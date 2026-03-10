#!/bin/bash
set -euo pipefail

bootstrap_server="${KAFKA_BOOTSTRAP_SERVER:-kafka:9092}"
default_partitions="${KAFKA_DEFAULT_PARTITIONS:-1}"
default_replication_factor="${KAFKA_DEFAULT_REPLICATION_FACTOR:-1}"
raw_topic_specs="${KAFKA_INIT_TOPIC_SPECS:-}"
raw_topics="${KAFKA_INIT_TOPICS:-pulsix.event.standard,pulsix.decision.result,pulsix.decision.log,pulsix.engine.error,pulsix.event.dlq,pulsix.ingest.error}"

echo "[kafka-init] 准备确保默认 Topic 存在，bootstrap-server: ${bootstrap_server}"

create_topic() {
  local topic="$1"
  local partitions="$2"
  local replication_factor="$3"

  echo "[kafka-init] 确保 Topic 存在: ${topic} (partitions=${partitions}, replicas=${replication_factor})"
  /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server "${bootstrap_server}" \
    --create \
    --if-not-exists \
    --topic "${topic}" \
    --partitions "${partitions}" \
    --replication-factor "${replication_factor}"
}

if [[ -n "${raw_topic_specs}" ]]; then
  IFS=',' read -r -a topic_specs <<< "${raw_topic_specs}"

  for topic_spec in "${topic_specs[@]}"; do
    topic_spec="$(echo "${topic_spec}" | xargs)"
    if [[ -z "${topic_spec}" ]]; then
      continue
    fi

    IFS=':' read -r topic partitions replication_factor <<< "${topic_spec}"
    topic="$(echo "${topic}" | xargs)"
    partitions="$(echo "${partitions:-${default_partitions}}" | xargs)"
    replication_factor="$(echo "${replication_factor:-${default_replication_factor}}" | xargs)"

    if [[ -z "${topic}" ]]; then
      continue
    fi

    create_topic "${topic}" "${partitions}" "${replication_factor}"
  done
else
  IFS=',' read -r -a topics <<< "${raw_topics}"

  for topic in "${topics[@]}"; do
    topic="$(echo "${topic}" | xargs)"
    if [[ -z "${topic}" ]]; then
      continue
    fi

    create_topic "${topic}" "${default_partitions}" "${default_replication_factor}"
  done
fi

echo "[kafka-init] Topic 初始化完成"
