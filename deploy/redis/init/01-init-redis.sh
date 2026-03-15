#!/bin/sh
set -eu

host="${REDIS_HOST:-redis}"
port="${REDIS_PORT:-6379}"
password="${REDIS_PASSWORD:-}"
database="${REDIS_DATABASE:-0}"
marker_key="${REDIS_INIT_MARKER_KEY:-pulsix:meta:seed:redis:v1}"
max_attempts="${REDIS_INIT_MAX_ATTEMPTS:-30}"
retry_interval_seconds="${REDIS_INIT_RETRY_INTERVAL_SECONDS:-2}"
force_init="${REDIS_INIT_FORCE:-false}"

string_no_ttl_count=0
string_ttl_count=0
hash_count=0
set_count=0
ttl_repair_count=0

redis_cli() {
  if [ -n "${password}" ]; then
    redis-cli --no-auth-warning -h "${host}" -p "${port}" -n "${database}" -a "${password}" "$@"
    return
  fi

  redis-cli -h "${host}" -p "${port}" -n "${database}" "$@"
}

ensure_expire() {
  redis_key="$1"
  redis_ttl="$2"

  if [ "${redis_ttl}" -le 0 ]; then
    return
  fi

  redis_type="$(redis_cli TYPE "${redis_key}")"
  if [ "${redis_type}" = "none" ]; then
    return
  fi

  current_ttl="$(redis_cli TTL "${redis_key}")"
  if [ "${current_ttl}" -lt 0 ]; then
    redis_cli EXPIRE "${redis_key}" "${redis_ttl}" >/dev/null
    ttl_repair_count=$((ttl_repair_count + 1))
  fi
}

set_string() {
  redis_key="$1"
  redis_value="$2"
  redis_ttl="${3:-0}"

  if [ "${force_init}" = "true" ]; then
    if [ "${redis_ttl}" -gt 0 ]; then
      redis_cli SETEX "${redis_key}" "${redis_ttl}" "${redis_value}" >/dev/null
      string_ttl_count=$((string_ttl_count + 1))
      return
    fi

    redis_cli SET "${redis_key}" "${redis_value}" >/dev/null
    string_no_ttl_count=$((string_no_ttl_count + 1))
    return
  fi

  if [ "${redis_ttl}" -gt 0 ]; then
    key_exists="$(redis_cli EXISTS "${redis_key}")"
    if [ "${key_exists}" -eq 0 ]; then
      redis_cli SETEX "${redis_key}" "${redis_ttl}" "${redis_value}" >/dev/null
      string_ttl_count=$((string_ttl_count + 1))
      return
    fi

    ensure_expire "${redis_key}" "${redis_ttl}"
    return
  fi

  written="$(redis_cli SETNX "${redis_key}" "${redis_value}")"
  if [ "${written}" -eq 1 ]; then
    string_no_ttl_count=$((string_no_ttl_count + 1))
  fi
}

set_hash() {
  redis_key="$1"
  redis_ttl="$2"
  shift 2

  if [ "${force_init}" = "true" ]; then
    redis_cli DEL "${redis_key}" >/dev/null
    redis_cli HSET "${redis_key}" "$@" >/dev/null
    ensure_expire "${redis_key}" "${redis_ttl}"
    hash_count=$((hash_count + 1))
    return
  fi

  wrote_any=0
  while [ "$#" -gt 1 ]; do
    redis_field="$1"
    redis_value="$2"
    shift 2

    inserted="$(redis_cli HSETNX "${redis_key}" "${redis_field}" "${redis_value}")"
    if [ "${inserted}" -eq 1 ]; then
      wrote_any=1
    fi
  done

  if [ "${wrote_any}" -eq 1 ]; then
    hash_count=$((hash_count + 1))
  fi
  ensure_expire "${redis_key}" "${redis_ttl}"
}

set_members() {
  redis_key="$1"
  redis_ttl="$2"
  shift 2

  if [ "${force_init}" = "true" ]; then
    redis_cli DEL "${redis_key}" >/dev/null
    redis_cli SADD "${redis_key}" "$@" >/dev/null
    ensure_expire "${redis_key}" "${redis_ttl}"
    set_count=$((set_count + 1))
    return
  fi

  added="$(redis_cli SADD "${redis_key}" "$@")"
  if [ "${added}" -gt 0 ]; then
    set_count=$((set_count + 1))
  fi
  ensure_expire "${redis_key}" "${redis_ttl}"
}

seed_redis() {
  ttl_10m=600
  ttl_20m=1200
  ttl_30m=1800
  ttl_40m=2400
  ttl_2h=7200
  ttl_24h=86400
  ttl_30d=2592000

  set_members "LOGIN_DEVICE_BLACKLIST" 0 D9001 D9999
  set_members "LOGIN_USER_WHITE_LIST" 0 U8888
  set_members "pulsix:list:black:device" 0 D9001 D9999
  set_members "pulsix:list:white:user" 0 U8888

  set_string "pulsix:list:black:device:D9001" "1"
  set_string "pulsix:list:black:device:D9999" "1"
  set_string "pulsix:list:black:ip:1.2.3.4" "1"
  set_string "pulsix:list:white:user:U8888" "1"

  set_hash "USER_RISK_PROFILE" "${ttl_24h}" U1001 H U2002 L U3003 L U4004 M U5001 H U5002 M U8888 L
  set_hash "IP_RISK_PROFILE" "${ttl_24h}" 1.1.1.1 LOW 1.2.3.4 HIGH 10.20.30.40 HIGH 66.77.88.99 MEDIUM
  set_hash "pulsix:profile:user:risk" "${ttl_24h}" U1001 H U2002 L U3003 L U4004 M U5001 H U5002 M U8888 L
  set_hash "pulsix:profile:ip:risk" "${ttl_24h}" 1.1.1.1 LOW 1.2.3.4 HIGH 10.20.30.40 HIGH 66.77.88.99 MEDIUM
  set_hash "pulsix:profile:device:score" "${ttl_24h}" D5001 92 D9001 87 D9002 35
  set_hash "pulsix:dict:merchant:risk" "${ttl_24h}" M1001 M M2001 H
  set_hash "pulsix:dict:geo:ip" "${ttl_30d}" 1.1.1.1 '{"country":"CN","province":"Beijing","city":"Beijing"}' 1.2.3.4 '{"country":"CN","province":"Guangdong","city":"Shenzhen"}' 10.20.30.40 '{"country":"CN","province":"Zhejiang","city":"Hangzhou"}' 66.77.88.99 '{"country":"CN","province":"Shanghai","city":"Shanghai"}'

  set_string "USER_RISK_PROFILE:U1001" "H" "${ttl_24h}"
  set_string "USER_RISK_PROFILE:U2002" "L" "${ttl_24h}"
  set_string "USER_RISK_PROFILE:U3003" "L" "${ttl_24h}"
  set_string "USER_RISK_PROFILE:U4004" "M" "${ttl_24h}"
  set_string "USER_RISK_PROFILE:U5001" "H" "${ttl_24h}"
  set_string "USER_RISK_PROFILE:U5002" "M" "${ttl_24h}"
  set_string "USER_RISK_PROFILE:U8888" "L" "${ttl_24h}"
  set_string "IP_RISK_PROFILE:1.1.1.1" "LOW" "${ttl_24h}"
  set_string "IP_RISK_PROFILE:1.2.3.4" "HIGH" "${ttl_24h}"
  set_string "IP_RISK_PROFILE:10.20.30.40" "HIGH" "${ttl_24h}"
  set_string "IP_RISK_PROFILE:66.77.88.99" "MEDIUM" "${ttl_24h}"

  set_string "pulsix:profile:user:risk:U1001" "H" "${ttl_24h}"
  set_string "pulsix:profile:user:risk:U2002" "L" "${ttl_24h}"
  set_string "pulsix:profile:user:risk:U3003" "L" "${ttl_24h}"
  set_string "pulsix:profile:user:risk:U4004" "M" "${ttl_24h}"
  set_string "pulsix:profile:user:risk:U5001" "H" "${ttl_24h}"
  set_string "pulsix:profile:user:risk:U5002" "M" "${ttl_24h}"
  set_string "pulsix:profile:user:risk:U8888" "L" "${ttl_24h}"
  set_string "pulsix:profile:ip:risk:1.1.1.1" "LOW" "${ttl_24h}"
  set_string "pulsix:profile:ip:risk:1.2.3.4" "HIGH" "${ttl_24h}"
  set_string "pulsix:profile:ip:risk:10.20.30.40" "HIGH" "${ttl_24h}"
  set_string "pulsix:profile:ip:risk:66.77.88.99" "MEDIUM" "${ttl_24h}"
  set_string "pulsix:profile:device:score:D5001" "92" "${ttl_24h}"
  set_string "pulsix:profile:device:score:D9001" "87" "${ttl_24h}"
  set_string "pulsix:profile:device:score:D9002" "35" "${ttl_24h}"

  set_hash "pulsix:profile:user:U1001" "${ttl_24h}" riskLevel H userType VIP registerDays 180 refundRate 0.13
  set_hash "pulsix:profile:user:U5001" "${ttl_24h}" riskLevel H userType NORMAL registerDays 30 refundRate 0.32
  set_hash "pulsix:profile:user:U8888" "${ttl_24h}" riskLevel L userType VIP registerDays 720 refundRate 0.01
  set_hash "pulsix:profile:device:D5001" "${ttl_24h}" riskScore 92 deviceType ANDROID emulatorFlag 1
  set_hash "pulsix:profile:device:D9001" "${ttl_24h}" riskScore 87 deviceType ANDROID emulatorFlag 0
  set_hash "pulsix:profile:device:D9002" "${ttl_24h}" riskScore 35 deviceType IOS emulatorFlag 0

  set_string "pulsix:feature:LOGIN_RISK:user_login_fail_cnt_10m:U1001" "6" "${ttl_20m}"
  set_string "pulsix:feature:LOGIN_RISK:ip_login_fail_cnt_10m:10.20.30.40" "8" "${ttl_20m}"
  set_string "pulsix:feature:LOGIN_RISK:device_login_user_cnt_1h:D9002" "1" "${ttl_2h}"
  set_string "pulsix:feature:TRADE_RISK:user_trade_cnt_5m:U1001" "3" "${ttl_10m}"
  set_string "pulsix:feature:TRADE_RISK:user_trade_cnt_5m:U5001" "4" "${ttl_10m}"
  set_string "pulsix:feature:TRADE_RISK:user_trade_amt_sum_30m:U1001" "18800" "${ttl_40m}"
  set_string "pulsix:feature:TRADE_RISK:user_trade_amt_sum_30m:U5001" "12800" "${ttl_40m}"
  set_string "pulsix:feature:TRADE_RISK:device_bind_user_cnt_1h:D9001" "4" "${ttl_2h}"
  set_string "pulsix:feature:TRADE_RISK:device_bind_user_cnt_1h:D5001" "5" "${ttl_2h}"

  set_string "pulsix:cache:scene:active_version:LOGIN_RISK" "1"
  set_string "pulsix:cache:scene:active_version:TRADE_RISK" "1"
  set_string "pulsix:cache:simulation:LOGIN_RISK:SIM_LOGIN_REVIEW:1" '{"finalAction":"REVIEW","score":60,"hitRules":["LOGIN_R002"]}' "${ttl_24h}"
  set_string "pulsix:cache:simulation:TRADE_RISK:SIM_TRADE_REJECT:1" '{"finalAction":"REJECT","score":100,"hitRules":["TRADE_R001","TRADE_R002","TRADE_R003"]}' "${ttl_24h}"
  set_string "pulsix:cache:warmup:LOGIN_RISK:device_in_blacklist" "DONE" "${ttl_30m}"
  set_string "pulsix:cache:warmup:LOGIN_RISK:ip_risk_level" "DONE" "${ttl_30m}"
  set_string "pulsix:cache:warmup:LOGIN_RISK:user_in_white_list" "DONE" "${ttl_30m}"
  set_string "pulsix:cache:warmup:TRADE_RISK:user_risk_level" "DONE" "${ttl_30m}"

  set_string "pulsix:dict:geo:ip:1.1.1.1" '{"country":"CN","province":"Beijing","city":"Beijing"}' "${ttl_30d}"
  set_string "pulsix:dict:geo:ip:1.2.3.4" '{"country":"CN","province":"Guangdong","city":"Shenzhen"}' "${ttl_30d}"
  set_string "pulsix:dict:geo:ip:10.20.30.40" '{"country":"CN","province":"Zhejiang","city":"Hangzhou"}' "${ttl_30d}"
  set_string "pulsix:dict:geo:ip:66.77.88.99" '{"country":"CN","province":"Shanghai","city":"Shanghai"}' "${ttl_30d}"
  set_string "pulsix:dict:merchant:risk:M1001" "M" "${ttl_24h}"
  set_string "pulsix:dict:merchant:risk:M2001" "H" "${ttl_24h}"
}

attempt=1
until redis_cli PING >/dev/null 2>&1; do
  if [ "${attempt}" -ge "${max_attempts}" ]; then
    echo "[redis-init] Redis 连接检查失败，已达到最大重试次数: ${max_attempts}" >&2
    exit 1
  fi

  echo "[redis-init] Redis 尚未准备好，${retry_interval_seconds}s 后进行第 $((attempt + 1)) 次重试"
  attempt=$((attempt + 1))
  sleep "${retry_interval_seconds}"
done

if [ "${force_init}" = "true" ]; then
  mode="force"
  echo "[redis-init] Redis 已就绪，准备全量重装开发种子数据"
else
  mode="ensure"
  echo "[redis-init] Redis 已就绪，准备校验并补齐开发种子数据"
fi

seed_redis
marker_value="${mode}:$(date -u +%Y-%m-%dT%H:%M:%SZ)"
redis_cli SET "${marker_key}" "${marker_value}" >/dev/null

echo "[redis-init] Redis 种子数据检查完成"
echo "[redis-init] mode=${mode}, string(no ttl writes)=${string_no_ttl_count}, string(ttl writes)=${string_ttl_count}, hash ops=${hash_count}, set ops=${set_count}, ttl repairs=${ttl_repair_count}"
