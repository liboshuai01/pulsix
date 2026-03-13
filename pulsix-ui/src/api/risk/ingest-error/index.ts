import request from '@/config/axios'

export interface IngestErrorVO {
  id?: number
  traceId: string
  sourceCode: string
  sceneCode?: string
  eventCode?: string
  rawEventId?: string
  ingestStage: string
  errorCode: string
  errorMessage: string
  errorTopicName?: string
  reprocessStatus: string
  status: number
  occurTime?: Date
}

export interface IngestErrorDetailVO extends IngestErrorVO {
  rawPayloadJson?: Record<string, any>
  standardPayloadJson?: Record<string, any>
}

export interface IngestErrorPageReqVO extends PageParam {
  sceneCode?: string
  sourceCode?: string
  traceId?: string
  rawEventId?: string
  ingestStage?: string
  errorCode?: string
  reprocessStatus?: string
  occurTime?: string[]
}

export const getIngestErrorPage = (params: IngestErrorPageReqVO) => {
  return request.get({ url: '/risk/ingest-error/page', params })
}

export const getIngestError = (id: number) => {
  return request.get({ url: '/risk/ingest-error/get?id=' + id })
}
