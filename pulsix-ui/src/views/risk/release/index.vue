<template>
  <ContentWrap>
    <el-form
      ref="queryFormRef"
      class="-mb-15px"
      :model="queryParams"
      :inline="true"
      label-width="92px"
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
      <el-form-item label="发布状态" prop="publishStatus">
        <el-select v-model="queryParams.publishStatus" class="!w-180px" clearable placeholder="请选择发布状态">
          <el-option v-for="item in publishStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="预检状态" prop="validationStatus">
        <el-select
          v-model="queryParams.validationStatus"
          class="!w-180px"
          clearable
          placeholder="请选择预检状态"
        >
          <el-option
            v-for="item in validationStatusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openCompileDialog" v-hasPermi="['risk:release:compile']">
          <Icon icon="ep:promotion" class="mr-5px" /> 预检 / 编译
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-alert
      title="S13 只做发布预检、依赖摘要和快照预览；正式发布与回滚放到 S14。"
      type="info"
      :closable="false"
      class="mb-16px"
    />

    <el-table v-loading="loading" :data="list">
      <el-table-column label="所属场景" align="center" prop="sceneCode" min-width="120" />
      <el-table-column label="候选版本" align="center" min-width="110">
        <template #default="scope">v{{ scope.row.versionNo }}</template>
      </el-table-column>
      <el-table-column label="发布状态" align="center" min-width="120">
        <template #default="scope">
          <el-tag :type="getReleasePublishStatusTag(scope.row.publishStatus)" effect="plain">
            {{ getReleasePublishStatusLabel(scope.row.publishStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="预检状态" align="center" min-width="120">
        <template #default="scope">
          <el-tag :type="getReleaseValidationStatusTag(scope.row.validationStatus)" effect="plain">
            {{ getReleaseValidationStatusLabel(scope.row.validationStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="编译产物" align="center" min-width="190">
        <template #default="scope">{{ formatReleaseCompileSummary(scope.row) }}</template>
      </el-table-column>
      <el-table-column label="编译耗时" align="center" min-width="110">
        <template #default="scope">{{ scope.row.compileDurationMs ?? '-' }} ms</template>
      </el-table-column>
      <el-table-column label="快照摘要" align="center" prop="checksum" min-width="260" show-overflow-tooltip />
      <el-table-column label="说明" align="center" prop="remark" min-width="220" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.remark || '-' }}</template>
      </el-table-column>
      <el-table-column label="更新时间" align="center" prop="updateTime" width="180" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="160" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openPreview(scope.row.id!)" v-hasPermi="['risk:release:preview']">
            预览
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
  </ContentWrap>

  <Dialog v-model="compileDialogVisible" title="发布预检 / 编译" width="620px">
    <el-form ref="compileFormRef" :model="compileFormData" :rules="compileRules" label-width="100px">
      <el-form-item label="所属场景" prop="sceneCode">
        <el-input v-model="compileFormData.sceneCode" placeholder="请输入场景编码，例如 TRADE_RISK" />
      </el-form-item>
      <el-form-item label="版本说明" prop="remark">
        <el-input
          v-model="compileFormData.remark"
          type="textarea"
          :rows="4"
          placeholder="可填写本次候选版本的预检背景，例如阈值调整或规则整理说明"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="compileDialogVisible = false">取 消</el-button>
      <el-button type="primary" :loading="compileLoading" @click="submitCompile">开始预检</el-button>
    </template>
  </Dialog>

  <ReleasePreview ref="previewRef" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import * as ReleaseApi from '@/api/risk/release'
import ReleasePreview from './ReleasePreview.vue'
import {
  formatReleaseCompileSummary,
  getReleasePublishStatusLabel,
  getReleasePublishStatusTag,
  getReleaseValidationStatusLabel,
  getReleaseValidationStatusTag,
  publishStatusOptions,
  validationStatusOptions
} from './constants'

defineOptions({ name: 'RiskRelease' })

const message = useMessage()

const createDefaultQueryParams = (): ReleaseApi.SceneReleasePageReqVO => ({
  pageNo: 1,
  pageSize: 10,
  sceneCode: 'TRADE_RISK',
  publishStatus: undefined,
  validationStatus: undefined
})

const loading = ref(true)
const total = ref(0)
const list = ref<ReleaseApi.SceneReleaseVO[]>([])
const queryParams = reactive<ReleaseApi.SceneReleasePageReqVO>(createDefaultQueryParams())
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await ReleaseApi.getSceneReleasePage(queryParams)
    list.value = data.list
    total.value = data.total
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

const previewRef = ref()
const openPreview = (id: number) => {
  previewRef.value.open(id)
}

const compileDialogVisible = ref(false)
const compileLoading = ref(false)
const compileFormRef = ref()
const compileFormData = reactive<ReleaseApi.SceneReleaseCompileReqVO>({
  sceneCode: 'TRADE_RISK',
  remark: ''
})
const compileRules = reactive({
  sceneCode: [{ required: true, message: '所属场景不能为空', trigger: 'blur' }],
  remark: [{ max: 512, message: '版本说明长度不能超过 512 个字符', trigger: 'blur' }]
})

const openCompileDialog = () => {
  compileDialogVisible.value = true
  compileFormRef.value?.resetFields()
  Object.assign(compileFormData, {
    sceneCode: queryParams.sceneCode || 'TRADE_RISK',
    remark: ''
  })
}

const submitCompile = async () => {
  const valid = await compileFormRef.value?.validate()
  if (!valid) {
    return
  }
  compileLoading.value = true
  try {
    const data = await ReleaseApi.compileSceneRelease({
      sceneCode: compileFormData.sceneCode.trim(),
      remark: compileFormData.remark?.trim() || undefined
    })
    compileDialogVisible.value = false
    if (data.validationStatus === 'PASSED') {
      message.success(`候选版本 v${data.versionNo} 预检通过`)
    } else {
      message.warning(`候选版本 v${data.versionNo} 预检未通过，请查看预检报告`)
    }
    await getList()
    previewRef.value.openWithData(data)
  } finally {
    compileLoading.value = false
  }
}

onMounted(() => {
  getList()
})
</script>
