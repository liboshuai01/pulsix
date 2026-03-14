# `pulsix-access` 功能开发阶段划分

> 基于 `2026-03-13` 仓库 `code view` 更新。主要参考：`docs/wiki/接入层设计.md`、`docs/wiki/kafka-redis-doris-落地清单.md`、`docs/wiki/中间件版本.md`、`docs/wiki/项目架构及技术栈.md`、`docs/wiki/风控功能清单.md`、`docs/wiki/pulsix-module-risk-管理端页面开发阶段划分.md`；辅助参考：`docs/wiki/风控功能模块与表映射.md`、`docs/sql/pulsix-risk.sql`、`docs/参考资料/实时风控系统第20章：Spring Boot 控制平台的模块设计与实现.md`、`docs/参考资料/实时风控系统第22章：项目代码结构设计与从0到1的落地顺序.md`、`docs/参考资料/实时风控系统第23章：测试体系——单元测试、仿真测试、回放测试、联调测试.md`、`deploy/README.md`、`deploy/docker-compose.yml`。
>
> 当前实际状态：基于 `2026-03-13` 仓库 `code view` 与 `mvn -q -pl pulsix-access -am test -Dsurefire.failIfNoSpecifiedTests=false` 的结果，`pulsix-access` 已不再只是 Maven 骨架。`pulsix-ingest` 已具备：设计态配置读取、本地缓存、运行时标准化内核、Kafka 投递、统一错误模型、HTTP / Beacon / Netty 入口、DLQ / `ingest_error_log`、最小指标与健康检查、`AKSK / JWT` 扩展鉴权、source 级限流；`pulsix-sdk` 已具备：Netty 客户端、批量发送、失败重试、内存缓冲、断线重连、ACK 回调、最小 smoke 测试与回归入口。
>
> 因此，当前更准确的判断是：`pulsix-access` 整体已经推进到 `A14`，但还不能简单视为“所有阶段都严格收口”。若按“功能是否已落地”口径，`A00 ~ A14` 基本都已覆盖；若按“与文档要求完全一致、无明显尾项”口径，仍有若干需要继续追踪修复的问题，详见文末“阶段完成度复盘”与“修复优先级建议”。

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
- 管理端与运行时的标准化逻辑必须保持一致：后续代码应优先复用或提取 `StandardEventNormalizer` 的纯标准化核心，并显式复用 `pulsix.access.ingest.zone-id`，而不是再手写一份不同规则。
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
| `pulsix.ingest.error` | 建议预留 | `traceId` 或 `rawEventId` | `pulsix-ingest` | 接入错误增强流（预留可选，一期默认不强依赖） |

> 一期 `pulsix-access` 默认只需要保证 `pulsix.event.standard` 与 `pulsix.event.dlq` 两类生产行为；`pulsix.ingest.error` 保持预留可选，`pulsix.decision.*`、`pulsix.engine.error` 由引擎负责。

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
- Beacon 接入源：`trade_beacon_demo`
- SDK 接入源：`trade_sdk_demo`
- HTTP 原始样例：`event_sample.sample_code = TRADE_RAW_HTTP`
- Beacon smoke 默认复用与 HTTP 相同的业务报文结构，只切换接入源、入口路径与鉴权方式

`trade_http_demo / trade_beacon_demo` 的关键映射口径：

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

## 11. 基于当前 `code view` 的阶段完成度复盘

> 本节用于给后续 AI 做“现状判断 + 修复追踪”基线。
>
> 判定口径分两层：
>
> - **完成**：核心能力、最小测试、主要验收链路都已存在，且未发现明显缺口。
> - **基本完成**：主能力已经落地，但仍有一致性、观测性、边界行为或文档兑现度问题，后续应继续收口。

### 11.1 阶段状态总表

| 阶段 | 当前判定 | 说明 |
| --- | --- | --- |
| `A00` | 完成 | Maven 模块、配置类、共享 DTO、错误码骨架、序列化测试均已存在。 |
| `A01` | 完成 | 已实现 `ingest_source / ingest_mapping_def / event_field_def` MySQL 读模型与本地缓存。 |
| `A02` | 完成 | 运行时标准化已复用 `pulsix-common` 的 `StandardEventNormalizer`，与管理端预览共用同一标准化核心。 |
| `A03` | 完成（按一期口径） | 标准事件 / 错误事件 producer、Key 选择、重试基础配置已实现；`pulsix.ingest.error` 已明确为预留增强流，不再作为一期默认产出项。 |
| `A04` | 完成 | `AUTH / RATE_LIMIT / PARSE / NORMALIZE / VALIDATE / PRODUCE` 错误模型已落地；HTTP 与 SDK 非法帧都已进入统一错误主链路。 |
| `A05` | 完成 | HTTP JSON 入口已存在，`NONE / TOKEN` 鉴权路径可走通。 |
| `A06` | 完成 | HMAC 鉴权已实现，样例、单测、HTTP smoke 脚本均已存在。 |
| `A07` | 完成 | Beacon 兼容入口已实现，支持表单参数 / 文本请求体两种轻量模式。 |
| `A08` | 完成 | 错误事件已可写 Kafka 与 `ingest_error_log`，且错误日志 JSON 字段的跨模块类型契约已统一。 |
| `A09` | 完成 | Netty 服务端、最小帧协议、ACK / NACK、本地 mock client 测试已存在。 |
| `A10` | 完成 | `pulsix-sdk` 最小客户端已具备连接、发送、ACK 回调与最小请求模型。 |
| `A11` | 完成 | SDK 批量、缓冲、重试、断线重连都已实现，并有专项测试覆盖。 |
| `A12` | 基本完成 | `ingest` 与 `sdk` 都已有最小指标 / 健康检查；但异常观测仍未完全统一到所有入口。 |
| `A13` | 完成 | `AKSK / JWT` 扩展鉴权与 source 级限流均已落地，并有单测。 |
| `A14` | 完成 | README、HTTP smoke、SDK smoke、模块回归入口均已存在，且 smoke 已支持可选 Kafka / MySQL 深验收。 |

### 11.2 当前推荐结论

- 若按**功能是否已落地**判断：`A00 ~ A14` 已基本全部覆盖，`pulsix-access` 当前整体可视为已推进到 `A14`。
- 若按**是否严格收口**判断：建议记为：`A00 / A01 / A02 / A03 / A04 / A05 / A06 / A07 / A08 / A09 / A10 / A11 / A13 / A14 = 完成`；`A12 = 基本完成`。
- 后续 AI 在修复问题时，不应再从“补全整条主链路”思路出发，而应从“补齐一致性、观测、文档兑现度、边界场景”思路出发。

## 12. 后续修复优先级建议

> 原则：优先修“会影响运行时一致性、错误追踪、后续 AI 判断”的问题，再修文档与死配置，再做体验层补强。

### 12.1 `P0`：优先立即修

> 进度更新（2026-03-13）：以下 3 项已在本轮完成代码修复与回归测试，后续 AI 无需重复按故障修复处理，可转为回归验证与继续推进 `P1`。

1. **统一 `ingest_error_log` 的 JSON 字段契约**
   - 当前 `pulsix-ingest` 写入侧允许 `rawPayloadJson / standardPayloadJson` 为任意 `Object`，包括字符串；但 `pulsix-module-risk` 读取侧与详情 VO 期望的是 `Map<String, Object>`。
   - 这会导致 `AUTH / PARSE` 一类“尚未成功解析成 JSON 对象”的错误事件，在治理端详情页存在类型不稳、反序列化不一致的风险。
   - 修复目标：统一写入 / 读取两端的类型契约。若要兼容字符串原文，建议统一包装为对象，例如：`{"rawText":"...","transportType":"HTTP"}`；或者两端统一改成 `Object` / `JsonNode` 承载。

2. **让 SDK 非法帧进入统一错误主链路**
   - 当前 `NettyIngestRequestHandler` 在非法 JSON 帧场景下只返回 `REJECTED`，不会生成统一 `IngestErrorEvent`，也不会写 Kafka DLQ、`ingest_error_log`、统一 rejected metrics。
   - 这会让 `A04` 的“非法输入统一错误对象化”和 `A12` 的“错误观测一致性”留下缺口。
   - 修复目标：抽取共享的 reject / error sink 逻辑，让 HTTP 主链路与 Netty 非法帧共用同一套错误落地能力。

3. **补 1 组跨模块回归用例，锁住上述两类问题**
   - 建议至少增加：
     - `SDK 非法帧 -> DLQ / ingest_error_log / metrics`；
     - `AUTH / PARSE` 错误详情在管理端可稳定展示原始报文。
   - 这样可以避免后续 AI 只修一侧、不修另一侧，导致问题反复出现。

#### 12.1.1 `P0` 修复落地记录（2026-03-13）

- 已新增统一错误分发服务：`pulsix-access/pulsix-ingest` 通过共享 `IngestErrorDispatchService` 统一完成 `IngestErrorEvent` 创建、DLQ 投递、`ingest_error_log` 写入。
- 已补齐 SDK 非法帧主链路：`NettyIngestRequestHandler` 在非法 JSON 帧场景下不再只返回 `REJECTED`，而是同步进入统一错误事件、错误日志、rejected metrics 主链路。
- 已统一治理端详情契约：`pulsix-module-risk` 的 `rawPayloadJson / standardPayloadJson` 已改为 `Object` 承载，兼容原始字符串 JSON 与对象 JSON。
- 已补回归测试：覆盖 `SDK 非法帧 -> 错误分发` 与 `错误详情 VO 保持原始 payload 契约` 两类回归场景。
- 当前结论：`12.1 P0` 已完成，可进入 `12.2 P1` 阶段。

### 12.2 `P1`：高优先级收口

> 进度更新（2026-03-13）：以下 3 项已在本轮完成修复或口径收敛，后续 AI 可直接进入 `P2`。

1. **统一管理端预览与运行时标准化的时区口径**
   - 运行时标准化使用 `pulsix.access.ingest.zone-id`；管理端预览当前使用 `ZoneId.systemDefault()`。
   - 这会在 `TIME_MILLIS_TO_DATETIME` 一类转换上留下环境相关差异。
   - 修复目标：让两端显式使用同一时区配置或同一公共约定，不再依赖宿主机默认时区。

2. **明确 `pulsix.ingest.error` 的真实策略**
   - 当前文档、部署、配置项都声明了 `pulsix.ingest.error`，但默认运行时代码并不会真正把错误增强流发送到该 Topic。
   - 修复目标二选一：
     - 要么真正实现增强错误流的生产逻辑；
     - 要么在一期文档中明确写成“仅预留 Topic 和配置项，默认仍只走 `pulsix.event.dlq`”。

3. **修正文档 / SQL 中的状态位注释冲突**
   - 本文档已明确要求后续 AI 遵守 `CommonStatusEnum: 0=开启, 1=关闭`。
   - 但 `docs/sql/pulsix-risk.sql` 仍有多处接入相关 DDL 注释写成 `1-启用，0-停用`，会误导后续初始化与排障。
   - 修复目标：统一文档、DDL 注释、样例数据的口径，避免“代码按 0 启用，文档注释却写成 1 启用”。

#### 12.2.1 `P1` 修复落地记录（2026-03-13）

- 已统一时区口径：管理端 `StandardEventPreviewService` 不再使用宿主机 `ZoneId.systemDefault()`，而是显式复用 `pulsix.access.ingest.zone-id`。
- 已明确错误增强流策略：`pulsix.ingest.error` 保持“预留可选 Topic”定位，一期默认运行时仍以 `pulsix.event.dlq` 为唯一必达错误流；部署文档中保留预建 Topic 说明，便于后续扩展。
- 已统一接入源命名口径：管理端与 SQL 中的 `errorTopicName / error_topic_name` 均明确表示“异常 / DLQ Topic”，避免与预留增强流混淆。
- 已修正 `docs/sql/pulsix-risk.sql` 中 `CommonStatusEnum` 相关 DDL 的默认值与注释，统一为 `0=启用`、`1=停用`。
- 当前结论：`12.2 P1` 已完成，可进入 `12.3 P2` 阶段。

### 12.3 `P2`：中优先级整理项

> 进度更新（2026-03-13）：以下 3 项已在本轮完成代码/脚本收口，后续 AI 可转向新的优化项。

1. **清理或落实当前未生效的配置项**
   - 当前已有但尚未真正生效的配置项包括：`http.enabled`、`http.maxPayloadBytes`、`kafka.enabled`、`configCache.maxSourceEntries`。
   - 修复目标：要么实现其真实运行时行为，要么删除死配置，避免“文档可配、代码不消费”。

2. **收紧 HMAC demo 的默认密钥语义**
   - 当前 HMAC 鉴权在缺少显式 secret 时会回退到 `appKey`，这对 smoke demo 方便，但不适合作为长期默认安全语义。
   - 修复目标：把 `appSecret = appKey` 这类约定收敛到 smoke 数据，不作为运行时代码的默认行为。

3. **增强真机 smoke 的验收力度**
   - 当前 `README` 与脚本主要验证“接口返回 `ACCEPTED`”。
   - 修复目标：后续可以追加最小 Kafka / MySQL 校验，例如：确认标准事件进入 `pulsix.event.standard`、错误事件进入 `ingest_error_log`，让 `A14` 验收更扎实。

#### 12.3.1 `P2` 修复落地记录（2026-03-13）

- 已落实配置项行为：`http.enabled` 可真实关闭 HTTP / Beacon 入口，`http.maxPayloadBytes` 可返回 `413`，`kafka.enabled` 可真实阻断 Kafka 发送，`configCache.maxSourceEntries` 可限制 source 缓存并联动逐出对应 runtime config。
- 已收紧 HMAC 密钥语义：运行时代码不再把 `appKey` 自动兼作 HMAC 密钥；该约定只保留在 demo SQL / smoke 脚本的显式样例数据中。
- 已增强 smoke 验收：`scripts/pulsix-access-http-smoke.sh` 新增可选 Kafka / MySQL 深验收，`scripts/pulsix-access-sdk-smoke.sh` 新增可选 Kafka 深验收。
- 已同步文档口径：`pulsix-access/README.md` 与本追踪文档都已更新，避免后续 AI 继续把这些项误判为“配置已声明但未生效”。
- 当前结论：`12.3 P2` 已完成。

#### 12.3.2 真机 smoke 收口补记（2026-03-14）

- 已补齐 `pulsix-ingest` standalone 默认运行参数：模块内置 `pulsix.info.base-package=cn.liboshuai.pulsix` 与 Kafka `JsonSerializer` 默认配置，仓库根目录可直接 `package + java -jar` 启动。
- 已消除 `ingest_error_log` 落库的安全模块硬依赖：`DefaultDBFieldHandler` 不再在运行期强依赖 `SecurityFrameworkUtils`，standalone `pulsix-ingest` 的 reject / error-log 路径可正常工作。
- 已完成真机 smoke 复验：HTTP 主链路 `ACCEPTED`、HTTP reject + `ingest_error_log` 深验收、SDK 主链路 + Kafka 深验收均已跑通。

## 13. 给后续 AI 的执行建议

- **不要再把当前状态误判为“只有骨架”。** 现在的问题重心已经不是补全主链路，而是收口尾项。
- **优先按 `P0 -> P1 -> P2` 顺序推进。** 不要跳过 `P0` 直接去做体验层优化。
- **修复时优先补回归测试。** 每修一个问题，都应补一个能长期锁定该问题的测试，而不是只改实现。
- **改文档时同步改 SQL 注释与 smoke 说明。** 当前 `pulsix-access` 的主要风险之一就是“代码与文档口径不完全一致”。
