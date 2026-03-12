export const riskEventSchemaTypeOptions = [
  { label: '业务事件', value: 'BUSINESS' },
  { label: '回调事件', value: 'CALLBACK' },
  { label: '测试事件', value: 'TEST' }
]

export const riskEventSchemaSourceTypeOptions = [
  { label: 'HTTP', value: 'HTTP' },
  { label: 'Beacon', value: 'BEACON' },
  { label: 'SDK', value: 'SDK' },
  { label: '混合接入', value: 'MIXED' }
]

export const getRiskEventSchemaTypeLabel = (value?: string) => {
  return riskEventSchemaTypeOptions.find((item) => item.value === value)?.label || value || '-'
}

export const getRiskEventSchemaSourceTypeLabel = (value?: string) => {
  return riskEventSchemaSourceTypeOptions.find((item) => item.value === value)?.label || value || '-'
}
