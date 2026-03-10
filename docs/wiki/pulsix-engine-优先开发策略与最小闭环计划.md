## 1. 核心结论

当前阶段的开发中心只放在：

- `pulsix-kernel`
- `pulsix-engine`

暂时不作为开发重点的模块：

- `pulsix-module-risk`
- `pulsix-access`

当前阶段的替代方案：

- `scene_release`：直接用 SQL 维护快照数据
- mock 事件：直接用 Kafka 工具、IDEA Kafka 插件或本地 producer 发送

目标不是先把平台做完整，而是先验证：

- `kernel` 的执行语义是否能落地
- `engine` 的 Flink 链路是否能跑通
- 本地执行与 Flink 执行是否一致

---

## 2. 当前最小闭环

当前最小闭环建议如下：

```text
SceneSnapshot JSON
    -> SQL 写入 scene_release
    -> pulsix-kernel 编译并本地执行
    -> pulsix-engine 加载快照并接收 Kafka 事件
    -> 输出 DecisionResult / Log / Error
```

当前阶段真正必须有的东西：

- 一份稳定的 `SceneSnapshot`
- 一份稳定的 `RiskEvent`
- 一张 `scene_release` 表
- 一个 Kafka 标准事件 topic
- `kernel` 本地验证能力
- `engine` Flink 执行能力
- 一组固定样例数据

---

## 3. 各模块当前定位

### 3.1 `pulsix-kernel`

当前阶段的核心模块，负责统一执行语义。

建议放：

- `RiskEvent`
- `SceneSnapshot`
- `EvalContext`
- `DecisionResult`
- `RuntimeCompiler`
- `SceneRuntimeManager`
- `DecisionExecutor`
- 规则/策略执行逻辑
- 本地 runner / 仿真 / 回放 / 测试支撑能力

要求：

- 纯执行语义
- 不依赖 Spring MVC
- 不依赖 MyBatis
- 不依赖 Flink API

### 3.2 `pulsix-engine`

当前阶段与 `kernel` 并列主线，负责 Flink 运行适配。

建议放：

- Flink Job
- 事件流消费
- 快照广播流消费
- Broadcast State / Keyed State 适配
- lookup 适配
- side output 输出
- Kafka sink / error sink / log sink

要求：

- 只认运行时快照
- 不直接读取设计态表拼执行逻辑
- 不承载控制面逻辑

### 3.3 `pulsix-module-risk`

当前阶段：

- 先不开发
- 先不用做页面、接口、CRUD
- 只保留 `scene_release` 这类最小发布结果

后续阶段：

- 再作为控制面接口层
- 依赖 `pulsix-kernel`
- 提供发布、仿真、回放、查询能力

### 3.4 `pulsix-access`

当前阶段：

- 先不开发
- 直接用 Kafka 工具 / mock producer 替代

后续阶段：

- 再恢复为正式接入层
- 承载标准化、鉴权、补齐、错误分流能力

---

## 4. 仿真与回放放在哪里

当前阶段：

- 优先放在 `pulsix-kernel` 或 `pulsix-engine` 内部
- 形式可以是：
  - 单元测试
  - 集成测试
  - 本地 runner
  - 命令行工具

后续阶段：

- 由 `pulsix-module-risk` 依赖 `pulsix-kernel`
- 对外提供：
  - `/simulation/evaluate`
  - `/replay/*`
  - `/release/*`

必须坚持的原则：

- `module-risk` 只能做接口包装
- 不能再实现第二套执行逻辑
- 仿真、回放、线上执行必须复用同一套 `kernel`

---

## 5. 当前必须先稳定的契约

### 5.1 `RiskEvent`

至少固定：

- `eventId`
- `sceneCode`
- `eventType`
- `eventTime`
- `traceId`
- `entity fields`
- `ext`

### 5.2 `SceneSnapshot`

至少固定：

- `scene`
- `features`
- `rules`
- `policy`
- `runtimeHints`
- `version`
- `checksum`

### 5.3 `EvalContext`

至少固定：

- `base fields`
- `feature values`
- `lookup values`
- `derived values`
- `metadata`

### 5.4 `DecisionResult`

至少固定：

- `sceneCode`
- `version`
- `finalAction`
- `score`
- `hitRules`
- `hitReasons`
- `latencyMs`
- `traceId`

### 5.5 固定样例

当前阶段必须补齐：

- 样例快照 JSON
- 样例事件 JSON
- 样例 SQL
- 样例 Kafka 消息

---

## 6. 推荐开发顺序

### 第 1 周：打稳 `kernel`

目标：给定快照和事件，本地稳定出结果。

重点：

- 冻结核心模型
- 打磨 `RuntimeCompiler`
- 打磨 `DecisionExecutor`
- 固化最小样例和 golden case

### 第 2 周：补本地验证工具

目标：不依赖 `module-risk`，也能做仿真/回放验证。

重点：

- 本地 runner 或命令行工具
- 支持读取快照 JSON 和事件 JSON
- 输出命中规则、原因、特征快照、最终动作

### 第 3 周：打通 `engine`

目标：从 Kafka 收事件，从快照流拿版本，输出结果流。

重点：

- 打磨 `DecisionEngineJob`
- 接入最小快照流
- 接入标准事件流
- 跑通一个 `FIRST_HIT` 场景

### 第 4 周：增强状态与版本切换

目标：让 `kernel + engine` 更接近可演进形态。

重点：

- 向 Flink Keyed State 过渡
- 增加状态清理、TTL、版本切换校验
- 增强错误分类和一致性校验

---

## 7. 当前最值钱的测试

第一优先级：

- 表达式执行器测试
- 规则执行器测试
- 策略执行器测试
- Snapshot 编译器测试
- 本地 runner / 仿真样例测试
- 一条 Kafka -> Flink -> 输出 的主链路测试

测试目标不是接口覆盖率，而是：

- `kernel` 执行正确
- `engine` 运行一致
- 版本切换可解释

---

## 8. 什么时候再启动 `module-risk` 和 `access`

当下面这些条件基本成立后，再正式启动这两个模块：

- `SceneSnapshot` 已比较稳定
- `RiskEvent` 已比较稳定
- 本地 runner 能稳定回归
- Flink 最小主链路已跑通
- 版本切换和错误分类已清晰

到那时：

- `pulsix-module-risk` 再提供正式发布、仿真、回放、查询接口
- `pulsix-access` 再提供正式接入与标准化链路

---

## 9. 最终建议

当前最理性的路线是：

- `pulsix-kernel + pulsix-engine` 作为唯一开发主线
- `pulsix-module-risk` 先用 SQL 替代
- `pulsix-access` 先用 Kafka 工具替代
- 仿真/回放当前先放在 `kernel/engine` 内部
- 后续接口层统一由 `module-risk` 依赖 `kernel` 做包装

核心原则只有一句话：

> 先把执行内核打透，再补控制面和接入面。
