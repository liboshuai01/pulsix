#!/bin/sh
set -eu

database="${MYSQL_DATABASE:-pulsix}"
base_sql_file="/opt/pulsix/init/pulsix-system-infra.sql"
risk_sql_file="/opt/pulsix/init/pulsix-risk.sql"
risk_menu_sql_file="/opt/pulsix/init/pulsix-risk-menu.sql"

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
import_sql "${risk_sql_file}" "风控 SQL"
import_sql "${risk_menu_sql_file}" "风控菜单 SQL"

echo "[mysql-init] SQL 导入完成"
