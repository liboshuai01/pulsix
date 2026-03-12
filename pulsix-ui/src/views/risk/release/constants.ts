import type { SceneReleaseVO } from '@/api/risk/release'

export const publishStatusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '生效中', value: 'ACTIVE' },
  { label: '已回滚', value: 'ROLLED_BACK' },
  { label: '失败', value: 'FAILED' }
]

export const validationStatusOptions = [
  { label: '待校验', value: 'PENDING' },
  { label: '已通过', value: 'PASSED' },
  { label: '已失败', value: 'FAILED' }
]

export const getReleasePublishStatusLabel = (value?: string) => {
  return publishStatusOptions.find((item) => item.value === value)?.label ?? value ?? '-'
}

export const getReleaseValidationStatusLabel = (value?: string) => {
  return validationStatusOptions.find((item) => item.value === value)?.label ?? value ?? '-'
}

export const getReleasePublishStatusTag = (value?: string) => {
  switch (value) {
    case 'ACTIVE':
    case 'PUBLISHED':
      return 'success'
    case 'ROLLED_BACK':
      return 'warning'
    case 'FAILED':
      return 'danger'
    case 'DRAFT':
    default:
      return 'info'
  }
}

export const getReleaseValidationStatusTag = (value?: string) => {
  switch (value) {
    case 'PASSED':
      return 'success'
    case 'FAILED':
      return 'danger'
    case 'PENDING':
    default:
      return 'info'
  }
}

export const formatReleaseCompileSummary = (row: SceneReleaseVO) => {
  return `${row.compiledFeatureCount ?? 0} 特征 / ${row.compiledRuleCount ?? 0} 规则 / ${row.compiledPolicyCount ?? 0} 策略`
}

export const isReleasePublishable = (row: SceneReleaseVO) => {
  return row.validationStatus === 'PASSED' && row.publishStatus === 'DRAFT'
}

export const isReleaseRollbackAvailable = (row: SceneReleaseVO) => {
  return row.validationStatus === 'PASSED' && ['PUBLISHED', 'ROLLED_BACK'].includes(row.publishStatus)
}
