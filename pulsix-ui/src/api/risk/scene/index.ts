import request from '@/config/axios'

export interface SceneVO {
  id?: number
  sceneCode: string
  sceneName: string
  sceneType?: string
  runtimeMode: string
  defaultPolicyCode?: string
  decisionTimeoutMs: number
  logLevel: string
  status: number
  description?: string
  creator?: string
  createTime?: Date
  updater?: string
  updateTime?: Date
}

// 查询风控场景分页
export const getScenePage = (params: PageParam) => {
  return request.get({ url: '/risk/scene/page', params })
}

// 查询风控场景详情
export const getScene = (id: number) => {
  return request.get({ url: '/risk/scene/get?id=' + id })
}

// 查询风控场景精简列表
export const getSimpleSceneList = (): Promise<SceneVO[]> => {
  return request.get({ url: '/risk/scene/simple-list' })
}

// 新增风控场景
export const createScene = (data: SceneVO) => {
  return request.post({ url: '/risk/scene/create', data })
}

// 修改风控场景
export const updateScene = (data: SceneVO) => {
  return request.put({ url: '/risk/scene/update', data })
}

// 修改风控场景状态
export const updateSceneStatus = (id: number, status: number) => {
  return request.put({ url: '/risk/scene/update-status', data: { id, status } })
}

// 删除风控场景
export const deleteScene = (id: number) => {
  return request.delete({ url: '/risk/scene/delete?id=' + id })
}
