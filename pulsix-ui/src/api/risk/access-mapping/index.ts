import request from '@/config/axios'

export interface AccessRawFieldItemVO {
  fieldName: string
  fieldLabel?: string
  fieldPath: string
  fieldType: string
  requiredFlag: number
  sampleValue?: string
  description?: string
  sortNo?: number
}

export interface AccessMappingRuleItemVO {
  targetFieldName: string
  mappingType: string
  sourceFieldPath?: string
  constantValue?: string
  scriptEngine?: string
  scriptContent?: string
  timePattern?: string
  enumMappingJson?: Record<string, string>
  description?: string
}

export interface AccessMappingVO {
  id?: number
  sceneCode: string
  eventCode: string
  eventName?: string
  sourceCode: string
  sourceName?: string
  sourceType?: string
  topicName?: string
  description?: string
  rawSampleJson: Record<string, any>
  sampleHeadersJson?: Record<string, any>
  rawFieldCount?: number
  mappingRuleCount?: number
  rawFields?: AccessRawFieldItemVO[]
  mappingRules?: AccessMappingRuleItemVO[]
  creator?: string
  createTime?: Date
  updater?: string
  updateTime?: Date
}

export interface AccessMappingSaveReqVO {
  id?: number
  eventCode: string
  sourceCode: string
  description?: string
  rawSampleJson: Record<string, any>
  sampleHeadersJson?: Record<string, any>
  rawFields: AccessRawFieldItemVO[]
  mappingRules: AccessMappingRuleItemVO[]
}

export interface AccessMappingPreviewVO {
  standardEventJson: Record<string, any>
  fieldSourceMap: Record<string, string>
  messages: string[]
}

// 查询接入映射分页
export const getAccessMappingPage = (params: PageParam) => {
  return request.get({ url: '/risk/access-mapping/page', params }) as Promise<
    PageResult<AccessMappingVO[]>
  >
}

// 查询接入映射详情
export const getAccessMapping = (id: number) => {
  return request.get({ url: '/risk/access-mapping/get?id=' + id }) as Promise<AccessMappingVO>
}

// 新增接入映射
export const createAccessMapping = (data: AccessMappingSaveReqVO) => {
  return request.post({ url: '/risk/access-mapping/create', data })
}

// 修改接入映射
export const updateAccessMapping = (data: AccessMappingSaveReqVO) => {
  return request.put({ url: '/risk/access-mapping/update', data })
}

// 删除接入映射
export const deleteAccessMapping = (id: number) => {
  return request.delete({ url: '/risk/access-mapping/delete?id=' + id })
}

// 预览标准事件
export const previewStandardEvent = (data: AccessMappingSaveReqVO): Promise<AccessMappingPreviewVO> => {
  return request.post({ url: '/risk/access-mapping/preview-standard', data })
}
