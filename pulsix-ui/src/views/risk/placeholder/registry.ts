export interface RiskPermissionSample {
  label: string
  action: string
}

export interface RiskPlaceholderMeta {
  code: string
  title: string
  summary: string
  stages: string[]
  tables: string[]
  permissionPrefix: string
  docRefs: string[]
  acceptance: string[]
  permissionSamples: RiskPermissionSample[]
}

const docs = [
  'docs/wiki/pulsix-module-risk-管理端页面开发阶段划分.md',
  'docs/wiki/风控功能清单.md',
  'docs/wiki/风控功能模块与表映射.md'
]

const queryCreateUpdateDeleteSamples: RiskPermissionSample[] = [
  { label: '查询权限样例', action: 'query' },
  { label: '新增权限样例', action: 'create' },
  { label: '修改权限样例', action: 'update' },
  { label: '删除权限样例', action: 'delete' }
]

export const riskPlaceholderRegistry: Record<string, RiskPlaceholderMeta> = {
  scene: {
    code: 'scene',
    title: '场景管理',
    summary: '维护风控场景的编码、名称、状态与默认配置，当前阶段先交付菜单入口与权限骨架。',
    stages: ['S01'],
    tables: ['scene_def'],
    permissionPrefix: 'risk:scene',
    docRefs: docs,
    acceptance: ['可新增 TRADE_RISK 场景', '可修改场景名称与状态', '刷新后列表与详情能正确展示'],
    permissionSamples: [
      { label: '查询权限样例', action: 'query' },
      { label: '新增权限样例', action: 'create' },
      { label: '修改权限样例', action: 'update' },
      { label: '启停权限样例', action: 'update-status' },
      { label: '详情权限样例', action: 'get' }
    ]
  },
  'event-schema': {
    code: 'event-schema',
    title: '事件 Schema',
    summary: '维护标准事件编码、事件类型和基础校验规则，对齐 TRADE_EVENT 等标准输入定义。',
    stages: ['S02'],
    tables: ['event_schema'],
    permissionPrefix: 'risk:event-schema',
    docRefs: docs,
    acceptance: ['可在 TRADE_RISK 下新增 TRADE_EVENT', '列表与详情可查看标准事件定义', '支持后续字段定义继续扩展'],
    permissionSamples: [
      ...queryCreateUpdateDeleteSamples,
      { label: '详情权限样例', action: 'get' }
    ]
  },
  'event-field': {
    code: 'event-field',
    title: '事件字段',
    summary: '维护标准事件字段名、类型、必填、顺序与说明，为标准事件预览和映射提供基础模型。',
    stages: ['S03'],
    tables: ['event_field_def'],
    permissionPrefix: 'risk:event-field',
    docRefs: docs,
    acceptance: ['可维护 eventId、userId、deviceId 等字段', '字段顺序与必填配置可保存', '为样例预览提供字段元信息'],
    permissionSamples: [
      ...queryCreateUpdateDeleteSamples,
      { label: '排序权限样例', action: 'sort' }
    ]
  },
  'event-sample': {
    code: 'event-sample',
    title: '事件样例',
    summary: '录入标准交易样例 JSON，并为后续标准事件预览、仿真与回放提供输入样本。',
    stages: ['S04'],
    tables: ['event_sample'],
    permissionPrefix: 'risk:event-sample',
    docRefs: docs,
    acceptance: ['可维护成功样例与异常样例', '可预览标准事件内容', '支持快速回归 TRADE_RISK 样例'],
    permissionSamples: [
      ...queryCreateUpdateDeleteSamples,
      { label: '预览权限样例', action: 'preview' }
    ]
  },
  'ingest-source': {
    code: 'ingest-source',
    title: '接入源',
    summary: '维护 HTTP / SDK 等来源编码、鉴权配置与启停状态，作为事件接入治理的主入口。',
    stages: ['S05'],
    tables: ['ingest_source'],
    permissionPrefix: 'risk:ingest-source',
    docRefs: docs,
    acceptance: ['支持新增 HTTP / SDK 来源', '支持查看鉴权配置', '支持启停来源状态'],
    permissionSamples: [
      { label: '查询权限样例', action: 'query' },
      { label: '新增权限样例', action: 'create' },
      { label: '修改权限样例', action: 'update' },
      { label: '启停权限样例', action: 'update-status' },
      { label: '详情权限样例', action: 'get' }
    ]
  },
  'ingest-mapping': {
    code: 'ingest-mapping',
    title: '字段映射',
    summary: '维护原始字段到标准字段的映射关系，并支持基于原始报文进行映射结果预览。',
    stages: ['S06'],
    tables: ['ingest_mapping_def'],
    permissionPrefix: 'risk:ingest-mapping',
    docRefs: docs,
    acceptance: ['支持 uid -> userId 等映射维护', '支持原始报文预览映射结果', '支持后续接入联调复用'],
    permissionSamples: [
      ...queryCreateUpdateDeleteSamples,
      { label: '预览权限样例', action: 'preview' }
    ]
  },
  list: {
    code: 'list',
    title: '名单中心',
    summary: '维护黑白名单集合与名单项，并为 Redis 同步与在线 lookup 场景提供基础配置。',
    stages: ['S07'],
    tables: ['list_set', 'list_item'],
    permissionPrefix: 'risk:list',
    docRefs: docs,
    acceptance: ['可新增 DEVICE_BLACKLIST', '可维护名单项与启停状态', '支持手动同步 Redis'],
    permissionSamples: [
      ...queryCreateUpdateDeleteSamples,
      { label: '同步权限样例', action: 'sync' }
    ]
  },
  'feature-stream': {
    code: 'feature-stream',
    title: '流式特征',
    summary: '维护 COUNT / SUM / MAX / LATEST / DISTINCT_COUNT 等流式特征定义与窗口配置。',
    stages: ['S08'],
    tables: ['entity_type_def', 'feature_def', 'feature_stream_conf'],
    permissionPrefix: 'risk:feature-stream',
    docRefs: docs,
    acceptance: ['可创建 user_trade_cnt_5m 等特征', '支持查看聚合口径与窗口配置', '限制在基础聚合能力范围内'],
    permissionSamples: [
      ...queryCreateUpdateDeleteSamples,
      { label: '详情权限样例', action: 'get' }
    ]
  },
  'feature-lookup': {
    code: 'feature-lookup',
    title: '查询特征',
    summary: '维护 Redis set/hash 等在线查询特征配置，包括 key 表达式、sourceRef、默认值与超时。',
    stages: ['S09'],
    tables: ['feature_def', 'feature_lookup_conf'],
    permissionPrefix: 'risk:feature-lookup',
    docRefs: docs,
    acceptance: ['可维护 device_in_blacklist 等配置', '支持查看默认值与超时', '支持为发布预检提供依赖信息'],
    permissionSamples: [
      ...queryCreateUpdateDeleteSamples,
      { label: '详情权限样例', action: 'get' }
    ]
  },
  'feature-derived': {
    code: 'feature-derived',
    title: '派生特征',
    summary: '维护依赖选择、表达式配置与结果类型，支撑 Aviator / Groovy 的校验与编译。',
    stages: ['S10'],
    tables: ['feature_def', 'feature_derived_conf'],
    permissionPrefix: 'risk:feature-derived',
    docRefs: docs,
    acceptance: ['可维护 high_amt_flag 等配置', '支持依赖声明与表达式校验', '结果类型可配置并展示'],
    permissionSamples: [
      ...queryCreateUpdateDeleteSamples,
      { label: '校验权限样例', action: 'validate' }
    ]
  },
  rule: {
    code: 'rule',
    title: '规则中心',
    summary: '维护规则表达式、优先级、动作类型、命中原因模板与启停状态。',
    stages: ['S11'],
    tables: ['rule_def'],
    permissionPrefix: 'risk:rule',
    docRefs: docs,
    acceptance: ['支持新增 R001 / R002 / R003', '支持规则语法校验', '支持维护命中原因模板'],
    permissionSamples: [
      ...queryCreateUpdateDeleteSamples,
      { label: '校验权限样例', action: 'validate' }
    ]
  },
  policy: {
    code: 'policy',
    title: '策略中心',
    summary: '维护 FIRST_HIT 策略、规则顺序与默认动作，为发布快照生成提供最终收敛配置。',
    stages: ['S12'],
    tables: ['policy_def', 'policy_rule_ref'],
    permissionPrefix: 'risk:policy',
    docRefs: docs,
    acceptance: ['支持维护 TRADE_RISK_POLICY', '支持查看规则顺序与默认动作', '为 FIRST_HIT 模式打好骨架'],
    permissionSamples: [
      ...queryCreateUpdateDeleteSamples,
      { label: '排序权限样例', action: 'sort' }
    ]
  },
  release: {
    code: 'release',
    title: '发布中心',
    summary: '承接预检、编译、发布、回滚与快照预览，统一复用 pulsix-kernel 的运行时快照能力。',
    stages: ['S13', 'S14'],
    tables: ['scene_release'],
    permissionPrefix: 'risk:release',
    docRefs: docs,
    acceptance: ['支持预检/编译生成候选版本', '支持发布与回滚版本记录', '支持查看快照 JSON 预览'],
    permissionSamples: [
      { label: '查询权限样例', action: 'query' },
      { label: '预检权限样例', action: 'compile' },
      { label: '预览权限样例', action: 'preview' },
      { label: '发布权限样例', action: 'publish' },
      { label: '回滚权限样例', action: 'rollback' }
    ]
  },
  simulation: {
    code: 'simulation',
    title: '仿真测试',
    summary: '输入标准事件、指定场景版本并查看特征快照、规则命中链路与最终动作结果。',
    stages: ['S15'],
    tables: ['simulation_case', 'simulation_report'],
    permissionPrefix: 'risk:simulation',
    docRefs: docs,
    acceptance: ['支持录入仿真用例', '支持执行仿真并查看报告', '支持展示 PASS / REVIEW / REJECT 结果'],
    permissionSamples: [
      ...queryCreateUpdateDeleteSamples,
      { label: '执行权限样例', action: 'execute' }
    ]
  },
  'decision-log': {
    code: 'decision-log',
    title: '决策日志',
    summary: '按 traceId / eventId 查询决策结果，并下钻查看命中规则、命中原因与版本信息。',
    stages: ['S16'],
    tables: ['decision_log', 'rule_hit_log'],
    permissionPrefix: 'risk:decision-log',
    docRefs: docs,
    acceptance: ['支持查询样例决策日志', '支持查看规则命中明细', '支持按版本号与动作追溯'],
    permissionSamples: [
      { label: '查询权限样例', action: 'query' },
      { label: '详情权限样例', action: 'get' },
      { label: '导出权限样例', action: 'export' },
      { label: '明细权限样例', action: 'detail' }
    ]
  },
  'ingest-error': {
    code: 'ingest-error',
    title: '接入异常',
    summary: '按来源、错误码与时间范围筛选接入异常与 DLQ 记录，优先基于 MySQL 样例数据闭环。',
    stages: ['S18'],
    tables: ['ingest_error_log'],
    permissionPrefix: 'risk:ingest-error',
    docRefs: docs,
    acceptance: ['支持来源维度筛选', '支持查看坏报文样例', '支持导出异常结果'],
    permissionSamples: [
      { label: '查询权限样例', action: 'query' },
      { label: '详情权限样例', action: 'get' },
      { label: '导出权限样例', action: 'export' }
    ]
  },
  dashboard: {
    code: 'dashboard',
    title: '监控大盘',
    summary: '展示事件量、决策量、动作占比与延迟指标，第一版优先消费 risk_metric_snapshot 样例数据。',
    stages: ['S19'],
    tables: ['risk_metric_snapshot'],
    permissionPrefix: 'risk:dashboard',
    docRefs: docs,
    acceptance: ['支持展示趋势图与统计卡片', '支持按场景筛选指标', '支持导出基础指标结果'],
    permissionSamples: [
      { label: '查询权限样例', action: 'query' },
      { label: '导出权限样例', action: 'export' },
      { label: '刷新权限样例', action: 'refresh' }
    ]
  },
  'audit-log': {
    code: 'audit-log',
    title: '审计日志',
    summary: '查看谁改了什么、谁发布了什么，后续重点补齐 risk_audit_log 的对象级 before/after 差异。',
    stages: ['S20'],
    tables: ['risk_audit_log'],
    permissionPrefix: 'risk:audit-log',
    docRefs: docs,
    acceptance: ['支持按对象类型与操作人筛选', '支持查看发布 / 回滚审计记录', '支持导出审计结果'],
    permissionSamples: [
      { label: '查询权限样例', action: 'query' },
      { label: '详情权限样例', action: 'get' },
      { label: '导出权限样例', action: 'export' }
    ]
  },
  replay: {
    code: 'replay',
    title: '回放对比',
    summary: '发起基线版本与目标版本的回放任务，查看动作差异、样例 diff 与摘要报告。',
    stages: ['S21'],
    tables: ['replay_job'],
    permissionPrefix: 'risk:replay',
    docRefs: docs,
    acceptance: ['支持创建回放任务', '支持执行并查看差异摘要', '支持导出回放结果'],
    permissionSamples: [
      { label: '查询权限样例', action: 'query' },
      { label: '新增权限样例', action: 'create' },
      { label: '执行权限样例', action: 'execute' },
      { label: '详情权限样例', action: 'get' },
      { label: '导出权限样例', action: 'export' }
    ]
  }
}

export const riskPlaceholderDefault: RiskPlaceholderMeta = {
  code: 'default',
  title: '风控占位页',
  summary: '当前菜单尚未绑定专属占位配置，请检查 system_menu.component 中的 code 参数是否与 registry 对齐。',
  stages: ['S00'],
  tables: ['system_menu'],
  permissionPrefix: 'risk:placeholder',
  docRefs: docs,
  acceptance: ['可打开菜单占位页', '可查看权限前缀约定', '可继续补齐后续阶段页面'],
  permissionSamples: [{ label: '查询权限样例', action: 'query' }]
}

