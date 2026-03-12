export const riskIngestSourceTypeOptions = [
  { label: 'HTTP', value: 'HTTP' },
  { label: 'Beacon', value: 'BEACON' },
  { label: 'SDK', value: 'SDK' }
]

export const riskIngestSourceAuthTypeOptions = [
  { label: '无需鉴权', value: 'NONE' },
  { label: 'Token', value: 'TOKEN' },
  { label: 'HMAC', value: 'HMAC' },
  { label: 'AK/SK', value: 'AKSK' },
  { label: 'JWT', value: 'JWT' }
]

export const getRiskIngestSourceTypeLabel = (value?: string) => {
  return riskIngestSourceTypeOptions.find((item) => item.value === value)?.label || value || '-'
}

export const getRiskIngestSourceAuthTypeLabel = (value?: string) => {
  return riskIngestSourceAuthTypeOptions.find((item) => item.value === value)?.label || value || '-'
}
