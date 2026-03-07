import request from '@/config/axios'

// ERP 销售出库 VO
export interface SaleOutVO {
  id?: number // 销售出库编号
  no?: string // 销售出库号
  orderId?: number // 关联销售订单编号
  orderNo?: string // 关联销售订单号
  customerId?: number // 客户编号
  accountId?: number // 结算账户
  saleUserId?: number // 销售人员
  outTime?: Date // 出库时间
  totalCount?: number // 合计数量
  totalPrice?: number // 合计金额，单位：元
  status?: number // 状态
  remark?: string // 备注
  discountPercent?: number // 优惠率
  discountPrice?: number // 优惠金额
  otherPrice?: number // 其他费用
  fileUrl?: string // 附件
  items?: Array<Record<string, any>> // 出库项
}

// ERP 销售出库 API
export const SaleOutApi = {
  // 查询销售出库分页
  getSaleOutPage: async (params: any) => {
    return await request.get({ url: `/erp/sale-out/page`, params })
  },

  // 查询销售出库详情
  getSaleOut: async (id: number) => {
    return await request.get({ url: `/erp/sale-out/get?id=` + id })
  },

  // 新增销售出库
  createSaleOut: async (data: SaleOutVO) => {
    return await request.post({ url: `/erp/sale-out/create`, data })
  },

  // 修改销售出库
  updateSaleOut: async (data: SaleOutVO) => {
    return await request.put({ url: `/erp/sale-out/update`, data })
  },

  // 更新销售出库的状态
  updateSaleOutStatus: async (id: number, status: number) => {
    return await request.put({
      url: `/erp/sale-out/update-status`,
      params: {
        id,
        status
      }
    })
  },

  // 删除销售出库
  deleteSaleOut: async (ids: number[]) => {
    return await request.delete({
      url: `/erp/sale-out/delete`,
      params: {
        ids: ids.join(',')
      }
    })
  },

  // 导出销售出库 Excel
  exportSaleOut: async (params: any) => {
    return await request.download({ url: `/erp/sale-out/export-excel`, params })
  }
}
