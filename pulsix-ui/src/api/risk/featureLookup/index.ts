import request from '@/config/axios'

export interface FeatureLookupVO {
  id?: number
  sceneCode: string
  featureCode: string
  featureName: string
  featureType?: string
  lookupType: string
  keyExpr: string
  sourceRef: string
  defaultValue?: string
  valueType: string
  cacheTtlSeconds?: number
  timeoutMs?: number
  extraJson?: Record<string, any>
  status: number
  version?: number
  description?: string
  creator?: string
  createTime?: Date
  updater?: string
  updateTime?: Date
}

export interface FeatureLookupPageReqVO extends PageParam {
  sceneCode?: string
  featureCode?: string
  featureName?: string
  status?: number
}

export const getFeatureLookupPage = (params: FeatureLookupPageReqVO) => {
  return request.get({ url: '/risk/feature-lookup/page', params })
}

export const getFeatureLookup = (id: number) => {
  return request.get({ url: '/risk/feature-lookup/get?id=' + id })
}

export const createFeatureLookup = (data: FeatureLookupVO) => {
  return request.post({ url: '/risk/feature-lookup/create', data })
}

export const updateFeatureLookup = (data: FeatureLookupVO) => {
  return request.put({ url: '/risk/feature-lookup/update', data })
}

export const deleteFeatureLookup = (id: number) => {
  return request.delete({ url: '/risk/feature-lookup/delete?id=' + id })
}
