#!/bin/bash
set -euo pipefail

host="${DORIS_HOST:-doris-fe}"
port="${DORIS_PORT:-9030}"
user="${DORIS_USER:-root}"
password="${DORIS_PASSWORD:-}"
database="${DORIS_DATABASE:-pulsix_olap}"
sql_file="${DORIS_INIT_SQL_FILE:-/opt/pulsix/sql/01-pulsix-olap.sql}"
max_attempts="${DORIS_INIT_MAX_ATTEMPTS:-30}"
retry_interval_seconds="${DORIS_INIT_RETRY_INTERVAL_SECONDS:-5}"

if [[ ! -f "${sql_file}" ]]; then
  echo "[doris-init] SQL 文件不存在: ${sql_file}"
  exit 1
fi

mysql_args=(
  --protocol=TCP
  --connect-timeout=5
  -h "${host}"
  -P "${port}"
  -u "${user}"
)

if [[ -n "${password}" ]]; then
  mysql_args+=("-p${password}")
fi

attempt=1
until mysql "${mysql_args[@]}" -e "SELECT 1" >/dev/null 2>&1; do
  if (( attempt >= max_attempts )); then
    echo "[doris-init] Doris 连接检查失败，已达到最大重试次数: ${max_attempts}"
    exit 1
  fi

  echo "[doris-init] Doris 尚未准备好，${retry_interval_seconds}s 后进行第 $((attempt + 1)) 次重试"
  attempt=$((attempt + 1))
  sleep "${retry_interval_seconds}"
done

echo "[doris-init] Doris 已就绪，开始建库建表"
mysql "${mysql_args[@]}" -e "CREATE DATABASE IF NOT EXISTS \`${database}\`;"

if ! mysql "${mysql_args[@]}" "${database}" < "${sql_file}"; then
  echo "[doris-init] SQL 执行失败，请检查 ${sql_file}"
  exit 1
fi

echo "[doris-init] Doris 建库建表完成"
