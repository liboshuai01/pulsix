import request from '@/config/axios'

export interface ListSetVO {
  id?: number
  sceneCode: string
  listCode: string
  listName: string
  matchType: string
  listType: string
  storageType: string
  syncMode: string
  syncStatus?: string
  lastSyncTime?: Date | string
  status: number
  description?: string
  redisKeyPrefix?: string
  creator?: string
  createTime?: Date
  updater?: string
  updateTime?: Date
}

export interface ListSetPageReqVO extends PageParam {
  sceneCode?: string
  listCode?: string
  listName?: string
  matchType?: string
  listType?: string
  syncStatus?: string
  status?: number
}

export interface ListItemVO {
  id?: number
  sceneCode: string
  listCode: string
  matchKey?: string
  matchValue: string
  expireAt?: string | Date
  status: number
  sourceType: string
  batchNo?: string
  remark?: string
  extJson?: Record<string, any>
  creator?: string
  createTime?: Date
  updater?: string
  updateTime?: Date
}

export interface ListItemPageReqVO extends PageParam {
  sceneCode: string
  listCode: string
  matchValue?: string
  status?: number
}

export interface ListSyncVO {
  listCode: string
  redisKeyPrefix: string
  syncedItemCount: number
  storageType: string
}

export const getListSetPage = (params: ListSetPageReqVO) => {
  return request.get({ url: '/risk/list/set/page', params })
}

export const getListSet = (id: number) => {
  return request.get({ url: '/risk/list/set/get?id=' + id })
}

export const createListSet = (data: ListSetVO) => {
  return request.post({ url: '/risk/list/set/create', data })
}

export const updateListSet = (data: ListSetVO) => {
  return request.put({ url: '/risk/list/set/update', data })
}

export const updateListSetStatus = (id: number, status: number) => {
  return request.put({ url: '/risk/list/set/update-status', data: { id, status } })
}

export const deleteListSet = (id: number) => {
  return request.delete({ url: '/risk/list/set/delete?id=' + id })
}

export const syncListSet = (id: number) => {
  return request.post({ url: '/risk/list/set/sync?id=' + id })
}

export const getListItemPage = (params: ListItemPageReqVO) => {
  return request.get({ url: '/risk/list/item/page', params })
}

export const getListItem = (id: number) => {
  return request.get({ url: '/risk/list/item/get?id=' + id })
}

export const createListItem = (data: ListItemVO) => {
  return request.post({ url: '/risk/list/item/create', data })
}

export const updateListItem = (data: ListItemVO) => {
  return request.put({ url: '/risk/list/item/update', data })
}

export const updateListItemStatus = (id: number, status: number) => {
  return request.put({ url: '/risk/list/item/update-status', data: { id, status } })
}

export const deleteListItem = (id: number) => {
  return request.delete({ url: '/risk/list/item/delete?id=' + id })
}
