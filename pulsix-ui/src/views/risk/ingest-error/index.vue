<template>
  <ContentWrap>
    <el-form ref="queryFormRef" class="-mb-15px" :model="queryParams" :inline="true" label-width="82px">
      <el-form-item label="所属场景" prop="sceneCode">
        <el-input v-model="queryParams.sceneCode" class="!w-220px" clearable placeholder="请输入场景编码" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="接入源" prop="sourceCode">
        <el-input v-model="queryParams.sourceCode" class="!w-220px" clearable placeholder="请输入接入源编码" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="异常阶段" prop="ingestStage">
        <el-select v-model="queryParams.ingestStage" class="!w-220px" clearable placeholder="请选择异常阶段">
          <el-option v-for="item in ingestStageOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="错误码" prop="errorCode">
        <el-input v-model="queryParams.errorCode" class="!w-220px" clearable placeholder="请输入错误码" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="重处理" prop="reprocessStatus">
        <el-select v-model="queryParams.reprocessStatus" class="!w-220px" clearable placeholder="请选择重处理状态">
          <el-option v-for="item in reprocessStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="发生时间" prop="occurTime">
        <el-date-picker
          v-model="queryParams.occurTime"
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
      title="S18 接入治理：支持按来源、错误码、阶段与时间范围筛选接入异常，并查看 DLQ / 坏报文详情。"
      type="info"
      :closable="false"
      class="mb-16px"
    />

    <el-table v-loading="loading" :data="list">
      <el-table-column label="接入源" align="center" prop="sourceCode" width="160" />
      <el-table-column label="链路号" align="center" prop="traceId" min-width="180" show-overflow-tooltip />
      <el-table-column label="原始事件编号" align="center" prop="rawEventId" min-width="170" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.rawEventId || '-' }}</template>
      </el-table-column>
      <el-table-column label="场景 / 事件" align="center" min-width="160">
        <template #default="scope">{{ scope.row.sceneCode || '-' }} / {{ scope.row.eventCode || '-' }}</template>
      </el-table-column>
      <el-table-column label="异常阶段" align="center" width="140">
        <template #default="scope">
          <el-tag :type="getIngestStageTag(scope.row.ingestStage)" effect="plain">
            {{ getIngestStageLabel(scope.row.ingestStage) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="错误码" align="center" prop="errorCode" min-width="180" show-overflow-tooltip />
      <el-table-column label="错误说明" align="center" prop="errorMessage" min-width="280" show-overflow-tooltip />
      <el-table-column label="DLQ Topic" align="center" prop="errorTopicName" min-width="180" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.errorTopicName || '-' }}</template>
      </el-table-column>
      <el-table-column label="重处理" align="center" width="140">
        <template #default="scope">
          <el-tag :type="getReprocessStatusTag(scope.row.reprocessStatus)" effect="plain">
            {{ getReprocessStatusLabel(scope.row.reprocessStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="记录状态" align="center" width="110">
        <template #default="scope">
          <el-tag :type="getIngestRecordStatusTag(scope.row.status)" effect="plain">
            {{ getIngestRecordStatusLabel(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="发生时间" align="center" prop="occurTime" width="180" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="100" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openDetail(scope.row.id)" v-hasPermi="['risk:ingest-error:get']">
            详情
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <IngestErrorDetailDialog ref="detailRef" />
  </ContentWrap>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import * as IngestErrorApi from '@/api/risk/ingest-error'
import IngestErrorDetailDialog from './IngestErrorDetailDialog.vue'
import {
  getIngestRecordStatusLabel,
  getIngestRecordStatusTag,
  getIngestStageLabel,
  getIngestStageTag,
  getReprocessStatusLabel,
  getReprocessStatusTag,
  ingestStageOptions,
  reprocessStatusOptions
} from './constants'

defineOptions({ name: 'RiskIngestError' })

const createDefaultQueryParams = (): IngestErrorApi.IngestErrorPageReqVO => ({
  pageNo: 1,
  pageSize: 10,
  sceneCode: 'TRADE_RISK',
  sourceCode: undefined,
  traceId: undefined,
  rawEventId: undefined,
  ingestStage: undefined,
  errorCode: undefined,
  reprocessStatus: undefined,
  occurTime: undefined
})

const loading = ref(true)
const total = ref(0)
const list = ref<IngestErrorApi.IngestErrorVO[]>([])
const queryParams = reactive<IngestErrorApi.IngestErrorPageReqVO>(createDefaultQueryParams())
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await IngestErrorApi.getIngestErrorPage(queryParams)
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
