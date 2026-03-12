import request from '@/config/axios'

export interface SceneReleaseVO {
  id?: number
  sceneCode: string
  versionNo: number
  snapshotJson?: Record<string, any>
  checksum?: string
  publishStatus: string
  validationStatus: string
  validationReportJson?: Record<string, any>
  dependencyDigestJson?: Record<string, any>
  compileDurationMs?: number
  compiledFeatureCount?: number
  compiledRuleCount?: number
  compiledPolicyCount?: number
  publishedBy?: string
  publishedAt?: Date
  effectiveFrom?: Date
  rollbackFromVersion?: number
  remark?: string
  creator?: string
  createTime?: Date
  updater?: string
  updateTime?: Date
}

export interface SceneReleasePageReqVO extends PageParam {
  sceneCode?: string
  publishStatus?: string
  validationStatus?: string
}

export interface SceneReleaseCompileReqVO {
  sceneCode: string
  remark?: string
}

export const getSceneReleasePage = (params: SceneReleasePageReqVO) => {
  return request.get({ url: '/risk/release/page', params })
}

export const getSceneRelease = (id: number) => {
  return request.get({ url: '/risk/release/get?id=' + id })
}

export const compileSceneRelease = (data: SceneReleaseCompileReqVO) => {
  return request.post({ url: '/risk/release/compile', data })
}
