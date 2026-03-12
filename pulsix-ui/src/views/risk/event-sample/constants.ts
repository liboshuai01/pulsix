export const riskEventSampleTypeOptions = [
  { label: '原始报文', value: 'RAW' },
  { label: '标准事件', value: 'STANDARD' },
  { label: '仿真输入', value: 'SIMULATION' }
]

export const getRiskEventSampleTypeLabel = (value?: string) => {
  return riskEventSampleTypeOptions.find((item) => item.value === value)?.label || value || '-'
}
