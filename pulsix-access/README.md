# pulsix-access 快速联调

A14 只补三件事：最小 smoke 脚本、可手工执行的 SDK smoke 测试、回归入口。

## 默认 demo 合同

- 场景：`TRADE_RISK`
- 事件：`TRADE_EVENT`
- HTTP 接入源：`trade_http_demo`
- SDK 接入源：`trade_sdk_demo`
- HTTP 鉴权：`X-Pulsix-Timestamp` + `X-Pulsix-Signature`，默认 `appKey/appSecret = trade-http-demo`
- SDK 鉴权：`Authorization: Bearer token-demo-001`
- HTTP / SDK smoke 默认共用同一份业务报文，只隔离传输协议与鉴权差异

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

`docs/sql/pulsix-access-smoke.sql` 只做一件事：给 `trade_sdk_demo` 补齐最小字段映射，便于 SDK smoke 复用 `TRADE_EVENT` 的最小原始样例。

3. 启动 `pulsix-ingest`：

```bash
cd /home/lbs/project/mine/pulsix/pulsix-access/pulsix-ingest
mvn -q spring-boot:run -Dspring-boot.run.arguments="--server.port=8080 --spring.datasource.dynamic.primary=master --spring.datasource.dynamic.datasource.master.url=jdbc:mysql://127.0.0.1:3306/pulsix?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&nullCatalogMeansCurrent=true&rewriteBatchedStatements=true --spring.datasource.dynamic.datasource.master.username=pulsix --spring.datasource.dynamic.datasource.master.password=pulsix_123 --spring.kafka.bootstrap-servers=127.0.0.1:29092"
```

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

常用覆盖参数：

```bash
PULSIX_ACCESS_HTTP_BASE_URL=http://127.0.0.1:8080 \
PULSIX_ACCESS_HTTP_REQUEST_ID=REQ_HTTP_SMOKE_CUSTOM \
PULSIX_ACCESS_HTTP_PAYLOAD='{"event_id":"E_HTTP_CUSTOM"}' \
bash scripts/pulsix-access-http-smoke.sh
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
bash scripts/pulsix-access-sdk-smoke.sh
```
