# 任务卡 K02

> 主题：将执行语义核心从 `pulsix-engine` 下沉到 `pulsix-kernel`
>
> 当前推荐直接执行：`K02a`

---

## 1. 为什么现在做 K02

当前现实是：

- `pulsix-framework/pulsix-kernel` 模块已经存在，但基本还是空壳
- 一期里最值钱的执行语义仍主要落在 `pulsix-engine`
- 如果继续在 `pulsix-engine` 上直接叠 Kafka / Redis / CDC，会让 `kernel` 和 `engine` 的边界越来越模糊

所以，`K02` 是一期后续工作的关键前置项。

一句话：

> 先把“怎么算”从 `engine` 里抽出来，后面真实接入和生产化才不容易做偏。

---

## 2. K02 的总目标

完成后应达到：

- `pulsix-kernel` 承载纯执行语义核心
- `pulsix-engine` 主要承载 Flink 适配层
- 本地仿真、轻量回放、Flink 执行可以复用同一套执行内核

---

## 3. K02 拆分

| 子任务 | 状态 | 目标 |
| --- | --- | --- |
| `K02a` | `TODO` | 下沉纯契约 + 纯执行核心，建立 `kernel` 真正承载面 |
| `K02b` | `TODO` | 下沉本地运行支撑（`LocalDecisionEngine` / in-memory 实现 / 必要 JSON 支撑） |
| `K02c` | `TODO` | 收窄 `pulsix-engine`，让其主要只保留 Flink/demo adapter |
| `K02d` | `TODO` | 补回归测试、文档收口、边界复核 |

规则：

- 一次只做一个子任务
- 不允许一口气把 `K02a~K02d` 全做完

---

## 4. 当前可直接执行的任务卡：K02a

### 4.1 ID

- `K02a`

### 4.2 目标

在 `pulsix-kernel` 中建立真实可用的执行语义骨架，并将**纯执行核心**从 `pulsix-engine` 下沉过去；完成后，`pulsix-engine` 应开始依赖 `pulsix-kernel` 中的这些核心类，而不是继续自己持有副本。

### 4.3 必读文档

- `docs/wiki/pulsix-engine-kernel-一期开发指南.md`
- `docs/wiki/pulsix-engine-kernel-一期进度.md`
- `docs/wiki/pulsix-engine-kernel-一期-ai自动化开发方案.md`
- `docs/wiki/项目架构及技术栈.md`

### 4.4 In Scope

本次只允许处理下面这些内容：

1. 在 `pulsix-framework/pulsix-kernel` 中建立源码目录与包结构
2. 将以下**纯执行契约 / 核心类**下沉到 `pulsix-kernel`
3. 调整 `pulsix-engine` 对这些类的 import / 依赖引用
4. 删除 `pulsix-engine` 中已经迁走且不再保留的重复类
5. 更新进度文档与 AI 方案中的任务状态

### 4.5 推荐迁移范围

建议优先下沉下面这些类：

- 模型层：`model/*`
- 上下文层：`context/EvalContext`
- 支撑层：
  - `support/CollectionCopier`
  - `support/DurationParser`
  - `support/TemplateRenderer`
  - `support/ValueConverter`
- 脚本层：`script/*`
- 运行时层：
  - `runtime/CompiledSceneRuntime`
  - `runtime/RuntimeCompiler`
  - `runtime/SceneRuntimeManager`
- 决策执行层：`core/DecisionExecutor`
- 接口层：
  - `feature/LookupService`
  - `feature/StreamFeatureStateStore`

### 4.6 包命名建议

建议下沉后使用清晰的新包前缀：

- `cn.liboshuai.pulsix.kernel.*`

原因：

- 模块名和包名一致
- 后续不容易把 `engine` / `kernel` 的职责继续混在一起

如果本次评估发现“一次性改包成本过高”，允许先保留旧包前缀，但必须在进度文档里明确写明这是过渡方案。

### 4.7 Out of Scope

本次明确不做：

- 不动 `Kafka` / `Redis` / `CDC` / `Doris`
- 不做 `module-risk` / `access` / UI 扩展
- 不动 Flink 适配层核心：
  - `DecisionBroadcastProcessFunction`
  - `DecisionEngineJob`
  - `EngineOutputTags`
  - `flink/typeinfo/*`
  - `FlinkKeyedStateStreamFeatureStateStore`
- 不处理真实 sink/source
- 不处理 `effectiveFrom` / rollback / recovery 语义增强
- 不做大规模测试体系重构

### 4.8 建议保留到 K02b 的内容

以下内容建议下一张卡再处理：

- `core/LocalDecisionEngine`
- `feature/InMemoryLookupService`
- `feature/InMemoryStreamFeatureStateStore`
- `json/*`
- 本地 simulation / replay runner

原因：

- 它们更偏本地仿真支撑，不是 `K02a` 的最小必要闭环
- 放到 `K02b` 更容易控制单次上下文和 diff 规模

### 4.9 验收条件

完成 `K02a` 时，至少要满足：

1. `pulsix-kernel` 不再是空壳，已承载一组真实执行核心类
2. `pulsix-engine` 对已迁移类改为依赖 `pulsix-kernel`
3. `pulsix-engine` 内不存在迁移类的重复副本
4. `pulsix-engine` 的 Flink 主链路代码仍能编译
5. 相关单测至少通过一轮
6. `docs/wiki/pulsix-engine-kernel-一期进度.md` 已更新为新的代码现实
7. `docs/wiki/pulsix-engine-kernel-一期-ai自动化开发方案.md` 的任务状态已回写

### 4.10 必跑验证

建议至少跑下面这些命令：

```bash
mvn -q -pl pulsix-framework/pulsix-kernel -DskipTests compile
mvn -q -pl pulsix-engine -DskipTests compile
mvn -q -pl pulsix-engine test -Dtest=LocalDecisionEngineTest,SceneRuntimeManagerTest,DecisionBroadcastProcessFunctionTest,FlinkTypeInfoRegressionTest
```

如果测试拆分参数在当前仓库不适用，再退回：

```bash
mvn -q -pl pulsix-engine test
```

### 4.11 主要风险

- 包重命名可能导致 import 改动面偏大
- `model/*` 下沉后，Flink `TypeInformation` / tests 需要同步改 import
- `StreamFeatureStateStore` / `LookupService` 下沉后，`FlinkKeyedStateStreamFeatureStateStore` 需要正确实现新接口位置
- 如果一次迁太多，容易让本次 diff 过大

### 4.12 推荐执行顺序

1. 先建 `pulsix-kernel` 包结构
2. 先迁纯模型与纯工具类
3. 再迁 `script/runtime/core/interface`
4. 再改 `pulsix-engine` import
5. 最后删除重复类并跑验证
6. 回写进度与任务状态

---

## 5. 给 Codex CLI 的直接执行 Prompt

```text
请执行任务卡 K02a。

开工前必须先读取：
- docs/wiki/pulsix-engine-kernel-一期开发指南.md
- docs/wiki/pulsix-engine-kernel-一期进度.md
- docs/wiki/pulsix-engine-kernel-一期-ai自动化开发方案.md
- docs/wiki/pulsix-engine-kernel-一期任务卡-K02.md
- docs/wiki/项目架构及技术栈.md

任务目标：
- 在 pulsix-kernel 中建立真实可用的执行语义骨架
- 将纯执行契约与纯执行核心从 pulsix-engine 下沉到 pulsix-kernel
- 暂不处理 Flink adapter、Kafka/Redis/CDC、真实 sink/source

工作要求：
- 一次只做 K02a
- 先输出简短计划，再实施
- 优先做最小闭环迁移
- 优先保持 pulsix-engine 现有行为不变
- 完成后必须更新：
  - docs/wiki/pulsix-engine-kernel-一期进度.md
  - docs/wiki/pulsix-engine-kernel-一期-ai自动化开发方案.md

建议验证：
- mvn -q -pl pulsix-framework/pulsix-kernel -DskipTests compile
- mvn -q -pl pulsix-engine -DskipTests compile
- mvn -q -pl pulsix-engine test

最终输出必须包含：
- 改了什么
- 验证了什么
- 还剩什么风险
- 下一张建议任务卡（通常应为 K02b）
```

---

## 6. 完成 K02a 后，下一张卡是什么

默认下一张是：`K02b`

目标：

- 将 `LocalDecisionEngine`、`InMemoryLookupService`、`InMemoryStreamFeatureStateStore`、必要 JSON 支撑下沉到 `pulsix-kernel`
- 让本地 simulation / replay 的最小执行底座跟 Flink adapter 彻底分层
