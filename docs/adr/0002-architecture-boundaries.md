# ADR 0002：架构边界

## Title

Architecture Boundaries for Control Plane, Compute Plane, and Analytics Plane

## Status

Accepted

## Context

- 本平台是实时风控平台，主链路固定为 `事件 -> 特征 -> 规则 -> 策略 -> 决策 -> 日志 -> 发布 / 回滚`。
- 当前仓库同时包含控制面、计算面、共享执行内核和控制台代码；如果边界不硬，后续实现会退化为“后台表 + 临时脚本 + 运行时拼装”。
- 风控平台必须优先保证可解释、可回放、可发布、可回滚，而不是优先追求页面堆叠或接口堆叠。

## Decision

- 控制面、计算面、分析面必须分离；三者通过契约协作，不能通过跨模块直接借用内部实现协作。
- `pulsix-module-risk`、`pulsix-server`、`pulsix-ui` 属于控制面，负责设计态对象管理、校验、编译、发布、回滚、仿真入口。
- `pulsix-engine` 属于计算面，负责消费事件流和快照版本、维护状态、执行特征与策略、输出决策结果与运行日志。
- 分析面负责日志查询、命中明细、版本效果对比、回放与审计；分析逻辑不得侵入实时决策热路径。
- `pulsix-framework/pulsix-kernel` 是共享执行内核，只承载纯输入 / 纯输出的稳定执行契约与规则 / 策略求值逻辑，不承载 Spring、MyBatis、Flink Runtime 或远程 SDK 依赖。
- `pulsix-module-system`、`pulsix-module-infra` 只提供基础支撑，不反向定义风控领域模型，不替代 `pulsix-module-risk` 的领域边界。
- 控制面维护设计态对象；计算面只能执行发布后的运行态快照。设计态对象与运行态对象必须分离，不能混读、混写、混执行。
- 发布必须是“编译快照 -> 生成版本 -> 发布 -> 引擎切换 -> 可回滚”，不能退化为“改表即生效”或“改状态位即生效”。
- 明确禁止：`pulsix-engine` 直接依赖控制面 `Mapper`、`Entity`、`Service`；Flink 运行时直接读取设计态多表拼装规则；在共享执行内核、规则层、策略层做外部 IO；把分析报表逻辑回灌到实时判定链路。

## Consequences

- 后续所有实现必须先判断自己属于控制面、计算面、分析面还是共享执行内核，再决定落模块位置。
- 任何跨平面能力都必须通过 ADR、contract、snapshot 或明确 API 暴露，不允许通过“先 import 进来再说”绕开边界。
- 共享执行逻辑要优先沉到 `pulsix-framework/pulsix-kernel`；控制面与计算面只负责各自平面的编排与适配。
- 若后续需要新增平面职责、跨模块直连或改变发布链路，必须先更新 ADR，再改代码。
