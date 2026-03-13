export const dashboardGranularityOptions = [
  { label: '1 分钟', value: '1m' },
  { label: '5 分钟', value: '5m' },
  { label: '1 小时', value: '1h' }
]

export const formatDashboardCount = (value?: number) => {
  return Number(value || 0).toFixed(0)
}

export const formatDashboardPercent = (value?: number) => {
  return `${(Number(value || 0) * 100).toFixed(2)}%`
}

export const formatDashboardLatency = (value?: number) => {
  return `${Number(value || 0).toFixed(2)} ms`
}
