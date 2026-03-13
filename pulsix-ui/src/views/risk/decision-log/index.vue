<template>
  <ContentWrap>
    <el-form ref="queryFormRef" class="-mb-15px" :model="queryParams" :inline="true" label-width="82px">
      <el-form-item label="所属场景" prop="sceneCode">
        <el-input v-model="queryParams.sceneCode" class="!w-220px" clearable placeholder="请输入场景编码" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="链路号" prop="traceId">
        <el-input v-model="queryParams.traceId" class="!w-220px" clearable placeholder="请输入 traceId" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="事件编号" prop="eventId">
        <el-input v-model="queryParams.eventId" class="!w-220px" clearable placeholder="请输入 eventId" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="最终动作" prop="finalAction">
        <el-select v-model="queryParams.finalAction" class="!w-220px" clearable placeholder="请选择最终动作">
          <el-option v-for="item in riskActionOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="版本号" prop="versionNo">
        <el-input-number v-model="queryParams.versionNo" :min="1" class="!w-220px" controls-position="right" placeholder="请输入版本号" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button
          type="success"
          plain
          @click="handleExport"
          :loading="exportLoading"
          v-hasPermi="['risk:decision-log:export']"
        >
          <Icon icon="ep:download" class="mr-5px" /> 导出
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="所属场景" align="center" prop="sceneCode" width="140" />
      <el-table-column label="链路号" align="center" prop="traceId" min-width="170" show-overflow-tooltip />
      <el-table-column label="事件编号" align="center" prop="eventId" min-width="170" show-overflow-tooltip />
      <el-table-column label="接入源" align="center" prop="sourceCode" width="140">
        <template #default="scope">{{ scope.row.sourceCode || '-' }}</template>
      </el-table-column>
      <el-table-column label="主体编号" align="center" prop="entityId" min-width="140">
        <template #default="scope">{{ scope.row.entityId || '-' }}</template>
      </el-table-column>
      <el-table-column label="最终动作" align="center" width="100">
        <template #default="scope">
          <el-tag :type="getRiskActionTag(scope.row.finalAction)" effect="plain">
            {{ getRiskActionLabel(scope.row.finalAction) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="版本" align="center" width="90">
        <template #default="scope">{{ scope.row.versionNo ? `v${scope.row.versionNo}` : '-' }}</template>
      </el-table-column>
      <el-table-column label="命中规则" align="center" min-width="180">
        <template #default="scope">
          <el-space wrap>
            <el-tag v-for="item in scope.row.hitRuleCodes || []" :key="item" effect="plain">{{ item }}</el-tag>
            <span v-if="!scope.row.hitRuleCodes || scope.row.hitRuleCodes.length === 0">未命中规则</span>
          </el-space>
        </template>
      </el-table-column>
      <el-table-column label="耗时" align="center" width="100">
        <template #default="scope">{{ scope.row.latencyMs ?? '-' }} ms</template>
      </el-table-column>
      <el-table-column label="事件时间" align="center" prop="eventTime" width="180" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="160" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openDetail(scope.row.id, 'detail')" v-hasPermi="['risk:decision-log:get']">
            详情
          </el-button>
          <el-button link type="primary" @click="openDetail(scope.row.id, 'hit')" v-hasPermi="['risk:decision-log:detail']">
            命中明细
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <DecisionLogDetailDialog ref="detailRef" />
  </ContentWrap>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import download from '@/utils/download'
import * as DecisionLogApi from '@/api/risk/decision-log'
import DecisionLogDetailDialog from './DecisionLogDetailDialog.vue'
import { getRiskActionLabel, getRiskActionTag, riskActionOptions } from './constants'

defineOptions({ name: 'RiskDecisionLog' })

const message = useMessage()

const createDefaultQueryParams = (): DecisionLogApi.DecisionLogPageReqVO => ({
  pageNo: 1,
  pageSize: 10,
  sceneCode: 'TRADE_RISK',
  traceId: undefined,
  eventId: undefined,
  finalAction: undefined,
  versionNo: undefined
})

const loading = ref(true)
const exportLoading = ref(false)
const total = ref(0)
const list = ref<DecisionLogApi.DecisionLogVO[]>([])
const queryParams = reactive<DecisionLogApi.DecisionLogPageReqVO>(createDefaultQueryParams())
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await DecisionLogApi.getDecisionLogPage(queryParams)
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

const handleExport = async () => {
  try {
    await message.exportConfirm()
    exportLoading.value = true
    const data = await DecisionLogApi.exportDecisionLog(queryParams)
    download.excel(data, '决策日志.xls')
  } catch {
  } finally {
    exportLoading.value = false
  }
}

const detailRef = ref()
const openDetail = (id?: number, tab: 'hit' | 'input' | 'feature' | 'detail' = 'detail') => {
  if (!id) {
    return
  }
  detailRef.value.open(id, tab)
}

onMounted(() => {
  getList()
})
</script>
