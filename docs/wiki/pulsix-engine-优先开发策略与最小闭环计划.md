## 1. 这份文档解决什么问题

这份文档用于回答一个非常实际的问题：

> 在 `pulsix` 从 0 到 1 的早期阶段，是否应该优先围绕 `pulsix-engine` 开发？如果要优先做，边界应该怎么收，才不会把项目做偏？

结论先说在前面：

- **应该优先围绕 `pulsix-engine` 的核心执行闭环推进。**
- **但不建议孤立地“只写 `pulsix-engine` 模块”，而应该同时保留最小配套：发布编译与仿真验证。**
- 一期目标不是把整个平台全部做完，而是先证明：
  - 运行时快照模型是稳定的；
  - 执行语义是可以落地的；
  - 仿真、 本地执行、Flink 执行三者最终能够收敛到一致结果。

换句话说，当前最值得投入的不是“功能面最广”，而是“内核最可信”。

---

## 2. 为什么优先围绕 `pulsix-engine` 是合理的

从项目风险来看，当前最大的未知数并不是：

- `pulsix-access` 能不能接事件；
- `pulsix-module-risk` 能不能做 CRUD；
- 页面能不能画出来。

真正最大的未知数是：

- 运行时快照能否稳定表达决策逻辑；
- 特征、规则、策略能否在统一上下文中执行；
- 热更新、版本切换、仿真一致性这些关键语义，是否真的能落地为稳定代码。

因此，从工程收益来看，优先验证 `pulsix-engine` 的核心链路有三个明显好处：

### 2.1 先验证最难点，减少后续大返工

如果 `engine` 的核心抽象没有定住，那么：

- `access` 输出的标准事件结构可能要改；
- `module-risk` 产出的快照结构可能要改；
- 仿真接口、日志模型、查询页面也都可能跟着返工。

反过来说，只要 `engine` 的输入、运行时模型、输出结果先稳定下来，其他模块大多是“围绕既定契约做适配”，不确定性会小很多。

### 2.2 先做最关键的“执行真相”

实时风控系统真正的核心价值，不是页面，也不是配置表，而是：

- 给定一个快照；
- 给定一条标准事件；
- 系统能否稳定、低延迟、可解释地给出决策。

这件事情的落地难度，远高于一般的后台 CRUD。既然这是最核心、最难、最容易踩坑的部分，那么它就应该成为一期主线。

### 2.3 先建立后续模块的适配标准

`pulsix` 后续很多模块，本质上都在围绕 `engine` 服务：

- `pulsix-access` 负责把外部输入整理成 `RiskEvent`；
- `pulsix-module-risk` 负责把设计态对象编译成 `SceneSnapshot`；
- 查询与日志侧负责消费 `DecisionResult`、`DecisionLogRecord`、`EngineErrorRecord`。

所以，早期最值得先稳定的并不是“页面长什么样”，而是下面这些运行契约：

- `RiskEvent`
- `SceneSnapshot`
- `EvalContext`
- `DecisionResult`

只要这些骨架稳定下来，其他模块都能围绕它们演进。

---

## 3. 但为什么又不能真的“只做 `pulsix-engine` 模块”

虽然主线应围绕 `engine`，但如果完全孤立开发 `pulsix-engine`，也会有明显问题。

原因在于：`engine` 真正消费的不是设计态表，而是**运行时快照**。

所以在工程上，至少还必须保留两个最小配套能力：

### 3.1 最小发布编译能力

必须有人负责把设计态对象编译成 `SceneSnapshot`，并写入 `scene_release`。

否则 `engine` 的输入来源不稳定，早期只能靠手写 Demo JSON，验证价值有限。

这里不要求一开始就做完整控制台，只需要做到最小版本：

- 能维护一个最小场景；
- 能维护最小特征、规则、策略；
- 能点击发布；
- 能生成并存储 `snapshot_json`。

### 3.2 最小仿真能力

如果只有 Flink 执行，没有仿真能力，那么你很难回答下面这些问题：

- 这份快照到底算得对不对？
- 规则命中原因是否符合预期？
- 新版本和旧版本差异是什么？
- AI 生成的引擎方案，是否真的可验证、可回归？

因此，仿真不是锦上添花，而是一期就应该具备的“可信度证明器”。

---

## 4. 当前阶段最推荐的主线：`engine + release + simulation`

对于当前阶段，推荐把“最小可落地主线”收敛成下面这个闭环：

```text
设计态最小对象
    -> SceneSnapshotCompiler
    -> scene_release.snapshot_json
    -> RuntimeCompiler
    -> CompiledSceneRuntime
    -> DecisionExecutor
    -> DecisionResult / DecisionLogRecord / EngineErrorRecord
    -> Simulation / Local Engine / Flink Engine 一致验证
```

这条主线的核心目标不是一次做全，而是先证明三件事：

1. 快照结构是否自包含、可执行；
2. 执行内核是否能稳定跑通最小规则链路；
3. 仿真结果与运行结果是否一致。

如果这三件事都成立，那么后续再扩展接入、页面、监控、回放，都会顺很多。

---

## 5. 一期不应该做成“完整平台”，而应该做成“最小可信闭环”

### 5.1 一期核心目标

一期的真实目标应该是：

- 固定标准事件模型；
- 固定快照模型；
- 固定上下文模型；
- 固定结果模型；
- 跑通一个场景的最小规则链路；
- 建立发布、仿真、运行的一致语义。

### 5.2 一期最小业务样例建议

建议只做一个最小场景，例如：`TRADE_RISK`。

最小能力集建议控制在：

- 一个 `stream feature`：`user_trade_cnt_5m`
- 一个 `lookup feature`：`device_in_blacklist`
- 一个 `derived feature`：`high_amt_flag`
- 两到三条规则
- 一个 `FIRST_HIT` 策略

只要这条最小链路能从发布、仿真、本地执行、Flink 执行全部跑通，它就已经足够证明架构方向是成立的。

### 5.3 一期明确不做什么

一期建议明确压住下面这些需求，避免把复杂度拉爆：

- 不先做全量 `Groovy` 重能力；
- 不先做复杂序列规则、CEP；
- 不先做拖拽式策略编排；
- 不先做完整接入治理平台；
- 不先做复杂 Dashboard / 分析面；
- 不先做同步在线决策 API；
- 不先做多租户、灰度发布、多环境推广。

一期应优先选择：

- `FIRST_HIT`
- Aviator/DSL
- 一个最小流式特征集
- 一个最小 lookup 能力集

---

## 6. 推荐的模块边界

### 6.1 `pulsix-module-risk`：设计态与发布态最小配套

这个模块在当前阶段不需要一下子做成完整的控制平台，但要承担最小发布编译责任。

推荐放：

- 场景/特征/规则/策略的最小管理能力
- 依赖分析器
- 表达式基础校验
- `SceneSnapshotCompiler`
- 发布接口 `/release/publish`
- 回滚接口 `/release/rollback`
- 仿真接口 `/simulation/evaluate`
- `scene_release` 的读写

注意：

- 这里负责的是**发布语义**；
- 不负责热路径逐条规则执行；
- 不应该直接承载一套独立的“后台规则执行逻辑”。

### 6.2 `pulsix-kernel`：统一执行语义所在层

如果从长期结构看，真正的执行语义最好统一沉到 `pulsix-kernel`。

它应该放：

- 运行时核心模型
- `RuntimeCompiler`
- `SceneRuntimeManager`
- `EvalContext`
- `DecisionExecutor`
- `RuleEvaluator`
- `PolicyEvaluator`
- 仿真和线上共用的纯 Java 执行内核

要求是：

- 不依赖 Spring MVC；
- 不依赖 MyBatis；
- 不依赖 Flink API；
- 不直接耦合 Redis 客户端实现；
- 可以被 `module-risk` 和 `pulsix-engine` 同时依赖。

如果当前阶段为了加快落地，暂时不把现有代码立刻搬到 `pulsix-kernel`，也没问题；但编码时必须按“未来可抽离”的边界来写。

### 6.3 `pulsix-engine`：Flink 运行适配层

`pulsix-engine` 在当前阶段应重点负责：

- 事件流消费
- 快照广播流消费
- 本地 runtime 激活
- `Broadcast + Event` 组合执行
- Keyed State 适配
- lookup 适配
- side output 输出
- Kafka / Doris / MySQL 等 sink 对接

它的原则非常明确：

- **只认运行时快照，不认设计态表；**
- 只做实时执行适配，不负责设计态编排；
- 不要把控制面逻辑直接塞进 Flink `ProcessFunction`。

### 6.4 `pulsix-access`：后置且轻量

`pulsix-access` 当前只需要做轻量骨架，不应该在一期吃掉太多精力。

它只需要完成：

- 统一接入
- 鉴权
- 字段标准化
- 公共字段补齐
- 投递标准事件到 Kafka
- 异常事件分流到 `DLQ / error topic`

也就是说：它要能服务主链路，但不应抢主线。

---

## 7. 一期必须先稳定下来的核心契约

当前阶段，不是所有东西都要同时稳定，但下面这几类一定要尽早固定。

### 7.1 标准事件模型 `RiskEvent`

至少应固定：

- `eventId`
- `sceneCode`
- `eventType`
- `eventTime`
- `traceId`
- `entity fields`
- `ext`

它是：

- Flink 输入模型；
- 仿真输入模型；
- 日志追溯的最小来源对象。

### 7.2 运行时快照 `SceneSnapshot`

至少应固定：

- `scene`
- `features`
- `rules`
- `policy`
- `runtimeHints`
- `version`
- `checksum`

它是：

- 发布编译产物；
- Flink 广播输入；
- 本地 runtime 编译输入；
- 版本管理和回滚的核心对象。

### 7.3 执行上下文 `EvalContext`

至少应固定：

- `base fields`
- `feature values`
- `lookup values`
- `derived values`
- `metadata`

它是规则、策略、脚本真正看到的统一变量世界。

### 7.4 决策结果 `DecisionResult`

至少应固定：

- `sceneCode`
- `version`
- `finalAction`
- `score`
- `hitRules`
- `hitReasons`
- `latencyMs`
- `traceId`

它既是下游消费契约，也是日志追溯与查询分析的基础对象。

### 7.5 发布接口与仿真接口

建议尽早固定：

- `/release/publish`
- `/release/rollback`
- `/simulation/evaluate`

因为这三个接口可以最直接地把“控制面 -> 快照 -> 执行 -> 验证”串起来。

---

## 8. 当前仓库可以直接利用的起点

从当前仓库现状来看，已经具备了明显的“先做本地执行内核”的基础：

- 已有 `LocalDecisionEngine`
- 已有 `DecisionExecutor`
- 已有 `SceneRuntimeManager`
- 已有 `RuntimeCompiler`
- 已有 `InMemoryLookupService`
- 已有 `InMemoryStreamFeatureStateStore`
- 已有 Demo fixtures 和基础单测

这说明当前工程其实已经自然站在“先验证引擎内核”的路线之上。

因此，当前最合理的推进方式不是另起炉灶，而是继续沿着这条线深化：

1. 先把本地执行内核打磨稳定；
2. 再把发布编译与仿真补齐；
3. 最后再把它挂到真正的 Flink 输入输出链路上。

---

## 9. 推荐的四周落地计划

下面给一版围绕“最小可信闭环”的四周开发计划。它不是唯一答案，但很适合当前阶段。

### 第 1 周：冻结核心契约，打磨本地执行内核

目标：给定一个 `SceneSnapshot` 和一批 `RiskEvent`，本地一定能稳定跑出一致结果。

建议工作项：

- 明确并冻结 `RiskEvent`、`SceneSnapshot`、`EvalContext`、`DecisionResult`
- 整理执行链路：事件校验 -> 上下文构建 -> stream -> lookup -> derived -> rule -> policy -> result
- 继续完善 `RuntimeCompiler`
- 继续完善 `DecisionExecutor`
- 补齐错误分类与错误输出模型
- 固化最小业务样例与 golden case

本周退出标准：

- 同一快照、同一输入事件，多次执行结果完全一致
- 本地规则命中链路可解释
- 本地失败场景可稳定复现并输出明确错误

### 第 2 周：补齐最小发布编译链路

目标：从设计态对象稳定产出 `scene_release.snapshot_json`。

建议工作项：

- 定义最小设计态对象集合
- 实现 `SceneSnapshotCompiler`
- 实现依赖分析器
- 实现派生特征依赖排序与循环依赖校验
- 实现表达式基础校验
- 实现 `/release/publish`
- 将快照写入 `scene_release`

本周退出标准：

- 点击发布后可以稳定产出结构化快照
- 编译失败不会覆盖旧版本
- 快照可被本地执行内核直接加载

### 第 3 周：补齐仿真闭环

目标：可以基于已发布快照对任意测试事件进行仿真，且结果与本地执行一致。

建议工作项：

- 实现 `/simulation/evaluate`
- 仿真输入复用 `RiskEvent`
- 仿真执行复用同一套运行时编译与执行逻辑
- 输出命中规则、命中原因、特征快照、最终动作、版本号、耗时
- 建立固定样例集做回归

本周退出标准：

- 仿真与本地引擎结果完全一致
- 可以针对版本号回放样例事件并比对结果
- 命中原因和特征快照具备基础可解释性

### 第 4 周：打通 Flink 最小主链路

目标：跑通 `发布 -> 快照广播 -> 标准事件 -> 决策输出`。

建议工作项：

- 快照广播流先支持 Demo/Mock 或最小真实来源
- 事件流接入标准 `RiskEvent`
- 跑通一个最小场景的 `FIRST_HIT`
- 输出 `DecisionResult`
- side output 输出 `DecisionLogRecord`、`EngineErrorRecord`
- 先实现一个最小 stream feature 和一个最小 lookup

本周退出标准：

- 版本切换可见且可验证
- 可以观察到结果流、日志流、错误流
- 最小主链路可稳定重复演示

---

## 10. 每周验收都必须关注的测试重点

如果资源有限，不可能一次做满所有测试，因此测试也必须按价值排序。

### 10.1 第一优先级

最先要覆盖的是：

- 表达式执行器
- 规则执行器
- 策略执行器
- Snapshot 编译器
- 仿真测试
- 一个完整主链路联调测试

这些测试最值钱，因为它们直接决定“逻辑正确性 + 主链路一致性”是否成立。

### 10.2 第二优先级

主链路稳定后，再加强：

- Flink 特征状态逻辑测试
- Broadcast State 切换测试
- Redis lookup 组件测试
- 命中原因渲染测试

### 10.3 第三优先级

更靠后的增强测试包括：

- 回放测试
- 压测脚本
- 恢复测试
- 长稳测试

也就是说，一期并不要求把整套测试体系全部补满，但一定要先把最关键的“内核正确性 + 版本一致性 + 仿真一致性”测扎实。

---

## 11. 一期最应该防的几个风险点

### 11.1 风险点一：把执行语义分散到多个地方

如果把规则执行分别写在：

- 后台 Service
- Flink `ProcessFunction`
- Simulation Controller

最终一定会出现：

- 线上一套；
- 仿真一套；
- 本地调试一套。

正确做法是：

- 所有执行语义尽量集中到统一执行内核；
- 其他层只做适配，不重复定义规则语义。

### 11.2 风险点二：Flink 直接依赖设计态表结构

如果 `pulsix-engine` 运行时直接读设计态表来拼装执行语义，那么：

- 设计态和运行态会混在一起；
- 发布、回滚、版本一致性会很难保证；
- 仿真和线上结果也更容易分裂。

正确原则是：

- Flink 只认运行时快照；
- 设计态表只服务于发布编译。

### 11.3 风险点三：一开始就全量上 `Groovy`

`Groovy` 不是不能做，而是不适合一上来就作为一期主能力。

因为它会立刻引入：

- 沙箱
- 类加载隔离
- import 限制
- 反射禁用
- 资源访问禁用

这些问题对一期来说过重。

因此更合理的做法是：

- 一期先以 Aviator/DSL 为主；
- `Groovy` 作为少量高级扩展能力，放在后续阶段加强。

### 11.4 风险点四：先做很多页面，再回头补执行内核

这种路径很容易做成：

- 页面很多；
- 配置很多；
- 但执行链路并不可信。

对于 `pulsix` 当前阶段，更合理的顺序仍然是：

- 先打通发布 -> 执行 -> 仿真 -> 日志
- 再让页面随着主链路一起补齐。

---

## 12. 现阶段的最终建议

如果当前只能选一条最有价值的主线，那么建议明确为：

> **先围绕 `pulsix-engine` 的核心执行闭环推进，但最小范围必须包含 `release + simulation`。**

更具体地说：

- 不是先铺开整个平台；
- 不是只做 `engine` 一个模块；
- 不是先做大量页面或外围治理能力；
- 而是先证明“同一份快照在本地、仿真、Flink 上都能给出一致结果”。

只要这件事做成了，`pulsix` 的最核心技术不确定性就基本被拿下了。

一旦这件事还没成立，那么其他外围模块做得越多，后面返工概率反而越高。

因此，这条路线并不是“偷懒只做核心”，而是当前阶段最理性的工程取舍。
