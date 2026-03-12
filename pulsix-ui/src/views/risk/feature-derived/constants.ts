import type { FeatureDerivedDependencyOptionVO, FeatureDerivedVO } from '@/api/risk/featureDerived'

export const riskFeatureDerivedEngineTypeOptions = [
  { label: 'AVIATOR / Aviator', value: 'AVIATOR' },
  { label: 'GROOVY / Groovy', value: 'GROOVY' }
]

export const riskFeatureDerivedValueTypeOptions = [
  { label: 'BOOLEAN / 布尔', value: 'BOOLEAN' },
  { label: 'INT / 整数', value: 'INT' },
  { label: 'LONG / 长整型', value: 'LONG' },
  { label: 'DECIMAL / 小数', value: 'DECIMAL' },
  { label: 'STRING / 字符串', value: 'STRING' }
]

export const riskFeatureDerivedDependencyTypeOptions = [
  { label: 'FIELD / 事件字段', value: 'FIELD' },
  { label: 'STREAM / 流式特征', value: 'STREAM' },
  { label: 'LOOKUP / 查询特征', value: 'LOOKUP' },
  { label: 'DERIVED / 派生特征', value: 'DERIVED' }
]

export const getRiskFeatureDerivedEngineTypeLabel = (value?: string) => {
  return riskFeatureDerivedEngineTypeOptions.find((item) => item.value === value)?.label || value || '-'
}

export const getRiskFeatureDerivedValueTypeLabel = (value?: string) => {
  return riskFeatureDerivedValueTypeOptions.find((item) => item.value === value)?.label || value || '-'
}

export const getRiskFeatureDerivedDependencyTypeLabel = (value?: string) => {
  return riskFeatureDerivedDependencyTypeOptions.find((item) => item.value === value)?.label || value || '-'
}

export const formatRiskFeatureDerivedDependsOn = (dependsOn?: string[]) => {
  return dependsOn?.length ? dependsOn.join('、') : '-'
}

export const formatRiskFeatureDerivedSandbox = (sandboxFlag?: number) => {
  return sandboxFlag === 1 ? '开启' : '关闭'
}

export const formatRiskFeatureDerivedTimeout = (row: Pick<FeatureDerivedVO, 'timeoutMs' | 'sandboxFlag'>) => {
  const timeoutText = row.timeoutMs != null ? `${row.timeoutMs} ms` : '-'
  return `${timeoutText} / 沙箱${formatRiskFeatureDerivedSandbox(row.sandboxFlag)}`
}

export const buildRiskFeatureDerivedDependencyLabel = (option: FeatureDerivedDependencyOptionVO) => {
  const typeLabel = getRiskFeatureDerivedDependencyTypeLabel(option.dependencyType)
  const nameText = option.name ? ` - ${option.name}` : ''
  const valueTypeText = option.valueType ? ` / ${option.valueType}` : ''
  return `[${typeLabel}] ${option.code}${nameText}${valueTypeText}`
}
