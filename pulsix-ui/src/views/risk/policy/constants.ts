import type { PolicyRuleRefVO, PolicyVO } from '@/api/risk/policy'

export const riskPolicyDecisionModeOptions = [
  { label: 'FIRST_HIT / 首条命中即返回', value: 'FIRST_HIT' },
  { label: 'SCORE_CARD / 命中分值累计后按区间决策', value: 'SCORE_CARD' }
]

export const riskPolicyScoreCalcModeOptions = [
  { label: 'NONE / 不累计分值', value: 'NONE' },
  { label: 'SUM_HIT_SCORE / 命中分值累计', value: 'SUM_HIT_SCORE' }
]

export const riskPolicyDefaultActionOptions = [
  { label: 'PASS / 放行', value: 'PASS' },
  { label: 'REVIEW / 人工复核', value: 'REVIEW' },
  { label: 'REJECT / 拒绝', value: 'REJECT' },
  { label: 'LIMIT / 限流/限制', value: 'LIMIT' },
  { label: 'TAG_ONLY / 仅打标签', value: 'TAG_ONLY' }
]

export const getRiskPolicyDecisionModeLabel = (value?: string) => {
  return riskPolicyDecisionModeOptions.find((item) => item.value === value)?.label || value || '-'
}

export const getRiskPolicyScoreCalcModeLabel = (value?: string) => {
  return riskPolicyScoreCalcModeOptions.find((item) => item.value === value)?.label || value || '-'
}

export const getRiskPolicyDefaultActionLabel = (value?: string) => {
  return riskPolicyDefaultActionOptions.find((item) => item.value === value)?.label || value || '-'
}

export const getRiskPolicyDefaultActionTag = (
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

export const formatRiskPolicyRuleCount = (row: Pick<PolicyVO, 'ruleRefs' | 'ruleCodes'>) => {
  return row.ruleRefs?.length ?? row.ruleCodes?.length ?? 0
}

export const formatRiskPolicyRuleOrder = (ruleRefs?: PolicyRuleRefVO[]) => {
  if (!ruleRefs?.length) {
    return '-'
  }
  return ruleRefs.map((item) => item.ruleName || item.ruleCode).join(' → ')
}
