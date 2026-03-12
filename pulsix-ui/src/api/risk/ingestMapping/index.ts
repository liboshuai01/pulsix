import request from '@/config/axios'

export interface IngestMappingVO {
  id?: number
  sourceCode: string
  sceneCode: string
  eventCode: string
  sourceFieldPath?: string
  targetFieldCode: string
  targetFieldName?: string
  transformType: string
  transformExpr?: string
  defaultValue?: string
  requiredFlag: number
  cleanRuleJson?: Record<string, any>
  sortNo: number
  status: number
  creator?: string
  createTime?: Date
  updater?: string
  updateTime?: Date
}

export interface IngestMappingPageReqVO extends PageParam {
  sourceCode?: string
  sceneCode?: string
  eventCode?: string
  targetFieldCode?: string
  transformType?: string
  status?: number
}

export interface IngestMappingPreviewReqVO {
  sourceCode: string
  sceneCode: string
  eventCode: string
  rawEventJson: Record<string, any>
}

export interface IngestMappingPreviewVO {
  sourceCode: string
  sceneCode: string
  eventCode: string
  rawEventJson: Record<string, any>
  standardEventJson: Record<string, any>
  missingRequiredFields: string[]
  defaultedFields: string[]
  mappedFields: string[]
}

export const getIngestMappingPage = (params: IngestMappingPageReqVO) => {
  return request.get({ url: '/risk/ingest-mapping/page', params })
}

export const getIngestMapping = (id: number) => {
  return request.get({ url: '/risk/ingest-mapping/get?id=' + id })
}

export const createIngestMapping = (data: IngestMappingVO) => {
  return request.post({ url: '/risk/ingest-mapping/create', data })
}

export const updateIngestMapping = (data: IngestMappingVO) => {
  return request.put({ url: '/risk/ingest-mapping/update', data })
}

export const deleteIngestMapping = (id: number) => {
  return request.delete({ url: '/risk/ingest-mapping/delete?id=' + id })
}

export const previewIngestMapping = (data: IngestMappingPreviewReqVO) => {
  return request.post({ url: '/risk/ingest-mapping/preview', data })
}
