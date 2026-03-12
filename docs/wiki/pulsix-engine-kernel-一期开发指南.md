# `pulsix-engine / pulsix-kernel` 一期开发指南

> 基于 `2026-03-12` 仓库现状整理。判断依据来自：`docs/sql/pulsix-risk.sql`、`docs/wiki/项目架构及技术栈.md`、`docs/wiki/风控功能清单.md`、`docs/wiki/风控功能模块与表映射.md`、`docs/参考资料/实时风控系统第7章：控制平台的数据模型设计.md`、`docs/参考资料/实时风控系统第22章：项目代码结构设计与从0到1的落地顺序.md`、`docs/参考资料/实时风控系统第23章：测试体系——单元测试、仿真测试、回放测试、联调测试.md`，以及当前 `pulsix-engine` / `pulsix-framework/pulsix-kernel` 代码。
>
> 当前仓库验证结果：`mvn -q -pl pulsix-framework/pulsix-kernel,pulsix-engine test`、`mvn -q -pl pulsix-engine test` 与 `bash scripts/decision-engine-job-demo-smoke.sh` 已通过；此前已补齐完整时间线版本治理与 Redis `connectTimeoutMs` 生效，本轮在 P0-A / P1 / P0-B 的基础上，又完成了 `P2 SCORE_CARD` 收口：`PolicySpec` 已补齐 `ruleRefs`，`DecisionExecutor` 已支持 `scoreWeight`、`stopOnHit`、score band reason 渲染与最小可解释结果字段（`totalScore`、`scoreContributions`、`matchedScoreBand`、`reason`），并新增最小 `SCORE_CARD` fixture 以及本地执行、仿真 / 回放、Flink harness 回归。
>
> 补充说明：后续 code review 原始结论中的 3 个问题，截至 `2026-03-12` 已全部完成首轮收口：P0-A、P1、P0-B 与 P2 均已落地。对应的修复顺序、设计取舍与验收口径已精简并入本文 `4.2` 节。

---

## 1. 一页结论

- 一期主线不变：先把 **执行内核** 和 **Flink 实时主链路** 打透，再补控制面和正式接入层。
- 当前最真实的 repo 状态已经从“`kernel` 还没开始”推进到：**共享执行语义已归位到 `pulsix-framework/pulsix-kernel`；`pulsix-engine` 主要保留 Flink 适配、Demo Job 与少量 Flink 专属状态适配代码。**
- 当前已经具备：运行时快照契约、事件契约、运行时编译、规则/策略执行、本地执行、基础流式特征、Flink `Broadcast + Keyed State` 适配、样例 SQL/JSON、单测回归、`kernel` 物理模块归位、本地仿真 / 轻量回放 / golden case 回归工具、`scene_release` 的 `demo/file/jdbc/cdc` 快照接入、Kafka 主链路稳定输出，以及真实 Redis Lookup / 降级 / 版本治理能力。
- 当前已补齐：阶段 8 的生产化收口，重点完成了 Groovy 安全边界、错误分级、关键指标与恢复 / 状态清理验证。
- 一期后续最合理的顺序应是：在当前基线之上继续补控制面与正式接入，而不是回头重做执行内核。

---

## 2. 一期边界

### 2.1 只做什么

- `pulsix-kernel`：统一执行语义、本地仿真、轻量回放、回归能力。
- `pulsix-engine`：Flink 运行适配、快照热切换、状态适配、结果/日志/错误输出。
- `scene_release.snapshot_json`：唯一运行态配置输入。
- `RiskEvent`：唯一标准事件输入。
- 固定样例数据：一组可重复回归的 `snapshot + events + SQL`。

### 2.2 当前不做什么

- `pulsix-module-risk` 的页面、CRUD、完整发布后台。
- `pulsix-access` 的正式 HTTP / SDK / Beacon 接入。
- 多租户、灰度发布、多环境推广、多活部署。
- CEP、复杂序列规则、拖拽式策略编排、同步在线决策 API。
- 把 `Groovy` 做成主能力。
- 完整 Dashboard / 分析面。

### 2.3 一期最小闭环

```text
scene_release.snapshot_json
    -> SceneSnapshot / SceneSnapshotEnvelope
    -> RuntimeCompiler / CompiledSceneRuntime

standard RiskEvent
    -> EvalContext
    -> Stream Feature
    -> Lookup Feature
    -> Derived Feature
    -> Rule
    -> Policy
    -> DecisionResult / DecisionLogRecord / EngineErrorRecord

本地执行 / 仿真 / 轻量回放 / Flink 执行
    -> 复用同一套 kernel 语义
```

---

## 3. 当前代码地图

### 3.1 当前模块真实分布

- `pulsix-framework/pulsix-kernel`：已承载 `model / context / script / runtime / core / feature / support / json / demo` 等共享执行语义。
- `pulsix-framework/pulsix-kernel/src/test/java/...`：已承载本地执行、运行时编译、JSON 编解码等共享回归测试。
- `pulsix-engine/src/main/java/cn/liboshuai/pulsix/engine/flink`：保留 Flink 广播快照、主链路作业、输出 tag 等运行适配层。
- `pulsix-engine/src/main/java/cn/liboshuai/pulsix/engine/feature/FlinkKeyedStateStreamFeatureStateStore.java`：保留 Flink 专属 Keyed State 适配。
- `pulsix-engine/src/main/java/cn/liboshuai/pulsix/engine/package-info.java`：保留模块说明。
- `pulsix-engine/src/test/java/...`：当前主要保留 Flink harness 与日志绑定回归测试。

### 3.2 这意味着什么

- 当前 **逻辑上的 `kernel` 已经存在**。
- 当前 **物理上的 `kernel` 也已经归位**。
- 所以下一优先级不再是模块搬迁，也不再是补本地仿真 / 回放基础工具，而是基于现有 `kernel` 继续补齐正式接入与生产化能力。

---

## 4. 当前完成度盘点

| 领域 | 当前状态 | 结论 |
| --- | --- | --- |
| `pulsix-kernel` 模块 | 已完成 | 共享执行语义、样例与核心回归已迁入 `pulsix-framework/pulsix-kernel` |
| 运行时契约 | 已完成 | `SceneSnapshot`、`SceneSnapshotEnvelope`、`RiskEvent`、`DecisionResult`、`DecisionLogRecord`、`EngineErrorRecord` 已定义 |
| 快照编译 | 已完成 | `RuntimeCompiler` 已支持 stream / lookup / derived / rule 编译、依赖排序、规则排序 |
| 本地执行 | 已完成 | `DecisionExecutor` + `LocalDecisionEngine` 已可跑通样例闭环 |
| Stream Feature 基础能力 | 已完成 | `COUNT`、`SUM`、`MAX`、`LATEST`、`DISTINCT_COUNT` 已实现 |
| Flink Keyed State 基础适配 | 已完成 | 已有 `FlinkKeyedStateStreamFeatureStateStore` 与事件时间定时清理 |
| Lookup 抽象 | 已完成 | 已同时具备 `InMemoryLookupService` 与 `RedisLookupService`，并统一通过 `LookupResult` 承载错误码与降级语义 |
| Derived Feature | 已完成 | Aviator / Groovy 两条分支均可编译执行 |
| Rule / Policy 执行 | 已完成 | `FIRST_HIT` 与 `SCORE_CARD` 均已收口；`scoreWeight`、`stopOnHit`、band reason 与解释字段已落地 |
| Flink 快照热切换 | 已完成 | 当前 prepared topology 已接管主作业；`DecisionBroadcastProcessFunction` 保留为 legacy harness，用于广播快照、版本切换与 side output 回归 |
| Flink Job | 已完成 | `DecisionEngineJob` 已支持 `demo/kafka` 事件源、`print/kafka` 可配置输出，快照来源保持 `demo/file/jdbc/cdc` |
| `scene_release` 运行时样例 | 已完成 | `docs/sql/pulsix-risk.sql` 已提供与 `DemoFixtures` 对齐的样例快照 |
| 仿真 Runner | 已完成 | 已有 `LocalSimulationRunner` 作为独立本地仿真入口，提供 `main` CLI，并支持标准化 overrides 注入 |
| 轻量回放 | 已完成 | 已有 `LocalReplayRunner` 提供文件级 replay / diff / golden case 工具 |
| 真实 Kafka Source / Sink | 已完成 | 已支持 `pulsix.event.standard -> DecisionResult/DecisionLogRecord/EngineErrorRecord` 的 Kafka 主链路 |
| 真实 `scene_release` Source / CDC | 已完成 | 已支持 `demo/file/jdbc/cdc` 四种快照来源，并复用统一 `scene_release -> SceneSnapshotEnvelope` 归一化入口 |
| 真实 Redis Lookup | 已完成 | `RedisLookupService` 与 lookup 降级错误流已落地，feature 超时与 Redis `connectTimeoutMs` 已分别生效 |
| 版本治理 | 已完成 | 已按 scene 维护完整 release timeline，支持 future 版本、回滚、编译失败保留旧版本与运行时约束 |
| 生产化安全 | 已完成 | Groovy 沙箱、结构化错误分级、Flink 指标、恢复 / 状态清理回归已补齐 |
| 测试基线 | 已完成 | 当前已有 kernel 侧共享回归与 engine 侧 Flink harness 回归，且 `mvn -q -pl pulsix-framework/pulsix-kernel,pulsix-engine test`、`mvn -q -pl pulsix-engine test` 均通过 |

### 4.1 已完成的关键内容

- **`kernel` 物理归位已经完成**：共享执行语义已从 `pulsix-engine` 收口到 `pulsix-framework/pulsix-kernel`。
- **快照契约已经稳定**：`SceneSnapshot`、`SceneSnapshotEnvelope`、`RuleSpec`、`PolicySpec`、`RuntimeHints` 等结构已经落地。
- **执行主链路已经存在**：事件校验、上下文构建、特征计算、规则执行、策略收敛、结果组装都已可运行。
- **Flink 适配骨架不是空的**：广播快照、Keyed State、事件时间 timer、side output、版本切换都已经有代码和测试。
- **样例闭环已经存在**：`DemoFixtures` + `docs/sql/pulsix-risk.sql` 已对齐，适合做后续回归基座。

### 4.2 本轮 3 个关键问题收口摘要

- **修复顺序**：本轮固定按“先 correctness、再 recovery、最后 completeness”推进，即 `P0 -> P1 -> P2`；这样可以先恢复 `kernel / replay / Flink` 一致性，再补恢复语义，最后收口 `SCORE_CARD` 能力完整性。
- **问题一已收口**：`kernel` 流式特征状态主键已统一为 `sceneCode + featureCode + entityKey`；Flink 侧也不再把 `routeKey()` 当成 stream feature 状态主键或首段拓扑分流键，而是新增 `RiskEvent.processingRouteKey()` 仅用于首段负载打散 / pending buffer 分片，随后仍按 `event.sceneCode` 选中生效快照，再通过 routing group fan-out 到 `groupKey + entityKey` keyed 子链，mixed-entity 场景已恢复正确语义。
- **问题二已收口**：无快照时的 pending buffer 已从进程内内存结构迁入 keyed state，并补齐 checkpoint / restore、缓冲超限、缓冲超时清理与结构化错误输出，事件先到、快照后到场景具备恢复语义。
- **问题三已收口**：`SCORE_CARD` 已补齐 `PolicySpec.ruleRefs`、`scoreWeight`、`stopOnHit`、`ScoreBandSpec.reasonTemplate`，并在 `DecisionResult` 中补齐 `totalScore`、`scoreContributions`、`matchedScoreBand`、`reason` 等解释字段；最小评分卡 fixture 与本地执行、仿真 / 回放、Flink harness 回归也已补齐。
- **当前结论**：这 3 个 code review 问题到此已全部完成首轮收口；后续优先级自然转向控制面补齐、正式接入层和更多生产化增量治理。

### 4.3 当前必须正视的缺口

- 当前一期主线已闭环；截至 `2026-03-12`，P0-A、P1、P0-B 与 P2 均已完成，后续优先级自然转向控制面补齐、正式接入层以及更多生产化增量治理。
- `DecisionEngineJob` 主链路已支持 `demo/kafka` 事件源、`print/kafka` 输出和 `demo/file/jdbc/cdc` 快照来源；当前 `routeKey()` 不再被当成 stream feature entity key，作业拓扑已收口为：先按 `RiskEvent.processingRouteKey()` 做首段负载打散，再按 `event.sceneCode` 选定当前有效快照，通过 routing group fan-out 到 `groupKey + entityKey` keyed 的 stream feature 子链，随后按 `eventJoinKey` 汇聚 partial snapshot，最后以非 keyed 的 prepared decision 主链完成执行。
- prepared decision 聚合段现已补齐 processing-time 超时清理与结构化错误输出（`PREPARED_DECISION_AGGREGATE_TIMEOUT`），避免 partial chunk 长时间滞留 keyed state。
- 新子链已补齐最小必要观测项：`preparedRouteCount`、`preparedBypassCount`、`preparedChunkCount`、`preparedAggregateCompleteCount`、`preparedAggregateTimeoutCount` 与 `preparedAggregatePendingGroups` gauge，并恢复 prepared 主链的 result / log / error 指标上报。旧 `DecisionBroadcastProcessFunction` 已降为 package-private legacy harness，新主链不再复用它的嵌套接口与默认常量。
- 无快照时的 pending buffer 已迁入 keyed state，并补齐 checkpoint / restore、缓冲超限与缓冲超时清理；`StreamFeatureRoutingProcessFunctionTest` 与 `PreparedDecisionAggregateProcessFunctionTest` 已补齐 prepared 新主链的恢复回归，Redis lookup 的 feature 超时、连接超时、默认值与缓存降级也已收口。

---

## 5. 一期后续开发阶段

下面的阶段切分保留已完成记录；截至 `2026-03-12`，阶段 1 ~ 8 已完成。每一阶段都要求：

- AI 完成后可以单独提交、单独回归。
- 你可以用少量人工步骤验证是否满足要求。
- 不依赖先把 `pulsix-module-risk` 和 `pulsix-access` 一起做完。

### 阶段 1：`kernel` 代码归位（已完成，`2026-03-11`）

**目标**

- 把当前已经落在 `pulsix-engine` 中的共享执行语义，迁移到 `pulsix-framework/pulsix-kernel`。
- 让 `pulsix-engine` 只保留 Flink 适配层和演示 / 测试入口。

**AI 交付内容**

- 迁移 `model / context / script / runtime / core / feature / support / json` 等共享代码。
- 调整 `pulsix-engine` 的依赖与 import，使其改为依赖 `pulsix-kernel`。
- 把本地执行类与对应测试同步迁移或复用。

**人工验证**

- `pulsix-framework/pulsix-kernel/src/main/java` 下已出现核心执行代码。
- `pulsix-engine/src/main/java` 下当前主要只剩 `flink`、少量适配代码与模块说明。
- 执行 `mvn -q -pl pulsix-framework/pulsix-kernel,pulsix-engine test` 已通过。

**本次落地结果**

- 已迁移 `model / context / script / runtime / core / feature / support / json / demo` 等共享代码到 `pulsix-framework/pulsix-kernel`。
- 已把 `LocalDecisionEngineTest`、`EngineJsonTest`、`SceneRuntimeManagerTest` 等共享回归迁入 `pulsix-kernel`；Flink `typeinfo` 与 `flink.runtime` 侧版本缓存回归保留在 `pulsix-engine`。
- `pulsix-engine` 当前保留 `flink` 适配、`FlinkKeyedStateStreamFeatureStateStore`、Demo 作业入口与 Flink 相关回归。
- 根目录新增 `.mvn/maven.config` 并默认启用 `-am`，保证 `mvn -q -pl pulsix-engine test` 在拆模块后仍可直接使用。

### 阶段 2：本地仿真 Runner（已完成，`2026-03-11`）

**接手前提（基于 `2026-03-11` 阶段 1 完成态）**

- 不再回退当前 `kernel / engine` 模块布局；新增共享执行能力继续优先落到 `pulsix-framework/pulsix-kernel`。
- 可直接复用 `DemoFixtures`、`EngineJson`、`LocalDecisionEngine`、`SceneRuntimeManager` 作为 Runner 的样例、解析与执行基座。
- 当前固定回归命令为 `mvn -q -pl pulsix-framework/pulsix-kernel,pulsix-engine test` 与 `mvn -q -pl pulsix-engine test`。
- 阶段 2 交付重点是“明确入口 + 固定输入输出”，不是再做第二套执行语义。

**目标**

- 在 `pulsix-kernel` 内提供一个明确的“给定快照 + 给定事件 -> 输出结果”的本地仿真入口。

**AI 交付内容**

- 支持读取 `SceneSnapshot` JSON / `SceneSnapshotEnvelope` JSON。
- 支持读取单条事件 JSON 或事件数组 JSON。
- 输出报告中可稳定获取 `finalAction`、`finalScore`、命中规则、命中原因、特征快照、trace。

**人工验证**

- 用 `docs/sql/pulsix-risk.sql` 中 `scene_release.snapshot_json` 或 `DemoFixtures` 样例快照运行一遍。
- 用黑名单事件验证结果应为 `REJECT`。
- 同样输入连续运行两次，结果完全一致。

**本次落地结果**

- `pulsix-framework/pulsix-kernel` 新增 `cn.liboshuai.pulsix.engine.simulation.LocalSimulationRunner`，作为阶段 2 的明确本地仿真入口。
- 支持读取 `SceneSnapshot` JSON、`SceneSnapshotEnvelope` JSON，以及单条 / 数组两种事件 JSON 形态。
- Runner 输出固定 `SimulationReport`；其中 `finalResult / results` 内可稳定获取 `finalAction`、`finalScore`、`hitRules`、`hitReasons`、`featureSnapshot`、`trace`，且不暴露波动性的 `latency` 字段，便于重复回归。
- 默认装配复用 `RuntimeCompiler + LocalDecisionEngine + InMemoryStreamFeatureStateStore + InMemoryLookupService.demo()`，没有新增第二套执行语义。
- 补充 `SimulationOverrides / EventOverrides` 固定模型与 `--overrides-file` CLI，支持按 feature code 注入 mock lookup / mock stream feature，并区分全局覆盖与按事件覆盖。
- `SimulationReport / SimulationEventResult` 新增 `usedVersion` 与 `overridesApplied` 等稳定字段，便于回放与控制面仿真对齐。
- 新增 `LocalSimulationRunnerTest`，覆盖文件输入、黑名单拒绝、数组事件顺序执行、重复运行结果一致，以及空白 / 空数组事件输入失败路径，并补充 overrides 覆盖回归。

### 阶段 3：轻量回放与 golden case 回归（已完成，`2026-03-11`）

**目标**

- 在 `pulsix-kernel` 内提供文件级 replay 和版本对比能力。

**AI 交付内容**

- 支持事件序列回放。
- 支持同一事件集在两个快照版本上的结果 diff。
- 支持固定 golden case，结果漂移时直接失败。

**人工验证**

- 用当前 `TRADE_RISK v12` 样例事件集回放，结果应与现有单测一致。
- 构造一个“关闭黑名单规则”的新快照，回放 diff 能看到 `REJECT -> PASS` 或命中链路变化。
- 人工改坏一条 golden case 后，回归应显式报错。

**本次落地结果**

- `pulsix-framework/pulsix-kernel` 新增 `cn.liboshuai.pulsix.engine.simulation.LocalReplayRunner`，复用 `LocalSimulationRunner` 提供文件级 replay、双版本 diff 与 golden case 校验能力。
- Replay 默认按 `eventTime` 升序回放，同时间再按 `eventId / traceId / 原始顺序` 稳定排序，避免输入文件顺序漂移影响回归结果。
- 新增 `ReplayReport`、`ReplayDiffReport`、`ReplayGoldenCase` 等固定输出模型，覆盖 action 统计、差异事件列表、命中规则与命中原因，便于长期回归。
- `LocalReplayRunner` 已补充 `main` CLI，支持 `replay / diff / capture-golden / verify-golden` 四种文件模式，便于直接做本地回放与回归校验。
- 新增 `LocalReplayRunnerTest`，覆盖乱序事件回放、关闭黑名单规则后的版本 diff、golden case 生成与校验，以及 golden case 漂移时的显式失败路径。

### 阶段 4：`scene_release` 快照接入收口（已完成，`2026-03-11`）

**目标**

- 让引擎不再只依赖 `DemoFixtures`，而是具备正式快照输入抽象。

**AI 交付内容**

- 抽象快照 Source：至少支持 `demo/file` 两种；再向 `JDBC / CDC` 兼容。
- 支持从 `scene_release.snapshot_json` 还原 `SceneSnapshotEnvelope`。
- 统一 `version / checksum / publishType / effectiveFrom` 的接收与校验入口。

**人工验证**

- 不改代码也能通过文件或数据库样例把快照喂给引擎。
- 同版本不同 `checksum` 的快照会被识别为冲突。
- 新版本快照进入后，无需重启即可替换当前运行时。

**本次落地结果**

- `SceneSnapshotSourceType` 与 `SceneSnapshotSourceFactory` 已统一支持 `demo/file/jdbc/cdc` 四种快照来源。
- `SceneReleaseJdbcSnapshotLoader`、`JdbcPollingSceneSnapshotSource`、`JdbcBootstrapSceneSnapshotSource` 已支持从 `scene_release` 读取快照，并统一还原为 `SceneSnapshotEnvelope`。
- `SceneReleaseCdcPayloadParser` 与 `MySqlCdcSceneSnapshotSourceFactory` 已支持 MySQL CDC 增量接入，并复用 JDBC bootstrap 做当前快照预热；CDC 首发已收口为“JDBC bootstrap + CDC latest 增量”。
- `SceneReleaseSnapshotSelector` 与 JDBC polling marker 已统一收口：bootstrap 默认保留每个 scene 的“当前 latest effective + 所有 future 版本”，并按 `sceneCode + version` 跟踪 release identity，避免多 future 版本或同版本 `effectiveFrom / publishType` 变化被遗漏。
- `DecisionEngineJob` 已补齐 `--snapshot-source demo|file|jdbc|cdc` 及 JDBC/CDC 相关 CLI 参数，文件或数据库样例都可直接喂给引擎。
- 新增 `SceneReleaseSnapshotSelectorTest` 与 `SceneReleaseCdcPayloadParserTest`，覆盖 future `effectiveFrom`、版本筛选、CDC payload 解析与发布状态过滤等关键回归。

### 阶段 5：Kafka 主链路接入（已完成，`2026-03-11`）

**目标**

- 把 Demo Job 升级为可接标准 topic 的最小正式链路。

**AI 交付内容**

- 事件输入改为 Kafka Source。
- 结果、日志、错误分别输出到正式 Sink 或可配置 Sink。
- 保留 Demo 模式，避免回归测试失去最小样例。

**人工验证**

- 向输入 topic 写入一条标准事件，可以在结果 topic 收到 `DecisionResult`。
- side output 能分别看到 `DecisionLogRecord` 和 `EngineErrorRecord`。
- `DecisionEngineJob` 不再硬编码依赖 `DemoRiskEventSource` / `DemoSnapshotSource` 才能工作。

**本次落地结果**

- `DecisionEngineJob` 新增 `--event-source demo|kafka`、`--kafka-bootstrap-servers`、`--event-kafka-topic`、`--event-kafka-group-id`、`--event-kafka-starting-offsets` 等参数，默认仍保留 Demo 事件流，正式模式可直接消费标准事件 topic。
- Flink 主链路的正式路由规划已在 `2026-03-12` 收口：首段输入不再使用 `sceneCode` 单热点，也不复用会误导语义的 `RiskEvent.routeKey()`；当前改为 `keyBy(RiskEvent::processingRouteKey)` 做负载打散，`StreamFeatureRoutingProcessFunction` 在运行时仍按事件自带的 `sceneCode` 解析有效快照并生成 `groupKey + entityKey` 子链路由，prepared decision 主链也已移除 `keyBy(PreparedDecisionInput::getSceneCode)` 的无意义热点。
- 新增 `--output-sink print|kafka`，并支持 `--result-sink`、`--log-sink`、`--error-sink` 做单流覆盖；默认 topic 分别为 `pulsix.decision.result`、`pulsix.decision.log`、`pulsix.engine.error`，Kafka 输出默认按 `traceId` 写 key，缺失时回退到 `eventId`。
- Kafka 输入链路新增 `RiskEvent` JSON 反序列化与最小合法性校验；反序列化失败或缺少 `sceneCode` 的事件会被转换为 `EngineErrorRecord`，与主链路 side output 错误流汇合后统一下沉。
- 新增 `EngineJsonSerializationSchema`，`DecisionResult`、`DecisionLogRecord`、`EngineErrorRecord` 统一按 JSON UTF-8 写入 Kafka，避免 Demo 模式与正式模式使用两套输出协议。
- 新增 `DecisionEngineJobOptionsTest` 与 `RiskEventJsonCodecTest`，覆盖 Kafka 参数解析、默认 Demo 回退、非法事件校验与 JSON 序列化关键回归。
- `DecisionEngineJob` 现已支持通过类路径默认 `pulsix-engine.properties`、外部 `--config-file` 与 `ParameterTool` 统一收口作业参数；Kafka、快照源、checkpoint、状态后端、MiniCluster 资源与本地日志路径都可以通过配置文件管理，默认仍保留纯 Demo 最小回归基线。

### 阶段 6：真实 Redis Lookup 与降级（已完成，`2026-03-12`）

**目标**

- 把 lookup 从内存版推进到真实在线查询版，同时保留默认值与降级兜底。

**AI 交付内容**

- 实现 Redis lookup provider。
- 让 `timeoutMs`、`cacheTtlSeconds`、`defaultValue` 真正生效。
- 失败时输出明确错误记录，但不把整个引擎拖死。

**人工验证**

- Redis 中写入黑名单设备、用户画像后，规则命中结果与预期一致。
- 人为制造 Redis 超时或不可用时，引擎仍可继续运行，结果按默认值 / 降级策略返回。
- 错误流能区分 lookup 超时、lookup 连接失败、lookup 返回空值。

**本次落地结果**

- `pulsix-kernel` 新增 `RedisLookupConfig`、`RedisLookupService`、`LookupResult`，已支持 `REDIS_SET / REDIS_HASH / REDIS_STRING / DICT` 在线查询。
- `LookupFeatureSpec.timeoutMs`、`cacheTtlSeconds`、`defaultValue` 已真实生效：Redis 访问超时走 feature 级超时，Redis `connectTimeoutMs` 单独驱动建连超时，成功值按 `cacheTtlSeconds` 做进程内短缓存，lookup key 为空 / Redis 值缺失 / 连接失败 / 其他运行时异常时按 `defaultValue` 或缓存值降级。
- `DecisionExecutor`、`EngineErrorRecord`、`DecisionBroadcastProcessFunction` 已打通 lookup 错误流，错误码可区分 `LOOKUP_TIMEOUT`、`LOOKUP_CONNECTION_FAILED`、`LOOKUP_VALUE_MISSING`、`LOOKUP_KEY_MISSING`，并携带 `featureCode`、`sourceRef`、`lookupKey`、`fallbackMode`。
- `DecisionEngineJob` 已支持 `--lookup-source redis` 及 Redis 连接参数；默认配置仍保留 `demo` lookup 作为最小样例基线，需要联调时再切到本地 `deploy` 启动的 Redis；`DecisionEngineJobOptions.LookupOptions` 也已补齐 `Serializable`，IDEA 本地启动不再因 Flink closure clean 失败。
- 新增 / 更新 `RedisLookupServiceTest`、`DecisionBroadcastProcessFunctionTest`、`DecisionEngineJobOptionsTest`，覆盖 Redis lookup、timeout/default fallback、错误流与参数解析回归。
- 已在本地 `deploy` 的 `Redis + Kafka` 上完成真实链路 smoke：先验证缺失画像时会输出 `LOOKUP_VALUE_MISSING + DEFAULT_VALUE`，再补齐 Redis 画像后复测，`DecisionEngineJob` 可直接消费 Kafka 事件并命中真实 `user_risk_level=H`；四条交易事件累积后，`E-SMOKE-20260312C-4` 最终得到 `REJECT / 80`，无额外 lookup 错误输出。
- `deploy/redis/init/01-init-redis.sh` 已改为“幂等校验 + TTL 自动补种”模式：再次执行 `docker compose up -d redis-init` 时会保留已有 seed，并自动补回过期的画像 / 特征 / 缓存 key，不再依赖手工打开 `REDIS_INIT_FORCE`。

### 阶段 7：版本治理与运行时约束落地（已完成，`2026-03-12`）

**目标**

- 让现有快照字段从“只建模”升级为“真正参与运行时治理”。

**AI 交付内容**

- 让 `effectiveFrom`、`publishType`、回滚、编译失败保留旧版本真正生效。
- 扩展 `pulsix-engine` 侧版本缓存 / 切换能力，不要只保留最近两个版本的最简缓存。
- 让 `RuntimeHints.maxRuleExecutionCount`、`allowGroovy`、`needFullDecisionLog` 具备基本约束作用。

**人工验证**

- 发送一个编译失败的新快照，当前运行版本不被破坏。
- 发送一个延迟生效的快照，在生效时间前后结果表现不同且可解释。
- 显式发送回滚版本后，当前 active runtime 能切回旧版本。

**本次落地结果**

- `DecisionBroadcastProcessFunction` 已从“active + 单 pending”收口为按 scene 维护 `SceneReleaseTimeline`；运行时按 `effectiveFrom / publishedAt / publishType / version` 解析当前有效版本，支持多 future 版本、乱序到达与显式回滚。
- 快照编译仍保持 `compile-before-activate`：同版本不同 `checksum` 会被识别为冲突并拒绝激活；编译失败继续保留旧 runtime，并通过 `ENGINE_ERROR` 输出失败原因。
- timeline 已与 JDBC bootstrap / polling 选择逻辑对齐；不再只保留一个 pending 版本，也不再默认裁剪时间线，避免大量 future 版本场景下丢版本。
- `SceneRuntimeCache` 默认缓存窗口已从“最近两个版本”扩到“每个场景最近 8 个编译版本”，用于承接回滚与历史版本切换；`StreamFeaturePrepareProcessFunction` 与 `PreparedDecisionProcessFunction` 现已共享有界 `CompiledSceneRuntimeResolver`，prepared 主链不再额外保留无界编译缓存。
- `RuntimeHints.maxRuleExecutionCount` 已在 `DecisionExecutor` 中生效：超过上限后会停止继续执行后续规则；`LocalDecisionEngineTest` 已覆盖“只允许执行首条规则时最终回落为 PASS”的场景。
- `RuntimeHints.allowGroovy` 已在 `RuntimeCompiler` 中生效：当 hint 显式关闭时，任何 Groovy 派生特征 / 规则都会在编译期失败，而不是带着风险进入运行态。
- `RuntimeHints.needFullDecisionLog` 已在 Flink 日志 side output 中生效：关闭后仍保留最终动作与命中规则，但不再下沉 `featureSnapshot / traceLogs` 全量细节；legacy harness 与 prepared 主链的 `DecisionLogRecord` 精简输出回归均已补齐。
- 顺手让 `SceneSpec.allowedEventTypes` 也真正参与运行时校验：事件类型不在场景白名单时，内核会直接拒绝执行，避免 scene 层约束继续只停留在快照契约上。

### 阶段 8：生产化收口（已完成，`2026-03-12`）

**目标**

- 把“能跑”升级为“能解释、能观测、能回归”。

**AI 交付内容**

- 完善错误分类、关键指标、日志字段。
- 补充恢复 / checkpoint / 状态清理相关测试或验证脚本。
- 固化回归入口，让本地仿真、轻量回放、Flink 测试三者共享同一套样例基线。

**人工验证**

- 能区分编译错误、执行错误、lookup 错误、状态错误、快照冲突错误。
- 关键链路有固定回归命令，跑完能看到通过 / 失败结果。
- `DecisionEngineJob` 本地 smoke 已固化为 `bash scripts/decision-engine-job-demo-smoke.sh`，默认复现 demo snapshot + demo event + print sink 最小链路。
- 新增一条规则后，至少可以通过仿真、回放、Flink harness 三类验证手段验证结果一致。

**本次落地结果**

- 已补齐 Groovy 沙箱，禁止危险能力进入运行态。
- 已补齐结构化错误分级与日志字段，可区分编译 / 执行 / lookup / 状态 / 快照冲突错误。
- 已补齐 Flink 关键指标与 `DecisionEngineJob` 本地 smoke 入口。
- 已补齐 checkpoint 恢复、状态清理与仿真 / 回放 / Flink 一致性回归；其中 prepared 新主链已覆盖 routing pending buffer 恢复、prepared aggregate 恢复与 aggregate timeout timer 恢复。

---

## 6. 每阶段都必须满足的验证基线

### 6.1 代码基线

- 不允许再把执行语义扩散到第二套实现。
- 新能力优先加到 `pulsix-kernel`，`pulsix-engine` 只做适配。
- Demo 样例能力不能删除，只能下沉为回归基线。

### 6.2 测试基线

- 每阶段结束时，至少保留一条**成功路径**回归。
- 每阶段结束时，至少新增一条**失败路径**回归。
- 每阶段结束时，`mvn -q -pl pulsix-framework/pulsix-kernel,pulsix-engine test` 与 `mvn -q -pl pulsix-engine test` 必须继续可通过。

### 6.3 人工验收基线

- 能说明本阶段新增了什么，不新增什么。
- 能提供固定输入样例。
- 能提供固定输出预期。
- 能说明失败时会看到什么错误表现。

---

## 7. 一期测试优先级

### 第一优先级

- `RuntimeCompiler` 编译结果与依赖顺序测试。
- `DecisionExecutor` 的规则 / 策略执行测试。
- Stream Feature 五种基础聚合测试。
- `SceneRuntimeCache` / 版本切换测试。
- 本地仿真 / 回放 golden case 测试。

### 第二优先级

- Flink `Broadcast State` 与 `Keyed State` 协同测试。
- lookup 超时 / 默认值 / 降级测试。
- 命中原因模板渲染测试。
- `SCORE_CARD` 独立回归测试。

### 第三优先级

- Kafka 端到端链路测试。
- 恢复 / checkpoint / 长稳测试。
- 回滚和延迟生效测试。
- Groovy 安全边界测试。

---

## 8. 给后续 AI 助手的默认工作假设

如果没有新的明确指令，默认遵循以下规则：

1. 当前开发中心只放在 `pulsix-kernel + pulsix-engine`，且阶段 1 ~ 8 已完成。
2. `scene_release.snapshot_json` 是唯一运行态配置来源。
3. 仿真、回放、Flink 执行必须复用同一套 kernel 语义。
4. 一期主策略优先级是 `FIRST_HIT > SCORE_CARD`。
5. 一期表达式主力是 Aviator；`Groovy` 只做补位，不做主路径扩张。
6. 不主动扩展 `pulsix-module-risk` 和 `pulsix-access`。
7. 每次改造都优先做“可验证的小步增量”，并附带人工验证说明。
8. 如果没有额外说明，默认在当前基线上做增量优化，不再回头重做阶段 1 ~ 8。

---

## 9. 快速记忆版

- `kernel` 语义、Flink 主链路、仿真 / 回放 / 版本治理与生产化收口都已经完成。
- 当前 `pulsix-engine` 已主要收敛为 Flink 适配层，`pulsix-kernel` 承载共享执行语义。
- 一期不是先做平台页面，而是先把 **快照 -> 编译 -> 执行 -> 输出 -> 回归** 这条链做稳。
- 后续工作默认在当前稳定基线上做增量推进，且每一步都必须可人工验证。
