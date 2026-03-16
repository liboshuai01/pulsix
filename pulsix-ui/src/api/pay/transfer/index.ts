import request from '@/config/axios'
import dayjs from 'dayjs'

export interface TransferVO {
  id: number
  no?: string
  appId?: number
  appName?: string
  channelCode?: string
  merchantTransferId?: string
  type?: number | string
  status?: number | string
  price?: number
  subject?: string
  userName?: string
  userAccount?: string
  userIp?: string
  channelTransferNo?: string
  notifyUrl?: string
  channelNotifyData?: string
  successTime?: dayjs.ConfigType
  createTime?: dayjs.ConfigType
}

export interface TransferPageReqVO extends PageParam {
  no?: string
  appId?: number
  channelId?: number
  channelCode?: string
  merchantTransferId?: string
  type?: number | string
  status?: number | string
  successTime?: dayjs.ConfigType[]
  price?: number
  subject?: string
  userName?: string
  userAccount?: string
  accountNo?: string
  channelTransferNo?: string
  createTime?: dayjs.ConfigType[]
}

// 查询转账单列表
export const getTransferPage = async (params: TransferPageReqVO) => {
  return await request.get<PageResult<TransferVO[]>>({ url: `/pay/transfer/page`, params })
}

// 查询转账单详情
export const getTransfer = async (id: number) => {
  return await request.get<TransferVO>({ url: '/pay/transfer/get?id=' + id })
}

// 导出转账单
export const exportTransfer = async (params: TransferPageReqVO) => {
  return await request.download({ url: '/pay/transfer/export-excel', params })
}
