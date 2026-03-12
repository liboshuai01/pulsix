import request from '@/config/axios'

export interface EventSampleVO {
  id?: number
  sceneCode: string
  eventCode: string
  sampleCode: string
  sampleName: string
  sampleType: string
  sourceCode?: string
  sampleJson: Record<string, any>
  description?: string
  sortNo: number
  status: number
  creator?: string
  createTime?: Date
  updater?: string
  updateTime?: Date
}

export interface EventSamplePageReqVO extends PageParam {
  sceneCode?: string
  eventCode?: string
  sampleCode?: string
  sampleName?: string
  sampleType?: string
  status?: number
}

export interface EventSamplePreviewVO {
  sampleId: number
  sampleCode: string
  sampleName: string
  sampleType: string
  sampleJson: Record<string, any>
  standardEventJson: Record<string, any>
  missingRequiredFields: string[]
  defaultedFields: string[]
  mappedFields: string[]
}

export const getEventSamplePage = (params: EventSamplePageReqVO) => {
  return request.get({ url: '/risk/event-sample/page', params })
}

export const getEventSample = (id: number) => {
  return request.get({ url: '/risk/event-sample/get?id=' + id })
}

export const createEventSample = (data: EventSampleVO) => {
  return request.post({ url: '/risk/event-sample/create', data })
}

export const updateEventSample = (data: EventSampleVO) => {
  return request.put({ url: '/risk/event-sample/update', data })
}

export const deleteEventSample = (id: number) => {
  return request.delete({ url: '/risk/event-sample/delete?id=' + id })
}

export const previewEventSample = (id: number) => {
  return request.get({ url: '/risk/event-sample/preview?id=' + id })
}
