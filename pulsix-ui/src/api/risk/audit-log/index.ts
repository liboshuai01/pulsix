import request from '@/config/axios'

export interface AuditLogVO {
  id?: number
  traceId: string
  sceneCode?: string
  operatorId?: number
  operatorName: string
  bizType: string
  bizCode: string
  actionType: string
  remark?: string
  operateTime?: Date | string
  createTime?: Date | string
}

export interface AuditLogDetailVO extends AuditLogVO {
  beforeJson?: Record<string, any>
  afterJson?: Record<string, any>
}

export interface AuditLogPageReqVO extends PageParam {
  sceneCode?: string
  bizType?: string
  bizCode?: string
  actionType?: string
  operatorName?: string
  operateTime?: string[]
}

export const getAuditLogPage = (params: AuditLogPageReqVO) => {
  return request.get({ url: '/risk/audit-log/page', params })
}

export const exportAuditLog = (params: AuditLogPageReqVO) => {
  return request.download({ url: '/risk/audit-log/export-excel', params })
}

export const getAuditLog = (id: number) => {
  return request.get({ url: '/risk/audit-log/get?id=' + id })
}
