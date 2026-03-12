import request from '@/config/axios'

export interface EntityTypeVO {
  id?: number
  entityType: string
  entityName: string
  keyFieldName: string
  status: number
  description?: string
}

export interface FeatureStreamVO {
  id?: number
  sceneCode: string
  featureCode: string
  featureName: string
  featureType?: string
  entityType: string
  entityName?: string
  entityKeyFieldName?: string
  eventCode?: string
  sourceEventCodes: string[]
  entityKeyExpr: string
  aggType: string
  valueType: string
  valueExpr?: string
  filterExpr?: string
  windowType: string
  windowSize: string
  windowSlide?: string
  includeCurrentEvent: number
  ttlSeconds?: number
  stateHintJson?: Record<string, any>
  status: number
  version?: number
  description?: string
  creator?: string
  createTime?: Date
  updater?: string
  updateTime?: Date
}

export interface FeatureStreamPageReqVO extends PageParam {
  sceneCode?: string
  featureCode?: string
  featureName?: string
  entityType?: string
  status?: number
}

export const getFeatureStreamPage = (params: FeatureStreamPageReqVO) => {
  return request.get({ url: '/risk/feature-stream/page', params })
}

export const getFeatureStream = (id: number) => {
  return request.get({ url: '/risk/feature-stream/get?id=' + id })
}

export const createFeatureStream = (data: FeatureStreamVO) => {
  return request.post({ url: '/risk/feature-stream/create', data })
}

export const updateFeatureStream = (data: FeatureStreamVO) => {
  return request.put({ url: '/risk/feature-stream/update', data })
}

export const deleteFeatureStream = (id: number) => {
  return request.delete({ url: '/risk/feature-stream/delete?id=' + id })
}

export const getEntityTypeList = () => {
  return request.get({ url: '/risk/feature-stream/entity-type-list' })
}
