<template>
  <ContentWrap>
    <el-form
      ref="queryFormRef"
      class="-mb-15px"
      :model="queryParams"
      :inline="true"
      label-width="82px"
    >
      <el-form-item label="所属场景" prop="sceneCode">
        <el-input
          v-model="queryParams.sceneCode"
          class="!w-220px"
          clearable
          placeholder="请输入场景编码"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="用例编码" prop="caseCode">
        <el-input
          v-model="queryParams.caseCode"
          class="!w-220px"
          clearable
          placeholder="请输入用例编码"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="用例名称" prop="caseName">
        <el-input
          v-model="queryParams.caseName"
          class="!w-220px"
          clearable
          placeholder="请输入用例名称"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="版本模式" prop="versionSelectMode">
        <el-select v-model="queryParams.versionSelectMode" class="!w-220px" clearable placeholder="请选择版本模式">
          <el-option
            v-for="item in simulationVersionSelectModeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" class="!w-220px" clearable placeholder="请选择状态">
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['risk:simulation:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="所属场景" align="center" prop="sceneCode" width="140" />
      <el-table-column label="用例编码" align="center" prop="caseCode" min-width="170" />
      <el-table-column label="用例名称" align="center" prop="caseName" min-width="180" />
      <el-table-column label="版本模式" align="center" min-width="160">
        <template #default="scope">
          <el-space wrap>
            <el-tag :type="getSimulationVersionSelectModeTag(scope.row.versionSelectMode)" effect="plain">
              {{ getSimulationVersionSelectModeLabel(scope.row.versionSelectMode) }}
            </el-tag>
            <span v-if="scope.row.versionSelectMode === 'FIXED'">
              {{ scope.row.versionNo ? `v${scope.row.versionNo}` : '-' }}
            </span>
            <span v-else>自动取最新已发布版本</span>
          </el-space>
        </template>
      </el-table-column>
      <el-table-column label="期望动作" align="center" min-width="110">
        <template #default="scope">
          <el-tag v-if="scope.row.expectedAction" :type="getRiskActionTag(scope.row.expectedAction)" effect="plain">
            {{ getRiskActionLabel(scope.row.expectedAction) }}
          </el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="期望规则" align="center" min-width="180">
        <template #default="scope">
          <el-space wrap>
            <el-tag v-for="item in scope.row.expectedHitRules || []" :key="item" effect="plain">{{ item }}</el-tag>
            <span v-if="!scope.row.expectedHitRules || scope.row.expectedHitRules.length === 0">-</span>
          </el-space>
        </template>
      </el-table-column>
      <el-table-column label="最近报告" align="center" min-width="300">
        <template #default="scope">
          <div v-if="scope.row.latestReportId" class="flex flex-col items-center gap-6px">
            <el-space wrap>
              <el-tag effect="plain">{{ scope.row.latestReportVersionNo ? `v${scope.row.latestReportVersionNo}` : '-' }}</el-tag>
              <el-tag :type="getRiskActionTag(scope.row.latestFinalAction)" effect="plain">
                {{ getRiskActionLabel(scope.row.latestFinalAction) }}
              </el-tag>
              <el-tag :type="getSimulationPassTag(scope.row.latestPassFlag)" effect="plain">
                {{ getSimulationPassLabel(scope.row.latestPassFlag) }}
              </el-tag>
            </el-space>
            <el-space wrap>
              <el-tag v-for="item in scope.row.latestHitRules || []" :key="item" effect="plain">{{ item }}</el-tag>
              <span v-if="!scope.row.latestHitRules || scope.row.latestHitRules.length === 0">未命中规则</span>
            </el-space>
            <div class="text-12px text-[var(--el-text-color-secondary)]">
              {{ scope.row.latestDurationMs ?? '-' }} ms / {{ scope.row.latestReportTime ? formatDate(scope.row.latestReportTime) : '-' }}
            </div>
          </div>
          <span v-else class="text-[var(--el-text-color-secondary)]">暂无执行记录</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="100">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="更新时间" align="center" prop="updateTime" width="180" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="260" fixed="right">
        <template #default="scope">
          <el-button
            link
            type="primary"
            :loading="executingId === scope.row.id"
            @click="handleExecute(scope.row)"
            v-hasPermi="['risk:simulation:execute']"
          >
            执行
          </el-button>
          <el-button
            link
            type="primary"
            :disabled="!scope.row.latestReportId"
            @click="openReport(scope.row.latestReportId)"
            v-hasPermi="['risk:simulation:query']"
          >
            报告
          </el-button>
          <el-button
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
            v-hasPermi="['risk:simulation:update']"
          >
            编辑
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(scope.row.id)"
            v-hasPermi="['risk:simulation:delete']"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <SimulationCaseForm ref="formRef" @success="getList" />
    <SimulationReportDialog ref="reportRef" />
  </ContentWrap>
</template>

<script lang="ts" setup>
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { dateFormatter, formatDate } from '@/utils/formatTime'
import * as SimulationApi from '@/api/risk/simulation'
import SimulationCaseForm from './SimulationCaseForm.vue'
import SimulationReportDialog from './SimulationReportDialog.vue'
import {
  extractSimulationFinalAction,
  getRiskActionLabel,
  getRiskActionTag,
  getSimulationPassLabel,
  getSimulationPassTag,
  getSimulationVersionSelectModeLabel,
  getSimulationVersionSelectModeTag,
  simulationVersionSelectModeOptions
} from './constants'

defineOptions({ name: 'RiskSimulation' })

const message = useMessage()

const createDefaultQueryParams = (): SimulationApi.SimulationCasePageReqVO => ({
  pageNo: 1,
  pageSize: 10,
  sceneCode: 'TRADE_RISK',
  caseCode: undefined,
  caseName: undefined,
  versionSelectMode: undefined,
  status: undefined
})

const loading = ref(true)
const total = ref(0)
const list = ref<SimulationApi.SimulationCaseVO[]>([])
const queryParams = reactive<SimulationApi.SimulationCasePageReqVO>(createDefaultQueryParams())
const queryFormRef = ref()
const executingId = ref<number>()

const getList = async () => {
  loading.value = true
  try {
    const data = await SimulationApi.getSimulationCasePage(queryParams)
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

const formRef = ref()
const openForm = (type: 'create' | 'update', id?: number) => {
  formRef.value.open(type, id)
}

const reportRef = ref()
const openReport = (id?: number) => {
  if (!id) {
    message.warning('暂无可查看的仿真报告')
    return
  }
  reportRef.value.open(id)
}

const handleExecute = async (row: SimulationApi.SimulationCaseVO) => {
  if (!row.id) {
    return
  }
  executingId.value = row.id
  try {
    const report = await SimulationApi.executeSimulation({ caseId: row.id })
    const finalAction = extractSimulationFinalAction(report)
    if (report.passFlag === 1) {
      message.success(`仿真完成：最终动作 ${getRiskActionLabel(finalAction)}`)
    } else {
      message.warning(`仿真完成：最终动作 ${getRiskActionLabel(finalAction)}，与期望不一致`)
    }
    await getList()
    reportRef.value.openWithData(report)
  } finally {
    executingId.value = undefined
  }
}

const handleDelete = async (id?: number) => {
  if (!id) {
    return
  }
  try {
    await message.delConfirm()
    await SimulationApi.deleteSimulationCase(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => {
  getList()
})
</script>
