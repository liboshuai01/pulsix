import request from '@/config/axios'

export interface SceneVO {
  id?: number
  sceneCode: string
  sceneName: string
  sceneType: string
  accessMode: string
  defaultEventCode?: string
  defaultPolicyCode?: string
  standardTopicName?: string
  decisionTopicName?: string
  status: number
  description?: string
  creator?: string
  createTime?: Date
  updater?: string
  updateTime?: Date
}

export interface ScenePageReqVO extends PageParam {
  sceneCode?: string
  sceneName?: string
  status?: number
}

export const getScenePage = (params: ScenePageReqVO) => {
  return request.get({ url: '/risk/scene/page', params })
}

export const getScene = (id: number) => {
  return request.get({ url: '/risk/scene/get?id=' + id })
}

export const createScene = (data: SceneVO) => {
  return request.post({ url: '/risk/scene/create', data })
}

export const updateScene = (data: SceneVO) => {
  return request.put({ url: '/risk/scene/update', data })
}

export const updateSceneStatus = (id: number, status: number) => {
  return request.put({ url: '/risk/scene/update-status', data: { id, status } })
}

