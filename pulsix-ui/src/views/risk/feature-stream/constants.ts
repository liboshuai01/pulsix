import type { FeatureStreamVO } from '@/api/risk/featureStream'

export const riskFeatureAggTypeOptions = [
  { label: 'COUNT / 计数', value: 'COUNT' },
  { label: 'SUM / 求和', value: 'SUM' },
  { label: 'MAX / 最大值', value: 'MAX' },
  { label: 'LATEST / 最新值', value: 'LATEST' },
  { label: 'DISTINCT_COUNT / 去重计数', value: 'DISTINCT_COUNT' }
]

export const riskFeatureValueTypeOptions = [
  { label: 'INT / 整数', value: 'INT' },
  { label: 'LONG / 长整型', value: 'LONG' },
  { label: 'DECIMAL / 小数', value: 'DECIMAL' },
  { label: 'BOOLEAN / 布尔', value: 'BOOLEAN' },
  { label: 'STRING / 字符串', value: 'STRING' }
]

export const riskFeatureWindowTypeOptions = [
  { label: 'SLIDING / 滑动窗口', value: 'SLIDING' },
  { label: 'TUMBLING / 滚动窗口', value: 'TUMBLING' },
  { label: 'NONE / 无窗口', value: 'NONE' }
]

export const riskDurationPattern = /^\d+[smhd]$/

export const getRiskFeatureAggTypeLabel = (value?: string) => {
  return riskFeatureAggTypeOptions.find((item) => item.value === value)?.label || value || '-'
}

export const getRiskFeatureValueTypeLabel = (value?: string) => {
  return riskFeatureValueTypeOptions.find((item) => item.value === value)?.label || value || '-'
}

export const getRiskFeatureWindowTypeLabel = (value?: string) => {
  return riskFeatureWindowTypeOptions.find((item) => item.value === value)?.label || value || '-'
}

export const formatRiskFeatureWindow = (row: Pick<FeatureStreamVO, 'windowType' | 'windowSize' | 'windowSlide'>) => {
  if (!row.windowType) {
    return '-'
  }
  if (row.windowType === 'NONE') {
    return '无窗口 / 最新值'
  }
  if (row.windowType === 'SLIDING') {
    return `${row.windowSize || '-'} / ${row.windowSlide || '-'} 滑动`
  }
  return `${row.windowSize || '-'} / 滚动`
}

export const formatRiskFeatureSourceEvents = (sourceEventCodes?: string[]) => {
  return sourceEventCodes?.length ? sourceEventCodes.join('、') : '-'
}
