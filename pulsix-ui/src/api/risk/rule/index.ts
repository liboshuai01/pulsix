import request from '@/config/axios'

export interface RuleVO {
  id?: number
  sceneCode: string
  ruleCode: string
  ruleName: string
  ruleType: string
  engineType: string
  exprContent: string
  priority: number
  hitAction: string
  riskScore: number
  hitReasonTemplate?: string
  status: number
  version?: number
  description?: string
  creator?: string
  createTime?: Date
  updater?: string
  updateTime?: Date
}

export interface RulePageReqVO extends PageParam {
  sceneCode?: string
  ruleCode?: string
  ruleName?: string
  hitAction?: string
  status?: number
}

export interface RuleValidateReqVO {
  sceneCode: string
  engineType: string
  exprContent: string
  hitReasonTemplate?: string
}

export interface RuleValidateRespVO {
  valid: boolean
  message: string
  invalidPlaceholders?: string[]
}

export const getRulePage = (params: RulePageReqVO) => {
  return request.get({ url: '/risk/rule/page', params })
}

export const getRule = (id: number) => {
  return request.get({ url: '/risk/rule/get?id=' + id })
}

export const createRule = (data: RuleVO) => {
  return request.post({ url: '/risk/rule/create', data })
}

export const updateRule = (data: RuleVO) => {
  return request.put({ url: '/risk/rule/update', data })
}

export const deleteRule = (id: number) => {
  return request.delete({ url: '/risk/rule/delete?id=' + id })
}

export const validateRule = (data: RuleValidateReqVO) => {
  return request.post({ url: '/risk/rule/validate', data })
}
