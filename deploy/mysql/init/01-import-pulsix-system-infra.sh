#!/bin/sh
set -eu

sql_file="/opt/pulsix/init/pulsix-system-infra.sql"
database="${MYSQL_DATABASE:-pulsix}"

if [ ! -f "${sql_file}" ]; then
  echo "[mysql-init] SQL 文件不存在: ${sql_file}" >&2
  exit 1
fi

echo "[mysql-init] 首次初始化，开始导入 ${sql_file} -> ${database}"
mysql \
  --protocol=socket \
  --default-character-set=utf8mb4 \
  -uroot \
  -p"${MYSQL_ROOT_PASSWORD}" \
  --database="${database}" \
  < "${sql_file}"
echo "[mysql-init] SQL 导入完成"
