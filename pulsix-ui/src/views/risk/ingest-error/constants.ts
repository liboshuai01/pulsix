export const ingestStageOptions = [
  { label: 'AUTH / 鉴权阶段', value: 'AUTH' },
  { label: 'PARSE / 报文解析', value: 'PARSE' },
  { label: 'NORMALIZE / 字段标准化', value: 'NORMALIZE' },
  { label: 'VALIDATE / 校验补齐', value: 'VALIDATE' },
  { label: 'PRODUCE / 投递输出', value: 'PRODUCE' }
]

export const reprocessStatusOptions = [
  { label: 'PENDING / 待处理', value: 'PENDING' },
  { label: 'IGNORED / 已忽略', value: 'IGNORED' },
  { label: 'RETRY_SUCCESS / 重试成功', value: 'RETRY_SUCCESS' },
  { label: 'RETRY_FAILED / 重试失败', value: 'RETRY_FAILED' }
]

export const getIngestStageLabel = (value?: string) => {
  return ingestStageOptions.find((item) => item.value === value)?.label || value || '-'
}

export const getIngestStageTag = (value?: string) => {
  switch (value) {
    case 'AUTH':
      return 'danger'
    case 'PARSE':
      return 'warning'
    case 'NORMALIZE':
      return 'primary'
    case 'VALIDATE':
      return 'warning'
    case 'PRODUCE':
      return 'info'
    default:
      return 'info'
  }
}

export const getReprocessStatusLabel = (value?: string) => {
  return reprocessStatusOptions.find((item) => item.value === value)?.label || value || '-'
}

export const getReprocessStatusTag = (value?: string) => {
  switch (value) {
    case 'PENDING':
      return 'warning'
    case 'IGNORED':
      return 'info'
    case 'RETRY_SUCCESS':
      return 'success'
    case 'RETRY_FAILED':
      return 'danger'
    default:
      return 'info'
  }
}

export const getIngestRecordStatusLabel = (value?: number) => {
  if (value === 1) {
    return '有效'
  }
  if (value === 0) {
    return '已归档/忽略'
  }
  return '-'
}

export const getIngestRecordStatusTag = (value?: number) => {
  if (value === 1) {
    return 'success'
  }
  if (value === 0) {
    return 'info'
  }
  return 'info'
}

export const ensureObject = (value: unknown): Record<string, any> => {
  return value && typeof value === 'object' && !Array.isArray(value) ? (value as Record<string, any>) : {}
}
