import request from '@/config/axios'

export interface SimulationCaseVO {
  id?: number
  sceneCode: string
  caseCode: string
  caseName: string
  versionSelectMode: string
  versionNo?: number
  inputEventJson: Record<string, any>
  mockFeatureJson?: Record<string, any>
  mockLookupJson?: Record<string, any>
  expectedAction?: string
  expectedHitRules: string[]
  status: number
  description?: string
  creator?: string
  createTime?: Date
  updater?: string
  updateTime?: Date
  latestReportId?: number
  latestReportVersionNo?: number
  latestFinalAction?: string
  latestHitRules?: string[]
  latestPassFlag?: number
  latestDurationMs?: number
  latestReportTime?: Date
}

export interface SimulationCasePageReqVO extends PageParam {
  sceneCode?: string
  caseCode?: string
  caseName?: string
  versionSelectMode?: string
  status?: number
}

export interface SimulationExecuteReqVO {
  caseId: number
}

export interface SimulationReportVO {
  id?: number
  caseId: number
  caseCode?: string
  caseName?: string
  sceneCode: string
  versionNo: number
  traceId?: string
  resultJson?: Record<string, any>
  passFlag?: number
  durationMs?: number
  creator?: string
  createTime?: Date
  updater?: string
  updateTime?: Date
}

export const getSimulationCasePage = (params: SimulationCasePageReqVO) => {
  return request.get({ url: '/risk/simulation/page', params })
}

export const getSimulationCase = (id: number) => {
  return request.get({ url: '/risk/simulation/get?id=' + id })
}

export const createSimulationCase = (data: SimulationCaseVO) => {
  return request.post({ url: '/risk/simulation/create', data })
}

export const updateSimulationCase = (data: SimulationCaseVO) => {
  return request.put({ url: '/risk/simulation/update', data })
}

export const deleteSimulationCase = (id: number) => {
  return request.delete({ url: '/risk/simulation/delete?id=' + id })
}

export const executeSimulation = (data: SimulationExecuteReqVO) => {
  return request.post({ url: '/risk/simulation/execute', data })
}

export const getSimulationReport = (id: number) => {
  return request.get({ url: '/risk/simulation/report/get?id=' + id })
}
