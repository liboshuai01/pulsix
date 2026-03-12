import request from '@/config/axios'

export interface IngestSourceVO {
  id?: number
  sourceCode: string
  sourceName: string
  sourceType: string
  authType: string
  authConfigJson?: Record<string, any>
  sceneScopeJson?: string[]
  standardTopicName: string
  errorTopicName: string
  rateLimitQps: number
  status: number
  description?: string
  creator?: string
  createTime?: Date
  updater?: string
  updateTime?: Date
}

export interface IngestSourcePageReqVO extends PageParam {
  sourceCode?: string
  sourceName?: string
  sourceType?: string
  authType?: string
  status?: number
}

export const getIngestSourcePage = (params: IngestSourcePageReqVO) => {
  return request.get({ url: '/risk/ingest-source/page', params })
}

export const getIngestSource = (id: number) => {
  return request.get({ url: '/risk/ingest-source/get?id=' + id })
}

export const createIngestSource = (data: IngestSourceVO) => {
  return request.post({ url: '/risk/ingest-source/create', data })
}

export const updateIngestSource = (data: IngestSourceVO) => {
  return request.put({ url: '/risk/ingest-source/update', data })
}

export const updateIngestSourceStatus = (id: number, status: number) => {
  return request.put({ url: '/risk/ingest-source/update-status', data: { id, status } })
}
