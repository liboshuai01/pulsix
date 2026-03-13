import request from '@/config/axios'

export interface DashboardTrendPointVO {
  statTime?: Date
  eventInTotal?: number
  decisionTotal?: number
  passRate?: number
  reviewRate?: number
  rejectRate?: number
  p95LatencyMs?: number
}

export interface DashboardSummaryVO {
  sceneCode?: string
  statGranularity?: string
  latestStatTime?: Date
  latestEventInTotal?: number
  latestDecisionTotal?: number
  latestPassRate?: number
  latestReviewRate?: number
  latestRejectRate?: number
  latestP95LatencyMs?: number
  trends?: DashboardTrendPointVO[]
}

export interface DashboardQueryReqVO {
  sceneCode?: string
  statGranularity?: string
  statTime?: string[]
}

export const getDashboardSummary = (params: DashboardQueryReqVO) => {
  return request.get({ url: '/risk/dashboard/summary', params })
}
