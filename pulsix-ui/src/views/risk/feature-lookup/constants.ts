import type { FeatureLookupVO } from '@/api/risk/featureLookup'

export const riskFeatureLookupTypeOptions = [
  { label: 'REDIS_SET / Redis Set', value: 'REDIS_SET' },
  { label: 'REDIS_HASH / Redis Hash', value: 'REDIS_HASH' }
]

export const riskFeatureLookupValueTypeOptions = [
  { label: 'BOOLEAN / 布尔', value: 'BOOLEAN' },
  { label: 'STRING / 字符串', value: 'STRING' },
  { label: 'INT / 整数', value: 'INT' },
  { label: 'LONG / 长整型', value: 'LONG' },
  { label: 'DECIMAL / 小数', value: 'DECIMAL' }
]

export const getRiskFeatureLookupTypeLabel = (value?: string) => {
  return riskFeatureLookupTypeOptions.find((item) => item.value === value)?.label || value || '-'
}

export const getRiskFeatureLookupValueTypeLabel = (value?: string) => {
  return riskFeatureLookupValueTypeOptions.find((item) => item.value === value)?.label || value || '-'
}

export const formatRiskFeatureLookupTimeout = (row: Pick<FeatureLookupVO, 'timeoutMs' | 'cacheTtlSeconds'>) => {
  const timeoutText = row.timeoutMs != null ? `${row.timeoutMs} ms` : '-'
  const cacheText = row.cacheTtlSeconds != null ? `${row.cacheTtlSeconds} s` : '-'
  return `${timeoutText} / ${cacheText}`
}
