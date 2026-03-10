# pulsix-engine-kernel 一期进度

> 截至 `2026-03-11`
>
> 这份文档只回答四个问题：现在做到哪一步、已经落了什么、还缺什么、下一步先做什么。

配套阅读：

- 目标边界看 `docs/wiki/pulsix-engine-kernel-一期开发指南.md`
- AI 执行协议看 `docs/wiki/pulsix-engine-kernel-一期-ai自动化开发方案.md`
- 当前推荐下一张任务卡：`docs/wiki/pulsix-engine-kernel-一期任务卡-K02.md`（从 `K02a` 开始）

---

## 1. 一句话结论

当前已经完成 **执行内核主语义 + Flink 最小闭环 + JDK17/Flink 序列化稳定化**，`pulsix-engine` 可以在本地稳定跑通 demo 场景；但 **真实 Kafka/Redis/CDC/生产化治理** 还没有接上。

也就是说：

- **“能跑、能测、能解释”已经有了**
- **“可直接上生产”还没有到**

---

## 2. 当前阶段总览

| 主题 | 状态 | 说明 |
| --- | --- | --- |
| 运行时模型与快照契约 | 已完成 | `RiskEvent`、`SceneSnapshot`、`DecisionResult`、`DecisionLogRecord`、`EngineErrorRecord` 已稳定成型 |
| 运行时编译与执行主链路 | 已完成 | `RuntimeCompiler`、`DecisionExecutor`、`SceneRuntimeManager`、`LocalDecisionEngine` 已可用 |
| `FIRST_HIT` 决策模式 | 已完成 | demo、单测、Flink 主链路都已覆盖 |
| `SCORE_CARD` 决策模式 | 部分完成 | `DecisionExecutor` 已有分支，但缺专门样例、回归测试和生产化验证 |
| Flink Broadcast + Keyed State 主链路 | 已完成 | 广播快照、事件处理、side output、event-time timer 清理已跑通 |
| Flink/JDK17 序列化稳定性 | 已完成 | 当前主链路已去掉 `--add-opens` 依赖，显式类型信息与集合归一化已补齐 |
| 本地 demo 运行与 checkpoint | 已完成 | `DecisionEngineJob` 能跑完、能 checkpoint、能正常退出 |
| kernel 独立模块沉淀 | 未完成 | 当前执行语义仍主要落在 `pulsix-engine` 内部，尚未抽成独立 engine-kernel 产物 |
| 真实 Kafka Source/Sink | 未完成 | 当前仍是 `DemoFixtures + print` |
| 真实 Redis Lookup | 未完成 | 当前仍是 `InMemoryLookupService.demo()` |
| 快照版本治理（延迟生效/回滚） | 未完成 | 当前只有基础版本缓存与切换，缺完整治理 |
| 生产化指标/恢复/回放 | 未完成 | 还没有形成完整可运营能力 |

---

## 3. 已经落地的关键能力

### 3.1 执行语义主链路已经通了

当前代码已经具备完整的最小决策闭环：

1. 加载 `RiskEvent`
2. 读取当前 `SceneSnapshot` / `SceneSnapshotEnvelope`
3. 构建 `EvalContext`
4. 计算 `Stream Feature`
5. 计算 `Lookup Feature`
6. 计算 `Derived Feature`
7. 执行 `Rule`
8. 按 `Policy` 收敛为 `DecisionResult`
9. 输出 `DecisionLogRecord` / `EngineErrorRecord`

对应核心类主要是：

- `pulsix-engine/src/main/java/cn/liboshuai/pulsix/engine/core/DecisionExecutor.java:26`
- `pulsix-engine/src/main/java/cn/liboshuai/pulsix/engine/runtime/RuntimeCompiler.java:29`
- `pulsix-engine/src/main/java/cn/liboshuai/pulsix/engine/runtime/SceneRuntimeManager.java:12`
- `pulsix-engine/src/main/java/cn/liboshuai/pulsix/engine/core/LocalDecisionEngine.java:12`

### 3.2 Demo 场景已经可解释

当前内置了一套 `TRADE_RISK` demo：

- 3 个流式特征：交易次数、金额和、设备关联用户数
- 2 个 lookup 特征：设备黑名单、用户风险等级
- 2 个派生特征：高金额标记、短时高频标记
- 3 条规则：黑名单拒绝 / 高频复核 / 多账号设备拒绝
- 1 条主策略：`FIRST_HIT`

对应样例来自：

- `pulsix-engine/src/main/java/cn/liboshuai/pulsix/engine/demo/DemoFixtures.java:14`

### 3.3 流式状态已经不是纯空壳

当前已经同时具备两套状态适配：

- 本地内存态：`InMemoryStreamFeatureStateStore`
- Flink Keyed State：`FlinkKeyedStateStreamFeatureStateStore`

已支持的基础聚合包括：

- `COUNT`
- `SUM`
- `MAX`
- `LATEST`
- `DISTINCT_COUNT`

并且已经补上：

- event-time timer 注册
- TTL / retention 清理
- 数值窗口、最新值、去重窗口三类状态清理

对应类：

- `pulsix-engine/src/main/java/cn/liboshuai/pulsix/engine/feature/FlinkKeyedStateStreamFeatureStateStore.java:15`
- `pulsix-engine/src/main/java/cn/liboshuai/pulsix/engine/feature/AbstractStreamFeatureStateStore.java:17`

### 3.4 Flink 运行闭环已经打通

当前 `DecisionBroadcastProcessFunction` 已经具备：

- 接收快照广播流
- 接收标准事件流
- 快照缺失时按 `sceneCode` 做内存级 pending buffer
- 快照到达后激活 runtime 并处理事件
- 输出结果流、日志流、错误流
- 处理快照版本冲突与低版本忽略

对应类：

- `pulsix-engine/src/main/java/cn/liboshuai/pulsix/engine/flink/DecisionBroadcastProcessFunction.java:31`
- `pulsix-engine/src/main/java/cn/liboshuai/pulsix/engine/flink/DecisionEngineJob.java:33`

### 3.5 Flink 类型信息和 JDK17 兼容已经稳定

这一轮已经额外收掉了几个关键技术坑：

- 去掉当前主链路对 `--add-opens` 的依赖
- 显式声明核心模型 `TypeInformation`
- 收紧状态对象序列化结构
- 归一化集合类型，避免 `List.of()` / 不可变集合穿过 Flink 边界
- 新增回归测试，防止再次退回 `GenericType`

对应类：

- `pulsix-engine/src/main/java/cn/liboshuai/pulsix/engine/flink/typeinfo/EnginePojoTypeInfos.java:1`
- `pulsix-engine/src/main/java/cn/liboshuai/pulsix/engine/flink/typeinfo/EngineTypeInfoFactories.java:1`
- `pulsix-engine/src/main/java/cn/liboshuai/pulsix/engine/support/CollectionCopier.java:1`
- `pulsix-engine/src/test/java/cn/liboshuai/pulsix/engine/flink/FlinkTypeInfoRegressionTest.java:13`

### 3.6 本地运行日志已经收口

本地 `DecisionEngineJob` 已处理以下噪音：

- `winutils.exe` / Hadoop config warning
- Flink 自身日志占位符 warning
- `web.log.path` / `log.file` 缺失 warning

对应配置：

- `pulsix-engine/src/main/java/cn/liboshuai/pulsix/engine/flink/DecisionEngineJob.java:93`
- `pulsix-engine/src/main/java/cn/liboshuai/pulsix/engine/flink/DecisionEngineJob.java:113`
- `pulsix-engine/src/main/resources/log4j2.xml:1`

---

## 4. 已验证到什么程度

当前至少已经验证了下面这些点：

- 本地执行能得到预期拒绝结果
- 黑名单规则能直接拒绝
- 快照广播更新后，Flink 能切到新版本运行时
- event-time timer 能触发状态清理
- runtime manager 能缓存并激活多个版本
- JSON 编解码可稳定解析样例
- Flink 核心模型不会退回 `GenericTypeInfo`
- JDK17 下当前主链路不再需要 `--add-opens`
- IDEA 本地运行 `DecisionEngineJob` 已成功结束，退出码为 `0`

主要测试文件：

- `pulsix-engine/src/test/java/cn/liboshuai/pulsix/engine/core/LocalDecisionEngineTest.java:19`
- `pulsix-engine/src/test/java/cn/liboshuai/pulsix/engine/flink/DecisionBroadcastProcessFunctionTest.java:18`
- `pulsix-engine/src/test/java/cn/liboshuai/pulsix/engine/runtime/SceneRuntimeManagerTest.java:10`
- `pulsix-engine/src/test/java/cn/liboshuai/pulsix/engine/json/EngineJsonTest.java:14`
- `pulsix-engine/src/test/java/cn/liboshuai/pulsix/engine/flink/FlinkTypeInfoRegressionTest.java:13`

---

## 5. 当前明确还没做完的事情

### 5.1 真实输入输出链路还没接

当前仍然是：

- 输入：`DemoFixtures` 造事件 / 快照
- 输出：控制台 `print`

还没有真正接入：

- Kafka 事件输入
- `scene_release` / CDC 快照输入
- Kafka / Doris / MySQL 输出落地

### 5.2 lookup 还是 demo 级实现

当前 `LookupService` 抽象已经有了，但真实能力还没落地：

- Redis 查询
- timeout
- 本地缓存
- 降级兜底
- 监控指标

### 5.3 版本治理还只是基础版

当前 `SceneRuntimeManager` 只具备：

- 编译
- 激活
- 最多保留少量历史版本

还没有：

- `effectiveFrom` 真正生效控制
- 延迟生效
- 回滚
- 编译失败保留旧版本的完整治理语义
- 跨 checkpoint / 恢复后的版本一致性验证

### 5.4 kernel 还没有真正独立出来

虽然文档里讲的是 `kernel + engine` 两层，但目前代码现实是：

- 执行语义核心类还主要都在 `pulsix-engine`
- 还没有一个独立、清晰的 `engine-kernel` 产物边界

这点对后续仿真、回放、控制面复用会有影响。

### 5.5 生产化治理还不够

当前缺少的生产化能力主要有：

- 完整指标体系
- 失败重试与异常分级治理
- Checkpoint / Recovery / 一致性专项验证
- 回放工具与 golden case 套件
- Groovy 沙箱与隔离
- 长稳与压测验证

---

## 6. 后面建议优先做什么

按当前实际代码状态，建议下一步按这个顺序推进：

### P0：继续收紧可回归性

先补齐：

- `SCORE_CARD` 的样例和测试
- 更多 golden case
- 快照切换 / 顺序乱序 / 恢复场景测试

### P1：把 demo 输入输出替换成真实链路

优先替换：

- 事件输入 -> Kafka Source
- 快照输入 -> `scene_release` 的 CDC / 广播流
- 结果输出 -> Kafka / DB sink

### P2：落真实 LookupService

优先做：

- Redis lookup
- timeout / cache / fallback
- 监控埋点

### P3：补版本治理

重点补：

- `effectiveFrom`
- 延迟生效
- 回滚
- 编译失败保留旧版本
- 版本切换一致性验证

### P4：再做平台化配套

最后再做：

- 仿真接口化
- 轻量回放 / 重型回放
- 指标体系
- 错误追踪
- 控制面包装

---

## 7. 当前默认判断（给你和后续 AI 用）

如果没有新的额外指令，当前可以默认认为：

1. 主开发范围仍然是 `pulsix-engine`
2. 当前最稳定的主链路是 `FIRST_HIT`
3. `SCORE_CARD` 只算“有代码入口”，不算“已稳定交付”
4. 当前 lookup 仍然是 demo 级，不要假设 Redis 已接好
5. 当前 Flink 主链路已能本地稳定跑通
6. 当前 JDK17 / Flink 序列化问题已在主路径收口
7. 当前还不适合把重心转去 `module-risk` 页面或 `access` 接入层
8. 下一阶段应该优先补真实输入输出、lookup、版本治理和回归能力

---

## 8. 快速记忆版

只记住下面 6 句话就够：

- 现在已经不是“空骨架”，而是“能跑通的最小引擎”。
- 真正稳定的是 `FIRST_HIT` 主链路，不是整个平台。
- Flink Broadcast、Keyed State、timer、checkpoint、side output 已经打通。
- JDK17 / Flink 序列化和本地 warning 这轮已经收过了。
- 真实 Kafka / Redis / CDC / 输出链路还没接上。
- 下一步不是做页面，而是做真实输入输出和生产化收口。
