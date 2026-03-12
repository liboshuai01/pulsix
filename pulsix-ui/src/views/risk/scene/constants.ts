export const riskSceneTypeOptions = [
  { label: '通用风控', value: 'GENERAL' },
  { label: '账号安全', value: 'ACCOUNT_SECURITY' },
  { label: '交易风控', value: 'TRADE_SECURITY' }
]

export const riskSceneAccessModeOptions = [
  { label: 'HTTP', value: 'HTTP' },
  { label: 'Beacon', value: 'BEACON' },
  { label: 'SDK', value: 'SDK' },
  { label: '混合接入', value: 'MIXED' }
]

export const getRiskSceneTypeLabel = (value?: string) => {
  return riskSceneTypeOptions.find((item) => item.value === value)?.label || value || '-'
}

export const getRiskSceneAccessModeLabel = (value?: string) => {
  return riskSceneAccessModeOptions.find((item) => item.value === value)?.label || value || '-'
}

