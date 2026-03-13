<template>
  <ContentWrap>
    <el-form ref="queryFormRef" class="-mb-15px" :model="queryParams" :inline="true" label-width="82px">
      <el-form-item label="所属场景" prop="sceneCode">
        <el-input v-model="queryParams.sceneCode" class="!w-220px" clearable placeholder="请输入场景编码" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="对象类型" prop="bizType">
        <el-select v-model="queryParams.bizType" class="!w-220px" clearable placeholder="请选择对象类型">
          <el-option v-for="item in auditBizTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="动作类型" prop="actionType">
        <el-select v-model="queryParams.actionType" class="!w-220px" clearable placeholder="请选择动作类型">
          <el-option v-for="item in auditActionOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="操作人" prop="operatorName">
        <el-input v-model="queryParams.operatorName" class="!w-220px" clearable placeholder="请输入操作人" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="操作时间" prop="operateTime">
        <el-date-picker
          v-model="queryParams.operateTime"
          value-format="YYYY-MM-DD HH:mm:ss"
          type="daterange"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          class="!w-360px"
        />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-alert
      title="S20 审计日志：支持按对象类型、操作人、时间范围过滤，并查看 before / after 变更快照。"
      type="info"
      :closable="false"
      class="mb-16px"
    />

    <el-table v-loading="loading" :data="list">
      <el-table-column label="所属场景" align="center" prop="sceneCode" width="140">
        <template #default="scope">{{ scope.row.sceneCode || '-' }}</template>
      </el-table-column>
      <el-table-column label="对象类型" align="center" width="140">
        <template #default="scope">
          <el-tag :type="getAuditBizTypeTag(scope.row.bizType)" effect="plain">
            {{ getAuditBizTypeLabel(scope.row.bizType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="对象编码" align="center" prop="bizCode" min-width="180" show-overflow-tooltip />
      <el-table-column label="动作类型" align="center" width="140">
        <template #default="scope">
          <el-tag :type="getAuditActionTag(scope.row.actionType)" effect="plain">
            {{ getAuditActionLabel(scope.row.actionType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作人" align="center" prop="operatorName" width="120" />
      <el-table-column label="变更说明" align="center" prop="remark" min-width="300" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.remark || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作时间" align="center" prop="operateTime" width="180" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="100" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openDetail(scope.row.id)" v-hasPermi="['risk:audit-log:get']">
            详情
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <AuditLogDetailDialog ref="detailRef" />
  </ContentWrap>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import * as AuditLogApi from '@/api/risk/audit-log'
import AuditLogDetailDialog from './AuditLogDetailDialog.vue'
import {
  auditActionOptions,
  auditBizTypeOptions,
  getAuditActionLabel,
  getAuditActionTag,
  getAuditBizTypeLabel,
  getAuditBizTypeTag
} from './constants'

defineOptions({ name: 'RiskAuditLog' })

const createDefaultQueryParams = (): AuditLogApi.AuditLogPageReqVO => ({
  pageNo: 1,
  pageSize: 10,
  sceneCode: 'TRADE_RISK',
  bizType: undefined,
  bizCode: undefined,
  actionType: undefined,
  operatorName: undefined,
  operateTime: undefined
})

const loading = ref(true)
const total = ref(0)
const list = ref<AuditLogApi.AuditLogVO[]>([])
const queryParams = reactive<AuditLogApi.AuditLogPageReqVO>(createDefaultQueryParams())
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await AuditLogApi.getAuditLogPage(queryParams)
    list.value = data.list ?? []
    total.value = data.total ?? 0
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value?.resetFields()
  Object.assign(queryParams, createDefaultQueryParams())
  handleQuery()
}

const detailRef = ref()
const openDetail = (id?: number) => {
  if (!id) {
    return
  }
  detailRef.value.open(id)
}

onMounted(() => {
  getList()
})
</script>
