# `pulsix-access` 功能开发阶段划分

> 基于 `2026-03-13` 仓库 `code view` 更新。主要参考：`docs/wiki/接入层设计.md`、`docs/wiki/kafka-redis-doris-落地清单.md`、`docs/wiki/中间件版本.md`、`docs/wiki/项目架构及技术栈.md`、`docs/wiki/风控功能清单.md`、`docs/wiki/pulsix-module-risk-管理端页面开发阶段划分.md`；辅助参考：`docs/wiki/风控功能模块与表映射.md`、`docs/sql/pulsix-risk.sql`、`docs/参考资料/实时风控系统第20章：Spring Boot 控制平台的模块设计与实现.md`、`docs/参考资料/实时风控系统第22章：项目代码结构设计与从0到1的落地顺序.md`、`docs/参考资料/实时风控系统第23章：测试体系——单元测试、仿真测试、回放测试、联调测试.md`、`deploy/README.md`、`deploy/docker-compose.yml`。
>
> 当前实际状态：`pulsix-access/pulsix-ingest` 与 `pulsix-access/pulsix-sdk` 已有 Maven 骨架；`pulsix-ingest` 仅有启动类，接入运行时主链路基本尚未开始。与之配套的设计态 / 治理态能力已经在 `pulsix-module-risk` 落下：`ingest_source`、`ingest_mapping_def`、`ingest_error_log`、标准事件预览、接入治理页均已存在。因此，后续工作重点不是再做一套配置后台，而是把 `pulsix-access` 的运行时链路按小阶段稳步落地。

## 1. 文档目标

- 给你和后续 AI 一个统一的 `pulsix-access` 开发顺序，避免一次做太多协议 / 组件导致上下文失控。
- 每个阶段都尽量控制为：**1 个核心能力、1 条清晰验收链路、最多 1 个新增外部依赖点**。
- 优先闭环 `HTTP/Beacon -> pulsix-ingest -> Kafka`，再做 `SDK(Netty) -> pulsix-ingest -> Kafka`。
- 在保持文档精简的前提下，保留后续 AI 写代码最容易遗漏的关键信息：边界、表、Topic、字段、鉴权、测试口径、联调环境。

## 2. 当前定位与边界

### 2.1 模块定位

```text
pulsix-access
  pulsix-ingest   服务端统一接入器
  pulsix-sdk      业务后端高性能接入 SDK
```

- `pulsix-ingest`：负责接收 `HTTP / Beacon / SDK` 事件，做鉴权、标准化、公共字段补齐、异常分流，并投递 Kafka。
- `pulsix-sdk`：面向业务后端，基于 Netty 长连接接入 `pulsix-ingest`，提供轻量预处理、批量发送、异步回调、重试与缓冲。
- `pulsix-module-risk`：只负责**轻治理**，即接入源、字段映射、错误查询、SDK 接入说明，不负责承载运行时接入主链路。

### 2.2 必须守住的边界

- **不要**再在 `pulsix-access` 里重复实现一套配置后台或治理页面。
- **不要**把 `pulsix-module-risk` 作为 `pulsix-access` 的运行时依赖；共享纯逻辑时，优先抽到 `pulsix-framework/pulsix-common` 或 `pulsix-access` 内部纯组件。
- `pulsix-sdk` 只做轻量预处理，**最终标准化以 `pulsix-ingest` 为准**。
- `pulsix-access` 一期只负责：接入、鉴权、标准化、投 Kafka、错误分流、最小监控。
- `pulsix-access` 一期**不负责**：同步在线决策 API、Doris 直写、Redis lookup、配置 Topic、可视化协议编排、复杂动态脚本标准化。

## 3. 固定约束

- 统一使用现有设计态表：`ingest_source`、`ingest_mapping_def`、`event_field_def`、`event_schema`、`event_sample`。
- 一期**不新增** `pulsix.config.snapshot` 一类配置 Topic；接入配置读取先走 MySQL 读模型 + 本地缓存 / 定时刷新，不把 CDC / Kafka 配置同步绑进首批阶段。
- 标准事件主 Topic 统一为 `pulsix.event.standard`；不要一开始就按场景拆 Topic。
- 异常流至少保证 `pulsix.event.dlq`；`pulsix.ingest.error` 作为推荐预留流。
- 管理端与运行时的标准化逻辑必须保持一致：后续代码应优先复用或提取 `StandardEventPreviewService` 的纯标准化核心，而不是再手写一份不同规则。
- 单阶段建议控制在：**1 个协议入口或 1 个核心组件、最多 2 个 package、1 条 smoke case、1 组最小测试**。
- 每个阶段都要至少包含：代码、测试、验收样例；如果改动设计态约束，还要同步文档或 SQL 样例。
- 仓库 `CommonStatusEnum` 约定为：`0=开启`、`1=关闭`。后续 AI 不要按常见习惯误判状态值。

## 4. 关键资料速记

### 4.1 中间件与运行时版本

| 项目 | 当前建议版本 | 用途 |
| --- | --- | --- |
| Java | `17` | 仓库统一版本 |
| Spring Boot | `3.5.9` | `pulsix-ingest` 服务容器 |
| Netty | `4.2.9.Final` | `pulsix-sdk` 与 `pulsix-ingest` 长连接通信 |
| MySQL | `8.4.8 LTS` | 设计态 / 治理态主库 |
| Redis | `7.4.7` | 名单 / 画像 / 特征副本；接入层一期不走热路径依赖 |
| Kafka | `3.9.1`（KRaft） | 标准事件流、错误流 |
| Doris | `3.1.4` | 接入异常 / 决策查询分析层 |

### 4.2 本地联调连接信息

- MySQL：`127.0.0.1:3306/pulsix`，用户 `pulsix`，密码 `pulsix_123`
- Redis：`127.0.0.1:6379`，密码 `pulsix_redis_123`
- Kafka：宿主机 `127.0.0.1:29092`，Docker 内 `kafka:9092`
- 启动命令：`docker compose up -d`
- 默认 Topic 创建由 `kafka-init` 负责，默认规格见 `deploy/docker-compose.yml`

### 4.3 Kafka Topic 速记

| Topic | 是否必建 | Key | 生产者 | 备注 |
| --- | --- | --- | --- | --- |
| `pulsix.event.standard` | 是 | `sceneCode` | `pulsix-ingest` | 标准事件主流 |
| `pulsix.event.dlq` | 是 | `traceId` 或 `rawEventId` | `pulsix-ingest` | 非法事件 / 死信 |
| `pulsix.ingest.error` | 建议预留 | `traceId` 或 `rawEventId` | `pulsix-ingest` | 接入错误增强流 |

> 一期 `pulsix-access` 只需要保证上表三类生产行为；`pulsix.decision.*`、`pulsix.engine.error` 由引擎负责。

### 4.4 设计态表与运行时职责

| 表 | 归属 | `pulsix-access` 如何使用 |
| --- | --- | --- |
| `ingest_source` | 管理端维护 | 读取接入源、接入方式、鉴权方式、Topic、场景范围、QPS |
| `ingest_mapping_def` | 管理端维护 | 读取字段映射、转换类型、默认值、清洗规则 |
| `event_field_def` | 管理端维护 | 读取标准事件字段定义、字段类型、必填约束、默认值 |
| `event_schema` | 管理端维护 | 校验场景 / 事件基础约束 |
| `event_sample` | 管理端维护 | 作为单测 / smoke / demo 输入样例 |
| `ingest_error_log` | 运行时写入 + 管理端查询 | 记录鉴权、解析、标准化、校验、投递阶段错误 |

### 4.5 默认演示样例

- 场景：`TRADE_RISK`
- 事件：`TRADE_EVENT`
- HTTP 接入源：`trade_http_demo`
- SDK 接入源：`trade_sdk_demo`
- HTTP 原始样例：`event_sample.sample_code = TRADE_RAW_HTTP`

`trade_http_demo` 的关键映射口径：

- `$.event_id -> eventId`
- `$.occur_time_ms -> eventTime`，转换类型 `TIME_MILLIS_TO_DATETIME`
- `$.req.traceId -> traceId`
- `$.uid -> userId`
- `$.dev_id -> deviceId`
- `$.client_ip -> ip`
- `$.pay_amt -> amount`，转换类型 `DIVIDE_100`
- `$.trade_result -> result`，转换类型 `ENUM_MAP`：`ok -> SUCCESS`，`fail -> FAIL`

### 4.6 标准事件最小字段口径

标准事件至少要围绕以下字段构建：

- `eventId`
- `traceId`
- `sceneCode`
- `eventType`
- `eventTime`
- `userId`
- `deviceId`
- `ip`
- `channel`
- `ext`

`TRADE_RISK / TRADE_EVENT` 当前主样例中的核心必填字段是：

- `eventId`
- `sceneCode`
- `eventType`
- `eventTime`
- `userId`
- `deviceId`
- `ip`
- `amount`
- `result`

### 4.7 映射 / 清洗 / 转换规则速记

当前管理端预览逻辑已支持：

- 转换类型：`DIRECT`、`CONST`、`TIME_MILLIS_TO_DATETIME`、`DIVIDE_100`、`ENUM_MAP`
- 字段类型：`STRING`、`LONG`、`DECIMAL`、`BOOLEAN`、`DATETIME`、`JSON`
- 清洗规则：`trim`、`blankToNull`、`upperCase`、`lowerCase`

> 后续 `pulsix-access` 的运行时标准化逻辑，必须与此保持一致；最稳妥的做法是抽出一个纯 Java `StandardEventNormalizer`，让管理端预览和运行时接入共同复用。

## 5. 推荐代码边界

### 5.1 `pulsix-ingest` 内部分层建议

- `controller`：只放 HTTP / Beacon 接口适配。
- `netty`：只放 SDK 长连接接入、编解码、连接管理。
- `app/service`：编排鉴权、标准化、投递、错误分流。
- `domain`：标准化、鉴权、限流、错误模型等纯业务逻辑。
- `infra`：MySQL 配置读取、Kafka Producer、错误日志持久化、定时刷新。
- `support/model`：DTO、事件模型、配置模型、协议响应。

### 5.2 `pulsix-sdk` 内部分层建议

- `client`：连接管理、发送入口。
- `codec`：请求 / 响应编解码。
- `callback`：异步回调接口。
- `buffer`：批量、重试、内存缓冲。
- `model`：SDK 发送请求、ACK、错误对象。

### 5.3 关键设计原则

- HTTP、Beacon、Netty 入口都应该复用同一条“鉴权 -> 标准化 -> 投递 -> 错误分流”主链路。
- 入口层尽量薄，复杂逻辑不要写在 Controller / Netty Handler。
- 后续如果需要共享标准化逻辑，优先抽纯组件，不要让 `pulsix-access` 直接依赖 `pulsix-module-risk` Service。

## 6. 四个里程碑

- **M1：运行时内核可测** —— 完成 `A00 ~ A04`，即配置读取、标准化、投 Kafka、错误模型具备单元测试闭环。
- **M2：HTTP 主链路可演示** —— 完成 `A05 ~ A08`，即 `HTTP / Beacon -> pulsix-ingest -> Kafka / DLQ / ingest_error_log` 跑通。
- **M3：SDK 主链路可演示** —— 完成 `A09 ~ A11`，即 `pulsix-sdk(Netty) -> pulsix-ingest -> Kafka` 跑通。
- **M4：可维护性与观测补齐** —— 完成 `A12 ~ A14`，即指标、健康检查、鉴权扩展、回归脚本补齐。

## 7. 阶段划分

> 约束：每个阶段都尽量做到“代码量可控、验收明确、失败面有限”。后续 AI 如果只拿到当前阶段上下文，也能独立完成。

| 阶段 | 只做什么 | 涉及模块 | 关键产物 | 最小验收 |
| --- | --- | --- | --- | --- |
| `A00` | 骨架对齐与共享约定 | `pulsix-ingest`、`pulsix-sdk`、必要时 `pulsix-common` | 包结构、配置类、基础 DTO、错误码骨架 | 应用可启动；测试可跑；关键模型可序列化 |
| `A01` | 设计态配置读取 | `pulsix-ingest` | 读取 `ingest_source` / `ingest_mapping_def` / `event_field_def` 的 MySQL 读模型 + 本地缓存 | 能按 `trade_http_demo + TRADE_RISK + TRADE_EVENT` 读到完整配置 |
| `A02` | 运行时标准化内核 | `pulsix-ingest`，必要时抽纯组件 | 路径取值、清洗、转换、默认值、必填校验、标准 JSON 输出 | 同一原始样例下，运行时输出与管理端预览结果一致 |
| `A03` | Kafka 投递器 | `pulsix-ingest` | 标准事件 producer、错误事件 producer、Key 选择、重试基础配置 | 标准事件能投到 `pulsix.event.standard` |
| `A04` | 接入错误模型 | `pulsix-ingest` | `AUTH / PARSE / NORMALIZE / VALIDATE / PRODUCE` 错误阶段枚举、错误 DTO、DLQ payload | 非法输入可被统一转成错误对象 |
| `A05` | HTTP 接入最小闭环 | `pulsix-ingest` | 一个 JSON HTTP 接口，支持 `NONE / TOKEN` 鉴权 | `curl` 原始报文后，可写入 `pulsix.event.standard` |
| `A06` | HMAC 鉴权 | `pulsix-ingest` | Header 取值、时间戳容忍、签名计算与验签失败分流 | `trade_http_demo` 样例可通过验签，错误签名进 DLQ |
| `A07` | Beacon 兼容 | `pulsix-ingest` | `sendBeacon` 兼容入口或兼容同一 HTTP 入口的轻量模式 | 浏览器 Beacon 报文能进入标准化主链路 |
| `A08` | 接入异常落库 | `pulsix-ingest` | `ingest_error_log` 写入器，与 Kafka 错误流并存 | 错误事件除写 Kafka 外，也能查到 MySQL 记录 |
| `A09` | Netty 服务端协议 | `pulsix-ingest` | 长连接服务端、最小帧协议、ACK / NACK | 本地 mock client 能连上并发送 1 条消息 |
| `A10` | SDK 最小客户端 | `pulsix-sdk` | 连接、发送、ACK 回调、最小请求模型 | `pulsix-sdk -> pulsix-ingest -> Kafka` 跑通 |
| `A11` | SDK 批量 / 重试 / 缓冲 | `pulsix-sdk` | 批量发送、失败重试、内存缓冲、断线重连 | 服务端短暂重启后 SDK 能恢复发送 |
| `A12` | 指标与健康检查 | `pulsix-ingest`、`pulsix-sdk` | source 维度计数、错误数、投递耗时、连接状态、健康检查 | 能看到最小健康信息和 source 维度指标 |
| `A13` | 扩展鉴权与限流 | `pulsix-ingest` | `AKSK / JWT` 扩展点、source 级限流 | 不影响已完成主链路，新增鉴权用例可测 |
| `A14` | 联调脚本与回归测试 | `pulsix-ingest`、`pulsix-sdk` | 单测、集成测试、最小 smoke 脚本、README 片段 | 新人按文档可本地跑通 1 条 HTTP 和 1 条 SDK 链路 |

## 8. 每阶段的最小验收模板

每做完一个阶段，至少回答下面 5 个问题：

1. 本阶段只新增了哪 **1 个核心能力**？
2. 是否有对应的 **最小测试**（单元测试或集成测试）？
3. 是否有 **1 条可手工验证** 的输入样例？
4. 对错误场景，是否能明确看到 **DLQ / error log / 返回错误** 之一？
5. 是否避免顺手把下一个阶段的功能一起做进去？

## 9. 对后续 AI 的直接约束

1. **不要一上来同时做 HTTP、Beacon、Netty、SDK、指标。** 必须按阶段推进。
2. **不要在 `pulsix-access` 中复制管理端的标准化规则。** 优先抽纯逻辑复用。
3. **不要先做性能优化。** 先保证标准化正确、错误链路清楚、Topic 投递稳定。
4. **不要先引入 Redis、Doris、CDC、配置 Topic。** 一期接入链路不需要这些复杂度。
5. **不要把 SDK 设计成比 ingest 更“聪明”。** SDK 只能轻处理，标准化最终口径在服务端。
6. **不要让一个阶段跨越两条协议主链路。** HTTP 和 SDK 分开做。
7. **不要忽略错误场景。** 接入模块最容易出问题的不是 happy path，而是鉴权失败、字段缺失、格式不合法、Kafka 投递失败。
8. **不要忽略测试优先级。** 第一优先级永远是：字段映射、类型转换、默认值补齐、非法事件入 DLQ。

## 10. 推荐执行顺序总结

最推荐的顺序是：

1. `A00 ~ A04`：先把**运行时内核**做正确。
2. `A05 ~ A08`：先闭环 **HTTP / Beacon**，因为最容易本地联调。
3. `A09 ~ A11`：再闭环 **Netty / SDK**，把高性能通路补上。
4. `A12 ~ A14`：最后再补**观测、扩展鉴权、回归脚本**。

一句话总结：

> **`pulsix-access` 的正确开发顺序不是“先把所有协议都接上”，而是“先把一条统一的接入主链路做对，再逐个给它加入口”。**

