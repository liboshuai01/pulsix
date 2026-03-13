export const auditBizTypeOptions = [
  { label: 'SCENE / 场景', value: 'SCENE' },
  { label: 'RULE / 规则', value: 'RULE' },
  { label: 'POLICY / 策略', value: 'POLICY' },
  { label: 'FEATURE / 特征', value: 'FEATURE' },
  { label: 'RELEASE / 发布', value: 'RELEASE' },
  { label: 'LIST / 名单', value: 'LIST' }
]

export const auditActionOptions = [
  { label: 'CREATE / 新增', value: 'CREATE' },
  { label: 'UPDATE / 修改', value: 'UPDATE' },
  { label: 'DELETE / 删除', value: 'DELETE' },
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
