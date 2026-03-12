export const riskIngestMappingTransformTypeOptions = [
  { label: '直接映射', value: 'DIRECT' },
  { label: '常量赋值', value: 'CONST' },
  { label: '毫秒时间戳转日期时间', value: 'TIME_MILLIS_TO_DATETIME' },
  { label: '数值除以 100', value: 'DIVIDE_100' },
  { label: '枚举映射', value: 'ENUM_MAP' }
]

export const getRiskIngestMappingTransformTypeLabel = (value?: string) => {
  return riskIngestMappingTransformTypeOptions.find((item) => item.value === value)?.label || value || '-'
}

export const getFlagLabel = (value?: number, trueLabel = '是', falseLabel = '否') => {
  return Number(value) === 1 ? trueLabel : falseLabel
}

const cleanRuleLabelMap: Record<string, string> = {
  trim: 'trim',
  blankToNull: '空串转 null',
  upperCase: '转大写',
  lowerCase: '转小写'
}

export const summarizeCleanRule = (value?: Record<string, any>) => {
  if (!value || Object.keys(value).length === 0) {
    return '-'
  }
  const labels = Object.entries(value)
    .filter(([, itemValue]) => itemValue === true || itemValue === 1 || String(itemValue).toLowerCase() === 'true')
    .map(([key]) => cleanRuleLabelMap[key] || key)
  return labels.length ? labels.join('、') : '-'
}
