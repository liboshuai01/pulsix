import request from '@/config/axios'

export interface DecisionLogVO {
  id?: number
  traceId: string
  eventId: string
  sceneCode: string
  sourceCode?: string
  eventCode?: string
  entityId?: string
  policyCode?: string
  finalAction: string
  finalScore?: number
  versionNo: number
  latencyMs?: number
  eventTime?: Date | string
  createTime?: Date | string
  hitRuleCodes?: string[]
}

export interface DecisionLogPageReqVO extends PageParam {
  sceneCode?: string
  traceId?: string
  eventId?: string
  finalAction?: string
  versionNo?: number
}

export interface DecisionLogDetailVO extends DecisionLogVO {
  inputJson?: Record<string, any>
  featureSnapshotJson?: Record<string, any>
  hitRulesJson?: Record<string, any>[]
  decisionDetailJson?: Record<string, any>
}

export interface RuleHitLogVO {
  id?: number
  decisionId: number
  ruleCode: string
  ruleName?: string
  ruleOrderNo?: number
  hitFlag: number
  hitReason?: string
  score?: number
  hitValueJson?: Record<string, any>
  createTime?: Date | string
}

export const getDecisionLogPage = (params: DecisionLogPageReqVO) => {
  return request.get({ url: '/risk/decision-log/page', params })
}

export const getDecisionLog = (id: number) => {
  return request.get({ url: '/risk/decision-log/get?id=' + id })
}

export const getRuleHitLogList = (decisionId: number) => {
  return request.get({ url: '/risk/decision-log/hit-log/list?decisionId=' + decisionId })
}
