export const riskActionOptions = [
  { label: '通过', value: 'PASS' },
  { label: '复核', value: 'REVIEW' },
  { label: '拒绝', value: 'REJECT' }
]

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

export const getHitFlagLabel = (value?: number) => {
  if (value === 1) {
    return '命中'
  }
  if (value === 0) {
    return '未命中'
  }
  return '-'
}

export const getHitFlagTag = (value?: number) => {
  if (value === 1) {
    return 'danger'
  }
  if (value === 0) {
    return 'info'
  }
  return 'info'
}

export const ensureObject = (value: unknown): Record<string, any> => {
  return value && typeof value === 'object' && !Array.isArray(value) ? (value as Record<string, any>) : {}
}

export const ensureObjectArray = (value: unknown): Record<string, any>[] => {
  return Array.isArray(value)
    ? value.filter((item) => item && typeof item === 'object' && !Array.isArray(item)) as Record<string, any>[]
    : []
}
