import request from '@/config/axios'

export interface PolicyRuleRefVO {
  ruleCode: string
  ruleName?: string
  orderNo?: number
  hitAction?: string
  priority?: number
  riskScore?: number
  scoreWeight?: number
  stopOnHit?: number
  status?: number
}

export interface PolicyRuleOptionVO {
  ruleCode: string
  ruleName: string
  hitAction: string
  priority: number
  riskScore?: number
  status: number
}

export interface PolicyScoreBandVO {
  bandNo?: number
  minScore?: number
  maxScore?: number
  hitAction: string
  hitReasonTemplate?: string
}

export interface PolicyScorePreviewReqVO {
  decisionMode: string
  defaultAction: string
  totalScore: number
  scoreBands?: PolicyScoreBandVO[]
}

export interface PolicyScorePreviewRespVO {
  decisionMode: string
  totalScore: number
  defaultAction: string
  finalAction: string
  matched: boolean
  matchedBandNo?: number
  matchedMinScore?: number
  matchedMaxScore?: number
  reason?: string
}

export interface PolicyVO {
  id?: number
  sceneCode: string
  policyCode: string
  policyName: string
  decisionMode?: string
  defaultAction: string
  scoreCalcMode?: string
  ruleCodes: string[]
  ruleRefs?: PolicyRuleRefVO[]
  scoreBands?: PolicyScoreBandVO[]
  status: number
  version?: number
  description?: string
  creator?: string
  createTime?: Date
  updater?: string
  updateTime?: Date
}

export interface PolicyPageReqVO extends PageParam {
  sceneCode?: string
  policyCode?: string
  policyName?: string
  status?: number
}

export interface PolicySortReqVO {
  id: number
  ruleCodes: string[]
}

export const getPolicyPage = (params: PolicyPageReqVO) => {
  return request.get({ url: '/risk/policy/page', params })
}

export const getPolicy = (id: number) => {
  return request.get({ url: '/risk/policy/get?id=' + id })
}

export const createPolicy = (data: PolicyVO) => {
  return request.post({ url: '/risk/policy/create', data })
}

export const updatePolicy = (data: PolicyVO) => {
  return request.put({ url: '/risk/policy/update', data })
}

export const deletePolicy = (id: number) => {
  return request.delete({ url: '/risk/policy/delete?id=' + id })
}

export const getRuleOptions = (sceneCode: string) => {
  return request.get({ url: '/risk/policy/rule-options', params: { sceneCode } })
}

export const sortPolicyRules = (data: PolicySortReqVO) => {
  return request.post({ url: '/risk/policy/sort-rules', data })
}

export const previewScoreCard = (data: PolicyScorePreviewReqVO) => {
  return request.post({ url: '/risk/policy/preview-score-card', data })
}
