import type { RuleVO } from '@/api/risk/rule'

export const riskRuleTypeOptions = [
  { label: 'NORMAL / 普通规则', value: 'NORMAL' },
  { label: 'TAG_ONLY / 仅打标签', value: 'TAG_ONLY' },
  { label: 'MANUAL_REVIEW_HINT / 人工复核提示', value: 'MANUAL_REVIEW_HINT' }
]

export const riskRuleEngineTypeOptions = [
  { label: 'DSL / DSL', value: 'DSL' },
  { label: 'AVIATOR / Aviator', value: 'AVIATOR' },
  { label: 'GROOVY / Groovy', value: 'GROOVY' }
]

export const riskRuleHitActionOptions = [
  { label: 'PASS / 放行', value: 'PASS' },
  { label: 'REVIEW / 人工复核', value: 'REVIEW' },
  { label: 'REJECT / 拒绝', value: 'REJECT' },
  { label: 'LIMIT / 限流/限制', value: 'LIMIT' },
  { label: 'TAG_ONLY / 仅打标签', value: 'TAG_ONLY' }
]

export const getRiskRuleTypeLabel = (value?: string) => {
  return riskRuleTypeOptions.find((item) => item.value === value)?.label || value || '-'
}

export const getRiskRuleEngineTypeLabel = (value?: string) => {
  return riskRuleEngineTypeOptions.find((item) => item.value === value)?.label || value || '-'
}

export const getRiskRuleHitActionLabel = (value?: string) => {
  return riskRuleHitActionOptions.find((item) => item.value === value)?.label || value || '-'
}

export const getRiskRuleHitActionTag = (
  value?: string
): 'success' | 'warning' | 'danger' | 'info' | 'primary' => {
  switch (value) {
    case 'PASS':
      return 'success'
    case 'REVIEW':
      return 'warning'
    case 'REJECT':
      return 'danger'
    case 'LIMIT':
      return 'primary'
    default:
      return 'info'
  }
}

export const formatRiskRuleScore = (row: Pick<RuleVO, 'riskScore' | 'priority'>) => {
  return `优先级 ${row.priority ?? '-'} / 分值 ${row.riskScore ?? 0}`
}
