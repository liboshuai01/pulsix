import request from '@/config/axios'

export interface EventFieldVO {
  id?: number
  sceneCode: string
  eventCode: string
  fieldCode: string
  fieldName: string
  fieldType: string
  fieldPath?: string
  standardFieldFlag: number
  requiredFlag: number
  nullableFlag: number
  defaultValue?: string
  sampleValue?: string
  description?: string
  sortNo: number
  creator?: string
  createTime?: Date
  updater?: string
  updateTime?: Date
}

export interface EventFieldPageReqVO extends PageParam {
  sceneCode?: string
  eventCode?: string
  fieldCode?: string
  fieldName?: string
  fieldType?: string
}

export const getEventFieldPage = (params: EventFieldPageReqVO) => {
  return request.get({ url: '/risk/event-field/page', params })
}

export const getEventField = (id: number) => {
  return request.get({ url: '/risk/event-field/get?id=' + id })
}

export const createEventField = (data: EventFieldVO) => {
  return request.post({ url: '/risk/event-field/create', data })
}

export const updateEventField = (data: EventFieldVO) => {
  return request.put({ url: '/risk/event-field/update', data })
}

export const deleteEventField = (id: number) => {
  return request.delete({ url: '/risk/event-field/delete?id=' + id })
}

export const updateEventFieldSort = (id: number, sortNo: number) => {
  return request.put({ url: '/risk/event-field/update-sort', data: { id, sortNo } })
}
