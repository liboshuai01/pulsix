# pulsix-engine-kernel 一期 AI 自动化开发方案

> 截至 `2026-03-11`
>
> 目标：让 `Codex CLI + GPT-5.4` 可以围绕一期目标持续迭代开发，并尽量避免因上下文漂移、任务扩散、文档失真而把产品做偏。

---

## 1. 这份文档解决什么问题

现有文档分工已经比较清楚，但还缺一层“AI 怎么长期接力开发”的执行协议：

- `docs/wiki/pulsix-engine-kernel-一期开发指南.md`：定义**目标边界**
- `docs/wiki/pulsix-engine-kernel-一期进度.md`：记录**当前做到哪里**
- **本文件**：定义**AI 怎么一步一步做完**

一句话：

> 开发指南回答“要做成什么”，进度文档回答“现在是什么”，本方案回答“AI 下一步应该怎么做且不做偏”。

---

## 2. 参考的官方最佳实践（已核对）

我这次优先参考了 OpenAI 官方近期资料，而没有先用社区经验做主：

- `GPT-5.4` 官方发布说明：适合更长周期、更多工具、更多验证步骤的专业工作流。
- `Codex` / `Codex app` 官方说明：强调 **清晰文档、可靠测试、AGENTS.md、任务切小、并行隔离**。
- OpenAI 官方 agent safety 指南：强调 **工具审批、结构化输入、不要让不可信文本直接驱动工具调用**。
- OpenAI 官方 prompt optimizer / trace grading：强调 **把重复 prompt 和 agent 行为做成可迭代优化的评测闭环**。

转成这套仓库里的做法，核心只有 6 条：

1. **永远把状态写进仓库文档，不依赖长聊天上下文。**
2. **一次只做一个任务卡，不让 AI 跨里程碑自由发挥。**
3. **任务必须带验收条件、测试命令、出界范围。**
4. **每次完成后必须回写进度，不允许“代码变了，文档没变”。**
5. **高风险操作、外部依赖、真实接入必须显式审批。**
6. **把 prompt、测试、回归样例做成可复用资产，而不是一次性对话。**

---

## 3. 单一事实源与优先级

后续 AI 开发时，统一按下面顺序理解需求，避免文档打架：

1. 当前用户的明确要求
2. 系统 / 开发者 / harness 指令
3. `docs/wiki/pulsix-engine-kernel-一期开发指南.md`
4. `docs/wiki/pulsix-engine-kernel-一期进度.md`
5. `docs/wiki/pulsix-engine-kernel-一期-ai自动化开发方案.md`
6. 其他 `docs/wiki` 文档
7. 当前代码现实

如果出现“文档和代码不一致”：

- **不要直接脑补成自己想要的状态**
- 先在进度文档里标明“现状 / 预期差异”
- 再决定是修代码还是修文档

---

## 4. 后续 AI 每次开工必须读取哪些文档

### 4.1 固定必读（每次都读）

- `docs/wiki/pulsix-engine-kernel-一期开发指南.md`
- `docs/wiki/pulsix-engine-kernel-一期进度.md`
- `docs/wiki/pulsix-engine-kernel-一期-ai自动化开发方案.md`

### 4.2 按任务补读（只读相关的）

- 架构/模块边界：`docs/wiki/项目架构及技术栈.md`
- 接入链路：`docs/wiki/接入层设计.md`
- 设计态/发布态映射：`docs/wiki/风控功能模块与表映射.md`
- 一期功能范围：`docs/wiki/风控功能清单.md`

规则：

- 固定必读一定读
- 补充文档只读当前任务直接相关的 1~2 份
- 不允许为了“保险”一次性把所有 wiki 都塞进上下文

---

## 5. 推荐的人机分工

### 5.1 人

负责：

- 选任务卡
- 做范围裁剪
- 审批高风险动作
- 最终验收

### 5.2 GPT-5.4

优先负责：

- 任务拆解
- 方案设计
- diff 审查
- 风险扫描
- 测试与回归建议

适合用在：

- 跨文件设计判断
- 中长期里程碑拆分
- 复杂 diff review
- “现在该先做哪个任务”判断

### 5.3 Codex CLI

优先负责：

- repo 探索
- 改代码
- 跑命令
- 跑测试
- 改文档
- 落地任务卡

适合用在：

- 单个任务卡的实现闭环
- 有明确边界的重构 / 修复 / 接线
- 需要真实命令验证的工作

---

## 6. 必须遵守的迭代协议

每一次 AI 开发，都必须走下面这 8 步：

### Step 0：先选一个任务卡

一次只允许做 **一个任务卡**。

不允许：

- 一次把一个里程碑全做完
- 一次同时做设计、实现、平台包装三件事
- 一次跨 `kernel` / `engine` / `module-risk` / `access` 多条主线乱跳

### Step 1：做开工对齐

开工时必须先回答这 4 个问题：

1. 当前任务卡 ID 是什么？
2. 任务目标是什么？
3. 明确不做什么？
4. 做完以后用什么命令验证？

### Step 2：先出实现计划，再改代码

先让 GPT-5.4 或 Codex 输出一个**小计划**，再动手。

计划至少要有：

- 涉及文件
- 改动点
- 验证方式
- 风险点

### Step 3：只在任务卡范围内实现

如果实现过程中发现“顺手还可以修别的”：

- 默认不做
- 只在结尾作为可选项提示

### Step 4：先跑针对性验证，再跑模块验证

建议顺序：

1. 先跑与改动最接近的测试
2. 再跑 `pulsix-engine` 模块级验证
3. 必要时再跑更大范围验证

### Step 5：回写文档

只要任务有实质性进展，至少要更新下面两处之一：

- `docs/wiki/pulsix-engine-kernel-一期进度.md`
- 本文中的任务卡状态表

### Step 6：给出“完成 / 未完成 / 风险”三段式结论

每次结束必须明确：

- 完成了什么
- 还有什么没做
- 还剩什么风险

### Step 7：如果做的是里程碑收口任务，要补验收结论

比如：

- Kafka Source 已接上
- Redis lookup 已接上
- `effectiveFrom` 已生效
- `SCORE_CARD` 已有 golden case

这些需要在进度文档里写明，不要只留在聊天记录里。

### Step 8：下一次从文档继续，而不是从聊天继续

后续接力时，优先从文档恢复上下文，不依赖“上次大概聊了什么”。

---

## 7. 一期目标覆盖矩阵

下表用于防止 AI 做着做着忘了总体目标。

| 一期目标 | 当前状态 | 主要落点 | 后续任务 |
| --- | --- | --- | --- |
| 运行时模型与快照契约 | 部分完成 | `pulsix-engine` 已成型 | `K01` `K02` |
| `kernel` 纯执行语义独立 | 未完成 | 仍主要在 `pulsix-engine` | `K02` |
| 本地仿真 / 轻量回放 | 部分完成 | 有 `LocalDecisionEngine`，无正式 runner | `K03` `K04` |
| `FIRST_HIT` 主链路 | 已完成 | 本地/Flink/demo 均可跑 | 持续回归 |
| `SCORE_CARD` 主链路 | 部分完成 | 有执行分支，缺样例与回归 | `K05` |
| Flink 最小执行链路 | 已完成 | Broadcast + Keyed State + side output | 持续回归 |
| 真实 Kafka 事件输入 | 未完成 | 仍是 demo source | `E01` |
| 真实快照广播输入 | 未完成 | 仍是 demo snapshot source | `E02` |
| 真实 Redis lookup | 未完成 | 仍是 in-memory demo lookup | `E03` |
| 结果/日志/错误落地输出 | 未完成 | 当前主要是 `print` | `E04` |
| 版本治理 / 延迟生效 / 回滚 | 部分完成 | 有基础 runtime cache | `E05` |
| 恢复 / 一致性 / 回归能力 | 未完成 | 还缺专项验证 | `Q01` |
| Docker Compose 最小交付 | 未完成 | 还未形成一期演示包 | `D01` `D02` |

---

## 8. 一期任务卡总表（未来持续更新）

状态约定：`TODO / DOING / BLOCKED / DONE`

| ID | 任务 | 状态 | 说明 |
| --- | --- | --- | --- |
| `G00` | 建立开发指南、进度、AI 方案三件套 | `DONE` | 基础文档与协作协议已落地 |
| `K01` | 冻结一期核心契约（RiskEvent / SceneSnapshot / EvalContext / Result） | `PARTIAL` | 模型已基本稳定，但还未正式抽成 `kernel` 契约 |
| `K02` | 将执行语义核心下沉到 `pulsix-kernel`（epic） | `TODO` | 详见 `docs/wiki/pulsix-engine-kernel-一期任务卡-K02.md` |
| `K02a` | 下沉纯契约 + 纯执行核心 | `TODO` | 当前推荐优先执行的第一张卡 |
| `K02b` | 下沉本地运行支撑与 in-memory 实现 | `TODO` | 让 simulation/replay 底座跟 Flink 彻底分层 |
| `K02c` | 收窄 `pulsix-engine` 为 Flink/demo adapter | `TODO` | 清理残留边界与重复类 |
| `K02d` | 回归测试与文档收口 | `TODO` | 确保迁移后可解释、可验证 |
| `K03` | 补本地 simulation runner | `TODO` | 支持“快照 JSON + 事件 JSON -> 决策输出” |
| `K04` | 补轻量 replay runner 与 golden case 数据集 | `TODO` | 面向回归与版本对比 |
| `K05` | 打稳 `SCORE_CARD` 样例与测试 | `TODO` | 当前只有执行分支，没有稳定验证 |
| `E01` | 用真实 Kafka Source 替换 demo 事件源 | `TODO` | 至少保留本地 demo 与真实 source 双模式 |
| `E02` | 用真实快照流/CDC 替换 demo 快照源 | `TODO` | 面向 `scene_release` 广播流 |
| `E03` | 落 Redis LookupService（timeout/cache/fallback） | `TODO` | 替换 `InMemoryLookupService.demo()` |
| `E04` | 落结果/日志/错误输出 sink | `TODO` | Kafka 或最小可验证 sink |
| `E05` | 完善版本治理（effectiveFrom/rollback/compile-fail keep-old） | `TODO` | 避免快照切换语义不完整 |
| `Q01` | 补 checkpoint/recovery/一致性专项测试 | `TODO` | 重点验证 state 和版本切换 |
| `Q02` | 补指标与基础观测 | `TODO` | 延迟、命中率、错误率、checkpoint 基线 |
| `D01` | 形成一期 Docker Compose 演示链路 | `TODO` | 至少能拉起依赖并跑通最小闭环 |
| `D02` | 补 README / 启动脚本 / Demo 操作说明 | `TODO` | 便于人和 AI 快速复现 |

> 建议规则：
>
> - 任意一次 Codex 任务，只允许选择一个 `ID`。
> - 如果当前在做 `K02`，优先从 `docs/wiki/pulsix-engine-kernel-一期任务卡-K02.md` 进入。
> - 如果一个任务太大，再把它拆成 `K02a / K02b / K02c` 这种子任务。

---

## 9. 里程碑顺序（建议执行顺序）

### M1：先把 `kernel` 边界做实

包含：

- `K01`
- `K02`

里程碑完成条件：

- 执行语义核心不再主要耦合在 `pulsix-engine`
- `pulsix-engine` 主要负责 Flink 适配
- `kernel` 可被本地仿真直接复用

### M2：补齐本地仿真与回归能力

包含：

- `K03`
- `K04`
- `K05`

里程碑完成条件：

- 可以稳定做样例仿真
- `FIRST_HIT` 与 `SCORE_CARD` 都有回归样例
- 新版本改动可以做 golden case 比对

### M3：把 `pulsix-engine` 从 demo 链路推到真实链路

包含：

- `E01`
- `E02`
- `E03`
- `E04`

里程碑完成条件：

- 真实事件流可进
- 真实快照流可进
- 真实 lookup 可查
- 真实结果/日志/错误可出

### M4：补版本治理和一致性

包含：

- `E05`
- `Q01`

里程碑完成条件：

- 快照生效语义明确
- rollback 可解释
- checkpoint/recovery 有专项回归

### M5：补演示交付和运维最小面

包含：

- `Q02`
- `D01`
- `D02`

里程碑完成条件：

- 一期可演示
- 可快速启动
- 有基础监控与定位手段

---

## 10. 每次任务卡的标准模板

后续发给 Codex CLI 或 GPT-5.4 的任务，建议统一长这样：

```md
# 任务卡
- ID: K02
- 目标: 将执行语义核心从 pulsix-engine 下沉到 pulsix-kernel
- 当前状态: 参考 docs/wiki/pulsix-engine-kernel-一期进度.md
- 必读文档:
  - docs/wiki/pulsix-engine-kernel-一期开发指南.md
  - docs/wiki/pulsix-engine-kernel-一期进度.md
  - docs/wiki/pulsix-engine-kernel-一期-ai自动化开发方案.md
  - docs/wiki/项目架构及技术栈.md
- In Scope:
  - 列出本次允许改的模块/类
- Out of Scope:
  - 明确本次不做 Kafka/Redis/module-risk/access 等
- 验收条件:
  - 列出代码结果
  - 列出测试结果
  - 列出文档更新点
- 必跑验证:
  - 具体命令 1
  - 具体命令 2
- 完成后必须更新:
  - docs/wiki/pulsix-engine-kernel-一期进度.md
  - docs/wiki/pulsix-engine-kernel-一期-ai自动化开发方案.md
```

---

## 11. 推荐 Prompt 模板

### 11.1 规划 Prompt（给 GPT-5.4）

```text
你现在是 pulsix-engine / pulsix-kernel 一期开发的规划助手。
请先读取以下文档：
1. docs/wiki/pulsix-engine-kernel-一期开发指南.md
2. docs/wiki/pulsix-engine-kernel-一期进度.md
3. docs/wiki/pulsix-engine-kernel-一期-ai自动化开发方案.md
4. [当前任务相关文档]

当前任务卡 ID: {TASK_ID}
目标: {TASK_GOAL}

请输出：
- 任务理解
- 最小实现方案
- 涉及文件
- 风险点
- 验收条件
- 推荐给 Codex CLI 的执行顺序

要求：
- 只围绕当前任务卡，不扩展范围
- 如果发现前置阻塞，请明确指出最小 unblocker
- 输出尽量结构化、简洁
```

### 11.2 执行 Prompt（给 Codex CLI）

```text
请只执行任务卡 {TASK_ID}。

开工前必须先读取：
- docs/wiki/pulsix-engine-kernel-一期开发指南.md
- docs/wiki/pulsix-engine-kernel-一期进度.md
- docs/wiki/pulsix-engine-kernel-一期-ai自动化开发方案.md
- [当前任务相关文档]

工作要求：
- 一次只做这个任务卡
- 先输出简短计划，再实施
- 优先做最小闭环实现
- 先跑针对性验证，再跑模块验证
- 不要顺手扩展到 module-risk / access / UI
- 完成后必须更新：
  - docs/wiki/pulsix-engine-kernel-一期进度.md
  - docs/wiki/pulsix-engine-kernel-一期-ai自动化开发方案.md

最终输出必须包含：
- 改了什么
- 验证了什么
- 还剩什么风险
- 下一张最小任务卡建议是什么
```

### 11.3 审查 Prompt（给 GPT-5.4）

```text
请基于当前任务卡 {TASK_ID} 审查本次 diff 是否满足一期目标。
重点审查：
- 是否超范围
- 是否破坏 guide 中的一期边界
- 是否遗漏验收条件
- 测试是否足够支撑本次改动
- 文档是否已经同步

请输出：
- 可以合并 / 不建议合并
- 主要问题
- 必修项
- 可后置项
```

---

## 12. 防止上下文漂移的硬规则

后续任何 AI 接力开发，都必须遵守：

1. **不允许只靠聊天记录接力，必须靠仓库文档接力。**
2. **没有任务卡 ID，不开工。**
3. **没有 In Scope / Out of Scope，不开工。**
4. **没有验收条件，不开工。**
5. **没有文档回写，不算完成。**
6. **涉及执行语义变更，必须有测试。**
7. **涉及版本/state/恢复语义，必须补专项验证。**
8. **涉及外部系统接入，必须明确是假接入还是真接入。**
9. **涉及 `module-risk` / `access` 的扩展，必须来自明确任务卡。**
10. **如果发现主指南需要变更，先说明为什么，再改，不要静默重写一期目标。**

---

## 13. 并行开发建议（可选）

如果后续要开多个 Codex 线程并行做：

- 只允许在**不同任务卡、不同文件集合**下并行
- 建议用独立 worktree / 分支工作目录隔离
- 不允许两个 agent 同时改同一个包的核心类

推荐可并行的组合：

- 一个 agent 做代码，一个 agent 做测试样例
- 一个 agent 做实现，一个 agent 做文档/回归用例整理
- 一个 agent 做 `kernel`，一个 agent 做 demo/compose

不推荐并行的组合：

- 两个 agent 同时改 `DecisionExecutor`
- 两个 agent 同时改 `SceneRuntimeManager`
- 两个 agent 同时改 `DecisionBroadcastProcessFunction`

---

## 14. 一期开发完成的判断标准

只有当下面这些都满足，才算一期目标真正完成：

- `kernel` 执行语义独立成型
- `FIRST_HIT` 和 `SCORE_CARD` 都有稳定样例与回归
- `engine` 已接真实事件流与快照流
- Redis lookup 已接入
- 结果 / 日志 / 错误链路可落地
- 快照切换 / 生效 / rollback 语义明确
- checkpoint / recovery 有验证
- Docker Compose 最小演示可跑
- 文档、进度、任务卡状态与代码现实一致

---

## 15. 官方参考链接

- [OpenAI: Introducing GPT-5.4](https://openai.com/index/introducing-gpt-5-4/)
- [OpenAI: Introducing Codex](https://openai.com/index/introducing-codex/)
- [OpenAI: Introducing the Codex app](https://openai.com/index/introducing-the-codex-app/)
- [OpenAI Developers: Agents guide](https://developers.openai.com/api/docs/guides/agents)
- [OpenAI Developers: Safety best practices](https://developers.openai.com/api/docs/guides/safety-best-practices)
- [OpenAI Developers: Graders / trace grading](https://developers.openai.com/api/docs/guides/graders)
- [OpenAI Developers: Latest model guide](https://developers.openai.com/api/docs/guides/latest-model)

> 注：本方案的关键做法——`AGENTS.md`、任务切小、可靠测试、工具审批、结构化任务卡、评测闭环——都直接来自上述官方资料的合并提炼，而不是单纯凭经验拍脑袋。
