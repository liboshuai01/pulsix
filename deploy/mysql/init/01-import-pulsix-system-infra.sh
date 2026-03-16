#!/bin/sh
set -eu

database="${MYSQL_DATABASE:-pulsix}"
base_sql_file="/opt/pulsix/init/pulsix-system-infra.sql"
risk_sql_file="/opt/pulsix/init/pulsix-risk.sql"

import_sql() {
  sql_file="$1"
  label="$2"

  if [ ! -f "${sql_file}" ]; then
    echo "[mysql-init] SQL 文件不存在: ${sql_file}" >&2
    exit 1
  fi

  echo "[mysql-init] 首次初始化，开始导入 ${label}: ${sql_file} -> ${database}"
  mysql \
    --protocol=socket \
    --default-character-set=utf8mb4 \
    -uroot \
    -p"${MYSQL_ROOT_PASSWORD}" \
    --database="${database}" \
    < "${sql_file}"
}

import_sql "${base_sql_file}" "基础设施 SQL"

if [ -f "${risk_sql_file}" ]; then
  import_sql "${risk_sql_file}" "风控 SQL"
else
  echo "[mysql-init] 未检测到风控 SQL，跳过: ${risk_sql_file}"
fi

echo "[mysql-init] SQL 导入完成"
