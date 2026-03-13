export const replayJobStatusOptions = [
  { label: 'INIT / 待执行', value: 'INIT' },
  { label: 'RUNNING / 执行中', value: 'RUNNING' },
  { label: 'SUCCESS / 成功', value: 'SUCCESS' },
  { label: 'FAILED / 失败', value: 'FAILED' }
]

export const replayInputSourceTypeOptions = [
  { label: 'DECISION_LOG_EXPORT / 决策日志导出', value: 'DECISION_LOG_EXPORT' }
]

export const riskActionOptions = [
  { label: '通过', value: 'PASS' },
  { label: '复核', value: 'REVIEW' },
  { label: '拒绝', value: 'REJECT' }
]

export const replayChangeTypeOptions = [
  { label: '最终动作变化', value: 'FINAL_ACTION' },
  { label: '最终分值变化', value: 'FINAL_SCORE' },
  { label: '命中规则变化', value: 'HIT_RULES' },
  { label: '命中原因变化', value: 'HIT_REASONS' },
  { label: '特征快照变化', value: 'FEATURE_SNAPSHOT' },
  { label: '执行链路变化', value: 'TRACE' }
]

export const getReplayJobStatusLabel = (value?: string) => {
  return replayJobStatusOptions.find((item) => item.value === value)?.label || value || '-'
}

export const getReplayJobStatusTag = (value?: string) => {
  switch (value) {
    case 'INIT':
      return 'info'
    case 'RUNNING':
      return 'warning'
    case 'SUCCESS':
      return 'success'
    case 'FAILED':
      return 'danger'
    default:
      return 'info'
  }
}

export const getReplayInputSourceTypeLabel = (value?: string) => {
  return replayInputSourceTypeOptions.find((item) => item.value === value)?.label || value || '-'
}

export const getRiskActionLabel = (value?: string) => {
  return riskActionOptions.find((item) => item.value === value)?.label ?? value ?? '-'
}

export const getRiskActionTag = (value?: string) => {
  switch (value) {
    case 'PASS':
      return 'success'
    case 'REVIEW':
      return 'warning'
    case 'REJECT':
      return 'danger'
    default:
      return 'info'
  }
}

export const getReplayChangeTypeLabel = (value?: string) => {
  return replayChangeTypeOptions.find((item) => item.value === value)?.label || value || '-'
}

export const ensureObject = (value: unknown): Record<string, any> => {
  return value && typeof value === 'object' && !Array.isArray(value) ? (value as Record<string, any>) : {}
}

export const ensureObjectArray = (value: unknown): Record<string, any>[] => {
  return Array.isArray(value)
    ? value.filter((item) => item && typeof item === 'object' && !Array.isArray(item)) as Record<string, any>[]
    : []
}

export const ensureStringArray = (value: unknown): string[] => {
  return Array.isArray(value) ? value.map((item) => String(item)).filter(Boolean) : []
}

export const formatReplayRate = (changed?: number, total?: number) => {
  const totalValue = Number(total || 0)
  if (totalValue <= 0) {
    return '0.00%'
  }
  return `${((Number(changed || 0) / totalValue) * 100).toFixed(2)}%`
}
