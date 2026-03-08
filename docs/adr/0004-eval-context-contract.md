# ADR 0004：统一求值上下文契约

## Title

Unified EvalContext Variable Model for Rule and Feature Evaluation

## Status

Accepted

## Context

- 规则表达式、派生特征、仿真和实时执行必须在同一套变量命名下运行，否则会出现“控制面可配、引擎不可跑、仿真不一致”的问题。
- 实时风控同时依赖原始事件字段、流式状态特征、在线查询特征、派生结果和运行元信息，变量来源多且容易混乱。
- 共享执行内核需要一套稳定、纯净、无框架依赖的统一输入模型。

## Decision

- 所有规则、策略和派生特征求值都必须基于统一的 `EvalContext`；不允许直接读取 DTO、Entity、Mapper、Flink State、Spring Bean。
- `EvalContext` 采用固定五段命名空间：
  - `base`：标准化后的事件基础字段与实体字段
  - `stream`：流式状态计算出的特征值
  - `lookup`：在线查询得到的特征值
  - `derived`：派生特征计算结果
  - `meta`：运行元信息
- `base` 只承载原始事件经标准化后的业务字段，不承载运行时状态，不承载后台表对象。
- `stream` 只承载当前事件绑定版本下已计算完成的流式特征结果；表达式和 Groovy 只能读取，不能自行管理状态。
- `lookup` 只承载经受控查询拿到的外部事实结果；求值阶段只能读取已放入上下文的值，不能在表达式内部再次发起查询。
- `derived` 只承载按依赖拓扑顺序求出的派生值；派生特征不得回写 `base`、`stream`、`lookup`。
- `meta` 至少包含：`eventId`、`traceId`、`sceneCode`、`eventType`、`eventTime`、`snapshotId`、`snapshotVersion`。
- 规则表达式、策略求值、命中原因模板、Groovy 补位脚本必须使用同一命名空间，不允许出现一处写 `amount`、另一处写 `event.amount`、第三处写 `ctx.get("amt")` 的多套模型。
- 明确禁止：在 `EvalContext` 中塞入 Spring 容器对象、Flink 状态对象、数据库连接、Redis 客户端、HTTP Client；禁止把调试临时变量当成稳定契约；禁止让脚本通过上下文反向修改引擎状态。

## Consequences

- 控制面编译器、共享执行内核、Flink 引擎、仿真入口都必须围绕同一份 `EvalContext` 命名和结构实现。
- 变量存在性校验、类型校验、依赖分析可以在发布前完成，减少运行时歧义。
- 后续若新增变量来源，必须先判断属于五段中的哪一段；若超出现有模型，需先更新 ADR 与契约。
- Groovy 补位、表达式执行、命中模板渲染因此具有统一输入，仿真与线上更容易保持一致。
