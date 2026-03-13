<template>
  <ContentWrap>
    <el-form ref="queryFormRef" class="-mb-15px" :model="queryParams" :inline="true" label-width="82px">
      <el-form-item label="所属场景" prop="sceneCode">
        <el-input v-model="queryParams.sceneCode" class="!w-220px" clearable placeholder="请输入场景编码" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="任务编码" prop="jobCode">
        <el-input v-model="queryParams.jobCode" class="!w-220px" clearable placeholder="请输入任务编码" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="任务状态" prop="jobStatus">
        <el-select v-model="queryParams.jobStatus" class="!w-220px" clearable placeholder="请选择任务状态">
          <el-option v-for="item in replayJobStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openCreateDialog" v-hasPermi="['risk:replay:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增回放
        </el-button>
        <el-button
          type="success"
          plain
          @click="handleExport"
          :loading="exportLoading"
          v-hasPermi="['risk:replay:export']"
        >
          <Icon icon="ep:download" class="mr-5px" /> 导出
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-alert
      title="S21 回放对比：当前支持 DECISION_LOG_EXPORT、FILE、KAFKA_EXPORT 三种输入源；可直接使用 classpath 样例完成演示。"
      type="info"
      :closable="false"
      class="mb-16px"
    />

    <el-table v-loading="loading" :data="list">
      <el-table-column label="所属场景" align="center" prop="sceneCode" width="140" />
      <el-table-column label="任务编码" align="center" prop="jobCode" min-width="220" show-overflow-tooltip />
      <el-table-column label="版本对比" align="center" min-width="140">
        <template #default="scope">v{{ scope.row.baselineVersionNo }} → v{{ scope.row.targetVersionNo }}</template>
      </el-table-column>
      <el-table-column label="输入源" align="center" min-width="180">
        <template #default="scope">{{ getReplayInputSourceTypeLabel(scope.row.inputSourceType) }}</template>
      </el-table-column>
      <el-table-column label="任务状态" align="center" width="140">
        <template #default="scope">
          <el-tag :type="getReplayJobStatusTag(scope.row.jobStatus)" effect="plain">
            {{ getReplayJobStatusLabel(scope.row.jobStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="事件 / 差异" align="center" min-width="140">
        <template #default="scope">
          {{ scope.row.diffEventCount ?? 0 }} / {{ scope.row.eventTotalCount ?? 0 }}
          <div class="text-12px text-[var(--el-text-color-secondary)]">
            {{ formatReplayRate(scope.row.diffEventCount, scope.row.eventTotalCount) }}
          </div>
        </template>
      </el-table-column>
      <el-table-column label="开始时间" align="center" prop="startedAt" width="180" :formatter="dateFormatter" />
      <el-table-column label="结束时间" align="center" prop="finishedAt" width="180" :formatter="dateFormatter" />
      <el-table-column label="备注" align="center" prop="remark" min-width="260" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.remark || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="180" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openDetail(scope.row.id)" v-hasPermi="['risk:replay:get']">详情</el-button>
          <el-button
            link
            type="success"
            :disabled="scope.row.jobStatus === 'RUNNING'"
            @click="executeReplay(scope.row.id)"
            v-hasPermi="['risk:replay:execute']"
          >
            执行
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <ReplayJobDetailDialog ref="detailRef" />
  </ContentWrap>

  <Dialog v-model="createDialogVisible" title="新增回放任务" width="680px">
    <el-form ref="createFormRef" :model="createFormData" :rules="createRules" label-width="110px">
      <el-form-item label="所属场景" prop="sceneCode">
        <el-input v-model="createFormData.sceneCode" placeholder="请输入场景编码，例如 TRADE_RISK" />
      </el-form-item>
      <el-form-item label="基线版本" prop="baselineVersionNo">
        <el-input-number v-model="createFormData.baselineVersionNo" :min="1" class="!w-full" controls-position="right" />
      </el-form-item>
      <el-form-item label="目标版本" prop="targetVersionNo">
        <el-input-number v-model="createFormData.targetVersionNo" :min="1" class="!w-full" controls-position="right" />
      </el-form-item>
      <el-form-item label="输入源类型" prop="inputSourceType">
        <el-select v-model="createFormData.inputSourceType" class="!w-full" placeholder="请选择输入源类型">
          <el-option v-for="item in replayInputSourceTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="输入引用" prop="inputRef">
        <div class="w-full">
          <el-input
            v-model="createFormData.inputRef"
            type="textarea"
            :rows="4"
            :placeholder="inputRefPlaceholder"
          />
          <div class="mt-6px flex items-center justify-between gap-12px text-12px text-[var(--el-text-color-secondary)]">
            <span>{{ inputRefHelpText }}</span>
            <el-button link type="primary" @click="fillInputRefExample">填充样例</el-button>
          </div>
        </div>
      </el-form-item>
      <el-form-item label="任务备注" prop="remark">
        <el-input
          v-model="createFormData.remark"
          type="textarea"
          :rows="4"
          placeholder="可填写本次回放的变更背景，例如候选版本阈值调整、文件样例验证或 Kafka 导出回归核对"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="createDialogVisible = false">取 消</el-button>
      <el-button type="primary" :loading="createLoading" @click="submitCreate">确 认</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import type { FormRules } from 'element-plus'
import { dateFormatter } from '@/utils/formatTime'
import download from '@/utils/download'
import * as ReplayApi from '@/api/risk/replay'
import ReplayJobDetailDialog from './ReplayJobDetailDialog.vue'
import {
  formatReplayRate,
  getReplayInputSourceHelpText,
  getReplayInputSourcePlaceholder,
  getReplayInputSourceSampleRef,
  getReplayInputSourceTypeLabel,
  getReplayJobStatusLabel,
  getReplayJobStatusTag,
  replayInputSourceTypeOptions,
  replayJobStatusOptions
} from './constants'

defineOptions({ name: 'RiskReplay' })

const message = useMessage()

const createDefaultQueryParams = (): ReplayApi.ReplayJobPageReqVO => ({
  pageNo: 1,
  pageSize: 10,
  sceneCode: 'TRADE_RISK',
  jobCode: undefined,
  jobStatus: undefined
})

const createDefaultCreateFormData = (): ReplayApi.ReplayJobCreateReqVO => ({
  sceneCode: 'TRADE_RISK',
  baselineVersionNo: 14,
  targetVersionNo: 15,
  inputSourceType: 'DECISION_LOG_EXPORT',
  inputRef: '7101,7102',
  remark: 'S21 回放样例：对比 v14 与候选版本 v15 的决策差异。'
})

const loading = ref(true)
const exportLoading = ref(false)
const total = ref(0)
const list = ref<ReplayApi.ReplayJobVO[]>([])
const queryParams = reactive<ReplayApi.ReplayJobPageReqVO>(createDefaultQueryParams())
const queryFormRef = ref()

const createDialogVisible = ref(false)
const createLoading = ref(false)
const createFormRef = ref()
const createFormData = reactive<ReplayApi.ReplayJobCreateReqVO>(createDefaultCreateFormData())
const createRules = reactive<FormRules>({
  sceneCode: [{ required: true, message: '所属场景不能为空', trigger: 'blur' }],
  baselineVersionNo: [{ required: true, message: '基线版本不能为空', trigger: 'blur' }],
  targetVersionNo: [{ required: true, message: '目标版本不能为空', trigger: 'blur' }],
  inputSourceType: [{ required: true, message: '输入源类型不能为空', trigger: 'change' }],
  inputRef: [
    {
      validator: (_rule, value, callback) => {
        if (createFormData.inputSourceType === 'DECISION_LOG_EXPORT') {
          callback()
          return
        }
        if (!String(value || '').trim()) {
          callback(new Error('当前输入源必须提供输入引用'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
})

const inputRefPlaceholder = computed(() => getReplayInputSourcePlaceholder(createFormData.inputSourceType))
const inputRefHelpText = computed(() => getReplayInputSourceHelpText(createFormData.inputSourceType))

const getList = async () => {
  loading.value = true
  try {
    const data = await ReplayApi.getReplayJobPage(queryParams)
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
    const data = await ReplayApi.exportReplayJob(queryParams)
    download.excel(data, '回放任务.xls')
  } catch {
  } finally {
    exportLoading.value = false
  }
}

const fillInputRefExample = () => {
  createFormData.inputRef = getReplayInputSourceSampleRef(createFormData.inputSourceType)
}

watch(
  () => createFormData.inputSourceType,
  (value, oldValue) => {
    const oldSample = getReplayInputSourceSampleRef(oldValue)
    if (!String(createFormData.inputRef || '').trim() || createFormData.inputRef === oldSample) {
      createFormData.inputRef = getReplayInputSourceSampleRef(value)
    }
  }
)

const openCreateDialog = () => {
  createDialogVisible.value = true
  Object.assign(createFormData, createDefaultCreateFormData())
  nextTick(() => createFormRef.value?.clearValidate())
}

const submitCreate = async () => {
  if (!createFormRef.value) {
    return
  }
  const valid = await createFormRef.value.validate().then(() => true).catch(() => false)
  if (!valid) {
    return
  }
  createLoading.value = true
  try {
    await ReplayApi.createReplayJob(createFormData)
    message.success('回放任务创建成功')
    createDialogVisible.value = false
    await getList()
  } finally {
    createLoading.value = false
  }
}

const detailRef = ref()
const openDetail = (id?: number) => {
  if (!id) {
    return
  }
  detailRef.value.open(id)
}

const executeReplay = async (id?: number) => {
  if (!id) {
    return
  }
  const data = await ReplayApi.executeReplayJob({ id })
  message.success('回放执行完成')
  await getList()
  detailRef.value.openWithData(data)
}

onMounted(() => {
  getList()
})
</script>
