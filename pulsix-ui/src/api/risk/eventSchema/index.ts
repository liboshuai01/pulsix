import request from '@/config/axios'

export interface EventSchemaVO {
  id?: number
  sceneCode: string
  eventCode: string
  eventName: string
  eventType: string
  sourceType: string
  rawTopicName?: string
  standardTopicName?: string
  version?: number
  description?: string
  creator?: string
  createTime?: Date
  updater?: string
  updateTime?: Date
}

export interface EventSchemaPageReqVO extends PageParam {
  sceneCode?: string
  eventCode?: string
  eventName?: string
  eventType?: string
}

export const getEventSchemaPage = (params: EventSchemaPageReqVO) => {
  return request.get({ url: '/risk/event-schema/page', params })
}

export const getEventSchema = (id: number) => {
  return request.get({ url: '/risk/event-schema/get?id=' + id })
}

export const createEventSchema = (data: EventSchemaVO) => {
  return request.post({ url: '/risk/event-schema/create', data })
}

export const updateEventSchema = (data: EventSchemaVO) => {
  return request.put({ url: '/risk/event-schema/update', data })
}

export const deleteEventSchema = (id: number) => {
  return request.delete({ url: '/risk/event-schema/delete?id=' + id })
}
