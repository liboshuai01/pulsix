# `pulsix-engine / pulsix-kernel` 一期开发指南

> 基于 `2026-03-14` 仓库与文档现状整理。主要参考：`docs/wiki/风控功能清单.md`、`docs/wiki/风控功能模块与表映射.md`、`docs/wiki/项目架构及技术栈.md`、`docs/wiki/接入层设计.md`、`docs/wiki/pulsix-access-功能开发阶段划分.md`、`docs/wiki/pulsix-module-risk-管理端页面开发阶段划分.md`、`docs/sql/pulsix-risk.sql`，以及当前 `pulsix-framework/pulsix-kernel` / `pulsix-engine` 代码。
>
> 当前判断：`pulsix-kernel + pulsix-engine` 的一期运行时基线已经完成。本文不再展开历史实现细节，而是只保留 **当前基线、与最终功能清单的差距、推荐后续阶段、默认验收口径**，方便你和后续 AI 直接继续推进。

---

## 1. 一页结论

- 当前已经完成的不是“半成品骨架”，而是 **可编译、可执行、可仿真、可回放、可热更新、可回归** 的运行时内核基线。
- 向 `docs/wiki/风控功能清单.md` 靠齐时，后续重点不再是重做 `kernel`，而是补齐 **控制面发布闭环、接入层正式联调、查询追溯闭环、观测恢复、默认演示包**。
- 默认最优顺序是：**发布/仿真/回放统一契约 -> 接入层到引擎端联调 -> 结果/日志/错误查询闭环 -> 监控恢复收口 -> 一键演示收口**。
- 后续如果需要改 `pulsix-module-risk` 或 `pulsix-access`，目标也应是 **和 `kernel / engine` 契约对齐**，不是各自再实现一套执行或标准化逻辑。

---

## 2. 本文边界

### 2.1 已完成基线

- `pulsix-kernel` 已承载共享执行语义：运行时快照、编译、特征、规则、策略、本地执行、仿真、回放、golden case。
- `pulsix-engine` 已承载 Flink 适配：快照热切换、Kafka 事件流、Redis lookup、版本治理、错误输出、指标与恢复相关回归。
- `FIRST_HIT` 与 `SCORE_CARD` 都已可执行，且最小解释字段已经具备。
- `scene_release.snapshot_json` 已是统一运行态配置入口，`RiskEvent` 已是统一标准事件入口。
- 运行时一期主线已经闭环：

```text
scene_release.snapshot_json
  -> RuntimeCompiler / CompiledSceneRuntime
  -> LocalSimulation / Replay / Flink Runtime

standard RiskEvent
  -> Stream Feature / Lookup Feature / Derived Feature
  -> Rule / Policy
  -> DecisionResult / DecisionLogRecord / EngineErrorRecord
```

### 2.2 本文继续跟踪什么

- `kernel / engine` 与 `module-risk` 的 **发布、仿真、回放、查询** 契约收口。
- `pulsix-access` 与 `pulsix-engine` 的 **标准事件正式联调**。
- `DecisionResult / DecisionLogRecord / EngineErrorRecord` 的 **稳定下游查询链路**。
- 本地 / Docker / Demo 环境的 **固定回归入口与默认样例**。

### 2.3 明确后置什么

- 不先做 CEP、复杂序列规则、拖拽式策略编排。
- 不先做灰度发布、多环境推广、多租户 SaaS。
- 不先做同步在线决策 API。
- 不把 `Groovy` 扩张为主路径能力。
- 不把机器学习平台和离线训练链路一起卷进一期主线。

---

## 3. 当前基线快照

### 3.1 模块定位

- `pulsix-framework/pulsix-kernel`：共享执行语义、仿真、回放、回归基座。
- `pulsix-engine`：Flink 运行适配、快照切换、状态管理、Kafka 输入输出、错误与指标。
- `pulsix-access`：统一接入、鉴权、标准化、Kafka 投递；它的阶段拆分见 `docs/wiki/pulsix-access-功能开发阶段划分.md`。
- `pulsix-module-risk`：设计态配置、发布、仿真、日志、回放、治理页面；它的阶段拆分见 `docs/wiki/pulsix-module-risk-管理端页面开发阶段划分.md`。

### 3.2 已完成阶段摘要

| 阶段 | 已完成内容 | 后续结论 |
| --- | --- | --- |
| 阶段 1 | `kernel` 共享代码已归位到 `pulsix-framework/pulsix-kernel` | 不再回退，不再重做模块搬迁 |
| 阶段 2 | 本地仿真 Runner 已完成 | 后续只做管理端复用与报告模型对齐 |
| 阶段 3 | 轻量回放 / diff / golden case 已完成 | 后续只做页面与样例数据收口 |
| 阶段 4 | `scene_release` 已支持 `demo/file/jdbc/cdc` | 后续重点是和发布中心联调 |
| 阶段 5 | Kafka 主链路已完成 | 后续重点是接入层正式联调 |
| 阶段 6 | Redis lookup、超时、默认值、降级已完成 | 后续重点是真实联调与观测 |
| 阶段 7 | timeline、future version、回滚、运行时约束已完成 | 后续重点是发布中心复用与验收 |
| 阶段 8 | 错误分级、指标、恢复 / 状态清理回归已完成 | 后续重点是监控与演示收口 |

### 3.3 固定验证入口

- `mvn -q -pl pulsix-framework/pulsix-kernel,pulsix-engine test`
- `mvn -q -pl pulsix-engine test`
- `bash scripts/decision-engine-job-demo-smoke.sh`
- 如果改到 `pulsix-access`，额外按其文档最小口径执行：`mvn -q -pl pulsix-access -am test -Dsurefire.failIfNoSpecifiedTests=false`

---

## 4. 对齐 `风控功能清单` 的差距清单

> 下表按 **`kernel / engine` 联调验收口径** 判断。即使外围页面、表结构或文档已经存在，只要还没有形成稳定的端到端验收链路，本文仍视为“后续目标”。

| 功能清单项 | 当前判断 | 还差什么 | 优先级 |
| --- | --- | --- | --- |
| 场景与事件模型 | 部分完成 | 管理态表与页面基本已具备；仍需冻结 `event_schema + event_field_def + ingest_mapping_def -> RiskEvent` 的统一契约与错误码 | `P1` |
| 事件接入层 | 部分完成 | `pulsix-access` 能产出标准事件，但还要固化 `HTTP / Beacon / SDK -> Kafka -> Engine` 的正式联调与字段贯通 | `P0` |
| 特征中心 | 已完成（内核） | 继续保持与管理端配置一致，不再扩第二套特征计算语义 | `P2` |
| 规则中心 | 已完成（内核） | 继续保持页面配置字段、命中原因模板与内核执行口径一致 | `P2` |
| 策略中心 | 已完成（内核） | `FIRST_HIT / SCORE_CARD` 后续只做展示、发布、仿真、回放的一致性收口 | `P1` |
| 发布中心 | 部分完成 | 运行时 `compile-before-activate` 已完成，但还要固化发布预检、快照预览、回滚与 `RuntimeCompiler` 的复用边界 | `P0` |
| 运行时快照 + 热更新 | 已完成（运行时） | 后续主要是和管理端发布动作、JDBC/CDC 实际链路做联调验收 | `P1` |
| Flink 实时执行引擎 | 已完成（主链路） | 后续主要是正式接入、查询下沉和运维脚本收口 | `P1` |
| 决策日志与追溯 | 部分完成 | Kafka 输出已有，但还要固化 `result / log / error` 下沉、查询模型、`traceId / eventId` 联查链路 | `P0` |
| 仿真测试 | 部分完成 | `kernel` CLI 已完成，但还要和管理端报告模型、样例数据、golden case 口径统一 | `P0` |
| 回放对比 | 部分完成 | `kernel` 已完成，但还要和页面输入源、diff 摘要、样例任务保持一致 | `P1` |
| Dashboard / 基础监控 | 部分完成 | 指标基础已具备，但还要固化指标命名、告警阈值、恢复 drill 与页面口径 | `P1` |
| 最小可运行交付 | 部分完成 | 还要收口 `docker compose + 初始化数据 + README + smoke`，形成新环境可复现 Demo 包 | `P0` |
| 一期明确后置项 | 后置 | CEP、复杂序列、拖拽编排、灰度发布、多租户、同步在线决策 API | `-` |

---

## 5. 推荐后续阶段（从当前基线继续）

### 5.1 主阶段

| 阶段 | 本阶段只做什么 | 主要模块 | 完成标准 |
| --- | --- | --- | --- |
| 阶段 9 | 发布 / 仿真 / 回放统一契约 | `pulsix-kernel`、`pulsix-module-risk`、必要时 `pulsix-server` | 发布预检、快照预览、仿真、回放全部复用 `kernel`；输出字段、错误码、样例数据口径一致 |
| 阶段 10 | 接入层到引擎端正式联调 | `pulsix-access`、`pulsix-engine` | `HTTP / Beacon / SDK` 至少各 1 条固定样例打通到 `DecisionResult / DecisionLogRecord / EngineErrorRecord` |
| 阶段 11 | 结果 / 日志 / 错误查询闭环 | `pulsix-engine`、`deploy`、`pulsix-module-risk` | 可按 `traceId / eventId` 查询结果、命中明细、错误记录，字段口径固定 |
| 阶段 12 | 观测、恢复与运行脚本收口 | `pulsix-engine`、`deploy` | 指标、告警、checkpoint/restore、状态清理、smoke 脚本固定，可重复验证 |
| 阶段 13 | 一键演示与默认样例收口 | `deploy`、`docs`、`sql`、必要时全模块 | 新环境按文档可完成“发布 -> 接入 -> 决策 -> 查询 -> 仿真 -> 回滚”的完整演示 |
| 阶段 14 | 一致性清理与收尾 | 跨模块小修 | SQL、菜单、权限、样例、Topic、README、字段名保持一致，不留下第二套逻辑 |

### 5.2 如果让 AI 按最小步推进

推荐拆成下面这些最小执行单元，按顺序做最稳：

1. `9A`：发布预检 / 快照预览复用 `RuntimeCompiler`
2. `9B`：仿真报告模型与页面展示字段对齐
3. `9C`：回放 diff / golden case 报告模型与页面字段对齐（已完成，Golden Case 生成/校验闭环已接通）
4. `10A`：`HTTP -> Kafka -> Engine` 固定样例联调（已完成，固定 smoke 脚本与 kernel 对齐验收口径已落地）
5. `10B`：`Beacon -> Kafka -> Engine` 固定样例联调（已完成，Beacon smoke 与 engine 包装脚本已落地）
6. `10C`：`SDK -> Kafka -> Engine` 固定样例联调
7. `11A`：`DecisionResult / DecisionLogRecord / EngineErrorRecord` 下沉字段冻结
8. `11B`：`traceId / eventId` 查询页与数据模型收口
9. `12A`：指标名、告警与恢复 drill 固化
10. `13A`：Docker Demo、初始化数据与 README 收口

### 5.3 推荐停点

- **停点 A：阶段 10 后** —— 已具备“正式接入 -> 实时决策”的主链路演示能力。
- **停点 B：阶段 11 后** —— 已具备“可查结果、可追溯、可比对”的平台核心闭环。
- **停点 C：阶段 13 后** —— 已具备面向新人或演示环境的一键运行能力。

---

## 6. 每阶段都必须满足的基线

### 6.1 代码基线

- 不再新增第二套执行语义；发布、仿真、回放、页面验证统一复用 `pulsix-kernel`。
- 需要改 `pulsix-module-risk` 或 `pulsix-access` 时，只以“契约收口”为目标，不在外围模块复制内核能力。
- Demo 样例只能沉淀为回归基线，不能删掉导致最小链路失效。

### 6.2 测试基线

- 每个阶段至少保留 1 条成功路径回归。
- 每个阶段至少补 1 条失败路径回归。
- 改 `kernel / engine` 时，上面 `3.3` 的固定命令必须继续通过。
- 改外围模块时，还要补目标模块自己的最小回归，但不要因此顺手扩成下一个阶段。

### 6.3 人工验收基线

- 必须能说清楚“本阶段新增什么，不新增什么”。
- 必须给出固定输入样例。
- 必须给出固定输出预期。
- 必须说明失败时会看到什么错误表现。
- 如果同时存在 CLI、页面、Flink 三种入口，三者结果口径必须一致。

---

## 7. 给后续 AI 助手的默认工作假设

1. 阶段 1 ~ 8 已完成，默认不回头重做。
2. 后续优先级按“阶段 9 -> 10 -> 11 -> 12 -> 13 -> 14”推进，除非你明确改顺序。
3. `scene_release.snapshot_json` 仍是唯一运行态配置来源。
4. `RiskEvent` 仍是唯一标准事件模型；`pulsix-access` 不应再发明第二套运行时事件结构。
5. 发布、仿真、回放必须复用同一套 `kernel` 语义。
6. 表达式主力仍是 `Aviator`，`Groovy` 继续只做补位能力。
7. “页面已存在”不等于“端到端已验收”；本文以后者为准。
8. 每次 AI 改造都优先做 **一个最小可验证子步骤**，并留下明确人工验收方法。

---

## 8. 快速记忆版

- `kernel / engine` 一期运行时基线已经完成，不要再回头重写内核。
- 后续最重要的是 5 件事：**发布契约、接入联调、查询闭环、监控恢复、默认演示包**。
- 最高优先级是：**阶段 9 -> 阶段 10 -> 阶段 11**。
- 任何外围页面、发布、仿真、回放能力，都不能绕过 `pulsix-kernel` 另起一套逻辑。
- 一期继续明确后置：**CEP、复杂序列、拖拽编排、灰度发布、多租户、同步在线决策 API**。
