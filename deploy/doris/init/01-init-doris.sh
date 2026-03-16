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

get_alive_backend_count() {
  local backends_output
  local alive_column

  backends_output="$(mysql "${mysql_args[@]}" -B -e "SHOW BACKENDS;" 2>/dev/null || true)"
  alive_column="$(printf '%s\n' "${backends_output}" | awk -F '\t' 'NR == 1 { for (i = 1; i <= NF; i++) if ($i == "Alive") { print i; exit } }')"

  if [[ -z "${alive_column}" ]]; then
    echo 0
    return
  fi

  printf '%s\n' "${backends_output}" \
    | awk -F '\t' -v alive_column="${alive_column}" '
        NR > 1 && tolower($alive_column) == "true" { count++ }
        END { print count + 0 }
      '
}

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

attempt=1
until [[ "$(get_alive_backend_count)" -ge 1 ]]; do
  if (( attempt >= max_attempts )); then
    echo "[doris-init] FE 已就绪，但在最大重试次数内未检测到存活的 BE"
    mysql "${mysql_args[@]}" -e "SHOW BACKENDS;" || true
    exit 1
  fi

  echo "[doris-init] FE 已就绪，但 BE 尚未注册成功，${retry_interval_seconds}s 后进行第 $((attempt + 1)) 次重试"
  attempt=$((attempt + 1))
  sleep "${retry_interval_seconds}"
done

echo "[doris-init] Doris FE/BE 已就绪，开始建库建表"
mysql "${mysql_args[@]}" -e "CREATE DATABASE IF NOT EXISTS \`${database}\`;"

if ! mysql "${mysql_args[@]}" "${database}" < "${sql_file}"; then
  echo "[doris-init] SQL 执行失败，请检查 ${sql_file}"
  exit 1
fi

echo "[doris-init] Doris 建库建表完成"
