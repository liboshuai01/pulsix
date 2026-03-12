import request from '@/config/axios'

export interface FeatureDerivedVO {
  id?: number
  sceneCode: string
  featureCode: string
  featureName: string
  featureType?: string
  engineType: string
  exprContent: string
  dependsOnJson: string[]
  valueType: string
  sandboxFlag: number
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

export interface FeatureDerivedPageReqVO extends PageParam {
  sceneCode?: string
  featureCode?: string
  featureName?: string
  status?: number
}

export interface FeatureDerivedDependencyOptionVO {
  code: string
  name: string
  dependencyType: string
  valueType?: string
  hint?: string
}

export interface FeatureDerivedValidateReqVO {
  sceneCode: string
  featureCode?: string
  engineType: string
  exprContent: string
  dependsOnJson: string[]
  sandboxFlag: number
}

export interface FeatureDerivedValidateRespVO {
  valid: boolean
  message: string
  missingDependencies?: string[]
  cycleDetected?: boolean
}

export const getFeatureDerivedPage = (params: FeatureDerivedPageReqVO) => {
  return request.get({ url: '/risk/feature-derived/page', params })
}

export const getFeatureDerived = (id: number) => {
  return request.get({ url: '/risk/feature-derived/get?id=' + id })
}

export const createFeatureDerived = (data: FeatureDerivedVO) => {
  return request.post({ url: '/risk/feature-derived/create', data })
}

export const updateFeatureDerived = (data: FeatureDerivedVO) => {
  return request.put({ url: '/risk/feature-derived/update', data })
}

export const deleteFeatureDerived = (id: number) => {
  return request.delete({ url: '/risk/feature-derived/delete?id=' + id })
}

export const getDependencyOptions = (sceneCode: string, currentFeatureCode?: string) => {
  return request.get({
    url: '/risk/feature-derived/dependency-options',
    params: {
      sceneCode,
      currentFeatureCode: currentFeatureCode || undefined
    }
  })
}

export const validateFeatureDerivedExpression = (data: FeatureDerivedValidateReqVO) => {
  return request.post({ url: '/risk/feature-derived/validate', data })
}
