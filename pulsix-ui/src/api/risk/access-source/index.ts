import request from '@/config/axios'

export interface AccessSourceVO {
  id?: number
  sourceCode: string
  sourceName: string
  sourceType: string
  topicName: string
  accessProtocol: string
  appId?: string
  ownerName?: string
  contactEmail?: string
  rateLimitQps?: number
  allowedSceneCodes: string[]
  ipWhitelist?: string[]
  status: number
  description?: string
  creator?: string
  createTime?: Date
  updater?: string
  updateTime?: Date
}

export interface AccessSourceSimpleVO {
  sourceCode: string
  sourceName: string
  sourceType: string
  topicName: string
  status: number
}

// 查询接入源分页
export const getAccessSourcePage = (params: PageParam) => {
  return request.get({ url: '/risk/access-source/page', params }) as Promise<
    PageResult<AccessSourceVO[]>
  >
}

// 查询接入源详情
export const getAccessSource = (id: number) => {
  return request.get({ url: '/risk/access-source/get?id=' + id }) as Promise<AccessSourceVO>
}

// 查询启用中的接入源精简列表
export const getSimpleAccessSourceList = (sceneCode?: string): Promise<AccessSourceSimpleVO[]> => {
  return request.get({ url: '/risk/access-source/simple-list', params: { sceneCode } })
}

// 新增接入源
export const createAccessSource = (data: AccessSourceVO) => {
  return request.post({ url: '/risk/access-source/create', data })
}

// 修改接入源
export const updateAccessSource = (data: AccessSourceVO) => {
  return request.put({ url: '/risk/access-source/update', data })
}

// 修改接入源状态
export const updateAccessSourceStatus = (id: number, status: number) => {
  return request.put({ url: '/risk/access-source/update-status', data: { id, status } })
}

// 删除接入源
export const deleteAccessSource = (id: number) => {
  return request.delete({ url: '/risk/access-source/delete?id=' + id })
}
