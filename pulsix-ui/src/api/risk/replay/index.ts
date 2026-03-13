import request from '@/config/axios'

export interface ReplayJobVO {
  id?: number
  jobCode: string
  sceneCode: string
  baselineVersionNo: number
  targetVersionNo: number
  inputSourceType: string
  inputRef?: string
  jobStatus: string
  eventTotalCount?: number
  diffEventCount?: number
  remark?: string
  startedAt?: Date | string
  finishedAt?: Date | string
  creator?: string
  createTime?: Date | string
  updater?: string
  updateTime?: Date | string
}

export interface ReplayJobDetailVO extends ReplayJobVO {
  summaryJson?: Record<string, any>
  sampleDiffJson?: Record<string, any>[]
}

export interface ReplayJobPageReqVO extends PageParam {
  sceneCode?: string
  jobCode?: string
  jobStatus?: string
}

export interface ReplayJobCreateReqVO {
  sceneCode: string
  baselineVersionNo: number
  targetVersionNo: number
  inputSourceType: string
  inputRef?: string
  remark?: string
}

export interface ReplayJobExecuteReqVO {
  id: number
}

export const getReplayJobPage = (params: ReplayJobPageReqVO) => {
  return request.get({ url: '/risk/replay/page', params })
}

export const exportReplayJob = (params: ReplayJobPageReqVO) => {
  return request.download({ url: '/risk/replay/export-excel', params })
}

export const getReplayJob = (id: number) => {
  return request.get({ url: '/risk/replay/get?id=' + id })
}

export const createReplayJob = (data: ReplayJobCreateReqVO) => {
  return request.post({ url: '/risk/replay/create', data })
}

export const executeReplayJob = (data: ReplayJobExecuteReqVO) => {
  return request.post({ url: '/risk/replay/execute', data })
}
