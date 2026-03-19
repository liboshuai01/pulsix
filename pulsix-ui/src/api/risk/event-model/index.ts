import request from '@/config/axios'

export interface EventFieldItemVO {
  fieldName: string
  fieldLabel?: string
  fieldType: string
  requiredFlag: number
  defaultValue?: string
  sampleValue?: string
  description?: string
  sortNo?: number
}

export interface EventModelVO {
  id?: number
  sceneCode: string
  eventCode: string
  eventName: string
  version?: number
  status: number
  description?: string
  fields: EventFieldItemVO[]
  bindingSources?: EventBindingSourceItemVO[]
  deletable?: boolean
  deleteBlockedReason?: string
  creator?: string
  createTime?: Date
  updater?: string
  updateTime?: Date
}

export interface EventModelSaveReqVO {
  id?: number
  sceneCode: string
  eventCode: string
  eventName: string
  status: number
  description?: string
  fields: EventFieldItemVO[]
}

export interface EventModelSimpleVO {
  sceneCode: string
  eventCode: string
  eventName: string
}

export interface EventBindingSourceItemVO {
  sourceCode: string
  sourceName: string
  sourceType: string
  topicName: string
  status: number
}

export interface EventModelPreviewVO {
  standardEventJson: Record<string, any>
  requiredFields: string[]
  optionalFields: string[]
  fieldTypes: Record<string, string>
}

// 查询事件模型分页
export const getEventModelPage = (params: PageParam) => {
  return request.get({ url: '/risk/event-model/page', params }) as Promise<PageResult<EventModelVO[]>>
}

// 查询事件模型详情
export const getEventModel = (id: number) => {
  return request.get({ url: '/risk/event-model/get?id=' + id }) as Promise<EventModelVO>
}

// 根据事件编码查询事件模型详情
export const getEventModelByCode = (eventCode: string) => {
  return request.get({ url: '/risk/event-model/get-by-code', params: { eventCode } }) as Promise<EventModelVO>
}

// 查询启用中的事件模型精简列表
export const getSimpleEventModelList = (sceneCode?: string): Promise<EventModelSimpleVO[]> => {
  return request.get({ url: '/risk/event-model/simple-list', params: { sceneCode } })
}

// 新增事件模型
export const createEventModel = (data: EventModelSaveReqVO) => {
  return request.post({ url: '/risk/event-model/create', data })
}

// 修改事件模型
export const updateEventModel = (data: EventModelSaveReqVO) => {
  return request.put({ url: '/risk/event-model/update', data })
}

// 修改事件模型状态
export const updateEventModelStatus = (id: number, status: number) => {
  return request.put({ url: '/risk/event-model/update-status', data: { id, status } })
}

// 删除事件模型
export const deleteEventModel = (id: number) => {
  return request.delete({ url: '/risk/event-model/delete?id=' + id })
}

// 预览标准事件
export const previewStandardEvent = (data: EventModelSaveReqVO): Promise<EventModelPreviewVO> => {
  return request.post({ url: '/risk/event-model/preview-standard', data })
}
