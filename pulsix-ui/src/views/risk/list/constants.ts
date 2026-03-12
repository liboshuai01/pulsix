export const riskListMatchTypeOptions = [
  { label: '用户', value: 'USER' },
  { label: '设备', value: 'DEVICE' },
  { label: 'IP', value: 'IP' },
  { label: '手机号', value: 'MOBILE' },
  { label: '卡号', value: 'CARD' }
]

export const riskListTypeOptions = [
  { label: '黑名单', value: 'BLACK' },
  { label: '白名单', value: 'WHITE' },
  { label: '观察名单', value: 'WATCH' }
]

export const riskListStorageTypeOptions = [
  { label: 'Redis 前缀 Key', value: 'REDIS_SET' },
  { label: 'Redis Hash', value: 'REDIS_HASH' }
]

export const riskListSyncModeOptions = [
  { label: '全量同步', value: 'FULL' },
  { label: '增量同步', value: 'INCREMENTAL' }
]

export const riskListSyncStatusOptions = [
  { label: '待同步', value: 'PENDING' },
  { label: '同步成功', value: 'SUCCESS' },
  { label: '同步失败', value: 'FAILED' }
]

export const riskListItemSourceTypeOptions = [
  { label: '手工录入', value: 'MANUAL' },
  { label: '文件导入', value: 'IMPORT_FILE' },
  { label: '接口同步', value: 'API_SYNC' }
]

const optionFind = (options: { label: string; value: string }[], value?: string) => {
  return options.find((item) => item.value === value)?.label || value || '-'
}

export const getRiskListMatchTypeLabel = (value?: string) => optionFind(riskListMatchTypeOptions, value)
export const getRiskListTypeLabel = (value?: string) => optionFind(riskListTypeOptions, value)
export const getRiskListStorageTypeLabel = (value?: string) => optionFind(riskListStorageTypeOptions, value)
export const getRiskListSyncModeLabel = (value?: string) => optionFind(riskListSyncModeOptions, value)
export const getRiskListSyncStatusLabel = (value?: string) => optionFind(riskListSyncStatusOptions, value)
export const getRiskListItemSourceTypeLabel = (value?: string) => optionFind(riskListItemSourceTypeOptions, value)

export const buildRiskListRedisKeyPrefix = (listCode?: string, listType?: string, matchType?: string) => {
  const normalizedCode = (listCode || '').toUpperCase()
  const normalizedListType = (listType || '').toUpperCase()
  const normalizedMatchType = (matchType || '').toUpperCase()
  const standardCodeMap: Record<string, string> = {
    BLACK: `${normalizedMatchType}_BLACKLIST`,
    WHITE: `${normalizedMatchType}_WHITELIST`,
    WATCH: `${normalizedMatchType}_WATCHLIST`
  }
  if (standardCodeMap[normalizedListType] && standardCodeMap[normalizedListType] === normalizedCode) {
    return `pulsix:list:${(listType || 'unknown').toLowerCase()}:${(matchType || 'unknown').toLowerCase()}`
  }
  return `pulsix:list:${(listCode || 'unknown').toLowerCase().replaceAll('_', ':')}`
}
