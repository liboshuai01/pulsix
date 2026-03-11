# `pulsix-engine / pulsix-kernel` 一期开发指南

> 基于 `2026-03-11` 仓库现状整理。判断依据来自：`docs/sql/pulsix-risk.sql`、`docs/wiki/项目架构及技术栈.md`、`docs/wiki/风控功能清单.md`、`docs/wiki/风控功能模块与表映射.md`、`docs/参考资料/实时风控系统第7章：控制平台的数据模型设计.md`、`docs/参考资料/实时风控系统第22章：项目代码结构设计与从0到1的落地顺序.md`、`docs/参考资料/实时风控系统第23章：测试体系——单元测试、仿真测试、回放测试、联调测试.md`，以及当前 `pulsix-engine` / `pulsix-framework/pulsix-kernel` 代码。
>
> 当前仓库验证结果：`mvn -q -pl pulsix-framework/pulsix-kernel,pulsix-engine test` 与 `mvn -q -pl pulsix-engine test` 已通过；另在 IDEA 直接运行 `cn.liboshuai.pulsix.engine.flink.DecisionEngineJob` 的 Demo 链路已完成 MiniCluster 启停、结果输出与 checkpoint 验证。

---

## 1. 一页结论

- 一期主线不变：先把 **执行内核** 和 **Flink 实时主链路** 打透，再补控制面和正式接入层。
- 当前最真实的 repo 状态已经从“`kernel` 还没开始”推进到：**共享执行语义已归位到 `pulsix-framework/pulsix-kernel`；`pulsix-engine` 主要保留 Flink 适配、Demo Job 与少量 Flink 专属状态适配代码。**
- 当前已经具备：运行时快照契约、事件契约、运行时编译、规则/策略执行、本地执行、基础流式特征、Flink `Broadcast + Keyed State` 适配、样例 SQL/JSON、单测回归，以及 `kernel` 物理模块归位。
- 当前还缺：本地仿真 / 轻量回放工具、真实 `scene_release` 接入、真实 Kafka/Redis 集成、版本治理、生产化观测与降级能力。
- 一期后续最合理的顺序应是：**本地仿真 -> 轻量回放 -> 快照接入 -> Kafka 主链路 -> Redis/版本治理 -> 生产化收口**。

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
- 所以下一优先级不再是模块搬迁，而是基于现有 `kernel` 继续补齐本地仿真、轻量回放与正式接入能力。

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
| Lookup 抽象 | 部分完成 | 接口已定义，当前只有 `InMemoryLookupService` |
| Derived Feature | 已完成 | Aviator / Groovy 两条分支均可编译执行 |
| Rule / Policy 执行 | 已完成 | `FIRST_HIT` 主链路已稳定；`SCORE_CARD` 代码已实现 |
| Flink 快照热切换 | 已完成 | `DecisionBroadcastProcessFunction` 已支持广播快照、版本切换、side output |
| Flink Job | 部分完成 | `DecisionEngineJob` 仍是 Demo Job，输入输出依赖 `DemoFixtures` 与 `print` |
| `scene_release` 运行时样例 | 已完成 | `docs/sql/pulsix-risk.sql` 已提供与 `DemoFixtures` 对齐的样例快照 |
| 仿真 Runner | 未完成 | 还没有独立的本地仿真入口或 CLI |
| 轻量回放 | 未完成 | 还没有文件级 replay / diff / golden case 工具 |
| 真实 Kafka Source / Sink | 未完成 | 当前没有正式事件 topic 接入和结果 topic 输出 |
| 真实 `scene_release` Source / CDC | 未完成 | 当前没有正式读取 MySQL / CDC / JDBC 的快照源 |
| 真实 Redis Lookup | 未完成 | `timeoutMs`、`cacheTtlSeconds` 等字段还未生效 |
| 版本治理 | 未完成 | `effectiveFrom`、`publishType`、回滚、编译失败保留旧版本尚未真正落地 |
| 生产化安全 | 未完成 | Groovy 沙箱、错误分级、指标、恢复验证仍缺 |
| 测试基线 | 已完成 | 当前已有 kernel 侧共享回归与 engine 侧 Flink harness 回归，且 `mvn -q -pl pulsix-framework/pulsix-kernel,pulsix-engine test`、`mvn -q -pl pulsix-engine test` 均通过 |

### 4.1 已完成的关键内容

- **`kernel` 物理归位已经完成**：共享执行语义已从 `pulsix-engine` 收口到 `pulsix-framework/pulsix-kernel`。
- **快照契约已经稳定**：`SceneSnapshot`、`SceneSnapshotEnvelope`、`RuleSpec`、`PolicySpec`、`RuntimeHints` 等结构已经落地。
- **执行主链路已经存在**：事件校验、上下文构建、特征计算、规则执行、策略收敛、结果组装都已可运行。
- **Flink 适配骨架不是空的**：广播快照、Keyed State、事件时间 timer、side output、版本切换都已经有代码和测试。
- **样例闭环已经存在**：`DemoFixtures` + `docs/sql/pulsix-risk.sql` 已对齐，适合做后续回归基座。

### 4.2 已有但还没收口的内容

- `SCORE_CARD` 已在 `DecisionExecutor` 中实现，但当前样例、回归、一期主链路仍以 `FIRST_HIT` 为主。
- `Groovy` 已可执行，但当前只是“可运行”，还不是“可生产”。
- `FlinkKeyedStateStreamFeatureStateStore` 已经存在，所以“Flink Keyed State 基础版”不能再算未开始；真正未完成的是**生产级状态治理**。
- `pulsix-engine` 侧已补上 `SceneRuntimeCache` 做按版本缓存，但当前缓存策略仍非常轻，只保留最近两个版本。

### 4.3 当前必须正视的缺口

- `pulsix-kernel` 里还没有独立的本地仿真 Runner，导致“给定快照 + 事件直接跑结果”仍主要依赖测试或 Demo Job。
- `DecisionEngineJob` 仍然依赖 Demo 快照和 Demo 事件源，说明正式输入输出链路还没有接上。
- `LookupFeatureSpec.timeoutMs`、`cacheTtlSeconds`、`RuntimeHints.*`、`SceneSpec.allowedEventTypes`、`SceneSnapshot.effectiveFrom`、`SceneSnapshotEnvelope.publishType` 等字段当前大多只是契约字段，还没有真正驱动执行语义。
- `GroovyCompiledScript` 直接使用 `GroovyClassLoader` 编译执行，当前没有沙箱、隔离、禁用反射、资源限制。

---

## 5. 一期后续开发阶段

下面的阶段切分只覆盖 **当前还没完成** 的内容，且每一阶段都要求：

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

### 阶段 2：本地仿真 Runner

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
- 输出 `finalAction`、`finalScore`、命中规则、命中原因、特征快照、trace。

**人工验证**

- 用 `docs/sql/pulsix-risk.sql` 中 `scene_release.snapshot_json` 或 `DemoFixtures` 样例快照运行一遍。
- 用黑名单事件验证结果应为 `REJECT`。
- 同样输入连续运行两次，结果完全一致。

**本次落地结果**

- `pulsix-framework/pulsix-kernel` 新增 `cn.liboshuai.pulsix.engine.simulation.LocalSimulationRunner`，作为阶段 2 的明确本地仿真入口。
- 支持读取 `SceneSnapshot` JSON、`SceneSnapshotEnvelope` JSON，以及单条 / 数组两种事件 JSON 形态。
- Runner 输出固定 `SimulationReport`，包含 `finalAction`、`finalScore`、`hitRules`、`hitReasons`、`featureSnapshot`、`trace`，且不暴露波动性的 `latency` 字段，便于重复回归。
- 默认装配复用 `RuntimeCompiler + LocalDecisionEngine + InMemoryStreamFeatureStateStore + InMemoryLookupService.demo()`，没有新增第二套执行语义。
- 新增 `LocalSimulationRunnerTest`，覆盖文件输入、黑名单拒绝、数组事件顺序执行、重复运行结果一致，以及空白 / 空数组事件输入失败路径。

### 阶段 3：轻量回放与 golden case 回归

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

### 阶段 4：`scene_release` 快照接入收口

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

### 阶段 5：Kafka 主链路接入

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

### 阶段 6：真实 Redis Lookup 与降级

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

### 阶段 7：版本治理与运行时约束落地

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

### 阶段 8：生产化收口

**目标**

- 把“能跑”升级为“能解释、能观测、能回归”。

**AI 交付内容**

- 完善错误分类、关键指标、日志字段。
- 补充恢复 / checkpoint / 状态清理相关测试或验证脚本。
- 固化回归入口，让本地仿真、轻量回放、Flink 测试三者共享同一套样例基线。

**人工验证**

- 能区分编译错误、执行错误、lookup 错误、状态错误、快照冲突错误。
- 关键链路有固定回归命令，跑完能看到通过 / 失败结果。
- 新增一条规则后，至少可以通过仿真、回放、Flink harness 三类验证手段验证结果一致。

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

1. 当前开发中心只放在 `pulsix-kernel + pulsix-engine`，且阶段 1 的模块归位已经完成。
2. `scene_release.snapshot_json` 是唯一运行态配置来源。
3. 仿真、回放、Flink 执行必须复用同一套 kernel 语义。
4. 一期主策略优先级是 `FIRST_HIT > SCORE_CARD`。
5. 一期表达式主力是 Aviator；`Groovy` 只做补位，不做主路径扩张。
6. 不主动扩展 `pulsix-module-risk` 和 `pulsix-access`。
7. 每次改造都优先做“可验证的小步增量”，并附带人工验证说明。
8. 如果没有额外说明，默认下一步从 **阶段 2：本地仿真 Runner** 开始，不再重复做阶段 1 的模块搬迁。

---

## 9. 快速记忆版

- `kernel` 语义已经有了，而且 **模块归位已经完成**；当前真正缺的是 **工具化 + 正式接入 + 生产化**。
- 当前 `pulsix-engine` 已主要收敛为 Flink 适配层，`pulsix-kernel` 承载共享执行语义。
- 一期不是先做平台页面，而是先把 **快照 -> 编译 -> 执行 -> 输出 -> 回归** 这条链做稳。
- 下一阶段默认从 **本地仿真 Runner** 开始；后续每一阶段都必须让你可以人工验证，不接受“一次性做完再看”。
