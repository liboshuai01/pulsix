import request from '@/config/axios'
import dayjs from 'dayjs'

export interface PayNotifyTaskLogVO {
  id: number
  status: number
  notifyTimes: number
  createTime?: dayjs.ConfigType
  response?: string
}

export interface PayNotifyTaskDetailVO {
  id?: number
  status?: number
  merchantOrderId?: string
  merchantRefundId?: string
  merchantTransferId?: string
  appId?: number
  appName?: string
  dataId?: number | string
  type?: number
  notifyTimes?: number
  maxNotifyTimes?: number
  lastExecuteTime?: dayjs.ConfigType
  nextNotifyTime?: dayjs.ConfigType
  createTime?: dayjs.ConfigType
  updateTime?: dayjs.ConfigType
  logs: PayNotifyTaskLogVO[]
}

export interface PayNotifyTaskPageItemVO {
  id: number
  appName?: string
  merchantOrderId?: string
  merchantRefundId?: string
  merchantTransferId?: string
  type?: number
  dataId?: number | string
  status?: number
  lastExecuteTime?: dayjs.ConfigType
  nextNotifyTime?: dayjs.ConfigType
  notifyTimes?: number
  maxNotifyTimes?: number
}

export interface PayNotifyTaskPageReqVO extends PageParam {
  appId?: number
  type?: number
  dataId?: number | string
  status?: number
  merchantOrderId?: string
  merchantRefundId?: string
  merchantTransferId?: string
  createTime?: dayjs.ConfigType[]
}

// 获得支付通知明细
export const getNotifyTaskDetail = (id: number) => {
  return request.get<PayNotifyTaskDetailVO>({
    url: '/pay/notify/get-detail?id=' + id
  })
}

// 获得支付通知分页
export const getNotifyTaskPage = (query: PayNotifyTaskPageReqVO) => {
  return request.get<PageResult<PayNotifyTaskPageItemVO[]>>({
    url: '/pay/notify/page',
    params: query
  })
}
