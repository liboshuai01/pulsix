# pulsix-access 快速联调

A14 只补三件事：最小 smoke 脚本、可手工执行的 SDK smoke 测试、回归入口。

## 默认 demo 合同

- 场景：`TRADE_RISK`
- 事件：`TRADE_EVENT`
- HTTP 接入源：`trade_http_demo`
- Beacon 接入源：`trade_beacon_demo`
- SDK 接入源：`trade_sdk_demo`
- HTTP 鉴权：`X-Pulsix-Timestamp` + `X-Pulsix-Signature`，demo 数据显式约定 `appKey=trade-http-demo`、`appSecret=trade-http-demo`
- Beacon 鉴权：`NONE`，默认使用表单 `payload=` 方式模拟浏览器 `sendBeacon`
- SDK 鉴权：`Authorization: Bearer token-demo-001`
- HTTP / Beacon / SDK smoke 默认共用同一份业务报文，只隔离传输协议与鉴权差异

## 前置准备

1. 拉起本地中间件：

```bash
cd deploy
cp .env.template .env
docker compose up -d
```

2. 导入设计态与 smoke 数据：

```bash
cd /home/lbs/project/mine/pulsix
MYSQL_PWD='pulsix_123' mysql --host='127.0.0.1' --port='3306' --user='pulsix' --database='pulsix' --default-character-set=utf8mb4 < docs/sql/pulsix-risk.sql
MYSQL_PWD='pulsix_123' mysql --host='127.0.0.1' --port='3306' --user='pulsix' --database='pulsix' --default-character-set=utf8mb4 < docs/sql/pulsix-access-smoke.sql
```

`docs/sql/pulsix-access-smoke.sql` 只做一件事：补齐 `trade_beacon_demo / trade_sdk_demo` 的最小 smoke 数据，便于 Beacon / SDK 复用 `TRADE_EVENT` 的最小原始样例。

3. 启动 `pulsix-ingest`（从仓库根目录执行）：

```bash
cd /home/lbs/project/mine/pulsix
mvn -q -pl pulsix-access/pulsix-ingest -am package -DskipTests
java -jar pulsix-access/pulsix-ingest/target/pulsix-ingest.jar \
  --server.port=8080 \
  --spring.datasource.dynamic.primary=master \
  '--spring.datasource.dynamic.datasource.master.url=jdbc:mysql://127.0.0.1:3306/pulsix?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&nullCatalogMeansCurrent=true&rewriteBatchedStatements=true' \
  --spring.datasource.dynamic.datasource.master.username=pulsix \
  --spring.datasource.dynamic.datasource.master.password=pulsix_123 \
  --spring.kafka.bootstrap-servers=127.0.0.1:29092
```

`pulsix-ingest` 已内置 `pulsix.info.base-package` 与 Kafka JSON 序列化默认值，因此独立运行时不需要再额外补这两项参数。

默认对外端口：HTTP `8080`，Netty `19100`。

## HTTP smoke

在仓库根目录执行：

```bash
bash scripts/pulsix-access-http-smoke.sh
```

成功标准：

- 返回 HTTP `200`
- 响应体包含 `"status":"ACCEPTED"`
- 脚本顺带打印 `/api/access/health` 与 `/api/access/metrics/summary`
- 可选追加 Kafka / MySQL 深验收：校验标准事件进入 `pulsix.event.standard`，校验拒绝事件写入 `ingest_error_log`

常用覆盖参数：

```bash
PULSIX_ACCESS_HTTP_BASE_URL=http://127.0.0.1:8080 \
PULSIX_ACCESS_HTTP_REQUEST_ID=REQ_HTTP_SMOKE_CUSTOM \
PULSIX_ACCESS_HTTP_PAYLOAD='{"event_id":"E_HTTP_CUSTOM"}' \
PULSIX_ACCESS_HTTP_VERIFY_KAFKA=true \
PULSIX_ACCESS_HTTP_VERIFY_ERROR_LOG=true \
bash scripts/pulsix-access-http-smoke.sh
```

如果你自定义了 `PULSIX_ACCESS_HTTP_APP_KEY`，请同时显式传入 `PULSIX_ACCESS_HTTP_APP_SECRET`；脚本不再把“自定义 appKey 自动兼作密钥”当作运行时默认语义。

## Beacon smoke

在仓库根目录执行：

```bash
bash scripts/pulsix-access-beacon-smoke.sh
```

成功标准：

- 返回 HTTP `200`
- 响应体包含 `"status":"ACCEPTED"`
- 默认走 `application/x-www-form-urlencoded` 的 `payload=` 模式，模拟浏览器 `sendBeacon`
- 可选追加 Kafka / MySQL 深验收：校验标准事件进入 `pulsix.event.standard`，校验拒绝事件写入 `ingest_error_log`

常用覆盖参数：

```bash
PULSIX_ACCESS_BEACON_BASE_URL=http://127.0.0.1:8080 \
PULSIX_ACCESS_BEACON_MODE=text \
PULSIX_ACCESS_BEACON_VERIFY_KAFKA=true \
PULSIX_ACCESS_BEACON_VERIFY_ERROR_LOG=true \
bash scripts/pulsix-access-beacon-smoke.sh
```

## HTTP -> Kafka -> Engine smoke（10A）

在 `pulsix-ingest` 已启动的前提下，在仓库根目录执行：

```bash
bash scripts/pulsix-access-http-engine-smoke.sh
```

这个脚本会做四件事：

- 用 JDBC 从 `scene_release` 固定加载 `TRADE_RISK` 的 `v14` 快照
- 以 Kafka `pulsix.event.standard` 作为输入源临时启动本地 `DecisionEngineJob`，默认从 `earliest` 启动并使用唯一消费组
- 复用 `scripts/pulsix-access-http-smoke.sh` 发 HTTP 固定业务样例；默认只给 `eventId/traceId` 追加时间后缀，避免历史 Topic 残留串扰
- 在引擎 stdout 中校验同一条样例已产出 `DecisionResult`、`DecisionLogRecord`，且 `finalAction=PASS`；若出现 lookup 缺值 fallback 观测错误，则要求为 `LOOKUP_VALUE_MISSING + DEFAULT_VALUE`

常用覆盖参数：

```bash
PULSIX_ACCESS_HTTP_ENGINE_SNAPSHOT_VERSION=14 \
PULSIX_ACCESS_HTTP_ENGINE_EXPECTED_FINAL_ACTION=PASS \
PULSIX_ACCESS_HTTP_ENGINE_ALLOW_FALLBACK_ENGINE_ERROR=false \
PULSIX_ACCESS_HTTP_ENGINE_VERIFY_ERROR_LOG=true \
bash scripts/pulsix-access-http-engine-smoke.sh
```

默认输出文件：

- 引擎 stdout：`/tmp/pulsix-http-engine-smoke.out`
- 引擎本地日志：`/tmp/pulsix-http-engine-smoke.log`
- 临时配置：`/tmp/pulsix-http-engine-smoke.properties`

## Beacon -> Kafka -> Engine smoke（10B）

在 `pulsix-ingest` 已启动的前提下，在仓库根目录执行：

```bash
bash scripts/pulsix-access-beacon-engine-smoke.sh
```

这个脚本直接复用 `10A` 的引擎联调骨架，但把接入入口切到 Beacon：

- 默认使用 `trade_beacon_demo` 作为接入源，走 `/api/access/ingest/beacon`
- 默认使用表单 `payload=` 模式模拟 `sendBeacon`，并沿用 `TRADE_RISK v14` 的 JDBC 快照
- 复用 `kernel` 对齐后的验收口径：校验 `DecisionResult`、`DecisionLogRecord`，允许 `LOOKUP_VALUE_MISSING + DEFAULT_VALUE` 的 fallback 观测错误

常用覆盖参数：

```bash
PULSIX_ACCESS_BEACON_ENGINE_BASE_URL=http://127.0.0.1:8080 \
PULSIX_ACCESS_BEACON_ENGINE_EXPECTED_FINAL_ACTION=PASS \
PULSIX_ACCESS_BEACON_ENGINE_ALLOW_FALLBACK_ENGINE_ERROR=false \
bash scripts/pulsix-access-beacon-engine-smoke.sh
```

## SDK smoke

在仓库根目录执行：

```bash
bash scripts/pulsix-access-sdk-smoke.sh
```

脚本本质上会执行 `NettyPulsixSdkSmokeTest`，直接走 `pulsix-sdk -> Netty -> pulsix-ingest -> Kafka ACK`。

常用覆盖参数：

```bash
PULSIX_ACCESS_SDK_HOST=127.0.0.1 \
PULSIX_ACCESS_SDK_PORT=19100 \
PULSIX_ACCESS_SDK_REQUEST_ID=REQ_SDK_SMOKE_CUSTOM \
PULSIX_ACCESS_SDK_AUTHORIZATION='Bearer token-demo-001' \
PULSIX_ACCESS_SDK_VERIFY_KAFKA=true \
bash scripts/pulsix-access-sdk-smoke.sh
```

## 健康与指标

```bash
curl -s http://127.0.0.1:8080/api/access/health
curl -s http://127.0.0.1:8080/api/access/metrics/summary
```

## 回归入口

模块回归：

```bash
mvn -q -pl pulsix-access/pulsix-ingest -am test -Dsurefire.failIfNoSpecifiedTests=false
mvn -q -pl pulsix-access/pulsix-sdk -am test -Dsurefire.failIfNoSpecifiedTests=false
mvn -q -pl pulsix-access -am test -Dsurefire.failIfNoSpecifiedTests=false
```

手动真机 smoke：

```bash
bash scripts/pulsix-access-http-smoke.sh
bash scripts/pulsix-access-beacon-smoke.sh
bash scripts/pulsix-access-sdk-smoke.sh
```
