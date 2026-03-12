export const riskEventFieldTypeOptions = [
  { label: '字符串', value: 'STRING' },
  { label: '长整型', value: 'LONG' },
  { label: '数值', value: 'DECIMAL' },
  { label: '布尔值', value: 'BOOLEAN' },
  { label: '日期时间', value: 'DATETIME' },
  { label: 'JSON 对象', value: 'JSON' }
]

export const getRiskEventFieldTypeLabel = (value?: string) => {
  return riskEventFieldTypeOptions.find((item) => item.value === value)?.label || value || '-'
}

export const getFlagLabel = (value?: number, trueLabel = '是', falseLabel = '否') => {
  return value === 1 ? trueLabel : falseLabel
}
