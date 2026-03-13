export const auditBizTypeOptions = [
  { label: 'SCENE / 场景', value: 'SCENE' },
  { label: 'EVENT_SCHEMA / 事件定义', value: 'EVENT_SCHEMA' },
  { label: 'EVENT_FIELD / 事件字段', value: 'EVENT_FIELD' },
  { label: 'EVENT_SAMPLE / 事件样例', value: 'EVENT_SAMPLE' },
  { label: 'RULE / 规则', value: 'RULE' },
  { label: 'POLICY / 策略', value: 'POLICY' },
  { label: 'FEATURE / 特征', value: 'FEATURE' },
  { label: 'RELEASE / 发布', value: 'RELEASE' },
  { label: 'LIST / 名单', value: 'LIST' },
  { label: 'INGEST_SOURCE / 接入来源', value: 'INGEST_SOURCE' },
  { label: 'INGEST_MAPPING / 接入映射', value: 'INGEST_MAPPING' },
  { label: 'SIMULATION / 仿真', value: 'SIMULATION' },
  { label: 'REPLAY / 回放', value: 'REPLAY' }
]

export const auditActionOptions = [
  { label: 'CREATE / 新增', value: 'CREATE' },
  { label: 'UPDATE / 修改', value: 'UPDATE' },
  { label: 'DELETE / 删除', value: 'DELETE' },
  { label: 'UPDATE_STATUS / 状态变更', value: 'UPDATE_STATUS' },
  { label: 'SORT / 排序调整', value: 'SORT' },
  { label: 'SYNC / 同步', value: 'SYNC' },
  { label: 'COMPILE / 编译', value: 'COMPILE' },
  { label: 'EXECUTE / 执行', value: 'EXECUTE' },
  { label: 'PUBLISH / 发布', value: 'PUBLISH' },
  { label: 'ROLLBACK / 回滚', value: 'ROLLBACK' },
  { label: 'IMPORT / 导入', value: 'IMPORT' }
]

export const getAuditBizTypeLabel = (value?: string) => {
  return auditBizTypeOptions.find((item) => item.value === value)?.label || value || '-'
}

export const getAuditBizTypeTag = (value?: string) => {
  switch (value) {
    case 'SCENE':
      return 'primary'
    case 'EVENT_SCHEMA':
      return 'success'
    case 'EVENT_FIELD':
      return 'warning'
    case 'EVENT_SAMPLE':
      return 'info'
    case 'RULE':
      return 'warning'
    case 'POLICY':
      return 'success'
    case 'FEATURE':
      return 'info'
    case 'RELEASE':
      return 'success'
    case 'LIST':
      return 'danger'
    case 'INGEST_SOURCE':
      return 'primary'
    case 'INGEST_MAPPING':
      return 'warning'
    case 'SIMULATION':
      return 'success'
    case 'REPLAY':
      return 'danger'
    default:
      return 'info'
  }
}

export const getAuditActionLabel = (value?: string) => {
  return auditActionOptions.find((item) => item.value === value)?.label || value || '-'
}

export const getAuditActionTag = (value?: string) => {
  switch (value) {
    case 'CREATE':
      return 'success'
    case 'UPDATE':
      return 'primary'
    case 'DELETE':
      return 'danger'
    case 'UPDATE_STATUS':
      return 'warning'
    case 'SORT':
      return 'info'
    case 'SYNC':
      return 'success'
    case 'COMPILE':
      return 'info'
    case 'EXECUTE':
      return 'primary'
    case 'PUBLISH':
      return 'success'
    case 'ROLLBACK':
      return 'warning'
    case 'IMPORT':
      return 'info'
    default:
      return 'info'
  }
}

export const ensureObject = (value: unknown): Record<string, any> => {
  return value && typeof value === 'object' && !Array.isArray(value) ? (value as Record<string, any>) : {}
}
